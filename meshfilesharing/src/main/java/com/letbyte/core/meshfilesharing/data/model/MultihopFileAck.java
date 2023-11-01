package com.letbyte.core.meshfilesharing.data.model;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;
import com.letbyte.core.meshfilesharing.core.BuyerFileManager;
import com.letbyte.core.meshfilesharing.data.BaseFileMessage;

/**
 * Created by Azizul Islam on 6/9/21.
 */
public class MultihopFileAck extends BaseFileMessage {
    @SerializedName("id")
    public String fileMessageId;

    @SerializedName("b")
    public long receivedBytes;

    @SerializedName("i")
    public int fileInfoStatus = BuyerFileManager.FILE_INFO_EXIST;
    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public MultihopFileAck(String fileMessageId, long receivedBytes) {
        this.fileMessageId = fileMessageId;
        this.receivedBytes = receivedBytes;
    }

    protected MultihopFileAck(Parcel in) {

    }
    public static final Creator<MultihopFileAck> CREATOR = new Creator<MultihopFileAck>() {
        @Override
        public MultihopFileAck createFromParcel(Parcel in) {
            return new MultihopFileAck(in);
        }

        @Override
        public MultihopFileAck[] newArray(int size) {
            return new MultihopFileAck[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
