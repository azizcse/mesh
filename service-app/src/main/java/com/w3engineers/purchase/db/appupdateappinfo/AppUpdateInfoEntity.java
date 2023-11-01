package com.w3engineers.purchase.db.appupdateappinfo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.w3engineers.purchase.db.TableInfo;


@Entity(tableName = TableInfo.TABLE_NAME)
public class AppUpdateInfoEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TableInfo.Column.ID)
    public int id;

    @ColumnInfo(name = TableInfo.Column.PACKAGE_NAME)
    public String packageName;

    @ColumnInfo(name = TableInfo.Column.MY_USER_ID)
    public String myUserId;

    @ColumnInfo(name = TableInfo.Column.RECEIVER_ID)
    public String receiverUserId;

    @ColumnInfo(name = TableInfo.Column.APP_NAME)
    public String appName;

    @ColumnInfo(name = TableInfo.Column.SELF_APP_VERSION_CODE)
    public int selfVersionCode;

    @ColumnInfo(name = TableInfo.Column.SELF_APP_VERSION_NAME)
    public String selfVersionName;

    @ColumnInfo(name = TableInfo.Column.RECEIVER_APP_VERSION_CODE)
    public int receiverVersionCode;

    @ColumnInfo(name = TableInfo.Column.RECEIVER_APP_VERSION_NAME)
    public String receiverVersionName;

    @ColumnInfo(name = TableInfo.Column.IS_CHECKING)
    public boolean isChecking;

    //default isSync is false. When data uploaded in server then isSync true
    @ColumnInfo(name = TableInfo.Column.IS_SYNC)
    public boolean isSyncs;

    /**
     * This size will be byte unit
     */
    @ColumnInfo(name = TableInfo.Column.APP_SIZE)
    public long appSize;

    @ColumnInfo(name = TableInfo.Column.TIME)
    public long timeStamp;
}
