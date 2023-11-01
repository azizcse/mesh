package com.letbyte.core.meshfilesharing.data;

import android.annotation.SuppressLint;
import android.os.Parcel;

@SuppressLint("ParcelCreator")
public class FileResendRequestMessage extends BaseFileMessage{
    public String mPeerAddress = "";
    public String mSourceAddress = "";
    public long mFileTransferId;
    public String appToken = "";

    public FileResendRequestMessage( String mSourceAddress, String mPeerAddress, long mFileTransferId, String appToken) {
        this.mPeerAddress = mPeerAddress;
        this.mSourceAddress = mSourceAddress;
        this.mFileTransferId = mFileTransferId;
        this.appToken = appToken;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
