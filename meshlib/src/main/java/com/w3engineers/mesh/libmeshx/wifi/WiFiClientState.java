package com.w3engineers.mesh.libmeshx.wifi;

/**
 * Provides p2p Client connection or disconnection event
 */
public interface WiFiClientState {

    void onConnected();
    void onDisconnected();

}
