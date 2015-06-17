/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.csq.downloadmanager.configer.DownloadConfiger;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.DbUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Entity mapped to table DOWNLOAD_INFO.
 */
public class DownloadInfo implements java.io.Serializable {

    //等待下载状态[0,10)
    public static final int StatusWaitingForExecute = 0;      //初始状态，还未被查询到并开始下载;或者当前下载的任务数已经饱和，等待正在下载的任务结束
    public static final int StatusWaitingForNet = 1;          //当前网络断开，等待网络
    public static final int StatusWaitingForWifi = 2;         //仅在wifi下才能下载，等待wifi
    public static final int StatusWaitingForRetry = 3;        //等待再次尝试下载，或者Retry-After，优先级低于StatusWaitingForExecute
    //正常下载状态[10,20)
    public static final int StatusDowning = 10;               //下载中
    public static final int StatusPaused = 11;                //暂停
    //下载成功[20,30)
    public static final int StatusSuccessed = 20;               //下载合并完成
    //下载失败[30,)
    public static final int StatusFailedUnknownError = 30;      //未知异常
    public static final int StatusFailedCanceled = 31;          //已经取消下载
    public static final int StatusFailedSdcardUnmounted = 32;   //存储卡未挂载
    public static final int StatusFailedStorageNotEnough = 33;  //存储空间不足
    public static final int StatusFailedCannotUseRoaming = 34;  //不能漫游
    public static final int StatusFailedRedirectError = 35;     //重定向过多或异常
    public static final int StatusFailedUnExpectedStatusCode = 36; //状态码>=400
    public static final int StatusFailedPermissionDenied = 37;  //权限异常
    public static final int StatusFailedOtherException = 38;    //其他异常,IOException、MalformedURLException......

    @IntDef({StatusWaitingForExecute,
            StatusWaitingForNet,
            StatusWaitingForWifi,
            StatusWaitingForRetry,
            StatusDowning,
            StatusPaused,
            StatusSuccessed,
            StatusFailedUnknownError,
            StatusFailedCanceled,
            StatusFailedSdcardUnmounted,
            StatusFailedStorageNotEnough,
            StatusFailedCannotUseRoaming,
            StatusFailedRedirectError,
            StatusFailedUnExpectedStatusCode,
            StatusFailedPermissionDenied,
            StatusFailedOtherException,
    })
    public @interface DownloadStatus {
    }


    /**
     * 自增id
     */
    private long id;
    /**
     * 保存的文件名, eg:abc.txt, 文件绝对路径：folderPath + “/” + fileName
     */
    private String fileName;
    /**
     * 任务描述
     */
    private String description;
    /**
     * 下载地址, Not-null value
     */
    private String url;
    /**
     * 保存文件夹路径, eg:/mnt/sdcard/CsqDownload
     */
    private String folderPath;
    /**
     * 下载任务线程数, 也是文件下载分段数, <= DownloadConfiger.MaxTreadNumPerTask
     */
    private int threadNum = DownloadConfiger.DefaultTreadNumPerTask;
    /**
     * 可以为下载任务分组，并根据组名groupName下载及查询，eg：包名
     */
    private String groupName;
    /**
     * 下载时是否在通知栏显示下载进度
     */
    private boolean isShowNotification = true;
    /**
     * 是否仅在wifi环境下下载
     */
    private boolean isOnlyWifi = false;
    /**
     * 是否允许在漫游的情况下下载
     */
    private boolean isAllowRoaming = false;
    /**
     * 文件类型
     */
    private String mimeType;
    /**
     * 文件总大小
     */
    private int totalBytes;
    /**
     * 当前下载的各段文件大小
     */
    private final DownloadedBytes currentBytes = new DownloadedBytes();
    /**
     * 重定位的下载地址, 可能与{@link #url 相同}
     */
    private String reDirectUrl;
    /**
     * ETag信息，如果Content-Length且Accept-Ranges且ETag均非空，则更新此字段，可以用来判定是否支持断点续传
     */
    private String eTag;
    /**
     * 上次更新时间
     */
    private long lastModifyTime;
    /**
     * 下载状态
     */
    @DownloadStatus
    private int status;
    /**
     * 失败下载次数
     */
    public int numFailed;
    /**
     * 再次尝试的时间点
     */
    public long retryAfterTime;


