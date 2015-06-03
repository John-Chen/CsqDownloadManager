/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.service;

import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.URLUtil;
import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.configer.DownloadConfiger;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.db.update.UpdateCondition;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.Helpers;
import com.csq.downloadmanager.util.LogUtil;
import com.csq.downloadmanager.util.StorageUtil;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DownloadThread implements Runnable, DownloadService.Cancelable {


    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------
    public final Context context;
    public final DownloadInfo downloadInfo;
    public final SystemFacade systemFacade;
    public boolean isCanceled = false;

    private HttpURLConnection connection;

    // ----------------------- Constructors ----------------------

    public DownloadThread(@NonNull Context context,
                          @NonNull DownloadInfo downloadInfo,
                          @NonNull SystemFacade systemFacade) {
        this.context = context;
        this.downloadInfo = downloadInfo;
        this.systemFacade = systemFacade;
    }


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        int finalStatus = DownloadInfo.StatusFailedUnknownError;

        try {
            checkWhetherCanceled();

            //检测网络连接
            checkConnectivity();

            //检查存储卡是否挂载
            if(!StorageUtil.isStorageVolumeMounted(context, downloadInfo.getFolderPath())){
                throw new StopRequest(DownloadInfo.StatusFailedSdcardUnmounted,
                        "failed to getConnection");
            }

            //更新数据库下载状态为StatusDowning
            downloadInfo.setStatus(DownloadInfo.StatusDowning);
            DownloadInfoDao.getInstace(context).updateDownload(
                    UpdateCondition.create()
                            .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusDowning)
                            .setWhere(new Where().eq(Downloads.ColumnID, downloadInfo.getId())));

            //更新重定向地址，没有的话reDirectUrl=url
            downloadInfo.setReDirectUrl(getRedirectUrl(downloadInfo.getUrl(), 0));

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
                if(StorageUtil.getAvailableBytes(new File(downloadInfo.getFolderPath())) < downloadInfo.getTotalBytes()){
                    throw new StopRequest(DownloadInfo.StatusFailedStorageNotEnough,
                            "the storage is not enough for " + downloadInfo.getTotalBytes() + "byte");
                }
            }else{
                //文件长度未知，单线程下载
                downloadInfo.setThreadNum(1);
                //清除下载记录，重新下载
                downloadInfo.getCurrentBytes().clear();
                //删除已下载的文件
                new File(downloadInfo.getTempFilePath(0)).deleteOnExit();
            }

            //更新ETag
            String headAcceptRanges = connection.getHeaderField("Accept-Ranges");
            int headContentLength = connection.getContentLength();
            String headEtag = (!TextUtils.isEmpty(headAcceptRanges) && headContentLength > 0)
                    ? connection.getHeaderField("ETag") : "";
            if(headEtag != null && !headEtag.equals(downloadInfo.getETag())){
                downloadInfo.setETag(headEtag);
            }else{
                //不支持断点续传，单线程下载
                downloadInfo.setThreadNum(1);
                //清除下载记录，重新下载
                downloadInfo.getCurrentBytes().clear();
                //删除已下载的文件
                new File(downloadInfo.getTempFilePath(0)).deleteOnExit();
            }

            //更新数据库
            DownloadInfoDao.getInstace(context).updateDownload(downloadInfo);

            //断开连接
            connection.disconnect();
            connection = null;

            //开始多线程下载


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

            DownloadInfoDao.getInstace(context).updateDownload(downloadInfo);
        }

    }

    @Override
    public void cancel() {
        isCanceled = true;
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


    // --------------------- logical fragments -----------------

}
