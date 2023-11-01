package com.w3engineers.mesh.wifidirect;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifidirect.listener.ConnectionInfoListener;
import com.w3engineers.mesh.wifidirect.listener.ServiceDisconnectedListener;
import com.w3engineers.mesh.wifidirect.receiver.WiFiDirectBroadcastReceiver;


/**
 * <h1>
 *     WiFi direct main platform dependent code here
 *     Always create single instance of WiFiDirect channel,
 *     Wifi p2p manage and P2p broadcast receiver
 *
 *     Channel connection and disconnection listener.
 *
 * </h1>
 *
 */
public class WiFiP2PInstance implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.ChannelListener {

    private static final String TAG = "[p2p_process]";

    private static WiFiP2PInstance instance;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver broadcastReceiver;

    private ConnectionInfoListener peerConnectedListener;
    private ServiceDisconnectedListener serviceDisconnectedListener;

    private WiFiP2PInstance() {
    }

    private WiFiP2PInstance(Context context) {
        this();
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), this);
        broadcastReceiver = new WiFiDirectBroadcastReceiver(this);
    }


    public static WiFiP2PInstance getInstance(Context context) {
        if (instance == null) {
            instance = new WiFiP2PInstance(context);
        }

        return instance;
    }

    public WifiP2pManager getWifiP2pManager() {
        return wifiP2pManager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public WiFiDirectBroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }


    public void setPeerConnectedListener(ConnectionInfoListener peerConnectedListener) {
        this.peerConnectedListener = peerConnectedListener;
    }

    public void setServerDisconnectedListener(ServiceDisconnectedListener serviceDisconnectedListener) {
        this.serviceDisconnectedListener = serviceDisconnectedListener;
    }

    public void startPeerDiscovering() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //MeshLog.i("[p2p_process] discoverPeers Success");
            }

            @Override
            public void onFailure(int reason) {
                MeshLog.i("[p2p_process] Error discoverPeers  Reason: " + WiFiP2PError.fromReason(reason));
            }
        });
    }


    public void stopPeerDiscovery(){
        wifiP2pManager.stopPeerDiscovery(channel, null);
    }



    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (peerConnectedListener != null) {
            //MeshLog.i("[p2p_process] onConnectionInfoAvailable in P2pinstance");
            peerConnectedListener.onConnectionInfoAvailable(info);
        }else {
            //MeshLog.i("[p2p_process] onConnectionInfoAvailable is null");
        }
    }

    public void onServerDeviceDisconnected() {
        //MeshLog.e("Service disconnected ------------");
        if (serviceDisconnectedListener != null) {
            serviceDisconnectedListener.onServerDisconnectedListener();
        }
    }

    @Override
    public void onChannelDisconnected() {
        MeshLog.e("Channel disconnected ------------");
    }
}
