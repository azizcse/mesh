package com.w3engineers.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class UserInfo implements Parcelable {
    private String address;
    private int avatar;
    private String userName;
    private long regTime;
    private String publicKey;
    private String appToken;
    public String registrationKey;

    // for App update hand shaking
    private int versionCode;
    private String versionName;
    private String appSize;

    private double lat;
    private double lang;

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String packageName) {
        this.appToken = packageName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAvatar() {
        return avatar;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getRegTime() {
        return regTime;
    }

    public void setRegTime(long regTime) {
        this.regTime = regTime;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getAppSize() {
        return appSize;
    }

    public void setAppSize(String appSize) {
        this.appSize = appSize;
    }

    public String getRegistrationKey() {
        return registrationKey;
    }

    public void setRegistrationKey(String registrationKey) {
        this.registrationKey = registrationKey;
    }

    public double getLatitude() {
        return lat;
    }

    public void setLatitude(double lat) {
        this.lat = lat;
    }

    public double getLongitude() {
        return lang;
    }

    public void setLongitude(double lang) {
        this.lang = lang;
    }

    protected UserInfo(Parcel in) {
        address = in.readString();
        avatar = in.readInt();
        userName = in.readString();
        regTime = in.readLong();
        publicKey = in.readString();
        appToken = in.readString();
        versionCode = in.readInt();
        versionName = in.readString();
        appSize = in.readString();
        registrationKey = in.readString();
        lat = in.readDouble();
        lang = in.readDouble();
    }

    public UserInfo() {

    }

    public static final Creator<UserInfo> CREATOR = new Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel in) {
            return new UserInfo(in);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeInt(avatar);
        dest.writeString(userName);
        dest.writeLong(regTime);
        dest.writeString(publicKey);
        dest.writeString(appToken);
        dest.writeInt(versionCode);
        dest.writeString(versionName);
        dest.writeString(appSize);
        dest.writeString(registrationKey);
        dest.writeDouble(lat);
        dest.writeDouble(lang);
    }

    @NonNull
    @Override
    public String toString() {
        return "Address: " + address + "\n name: " + userName + "\n package: " + appToken;
    }
}
