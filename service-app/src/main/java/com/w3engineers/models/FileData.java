package com.w3engineers.models;

import android.os.Parcel;
import android.os.Parcelable;

public class FileData implements Parcelable {
    private String receiverID;
    private String senderID;
    private String sourceAddress;
    private String filePath;
    private byte[] msgMetaData;
    private String appToken;
    private String contentId;
    private int appVersion;
    private String versionName;
    private String appSize;
    private String fileTransferId;


    public String getReceiverID() {
        return receiverID;
    }

    public FileData setReceiverID(String receiverID) {
        this.receiverID = receiverID;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileData setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public byte[] getMsgMetaData() {
        return msgMetaData;
    }

    public FileData setMsgMetaData(byte[] msgMetaData) {
        this.msgMetaData = msgMetaData;
        return this;
    }

    public String getAppToken() {
        return appToken;
    }

    public FileData setAppToken(String appToken) {
        this.appToken = appToken;
        return this;
    }

    public String getContentId() {
        return contentId;
    }

    public FileData setContentId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public FileData setAppVersion(int appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public String getVersionName() {
        return versionName;
    }

    public FileData setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public String getAppSize() {
        return appSize;
    }

    public FileData setAppSize(String appSize) {
        this.appSize = appSize;
        return this;
    }

    public String getSenderID() {
        return senderID;
    }

    public FileData setSenderID(String senderID) {
        this.senderID = senderID;
        return this;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public FileData setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
        return this;
    }

    public String getFileTransferId() {
        return fileTransferId;
    }

    public FileData setFileTransferId(String fileTransferId) {
        this.fileTransferId = fileTransferId;
        return this;
    }

    public static Creator<FileData> getCREATOR() {
        return CREATOR;
    }

    public FileData() {

    }

    protected FileData(Parcel in) {
        receiverID = in.readString();
        senderID = in.readString();
        sourceAddress = in.readString();
        filePath = in.readString();
        //  in.readByteArray(new byte[in.readInt()]);
        msgMetaData = in.createByteArray();
        appToken = in.readString();
        contentId = in.readString();
        appVersion = in.readInt();
        versionName = in.readString();
        appSize = in.readString();
        fileTransferId = in.readString();
    }

    public static final Creator<FileData> CREATOR = new Creator<FileData>() {
        @Override
        public FileData createFromParcel(Parcel in) {
            return new FileData(in);
        }

        @Override
        public FileData[] newArray(int size) {
            return new FileData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(receiverID);
        parcel.writeString(senderID);
        parcel.writeString(sourceAddress);
        parcel.writeString(filePath);
        parcel.writeByteArray(msgMetaData);
        parcel.writeString(appToken);
        parcel.writeString(contentId);
        parcel.writeInt(appVersion);
        parcel.writeString(versionName);
        parcel.writeString(appSize);
        parcel.writeString(fileTransferId);
    }
}
