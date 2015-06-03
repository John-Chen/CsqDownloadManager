/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.db.query;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OrWhere implements IWhere {


    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    private final List<IWhere> wheres = new ArrayList<>();


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public String getSelection() {
        if(wheres.size() == 1){
            return wheres.get(0).getSelection();
        }
        StringBuffer sb = new StringBuffer();
        IWhere w = null;
        for(int i = 0, num = wheres.size(); i < num; i++){
            w = wheres.get(i);
            if(i != 0){
                sb.append(" or ");
            }
            sb.append("(" + w.getSelection() + ")");
        }
        return sb.toString();
    }

    // --------------------- Methods public ----------------------

    public static OrWhere create(){
        return new OrWhere();
    }

    public OrWhere or(@NonNull IWhere w){
        if(!TextUtils.isEmpty(w.getSelection())){
            wheres.add(w);
        }
        return this;
    }

    public OrWhere or(@NonNull IWhere... ws){
        if(ws.length < 1){
            return this;
        }

        for(IWhere w : ws){
            if(!TextUtils.isEmpty(w.getSelection())){
                wheres.add(w);
            }
        }
        return this;
    }

    public OrWhere or(@NonNull Iterable<IWhere> ws){
        Iterator<IWhere> it = ws.iterator();
        IWhere w = null;
        if(it.hasNext()){
            w = it.next();
            if(!TextUtils.isEmpty(w.getSelection())){
                wheres.add(w);
            }
        }
        return this;
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