    public DownloadInfo(@NonNull String url) {
        this.url = url;
        this.folderPath = DownloadConfiger.getDefaultDownloadPath();
        this.status = StatusWaitingForExecute;
        this.lastModifyTime = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public DownloadInfo setId(long id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    public DownloadInfo setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public DownloadInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public DownloadInfo setUrl(@NonNull String url) {
        this.url = url;
        return this;
    }

    @NonNull
    public String getFolderPath() {
        if(folderPath == null){
            folderPath = DownloadConfiger.getDefaultDownloadPath();
        }
        return folderPath;
    }

    public DownloadInfo setFolderPath(String folderPath) {
        this.folderPath = folderPath;
        return this;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public DownloadInfo setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }

    @NonNull
    public String getGroupName() {
        if(groupName == null){
            groupName = "";
        }
        return groupName;
    }

    public DownloadInfo setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public boolean getIsShowNotification() {
        return isShowNotification;
    }

    public DownloadInfo setIsShowNotification(boolean isShowNotification) {
        this.isShowNotification = isShowNotification;
        return this;
    }

    public boolean getIsOnlyWifi() {
        return isOnlyWifi;
    }

    public DownloadInfo setIsOnlyWifi(boolean isOnlyWifi) {
        this.isOnlyWifi = isOnlyWifi;
        return this;
    }

    public boolean getIsAllowRoaming() {
        return isAllowRoaming;
    }

    public DownloadInfo setIsAllowRoaming(boolean isAllowRoaming) {
        this.isAllowRoaming = isAllowRoaming;
        return this;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public DownloadInfo setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public DownloadInfo setTotalBytes(int totalBytes) {
        this.totalBytes = totalBytes;
        return this;
    }

    public DownloadInfo setCurrentBytes(String currentBytesDbJson) {
        this.currentBytes.update(currentBytesDbJson);
        return this;
    }

    public DownloadedBytes getCurrentBytes() {
        return currentBytes;
    }

    public int getSingleThreadLength(){
        return getTotalBytes()/getThreadNum();
    }

    public float getProgress(){
        if(totalBytes == 0){
            return 0;
        }
        return (float)currentBytes.getAllDownloadedBytes()/totalBytes;
    }

    @Nullable
    public String getReDirectUrl() {
        return reDirectUrl;
    }

    public DownloadInfo setReDirectUrl(String reDirectUrl) {
        this.reDirectUrl = reDirectUrl;
        return this;
    }

    @Nullable
    public String getETag() {
        return eTag;
    }

    public DownloadInfo setETag(String eTag) {
        this.eTag = eTag;
        return this;
    }

    public long getLastModifyTime() {
        return lastModifyTime;
    }

    public DownloadInfo setLastModifyTime(long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
        return this;
    }

    @DownloadStatus
    public int getStatus() {
        return status;
    }

    public DownloadInfo setStatus(@DownloadStatus int status) {
        this.status = status;
        return this;
    }

    public int getNumFailed() {
        return numFailed;
    }

    public DownloadInfo setNumFailed(int numFailed) {
        this.numFailed = numFailed;
        return this;
    }

    public long getRetryAfterTime() {
        return retryAfterTime;
    }

    public DownloadInfo setRetryAfterTime(long retryAfterTime) {
        this.retryAfterTime = retryAfterTime;
        return this;
    }

    public boolean isWaiting(){
        return status < 10;
    }

    public boolean isDowning(){
        return status == StatusDowning;
    }

    public boolean isPaused(){
        return status == StatusPaused;
    }

    public boolean isSuccessed(){
        return status == StatusSuccessed;
    }

    public boolean isFailed(){
        return status >= StatusFailedUnknownError;
    }

    public boolean isFinished(){
        return status >= StatusSuccessed;
    }

    public boolean isDownloadSuccessed(){
        return status == StatusSuccessed ||
                (totalBytes > 0 && currentBytes.getAllDownloadedBytes() >= totalBytes);
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        DbUtil.putIfNonNull(cv, Downloads.ColumnFileName, fileName);
        DbUtil.putIfNonNull(cv, Downloads.ColumnDescription, description);
        DbUtil.putIfNonNull(cv, Downloads.ColumnUrl, url);
        DbUtil.putIfNonNull(cv, Downloads.ColumnFolderPath, folderPath);
        DbUtil.putIfNonNull(cv, Downloads.ColumnThreadNum, threadNum);
        DbUtil.putIfNonNull(cv, Downloads.ColumnGroupName, groupName);
        DbUtil.putIfNonNull(cv, Downloads.ColumnIsShowNotification, isShowNotification);
        DbUtil.putIfNonNull(cv, Downloads.ColumnIsOnlyWifi, isOnlyWifi);
        DbUtil.putIfNonNull(cv, Downloads.ColumnIsAllowRoaming, isAllowRoaming);
        DbUtil.putIfNonNull(cv, Downloads.ColumnMimeType, mimeType);
        DbUtil.putIfNonNull(cv, Downloads.ColumnTotalBytes, totalBytes);
        DbUtil.putIfNonNull(cv, Downloads.ColumnCurrentBytes, currentBytes.toDbJsonString());
        DbUtil.putIfNonNull(cv, Downloads.ColumnReDirectUrl, reDirectUrl);
        DbUtil.putIfNonNull(cv, Downloads.ColumnETag, eTag);
        DbUtil.putIfNonNull(cv, Downloads.ColumnLastModifyTime, lastModifyTime);
        DbUtil.putIfNonNull(cv, Downloads.ColumnStatus, status);
        DbUtil.putIfNonNull(cv, Downloads.ColumnNumFailed, numFailed);
        DbUtil.putIfNonNull(cv, Downloads.ColumnRetryAfterTime, retryAfterTime);
        return cv;
    }

    /**
     * 接收到EventDispatcher下发的数据改变事件后，将改变的数据更新到某个DownloadInfo
     * {@link com.csq.downloadmanager.dispatcher.EventDispatcher#downloadInfoChanged(Context,long[],ContentValues)}
     * @param updatedContentValues 收到的EventValueUpdatedContentValues事件数据
     */
    public void updateByUpdatedContentValues(ContentValues updatedContentValues){
        if(updatedContentValues.containsKey(Downloads.ColumnFileName)){
            fileName = updatedContentValues.getAsString(Downloads.ColumnFileName);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnDescription)){
            description = updatedContentValues.getAsString(Downloads.ColumnDescription);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnFolderPath)){
            folderPath = updatedContentValues.getAsString(Downloads.ColumnFolderPath);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnGroupName)){
            groupName = updatedContentValues.getAsString(Downloads.ColumnGroupName);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnIsShowNotification)){
            isShowNotification = updatedContentValues.getAsBoolean(Downloads.ColumnIsShowNotification);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnIsOnlyWifi)){
            isOnlyWifi = updatedContentValues.getAsBoolean(Downloads.ColumnIsOnlyWifi);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnIsAllowRoaming)){
            isAllowRoaming = updatedContentValues.getAsBoolean(Downloads.ColumnIsAllowRoaming);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnMimeType)){
            mimeType = updatedContentValues.getAsString(Downloads.ColumnMimeType);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnTotalBytes)){
            totalBytes = updatedContentValues.getAsInteger(Downloads.ColumnTotalBytes);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnCurrentBytes)){
            currentBytes.update(updatedContentValues.getAsString(Downloads.ColumnCurrentBytes));
        }

        if(updatedContentValues.containsKey(Downloads.ColumnLastModifyTime)){
            lastModifyTime = updatedContentValues.getAsLong(Downloads.ColumnLastModifyTime);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnStatus)){
            //noinspection ResourceType
            status = updatedContentValues.getAsInteger(Downloads.ColumnStatus);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnRetryAfterTime)){
            retryAfterTime = updatedContentValues.getAsLong(Downloads.ColumnRetryAfterTime);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnThreadNum)){
            threadNum = updatedContentValues.getAsInteger(Downloads.ColumnThreadNum);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnETag)){
            eTag = updatedContentValues.getAsString(Downloads.ColumnETag);
        }

        if(updatedContentValues.containsKey(Downloads.ColumnNumFailed)){
            numFailed = updatedContentValues.getAsInteger(Downloads.ColumnNumFailed);
        }
    }

    /**
     * 返回下载中的文件路径，eg:/mnt/sdcard/CsqDownload/.a.txt
     * 下载成功之后重命名为{@link #getDestFilePath()}
     */
    public String getTempFilePath(){
        return getFolderPath() + File.separator + "." + fileName;
    }

    /**
     * 返回临时下载某段文件路径，eg:/mnt/sdcard/CsqDownload/a.txt
     */
    public String getDestFilePath(){
        return getFolderPath() + File.separator + fileName;
    }

    @Nullable
    public Intent getMimeTypeHandleIntent(Context context){
        if(TextUtils.isEmpty(mimeType)){
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        PackageManager pm = context.getPackageManager();
        intent.setDataAndType(Uri.fromFile(new File(getDestFilePath())), mimeType);
        ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if(ri != null) {
            return intent;
        }
        return null;
    }

    public class DownloadedBytes{

        public JSONObject allBytes = new JSONObject();

        public synchronized void update(@Nullable String dbJsonString){
            allBytes = new JSONObject();
            if(!TextUtils.isEmpty(dbJsonString)){
                JSONObject jo = null;
                try {
                    jo = new JSONObject(dbJsonString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(jo != null){
                    String key = null;
                    for(int i = 0; i < threadNum; i++){
                        key = getIndexKey(i);
                        try {
                            allBytes.put(key, jo.optInt(key, 0));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        public void clear(){
            update(null);
        }

        public synchronized void updateSectionBytes(int index, int bytes){
            try {
                allBytes.put(getIndexKey(index), bytes);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public synchronized int getSectionBytes(int index){
            return allBytes.optInt(getIndexKey(index), 0);
        }

        public synchronized int getAllDownloadedBytes(){
            int all = 0;
            for(int i = 0; i < threadNum; i++){
                all += allBytes.optInt(getIndexKey(i), 0);
            }
            return all;
        }

        /**
         * 通过json保存
         */
        public synchronized String toDbJsonString(){
            return allBytes.toString();
        }

        private String getIndexKey(int index){
            return index + "-" + threadNum;
        }

    }

}
