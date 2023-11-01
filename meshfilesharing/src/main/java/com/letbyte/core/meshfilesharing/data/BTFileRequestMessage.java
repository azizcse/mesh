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
public class BTFileRequestMessage extends BaseFileMessage {

    @SerializedName("tid")
    public long mFileTransferId;

    @SerializedName("tb")
    public long mTransferredBytes;

    public String mPeerAddress, mSourceAddress;

    public BTFileRequestMessage(long fileTransferId) {
        this.mFileTransferId = fileTransferId;
    }

    public BTFileRequestMessage copy() {
        BTFileRequestMessage btFileRequestMessage = new BTFileRequestMessage(mFileTransferId);
        return btFileRequestMessage;
    }

    @NonNull
    @Override
    public String toString() {
        String message = "id:"+mFileTransferId+"-Src:"+ AddressUtil.makeShortAddress(mSourceAddress)
                +"-Peer:"+AddressUtil.makeShortAddress(mPeerAddress);
        return message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mFileTransferId);
        dest.writeLong(this.mTransferredBytes);
        dest.writeString(this.mPeerAddress);
        dest.writeString(this.mSourceAddress);
    }

    protected BTFileRequestMessage(Parcel in) {
        this.mFileTransferId = in.readLong();
        this.mTransferredBytes = in.readLong();
        this.mPeerAddress = in.readString();
        this.mSourceAddress = in.readString();
    }

    public static final Creator<BTFileRequestMessage> CREATOR = new Creator<BTFileRequestMessage>() {
        @Override
        public BTFileRequestMessage createFromParcel(Parcel source) {
            return new BTFileRequestMessage(source);
        }

        @Override
        public BTFileRequestMessage[] newArray(int size) {
            return new BTFileRequestMessage[size];
        }
    };
}
