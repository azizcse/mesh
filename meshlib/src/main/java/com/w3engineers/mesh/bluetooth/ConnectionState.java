package com.w3engineers.mesh.bluetooth;

/**
 * <h1>Bluetooth Connection state is notified through this</h1>
 */
public interface ConnectionState {
    void onConnectionState(int messageId, String deviceName);
}
