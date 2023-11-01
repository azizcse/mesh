package com.w3engineers.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class ContentMetaInfo implements Parcelable {

    private String messageId;
    private String metaInfo;
    private int messageType;
    private byte[] thumbData;

    public ContentMetaInfo() {

    }

    protected ContentMetaInfo(Parcel in) {
        messageId = in.readString();
        messageType = in.readInt();
        metaInfo = in.readString();
        thumbData = in.createByteArray();
    }

    public static final Creator<ContentMetaInfo> CREATOR = new Creator<ContentMetaInfo>() {
        @Override
        public ContentMetaInfo createFromParcel(Parcel in) {
            return new ContentMetaInfo(in);
        }

        @Override
        public ContentMetaInfo[] newArray(int size) {
            return new ContentMetaInfo[size];
        }
    };

    public String getMessageId() {
        return messageId;
    }

    public ContentMetaInfo setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public int getMessageType() {
        return messageType;
    }

    public ContentMetaInfo setMessageType(int messageType) {
        this.messageType = messageType;
        return this;
    }

    public String getMetaInfo() {
        return metaInfo;
    }

    public ContentMetaInfo setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
        return this;
    }

    public byte[] getThumbData() {
        return thumbData;
    }

    public ContentMetaInfo setThumbData(byte[] thumbData) {
        this.thumbData = thumbData;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(messageId);
        dest.writeInt(messageType);
        dest.writeString(metaInfo);
        dest.writeByteArray(thumbData);
    }
}
