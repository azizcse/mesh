package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.util.AddressUtil;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-09 at 11:08 AM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-09 at 11:08 AM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-09 at 11:08 AM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class FileResumeRequestMessageFromReceiver extends BaseFileMessage {

    /**
     * The address of peer who is the primary destination of this message
     */
    @SerializedName("mpa")
    public String mPeerAddress;

    //Initiating by immediate next receiver can reduce data traffic for few cases
    @SerializedName("ra")
    public String mRequesterAddress;

    @SerializedName("tid")
    public long mFileTransferId;

    @SerializedName("lrp")
    public long mRequestingBytesFrom;

    // App token need to identify the specific app request
    @SerializedName("at")
    public String appToken;

    @SerializedName("fs")
    public long mFileSize;

    public FileResumeRequestMessageFromReceiver() {

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mFileTransferId);
        dest.writeLong(mRequestingBytesFrom);
        dest.writeString(mRequesterAddress);
        dest.writeString(mPeerAddress);
    }

    public FileResumeRequestMessageFromReceiver(Parcel in) {
        mFileTransferId = in.readLong();
        mRequestingBytesFrom = in.readLong();
        mRequesterAddress = in.readString();
        mPeerAddress = in.readString();
    }

    public static final Creator<FileResumeRequestMessageFromReceiver> CREATOR = new Creator<FileResumeRequestMessageFromReceiver>() {
        @Override
        public FileResumeRequestMessageFromReceiver createFromParcel(Parcel source) {
            return new FileResumeRequestMessageFromReceiver(source);
        }

        @Override
        public FileResumeRequestMessageFromReceiver[] newArray(int size) {
            return new FileResumeRequestMessageFromReceiver[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "Peer:"+ AddressUtil.makeShortAddress(mPeerAddress)+"-Requester:"+
                AddressUtil.makeShortAddress(mRequesterAddress)+"-from:"+mRequestingBytesFrom+"-" +
                "size:"+mFileSize;
    }
}
