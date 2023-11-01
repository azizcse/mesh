package com.w3engineers.mesh.model;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.util.JsonDataBuilder;

public class Broadcast {

    @SerializedName(JsonDataBuilder.KEY_BROADCAST_ID)
    private String broadcastId;

    @SerializedName(JsonDataBuilder.KEY_SENDER_ID)
    private String senderId;

    @SerializedName(JsonDataBuilder.KEY_RECEIVER_ID)
    private String receiverId;

    @SerializedName(JsonDataBuilder.KEY_APP_TOKEN)
    private String appToken;

    @SerializedName(JsonDataBuilder.KEY_BROADCAST_TEXT)
    private String broadcastMeta;

    @SerializedName(JsonDataBuilder.KEY_CONTENT_PATH)
    private String contentPath;

    @SerializedName(JsonDataBuilder.KEY_CONTENT_LATITUDE)
    private double latitude;

    @SerializedName(JsonDataBuilder.KEY_CONTENT_LONGITUDE)
    private double longitude;

    @SerializedName(JsonDataBuilder.KEY_CONTENT_EXPIRE_TIME)
    private double range;

    @SerializedName(JsonDataBuilder.KEY_BROADCAST_RANGE)
    private long expiryTime;

    @SerializedName(JsonDataBuilder.KEY_MESSAGE_TYPE)
    private int type;

    @SerializedName(JsonDataBuilder.KEY_MESSAGE)
    private String baseMessage;

    public String getBroadcastId() {
        return broadcastId;
    }

    public Broadcast setBroadcastId(String broadcastId) {
        this.broadcastId = broadcastId;
        return this;
    }

    public String getSenderId() {
        return senderId;
    }

    public Broadcast setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public Broadcast setReceiverId(String receiverId) {
        this.receiverId = receiverId;
        return this;
    }

    public String getAppToken() {
        return appToken;
    }

    public Broadcast setAppToken(String appToken) {
        this.appToken = appToken;
        return this;
    }

    public String getBroadcastMeta() {
        return broadcastMeta;
    }

    public Broadcast setBroadcastMeta(String broadcastMeta) {
        this.broadcastMeta = broadcastMeta;
        return this;
    }

    public String getContentPath() {
        return contentPath;
    }

    public Broadcast setContentPath(String contentPath) {
        this.contentPath = contentPath;
        return this;
    }

    public double getLatitude() {
        return latitude;
    }

    public Broadcast setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public Broadcast setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public double getRange() {
        return range;
    }

    public Broadcast setRange(double range) {
        this.range = range;
        return this;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public Broadcast setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
        return this;
    }

    public int getType() {
        return type;
    }

    public Broadcast setType(int type) {
        this.type = type;
        return this;
    }

    public String getBaseMessage() {
        return baseMessage;
    }

    public Broadcast setBaseMessage(String baseMessage) {
        this.baseMessage = baseMessage;
        return this;
    }
}
