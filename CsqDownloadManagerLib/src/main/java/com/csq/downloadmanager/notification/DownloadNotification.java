/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.csq.downloadmanager.DownloadListActivity;
import com.csq.downloadmanager.R;
import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.util.FileUtil;

public class DownloadNotification {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    private final Context context;
    private Notification notification;


    // ----------------------- Constructors ----------------------

    public DownloadNotification(Context context) {
        this.context = context;
    }

    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    /**
     * 更新通知栏下载状态，如果没有在通知栏显示，添加
     */
    public void update(@NonNull SystemFacade systemFacade, @NonNull DownloadInfo info){
        if(info.isWaiting() || info.isPaused()){
            //等待、暂停状态不显示
            cancel(systemFacade, info);
            return;
        }

        if(notification == null){
            notification = new Notification();
            notification.icon = R.mipmap.app_icon;
            setFlag(Notification.FLAG_ONGOING_EVENT, true); //将此通知放到通知栏的"Ongoing"即"正在运行"组中
            notification.contentView = new RemoteViews(context.getPackageName(),
                    R.layout.download_notification);
        }

        if(!info.isFinished()){
            setFlag(Notification.FLAG_NO_CLEAR, true);      //击了通知栏中的"清除通知"后，此通知不清除
            setFlag(Notification.FLAG_AUTO_CANCEL, false);  //点击不隐藏
        }else{
            setFlag(Notification.FLAG_NO_CLEAR, false);     //击了通知栏中的"清除通知"后，此通知清除
            setFlag(Notification.FLAG_AUTO_CANCEL, true);   //点击隐藏
        }

        //名称
        notification.contentView.setTextViewText(R.id.tvName,
                info.getFileName());
        //进度
        notification.contentView.setProgressBar(R.id.pbProgress,
                info.getTotalBytes(),
                info.getTotalBytes() > 0 ? info.getCurrentBytes().getAllDownloadedBytes() : 1,
                false);
        //状态
        if(info.isSuccessed()){
            notification.contentView.setTextViewText(R.id.tvStatus,
                    context.getResources().getText(R.string.download_success));
        }else if(info.isFailed()){
            notification.contentView.setTextViewText(R.id.tvStatus,
                    context.getResources().getText(R.string.download_failed));
        }else{
            //downing
            notification.contentView.setTextViewText(R.id.tvStatus,
                    FileUtil.getSizeStr(info.getCurrentBytes().getAllDownloadedBytes())
                            + " / " + FileUtil.getSizeStr(info.getTotalBytes()));
        }

        //点击事件
        Intent intent = null;
        if(info.isSuccessed()){
            intent = info.getMimeTypeHandleIntent(context);
            if(intent == null){
                intent = DownloadListActivity.getLaunchIntent(context);
            }
        }else{
            intent = DownloadListActivity.getLaunchIntent(context);
        }
        notification.contentIntent = PendingIntent.getActivity(context,
                0,
                intent,
                0);

        systemFacade.postNotification(info.getId(), notification);
    }

    /**
     * 取消通知栏显示
     * @param info 要取消的下载纪录
     */
    public void cancel(@NonNull SystemFacade systemFacade, @NonNull DownloadInfo info){
        if(notification != null){
            systemFacade.cancelNotification(info.getId());
            notification = null;
        }
    }

    // --------------------- Methods private ---------------------

    private void setFlag(int mask, boolean value) {
        if (value) {
            notification.flags |= mask;
        } else {
            notification.flags &= ~mask;
        }
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
