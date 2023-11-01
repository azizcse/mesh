package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;
import com.letbyte.core.meshfilesharing.core.BuyerFileManager;

public class BuyerFileAck extends BaseFileMessage{

    @SerializedName("id")
    public String fileMessageId;

    @SerializedName("b")
    public long receivedBytes;

    @SerializedName("i")
    public int fileInfoStatus = BuyerFileManager.FILE_INFO_EXIST;

    public BuyerFileAck(String fileMessageId, long receivedBytes) {
        this.fileMessageId = fileMessageId;
        this.receivedBytes = receivedBytes;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    protected BuyerFileAck(Parcel in) {

    }
    public static final Creator<BuyerFileAck> CREATOR = new Creator<BuyerFileAck>() {
        @Override
        public BuyerFileAck createFromParcel(Parcel in) {
            return new BuyerFileAck(in);
        }

        @Override
        public BuyerFileAck[] newArray(int size) {
            return new BuyerFileAck[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
