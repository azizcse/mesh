package com.w3engineers.purchase.db.broadcast_track;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.w3engineers.purchase.db.TableInfo;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;


@Entity(tableName = TableInfo.TABLE_BROADCAST_TRACK,
        indices = {@Index(value = {TableInfo.Column.BROADCAST_MESSAGE_ID,
                TableInfo.Column.BROADCAST_TRACK_USER_ID}, unique = true)},
        foreignKeys = @ForeignKey(entity = BroadcastEntity.class,
                parentColumns = TableInfo.Column.BROADCAST_ID,
                childColumns = TableInfo.Column.BROADCAST_MESSAGE_ID))
public class BroadcastTrackEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TableInfo.Column.ID)
    public int id;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_MESSAGE_ID)
    public String broadcastMessageId;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_TRACK_USER_ID)
    public String broadcastTrackUserId;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_SEND_STATUS)
    public int broadcastSendStatus;
}
