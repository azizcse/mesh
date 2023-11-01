package com.w3engineers.models;
 
/*
============================================================================
Copyright (C) 2020 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MeshControlConfig {
    @SerializedName("app_token")
    @Expose
    private String appToken;
    @SerializedName("wallet_creation_enable")
    @Expose
    private boolean walletCreationEnable;
    @SerializedName("discovery_enable")
    @Expose
    private boolean discoveryEnable;
    @SerializedName("message_enable")
    @Expose
    private boolean messageEnable;
    @SerializedName("blokchain_enable")
    @Expose
    private boolean blokchainEnable;
    @SerializedName("app_download_enable")
    @Expose
    private boolean appDownloadEnable;

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public boolean isWalletCreationEnable() {
        return walletCreationEnable;
    }

    public void setWalletCreationEnable(boolean walletCreationEnable) {
        this.walletCreationEnable = walletCreationEnable;
    }

    public boolean getDiscoveryEnable() {
        return discoveryEnable;
    }

    public void setDiscoveryEnable(boolean discoveryEnable) {
        this.discoveryEnable = discoveryEnable;
    }

    public boolean getMessageEnable() {
        return messageEnable;
    }

    public void setMessageEnable(boolean messageEnable) {
        this.messageEnable = messageEnable;
    }

    public boolean getBlokchainEnable() {
        return blokchainEnable;
    }

    public void setBlokchainEnable(boolean blokchainEnable) {
        this.blokchainEnable = blokchainEnable;
    }

    public boolean getAppDownloadEnable() {
        return appDownloadEnable;
    }

    public void setAppDownloadEnable(boolean appDownloadEnable) {
        this.appDownloadEnable = appDownloadEnable;
    }

}
