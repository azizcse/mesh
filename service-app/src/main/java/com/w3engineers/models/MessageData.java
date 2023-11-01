package com.w3engineers.models;


import android.os.Parcel;
import android.os.Parcelable;

public class MessageData implements Parcelable {
    private String receiverID;
    private String senderID;
    private String messageID;
    private byte[] msgData;
    private boolean isNotificationNeeded;
    private String appToken;

    public String getReceiverID() {
        return receiverID;
    }

    public MessageData setReceiverID(String receiverID) {
        this.receiverID = receiverID;
        return this;
    }

    public String getSenderID() {
        return senderID;
    }

    public MessageData setSenderID(String senderID) {
        this.senderID = senderID;
        return this;
    }

    public String getMessageID() {
        return messageID;
    }

    public MessageData setMessageID(String messageID) {
        this.messageID = messageID;
        return this;
    }

    public byte[] getMsgData() {
        return msgData;
    }

    public MessageData setMsgData(byte[] msgData) {
        this.msgData = msgData;
        return this;
    }

    public boolean isNotificationNeeded() {
        return isNotificationNeeded;
    }

    public MessageData setNotificationNeeded(boolean notificationNeeded) {
        isNotificationNeeded = notificationNeeded;
        return this;
    }

    public String getAppToken() {
        return appToken;
    }

    public MessageData setAppToken(String appToken) {
        this.appToken = appToken;
        return this;
    }

    public MessageData(){
    }

    protected MessageData(Parcel in) {
        receiverID = in.readString();
        senderID = in.readString();
        messageID = in.readString();
        msgData = in.createByteArray();
        appToken = in.readString();
        isNotificationNeeded = in.readByte() != 0;
    }


    public static final Creator<MessageData> CREATOR = new Creator<MessageData>() {
        @Override
        public MessageData createFromParcel(Parcel in) {
            return new MessageData(in);
        }

        @Override
        public MessageData[] newArray(int size) {
            return new MessageData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(receiverID);
        parcel.writeString(senderID);
        parcel.writeString(messageID);
        parcel.writeByteArray(msgData);
        parcel.writeString(appToken);
        parcel.writeByte((byte) (isNotificationNeeded ? 1 : 0));
    }
}
