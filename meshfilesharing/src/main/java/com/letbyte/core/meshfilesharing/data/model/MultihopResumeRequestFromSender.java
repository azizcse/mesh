package com.letbyte.core.meshfilesharing.data.model;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;
import com.letbyte.core.meshfilesharing.data.BaseFileMessage;

/**
 * Created by Azizul Islam on 6/9/21.
 */
public class MultihopResumeRequestFromSender extends BaseFileMessage {
    @SerializedName("id")
    public long fileTransferId;

    public MultihopResumeRequestFromSender(long fileTransferId){
        this.fileTransferId = fileTransferId;
    }
    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    protected MultihopResumeRequestFromSender(Parcel in) {

    }
    public static final Creator<MultihopResumeRequestFromSender> CREATOR = new Creator<MultihopResumeRequestFromSender>() {
        @Override
        public MultihopResumeRequestFromSender createFromParcel(Parcel in) {
            return new MultihopResumeRequestFromSender(in);
        }

        @Override
        public MultihopResumeRequestFromSender[] newArray(int size) {
            return new MultihopResumeRequestFromSender[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
