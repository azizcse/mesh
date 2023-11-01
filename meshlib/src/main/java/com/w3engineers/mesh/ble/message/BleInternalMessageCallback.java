package com.w3engineers.mesh.ble.message;

import android.bluetooth.BluetoothDevice;

public interface BleInternalMessageCallback {
    void onReceiveIdentityMessage(String userId, BluetoothDevice device);

    void onReceiveCredentialMessage(String userId, String password, String ssid);

    void onMessageSendingStatus(String messageId, boolean isSuccess);

    void onGetForceConnectionRequest(String data, BluetoothDevice device);

    void onGetForceConnectionReply(String data);
}
