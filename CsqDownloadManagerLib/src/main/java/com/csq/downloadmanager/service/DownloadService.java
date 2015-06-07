/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.*;
import android.os.Process;
import com.csq.downloadmanager.SystemFacade;
import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.provider.Downloads;
import com.csq.downloadmanager.util.LogUtil;
import java.util.HashSet;
import java.util.Set;

public class DownloadService extends Service {


    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    /**
     * Observer to get notified when the content observer's data changes
     */
    private DownloadManagerContentObserver mObserver;

    /**
     * Whether the internal download list should be updated from the content
     * provider.
     */
    private boolean isUpdating;

    /**
     * The thread that updates the internal download list from the content
     * provider.
     */
    private UpdateThread mUpdateThread;

    private SystemFacade mSystemFacade;




    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException(
                "Cannot bind to DownloadService");
    }

    /**
     * Initializes the service when it is first created
     */
    public void onCreate() {
        super.onCreate();
        LogUtil.d(DownloadService.class, "Service onCreate");

        if (mSystemFacade == null) {
            mSystemFacade = new SystemFacade(this);
        }

        mObserver = new DownloadManagerContentObserver();
        getContentResolver().registerContentObserver(
                Downloads.CONTENT_URI, true, mObserver);

        mSystemFacade.cancelAllNotifications();

        updateFromProvider();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        LogUtil.d(DownloadService.class, "Service onStart");
        updateFromProvider();
        return returnValue;
    }

    /**
     * Cleans up when the service is destroyed
     */
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
        LogUtil.d(DownloadService.class, "Service onDestroy");
        super.onDestroy();
    }

    // --------------------- Methods public ----------------------


    // --------------------- Methods private ---------------------

    /**
     * Parses data from the content provider into private array
     */
    private void updateFromProvider() {
        synchronized (this) {
            isUpdating = true;
            if (mUpdateThread == null) {
                mUpdateThread = new UpdateThread();
                mUpdateThread.start();
            }
        }
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------

    /**
     * Receives notifications when the data in the content provider changes
     */
    private class DownloadManagerContentObserver extends ContentObserver {

        public DownloadManagerContentObserver() {
            super(new Handler());
        }

        /**
         * Receives notification when the data in the observed content provider
         * changes.
         */
        public void onChange(final boolean selfChange) {
            LogUtil.d(DownloadService.class, "Service ContentObserver received notification");
            updateFromProvider();
        }

    }

    private class UpdateThread extends Thread {
        public UpdateThread() {
            super("Download Service");
        }

        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            trimDatabase();
            removeSpuriousFiles();

            boolean keepService = false;
            // for each update from the database, remember which download is
            // supposed to get restarted soonest in the future
            long wakeUp = Long.MAX_VALUE;
            for (; ; ) {
                synchronized (DownloadService.this) {
                    if (mUpdateThread != this) {
                        throw new IllegalStateException(
                                "multiple UpdateThreads in DownloadService");
                    }
                    if (!isUpdating) {
                        mUpdateThread = null;
                        if (!keepService) {
                            stopSelf();
                        }
                        if (wakeUp != Long.MAX_VALUE) {
                            scheduleAlarm(wakeUp);
                        }
                        return;
                    }
                    isUpdating = false;
                }

                long now = mSystemFacade.currentTimeMillis();
                keepService = false;
                wakeUp = Long.MAX_VALUE;
                Set<Long> idsNoLongerInDatabase = new HashSet<Long>(
                        mDownloads.keySet());

                Cursor cursor = getContentResolver().query(
                        Downloads.CONTENT_URI, null, null, null,
                        null);
                if (cursor == null) {
                    continue;
                }
                try {
                    DownloadInfo.Reader reader = new DownloadInfo.Reader(
                            getContentResolver(), cursor);
                    int idColumn = cursor.getColumnIndexOrThrow(Downloads._ID);

                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                            .moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        idsNoLongerInDatabase.remove(id);
                        DownloadInfo info = mDownloads.get(id);
                        if (info != null) {
                            updateDownload(reader, info, now);
                        } else {
                            info = insertDownload(reader, now);
                        }
                        if (info.hasCompletionNotification()) {
                            keepService = true;
                        }
                        long next = info.nextAction(now);
                        if (next == 0) {
                            keepService = true;
                        } else if (next > 0 && next < wakeUp) {
                            wakeUp = next;
                        }
                    }
                } finally {
                    cursor.close();
                }

                for (Long id : idsNoLongerInDatabase) {
                    deleteDownload(id);
                }

                // is there a need to start the DownloadService? yes, if there
                // are rows to be deleted.

                for (DownloadInfo info : mDownloads.values()) {
                    if (info.mDeleted) {
                        keepService = true;
                        break;
                    }
                }

                mNotifier.updateNotification(mDownloads.values());

                // look for all rows with deleted flag set and delete the rows
                // from the database
                // permanently
                for (DownloadInfo info : mDownloads.values()) {
                    if (info.mDeleted) {
                        Helpers.deleteFile(getContentResolver(), info.mId,
                                info.mFileName, info.mMimeType);
                    }
                }
            }
        }

        private void scheduleAlarm(long wakeUp) {
            AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarms == null) {
                LogUtil.e(DownloadService.class, "couldn't get alarm manager");
                return;
            }

            LogUtil.d(DownloadService.class, "scheduling retry in " + wakeUp + "ms");

            Intent intent = new Intent(Constants.ACTION_RETRY);
            intent.setClassName(getPackageName(),
                    DownloadReceiver.class.getName());
            alarms.set(AlarmManager.RTC_WAKEUP,
                    mSystemFacade.currentTimeMillis() + wakeUp, PendingIntent
                            .getBroadcast(DownloadService.this, 0, intent,
                                    PendingIntent.FLAG_ONE_SHOT));
        }
    }

    public static interface Cancelable{
        public void cancel();
    }

    // --------------------- logical fragments -----------------

}
