/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.service.DownloadService;

public class DownloadManager {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    private volatile static DownloadManager instance;

    private DownloadManager(Context context) {
        app = context.getApplicationContext();
        dao = DownloadInfoDao.getInstace(context);
    }

    public static DownloadManager getInstace(Context context) {
        synchronized (DownloadManager.class) {
            if (instance == null) {
                instance = new DownloadManager(context);
            }
        }
        return instance;
    }

    private Context app;
    private DownloadInfoDao dao;

    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    /**
     * 开始下载
     * @param info 下载信息
     * @return DownloadInfo.id
     */
    public long startDownload(@NonNull DownloadInfo info)
            throws DownloadInfoDao.UrlInvalidError, DownloadInfoDao.UrlAlreadyDownloadError {
        long id = dao.startDownload(info);
        if(id > 0){
            startService(app);
        }
        return id;
    }

    /**
     * 删除下载记录
     * @param ids 要删除的DownloadInfo.id
     * @return 删除的数量
     */
    public int deleteDownload(long... ids) {
        int num = dao.deleteDownload(ids);
        if(num > 0){
            startService(app);
        }
        return num;
    }

    /**
     * 暂停下载记录,只能暂停等待以及下载中的记录
     * @param ids 要暂停的DownloadInfo.id
     * @return 暂停的数量
     */
    public int pauseDownload(long... ids) throws DownloadInfoDao.UnExpectedStatus {
        int num = dao.pauseDownload(ids);
        if(num > 0){
            startService(app);
        }
        return num;
    }

    /**
     * 恢复下载记录，只有StatusPaused状态的下载记录才能调用此方法恢复
     * @param ids 要恢复的DownloadInfo.id
     * @return 恢复的数量
     */
    public int resumeDownload(long... ids) throws DownloadInfoDao.UnExpectedStatus {
        int num = dao.resumeDownload(ids);
        if(num > 0){
            startService(app);
        }
        return num;
    }

    /**
     * 重新下载失败的任务，只能重新下载下载失败的记录
     * @param ids 要重新下载的DownloadInfo.id
     * @return 重新下载的数量
     */
    public int restartDownload(long... ids) throws DownloadInfoDao.UnExpectedStatus {
        int num = dao.restartDownload(ids);
        if(num > 0){
            startService(app);
        }
        return num;
    }

    // --------------------- Methods private ---------------------

    public static void startService(Context context){
        context.startService(new Intent(context, DownloadService.class));
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
