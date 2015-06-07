/**
 * description : 广播分发器
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.dispatcher;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

public class BroadcastEventDispatcher implements EventDispatcher {

    @Override
    public void downloadInfoAdded(Context context, long[] downloadIds) {
        if(downloadIds.length > 0){
            Intent i = new Intent(EventDownloadInfoAdded);
            i.putExtra(EventValueChangedIds, downloadIds);
            context.sendBroadcast(i);
        }
    }

    @Override
    public void downloadInfoRemoved(Context context, long[] downloadIds) {
        if(downloadIds.length > 0){
            Intent i = new Intent(EventDownloadInfoRemoved);
            i.putExtra(EventValueChangedIds, downloadIds);
            context.sendBroadcast(i);
        }
    }

    @Override
    public void downloadInfoChanged(Context context, long[] downloadIds, ContentValues changedColumns) {
        if(changedColumns.size() > 0){
            Intent i = new Intent(EventDownloadInfoChanged);
            i.putExtra(EventValueChangedIds, downloadIds);
            i.putExtra(EventValueUpdatedContentValues, changedColumns);
            context.sendBroadcast(i);
        }
    }

}
