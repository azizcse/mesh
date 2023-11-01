package com.w3engineers.mesh.ble.message;

import android.bluetooth.BluetoothDevice;

public interface BleDataListener {
    void onGetMessageSendResponse(boolean isSuccess);

    void onGetMessage(String senderId, BluetoothDevice device, byte[] partialData);

    default void onGetRawMessage(BluetoothDevice device, byte[] rawData) {
    }


}
