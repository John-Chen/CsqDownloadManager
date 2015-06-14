/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.URLUtil;
import com.csq.downloadmanager.db.query.IWhere;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.db.update.UpdateCondition;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.DbUtil;
import com.csq.downloadmanager.util.Helpers;
import com.csq.downloadmanager.util.SqlUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadInfoDao {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    private volatile static Context app;
    private volatile static DownloadInfoDao instance;

    private DownloadInfoDao(Context context) {
        this.app = context.getApplicationContext();
        this.mResolver = this.app.getContentResolver();
    }

    public static DownloadInfoDao getInstace(Context context) {
        synchronized (DownloadInfoDao.class) {
            if (instance == null) {
                instance = new DownloadInfoDao(context);
            }
        }
        return instance;
    }

    private ContentResolver mResolver;

    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    /**
     * 开始下载
     * @param info 下载信息
     * @return DownloadInfo.id
     */
    public long startDownload(@NonNull DownloadInfo info)
            throws UrlInvalidError, UrlAlreadyDownloadError {
        Helpers.checkAccessPermission(app);

        if(!URLUtil.isNetworkUrl(info.getUrl())){
            throw new UrlInvalidError(info.getUrl());
        }
        if(!queryDownloadInfos(Where.create().eq(Downloads.ColumnUrl, info.getUrl()), null).isEmpty()){
            throw new UrlAlreadyDownloadError(info.getUrl());
        }
        info.setStatus(DownloadInfo.StatusWaitingForExecute);  //ensure status
        Uri downloadUri = mResolver.insert(Downloads.CONTENT_URI, info.toContentValues());
        long id = Long.parseLong(downloadUri.getLastPathSegment());
        info.setId(id);
        return id;
    }

    /**
     * 删除下载记录
     * @param ids 要删除的DownloadInfo.id
     * @return 删除的数量
     */
    public int deleteDownload(long... ids) {
        if(ids.length < 1){
            return 0;
        }
        return mResolver.delete(Downloads.CONTENT_URI,
                SqlUtils.getWhereClauseForIds(ids),
                SqlUtils.getWhereArgsForIds(ids));
    }

    /**
     * 更新下载记录
     * @param condition 更新字段及条件
     * @return 被更改的数量
     */
    public int updateDownload(@NonNull UpdateCondition condition){
        if(condition.getContentValues().size() < 1){
            return 0;
        }
        return mResolver.update(Downloads.CONTENT_URI,
                condition.getContentValues(),
                condition.getWhereClause(),
                condition.getWhereArgs());
    }

    /**
     * 更新下载记录
     * @return 被更改的数量
     */
    public int updateDownload(@NonNull DownloadInfo info){
        ContentValues cv = info.toContentValues();

        return mResolver.update(Downloads.CONTENT_URI,
                cv,
                Downloads.ColumnID + " = " + info.getId(),
                null);
    }

    /**
     * 暂停下载记录,只能暂停等待以及下载中的记录
     * @param ids 要暂停的DownloadInfo.id
     * @return 暂停的数量
     */
    public int pauseDownload(long... ids) throws UnExpectedStatus {
        if(ids.length < 1){
            return 0;
        }
        List<DownloadInfo> infos = queryDownloadInfos(new Where().in(Downloads.ColumnID, ids), null);
        if(infos == null || infos.isEmpty()){
            return 0;
        }
        for(DownloadInfo di : infos){
            if(!di.isWaiting() && !di.isDowning()){
                throw new UnExpectedStatus(
                        "Can only pause a waiting or downing downloadInfo: "
                                + " id = " + di.getId()
                                + " name = " + di.getFileName()
                                + " status = " + di.getStatus());
            }
        }

        return updateDownload(UpdateCondition.create()
                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusPaused)
                .setWhere(new Where().in(Downloads.ColumnID, ids)));
    }

    /**
     * 恢复下载记录，只有StatusPaused状态的下载记录才能调用此方法恢复
     * @param ids 要恢复的DownloadInfo.id
     * @return 恢复的数量
     */
    public int resumeDownload(long... ids) throws UnExpectedStatus {
        if(ids.length < 1){
            return 0;
        }
        List<DownloadInfo> infos = queryDownloadInfos(new Where().in(Downloads.ColumnID, ids), null);
        if(infos == null || infos.isEmpty()){
            return 0;
        }
        for(DownloadInfo di : infos){
            if(!di.isPaused()){
                throw new UnExpectedStatus(
                        "Can only resume a paused downloadInfo: "
                                + " id = " + di.getId()
                                + " name = " + di.getFileName()
                                + " status = " + di.getStatus());
            }
        }

        return updateDownload(UpdateCondition.create()
                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForExecute)
                .setWhere(new Where().in(Downloads.ColumnID, ids)));
    }

    /**
     * 重新下载失败的任务，只能重新下载下载失败的记录
     * @param ids 要重新下载的DownloadInfo.id
     * @return 重新下载的数量
     */
    public int restartDownload(long... ids) throws UnExpectedStatus {
        if(ids.length < 1){
            return 0;
        }
        List<DownloadInfo> infos = queryDownloadInfos(new Where().in(Downloads.ColumnID, ids), null);
        if(infos == null || infos.isEmpty()){
            return 0;
        }
        for(DownloadInfo di : infos){
            if(!di.isFailed()){
                throw new UnExpectedStatus(
                        "Can only reStart a failed downloadInfo: "
                                + " id = " + di.getId()
                                + " name = " + di.getFileName()
                                + " status = " + di.getStatus());
            }
        }

        return updateDownload(UpdateCondition.create()
                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForExecute)
                .setWhere(new Where().in(Downloads.ColumnID, ids)));
    }

    /**
     * 注意查询完成之后cursor.close();
     * @param where 查询条件
     * @param sortOrder 排序方式，{@link SqlUtils#getSortOrder(String, boolean)}
     */
    public Cursor query(@NonNull IWhere where, @Nullable String sortOrder){
        String[] pro = new String[Downloads.AllProjectionMap.size()];
        Downloads.AllProjectionMap.keySet().toArray(pro);
        return mResolver.query(Downloads.CONTENT_URI,
                pro,
                where.getSelection(),
                null,
                sortOrder
        );
    }


    /**
     * 查询下载记录
     * @param where 查询条件
     * @param sortOrder 排序方式，{@link SqlUtils#getSortOrder(String, boolean)}
     */
    @NonNull
    public List<DownloadInfo> queryDownloadInfos(@NonNull IWhere where, String sortOrder) {
        Cursor cursor = query(where, sortOrder);
        try {
            if(cursor == null || cursor.getCount() < 1){
                return Collections.EMPTY_LIST;
            }
            List<DownloadInfo> results = new ArrayList<DownloadInfo>(cursor.getCount());
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
                results.add(DbUtil.readEntity(cursor));
            }
            return results;
        } finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    @NonNull
    public List<DownloadInfo> queryAll(){
        String[] pro = new String[Downloads.AllProjectionMap.size()];
        Downloads.AllProjectionMap.keySet().toArray(pro);
        Cursor cursor = mResolver.query(Downloads.CONTENT_URI,
                pro,
                null,
                null,
                null);
        try {
            if(cursor == null || cursor.getCount() < 1){
                return Collections.EMPTY_LIST;
            }
            List<DownloadInfo> results = new ArrayList<DownloadInfo>(cursor.getCount());
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
                results.add(DbUtil.readEntity(cursor));
            }
            return results;
        } finally {
            if(cursor != null){
                cursor.close();
            }
        }
    }

    /**
     * 通过id查询下载信息
     * @param id 要查询的id值
     */
    @Nullable
    public DownloadInfo queryById(long id){
        List<DownloadInfo> list = queryDownloadInfos(new Where().eq(Downloads.ColumnID, id), null);
        if(list != null && !list.isEmpty()){
            return list.get(0);
        }
        return null;
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------

    public class UrlInvalidError extends Throwable {
        private static final long serialVersionUID = 1L;

        public UrlInvalidError(String url) {
            super("downloadInfo.url is invalid : " + url);
        }
    }

    public class UrlAlreadyDownloadError extends Throwable {
        private static final long serialVersionUID = 1L;

        public UrlAlreadyDownloadError(String url) {
            super("already have a valid download task of :(url) " + url);
        }
    }

    public class UnExpectedStatus extends Throwable {
        private static final long serialVersionUID = 1L;

        public UnExpectedStatus(String msg) {
            super(msg);
        }
    }

    // --------------------- logical fragments -----------------

}
