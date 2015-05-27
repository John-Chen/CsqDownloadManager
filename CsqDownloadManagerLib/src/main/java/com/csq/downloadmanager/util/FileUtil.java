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

}
