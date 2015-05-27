/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.csq.downloadmanager.util.TableUtil;

public class DownloadOpenHelper extends SQLiteOpenHelper {


    // ------------------------ Constants ------------------------
    public static final String dbName = "CsqDownload.db";
    public static final int dbVersion = 1;


    // ------------------------- Fields --------------------------
    private volatile static DownloadOpenHelper instance;

    public static DownloadOpenHelper getInstace(Context context) {
        synchronized (DownloadOpenHelper.class) {
            if (instance == null) {
                instance = new DownloadOpenHelper(context);
            }
        }
        return instance;
    }


    // ----------------------- Constructors ----------------------

    private DownloadOpenHelper(Context context) {
        super(context, dbName, null, dbVersion);
    }

    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public void onCreate(SQLiteDatabase db) {
        TableUtil.dropAndCreateTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgradeDb(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        downgradeDb(db, oldVersion, newVersion);
    }

    // --------------------- Methods public ----------------------


    // --------------------- Methods private ---------------------

    private void upgradeDb(SQLiteDatabase db, int oldVersion, int newVersion){
        while (oldVersion < newVersion){
            switch (++oldVersion){
                case 2:

                    break;

                case 3:

                    break;

                //......

            }
        }
    }

    private void downgradeDb(SQLiteDatabase db, int oldVersion, int newVersion){
        TableUtil.dropAndCreateTable(db);
    }


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
