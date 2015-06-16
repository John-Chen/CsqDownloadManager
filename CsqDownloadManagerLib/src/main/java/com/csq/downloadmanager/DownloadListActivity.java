/**
 * description : 所有下载记录列表
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */

package com.csq.downloadmanager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.db.DownloadInfoDao;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.dispatcher.EventDispatcher;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.DialogUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


public class DownloadListActivity extends Activity {

    public final static String IntentAction = "com.csq.downloadmanager.DownloadListActivity";

    public static Intent getLaunchIntent(Context context){
        Intent i = new Intent();
        i.setAction(IntentAction);
        i.setPackage(context.getPackageName());
        return i;
    }

    private ListView lvDownload;
    private long expandeItemId = -1;
    private DownloadAdapter adapter;
    private DownloadInfoDao downloadInfoDao;

    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int changeType = intent.getIntExtra(EventDispatcher.EventDownloadInfoChangeType, -1);
            if(changeType > 0){
                if(changeType == EventDispatcher.ChangeTypeAdded){
                    long[] downloadIds = intent.getLongArrayExtra(EventDispatcher.EventValueChangedIds);
                    if(downloadIds != null && downloadIds.length > 0){
                        adapter.addDatas(downloadIds);
                    }

                }else if(changeType == EventDispatcher.ChangeTypeRemoved){
                    long[] downloadIds = intent.getLongArrayExtra(EventDispatcher.EventValueChangedIds);
                    if(downloadIds != null && downloadIds.length > 0){
                        adapter.removeDatas(downloadIds);
                    }

                }else if(changeType == EventDispatcher.ChangeTypeUpdated){
                    long[] downloadIds = intent.getLongArrayExtra(EventDispatcher.EventValueChangedIds);
                    ContentValues changedColumns = intent.getParcelableExtra(EventDispatcher.EventValueUpdatedContentValues);
                    if(downloadIds != null && downloadIds.length > 0 && changedColumns != null && changedColumns.size() > 0){
                        adapter.updatDatas(downloadIds, changedColumns);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        downloadInfoDao = DownloadInfoDao.getInstace(this);
        if(savedInstanceState != null){
            expandeItemId = savedInstanceState.getLong("expandeItemId", -1);
        }
        lvDownload = (ListView) findViewById(R.id.lvDownload);
        lvDownload.setEmptyView(findViewById(R.id.tvEmptyList));
        adapter = new DownloadAdapter();
        lvDownload.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(EventDispatcher.EventDownloadInfoAction);
        registerReceiver(mDownloadReceiver, intentFilter);

        loadDatas();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mDownloadReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("expandeItemId", expandeItemId);
    }

    private void loadDatas(){
        new AsyncTask<Void, Integer, List<DownloadInfo>>(){
            @Override
            protected List<DownloadInfo> doInBackground(Void... params) {
                return downloadInfoDao.queryAll();
            }

            @Override
            protected void onPostExecute(List<DownloadInfo> downloadInfos) {
                super.onPostExecute(downloadInfos);
                adapter.setDatas(downloadInfos);
            }
        }.execute();
    }

    private String getProgressTag(long downloadInfoId){
        return "pb" + downloadInfoId;
    }

    private ProgressBar findProgressBar(long downloadInfoId){
        View v = lvDownload.findViewWithTag(getProgressTag(downloadInfoId));
        if(v != null){
            return (ProgressBar) v;
        }
        return null;
    }


    private class DownloadAdapter extends BaseAdapter{

        private final List<DownloadInfo> datas = new ArrayList<>();

        public DownloadAdapter(){ }

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
                convertView = LayoutInflater.from(DownloadListActivity.this).inflate(R.layout.itemview_download_list, null);
                vh = new ViewHolder(convertView);
                convertView.setTag(vh);
            }else{
                vh = (ViewHolder) convertView.getTag();
            }
            vh.setData((DownloadInfo) getItem(position));
            return convertView;
        }

        public void setDatas(List<DownloadInfo> datas) {
            synchronized (this.datas){
                this.datas.clear();
                if(datas != null && !datas.isEmpty()){
                    this.datas.addAll(datas);
                }
                notifyDataSetChanged();
            }
        }

        public List<DownloadInfo> getDatas() {
            return datas;
        }

        public void addDatas(final long[] downloadIds){
            new AsyncTask<Void, Integer, List<DownloadInfo>>(){
                @Override
                protected List<DownloadInfo> doInBackground(Void... params) {
                    return downloadInfoDao.queryDownloadInfos(
                            Where.create().in(Downloads.ColumnID, downloadIds), null);
                }

                @Override
                protected void onPostExecute(List<DownloadInfo> downloadInfos) {
                    super.onPostExecute(downloadInfos);
                    if(downloadInfos != null && !downloadInfos.isEmpty()){
                        synchronized (datas){
                            datas.addAll(0, downloadInfos);
                            notifyDataSetChanged();
                        }
                    }
                }
            }.execute();
        }

        public void removeDatas(long[] downloadIds){
            HashSet<Long> removed = new HashSet<>(downloadIds.length);
            for(long id : downloadIds){
                removed.add(id);
            }
            synchronized (datas){
                Iterator<DownloadInfo> it = datas.iterator();
                while (it.hasNext()){
                    if(removed.contains(it.next().getId())){
                        it.remove();
                    }
                }
                notifyDataSetChanged();
            }
        }

        public void updatDatas(long[] downloadIds, ContentValues changedColumns){
            HashSet<Long> updated = new HashSet<>(downloadIds.length);
            for(long id : downloadIds){
                updated.add(id);
            }
            //只是更新进度
            boolean isUpdateProgress = changedColumns.size() == 1
                    && changedColumns.containsKey(Downloads.ColumnCurrentBytes);
            synchronized (datas){
                ProgressBar progressBar = null;
                for(DownloadInfo di : datas){
                    if(updated.contains(di.getId())){
                        di.updateByUpdatedContentValues(changedColumns);

                        if(isUpdateProgress){
                            progressBar = findProgressBar(di.getId());
                            if(progressBar != null){
                                progressBar.setProgress((int) (di.getProgress()*100));
                            }
                        }
                    }
                }
                if(!isUpdateProgress){
                    //只是更新进度
                    notifyDataSetChanged();
                }
            }
        }
    }

