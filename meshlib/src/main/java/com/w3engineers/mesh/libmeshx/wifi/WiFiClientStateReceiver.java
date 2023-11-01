package com.w3engineers.mesh.libmeshx.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

import com.w3engineers.ext.strom.App;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.NetworkCallBackReceiver;
import com.w3engineers.mesh.wifi.WiFiUtil;

/**
 * Receiver of Wifi client
 */
public class WiFiClientStateReceiver {

    private enum ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }

    private boolean misDestroyed;
    private WiFiClientState mWiFiClientState;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private ConnectivityManager mConnectivityManager;
    private NetworkCallBackReceiver mNetworkCallback;

    public WiFiClientStateReceiver(Context context, final WiFiClientState wiFiClientState) {

        mWiFiClientState = wiFiClientState;
        misDestroyed = false;

        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        mNetworkCallback = new NetworkCallBackReceiver() {
            /**
             * @param network
             */
            @Override
            public void onAvailable(Network network) {
                if(!misDestroyed) {
                    MeshLog.e("p2p_process Connection Available");


                    NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                    if (mConnectionState != ConnectionState.CONNECTED &&
                            networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                        mConnectionState = ConnectionState.CONNECTED;
                        mWiFiClientState.onConnected();
                    }
                }else {
                    MeshLog.e("p2p_process miss destroyed occurred ");
                }
            }

            /**
             * @param network
             */
            @Override
            public void onLost(Network network) {
                String log = "[DC-IssueWiFi]onLost. Connected ssid:" +
                        WiFiUtil.getConnectedSSID(App.getContext()) + ":";
                if(network == null) {
                    log += network;
                } else {
                    final ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = cm.getNetworkInfo(network);
                    log += networkInfo;
                }

                MeshLog.v(log);
                if (mConnectionState != ConnectionState.DISCONNECTED) {
                    mConnectionState = ConnectionState.DISCONNECTED;
                    mWiFiClientState.onDisconnected();
                }
            }
        };

        mConnectivityManager.registerNetworkCallback(
                builder.build(),
                mNetworkCallback
        );
    }

    public void destroy() {

        misDestroyed = true;
        try {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);

        } catch (IllegalArgumentException illegalArgumentException) {
//            illegalArgumentException.printStackTrace();
        }
    }
}
