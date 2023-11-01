package com.letbyte.core.meshfilesharing.data.model;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;
import com.letbyte.core.meshfilesharing.data.BaseFileMessage;
import com.letbyte.core.meshfilesharing.data.BuyerFileMessage;

/**
 * Created by Azizul Islam on 6/9/21.
 */
public class MultihopFileMessage extends BaseFileMessage {

    @SerializedName("tid")
    public long fileTransferId;

    @SerializedName("fn")
    public String fileName;

    @SerializedName("s")
    public String sourceAddress;

    @SerializedName("d")
    public String destinationId;

    @SerializedName("fs")
    public long fileSize;

    @SerializedName("to")
    public String appToken;

    public MultihopFileMessage( String sourceAddress, String destinationId, long fileTransferId, String fileName, long fileSize, String appToken) {
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

    protected MultihopFileMessage(Parcel in) {

    }
    public static final Creator<MultihopFileMessage> CREATOR = new Creator<MultihopFileMessage>() {
        @Override
        public MultihopFileMessage createFromParcel(Parcel in) {
            return new MultihopFileMessage(in);
        }

        @Override
        public MultihopFileMessage[] newArray(int size) {
            return new MultihopFileMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
