package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothDevice;

public interface GattClientActionListener {

    void setConnected(boolean connected);

    void initializeEcho();

    void disconnectGattServer(boolean isRescanNeed);

    void onGetMessage(BluetoothDevice device, byte[] message);

    void onMtuChanged(int status, int mtu, BluetoothDevice device);

    void onMessageSendSuccess(boolean isSuccess);
}
