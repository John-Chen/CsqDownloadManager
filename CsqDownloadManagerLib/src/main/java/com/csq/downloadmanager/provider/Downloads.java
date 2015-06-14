/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.provider;

import android.net.Uri;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Downloads {

    // ------------------------ Constants ------------------------
    /**
     * DownloadProvider authority
     */
    public static final String AUTHORITY
            = "com.csq.downloadmanager.provider";

    /**
     * The permission to access the download manager
     */
    public static final String PERMISSION_ACCESS
            = "com.csq.permission.ACCESS_DOWNLOAD_MANAGER";

    /**
     * The content:// URI for the data table in the provider
     * @hide
     */
    public static final Uri CONTENT_URI
            = Uri.parse("content://" + AUTHORITY + "/downloads");



    /**
     * Type: int
     */
    public static final String ColumnID = "id";

    /**
     * Type: String
     */
    public static final String ColumnFileName = "fileName";

    /**
     * Type: String
     */
    public static final String ColumnDescription = "description";

    /**
     * Type: String
     */
    public static final String ColumnUrl = "url";

    /**
     * Type: String
     */
    public static final String ColumnFolderPath = "folderPath";

    /**
     * Type: int
     */
    public static final String ColumnThreadNum = "threadNum";

    /**
     * Type: String
     */
    public static final String ColumnGroupName = "groupName";

    /**
     * Type: boolean
     */
    public static final String ColumnIsShowNotification = "isShowNotification";

    /**
     * Type: boolean
     */
    public static final String ColumnIsOnlyWifi = "isOnlyWifi";

    /**
     * Type: boolean
     */
    public static final String ColumnIsAllowRoaming = "isAllowRoaming";

    /**
     * Type: String
     */
    public static final String ColumnMimeType = "mimeType";

    /**
     * Type: long
     */
    public static final String ColumnTotalBytes = "totalBytes";

    /**
     * Type: long
     */
    public static final String ColumnCurrentBytes = "currentBytes";

    /**
     * Type: String
     */
    public static final String ColumnReDirectUrl = "reDirectUrl";

    /**
     * Type: String
     */
    public static final String ColumnETag = "eTag";

    /**
     * Type: long
     */
    public static final String ColumnLastModifyTime = "lastModifyTime";

    /**
     * Type: int
     */
    public static final String ColumnStatus = "status";

    /**
     * Type: int
     */
    public static final String ColumnNumFailed = "numFailed";

    /**
     * Type: lont
     */
    public static final String ColumnRetryAfterTime = "retryAfterTime";


    public static final Map<String, String> ProjectionMap = new HashMap<String, String>();
    static {
        ProjectionMap.put(ColumnID, ColumnID);
        ProjectionMap.put(ColumnFileName, ColumnFileName);
        ProjectionMap.put(ColumnDescription, ColumnDescription);
        ProjectionMap.put(ColumnUrl, ColumnUrl);
        ProjectionMap.put(ColumnFolderPath, ColumnFolderPath);
        ProjectionMap.put(ColumnThreadNum, ColumnThreadNum);
        ProjectionMap.put(ColumnGroupName, ColumnGroupName);
        ProjectionMap.put(ColumnIsShowNotification, ColumnIsShowNotification);
        ProjectionMap.put(ColumnIsAllowRoaming, ColumnIsAllowRoaming);
        ProjectionMap.put(ColumnIsOnlyWifi, ColumnIsOnlyWifi);
        ProjectionMap.put(ColumnMimeType, ColumnMimeType);
        ProjectionMap.put(ColumnTotalBytes, ColumnTotalBytes);
        ProjectionMap.put(ColumnCurrentBytes, ColumnCurrentBytes);
        ProjectionMap.put(ColumnReDirectUrl, ColumnReDirectUrl);
        ProjectionMap.put(ColumnETag, ColumnETag);
        ProjectionMap.put(ColumnLastModifyTime, ColumnLastModifyTime);
        ProjectionMap.put(ColumnStatus, ColumnStatus);
        ProjectionMap.put(ColumnNumFailed, ColumnNumFailed);
        ProjectionMap.put(ColumnRetryAfterTime, ColumnRetryAfterTime);
    }

    public static final Map<String, Integer> AllProjectionMap = new LinkedHashMap<String, Integer>(18);
    static {
        AllProjectionMap.put(ColumnID, 0);
        AllProjectionMap.put(ColumnFileName, 1);
        AllProjectionMap.put(ColumnDescription, 2);
        AllProjectionMap.put(ColumnUrl, 3);
        AllProjectionMap.put(ColumnFolderPath, 4);
        AllProjectionMap.put(ColumnThreadNum, 5);
        AllProjectionMap.put(ColumnGroupName, 6);
        AllProjectionMap.put(ColumnIsShowNotification, 7);
        AllProjectionMap.put(ColumnIsAllowRoaming, 8);
        AllProjectionMap.put(ColumnIsOnlyWifi, 9);
        AllProjectionMap.put(ColumnMimeType, 10);
        AllProjectionMap.put(ColumnTotalBytes, 11);
        AllProjectionMap.put(ColumnCurrentBytes, 12);
        AllProjectionMap.put(ColumnReDirectUrl, 13);
        AllProjectionMap.put(ColumnETag, 14);
        AllProjectionMap.put(ColumnLastModifyTime, 15);
        AllProjectionMap.put(ColumnStatus, 16);
        AllProjectionMap.put(ColumnNumFailed, 17);
        AllProjectionMap.put(ColumnRetryAfterTime, 18);
    };

    /**
     * 默认排序常量，按lastModifyTime排序
     */
    public static final String  DEFAULT_SORT_ORDER  = "lastModifyTime DESC";

    // ------------------------- Fields --------------------------


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------


    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------


    // --------------------- logical fragments -----------------

}
