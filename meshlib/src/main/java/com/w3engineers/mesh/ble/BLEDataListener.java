package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothDevice;

public interface BLEDataListener {
    void onGetCredential(String ssid, String password, String userId);

    void onGetServerServerStartStatus(boolean isStarted);

    default void onGetDiscover(boolean isServer, BluetoothDevice device) {
    }

    default void onGetNewNode(String userId, BluetoothDevice device, boolean isServer) {

    }

    default void onGetClientSideDiscover(String userId, BluetoothDevice device, boolean isForceConnection) {
    }

    void onGetMyMode(boolean isServer, boolean isForceConnection);

    default void onNodeDisconnected(BluetoothDevice device) {
    }

    /**
     * Update node device when this device object changed for BLE server side.
     * We will pass this object in BleManager layer and update the bleUserMap.
     * Means we use updated BluetoothDevice object
     *
     * @param device BluetoothDevice from server
     */
    default void onNodeUpdate(BluetoothDevice device) {
    }

    default void onStatSpecialSearch(String userId) {
    }

    default void onClientSideDisconnected(){

    }

}
