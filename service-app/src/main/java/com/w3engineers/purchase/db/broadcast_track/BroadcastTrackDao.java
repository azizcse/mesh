package com.w3engineers.purchase.db.broadcast_track;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.mesh.util.Utils;
import com.w3engineers.purchase.db.TableInfo;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;

import java.util.List;

@Dao
public interface BroadcastTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(BroadcastTrackEntity broadcastTrackEntity);

    @Query("SELECT * FROM " + TableInfo.TABLE_BROADCAST + " WHERE " + TableInfo.Column.BROADCAST_ID
            + " NOT IN (SELECT " + TableInfo.Column.BROADCAST_MESSAGE_ID + " FROM "
            + TableInfo.TABLE_BROADCAST_TRACK + " WHERE " + TableInfo.Column.BROADCAST_TRACK_USER_ID
            + " = :userId) AND " + TableInfo.Column.APP_TOKEN + " = :appToken")
    List<BroadcastEntity> getUnsentBroadcast(String userId, String appToken);

    @Query("SELECT " + TableInfo.Column.BROADCAST_ID + " FROM " + TableInfo.TABLE_BROADCAST + " WHERE "
            + TableInfo.Column.BROADCAST_ID + " NOT IN (SELECT " + TableInfo.Column.BROADCAST_MESSAGE_ID
            + " FROM " + TableInfo.TABLE_BROADCAST_TRACK + " WHERE " + TableInfo.Column.BROADCAST_TRACK_USER_ID
            + " = :userId) AND " + TableInfo.Column.APP_TOKEN + " = :appToken")
    List<String> getUnsentBroadcastIds(String userId, String appToken);

    @Query("SELECT " + TableInfo.Column.BROADCAST_ID + " FROM " + TableInfo.TABLE_BROADCAST + " WHERE "
            + TableInfo.Column.BROADCAST_ID + " IN (:ids) AND " + TableInfo.Column.APP_TOKEN + " = :appToken AND "
            + TableInfo.Column.BROADCAST_RECEIVE_STATUS + " != " + Utils.BroadcastReceiveStatus.FAILED)
    List<String> receivedBroadcastIds(List<String> ids, String appToken);

    @Query("SELECT * FROM " + TableInfo.TABLE_BROADCAST + " WHERE " + TableInfo.Column.BROADCAST_ID + " IN (:ids)")
    List<BroadcastEntity> getUnsentBroadcast(List<String> ids);

    @Query("DELETE FROM " + TableInfo.TABLE_BROADCAST_TRACK + " WHERE " + TableInfo.Column.BROADCAST_MESSAGE_ID
            + " = :broadcastId AND " + TableInfo.Column.BROADCAST_TRACK_USER_ID + " = :userId")
    void deleteTrackEntity(String broadcastId, String userId);

    @Query("DELETE FROM " + TableInfo.TABLE_BROADCAST_TRACK + " WHERE " + TableInfo.Column.BROADCAST_TRACK_USER_ID
            + " = :userId AND " + TableInfo.Column.BROADCAST_SEND_STATUS + " = :status")
    void deleteTrackDuringDisconnect(String userId, int status);

    @Query("DELETE FROM " + TableInfo.TABLE_BROADCAST_TRACK + " WHERE "
            + TableInfo.Column.BROADCAST_SEND_STATUS + " = :status")
    void deleteTrackDuringMyDisconnection(int status);

    @Query("SELECT * FROM " + TableInfo.TABLE_BROADCAST_TRACK +
            " WHERE " + TableInfo.Column.BROADCAST_MESSAGE_ID + "=:broadcastId AND " +
            TableInfo.Column.BROADCAST_TRACK_USER_ID + " = :receiver")
    BroadcastTrackEntity getBroadcastEntityByIdAndReceiver(String broadcastId, String receiver);
}
