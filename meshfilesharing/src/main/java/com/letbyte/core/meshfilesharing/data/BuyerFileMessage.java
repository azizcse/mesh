package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

public class BuyerFileMessage extends BaseFileMessage {

    @SerializedName("id")
    public long fileTransferId;

    @SerializedName("n")
    public String fileName;

    @SerializedName("sa")
    public String sourceAddress;

    @SerializedName("r")
    public String destinationId;

    @SerializedName("s")
    public long fileSize;

    @SerializedName("to")
    public String appToken;

    public BuyerFileMessage( String sourceAddress, String destinationId, long fileTransferId, String fileName, long fileSize, String appToken) {
        this.fileTransferId = fileTransferId;
        this.fileName = fileName;
        this.sourceAddress = sourceAddress;
        this.destinationId = destinationId;
        this.fileSize = fileSize;
        this.appToken = appToken;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    protected BuyerFileMessage(Parcel in) {

    }
    public static final Creator<BuyerFileMessage> CREATOR = new Creator<BuyerFileMessage>() {
        @Override
        public BuyerFileMessage createFromParcel(Parcel in) {
            return new BuyerFileMessage(in);
        }

        @Override
        public BuyerFileMessage[] newArray(int size) {
            return new BuyerFileMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
