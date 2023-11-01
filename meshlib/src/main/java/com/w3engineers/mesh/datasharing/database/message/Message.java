package com.w3engineers.mesh.datasharing.database.message;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.w3engineers.mesh.db.routing.RoutingEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Message Entity
 */
@Entity(indices = {@Index(value = {"messageId"},
        unique = true)})
public class Message implements Parcelable {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int pid;

    @ColumnInfo(name = "senderId")
    public String senderId;

    @ColumnInfo(name = "receiverId")
    public String receiverId;


    @ColumnInfo(name = "messageId")
    public String messageId;

    @ColumnInfo(name = "data")
    public byte[] data;

    @ColumnInfo(name = "is_incoming")
    public boolean isIncoming;

    @ColumnInfo(name = "app_token")
    public String appToken;

    @Ignore
    public Queue<RoutingEntity> reachablePathQueue = new LinkedList<>();

    public Message(String senderId, String receiverId, String messageId, List<RoutingEntity> entityList, byte[] data) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageId = messageId;
        reachablePathQueue.addAll(entityList);
        this.data = data;
    }

    public Message() {

    }

    protected Message(Parcel in) {
        pid = in.readInt();
        senderId = in.readString();
        receiverId = in.readString();
        messageId = in.readString();
        data = in.createByteArray();
        isIncoming = in.readByte() != 0;
        appToken = in.readString();
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public byte[] getData() {
        return data;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pid);
        dest.writeString(senderId);
        dest.writeString(receiverId);
        dest.writeString(messageId);
        dest.writeByteArray(data);
        dest.writeByte((byte) (isIncoming ? 1 : 0));
        dest.writeString(appToken);
    }
}
