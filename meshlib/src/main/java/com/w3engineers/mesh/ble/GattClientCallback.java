package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import com.w3engineers.mesh.util.MeshLog;

import java.util.List;

public class GattClientCallback extends BluetoothGattCallback {
    private final String PREFIX_TAG = "[BLE_PROCESS]";

    private GattClientActionListener mClientActionListener;
    private String serviceUUID;

    public GattClientCallback(GattClientActionListener listener, String uuid) {
        this.mClientActionListener = listener;
        this.serviceUUID = uuid;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        MeshLog.v(PREFIX_TAG + " onConnectionStateChange newState: " + newState + " Status : " + status);

        if (status == BluetoothGatt.GATT_FAILURE) {
            //MeshLog.v(PREFIX_TAG + " Connection Gatt failure status " + status);
            mClientActionListener.disconnectGattServer(false);
            return;
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            // handle anything not SUCCESS as failure
            //MeshLog.v(PREFIX_TAG + " Connection not GATT success status " + status);
            mClientActionListener.disconnectGattServer(true);
            return;
        }

        if (newState == BluetoothProfile.STATE_CONNECTED) {
           // MeshLog.v(PREFIX_TAG + " Connected to device " + gatt.getDevice().getAddress());
            mClientActionListener.setConnected(true);
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            //MeshLog.v(PREFIX_TAG + " Disconnected from device");
            mClientActionListener.disconnectGattServer(false);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        MeshLog.v(PREFIX_TAG + " Service discovered: " + status + " ");

        if (status != BluetoothGatt.GATT_SUCCESS) {
           // MeshLog.v(PREFIX_TAG + " Device service discovery unsuccessful, status " + status);
            return;
        }

       // MeshLog.v(PREFIX_TAG + " Service UUID string " + serviceUUID);

        List<BluetoothGattCharacteristic> matchingCharacteristics = BluetoothUtils.
                findCharacteristics(gatt, BleConstant.CHARACTERISTIC_ECHO_STRING);
        if (matchingCharacteristics.isEmpty()) {
          //  MeshLog.v(PREFIX_TAG + " Unable to find characteristics.");
            // Try to rescan it again.
            mClientActionListener.disconnectGattServer(true);
            return;
        }


        //MeshLog.v(PREFIX_TAG + " Initializing: setting write type and enabling notification " + matchingCharacteristics.size());
        for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            enableCharacteristicNotification(gatt, characteristic);
            // break;
        }

       /* BluetoothGattService service = gatt.getService(Constant.SERVICE_UUID);
        BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(Constant.CHARACTERISTIC_ECHO_UUID);
        if (characteristic1 != null) {
            boolean isSuccess = gatt.readCharacteristic(characteristic1);
            Log.d(Constant.TAG, "readCharacteristic: " + isSuccess);
        }*/
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            MeshLog.v(PREFIX_TAG + " Characteristic written successfully");
            mClientActionListener.onMessageSendSuccess(true);
        } else {
            MeshLog.v(PREFIX_TAG + " Characteristic write unsuccessful, status: " + status);
            mClientActionListener.onMessageSendSuccess(false);

            mClientActionListener.disconnectGattServer(false);

        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        //super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            /*MeshLog.v(PREFIX_TAG + " Characteristic read successfully: " + characteristic.getValue().length +
                    " " + characteristic.getStringValue(0));*/
            readCharacteristic(gatt.getDevice(), characteristic);
        } else {
            MeshLog.v(PREFIX_TAG + " Characteristic read unsuccessful, status: " + status);
            // Trying to read from the Time Characteristic? It doesnt have the property or permissions
            // set to allow this. Normally this would be an error and you would want to:
            // disconnectGattServer();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        //MeshLog.v(PREFIX_TAG + " Characteristic changed, " + characteristic.getUuid().toString());
        readCharacteristic(gatt.getDevice(), characteristic);
    }


    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
           // MeshLog.v(PREFIX_TAG + " Descriptor written successfully: " + descriptor.getUuid().toString());

        } else {
           // MeshLog.v(PREFIX_TAG + " Descriptor write unsuccessful: " + descriptor.getUuid().toString() + " Status: " + status);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        //MeshLog.v(PREFIX_TAG + " onMtuChanged status: " + status + " mtu: " + mtu);
        mClientActionListener.onMtuChanged(status, mtu,gatt.getDevice());
    }


    private void enableCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        boolean characteristicWriteSuccess = gatt.setCharacteristicNotification(characteristic, true);
        if (characteristicWriteSuccess) {
            MeshLog.v(PREFIX_TAG + " Characteristic notification set successfully for " + characteristic.getUuid().toString());
            if (BluetoothUtils.isEchoCharacteristic(characteristic, serviceUUID)) {
                mClientActionListener.initializeEcho();
            }

            if (BluetoothUtils.isTimeCharacteristic(characteristic, serviceUUID)) {
                enableCharacteristicConfigurationDescriptor(gatt, characteristic);
            }
        } else {
            MeshLog.v(PREFIX_TAG + " Characteristic notification set failure for " + characteristic.getUuid().toString());
        }
    }

    private void enableCharacteristicConfigurationDescriptor(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
        BluetoothGattDescriptor descriptor = BluetoothUtils.findClientConfigurationDescriptor(descriptorList);
        if (descriptor == null) {
            //MeshLog.v(PREFIX_TAG + " Unable to find Characteristic Configuration Descriptor");
            return;
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        boolean descriptorWriteInitiated = gatt.writeDescriptor(descriptor);
        if (descriptorWriteInitiated) {
           // MeshLog.v(PREFIX_TAG + " Characteristic Configuration Descriptor write initiated: " + descriptor.getUuid().toString());
        } else {
           // MeshLog.v(PREFIX_TAG + " Characteristic Configuration Descriptor write failed to initiate: " + descriptor.getUuid().toString());
        }
    }


    private void readCharacteristic(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        byte[] messageBytes = characteristic.getValue();

        /*String message = new String(messageBytes);
        MeshLog.v(PREFIX_TAG + " Message is  " + message);*/
        mClientActionListener.onGetMessage(device, messageBytes);
    }
}
