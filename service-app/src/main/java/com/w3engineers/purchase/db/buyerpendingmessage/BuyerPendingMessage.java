package com.w3engineers.purchase.db.buyerpendingmessage;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class BuyerPendingMessage {
//    @NonNull
//    @PrimaryKey(autoGenerate = true)
//    public int id;

    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "owner")
    public String owner;

    @ColumnInfo(name = "msg_data")
    public String msgData;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "msg_id")
    public String msgId;

    @ColumnInfo(name = "data_size")
    public long dataSize;

    @ColumnInfo(name = "status")
    public int status;

    @ColumnInfo(name = "comment")
    public String comment;

    @ColumnInfo(name = "create_time")
    public long createTime;

    @ColumnInfo(name = "update_time")
    public long updateTime;

    @ColumnInfo(name = "is_incomming")
    public boolean isIncomming;

    @ColumnInfo(name = "is_file")
    public boolean isFile;
}

