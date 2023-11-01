package com.w3engineers.mesh.libmeshx.wifid;

import android.net.wifi.p2p.WifiP2pDevice;
import androidx.annotation.IntRange;

import java.util.Collection;

/**
 * Listener for P2P
 */
public interface P2PStateListener {

    void onP2PStateChange(@IntRange(from = 1, to = 2) int state);
    void onP2PPeersStateChange();
    void onP2PConnectionChanged();
    void onP2PDisconnected();
    default void onP2PConnectionChanged(Collection<WifiP2pDevice> wifiP2pDevices) {}
    void onP2PPeersDiscoveryStarted();
    void onP2PPeersDiscoveryStopped();

}
