package com.w3engineers.mesh.model;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.util.JsonDataBuilder;

public class BroadcastAck {

    @SerializedName(JsonDataBuilder.KEY_BROADCAST_ID)
    private String broadcastId;

    @SerializedName(JsonDataBuilder.KEY_SENDER_ID)
    private String senderId;

    @SerializedName(JsonDataBuilder.KEY_RECEIVER_ID)
    private String receiverId;

    @SerializedName(JsonDataBuilder.KEY_MESSAGE_TYPE)
    private int type;

    public String getBroadcastId() {
        return broadcastId;
    }

    public BroadcastAck setBroadcastId(String broadcastId) {
        this.broadcastId = broadcastId;
        return this;
    }

    public String getSenderId() {
        return senderId;
    }

    public BroadcastAck setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public BroadcastAck setReceiverId(String receiverId) {
        this.receiverId = receiverId;
        return this;
    }

    public int getType() {
        return type;
    }

    public BroadcastAck setType(int type) {
        this.type = type;
        return this;
    }
}
