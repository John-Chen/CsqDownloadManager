/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;

import java.io.File;

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
