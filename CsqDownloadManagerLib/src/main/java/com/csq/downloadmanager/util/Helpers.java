/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.csq.downloadmanager.provider.Downloads;

public class Helpers {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------



    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    public static void checkAccessPermission(Context context){
        // 检查自己或者其它调用者是否有 permission 权限
        context.enforceCallingOrSelfPermission(
                Downloads.PERMISSION_ACCESS,
                Downloads.PERMISSION_ACCESS + " permission is required to use the download manager");
    }

    @NonNull
    public static String getFileName(@NonNull String url,
                              String contentDisposition,
                              String mineType){
        String name = UrlUtil.guessFileName(url, contentDisposition, mineType);
        if(TextUtils.isEmpty(name)){
            name = "" + System.currentTimeMillis();
        }
        return name;
    }


    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
