/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.csq.downloadmanager.util.LogUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SystemFacade {

    // ------------------------ Constants ------------------------
    private static final ExecutorService executorService = Executors.newCachedThreadPool();


    // ------------------------- Fields --------------------------
    private Context mContext;
    private NotificationManager mNotificationManager;
    private ConnectivityManager connectivity;
    private TelephonyManager telephonyManager;

    // ----------------------- Constructors ----------------------

    public SystemFacade(Context context) {
        this.mContext = context;
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    public Integer getActiveNetworkType() {
        ConnectivityManager connectivity = getConnectivityManager();
        if (connectivity == null) {
            return null;
        }

        NetworkInfo activeInfo = connectivity.getActiveNetworkInfo();
        if (activeInfo == null) {
            LogUtil.d(SystemFacade.class, "network is not available");
            return null;
        }
        return activeInfo.getType();
    }

    public boolean isNetworkConnected(){
        return getActiveNetworkType() != null;
    }

    public boolean isWifiActive(){
        Integer type = getActiveNetworkType();
        return type != null && type == ConnectivityManager.TYPE_WIFI;
    }

    public boolean isNetworkRoaming() {
        ConnectivityManager connectivity = getConnectivityManager();
        if (connectivity == null) {
            return false;
        }

        NetworkInfo info = connectivity.getActiveNetworkInfo();
        boolean isMobile = (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE);
        final TelephonyManager mgr = getTelephonyManager();
        boolean isRoaming = isMobile && mgr.isNetworkRoaming();
        LogUtil.d(SystemFacade.class, "network is roaming ? " + isRoaming);
        return isRoaming;
    }

    public void postNotification(long id, Notification notification) {
        mNotificationManager.notify((int) id, notification);
    }

    public void cancelNotification(long id) {
        mNotificationManager.cancel((int) id);
    }

    public void cancelAllNotifications() {
        mNotificationManager.cancelAll();
    }

    public void startThread(Runnable runnable) {
        executorService.execute(runnable);
    }

    // --------------------- Methods private ---------------------

    private ConnectivityManager getConnectivityManager(){
        if(connectivity == null){
            connectivity = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity == null) {
                LogUtil.d(SystemFacade.class, "couldn't get connectivity manager");
            }
        }
        return connectivity;
    }

    private TelephonyManager getTelephonyManager(){
        if(telephonyManager == null){
            telephonyManager = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
        return telephonyManager;
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
