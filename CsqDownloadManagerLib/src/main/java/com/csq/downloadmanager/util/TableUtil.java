/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.provider.Downloads;

public class TableUtil {

    // ------------------------ Constants ------------------------

    public static final String TABLE_NAME = "download_info";

    // ------------------------- Fields --------------------------


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    /** Creates the underlying database table. */
    public static void dropAndCreateTable(@NonNull SQLiteDatabase db) {
        dropTable(db);

        String sql = "CREATE TABLE " + TABLE_NAME + "(" +
                Downloads.ColumnID + " BIGINT PRIMARY KEY AUTOINCREMENT," + // 0: id
                Downloads.ColumnFileName + " TEXT," + // 1: fileName
                Downloads.ColumnDescription + " TEXT," + // 2: description
                Downloads.ColumnUrl + " TEXT NOT NULL ," + // 3: url
                Downloads.ColumnFolderPath + " TEXT," + // 4: folderPath
                Downloads.ColumnThreadNum + " INTEGER," + // 5: threadNum
                Downloads.ColumnGroupName + " TEXT," + // 6: groupName
                Downloads.ColumnIsShowNotification + " BOOLEAN," + // 7: isShowNotification
                Downloads.ColumnIsOnlyWifi + " BOOLEAN," + // 8: isOnlyWifi
                Downloads.ColumnIsAllowRoaming + " BOOLEAN," + // 9: isAllowRoaming
                Downloads.ColumnMimeType + " TEXT," + // 10: mimeType
                Downloads.ColumnTotalBytes + " INTEGER," + // 11: totalBytes
                Downloads.ColumnCurrentBytes + " TEXT," + // 12: currentBytes
                Downloads.ColumnReDirectUrl + " TEXT," + // 13: reDirectUrl
                Downloads.ColumnETag + " TEXT," + // 14: eTag
                Downloads.ColumnLastModifyTime + " BIGINT," + // 15: lastModifyTime
                Downloads.ColumnStatus + " INTEGER DEFAULT " + DownloadInfo.StatusWaitingForExecute + "," + // 16: status
                Downloads.ColumnNumFailed + " INTEGER," + // 17: waitingReason
                Downloads.ColumnRetryAfterTime + " BIGINT," + // 18: retryAfterTime
                ");";
        db.execSQL(sql);

        db.execSQL("CREATE INDEX index_url ON " + TABLE_NAME + "(" + Downloads.ColumnUrl + ")");

        LogUtil.d(TableUtil.class, sql);
    }

    /** Drops the underlying database table. */
    public static void dropTable(@NonNull SQLiteDatabase db) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);

        LogUtil.d(TableUtil.class, sql);
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
