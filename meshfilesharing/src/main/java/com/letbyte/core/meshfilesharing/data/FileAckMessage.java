package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

public class FileAckMessage extends BaseMessage {

    // TODO: 6/23/2020 Need this message as Aziz vai modified File API only to process a single hop,
    //Upon discussion later this should be changed because it causes reasonable performance issue
    //If this is done at lower layer then we might survive some serialization-deserialization over
    // some hop or router
    /**
     * The target of the ACK message. Generally the {@link FilePacket#mSourceAddress}
     */
    @SerializedName("pa")
    public String mPeerAddress;

    @SerializedName("tb")
    public long mTransferredBytes;

    @SerializedName("id")
    public long mFileTransferId;

    public FileAckMessage(long fileTransferId) {
        mFileTransferId = fileTransferId;
    }

    public FileAckMessage(Parcel in) {
        mTransferredBytes = in.readLong();
        mFileTransferId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTransferredBytes);
        dest.writeLong(mFileTransferId);
    }

    public static final Creator<FileAckMessage> CREATOR = new Creator<FileAckMessage>() {
        @Override
        public FileAckMessage createFromParcel(Parcel source) {
            return new FileAckMessage(source);
        }

        @Override
        public FileAckMessage[] newArray(int size) {
            return new FileAckMessage[size];
        }
    };
}
