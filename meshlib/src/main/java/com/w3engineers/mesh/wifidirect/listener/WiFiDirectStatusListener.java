package com.w3engineers.mesh.wifidirect.listener;

import android.net.wifi.WifiInfo;

import com.w3engineers.mesh.libmeshx.wifid.APCredential;

/**
 * Created by Azizul Islam on 12/9/20.
 */
public interface WiFiDirectStatusListener {
    void onGoCreated(String ssid, String password);
    void onGoFound(APCredential credential);
    void onWifiConnect(WifiInfo wifiInfo);
    void onWifiDisconnect();
    void onWifiAlreadyConnected();
    void onConnectionAttemptTimeout(boolean isConnected);
    void onConnectedWithSelfGo();
}
