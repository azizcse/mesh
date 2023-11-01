package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

public class BuyerFileResumeRequestFromSender extends BaseFileMessage {
    @SerializedName("id")
    public long fileTransferId;

    public BuyerFileResumeRequestFromSender(long fileTransferId){
        this.fileTransferId = fileTransferId;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    protected BuyerFileResumeRequestFromSender(Parcel in) {

    }
    public static final Creator<BuyerFileResumeRequestFromSender> CREATOR = new Creator<BuyerFileResumeRequestFromSender>() {
        @Override
        public BuyerFileResumeRequestFromSender createFromParcel(Parcel in) {
            return new BuyerFileResumeRequestFromSender(in);
        }

        @Override
        public BuyerFileResumeRequestFromSender[] newArray(int size) {
            return new BuyerFileResumeRequestFromSender[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
