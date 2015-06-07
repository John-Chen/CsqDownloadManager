/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.support.annotation.NonNull;

import java.io.File;

public class FileUtil {

    /**
     * 保证文件所在文件夹已创建
     * @return 返回文件夹是否已创建
     */
    public static boolean checkPathExist(@NonNull String path){
        File f = new File(path);
        if(f.isDirectory()){
            if(!f.exists()){
                return f.mkdirs();
            }
        }else{
            if(!f.getParentFile().exists()){
                return f.getParentFile().mkdirs();
            }
        }
        return true;
    }

    public static String getSizeStr(long fileLength) {
        String strSize = "";
        try {
            if(fileLength >= 1024*1024*1024){
                strSize = (float)Math.round(10*fileLength/(1024*1024*1024))/10 + " GB";
            }else if(fileLength >= 1024*1024){
                strSize = (float)Math.round(10*fileLength/(1024*1024*1.0))/10 + " MB";
            }else if(fileLength >= 1024){
                strSize = (float)Math.round(10*fileLength/(1024))/10 + " KB";
            }else if(fileLength >= 0){
                strSize = fileLength + " B";
            }else {
                strSize = "0 B";
            }
        } catch (Exception e) {
            e.printStackTrace();
            strSize = "0 B";
        }
        return strSize;
    }

}
