/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager;

import android.os.Environment;

import com.csq.downloadmanager.dispatcher.BroadcastEventDispatcher;
import com.csq.downloadmanager.dispatcher.EventDispatcher;
import com.csq.downloadmanager.util.FileUtil;

import java.io.File;

public class DownloadConfiger {

    /**
     * 是否打印log日志
     */
    public static final boolean LOG = true;

    /**
     * 默认文件下载目录
     */
    private static final String DefaultDownloadPath
            = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "CsqDownload";
    /**
     * 获取默认文件下载目录
     */
    public static String getDefaultDownloadPath() {
        FileUtil.checkPathExist(DefaultDownloadPath);
        return DefaultDownloadPath;
    }

    /**
     * 每个下载任务的默认下载线程数
     */
    public static final int DefaultTreadNumPerTask = 3;

    /**
     * 每个下载任务最大下载线程数
     */
    public static final int MaxTreadNumPerTask = 6;

    /**
     * 同时下载的最大任务数
     */
    public static final int MaxDownloadingTaskNum = 3;

    /**
     * 手机网络下能下载的最大大小，1G
     */
    public static final long DOWNLOAD_MAX_BYTES_OVER_MOBILE = 1024 * 1024 * 1024L;
    /**
     * 推荐wifi下下载的大小，512M
     */
    public static final long DOWNLOAD_BYTES_RECOMMEND_WIFI = 512 * 1024 * 1024L;

    /**
     * 下载事件分发器，默认通过广播的方式
     */
    public static EventDispatcher EventDispatcher = new BroadcastEventDispatcher();

}
