package com.w3engineers.purchase.db.networkinfo;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.w3engineers.eth.data.helper.model.PayLibNetworkInfo;
import com.w3engineers.purchase.model.Network;

import java.util.ArrayList;
import java.util.List;


@Entity(indices = {@Index(value = {"network_type"}, unique = true)})
public class NetworkInfo extends WalletInfo{

    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int nid;

    @ColumnInfo(name = "network_name")
    public String networkName;

    @ColumnInfo(name = "network_url")
    public String networkUrl;

    @ColumnInfo(name = "token_address")
    public String tokenAddress;

    @ColumnInfo(name = "channel_address")
    public String channelAddress;

    @ColumnInfo(name = "gas_price")
    public long gasPrice;

    @ColumnInfo(name = "gas_limit")
    public long gasLimit;



    public PayLibNetworkInfo toPayLibNetworkInfo() {
        return new PayLibNetworkInfo().setNetworkType(networkType).setNetworkName(networkName)
                .setNetworkUrl(networkUrl).setCurrencySymbol(currencySymbol).setTokenSymbol(tokenSymbol)
                .setTokenAddress(tokenAddress).setChannelAddress(channelAddress).setGasPrice(gasPrice)
                .setGasLimit(gasLimit).setTokenAmount(tokenAmount).setCurrencyAmount(currencyAmount);
    }

    public NetworkInfo toNetworkInfo(PayLibNetworkInfo payLibNetworkInfo) {
        networkType = payLibNetworkInfo.networkType;
        networkName = payLibNetworkInfo.networkName;
        networkUrl = payLibNetworkInfo.networkUrl;

        currencySymbol = payLibNetworkInfo.currencySymbol;
        tokenSymbol = payLibNetworkInfo.tokenSymbol;
        tokenAddress = payLibNetworkInfo.tokenAddress;

        channelAddress = payLibNetworkInfo.channelAddress;
        gasPrice = payLibNetworkInfo.gasPrice;
        gasLimit = payLibNetworkInfo.gasLimit;

        tokenAmount = payLibNetworkInfo.tokenAmount;
        currencyAmount = payLibNetworkInfo.currencyAmount;
        return this;
    }

    public NetworkInfo toNetworkInfo(Network network) {
        networkType = network.getNetworkType();
        networkName = network.getNetworkName();
        networkUrl = network.getNetworkUrl();

        currencySymbol = network.getCurrencySymbol();
        tokenSymbol = network.getTokenSymbol();
        tokenAddress = network.getTokenAddress();

        channelAddress = network.getChannelAddress();
        gasPrice = network.getGasPrice();
        gasLimit = network.getGasLimit();

        tokenAmount = network.getTokenAmount();
        currencyAmount = network.getCurrencyAmount();

        return this;
    }

    public List<PayLibNetworkInfo> toPayLibNetworkInfos(List<NetworkInfo> networkInfos) {
        if (networkInfos != null) {
            List<PayLibNetworkInfo> payLibNetworkInfos = new ArrayList<>();
            if (networkInfos.size() > 0) {

                for (NetworkInfo networkInfo : networkInfos) {
                    payLibNetworkInfos.add(networkInfo.toPayLibNetworkInfo());
                }
            }
            return payLibNetworkInfos;
        }
        return null;
    }
}
