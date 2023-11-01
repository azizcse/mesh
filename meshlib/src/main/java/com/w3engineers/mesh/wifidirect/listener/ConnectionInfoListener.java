package com.w3engineers.mesh.wifidirect.listener;


import android.net.wifi.p2p.WifiP2pInfo;

public interface ConnectionInfoListener {

    void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo);

}
