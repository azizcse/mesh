package com.w3engineers.mesh.model;

import com.google.gson.annotations.SerializedName;

public class DisconnectionModel {

    @SerializedName("id")
    private String nodeId;

    @SerializedName("tp")
    private int userType;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }
}
