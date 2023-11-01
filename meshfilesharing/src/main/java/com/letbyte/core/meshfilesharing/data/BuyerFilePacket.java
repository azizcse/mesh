package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

public class BuyerFilePacket extends BaseFileMessage {

    @SerializedName("id")
    public long fileTransferId;

    @SerializedName("s")
    public String sourceAddress;

    @SerializedName("r")
    public String destinationId;

    @SerializedName("d")
    public String data;

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public BuyerFilePacket(long fileTransferId){
        this.fileTransferId = fileTransferId;
    }

    protected BuyerFilePacket(Parcel in) {

    }
    public static final Creator<BuyerFilePacket> CREATOR = new Creator<BuyerFilePacket>() {
        @Override
        public BuyerFilePacket createFromParcel(Parcel in) {
            return new BuyerFilePacket(in);
        }

        @Override
        public BuyerFilePacket[] newArray(int size) {
            return new BuyerFilePacket[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
