package com.w3engineers.purchase.db.handshaking_track;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.purchase.db.TableInfo;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;
import com.w3engineers.purchase.db.broadcast_track.BroadcastTrackEntity;

import java.util.List;

@Dao
public interface HandshakeTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(HandshakeTrackEntity handshakeTrackEntity);

    @Query("SELECT * FROM " + TableInfo.TABLE_HANDSHAKE_TRACK + " WHERE " + TableInfo.Column.HANDSHAKE_USER_ID + "=:userId")
    HandshakeTrackEntity getHandshakeTrackEntity(String userId);
}
