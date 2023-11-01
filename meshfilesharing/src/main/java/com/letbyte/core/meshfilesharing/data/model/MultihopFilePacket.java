package com.letbyte.core.meshfilesharing.data.model;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;
import com.letbyte.core.meshfilesharing.data.BaseFileMessage;

/**
 * Created by Azizul Islam on 6/9/21.
 */
public class MultihopFilePacket extends BaseFileMessage {

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

    public MultihopFilePacket(long fileTransferId){
        this.fileTransferId = fileTransferId;
    }

    protected MultihopFilePacket(Parcel in) {

    }
    public static final Creator<MultihopFilePacket> CREATOR = new Creator<MultihopFilePacket>() {
        @Override
        public MultihopFilePacket createFromParcel(Parcel in) {
            return new MultihopFilePacket(in);
        }

        @Override
        public MultihopFilePacket[] newArray(int size) {
            return new MultihopFilePacket[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
