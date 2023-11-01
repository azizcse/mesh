package com.w3engineers.mesh.db.peers;


import android.annotation.SuppressLint;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

import com.w3engineers.ext.strom.application.data.helper.local.base.BaseEntity;
import com.w3engineers.mesh.db.meta.TableMeta;

import io.reactivex.annotations.NonNull;

/**
 * Entity for Peers
 */
// To resolve multiple row insert issue now we only consider address as unique key
//
@SuppressLint("ParcelCreator")
@Entity(tableName = TableMeta.TableNames.PEERS,
        indices = {@Index(value = {TableMeta.ColumnNames.ADDRESS, TableMeta.ColumnNames.APP_TOKEN}, unique = true)})
public class PeersEntity extends BaseEntity {

    @ColumnInfo(name = TableMeta.ColumnNames.ADDRESS)
    @NonNull
    public String address;

    @ColumnInfo(name = TableMeta.ColumnNames.PUBLIC_KEY)
    public String publicKey;

    @ColumnInfo(name = TableMeta.ColumnNames.APP_TOKEN)
    @NonNull
    public String appToken;

    @ColumnInfo(name = TableMeta.ColumnNames.IS_ME)
    public boolean isMe;

    @ColumnInfo(name = TableMeta.ColumnNames.TIME_MODIFIED)
    public long timeModified;

    @ColumnInfo(name =  TableMeta.ColumnNames.IS_ONLINE)
    public boolean isOnline;

    @ColumnInfo(name =  TableMeta.ColumnNames.APP_USER_INFO)
    public String appUserInfo;

    @ColumnInfo(name =  TableMeta.ColumnNames.USER_APP_VERSION)
    public int userAppVersion;

    @ColumnInfo(name =  TableMeta.ColumnNames.USER_LATITUDE)
    public double userLatitude;

    @ColumnInfo(name =  TableMeta.ColumnNames.USER_LONGITUDE)
    public double userLongitude;

    /*@ColumnInfo(name =  TableMeta.ColumnNames.MESH_CONTROL_CONFIG)
    public String meshControlConfig;*/

    public boolean getOnlineStatus() {
        return isOnline;
    }

    public void setOnlineStatus(boolean onlineStatus) {
        this.isOnline = onlineStatus;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }



    public long isTimeModified() {
        return timeModified;
    }

    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }

    public String getAppUserInfo() {
        return appUserInfo;
    }

    public void setAppUserInfo(String appUserInfo) {
        this.appUserInfo = appUserInfo;
    }

    public int getUserAppVersion() {
        return userAppVersion;
    }

    public void setUserAppVersion(int userAppVersion) {
        this.userAppVersion = userAppVersion;
    }

    public double getUserLatitude() {
        return userLatitude;
    }

    public void setUserLatitude(double userLatitude) {
        this.userLatitude = userLatitude;
    }

    public double getUserLongitude() {
        return userLongitude;
    }

    public void setUserLongitude(double userLongitude) {
        this.userLongitude = userLongitude;
    }

    /*public String getMeshControlConfig() {
        return meshControlConfig;
    }

    public void setMeshControlConfig(String meshControlConfig) {
        this.meshControlConfig = meshControlConfig;
    }*/
}
