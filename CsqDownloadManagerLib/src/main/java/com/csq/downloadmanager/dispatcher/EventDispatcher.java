package com.csq.downloadmanager.dispatcher;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * description : 下载事件分发器接口，默认广播，
 *      也可以修改{@link com.csq.downloadmanager.configer.DownloadConfiger#EventDispatcher}
 *      通过消息总线等其他方式分发
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
public interface EventDispatcher {

    String EventDownloadInfoAdded = "EventDownloadInfoAdded";
    String EventDownloadInfoRemoved = "EventDownloadInfoRemoved";
    String EventDownloadInfoChanged = "EventDownloadInfoChanged";

    String EventValueChangedIds = "EventValueChangedIds";
    String EventValueUpdatedContentValues = "EventValueUpdatedContentValues";

    /**
     * 添加下载任务
     * @param downloadIds 新加记录的数据库id
     */
    void downloadInfoAdded(@NonNull Context context, @NonNull long[] downloadIds);

    /**
     * 移除下载任务
     * @param downloadIds 已移除记录的数据库id
     */
    void downloadInfoRemoved(@NonNull Context context, @NonNull long[] downloadIds);

    /**
     * 下载信息改变
     * @param changedColumns 更新的数据库字段，key--ColumnName
     */
    void downloadInfoChanged(@NonNull Context context, @NonNull long[] downloadIds, @NonNull ContentValues changedColumns);

}
