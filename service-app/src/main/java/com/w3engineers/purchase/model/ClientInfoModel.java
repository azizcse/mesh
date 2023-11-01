package com.w3engineers.purchase.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ClientInfoModel {
    @SerializedName("version")
    public int version;

    @SerializedName("clients")
    public List<Client> clientList;


    public static class Client {
        @SerializedName("app_token")
        public String appToken;

        @SerializedName("key_hash")
        public String keyHash;

        @SerializedName("sha_key")
        public String shaKey;

        @SerializedName("wallet_creation_enable")
        public boolean isWalletCreationEnable;

        @SerializedName("discovery_enable")
        public boolean isDiscoverEnable;

        @SerializedName("message_enable")
        public boolean isMessageEnable;

        @SerializedName("blokchain_enable")
        public boolean isBlockchainEnable;

        @SerializedName("app_download_enable")
        public boolean isAppDownloadEnable;
    }

}
