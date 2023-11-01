package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-08-26 at 1:09 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: MESH.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-08-26 at 1:09 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-08-26 at 1:09 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class BroadcastFilePacket extends FilePacket {
    @SerializedName("bci")
    public String mBroadcastId;

    @SerializedName("bct")
    public String mBroadcastText;

    @SerializedName("bet")
    public long mBroadcastExpireTime;

    // TODO mimo broadcast adjustment for calling path investigate

    @SerializedName("bla")
    public double broadcastLatitude;

    @SerializedName("blo")
    public double broadcastLongitude;

    @SerializedName("br")
    public double broadcastRange;


    public BroadcastFilePacket(long fileId) {
        super(fileId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mBroadcastId);
        dest.writeString(this.mBroadcastText);
        dest.writeLong(this.mBroadcastExpireTime);
        dest.writeDouble(this.broadcastLatitude);
        dest.writeDouble(this.broadcastLongitude);
        dest.writeDouble(this.broadcastRange);
    }

    protected BroadcastFilePacket(Parcel in) {
        super(in);
        this.mBroadcastId = in.readString();
        this.mBroadcastText = in.readString();
        this.mBroadcastExpireTime = in.readLong();
        this.broadcastLatitude = in.readDouble();
        this.broadcastLongitude = in.readDouble();
        this.broadcastRange = in.readDouble();
    }

    public static final Creator<BroadcastFilePacket> CREATOR = new Creator<BroadcastFilePacket>() {
        @Override
        public BroadcastFilePacket createFromParcel(Parcel source) {
            return new BroadcastFilePacket(source);
        }

        @Override
        public BroadcastFilePacket[] newArray(int size) {
            return new BroadcastFilePacket[size];
        }
    };

    public BroadcastFilePacket copyBroadCastFilePacket() {
        BroadcastFilePacket filePacket = new BroadcastFilePacket(mFileId);
        filePacket.mSelfFullFilePath = mSelfFullFilePath;
        filePacket.mFileName = mFileName;
        filePacket.mFileSize = mFileSize;
        filePacket.mPeerAddress = mPeerAddress;
        filePacket.mSourceAddress = mSourceAddress;
        filePacket.fileStatus = fileStatus;
        filePacket.mRelativeOffset = mRelativeOffset;
        filePacket.appToken = appToken;
        filePacket.mTransferredBytes = mTransferredBytes;
        filePacket.mData = mData;
        filePacket.mLastModified = mLastModified;
        filePacket.mIsLastPacket = mIsLastPacket;
        filePacket.mPeerFullFilePath = mPeerFullFilePath;
        filePacket.metaData = metaData;
        filePacket.mBroadcastId = mBroadcastId;
        filePacket.mBroadcastText = mBroadcastText;
        filePacket.mBroadcastExpireTime = mBroadcastExpireTime;
        filePacket.broadcastLatitude = broadcastLatitude;
        filePacket.broadcastLongitude = broadcastLongitude;
        filePacket.broadcastRange = broadcastRange;
        return filePacket;
    }
}
