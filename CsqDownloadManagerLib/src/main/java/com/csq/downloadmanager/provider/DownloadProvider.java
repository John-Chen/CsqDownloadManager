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

import com.csq.downloadmanager.configer.DownloadConfiger;
import com.csq.downloadmanager.db.DownloadOpenHelper;
import com.csq.downloadmanager.db.query.Where;
import com.csq.downloadmanager.util.Helpers;
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
        Helpers.checkAccessPermission(getContext());

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

        values.put(Downloads.ColumnLastModifyTime, System.currentTimeMillis());
        long rowID = db.insert(TableUtil.TABLE_NAME, null, values);
        if (rowID == -1) {
            LogUtil.d(DownloadProvider.class, "couldn't insert into downloads database");
            return null;
        }

        getContext().startService(new Intent(getContext(), DownloadService.class));

        notifyChange(uri, match);
        DownloadConfiger.EventDispatcher.downloadInfoAdded(getContext(), new long[]{rowID});

        return ContentUris.withAppendedId(Downloads.CONTENT_URI, rowID);
    }

    @Override
    public int delete(Uri uri,
                      String selection,
                      String[] selectionArgs) {
        LogUtil.d(DownloadProvider.class, "delete uri = " + uri.toString() + ", "
            + "selection = " + selection + ", "
            + "selectionArgs = " + selectionArgs);

        long[] ids = queryIds(uri,
                selection,
                selectionArgs);
        if(ids == null || ids.length < 1){
            return 0;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String ws = Where.create().in(Downloads.ColumnID, ids).getSelection();
        int count = db.delete(TableUtil.TABLE_NAME, ws, null);

        int match = sURIMatcher.match(uri);
        notifyChange(uri, match);

        DownloadConfiger.EventDispatcher.downloadInfoRemoved(getContext(), ids);

        return count;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs) {
        LogUtil.d(DownloadProvider.class, "update uri = " + uri.toString() + ", "
                + "values = " + values.toString()
                + "selection = " + selection + ", "
                + "selectionArgs = " + selectionArgs);

        long[] ids = queryIds(uri,
                selection,
                selectionArgs);
        if(ids == null || ids.length < 1){
            return 0;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        values.put(Downloads.ColumnLastModifyTime, System.currentTimeMillis());
        String ws = Where.create().in(Downloads.ColumnID, ids).getSelection();
        int count = db.update(TableUtil.TABLE_NAME,
                values,
                ws,
                null);

        int match = sURIMatcher.match(uri);
        notifyChange(uri, match);

        if (values.containsKey(Downloads.ColumnStatus)) {
            getContext().startService(new Intent(getContext(), DownloadService.class));
        }

        DownloadConfiger.EventDispatcher.downloadInfoChanged(getContext(), ids, values);

        return count;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {

        LogUtil.d(DownloadProvider.class, "query uri = " + uri.toString() + ", "
                + "selection = " + selection + ", "
                + "selectionArgs = " + selectionArgs + ", "
                + "sortOrder = " + sortOrder);

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

    private long[] queryIds(Uri uri,
                            String selection,
                            String[] selectionArgs){
        Cursor cursor = query(uri, new String[]{Downloads.ColumnID}, selection, selectionArgs, null);
        if(cursor == null || cursor.getCount() < 1){
            return null;
        }
        long[] ids = new long[cursor.getCount()];
        int i = 0;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
            ids[i++] = cursor.getLong(0);
        }
        return ids;
    }

    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
