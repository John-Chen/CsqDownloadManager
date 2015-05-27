/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import com.csq.downloadmanager.provider.Downloads;

public class SqlUtils {

    /**
     * Get a parameterized SQL WHERE clause to select a bunch of IDs.
     */
    public static String getWhereClauseForIds(long[] ids) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                whereClause.append("OR ");
            }
            whereClause.append(Downloads.ColumnID);
            whereClause.append(" = ? ");
        }
        whereClause.append(")");
        return whereClause.toString();
    }

    /**
     * Get the selection args for a clause returned by
     * {@link #getWhereClauseForIds(long[])}.
     */
    public static String[] getWhereArgsForIds(long[] ids) {
        String[] whereArgs = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            whereArgs[i] = Long.toString(ids[i]);
        }
        return whereArgs;
    }

    public static String getSortOrder(String column, boolean asc){
        return column + (asc ? " ASC" : " DESC");
    }

}
