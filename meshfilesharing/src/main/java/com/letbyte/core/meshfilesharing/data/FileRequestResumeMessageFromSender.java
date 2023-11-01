package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.util.AddressUtil;

public class FileRequestResumeMessageFromSender extends BaseFileMessage {

    @SerializedName("tid")
    public long fileTransferId;

    @SerializedName("pa")
    public String mPeerAddress;

    @SerializedName("sa")
    public String mSourceAddress;

    @SerializedName("at")
    public String appToken;

    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeLong(fileTransferId);
        dest.writeString(appToken);
        dest.writeString(mPeerAddress);
        dest.writeString(mSourceAddress);
    }

    public FileRequestResumeMessageFromSender() {
    }

    public FileRequestResumeMessageFromSender(Parcel in) {
        fileTransferId = in.readLong();
        appToken = in.readString();
        mPeerAddress = in.readString();
        mSourceAddress = in.readString();
    }

    public static final Creator<FileRequestResumeMessageFromSender> CREATOR = new Creator<FileRequestResumeMessageFromSender>() {
        @Override
        public FileRequestResumeMessageFromSender createFromParcel(Parcel source) {
            return new FileRequestResumeMessageFromSender(source);
        }

        @Override
        public FileRequestResumeMessageFromSender[] newArray(int size) {
            return new FileRequestResumeMessageFromSender[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "FileID:"+ fileTransferId+"-peer address:"+
                AddressUtil.makeShortAddress(mPeerAddress) + " - Source address:"+AddressUtil.
                makeShortAddress(mSourceAddress);
    }
}
