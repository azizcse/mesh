package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-08-25 at 12:10 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: MESH.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-08-25 at 12:10 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-08-25 at 12:10 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class BroadcastMessage extends FileMessage {

    @SerializedName("bcid")
    public String mBroadcastContentId;

    @SerializedName("btd")
    public String mBroadcastTextData;

    @SerializedName("bet")
    public long mBroadcastExpireTime;

    // TODO mimo broadcast adjustment for calling path investigate

    @SerializedName("bla")
    public double broadcastLatitude;

    @SerializedName("blo")
    public double broadcastLongitude;

    @SerializedName("br")
    public double broadcastRange;

    public BroadcastMessage(long fileTransferId) {
        super(fileTransferId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mBroadcastContentId);
        dest.writeString(this.mBroadcastTextData);
        dest.writeLong(this.mBroadcastExpireTime);
        dest.writeDouble(this.broadcastLatitude);
        dest.writeDouble(this.broadcastLongitude);
        dest.writeDouble(this.broadcastRange);
    }

    protected BroadcastMessage(Parcel in) {
        super(in);
        this.mBroadcastContentId = in.readString();
        this.mBroadcastTextData = in.readString();
        this.mBroadcastExpireTime = in.readLong();
        this.broadcastLatitude = in.readDouble();
        this.broadcastLongitude = in.readDouble();
        this.broadcastRange = in.readDouble();
    }

    public static final Creator<BroadcastMessage> CREATOR = new Creator<BroadcastMessage>() {
        @Override
        public BroadcastMessage createFromParcel(Parcel source) {
            return new BroadcastMessage(source);
        }

        @Override
        public BroadcastMessage[] newArray(int size) {
            return new BroadcastMessage[size];
        }
    };


    @Override
    public String toString() {
        return "BroadcastFileMessage{" +
                "mBroadcastContentId='" + mBroadcastContentId + '\'' +
                ", mBroadcastTextData='" + mBroadcastTextData + '\'' +
                '}';
    }
}
