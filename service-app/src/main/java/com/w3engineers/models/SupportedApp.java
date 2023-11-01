package com.w3engineers.models;

public class SupportedApp {
    private String type;
    private String appId;
    private String sha256;

    public SupportedApp(String type, String appId, String sha256) {
        this.type = type;
        this.appId = appId;
        this.sha256 = sha256;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }
}
