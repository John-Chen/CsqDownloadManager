# CsqDownloadManager


## 概述

一个支持多任务、任务多进程、断点续传的下载管理器，适用于较大文件下载。minSdkVersion 9。


**特点**：

1、支持多个下载任务（默认最多3个）同时下载，如果任务支持断点续传，可以配置此任务多线程（1-6个，默认3个）下载，并支持任务暂停/恢复/删除等操作；

2、使用原生sql语句，不依赖其他组件，尽量减少方法数量；

3、数据库操作通过ContentProvider，其他应用也可以通过ContentProvider进行下载管理；

4、支持自定义下载事件分发器，默认通过广播分发，可以implements EventDispatcher实现其他事件分发。

<br>
#### 建议与反馈

作者能力有限，下载过程异常情况也特别多，暂时也还在测试验证阶段，所以可能有一些错误，如果大家有什么好的建议或者测试bug，欢迎反馈。

Email : <csqwyyx@163.com>


<br>
<br>
## 使用

<br>
**权限**：
    
    <permission
        android:name="com.csq.permission.ACCESS_DOWNLOAD_MANAGER"
        android:protectionLevel="normal" />
    <uses-permission android:name="com.csq.permission.ACCESS_DOWNLOAD_MANAGER" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

<br>
**AndroidManifest.xml**：
    
        <activity
            android:name="com.csq.downloadmanager.DownloadListActivity"
            android:label="@string/title_activity_download_list"
            android:launchMode="singleInstance">
            <intent-filter>
                <action android:name="com.csq.downloadmanager.DownloadListActivity"/>
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <provider
            android:name="com.csq.downloadmanager.provider.DownloadProvider"
            android:authorities="com.csq.downloadmanager.provider" />

        <service android:name="com.csq.downloadmanager.service.DownloadService" />

<br>
**增**：

    DownloadInfo info = new DownloadInfo(url);
    info.setThreadNum(3);//线程数
    info.setIsOnlyWifi(true);//仅在wifi下下载
    info.setFolderPath(Environment.getDownloadCacheDirectory().getAbsolutePath());//下载目录
    info.setIsShowNotification(true);//是否在通知栏显示下载进度
    DownloadInfoDao.getInstace(context).startDownload(info);

<br>
**删**：
    
    DownloadInfoDao.getInstace(context).deleteDownload(rowId);

<br>
**改**：

    DownloadInfoDao.getInstace(context).updateDownload(
    UpdateCondition.create()
          .addColumn(Downloads.ColumnStatus, DownloadInfo.StatusDowning)
          .setWhere(new Where().eq(Downloads.ColumnID, downloadInfo.getId()))
          );

<br>
**查**：

    DownloadInfoDao.getInstace(context).queryDownloadInfos(
        Where.create().in(Downloads.ColumnID, downloadIds), null);







