package com.w3engineers.purchase.db.clientinfo;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.w3engineers.purchase.db.TableInfo;
import com.w3engineers.purchase.model.ClientInfoModel;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = TableInfo.CLIENT_INFO)
public class ClientInfoEntity {

    @PrimaryKey
    @ColumnInfo(name = TableInfo.Column.APP_TOKEN)
    @NonNull
    public String appToken;

    @ColumnInfo(name = TableInfo.Column.KEY_HASH)
    public String keyHash;

    @ColumnInfo(name = TableInfo.Column.SHA_KEY)
    public String shaKey;

    @ColumnInfo(name = TableInfo.Column.WALLET_CREATION_ENABLE)
    public boolean isWalletCreationEnable;

    @ColumnInfo(name = TableInfo.Column.DISCOVERY_ENABLE)
    public boolean isDiscoveryEnable;

    @ColumnInfo(name = TableInfo.Column.MESSAGE_ENABLE)
    public boolean isMessageEnable;

    @ColumnInfo(name = TableInfo.Column.BLOCK_CHAIN_ENABLE)
    public boolean isBlockchainEnable;

    @ColumnInfo(name = TableInfo.Column.APP_DOWNLOAD_ENABLE)
    public boolean isAppDownloadEnable;


    public static List<ClientInfoEntity> convertClintDataToEntity(ClientInfoModel model) {
        List<ClientInfoEntity> clientInfoEntityList = new ArrayList<>();
        for (ClientInfoModel.Client client : model.clientList) {
            ClientInfoEntity entity = new ClientInfoEntity();
            entity.appToken = client.appToken;
            entity.keyHash = client.keyHash;
            entity.shaKey = client.shaKey;
            entity.isWalletCreationEnable = client.isWalletCreationEnable;
            entity.isDiscoveryEnable = client.isDiscoverEnable;
            entity.isMessageEnable = client.isMessageEnable;
            entity.isBlockchainEnable = client.isBlockchainEnable;
            entity.isAppDownloadEnable = client.isAppDownloadEnable;

            clientInfoEntityList.add(entity);
        }
        return clientInfoEntityList;
    }

    public static ClientInfoModel convertDBToClientData(List<ClientInfoEntity> clientEntityList) {
        ClientInfoModel model = new ClientInfoModel();
        List<ClientInfoModel.Client> clientList = new ArrayList<>();

        for (ClientInfoEntity entity : clientEntityList) {
            ClientInfoModel.Client client = new ClientInfoModel.Client();
            client.appToken = entity.appToken;
            client.keyHash = entity.keyHash;
            client.shaKey = entity.shaKey;
            client.isWalletCreationEnable = entity.isWalletCreationEnable;
            client.isDiscoverEnable = entity.isDiscoveryEnable;
            client.isMessageEnable = entity.isMessageEnable;
            client.isBlockchainEnable = entity.isBlockchainEnable;
            client.isAppDownloadEnable = entity.isAppDownloadEnable;
            clientList.add(client);
        }

        model.clientList = clientList;

        return model;
    }
}
