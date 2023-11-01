package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.gson.Exclude;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 4:26 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 4:26 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 4:26 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/

/**
 * Initial meta data message to init file sending
 */
public class FileMessage extends BaseFileMessage {

    @SerializedName("tid")
    public long mFileTransferId;

    @SerializedName(TableMeta.Serialization.FILE_PATH)
    public String mFilePath;

    @SerializedName("nm")
    public String mFileName;

    /**
     * mPeerAddress is the main receiver/destination address
     */
    @SerializedName("pad")
    public String mPeerAddress;

    /**
     * mSender address is the actual sender address
     */
    @SerializedName("sd")
    public String mSourceAddress;

    @SerializedName("fs")
    public long mFileSize;

    @SerializedName("at")
    public String appToken;

    @Exclude
    public long mTransferredBytes;

    public FileMessage(long fileTransferId) {
        this.mFileTransferId = fileTransferId;
    }

    protected FileMessage(Parcel in) {
        mFileTransferId = in.readLong();
        mFilePath = in.readString();
        mFileName = in.readString();
        mPeerAddress = in.readString();
        mFileSize = in.readLong();
        appToken = in.readString();
        mSourceAddress = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mFileTransferId);
        dest.writeString(mFilePath);
        dest.writeString(mFileName);
        dest.writeString(mPeerAddress);
        dest.writeLong(mFileSize);
        dest.writeString(appToken);
        dest.writeString(mSourceAddress);
    }

    /**
     * Clone {@link FileMessage}. Ignored native {@link Cloneable}
     * <br/>
     *
     * @return
     * @see <a href="https://www.artima.com/intv/bloch.html#part13">Former Java dev recommendation</a>
     */
    public FileMessage clone() {
        FileMessage fileMessage = new FileMessage(mFileTransferId);
        fileMessage.mFileName = mFileName;
        fileMessage.mFilePath = mFilePath;
        fileMessage.mPeerAddress = mPeerAddress;
        fileMessage.mFileSize = mFileSize;
        fileMessage.appToken = appToken;
        fileMessage.mTransferredBytes = mTransferredBytes;
        fileMessage.mSourceAddress = mSourceAddress;
        fileMessage.messageMetaData = messageMetaData;
        return fileMessage;
    }

    public FileMessage copy() {
        FileMessage fileMessage = new FileMessage(mFileTransferId);
        fileMessage.mFileName = mFileName;
        fileMessage.mFilePath = mFilePath;
        fileMessage.mPeerAddress = mPeerAddress;
        fileMessage.mFileSize = mFileSize;
        fileMessage.appToken = appToken;
        fileMessage.mTransferredBytes = mTransferredBytes;
        fileMessage.mSourceAddress = mSourceAddress;
        fileMessage.messageMetaData = messageMetaData;
        return fileMessage;
    }

    @NonNull
    @Override
    public String toString() {
        String message = "Name:"+mFileName + "-Path:"+mFilePath+"-Dest:"+ AddressUtil.
                makeShortAddress(mPeerAddress) + "-Size:"+ Util.humanReadableByteCount(mFileSize) +
                "-Transferred:"+Util.humanReadableByteCount(mTransferredBytes);
        return message;
    }
}
