package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothDevice;

public interface GattServerActionListener {

    void addDevice(BluetoothDevice device);

    void removeDevice(BluetoothDevice device);

    void addClientConfiguration(BluetoothDevice device, byte[] value);

    void sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value);

    void notifyCharacteristicEcho(BluetoothDevice device, byte[] value);

    void serviceAdded();

    void onMtuChange(BluetoothDevice device,int mtu);

    void onReadRequestFound(BluetoothDevice device, int requestId, int gattSuccess, int offset, byte[] value);

    void onMessageSendSuccess(boolean isSuccess);
}
