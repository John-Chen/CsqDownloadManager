/**
 * description : 构造查询语句，eg:((name = 'foo' AND passwd = '_secret') OR (name = 'bar' AND passwd = 'qwerty'))
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.db.query;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Where implements IWhere{

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------

    private StringBuilder stringBuilder = new StringBuilder();


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------

    @Override
    public String getSelection() {
        return stringBuilder.toString();
    }

    // --------------------- Methods public ----------------------

    public static Where create(){
        return new Where();
    }

    /**
     * Add a '=' clause so the column must be equal to the value.
     */
    public Where eq(@NonNull String columnName, @NonNull Object value) {
        stringBuilder.append(columnName + " = " + "'" + value + "'");
        return this;
    }

    /**
     * Add a '!=' clause so the column must be not-equal-to the value.
     */
    public Where ne(@NonNull String columnName, @NonNull Object value) {
        stringBuilder.append(columnName + " != " + "'" + value + "'");
        return this;
    }

    /**
     * Add a '>=' clause so the column must be greater-than or equals-to the value.
     */
    public Where ge(@NonNull String columnName, @NonNull Object value){
        stringBuilder.append(columnName + " >= " + "'" + value + "'");
        return this;
    }

    /**
     * Add a '>' clause so the column must be greater-than the value.
     */
    public Where gt(@NonNull String columnName, @NonNull Object value) {
        stringBuilder.append(columnName + " > " + "'" + value + "'");
        return this;
    }

    /**
     * Add a '<=' clause so the column must be less-than or equals-to the value.
     */
    public Where le(@NonNull String columnName, @NonNull Object value) {
        stringBuilder.append(columnName + " <= " + "'" + value + "'");
        return this;
    }

    /**
     * Add a '<' clause so the column must be less-than the value.
     */
    public Where lt(@NonNull String columnName, @NonNull Object value) {
        stringBuilder.append(columnName + " < " + "'" + value + "'");
        return this;
    }

    /**
     * Add a LIKE clause so the column must mach the value using '%' patterns.
     */
    public Where like(@NonNull String columnName, @NonNull Object value) {
        stringBuilder.append(columnName + " like " + "'" + value + "'");
        return this;
    }


    /**
     * Add a BETWEEN clause so the column must be between the low and high parameters.
     */
    public Where between(@NonNull String columnName, @NonNull Object low, @NonNull Object high){
        stringBuilder.append(columnName + " between " + low + " and " + high);
        return this;
    }

    /**
     * Add a IN clause so the column must be equal-to one of the objects from the list passed in.
     * eg : id  in  (‘13’, ‘9’, ‘87’);
     */
    public Where in(@NonNull String columnName, @NonNull Iterable<?> objects) {
        Iterator<?> it = objects.iterator();
        if(!it.hasNext()){
            return this;
        }
        stringBuilder.append(columnName + " in " + "(");
        while (it.hasNext()){
            Object next = it.next();
            Class clz = next.getClass().getComponentType();

            if(clz == Long.class || clz == long.class){
                stringBuilder.append("'" + (Long)next + "',");

            }else if(clz == Integer.class || clz == int.class){
                stringBuilder.append("'" + (Integer)next + "',");

            }else if(clz == String.class){
                stringBuilder.append("'" + (String)next + "',");

            }else if(clz == Boolean.class || clz == boolean.class){
                stringBuilder.append("'" + ((Boolean)next ? "1" : "0") + "',");

            }else{
                stringBuilder.append("'" + next + "',");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(")");
        return this;
    }

    /**
     * Same as {@link #in(String, Iterable)} except with a NOT IN clause.
     */
    public Where notIn(@NonNull String columnName, @NonNull Iterable<?> objects) {
        Iterator<?> it = objects.iterator();
        if(!it.hasNext()){
            return this;
        }
        stringBuilder.append(columnName + " not in " + "(");
        while (it.hasNext()){
            Object next = it.next();
            Class clz = next.getClass().getComponentType();

            if(clz == Long.class || clz == long.class){
                stringBuilder.append("'" + (Long)next + "',");

            }else if(clz == Integer.class || clz == int.class){
                stringBuilder.append("'" + (Integer)next + "',");

            }else if(clz == String.class){
                stringBuilder.append("'" + (String)next + "',");

            }else if(clz == Boolean.class || clz == boolean.class){
                stringBuilder.append("'" + ((Boolean)next ? "1" : "0") + "',");

            }else{
                stringBuilder.append("'" + next + "',");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append(")");
        return this;
    }

    /**
     * Add a IN clause so the column must be equal-to one of the objects passed in.
     */
    public Where in(@NonNull String columnName, @NonNull Object[] objects) {
        List<Object> os = new ArrayList<>(objects.length);
        for(Object o : objects){
            os.add(o);
        }
        return in(columnName, os);
    }

    /**
     * Same as {@link #in(String, Object...)} except with a NOT IN clause.
     */
    public Where notIn(@NonNull String columnName, @NonNull Object[] objects) {
        List<Object> os = new ArrayList<>(objects.length);
        for(Object o : objects){
            os.add(o);
        }
        return notIn(columnName, os);
    }

    public Where in(@NonNull String columnName, @NonNull int[] objects) {
        List<Object> ints = new ArrayList<>(objects.length);
        for(Integer i : objects){
            ints.add(i);
        }
        return in(columnName, ints);
    }

    public Where notIn(@NonNull String columnName, @NonNull int[] objects) {
        List<Object> ints = new ArrayList<>(objects.length);
        for(Integer i : objects){
            ints.add(i);
        }
        return notIn(columnName, ints);
    }

    public Where in(@NonNull String columnName, @NonNull long[] objects) {
        List<Object> longs = new ArrayList<>(objects.length);
        for(Long i : objects){
            longs.add(i);
        }
        return in(columnName, longs);
    }

    public Where notIn(@NonNull String columnName, @NonNull long[] objects) {
        List<Object> longs = new ArrayList<>(objects.length);
        for(Long i : objects){
            longs.add(i);
        }
        return notIn(columnName, longs);
    }

    public Where in(@NonNull String columnName, @NonNull boolean[] objects) {
        List<Object> bools = new ArrayList<>(objects.length);
        for(Boolean i : objects){
            bools.add(i);
        }
        return in(columnName, bools);
    }

    public Where notIn(@NonNull String columnName, @NonNull boolean[] objects) {
        List<Object> bools = new ArrayList<>(objects.length);
        for(Boolean i : objects){
            bools.add(i);
        }
        return notIn(columnName, bools);
    }

    /**
     * Add a 'IS NULL' clause so the column must be null. '=' NULL does not work.
     */
    public Where isNull(@NonNull String columnName) {
        stringBuilder.append(columnName + " is null");
        return this;
    }

    /**
     * Add a 'IS NOT NULL' clause so the column must not be null. '&lt;&gt;' NULL does not work.
     */
    public Where isNotNull(@NonNull String columnName) {
        stringBuilder.append(columnName + " is not null");
        return this;
    }

    /**
     * Reset the Where object so it can be re-used.
     */
    public Where reset() {
        stringBuilder = new StringBuilder();
        return this;
    }


    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
