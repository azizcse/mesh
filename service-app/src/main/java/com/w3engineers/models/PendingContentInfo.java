package com.w3engineers.models;

import android.os.Parcel;
import android.os.Parcelable;

public class PendingContentInfo implements Parcelable {
    private String senderId, contentId, contentPath;
    private int progress, state;
    private boolean isIncoming;
    private ContentMetaInfo contentMetaInfo;

    public PendingContentInfo() {

    }

    protected PendingContentInfo(Parcel in) {
        senderId = in.readString();
        contentId = in.readString();
        contentPath = in.readString();
        progress = in.readInt();
        state = in.readInt();
        isIncoming = in.readByte() != 0;
        contentMetaInfo = in.readParcelable(ContentMetaInfo.class.getClassLoader());
    }

    public static final Creator<PendingContentInfo> CREATOR = new Creator<PendingContentInfo>() {
        @Override
        public PendingContentInfo createFromParcel(Parcel in) {
            return new PendingContentInfo(in);
        }

        @Override
        public PendingContentInfo[] newArray(int size) {
            return new PendingContentInfo[size];
        }
    };

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

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String pathPath) {
        this.contentPath = pathPath;
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

    public ContentMetaInfo getContentMetaInfo() {
        return contentMetaInfo;
    }

    public void setContentMetaInfo(ContentMetaInfo contentMetaInfo) {
        this.contentMetaInfo = contentMetaInfo;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(senderId);
        dest.writeString(contentId);
        dest.writeString(contentPath);
        dest.writeInt(progress);
        dest.writeInt(state);
        dest.writeByte((byte) (isIncoming ? 1 : 0));
        dest.writeParcelable(contentMetaInfo, flags);
    }
}