    private class ViewHolder implements View.OnClickListener{
        private TextView tvName, tvStatus, tvUrl, tvProgress;
        private View lyProgress;
        private ProgressBar pbProgress;
        private View lyExpandable;
        private Button btnOpen, btnPauseOrResume, btnDelete;
        private DownloadInfo data;

        public ViewHolder(View convertView){
            tvName = (TextView) convertView.findViewById(R.id.tvName);
            tvStatus = (TextView) convertView.findViewById(R.id.tvStatus);
            tvUrl = (TextView) convertView.findViewById(R.id.tvUrl);
            tvProgress = (TextView) convertView.findViewById(R.id.tvProgress);
            lyProgress = convertView.findViewById(R.id.lyProgress);
            pbProgress = (ProgressBar) convertView.findViewById(R.id.pbProgress);
            lyExpandable = convertView.findViewById(R.id.lyExpandable);
            btnOpen = (Button) convertView.findViewById(R.id.btnOpen);
            btnPauseOrResume = (Button) convertView.findViewById(R.id.btnPauseOrResume);
            btnDelete = (Button) convertView.findViewById(R.id.btnDelete);

            convertView.setOnClickListener(this);
            btnOpen.setOnClickListener(this);
            btnPauseOrResume.setOnClickListener(this);
            btnDelete.setOnClickListener(this);
        }

