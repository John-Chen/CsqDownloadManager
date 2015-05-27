/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.service;

import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.configer.DownloadConfiger;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.util.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class DownloadThread implements Runnable, DownloadService.Cancelable {


    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------
    public final Context context;
    public final DownloadInfo downloadInfo;
    public final SystemFacade systemFacade;
    public boolean isCanceled = false;

    private HttpURLConnection connection;
    private InputStream inputStream;

    // ----------------------- Constructors ----------------------

    public DownloadThread(@NonNull Context context,
                          @NonNull DownloadInfo downloadInfo,
                          @NonNull SystemFacade systemFacade) {
        this.context = context;
        this.downloadInfo = downloadInfo;
        this.systemFacade = systemFacade;
    }


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        int finalStatus = DownloadInfo.StatusFailedUnknownError;

        try {
            URL url = new URL(TextUtils.isEmpty(downloadInfo.getReDirectUrl())
                    ? downloadInfo.getUrl() : downloadInfo.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setFollowRedirects(true);
            connection.connect();

            if(DownloadConfiger.LOG){
                printResponseHeader(connection);
            }

            String headAcceptRanges = connection.getHeaderField("Accept-Ranges");
            int headContentLength = connection.getContentLength();
            String headEtag = (!TextUtils.isEmpty(headAcceptRanges) && headContentLength > 0)
                    ? connection.getHeaderField("ETag") : "";
            if(!headEtag.equals(downloadInfo.getETag())){
                downloadInfo.setETag(headEtag);
                DownloadInfoDao.getInstace(context).
            }



            int responseCode= connection.getResponseCode();
            int contentLength = connection.getContentLength();
            InputStream is = connection.getInputStream();
            connection.getHeaderField()

            if(TextUtils.isEmpty(downloadInfo.getReDirectUrl())
                    && downloadInfo.getCurrentBytes().getAllDownloadedBytes() < 1){

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void cancel() {
        isCanceled = true;
    }

    // --------------------- Methods public ----------------------


    // --------------------- Methods private ---------------------

    private static void printResponseHeader(HttpURLConnection connection) throws UnsupportedEncodingException {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0;; i++) {
            String mine = connection.getHeaderField(i);
            if (mine == null)
                break;
            header.put(connection.getHeaderFieldKey(i), mine);
        }
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            LogUtil.d(DownloadThread.class, "ResponseHeader " + key + " : " + entry.getValue());
        }
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
