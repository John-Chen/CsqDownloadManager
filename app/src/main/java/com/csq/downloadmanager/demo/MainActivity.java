package com.csq.downloadmanager.demo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.csq.downloadmanager.DownloadListActivity;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.provider.Downloads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends Activity {

    private ListView lvStartUrls;
    private List<String> mTestUrls = new ArrayList<>();

    private UrlAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lvStartUrls = (ListView) findViewById(R.id.lvStartUrls);

        adapter = new UrlAdapter();
        lvStartUrls.setAdapter(adapter);

        mTestUrls.add("http://www.2bulu.com/base/file_down_d.htm?downParams=f7k2olcfLJ4=");
        mTestUrls.add("http://www.2bulu.com/base/file_down_d.htm?downParams=IcC6xlnyzeI=");
        mTestUrls.add("http://d.3987.com/lvsezhiwu_150605/002.jpg");
        mTestUrls.add("http://dl.wenku.baidu.com/wenku29/%2Fc58ea6e3b9d350d9784e7b5d84ffbba9?sign=MBOT:y1jXjmMD4FchJHFHIGN4z:Rp5JvZu0AcS2BH4uOoE9XjzmMLY%3D&time=1434556342&response-content-disposition=attachment;%20filename=%22Android%D3%A6%D3%C3%D0%D4%C4%DC%D3%C5%BB%AF.pdf%22&response-content-type=application%2foctet-stream");
        mTestUrls.add("http://yinyueshiting.baidu.com/data2/music/109017312/3514661434502861128.mp3?xcode=20d1077fafe3575019a583210f1a2e09");
        mTestUrls.add("http://60.217.224.79/youku/67723E98BC94783D3CF6ED51DB/0300080100507E6CE267C6051312EF1BA7C738-B2B7-F77E-0486-1707688E800D.mp4");

        new AsyncTask<Void, Integer, List<DownloadInfo>>(){
            @Override
            protected List<DownloadInfo> doInBackground(Void... params) {
                return DownloadInfoDao.getInstace(MainActivity.this).queryDownloadInfos(
                        Where.create().in(Downloads.ColumnUrl, mTestUrls),
                        null);
            }

            @Override
            protected void onPostExecute(List<DownloadInfo> downloadInfos) {
                super.onPostExecute(downloadInfos);
                if(downloadInfos != null && !downloadInfos.isEmpty()){
                    for(DownloadInfo di : downloadInfos){
                        mTestUrls.remove(di.getUrl());
                    }
                }
                adapter.updateDatas(mTestUrls);
            }
        }.execute();

    }

    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnDownloadList:
                startActivity(DownloadListActivity.getLaunchIntent(this));
                break;

            default:

                break;
        }
    }


    private class UrlAdapter extends BaseAdapter{

        private List<String> datas = Collections.EMPTY_LIST;

        public void updateDatas(List<String> datas){
            if(datas != null){
                this.datas = datas;
                notifyDataSetChanged();
            }
        }

        public void remove(String url){
            if(datas.remove(url)){
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            return datas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if(convertView == null){
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.itemview_start_download, null);
                vh = new ViewHolder(convertView);
                convertView.setTag(vh);
            }else{
                vh = (ViewHolder) convertView.getTag();
            }
            vh.setUrl(datas.get(position));
            return convertView;
        }
    }

    private class ViewHolder implements View.OnClickListener{
        private TextView tvUrl;
        private Button btnDown;
        private String url;

        public ViewHolder(View convertView){
            tvUrl = (TextView) convertView.findViewById(R.id.tvUrl);
            btnDown = (Button) convertView.findViewById(R.id.btnDown);
            btnDown.setOnClickListener(this);
        }

        public void setUrl(String url) {
            this.url = url;
            tvUrl.setText(url);
        }

        @Override
        public void onClick(View v) {
            if(!TextUtils.isEmpty(url)){
                DownloadInfo info = new DownloadInfo(url);
                try {
                    DownloadInfoDao.getInstace(MainActivity.this).startDownload(info);
                    adapter.remove(url);
                } catch (DownloadInfoDao.UrlInvalidError urlInvalidError) {
                    urlInvalidError.printStackTrace();
                    Toast.makeText(MainActivity.this, "无效地址", Toast.LENGTH_SHORT).show();
                } catch (DownloadInfoDao.UrlAlreadyDownloadError urlAlreadyDownloadError) {
                    urlAlreadyDownloadError.printStackTrace();
                    Toast.makeText(MainActivity.this, "任务已存在", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