        public void setData(DownloadInfo data) {
            this.data = data;

            tvName.setText(data.getFileName());

            if(data.isDowning()){
                tvStatus.setText(R.string.downing);
            }else if(data.isSuccessed()){
                tvStatus.setText(R.string.download_success);
            }else if(data.isFailed()){
                tvStatus.setText(R.string.download_failed);
            }else if(data.isPaused()){
                tvStatus.setText(R.string.paused);
            }else if(data.getStatus() == DownloadInfo.StatusWaitingForNet){
                tvStatus.setText(R.string.waiting_for_net);
            }else if(data.getStatus() == DownloadInfo.StatusWaitingForWifi){
                tvStatus.setText(R.string.waiting_for_wifi);
            }else{
                tvStatus.setText(R.string.waiting);
            }

            tvUrl.setText(data.getUrl());

            if(data.isSuccessed()){
                lyProgress.setVisibility(View.GONE);
            }else{
                lyProgress.setVisibility(View.VISIBLE);
                pbProgress.setProgress((int) (data.getProgress()*100));
                if(data.getTotalBytes() < 1){
                    tvProgress.setText(R.string.unknown_size);
                }else{
                    tvProgress.setText(pbProgress.getProgress() + "%");
                }
            }

            if(expandeItemId != data.getId()){
                lyExpandable.setVisibility(View.GONE);
            }else{
                lyExpandable.setVisibility(View.VISIBLE);

                if(data.isSuccessed()){
                    btnOpen.setEnabled(true);
                    btnPauseOrResume.setVisibility(View.GONE);
                }else{
                    btnOpen.setEnabled(false);
                    btnPauseOrResume.setVisibility(View.VISIBLE);
                }

                if(data.isWaiting() || data.isDowning()){
                    btnPauseOrResume.setText(R.string.pause);
                }else if(data.isPaused()){
                    btnPauseOrResume.setText(R.string.resume);
                }else if(data.isFailed()){
                    btnPauseOrResume.setText(R.string.re_download);
                }
            }

            pbProgress.setTag(getProgressTag(data.getId()));
        }

        @Override
        public void onClick(View v) {
            if(data != null){
                if (v.getId() == R.id.btnOpen) {
                    Intent intent = data.getMimeTypeHandleIntent(DownloadListActivity.this);
                    if(intent != null){
                        startActivity(intent);
                    }else{
                        Toast.makeText(DownloadListActivity.this,
                                R.string.file_have_no_handled_app,
                                Toast.LENGTH_SHORT).show();
                    }

                }else if(v.getId() == R.id.btnPauseOrResume){
                    if(data.isWaiting()){
                        try {
                            downloadInfoDao.pauseDownload(data.getId());
                        } catch (DownloadInfoDao.UnExpectedStatus unExpectedStatus) {
                            unExpectedStatus.printStackTrace();
                        }

                    }else if(data.isDowning()){
                        if(TextUtils.isEmpty(data.getETag())){
                            //不支持断点续传
                            DialogUtil.showConfirmDialog(DownloadListActivity.this,
                                    getResources().getString(R.string.confirm_pause_unsupport_breakpoint),
                                    new DialogUtil.ConfirmListener() {
                                        @Override
                                        public void ok() {
                                            try {
                                                downloadInfoDao.pauseDownload(data.getId());
                                            } catch (DownloadInfoDao.UnExpectedStatus unExpectedStatus) {
                                                unExpectedStatus.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void cancel() {
                                        }
                                    });

                        }else{
                            try {
                                downloadInfoDao.pauseDownload(data.getId());
                            } catch (DownloadInfoDao.UnExpectedStatus unExpectedStatus) {
                                unExpectedStatus.printStackTrace();
                            }
                        }

                    }else if(data.isPaused()){
                        try {
                            downloadInfoDao.resumeDownload(data.getId());
                        } catch (DownloadInfoDao.UnExpectedStatus unExpectedStatus) {
                            unExpectedStatus.printStackTrace();
                        }

                    }else if(data.isFailed()){
                        try {
                            downloadInfoDao.restartDownload(data.getId());
                        } catch (DownloadInfoDao.UnExpectedStatus unExpectedStatus) {
                            unExpectedStatus.printStackTrace();
                        }
                    }

                }else if(v.getId() == R.id.btnDelete){
                    DialogUtil.showConfirmDialog(DownloadListActivity.this,
                            getResources().getString(R.string.confirm_delete_task),
                            new DialogUtil.ConfirmListener() {
                                @Override
                                public void ok() {
                                    downloadInfoDao.deleteDownload(data.getId());
                                    if(expandeItemId == data.getId()){
                                        expandeItemId = -1;
                                    }
                                }

                                @Override
                                public void cancel() {
                                }
                            });

                }else{
                    if(expandeItemId == data.getId()){
                        expandeItemId = -1;
                        lyExpandable.setVisibility(View.GONE);

                    }else{
                        expandeItemId = data.getId();
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

}
