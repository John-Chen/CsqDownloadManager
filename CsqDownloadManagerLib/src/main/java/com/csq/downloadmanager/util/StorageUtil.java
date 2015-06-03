/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Method;

public class StorageUtil {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    public static boolean isExternalMediaMounted() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            LogUtil.d(StorageUtil.class, "no external storage");
            return false;
        }
        return true;
    }

    /**
     * 获取Volume挂载状态, 例如Environment.MEDIA_MOUNTED
     */
    public static String getVolumeState(Context context, String path){
        //mountPoint是挂载点名Storage'paths[1]:/mnt/extSdCard不是/mnt/extSdCard/
        //不同手机外接存储卡名字不一样。/mnt/sdcard
        StorageManager mStorageManager = (StorageManager)context
                .getSystemService(Activity.STORAGE_SERVICE);
        String status = null;
        try {
            Method mMethodGetPathsState = mStorageManager.getClass().
                    getMethod("getVolumeState", String.class);
            return (String)mMethodGetPathsState.invoke(mStorageManager, path);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    public static boolean isStorageVolumeMounted(Context context, String path){
        return Environment.MEDIA_MOUNTED.equals(getVolumeState(context, path));
    }

    public static long getAvailableBytes(@NonNull File root) {
        StatFs stat = new StatFs(root.getPath());
        long availableBlocks = (long) stat.getAvailableBlocks() - 4;
        return stat.getBlockSize() * availableBlocks;
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
