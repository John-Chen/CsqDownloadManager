/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.service;

import android.content.Context;
import android.os.PowerManager;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.webkit.URLUtil;
import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.configer.DownloadConfiger;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.db.update.UpdateCondition;
import com.csq.downloadmanager.notification.DownloadNotification;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.FileUtil;
import com.csq.downloadmanager.util.Helpers;
import com.csq.downloadmanager.util.LogUtil;
import com.csq.downloadmanager.util.StorageUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DownloadThread implements Runnable, DownloadService.Cancelable {


    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------
    private final Context context;
    private final DownloadInfo downloadInfo;
    private final SystemFacade systemFacade;
    private final DownloadNotification downloadNotification;
    private boolean isCanceled = false;

    private final List<DownloadTask> taskList = new ArrayList<>();
    private final SparseArray<Integer> taskResults = new SparseArray<>();

    // ----------------------- Constructors ----------------------

    public DownloadThread(@NonNull Context context,
                          @NonNull DownloadInfo downloadInfo,
                          @NonNull SystemFacade systemFacade) {
        this.context = context;
        this.downloadInfo = downloadInfo;
        this.systemFacade = systemFacade;
        downloadNotification = new DownloadNotification(context);
    }


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public void run() {
        LogUtil.w(DownloadThread.class, "Thread start : " + downloadInfo.getUrl());

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        PowerManager.WakeLock wakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CsqDownMng");

        HttpURLConnection connection = null;
        try {
            wakeLock.acquire();

            checkWhetherCanceled();

            //检测网络连接
            checkConnectivity();

            //检查存储目录
            if(!FileUtil.checkPathExist(downloadInfo.getFolderPath())){
                throw new StopRequest(DownloadInfo.StatusFailedSdcardUnmounted,
                        "failed to getConnection");
            }

            //更新数据库下载状态为StatusDowning
            downloadInfo.setStatus(DownloadInfo.StatusDowning);
            DownloadInfoDao.getInstace(context).updateDownload(
                    UpdateCondition.create()
                            .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusDowning)
                            .setWhere(new Where().eq(Downloads.ColumnID, downloadInfo.getId())));
            downloadNotification.update(systemFacade, downloadInfo);

            //更新重定向地址，没有的话reDirectUrl=url
            String reDirectUrl = getRedirectUrl(downloadInfo.getUrl(), 0);
            LogUtil.w(DownloadThread.class, "reDirectUrl = " + reDirectUrl);
            downloadInfo.setReDirectUrl(reDirectUrl);

            checkWhetherCanceled();
            connection = getConnection(downloadInfo.getReDirectUrl(), 0, 0);
            connection.connect();
            if(DownloadConfiger.LOG){
                printResponseHeader(connection);
            }

            int statusCode = connection.getResponseCode();

            //状态异常
            handleErrorHttpStatusCode(connection, statusCode);

            //文件类型
            downloadInfo.setMimeType(sanitizeMimeType(connection.getHeaderField("Content-Type")));

            //更新名字
            if(TextUtils.isEmpty(downloadInfo.getFileName())){
                downloadInfo.setFileName(Helpers.getFileName(downloadInfo.getReDirectUrl(),
                        connection.getHeaderField("Content-disposition"),
                        downloadInfo.getMimeType()));
            }
            if(downloadInfo.getCurrentBytes().getAllDownloadedBytes() < 1){
                String name = downloadInfo.getFileName();
                //检查是否有重名的下载任务
                List<DownloadInfo> ds = DownloadInfoDao.getInstace(context).queryDownloadInfos(
                        Where.create().eq(Downloads.ColumnFileName, name), null);
                int index = 0;
                while(!ds.isEmpty()){
                    index++;
                    ds = DownloadInfoDao.getInstace(context).queryDownloadInfos(
                            Where.create().eq(Downloads.ColumnFileName, name + "(" + index + ")"), null);
                }
                if(index > 0){
                    downloadInfo.setFileName(name + "(" + index + ")");
                }
                //删除已下载的文件
                new File(downloadInfo.getTempFilePath()).deleteOnExit();
            }

            //文件长度
            int contentLenght = connection.getContentLength();
            if(contentLenght != downloadInfo.getTotalBytes()){
                downloadInfo.getCurrentBytes().clear();
            }
            downloadInfo.setTotalBytes(contentLenght);

            if(downloadInfo.getTotalBytes() > DownloadConfiger.DOWNLOAD_MAX_BYTES_OVER_MOBILE){
                downloadInfo.setIsOnlyWifi(true);
                throw new StopRequest(DownloadInfo.StatusWaitingForWifi,
                        "wifi is unable");
            }

            //检查存储空间
            if(downloadInfo.getTotalBytes() > 0){
                long availableBytes = StorageUtil.getAvailableBytes(new File(downloadInfo.getFolderPath()));
                LogUtil.w(DownloadThread.class, "availableBytes = " + availableBytes + "  of  " + downloadInfo.getUrl());
                if(availableBytes < downloadInfo.getTotalBytes()){
                    throw new StopRequest(DownloadInfo.StatusFailedStorageNotEnough,
                            "the storage is not enough for " + downloadInfo.getTotalBytes() + "byte");
                }
            }else{
                //文件长度未知，单线程下载
                downloadInfo.setThreadNum(1);
                //清除下载记录，重新下载
                downloadInfo.getCurrentBytes().clear();
                //删除已下载的文件
                new File(downloadInfo.getTempFilePath()).deleteOnExit();
            }

            //更新ETag
            String headAcceptRanges = connection.getHeaderField("Accept-Ranges");
            String headEtag = (!TextUtils.isEmpty(headAcceptRanges) && contentLenght > 0)
                    ? connection.getHeaderField("ETag") : null;
            if(headEtag != null && !headEtag.equals(downloadInfo.getETag())){
                downloadInfo.setETag(headEtag);
            }else{
                //不支持断点续传，单线程下载
                downloadInfo.setThreadNum(1);
                //清除下载记录，重新下载
                downloadInfo.getCurrentBytes().clear();
                //删除已下载的文件
                new File(downloadInfo.getTempFilePath()).deleteOnExit();
            }

            //每个下载任务最大下载线程数
            if(downloadInfo.getCurrentBytes().getAllDownloadedBytes() < 1){
                if(downloadInfo.getThreadNum() > DownloadConfiger.MaxTreadNumPerTask){
                    downloadInfo.setThreadNum(DownloadConfiger.MaxTreadNumPerTask);
                }
            }

            //更新数据库
            DownloadInfoDao.getInstace(context).updateDownload(
                    UpdateCondition.create()
                            .addColumn(Downloads.ColumnReDirectUrl, downloadInfo.getReDirectUrl())
                            .addColumn(Downloads.ColumnMimeType, downloadInfo.getMimeType())
                            .addColumn(Downloads.ColumnFileName, downloadInfo.getFileName())
                            .addColumn(Downloads.ColumnTotalBytes, downloadInfo.getTotalBytes())
                            .addColumn(Downloads.ColumnCurrentBytes, downloadInfo.getCurrentBytes().toDbJsonString())
                            .addColumn(Downloads.ColumnIsOnlyWifi, downloadInfo.getIsOnlyWifi())
                            .addColumn(Downloads.ColumnThreadNum, downloadInfo.getThreadNum())
                            .addColumn(Downloads.ColumnETag, downloadInfo.getETag())
                            .setWhere(new Where().eq(Downloads.ColumnID, downloadInfo.getId())));
            downloadNotification.update(systemFacade, downloadInfo);

            //断开连接
            connection.disconnect();
            connection = null;

            //开始多线程下载
            DownloadTask task = null;
            for(int i = 0; i < downloadInfo.getThreadNum(); i++){
                task = new DownloadTask(downloadInfo, i);
                taskList.add(task);
                systemFacade.startThread(task);
            }
            while (!taskList.isEmpty()
                    && !isCanceled
                    && taskResults.size() < taskList.size()){
                synchronized (taskResults){
                    taskResults.wait();
                }
            }
            //分段下载完全
            if(!isCanceled){
                int status = DownloadInfo.StatusSuccessed;
                for(int i = 0; i < downloadInfo.getThreadNum(); i++){
                    int result = taskResults.get(i);
                    if(result != DownloadInfo.StatusSuccessed){
                        //如果有下载失败的，直接取第一个失败的错误状态吧
                        status = result;
                        break;
                    }
                }

                if(status == DownloadInfo.StatusSuccessed
                        && new File(downloadInfo.getTempFilePath())
                            .renameTo(new File(downloadInfo.getDestFilePath()))){
                    //noinspection ResourceType
                    downloadInfo.setStatus(status);
                }
            }

        } catch (MalformedURLException e) {
            LogUtil.printException(DownloadThread.class, e);
            downloadInfo.setStatus(DownloadInfo.StatusFailedOtherException);
        } catch (StopRequest e) {
            LogUtil.printException(DownloadThread.class, e);
            downloadInfo.setStatus(e.mFinalStatus);
        } catch (IOException e) {
            LogUtil.printException(DownloadThread.class, e);
            downloadInfo.setStatus(DownloadInfo.StatusFailedOtherException);
        } catch (Exception e) {
            LogUtil.printException(DownloadThread.class, e);
            downloadInfo.setStatus(DownloadInfo.StatusFailedOtherException);
        }finally {
            if(connection != null){
                connection.disconnect();
            }

            if(isCanceled && !downloadInfo.isDownloadSuccessed()){
                downloadInfo.setStatus(DownloadInfo.StatusFailedCanceled);
            }

            DownloadInfoDao.getInstace(context).updateDownload(
                    UpdateCondition.create()
                            .addColumn(Downloads.ColumnStatus, downloadInfo.getStatus())
                            .addColumn(Downloads.ColumnReDirectUrl, downloadInfo.getReDirectUrl())
                            .addColumn(Downloads.ColumnMimeType, downloadInfo.getMimeType())
                            .addColumn(Downloads.ColumnFileName, downloadInfo.getFileName())
                            .addColumn(Downloads.ColumnTotalBytes, downloadInfo.getTotalBytes())
                            .addColumn(Downloads.ColumnCurrentBytes, downloadInfo.getCurrentBytes().toDbJsonString())
                            .addColumn(Downloads.ColumnIsOnlyWifi, downloadInfo.getIsOnlyWifi())
                            .addColumn(Downloads.ColumnThreadNum, downloadInfo.getThreadNum())
                            .addColumn(Downloads.ColumnETag, downloadInfo.getETag())
                            .addColumn(Downloads.ColumnRetryAfterTime, downloadInfo.getRetryAfterTime())
                            .setWhere(new Where().eq(Downloads.ColumnID, downloadInfo.getId())));
            downloadNotification.update(systemFacade, downloadInfo);

            DownloadService.downloadThreadFinished(downloadInfo.getId());

            wakeLock.release();

            LogUtil.w(DownloadThread.class, "Thread finished : " + downloadInfo.getUrl());
        }
    }

    @Override
    public void cancel() {
        if(isCanceled){
            return;
        }

        isCanceled = true;

        if(!taskList.isEmpty()){
            for(DownloadTask t : taskList){
                t.cancel();
            }

            synchronized (taskResults){
                taskResults.notify();
            }
        }

        LogUtil.w(DownloadThread.class, "Thread canceled : " + downloadInfo.getUrl());
    }

    // --------------------- Methods public ----------------------


    // --------------------- Methods private ---------------------
    private void checkWhetherCanceled() throws StopRequest {
        if(isCanceled){
            throw new StopRequest(DownloadInfo.StatusFailedCanceled,
                    "task is canceled by user");
        }
    }

    private void checkConnectivity() throws StopRequest {
        if(systemFacade.getActiveNetworkType() == null){
            throw new StopRequest(DownloadInfo.StatusWaitingForNet,
                    "newwork is unable");
        }

        if((downloadInfo.getIsOnlyWifi() && !systemFacade.isWifiActive())
                || downloadInfo.getTotalBytes() > DownloadConfiger.DOWNLOAD_MAX_BYTES_OVER_MOBILE){
            downloadInfo.setIsOnlyWifi(true);
            throw new StopRequest(DownloadInfo.StatusWaitingForWifi,
                    "wifi is unable");
        }

        if(!downloadInfo.getIsAllowRoaming() && systemFacade.isNetworkRoaming()){
            throw new StopRequest(DownloadInfo.StatusFailedCannotUseRoaming,
                    "network is roaming");
        }
    }

    private HttpURLConnection getConnection(String urlStr, int rangeFrom, int rangeTo) throws StopRequest {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            if((rangeFrom != 0 || rangeTo != 0) && !TextUtils.isEmpty(downloadInfo.getETag())){
                conn.addRequestProperty("Range", "bytes=" + rangeFrom + "-" + rangeTo);
                conn.addRequestProperty("If-Range", downloadInfo.getETag());
            }
            return conn;
        } catch (MalformedURLException e) {
            LogUtil.printException(DownloadThread.class, e);
            throw new StopRequest(DownloadInfo.StatusFailedOtherException,
                    "failed to getConnection : " + e.toString());
        } catch (ProtocolException e) {
            throw new StopRequest(DownloadInfo.StatusFailedOtherException,
                    "failed to getConnection : " + e.toString());
        } catch (IOException e) {
            throw new StopRequest(DownloadInfo.StatusFailedOtherException,
                    "failed to getConnection : " + e.toString());
        }
    }

    private static void printResponseHeader(HttpURLConnection connection){
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0;; i++) {
            String mine = connection.getHeaderField(i);
            if (mine == null)
                break;
            header.put(connection.getHeaderFieldKey(i), mine);
        }
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            LogUtil.d(DownloadThread.class, "ResponseHeader " + key + " : " + entry.getValue());
        }
    }

    private boolean isErrorStatusCode(int statusCode){
        return statusCode >= 400;
    }

    private void handleErrorHttpStatusCode(HttpURLConnection conn, int statusCode) throws StopRequest {
        if(isErrorStatusCode(statusCode)){
            if(statusCode == 503){
                String retryAfter = conn.getHeaderField("Retry-After");
                if(!TextUtils.isEmpty(retryAfter)){
                    int seconds = 30;
                    try {
                        seconds = Integer.valueOf(retryAfter);
                        if(seconds < 30){
                            seconds = 30;
                        }
                    } catch (NumberFormatException e){
                        LogUtil.e(getClass(), e.toString());
                    }
                    downloadInfo.setRetryAfterTime(System.currentTimeMillis() + seconds*1000);
                    throw new StopRequest(DownloadInfo.StatusWaitingForRetry,
                            "retry after " + retryAfter);
                }
            }
            throw new StopRequest(DownloadInfo.StatusFailedUnExpectedStatusCode,
                    "error http status code : " + statusCode);
        }else if (statusCode >= 300 && statusCode < 400) {
            throw new StopRequest(DownloadInfo.StatusFailedRedirectError,
                    "http redirect error : " + statusCode);
        }
    }

    @NonNull
    private String getRedirectUrl(@NonNull String originalUrl, int redirectCount) throws StopRequest {
        if(redirectCount > 5){
            throw new StopRequest(DownloadInfo.StatusFailedRedirectError,
                    "too many redirects");
        }

        checkWhetherCanceled();

        HttpURLConnection conn = null;
        try {
            conn = getConnection(originalUrl, 0, 0);
            conn.connect();
            if(DownloadConfiger.LOG){
                printResponseHeader(conn);
            }

            int statusCode = conn.getResponseCode();
            if (statusCode == 301
                    || statusCode == 302
                    || statusCode == 303
                    || statusCode == 307) {
                String redirect = conn.getHeaderField("Location");
                if(URLUtil.isNetworkUrl(redirect)){
                    return getRedirectUrl(originalUrl, redirectCount + 1);
                }else{
                    throw new StopRequest(DownloadInfo.StatusFailedRedirectError,
                            "invalid Location : " + redirect);
                }
            }else{
                handleErrorHttpStatusCode(conn, statusCode);
                return originalUrl;
            }
        } catch (IOException e) {
            LogUtil.printException(DownloadThread.class, e);
            throw new StopRequest(DownloadInfo.StatusFailedOtherException,
                    "failed to connect");
        } finally {
            if(conn != null){
                conn.disconnect();
            }
        }
    }

    /**
     * Clean up a mimeType string so it can be used to dispatch an intent to
     * view a downloaded asset.
     *
     * @param mimeType either null or one or more mime types (semi colon separated).
     * @return null if mimeType was null. Otherwise a string which represents a
     * single mimetype in lowercase and with surrounding whitespaces
     * trimmed.
     */
    @Nullable
    private static String sanitizeMimeType(String mimeType) {
        if(mimeType == null){
            return "";
        }
        try {
            mimeType = mimeType.trim().toLowerCase(Locale.ENGLISH);

            final int semicolonIndex = mimeType.indexOf(';');
            if (semicolonIndex != -1) {
                mimeType = mimeType.substring(0, semicolonIndex);
            }
            return mimeType;
        } catch (Exception npe) {
            return "";
        }
    }

    private volatile int lastUpdateByte = 0;
    private volatile long lastUpdateTime = 0;
    private volatile int lastUpdateProgress = 0;
    /**
     * 更新数据库下载大小、广播通知
     */
    private void updateDbProgress(){
        synchronized (downloadInfo){
            long thisTime = System.currentTimeMillis();
            int thisByte = downloadInfo.getCurrentBytes().getAllDownloadedBytes();
            int thisProgress = (int) (downloadInfo.getProgress()*100);
            if(thisTime - lastUpdateTime >= 1000
                    || thisByte - lastUpdateByte > 128*1024
                    || thisProgress != lastUpdateProgress){
                //间隔1秒、下载128kb、进度+1/100
                //更新进度
                DownloadInfoDao.getInstace(context).updateDownload(
                        UpdateCondition.create()
                                .addColumn(Downloads.ColumnCurrentBytes, downloadInfo.getCurrentBytes().toDbJsonString())
                                .setWhere(new Where().eq(Downloads.ColumnID, downloadInfo.getId())));
                //更新Notification
                downloadNotification.update(systemFacade, downloadInfo);
            }
        }
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------

    private class StopRequest extends Throwable {
        private static final long serialVersionUID = 1L;

        @DownloadInfo.DownloadStatus
        public int mFinalStatus;

        public StopRequest(int finalStatus, String message) {
            super(message);
            mFinalStatus = finalStatus;
        }

        public StopRequest(int finalStatus, String message, Throwable throwable) {
            super(message, throwable);
            mFinalStatus = finalStatus;
        }
    }

    private class DownloadTask implements Runnable, DownloadService.Cancelable{

        private final DownloadInfo downloadInfo;
        private final int index;

        private HttpURLConnection connection = null;
        private InputStream inputStream;
        private RandomAccessFile randomAccessFile;
        private int curByte, endByte;

        public DownloadTask(@NonNull DownloadInfo downloadInfo,
                            int index) {
            this.downloadInfo = downloadInfo;
            this.index = index;
            try {
                randomAccessFile = new RandomAccessFile(downloadInfo.getTempFilePath(), "rw");
            } catch (FileNotFoundException e) {
                LogUtil.printException(getClass(), e);
            }
        }

        @Override
        public void run() {
            int status = DownloadInfo.StatusFailedUnknownError;

            try {
                checkWhetherCanceled();

                int singleLength = downloadInfo.getSingleThreadLength(); //maybe 0
                int sectionStart = singleLength * index;

                curByte = sectionStart + downloadInfo.getCurrentBytes().getSectionBytes(index);
                endByte = singleLength * (index+1);
                LogUtil.d(getClass(), "DownloadTask byteRange : " + curByte + " --> " + endByte);

                if(downloadInfo.getTotalBytes() < 1 || curByte < endByte){
                    connection = getConnection(downloadInfo.getReDirectUrl(), curByte, endByte);
                    connection.connect();

                    inputStream = connection.getInputStream();

                    byte[] buffer = new byte[4096];
                    int byteReaded = inputStream.read(buffer);
                    while (!isCanceled && byteReaded > 0){
                        randomAccessFile.seek(curByte);
                        randomAccessFile.write(buffer, 0, byteReaded);
                        curByte += byteReaded;
                        downloadInfo.getCurrentBytes().updateSectionBytes(index, curByte - sectionStart);
                        updateDbProgress();
                        byteReaded = inputStream.read(buffer);
                    }
                }

                if(!isCanceled){
                    status = DownloadInfo.StatusSuccessed;
                }

            } catch (StopRequest e){
                status = e.mFinalStatus;
                LogUtil.printException(getClass(), e);
            } catch (IOException e) {
                status = DownloadInfo.StatusFailedOtherException;
                LogUtil.printException(getClass(), e);
            } catch (Exception e){
                status = DownloadInfo.StatusFailedOtherException;
                LogUtil.printException(getClass(), e);
            } finally {
                if(inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(randomAccessFile != null){
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(connection != null){
                    connection.disconnect();
                }

                synchronized (taskResults){
                    taskResults.put(index, status);
                    taskResults.notify();
                }

                LogUtil.d(getClass(), "DownloadTask finish " + status);
            }

        }


        @Override
        public void cancel() {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(connection != null){
                connection.disconnect();
            }
        }

    }


    // --------------------- logical fragments -----------------

}
