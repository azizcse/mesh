package com.w3engineers.purchase.util;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import android.content.Context;
import android.net.Network;
import android.util.Log;

import com.w3engineers.eth.data.helper.model.PayLibNetworkInfo;
import com.w3engineers.eth.data.remote.EthereumService;
import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.purchase.db.DatabaseService;
import com.w3engineers.purchase.db.SharedPref;
import com.w3engineers.purchase.db.networkinfo.NetworkInfo;
import com.w3engineers.purchase.manager.PurchaseConstants;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class EthereumServiceUtil implements EthereumService.NetworkInfoCallback {

    private static EthereumServiceUtil ethereumServiceUtil = null;
    private DatabaseService databaseService;
    private EthereumService ethereumService;
    private static final String GO_PREFIX = "DIRECT-";
    private Context context;
    private String SOCKET_URL = "https://signal.telemesh.net";

    private EthereumServiceUtil(Context context) {
        MeshLog.v("EthereumServiceUtil started");
        databaseService = DatabaseService.getInstance(context);
        this.context = context;
        ethereumService = EthereumService.getInstance(this.context, EthereumServiceUtil.this,
                SharedPref.read(PurchaseConstants.GIFT_KEYS.GIFT_DONATE_LINK), SharedPref.read(PurchaseConstants.GIFT_KEYS.GIFT_DONATE_USERNAME), SharedPref.read(PurchaseConstants.GIFT_KEYS.GIFT_DONATE_PASS), SharedPref.read(PurchaseConstants.GIFT_KEYS.GIFT_DONATE_PUBLIC_KEY));

        startNetworkMonitor();
    }

    public static EthereumServiceUtil getInstance(Context context) {
        if (ethereumServiceUtil == null) {
            ethereumServiceUtil = new EthereumServiceUtil(context);
        }
        return ethereumServiceUtil;
    }

    public EthereumService getEthereumService() {
        return ethereumService;
    }

    @Override
    public List<PayLibNetworkInfo> getNetworkInfo() {
        try {

            List<NetworkInfo> networkInfos = databaseService.getAllNetworkInfo();
            return new NetworkInfo().toPayLibNetworkInfos(networkInfos);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateCurrencyAndToken(int networkType, double currency, double token) {
        databaseService.updateCurrencyAndToken(networkType, currency, token);
    }

    public void updateCurrency(int networkType, double currency) {
        databaseService.updateCurrency(networkType, currency);
    }

    public void updateToken(int networkType, double token) {
        databaseService.updateToken(networkType, token);
    }

    public double getCurrency(int networkType) {
        try {
            return databaseService.getCurrencyByType(networkType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0D;
    }

    public double getToken(int networkType) {
        try {
            return databaseService.getTokenByType(networkType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0D;
    }

    public void insertNetworkInfo(NetworkInfo networkInfo) {
        try {
            databaseService.insertNetworkInfo(networkInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startNetworkMonitor(){
        NetworkMonitor.start(context, SOCKET_URL, new NetworkMonitor.NetworkInterfaceListener() {
            @Override
            public void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi) {
                Log.v("***********************","***********************");
                if (network != null){
                    Log.v("onNetworkAvailable", isOnline + " " + network.toString() + (isWiFi ? " wifi" : " cellular"));
                } else {
                    Log.v("onNetworkAvailable", isOnline + " ");
                }
                Log.v("***********************","***********************");
//                ethereumService.changeNetworkInterface(network);
                if (isOnline){
                    Network mobileNetwork  = NetworkMonitor.getConnectedMobileNetwork(context);
                    ethereumService.changeNetworkInterface(mobileNetwork);
                }
            }
        });


        Network mobileNetwork  = NetworkMonitor.getConnectedMobileNetwork(this.context);
        ethereumService.changeNetworkInterface(mobileNetwork);
    }


}
