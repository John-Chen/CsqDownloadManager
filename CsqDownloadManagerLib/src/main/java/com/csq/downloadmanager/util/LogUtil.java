/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.csq.downloadmanager.configer.DownloadConfiger;

public class LogUtil {

    public static void d(@NonNull Class cls, @NonNull String info){
        if(DownloadConfiger.LOG){
            Log.d(cls.getName(), "CsqDownMng --> " + info);
        }
    }

    public static void i(@NonNull Class cls, @NonNull String info){
        if(DownloadConfiger.LOG){
            Log.i(cls.getName(), "CsqDownMng --> " + info);
        }
    }

    public static void w(@NonNull Class cls, @NonNull String info){
        if(DownloadConfiger.LOG){
            Log.w(cls.getName(), "CsqDownMng --> " + info);
        }
    }

    public static void e(@NonNull Class cls, @NonNull String info){
        if(DownloadConfiger.LOG){
            Log.e(cls.getName(), "CsqDownMng --> " + info);
        }
    }

    public static void printException(@NonNull Class cls, @NonNull Throwable info){
        Log.e(cls.getName(), "CsqDownMng --> Exception : " + info.fillInStackTrace().toString());
    }
}
