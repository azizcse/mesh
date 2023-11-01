package com.w3engineers.purchase.db.appupdateappinfo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;


import com.w3engineers.purchase.db.TableInfo;

import java.util.List;

@Dao
public interface AppUpdateInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAppUpdateInfo(AppUpdateInfoEntity entity);

    @Query("SELECT * FROM " + TableInfo.TABLE_NAME + " WHERE " + TableInfo.Column.IS_SYNC +
            " = 0 ORDER BY " + TableInfo.Column.TIME + " ASC")
    List<AppUpdateInfoEntity> getAllAPpUpdateInfo();

    @Query("SELECT * FROM " + TableInfo.TABLE_NAME + " WHERE " + TableInfo.Column.MY_USER_ID +
            " LIKE :myUserId AND " + TableInfo.Column.RECEIVER_ID + " LIKE :receiverId AND "
            + TableInfo.Column.PACKAGE_NAME + " LIKE :appToken " +
            " ORDER BY " + TableInfo.Column.TIME + " DESC LIMIT 1")
    AppUpdateInfoEntity getCurrentAppCheckingInfo(String myUserId, String receiverId, String appToken);

    @Query("UPDATE " + TableInfo.TABLE_NAME + " SET " + TableInfo.Column.IS_SYNC + " = 1 WHERE "
            + TableInfo.Column.ID + " = :id")
    void updateSyncedAppUpdateInformation(int id);

    @Query("DELETE FROM " + TableInfo.TABLE_NAME + " WHERE " + TableInfo.Column.ID + " = :id")
    void deleteAppUpdateInfo(int id);

}
