/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.configer.DownloadConfiger;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.db.query.AndWhere;
import com.csq.downloadmanager.db.query.IWhere;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.db.update.UpdateCondition;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.LogUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadService extends Service {


    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    /**
     * 监听数据库改变
     */
    private DownloadManagerContentObserver mObserver;

    /**
     * 是否在检测数据库更新信息
     */
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    /**
     * 是否有更新请求
     */
    private final AtomicBoolean isHaveUpdateRequest = new AtomicBoolean(false);
    /**
     * 保持一个更新线程
     */
    private final ExecutorService mUpdateExecutorService = Executors.newSingleThreadExecutor();

    private SystemFacade mSystemFacade;
    private DownloadInfoDao dao;


    /**
     * 正在下载的任务，key--DownloadInfo.id
     */
    private static final ConcurrentHashMap<Long, DownloadThread> downingThreads = new ConcurrentHashMap<>(4);


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException(
                "Cannot bind to DownloadService");
    }

    /**
     * Initializes the service when it is first created
     */
    public void onCreate() {
        super.onCreate();
        LogUtil.d(DownloadService.class, "Service onCreate");

        if (mSystemFacade == null) {
            mSystemFacade = new SystemFacade(this);
        }

        mObserver = new DownloadManagerContentObserver();
        getContentResolver().registerContentObserver(
                Downloads.CONTENT_URI, true, mObserver);

        dao = DownloadInfoDao.getInstace(getApplication());

        mSystemFacade.cancelAllNotifications();

        updateFromProvider();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        LogUtil.d(DownloadService.class, "Service onStart");
        updateFromProvider();
        return returnValue;
    }

    /**
     * Cleans up when the service is destroyed
     */
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
        LogUtil.d(DownloadService.class, "Service onDestroy");
        super.onDestroy();
    }

    // --------------------- Methods public ----------------------

    public static void startService(Context context){
        context.startService(new Intent(context, DownloadService.class));
    }

    public static DownloadThread downloadThreadFinished(long downloadId){
        return downingThreads.remove(downloadId);
    }


    // --------------------- Methods private ---------------------

    /**
     * Parses data from the content provider into private array
     */
    private void updateFromProvider() {
        if(isUpdating.get()){
            isHaveUpdateRequest.set(true);
            return;
        }
        mUpdateExecutorService.execute(new UpdateThread());
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------

    /**
     * Receives notifications when the data in the content provider changes
     */
    private class DownloadManagerContentObserver extends ContentObserver {

        public DownloadManagerContentObserver() {
            super(new Handler());
        }

        /**
         * Receives notification when the data in the observed content provider
         * changes.
         */
        public void onChange(final boolean selfChange) {
            LogUtil.d(DownloadService.class, "Service ContentObserver received notification");
            updateFromProvider();
        }

    }

    private class UpdateThread implements Runnable {
        @Override
        public void run() {
            LogUtil.d(DownloadService.class, "UpdateThread start");
            isUpdating.set(true);
            isHaveUpdateRequest.set(false);

            try {
                //删除或暂停的
                synchronized (downingThreads){
                    if(!downingThreads.isEmpty()){
                        List<DownloadInfo> downingDbs = dao.queryDownloadInfos(Where.create().in(Downloads.ColumnID, downingThreads.keySet()), null);
                        for(Map.Entry<Long, DownloadThread> entry : downingThreads.entrySet()){
                            DownloadInfo db = null;
                            for(DownloadInfo di : downingDbs){
                                if(di.getId() == entry.getKey()){
                                    db = di;
                                    break;
                                }
                            }
                            if(db == null){
                                //deleted, cancel Thread
                                entry.getValue().cancel();
                            }else{
                                if(db.isPaused()){
                                    entry.getValue().cancel();
                                }
                            }
                        }
                    }
                }

                boolean isHaveWaiting = false;
                if(downingThreads.size() < DownloadConfiger.MaxDownloadingTaskNum){
                    //StatusDowning、StatusWaitingForExecute、StatusWaitingForNet、StatusWaitingForWifi、StatusWaitingForRetry
                    List<Integer> canExcuteStatus = new LinkedList<>();
                    canExcuteStatus.add(DownloadInfo.StatusDowning);
                    canExcuteStatus.add(DownloadInfo.StatusWaitingForExecute);
                    canExcuteStatus.add(DownloadInfo.StatusWaitingForNet);
                    canExcuteStatus.add(DownloadInfo.StatusWaitingForWifi);
                    canExcuteStatus.add(DownloadInfo.StatusWaitingForRetry);
                    IWhere where = null;
                    if(downingThreads.isEmpty()){
                        where = Where.create().in(Downloads.ColumnStatus, canExcuteStatus);
                    }else{
                        where = AndWhere.create()
                                .and(Where.create().in(Downloads.ColumnStatus, canExcuteStatus))
                                .and(Where.create().notIn(Downloads.ColumnID, downingThreads.keySet()));
                    }
                    //downingThreads不包含的需要下载的任务
                    List<DownloadInfo> downloadInfos = dao.queryDownloadInfos(where, null);

                    if(!downloadInfos.isEmpty()){
                        boolean isNetworkConnected = mSystemFacade.isNetworkConnected();
                        boolean isWifiActive = mSystemFacade.isWifiActive();
                        while (downingThreads.size() < DownloadConfiger.MaxDownloadingTaskNum && !canExcuteStatus.isEmpty()){
                            Integer status = canExcuteStatus.remove(0);
                            if(status == DownloadInfo.StatusWaitingForNet && !isNetworkConnected){
                                isHaveWaiting = true;
                                continue;
                            }
                            if(status == DownloadInfo.StatusWaitingForWifi && !isWifiActive){
                                isHaveWaiting = true;
                                continue;
                            }

                            for(DownloadInfo di : downloadInfos){
                                //noinspection ResourceType
                                if(di.getStatus() != status){
                                    continue;
                                }
                                if(di.getStatus() == DownloadInfo.StatusDowning){
                                    if(!isNetworkConnected){
                                        dao.updateDownload(UpdateCondition.create()
                                                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForNet)
                                                .setWhere(Where.create().eq(Downloads.ColumnID, di.getId())));
                                        isHaveWaiting = true;
                                        continue;
                                    }else if(di.getIsOnlyWifi() && !isWifiActive){
                                        dao.updateDownload(UpdateCondition.create()
                                                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForWifi)
                                                .setWhere(Where.create().eq(Downloads.ColumnID, di.getId())));
                                        isHaveWaiting = true;
                                        continue;
                                    }
                                }


                                synchronized (downingThreads){
                                    if(downingThreads.size() >= DownloadConfiger.MaxDownloadingTaskNum){
                                        break;
                                    }
                                    //noinspection ResourceType
                                    if(di.getStatus() == status && !downingThreads.containsKey(di.getId())){
                                        DownloadThread dt = new DownloadThread(getApplication(), di, mSystemFacade);
                                        mSystemFacade.startThread(dt);
                                        downingThreads.put(di.getId(), dt);
                                        LogUtil.d(DownloadService.class, "UpdateThread start thread : " + di.getUrl());
                                    }
                                }
                            }
                        }
                    }
                }

                if(downingThreads.isEmpty() && !isHaveWaiting){
                    //没有等待网络或正在下载的任务，可以停止服务
                    stopSelf();
                }
            } catch (Exception e){
                LogUtil.e(getClass(), e.toString());
            } finally {
                LogUtil.d(DownloadService.class, "UpdateThread finish");
                isUpdating.set(false);
                if(isHaveUpdateRequest.get()){
                    updateFromProvider();
                }
            }
        }
    }

    public interface Cancelable{
        void cancel();
    }

    // --------------------- logical fragments -----------------

}
