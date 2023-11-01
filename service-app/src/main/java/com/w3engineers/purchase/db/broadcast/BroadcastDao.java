package com.w3engineers.purchase.db.broadcast;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.mesh.db.meta.TableMeta;
import com.w3engineers.purchase.db.TableInfo;

import java.util.List;

@Dao
public interface BroadcastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(BroadcastEntity broadcastEntity);

    @Query("SELECT * FROM " + TableInfo.TABLE_BROADCAST + " WHERE " + TableInfo.Column.APP_TOKEN
            + " = :tokenName AND " + TableInfo.Column.BROADCAST_RECEIVE_STATUS + " = :status")
    List<BroadcastEntity> getUnReceivedBroadcast(String tokenName, int status);

    @Query("SELECT * FROM " + TableInfo.TABLE_BROADCAST + " WHERE " + TableInfo.Column.BROADCAST_ID
            + " = :broadcastId")
    BroadcastEntity getBroadcastEntity(String broadcastId);

    @Query("UPDATE " + TableInfo.TABLE_BROADCAST + " SET " + TableInfo.Column.BROADCAST_RECEIVE_STATUS
            + " = :status" + " WHERE " + TableInfo.Column.BROADCAST_ID + " = :broadcastId")
    void updateBroadcastEntity(String broadcastId, int status);

    @Query("UPDATE " + TableInfo.TABLE_BROADCAST + " SET " + TableInfo.Column.BROADCAST_RECEIVE_STATUS
            + " = :updateStatus" + " WHERE " + TableInfo.Column.BROADCAST_USER_ID
            + " = :userId AND " + TableInfo.Column.BROADCAST_RECEIVE_STATUS + " = :status")
    void updateBroadcastEntity(String userId, int status, int updateStatus);

    @Query("UPDATE " + TableInfo.TABLE_BROADCAST + " SET " + TableInfo.Column.BROADCAST_RECEIVE_STATUS
            + " = :updateStatus" + " WHERE " + TableInfo.Column.BROADCAST_RECEIVE_STATUS + " = :status")
    void updateBroadcastEntity(int status, int updateStatus);
}
