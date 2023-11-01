package com.w3engineers.mesh.libmeshx.discovery;


import android.net.wifi.p2p.WifiP2pDevice;

import com.w3engineers.mesh.db.routing.RoutingEntity;

import java.util.Collection;
import java.util.List;

/**
 * Contains method to communicate at App layer
 */
public interface MeshXAPListener {

    void onSoftAPStateChanged(boolean isEnabled, String Ssid);

    void onGOConnectedWith(Collection<WifiP2pDevice> wifiP2pDevices);

    void onGODisconnectedWith(String ip);

    default void onReceiveCredentialFromBle(String ssid, String password, String userId) {
    }

    default void onGetDisconnectedList(List<RoutingEntity> offlineList) {
    }
}
