package com.w3engineers.purchase.db.handshaking_track;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.w3engineers.purchase.db.TableInfo;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;


@Entity(tableName = TableInfo.TABLE_HANDSHAKE_TRACK,
        indices = {@Index(value = {TableInfo.Column.HANDSHAKE_USER_ID}, unique = true)})
public class HandshakeTrackEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TableInfo.Column.ID)
    public int id;

    @ColumnInfo(name = TableInfo.Column.HANDSHAKE_USER_ID)
    public String userId;

    @ColumnInfo(name = TableInfo.Column.HANDSHAKING_DATA)
    public String handshakeData;

    @ColumnInfo(name = TableInfo.Column.HANDSHAKING_TIME)
    public long handshakeTime;
}
