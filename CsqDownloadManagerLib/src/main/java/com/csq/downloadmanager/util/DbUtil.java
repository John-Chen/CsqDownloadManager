/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.csq.downloadmanager.db.DownloadInfo;
import com.csq.downloadmanager.provider.Downloads;
import java.util.concurrent.Callable;

public class DbUtil {

    @Nullable
    public static <T> T transaction(@NonNull SQLiteDatabase db, @NonNull Callable<T> task){
        T result = null;
        db.beginTransaction();
        try {
            result = task.call();
            db.setTransactionSuccessful();
        } catch (Exception e) {
            LogUtil.e(DbUtil.class, e.toString());
        } finally {
            db.endTransaction();
        }
        return result;
    }

    /**
     * SQLiteStatement stmt = db.compileStatement(sql);
     * bindValues(stmt, entity);
     * long result = stat.executeInsert();
     */
    public static void bindValues(@NonNull SQLiteStatement stmt, @NonNull DownloadInfo entity) {
        stmt.clearBindings();

        long id = entity.getId();
        stmt.bindLong(1, id);

        String fileName = entity.getFileName();
        if (fileName != null) {
            stmt.bindString(2, fileName);
        }

        String description = entity.getDescription();
        if (description != null) {
            stmt.bindString(3, description);
        }
        stmt.bindString(4, entity.getUrl());

        String folderPath = entity.getFolderPath();
        if (folderPath != null) {
            stmt.bindString(5, folderPath);
        }

        Integer threadNum = entity.getThreadNum();
        if (threadNum != null) {
            stmt.bindLong(6, threadNum);
        }

        String groupName = entity.getGroupName();
        if (groupName != null) {
            stmt.bindString(7, groupName);
        }

        Boolean isShowNotification = entity.getIsShowNotification();
        if (isShowNotification != null) {
            stmt.bindLong(8, isShowNotification ? 1l: 0l);
        }

        Boolean isOnlyWifi = entity.getIsOnlyWifi();
        if (isOnlyWifi != null) {
            stmt.bindLong(9, isOnlyWifi ? 1l: 0l);
        }

        Boolean isAllowRoaming = entity.getIsAllowRoaming();
        if (isAllowRoaming != null) {
            stmt.bindLong(10, isAllowRoaming ? 1l: 0l);
        }

        String mimeType = entity.getMimeType();
        if (mimeType != null) {
            stmt.bindString(11, mimeType);
        }

        Integer totalBytes = entity.getTotalBytes();
        if (totalBytes != null) {
            stmt.bindLong(12, totalBytes);
        }

        String currentBytes = entity.getCurrentBytes().toDbJsonString();
        if (currentBytes != null) {
            stmt.bindString(13, currentBytes);
        }

        String reDirectUrl = entity.getReDirectUrl();
        if (reDirectUrl != null) {
            stmt.bindString(14, reDirectUrl);
        }

        String eTag = entity.getETag();
        if (eTag != null) {
            stmt.bindString(15, eTag);
        }

        Long lastModifyTime = entity.getLastModifyTime();
        if (lastModifyTime != null) {
            stmt.bindLong(16, lastModifyTime);
        }

        Integer status = entity.getStatus();
        if (status != null) {
            stmt.bindLong(17, status);
        }

        Integer numFailed = entity.getNumFailed();
        if (numFailed != null) {
            stmt.bindLong(18, numFailed);
        }
    }

    public static void putIfNonNull(ContentValues contentValues, String key, Object value) {
        if (value != null) {
            contentValues.put(key, value.toString());
        }
    }


    @SuppressWarnings("ResourceType")
    @NonNull
    public static DownloadInfo readEntity(@NonNull Cursor cursor) {
        return new DownloadInfo(cursor.getString(cursor.getColumnIndex(Downloads.ColumnUrl)))
                .setId(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnID)))
                .setFileName(cursor.getString(cursor.getColumnIndex(Downloads.ColumnFileName)))
                .setDescription(cursor.getString(cursor.getColumnIndex(Downloads.ColumnDescription)))
                .setFolderPath(cursor.getString(cursor.getColumnIndex(Downloads.ColumnFolderPath)))
                .setThreadNum(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnThreadNum)))
                .setGroupName(cursor.getString(cursor.getColumnIndex(Downloads.ColumnGroupName)))
                .setIsShowNotification(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnIsShowNotification)) == 1)
                .setIsOnlyWifi(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnIsOnlyWifi)) == 1)
                .setIsAllowRoaming(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnIsAllowRoaming)) == 1)
                .setMimeType(cursor.getString(cursor.getColumnIndex(Downloads.ColumnMimeType)))
                .setTotalBytes(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnTotalBytes)))
                .setCurrentBytes(cursor.getString(cursor.getColumnIndex(Downloads.ColumnCurrentBytes)))
                .setReDirectUrl(cursor.getString(cursor.getColumnIndex(Downloads.ColumnReDirectUrl)))
                .setETag(cursor.getString(cursor.getColumnIndex(Downloads.ColumnETag)))
                .setLastModifyTime(cursor.getLong(cursor.getColumnIndex(Downloads.ColumnLastModifyTime)))
                .setStatus(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnStatus)))
                .setNumFailed(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnNumFailed)));
    }

    @SuppressWarnings("ResourceType")
    public static void readEntity(@NonNull Cursor cursor, @NonNull DownloadInfo entity) {
        entity.setUrl(cursor.getString(cursor.getColumnIndex(Downloads.ColumnUrl)))
                .setId(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnID)))
                .setFileName(cursor.getString(cursor.getColumnIndex(Downloads.ColumnFileName)))
                .setDescription(cursor.getString(cursor.getColumnIndex(Downloads.ColumnDescription)))
                .setFolderPath(cursor.getString(cursor.getColumnIndex(Downloads.ColumnFolderPath)))
                .setThreadNum(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnThreadNum)))
                .setGroupName(cursor.getString(cursor.getColumnIndex(Downloads.ColumnGroupName)))
                .setIsShowNotification(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnIsShowNotification)) == 1)
                .setIsOnlyWifi(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnIsOnlyWifi)) == 1)
                .setIsAllowRoaming(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnIsAllowRoaming)) == 1)
                .setMimeType(cursor.getString(cursor.getColumnIndex(Downloads.ColumnMimeType)))
                .setTotalBytes(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnTotalBytes)))
                .setCurrentBytes(cursor.getString(cursor.getColumnIndex(Downloads.ColumnCurrentBytes)))
                .setReDirectUrl(cursor.getString(cursor.getColumnIndex(Downloads.ColumnReDirectUrl)))
                .setETag(cursor.getString(cursor.getColumnIndex(Downloads.ColumnETag)))
                .setLastModifyTime(cursor.getLong(cursor.getColumnIndex(Downloads.ColumnLastModifyTime)))
                .setStatus(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnStatus)))
                .setNumFailed(cursor.getInt(cursor.getColumnIndex(Downloads.ColumnNumFailed)));
    }

}
