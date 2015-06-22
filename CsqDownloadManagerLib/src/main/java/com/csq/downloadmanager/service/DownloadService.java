/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.DownloadConfiger;
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

    private NetworkConnectionChangeReceiver mNetReceiver;

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

        dao = DownloadInfoDao.getInstace(getApplication());

        mSystemFacade.cancelAllNotifications();

        mNetReceiver = new NetworkConnectionChangeReceiver();
        mNetReceiver.regist(this);

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
        LogUtil.d(DownloadService.class, "Service onDestroy");
        mNetReceiver.unRegist(this);

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

    private class UpdateThread implements Runnable {
        @Override
        public void run() {
            LogUtil.d(DownloadService.class, "UpdateThread start");
            isUpdating.set(true);
            isHaveUpdateRequest.set(false);

            try {
                //检测正在下载的任务是否已删除或暂停，或者网络改变需要等待或取消
                if(!downingThreads.isEmpty()){
                    List<DownloadInfo> downingDbs = dao.queryDownloadInfos(Where.create().in(Downloads.ColumnID, downingThreads.keySet().toArray()), null);
                    for(Map.Entry<Long, DownloadThread> entry : downingThreads.entrySet()){
                        DownloadInfo db = null;
                        for(DownloadInfo di : downingDbs){
                            if(di.getId() == entry.getKey()){
                                db = di;
                                break;
                            }
                        }
                        if(db == null){
                            //已删除，deleted, cancel Thread
                            if(!entry.getValue().isCanceled()){
                                entry.getValue().cancel();
                            }

                        }else{
                            if(db.isPaused()){
                                //暂停
                                if(!entry.getValue().isCanceled()){
                                    entry.getValue().cancel();
                                }

                            }else if(!mNetReceiver.isNetworkConnected){
                                //网络连接 --> 断开，等待
                                dao.updateDownload(UpdateCondition.create()
                                        .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForNet)
                                        .setWhere(Where.create().eq(Downloads.ColumnID, db.getId())));
                                if(!entry.getValue().isCanceled()){
                                    entry.getValue().cancel();
                                }

                            }else if(db.getIsOnlyWifi() && !mNetReceiver.isWifiActive){
                                //wifi --> 非wifi状态，等待
                                dao.updateDownload(UpdateCondition.create()
                                        .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForWifi)
                                        .setWhere(Where.create().eq(Downloads.ColumnID, db.getId())));
                                if(!entry.getValue().isCanceled()){
                                    entry.getValue().cancel();
                                }

                            }else if(!db.getIsAllowRoaming() && mNetReceiver.isNetworkRoaming){
                                //非漫游 --> 漫游状态，取消下载
                                dao.updateDownload(UpdateCondition.create()
                                        .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusFailedCannotUseRoaming)
                                        .setWhere(Where.create().eq(Downloads.ColumnID, db.getId())));
                                if(!entry.getValue().isCanceled()){
                                    entry.getValue().cancel();
                                }
                            }
                        }
                    }
                }

                //downingThreads中没有而状态为StatusDowning的全部状态改为StatusWaitingForExecute/StatusWaitingForNet
                Where whereIng = Where.create().eq(Downloads.ColumnStatus, DownloadInfo.StatusDowning);
                if(!downingThreads.isEmpty()){
                    whereIng.notIn(Downloads.ColumnID, downingThreads.keySet());
                }
                int statusTo = DownloadInfo.StatusWaitingForExecute;
                if(!mNetReceiver.isNetworkConnected){
                    statusTo = DownloadInfo.StatusWaitingForNet;
                }
                dao.updateDownload(UpdateCondition.create()
                        .addColumn(Downloads.ColumnStatus, statusTo)
                        .setWhere(whereIng));

                if(mNetReceiver.isNetworkConnected){
                    //网络连接，所有StatusWaitingForNet的状态改为StatusWaitingForExecute
                    Where whereNet = Where.create().eq(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForNet);
                    if(!downingThreads.isEmpty()){
                        whereNet.notIn(Downloads.ColumnID, downingThreads.keySet());
                    }
                    dao.updateDownload(UpdateCondition.create()
                            .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForExecute)
                            .setWhere(whereNet));

                    if(mNetReceiver.isWifiActive){
                        //wifi连接，所有StatusWaitingForWifi的状态改为StatusWaitingForExecute
                        Where where = Where.create().eq(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForWifi);
                        if(!downingThreads.isEmpty()){
                            where.notIn(Downloads.ColumnID, downingThreads.keySet());
                        }
                        dao.updateDownload(UpdateCondition.create()
                                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForExecute)
                                .setWhere(where));
                    }else{
                        //wifi断开，所有StatusWaitingForExecute、StatusWaitingForRetry且只能在wifi下下载的，状态改为StatusWaitingForWifi
                        Where where1 = Where.create().in(Downloads.ColumnStatus,
                                new Integer[]{DownloadInfo.StatusWaitingForExecute,
                                        DownloadInfo.StatusWaitingForRetry});
                        if(!downingThreads.isEmpty()){
                            where1.notIn(Downloads.ColumnID, downingThreads.keySet());
                        }
                        Where where2 = Where.create().eq(Downloads.ColumnIsOnlyWifi, true);
                        dao.updateDownload(UpdateCondition.create()
                                .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForWifi)
                                .setWhere(AndWhere.create().and(where1, where2)));
                    }

                }else{
                    //网络断开，所有StatusWaitingForExecute、StatusWaitingForWifi、StatusWaitingForRetry改为StatusWaitingForNet
                    Where where = Where.create().in(Downloads.ColumnStatus,
                            new Integer[]{DownloadInfo.StatusWaitingForExecute,
                                    DownloadInfo.StatusWaitingForWifi,
                                    DownloadInfo.StatusWaitingForRetry});
                    if(!downingThreads.isEmpty()){
                        where.notIn(Downloads.ColumnID, downingThreads.keySet());
                    }
                    dao.updateDownload(UpdateCondition.create()
                            .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusWaitingForNet)
                            .setWhere(where));
                }

                if(mNetReceiver.isNetworkConnected
                        && downingThreads.size() < DownloadConfiger.MaxDownloadingTaskNum){
                    List<Integer> canExcuteStatus = new LinkedList<>();
                    canExcuteStatus.add(DownloadInfo.StatusWaitingForExecute);
                    canExcuteStatus.add(DownloadInfo.StatusWaitingForRetry);
                    IWhere where = null;
                    if(downingThreads.isEmpty()){
                        where = Where.create().in(Downloads.ColumnStatus, canExcuteStatus);
                    }else{
                        where = Where.create().in(Downloads.ColumnStatus, canExcuteStatus)
                                .notIn(Downloads.ColumnID, downingThreads.keySet());
                    }
                    //downingThreads不包含的需要下载的任务
                    List<DownloadInfo> downloadInfos = dao.queryDownloadInfos(where, null);

                    if(!downloadInfos.isEmpty()){
                        while (downingThreads.size() < DownloadConfiger.MaxDownloadingTaskNum
                                && !canExcuteStatus.isEmpty()){
                            Integer status = canExcuteStatus.remove(0);

                            for(DownloadInfo di : downloadInfos){
                                //noinspection ResourceType
                                if(di.getStatus() != status){
                                    continue;
                                }

                                if(downingThreads.size() >= DownloadConfiger.MaxDownloadingTaskNum){
                                    break;
                                }

                                //noinspection ResourceType
                                if(!downingThreads.containsKey(di.getId())){
                                    DownloadThread dt = new DownloadThread(getApplication(), di, mSystemFacade);
                                    mSystemFacade.startThread(dt);
                                    downingThreads.put(di.getId(), dt);
                                    LogUtil.d(DownloadService.class, "UpdateThread start thread : " + di.getUrl());
                                }
                            }
                        }
                    }
                }


                int waitingSize = dao.query(Where.create()
                        .lt(Downloads.ColumnStatus, DownloadInfo.StatusDowning), null).getCount();
                if(downingThreads.isEmpty() && waitingSize < 1){
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

    private class NetworkConnectionChangeReceiver extends BroadcastReceiver {
        public boolean isNetworkConnected = false;
        public boolean isWifiActive = false;
        public boolean isNetworkRoaming = false;

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            refreshNetStatus();
            updateFromProvider();
        }

        private void refreshNetStatus(){
            isNetworkConnected = mSystemFacade.isNetworkConnected();
            isWifiActive = mSystemFacade.isWifiActive();
            isNetworkRoaming = mSystemFacade.isNetworkRoaming();
        }

        public void regist(Context context){
            IntentFilter f = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            context.registerReceiver(this, f);
            refreshNetStatus();
        }

        public void unRegist(Context context){
            context.unregisterReceiver(this);
        }
    }

    public interface Cancelable{
        boolean isCanceled();
        void cancel();
    }

    // --------------------- logical fragments -----------------

}
