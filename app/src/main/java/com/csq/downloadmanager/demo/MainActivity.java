package com.csq.downloadmanager.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.csq.downloadmanager.DownloadListActivity;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import java.util.HashMap;


public class MainActivity extends Activity {

    private HashMap<Integer, EditText> downloadUrls = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadUrls.put(R.id.btnDown1, (EditText) findViewById(R.id.etDown1));
        downloadUrls.put(R.id.btnDown2, (EditText) findViewById(R.id.etDown2));
        downloadUrls.put(R.id.btnDown3, (EditText) findViewById(R.id.etDown3));
    }

    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnDown1:
                String url1 = downloadUrls.get(v.getId()).getText().toString();
                DownloadInfo di1 = new DownloadInfo(url1)
                        .setDescription("户外助手")
                        .setIsOnlyWifi(true);
                try {
                    DownloadInfoDao.getInstace(this).startDownload(di1);
                } catch (DownloadInfoDao.UrlInvalidError urlInvalidError) {
                    urlInvalidError.printStackTrace();
                    Toast.makeText(this, "无效地址", Toast.LENGTH_SHORT).show();
                } catch (DownloadInfoDao.UrlAlreadyDownloadError urlAlreadyDownloadError) {
                    urlAlreadyDownloadError.printStackTrace();
                    Toast.makeText(this, "任务已存在", Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.btnDown2:
                String url2 = downloadUrls.get(v.getId()).getText().toString();
                DownloadInfo di2 = new DownloadInfo(url2);
                try {
                    DownloadInfoDao.getInstace(this).startDownload(di2);
                } catch (DownloadInfoDao.UrlInvalidError urlInvalidError) {
                    urlInvalidError.printStackTrace();
                    Toast.makeText(this, "无效地址", Toast.LENGTH_SHORT).show();
                } catch (DownloadInfoDao.UrlAlreadyDownloadError urlAlreadyDownloadError) {
                    urlAlreadyDownloadError.printStackTrace();
                    Toast.makeText(this, "任务已存在", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnDown3:
                String url3 = downloadUrls.get(v.getId()).getText().toString();
                DownloadInfo di3 = new DownloadInfo(url3);
                try {
                    DownloadInfoDao.getInstace(this).startDownload(di3);
                } catch (DownloadInfoDao.UrlInvalidError urlInvalidError) {
                    urlInvalidError.printStackTrace();
                    Toast.makeText(this, "无效地址", Toast.LENGTH_SHORT).show();
                } catch (DownloadInfoDao.UrlAlreadyDownloadError urlAlreadyDownloadError) {
                    urlAlreadyDownloadError.printStackTrace();
                    Toast.makeText(this, "任务已存在", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnDownloadList:
                startActivity(DownloadListActivity.getLaunchIntent(this));
                break;

            default:

                break;
        }
    }

}
