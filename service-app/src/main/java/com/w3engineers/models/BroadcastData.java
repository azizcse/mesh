package com.w3engineers.models;


import android.os.Parcel;
import android.os.Parcelable;

public class BroadcastData implements Parcelable {
    private String broadcastId;
    private String metaData;
    private String contentPath;
    private double latitude;
    private double longitude;
    private double range;
    private String expiryTime;
    private String appToken;

    public String getBroadcastId() {
        return broadcastId;
    }

    public void setBroadcastId(String broadcastId) {
        this.broadcastId = broadcastId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public String getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public static Creator<BroadcastData> getCREATOR() {
        return CREATOR;
    }

    public BroadcastData(){ }

    protected BroadcastData(Parcel in) {
        broadcastId = in.readString();
        metaData = in.readString();
        contentPath = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        range = in.readDouble();
        expiryTime = in.readString();
        appToken = in.readString();
    }

    public static final Creator<BroadcastData> CREATOR = new Creator<BroadcastData>() {
        @Override
        public BroadcastData createFromParcel(Parcel in) {
            return new BroadcastData(in);
        }

        @Override
        public BroadcastData[] newArray(int size) {
            return new BroadcastData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(broadcastId);
        parcel.writeString(metaData);
        parcel.writeString(contentPath);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeDouble(range);
        parcel.writeString(expiryTime);
        parcel.writeString(appToken);
    }
}
