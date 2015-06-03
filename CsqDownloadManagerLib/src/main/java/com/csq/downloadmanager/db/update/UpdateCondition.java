/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.db.update;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.csq.downloadmanager.db.query.IWhere;

public class UpdateCondition {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------
    private ContentValues contentValues = new ContentValues();
    private IWhere where;

    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    public static UpdateCondition create(){
        return new UpdateCondition();
    }

    public UpdateCondition addColumn(@NonNull String columnName, @Nullable byte[] value){
        if(value == null){
            contentValues.putNull(columnName);
        }else{
            contentValues.put(columnName, value);
        }
        return this;
    }

    public UpdateCondition addColumn(@NonNull String columnName, Object value){
        if(value == null){
            contentValues.putNull(columnName);
        }else{
            contentValues.put(columnName, value.toString());
        }
        return this;
    }

    public UpdateCondition setWhere(IWhere where) {
        this.where = where;
        return this;
    }

    public String getWhereClause() {
        if(where != null){
            return where.getSelection();
        }
        return null;
    }

    public String[] getWhereArgs() {
        return null;
    }

    public ContentValues getContentValues() {
        return contentValues;
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
