/*
package com.w3engineers.mesh.wifidirect.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.w3engineers.mesh.Adhoc.protocol.WifiDetector;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifidirect.listener.WiFiDirectStatusListener;

import java.net.InetAddress;

*/
/**
 * Created by Azizul Islam on 12/9/20.
 *//*

public class WifiStateReceiver {
    private Context context;
    private WiFiDirectStatusListener statusListener;
    private boolean running;
    private BroadcastReceiver receiver;
    private DispatchQueue queue;
    private boolean connected;
    private boolean isWifiEnabled;

    public WifiStateReceiver(Context context, DispatchQueue queue, WiFiDirectStatusListener listener){
        this.context = context;
        this.queue = queue;
        this.statusListener = listener;
    }

    public void register(){
        if (running)
            return;

        running = true;
        if(isWifiConnected()){
            statusListener.onWifiAlreadyConnected();
        }

        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiStateReceiver.this.onReceive(context, intent);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        context.registerReceiver(receiver, intentFilter, null, queue.getHandler());
    }

    private boolean isWifiConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info.isConnected();
    }

    private void onReceive(Context context, Intent intent){
        final String action = intent.getAction();

        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            onReceive_NETWORK_STATE_CHANGED_ACTION(context, intent);
        }else if(action.equals("android.net.wifi.WIFI_STATE_CHANGED")){
            onReceivedWifiStateChangedAction(intent);
        }

    }

    private void onReceive_NETWORK_STATE_CHANGED_ACTION(Context context, Intent intent) {
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

        if (NetworkInfo.State.CONNECTED.equals(info.getState())) {
            if (connected)
                return;
            this.connected = true;
            queue.dispatch(new Runnable() {
                @Override
                public void run() {
                    statusListener.onWifiConnect();
                }
            });
            return;
        }

        if (NetworkInfo.State.DISCONNECTED.equals(info.getState())) {
            if (!connected)
                return;
            this.connected = false;
            queue.dispatch(() ->statusListener.onWifiDisconnect());
        }
    }

    private void onReceivedWifiStateChangedAction(Intent intent){
        int statusInt = intent.getIntExtra("wifi_state", 0);
        switch (statusInt) {
            case WifiManager.WIFI_STATE_UNKNOWN:
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                if(!isWifiEnabled){
                    isWifiEnabled = true;
                    MeshLog.v("[p2p_process] wifi WIFI_STATE_ENABLED");
                    //listener.onWiFiEnabled();
                }
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                MeshLog.v("[p2p_process] wifi WIFI_STATE_DISABLED");
                isWifiEnabled = false;
                break;
            default:
                break;
        }
    }


}
*/
