package com.w3engineers.mesh.db.users;


import android.annotation.SuppressLint;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

import com.w3engineers.ext.strom.application.data.helper.local.base.BaseEntity;
import com.w3engineers.mesh.db.meta.TableMeta;

import io.reactivex.annotations.NonNull;

/**
 * Entity for User details
 */
// To resolve multiple row insert issue now we only consider address as unique key
//
@SuppressLint("ParcelCreator")
@Entity(tableName = TableMeta.TableNames.USERS,
        indices = {@Index(value = {TableMeta.ColumnNames.ADDRESS}, unique = true)})
public class UserEntity extends BaseEntity {

    @ColumnInfo(name = TableMeta.ColumnNames.ADDRESS)
    @NonNull
    public String address;

    @ColumnInfo(name = TableMeta.ColumnNames.AVATAR)
    public int avatar;

    @ColumnInfo(name = TableMeta.ColumnNames.USER_NAME)
    public String userName;

    @ColumnInfo(name = TableMeta.ColumnNames.REG_TIME)
    public long regTime;

    @ColumnInfo(name = TableMeta.ColumnNames.IS_SYNCED)
    public boolean isSync;

    @ColumnInfo(name = TableMeta.ColumnNames.PUBLIC_KEY)
    public String publicKey;

    @ColumnInfo(name = TableMeta.ColumnNames.PACKAGE_NAME)
    public String packageName;

    @ColumnInfo(name = TableMeta.ColumnNames.IS_ME)
    public boolean isMe;

    @ColumnInfo(name = TableMeta.ColumnNames.IS_NOTIFIED)
    public boolean isNotified;

    @ColumnInfo(name = TableMeta.ColumnNames.TIME_CREATED)
    public long timeCreated;

    @ColumnInfo(name = TableMeta.ColumnNames.TIME_MODIFIED)
    public long timeModified;

    @ColumnInfo(name =  TableMeta.ColumnNames.IS_ONLINE)
    public boolean isOnline;

    @ColumnInfo(name =  TableMeta.ColumnNames.CONFIG_VERSION)
    public int configVersion;

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

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }


    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean notified) {
        isNotified = notified;
    }

    public long isTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long isTimeModified() {
        return timeModified;
    }

    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }
}
