package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

public class BleFileAckMessage extends BaseFileMessage {
    @SerializedName("id")
    public String fileMessageId;


    protected BleFileAckMessage(Parcel in) {
        fileMessageId = in.readString();
    }

    public BleFileAckMessage(String fileId) {
        this.fileMessageId = fileId;
    }

    public static final Creator<BleFileAckMessage> CREATOR = new Creator<BleFileAckMessage>() {
        @Override
        public BleFileAckMessage createFromParcel(Parcel in) {
            return new BleFileAckMessage(in);
        }

        @Override
        public BleFileAckMessage[] newArray(int size) {
            return new BleFileAckMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileMessageId);
    }
}
