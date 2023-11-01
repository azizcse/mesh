package com.w3engineers.mesh.util;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

/**
 * Provides Network related callback
 */
public class NetworkCallBackReceiver extends ConnectivityManager.NetworkCallback {
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);
        Log.d(TAG, network.toString());
    }

    @Override
    public void onLosing(Network network, int maxMsToLive) {
        Log.d(TAG, network.toString() + "--" + maxMsToLive);
    }

    @Override
    public void onLost(Network network) {
        Log.d(TAG, network.toString());
    }

    @Override
    public void onUnavailable() {
        Log.d(TAG, "Unavailable");}


   /* @Override
    public void onCapabilitiesChanged(Network network,
                                      NetworkCapabilities networkCapabilities) {
        Log.d(TAG, network.toString() + "--" + networkCapabilities.toString());
    }*/

    @Override
    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        Log.d(TAG, network.toString());
    }


}
