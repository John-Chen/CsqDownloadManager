/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import com.csq.downloadmanager.db.DownloadOpenHelper;
import com.csq.downloadmanager.util.TableUtil;
import com.csq.downloadmanager.service.DownloadService;
import com.csq.downloadmanager.util.LogUtil;

public class DownloadProvider extends ContentProvider {


    // ------------------------ Constants ------------------------
    private static final int MatchCode = 1, MatchCodeItem = 2;
    /**
     * MIME type for an individual download
     */
    private static final String MIME_TYPE = "vnd.android.cursor.dir/csqdownload";
    /**
     * MIME type for an individual download
     */
    private static final String MIME_TYPE_ITEM = "vnd.android.cursor.item/csqdownload";


    // ------------------------- Fields --------------------------

    private DownloadOpenHelper mOpenHelper;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static{
        sURIMatcher.addURI(Downloads.AUTHORITY, "csq_downloads", MatchCode);
        sURIMatcher.addURI(Downloads.AUTHORITY, "csq_downloads/#", MatchCodeItem);
    }


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public boolean onCreate() {
        mOpenHelper = DownloadOpenHelper.getInstace(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case MatchCode: {
                return MIME_TYPE;
            }
            case MatchCodeItem: {
                return MIME_TYPE_ITEM;
            }
            default: {
                LogUtil.d(DownloadProvider.class, "calling getType on an unknown URI: " + uri);
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        checkAccessPermission();

        int match = sURIMatcher.match(uri);
        if (match != MatchCode) {
            LogUtil.d(DownloadProvider.class, "calling insert on an unknown/invalid URI: " + uri);
            throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }

        if(!values.containsKey(Downloads.ColumnUrl)
                || values.get(Downloads.ColumnUrl) == null
                || TextUtils.isEmpty("" + values.get(Downloads.ColumnUrl))){
            return null;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowID = db.insert(TableUtil.TABLE_NAME, null, values);
        if (rowID == -1) {
            LogUtil.d(DownloadProvider.class, "couldn't insert into downloads database");
            return null;
        }

        getContext().startService(new Intent(getContext(), DownloadService.class));

        notifyChange(uri, match);

        return ContentUris.withAppendedId(Downloads.CONTENT_URI, rowID);
    }

    @Override
    public int delete(Uri uri,
                      String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        int match = sURIMatcher.match(uri);
        switch (match) {
            case MatchCode:
                count = db.delete(TableUtil.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;

            case MatchCodeItem:
                String rowId = getDownloadIdFromUri(uri);
                count = db.delete(TableUtil.TABLE_NAME,
                        Downloads.ColumnID + "=" + rowId + (selection.length() > 0 ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;

            default:
                LogUtil.d(DownloadProvider.class, "deleting unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot delete URI: " + uri);
        }

        notifyChange(uri, match);

        return count;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs) {
        int count = 0;

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int match = sURIMatcher.match(uri);
        switch (match) {
            case MatchCode:
                count = db.update(TableUtil.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;

            case MatchCodeItem:
                String rowId = getDownloadIdFromUri(uri);
                count = db.update(TableUtil.TABLE_NAME,
                        values,
                        Downloads.ColumnID + "=" + rowId + (selection.length() > 0 ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;

            default:
                LogUtil.d(DownloadProvider.class, "updating unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }

        notifyChange(uri, match);
        if (values.containsKey(Downloads.ColumnStatus)) {
            getContext().startService(new Intent(getContext(), DownloadService.class));
        }

        return count;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = sURIMatcher.match(uri);
        switch (match) {
            case MatchCode:
                qb.setTables(TableUtil.TABLE_NAME);
                qb.setProjectionMap(Downloads.ProjectionMap);
                break;

            case MatchCodeItem:
                qb.setTables(TableUtil.TABLE_NAME);
                qb.setProjectionMap(Downloads.ProjectionMap);
                qb.appendWhere(Downloads.ColumnID + "=" + getDownloadIdFromUri(uri));
                break;

            default:
                LogUtil.d(DownloadProvider.class, "updating unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }

        //排序方式
        String orderBy;
        if (sortOrder == null) {
            orderBy = Downloads.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        //获取数据库实例
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        //返回游标集合
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    // --------------------- Methods public ----------------------


    // --------------------- Methods private ---------------------

    private void checkAccessPermission(){
        // 检查自己或者其它调用者是否有 permission 权限
        getContext().enforceCallingOrSelfPermission(
                Downloads.PERMISSION_ACCESS,
                Downloads.PERMISSION_ACCESS + " permission is required to use the download manager");
    }

    private String getDownloadIdFromUri(final Uri uri) {
        return uri.getPathSegments().get(1);
    }

    private void notifyChange(Uri uri, int match){
        long downId = 0;
        if(match == MatchCodeItem){
            downId = Long.parseLong(getDownloadIdFromUri(uri));
        }
        getContext().getContentResolver().notifyChange(
                ContentUris.withAppendedId(Downloads.CONTENT_URI, downId), null);
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
