package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class BleFileMessage extends BaseFileMessage {
    @SerializedName("tid")
    public long mFileTransferId;

    @SerializedName("data")
    public String data;

    public String mPeerAddress, mSourceAddress;

    public BleFileMessage(){}

    protected BleFileMessage(Parcel in) {
        mFileTransferId = in.readLong();
        //data = in.createByteArray();
        data = in.readString();
        mPeerAddress = in.readString();
        mSourceAddress = in.readString();
    }

    public static final Creator<BleFileMessage> CREATOR = new Creator<BleFileMessage>() {
        @Override
        public BleFileMessage createFromParcel(Parcel in) {
            return new BleFileMessage(in);
        }

        @Override
        public BleFileMessage[] newArray(int size) {
            return new BleFileMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mFileTransferId);
        //dest.writeByteArray(data);
        dest.writeString(data);
        dest.writeString(mPeerAddress);
        dest.writeString(mSourceAddress);
    }
}
