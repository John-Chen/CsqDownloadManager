/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.content.Context;
import android.os.PowerManager;

public class WakeLockUtil {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    private volatile static WakeLockUtil instance;

    private WakeLockUtil(Context context) {
        PowerManager pm = (PowerManager) context
                .getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CsqDownMng");
    }

    public static WakeLockUtil getInstace(Context context) {
        synchronized (WakeLockUtil.class) {
            if (instance == null) {
                instance = new WakeLockUtil(context);
            }
        }
        return instance;
    }

    private PowerManager.WakeLock wakeLock = null;
    private boolean isAwake = false;

    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    public synchronized void acquire(){
        if(!isAwake){
            wakeLock.acquire();
        }
    }

    public synchronized void release(){
        if(isAwake){
            wakeLock.release();
        }
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
