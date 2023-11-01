package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.w3engineers.mesh.util.MeshLog;

import java.util.UUID;

public class GattServerCallback extends BluetoothGattServerCallback {
    private GattServerActionListener mServerActionListener;
    private UUID myUUID;

    private final String PREFIX_TAG = "[BLE_PROCESS]";

    public GattServerCallback(GattServerActionListener listener, UUID uuid) {
        this.mServerActionListener = listener;
        this.myUUID = uuid;
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);

        MeshLog.v(PREFIX_TAG + " onConnectionStateChange address: " + device.getAddress()
                + " Name: " + device.getName() + " status: " + status + " newState: " + newState);


        if (newState == BluetoothProfile.STATE_CONNECTED) {
            mServerActionListener.addDevice(device);
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            mServerActionListener.removeDevice(device);
        }
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
       // MeshLog.v(PREFIX_TAG + " Gatt server a service added:" + status);
        mServerActionListener.serviceAdded();
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        mServerActionListener.onReadRequestFound(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);


       /* MeshLog.v(PREFIX_TAG + " onCharacteristicWriteRequest" + characteristic.getUuid().toString()
                + " My UIID: " + myUUID.toString());*/

        // We are checking here the actual message is received here or not
        if (BleConstant.CHARACTERISTIC_ECHO_UUID.toString().equalsIgnoreCase(characteristic.getUuid().toString())) {
            //MeshLog.v(PREFIX_TAG + " received correct write request");
            mServerActionListener.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);

            // Reverse message to differentiate original message & response
            characteristic.setValue(value);
            mServerActionListener.notifyCharacteristicEcho(device, value);
        }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor);

        //MeshLog.v(PREFIX_TAG + " onDescriptorReadRequest" + descriptor.getUuid().toString());
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device,
                                         int requestId, BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded,
                                         int offset, byte[] value) {
        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

       /* MeshLog.v(PREFIX_TAG + " onDescriptorWriteRequest: " + descriptor.getUuid().toString()
                + "\nvalue: " + new String(value));*/

        mServerActionListener.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

        // If we need any request to change the config we can do it from here or we can use this as
        // data passing section

       /* if (Constant.CLIENT_CONFIGURATION_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
            mServerActionListener.addClientConfiguration(device, value);
            mServerActionListener.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }*/
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);

        mServerActionListener.onMtuChange(device, mtu);

        //MeshLog.v(PREFIX_TAG + " onMtuChanged name: " + device.getName() + " address: " + device.getAddress());
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        // super.onNotificationSent(device, status);
        //MeshLog.v(PREFIX_TAG + " Message send status: " + device.getAddress() + " status: " + status);

        if (mServerActionListener != null) {
            boolean isSuccess;
            isSuccess = status == BluetoothGatt.GATT_SUCCESS;
            mServerActionListener.onMessageSendSuccess(isSuccess);
        }
    }
}
