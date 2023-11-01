package com.w3engineers.purchase.db.content;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Message Entity
 */
@Entity(indices = {@Index(value = {"contentId"},
        unique = true)})
public class Content implements Parcelable {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int pid;

    @ColumnInfo(name = "senderId")
    public String senderId;

    @ColumnInfo(name = "contentId")
    public String contentId;

    @ColumnInfo(name = "contentMetaInfo")
    public String contentMetaInfo;

    @ColumnInfo(name = "contentPath")
    public String contentPath;

    @ColumnInfo(name = "progress")
    public int progress;

    @ColumnInfo(name = "state")
    public int state;

    @ColumnInfo(name = "is_incoming")
    public boolean isIncoming;

    @ColumnInfo(name = "app_token")
    public String appToken;

    public Content() {

    }

    protected Content(Parcel in) {
        pid = in.readInt();
        senderId = in.readString();
        contentId = in.readString();
        contentMetaInfo = in.readString();
        contentPath = in.readString();
        progress = in.readInt();
        state = in.readInt();
        isIncoming = in.readByte() != 0;
        appToken = in.readString();
    }

    public static final Creator<Content> CREATOR = new Creator<Content>() {
        @Override
        public Content createFromParcel(Parcel in) {
            return new Content(in);
        }

        @Override
        public Content[] newArray(int size) {
            return new Content[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pid);
        dest.writeString(senderId);
        dest.writeString(contentId);
        dest.writeString(contentMetaInfo);
        dest.writeString(contentPath);
        dest.writeInt(progress);
        dest.writeInt(state);
        dest.writeByte((byte) (isIncoming ? 1 : 0));
        dest.writeString(appToken);
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentMetaInfo() {
        return contentMetaInfo;
    }

    public void setContentMetaInfo(String contentMetaInfo) {
        this.contentMetaInfo = contentMetaInfo;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
