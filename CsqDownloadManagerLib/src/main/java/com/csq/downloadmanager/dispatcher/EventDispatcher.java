package com.csq.downloadmanager.dispatcher;

import com.csq.downloadmanager.db.DownloadInfo;

/**
 * description : 下载时间分发器
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
public interface EventDispatcher {

    /**
     * 下载信息改变
     */
    public void downloadInfoChanged(DownloadInfo info);

}
