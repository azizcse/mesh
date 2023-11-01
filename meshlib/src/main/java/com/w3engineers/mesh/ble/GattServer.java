package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.widget.Toast;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.ble.message.BleDataListener;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.IdentifierUtility;
import com.w3engineers.mesh.util.MeshLog;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GattServer implements GattServerActionListener {

    private final String PREFIX_TAG = "[BLE_PROCESS]";

    private BluetoothAdapter mBtAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private Context mContext;

    private AdvertiseSettings settings;
    private AdvertiseData data;

    private BleDataListener messageCallback;

    private ConcurrentLinkedQueue<BluetoothDevice> mDevices;

    private BLEDataListener bleDataListener;

    private volatile boolean isServerRunning;
    private String myUserId;
    private UUID myServiceUUID;


    private String ssid;
    private String password;


    public GattServer(BluetoothAdapter adapter, BluetoothManager manager, Context context,
                      String myUserId, BLEDataListener listener, BleDataListener callback) {

        this.mBtAdapter = adapter;
        this.mBluetoothManager = manager;
        this.mContext = context;
        this.bleDataListener = listener;
        this.myUserId = myUserId;

        this.messageCallback = callback;
    }

    public void startServer() {

        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            MeshLog.e(PREFIX_TAG + " This deice not support BLE Gatt Server");
            return;
        }

        if (isServerRunning) {
            // We do not create server again if it is running
            MeshLog.v(PREFIX_TAG + " Server already running");
            //MeshLog.v("[lazy] Server already running");

            /*stopAdvertising();
            stopServer();*/
            return;
        } else {
            //MeshLog.e(PREFIX_TAG + " Server restarted from start server");
            stopServer();
        }

        //String randomId = BleConstant.SERVICE_STRING;
        myServiceUUID = IdentifierUtility.getUUIDFromEthereumAddress(myUserId);
        //myServiceUUID = BleUUIDHelper.generateUUID(myUserId);

        mDevices = new ConcurrentLinkedQueue<>();
        startAdvertising();
    }

    private void setupServer() {
        BluetoothGattService service = new BluetoothGattService(myServiceUUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
                BleConstant.CHARACTERISTIC_ECHO_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

        // Characteristic with Descriptor
       /* BluetoothGattCharacteristic notifyCharacteristic = new BluetoothGattCharacteristic(
                myServiceUUID,
                0,
                0);*/

        BluetoothGattDescriptor clientConfigurationDescriptor = new BluetoothGattDescriptor(
                BleConstant.CHARACTERISTIC_ECHO_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        clientConfigurationDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        //notifyCharacteristic.addDescriptor(clientConfigurationDescriptor);
        writeCharacteristic.addDescriptor(clientConfigurationDescriptor);

        service.addCharacteristic(writeCharacteristic);
        //service.addCharacteristic(notifyCharacteristic);

        if (mGattServer != null) {
            mGattServer.addService(service);
        } else {
            MeshLog.e(PREFIX_TAG + " Maybe you Bluetooth off bro");
            HandlerUtil.postForeground(() -> Toast.makeText(mContext, "Bluetooth Off", Toast.LENGTH_SHORT).show());
        }
    }

    private void startAdvertising() {
        GattServerCallback gattServerCallback = new GattServerCallback(this, myServiceUUID);
        mGattServer = mBluetoothManager.openGattServer(mContext, gattServerCallback);


        setupServer();

        //mBtAdapter.setName("mesh");

        settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();


        byte[] manufactureData = IdentifierUtility.append(BleConstant.MANUFACTURE_DATA,
                IdentifierUtility.getLast4bytesFromEthereumAddress(myUserId));
        ParcelUuid parcelUuid = new ParcelUuid(myServiceUUID);
        data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(parcelUuid)
                .addManufacturerData(BleConstant.MANUFACTURE_ID, manufactureData)
                .build();


        mBluetoothLeAdvertiser = mBtAdapter.getBluetoothLeAdvertiser();

        if (mBluetoothManager != null && mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        } else {
            MeshLog.e(PREFIX_TAG + " Maybe you Bluetooth off bro");
            HandlerUtil.postForeground(() -> Toast.makeText(mContext, "Bluetooth Off", Toast.LENGTH_SHORT).show());
        }
    }

    public void restartAdvertise() {
        if (mBluetoothManager != null && mBluetoothLeAdvertiser != null) {

            // For restart advertise.the advertise will start with another manufacture id

            byte[] manufactureData = IdentifierUtility.append(BleConstant.MANUFACTURE_DATA_SPECIAL,
                    IdentifierUtility.getLast4bytesFromEthereumAddress(myUserId));
            ParcelUuid parcelUuid = new ParcelUuid(myServiceUUID);
            data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .setIncludeTxPowerLevel(false)
                    .addServiceUuid(parcelUuid)
                    .addManufacturerData(BleConstant.MANUFACTURE_ID_SPECIAL, manufactureData)
                    .build();

            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);

            mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
            MeshLog.i(PREFIX_TAG + " restart advertising");
        }
    }

    public void stopAdvertising() {
        if (mBluetoothManager != null && mBluetoothLeAdvertiser != null) {
            MeshLog.v(PREFIX_TAG + " BLE server stooping from stopAdvertising");
            isServerRunning = false;
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    public boolean sendDataFromServer(byte[] data, BluetoothDevice device) {
        if (mGattServer == null) return false;
        BluetoothGattService service = mGattServer.getService(myServiceUUID);

        if (service == null) {
            MeshLog.e(PREFIX_TAG + " Gatt server service null");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleConstant.CHARACTERISTIC_ECHO_UUID);

        MeshLog.v(PREFIX_TAG + " Client before sending characteristics: " + characteristic.getUuid().toString());

        BluetoothGattDescriptor gD = new BluetoothGattDescriptor(BleConstant.CHARACTERISTIC_ECHO_UUID,
                BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        characteristic.addDescriptor(gD);

        characteristic.setValue(data);

        boolean confirm = BluetoothUtils.requiresConfirmation(characteristic);
        MeshLog.v(PREFIX_TAG + " Gatt server properties: " + characteristic.getProperties() + " is confirm: " + confirm);

        boolean isSend = false;


        // MeshLog.v(PREFIX_TAG + " device list: " + mDevices.size());
        /*for (BluetoothDevice device : mDevices) {
            isSend = mGattServer.notifyCharacteristicChanged(device, characteristic, true);
            MeshLog.v(PREFIX_TAG + " Data send from server side: " + isSend + " Device address: " + device.getAddress());
        }*/

        /*List<BluetoothDevice> allBleDevices = mGattServer.getConnectedDevices();
        MeshLog.v(PREFIX_TAG + " Gatt sever side device list: " + allBleDevices.size());*/

        for (BluetoothDevice d : mDevices) {
            if (d.getAddress().equals(device.getAddress())) {
                device = d;
                break;
            }
        }


        List<BluetoothDevice> bluetoothDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (device != null && CollectionUtil.hasItem(bluetoothDevices) && bluetoothDevices.contains(device)) {

            isSend = mGattServer.notifyCharacteristicChanged(device, characteristic, false);
            MeshLog.v(PREFIX_TAG + " Data send from server side: " + isSend + " Device address: " + device.getAddress());
        } else {
            MeshLog.e(PREFIX_TAG + " Gatt server the Bluetooth device not valid");
        }

        return isSend;
    }

    public void stopServer() {
        stopAdvertising();
        //When server stop we clear the whole device
        //Todo clear the ble user from routing table
        MeshLog.e(PREFIX_TAG, " GATT server server stopped");
        if (CollectionUtil.hasItem(mDevices)) {
            for (BluetoothDevice device : mDevices){
                disconnectDevice(device);
            }
            mDevices.clear();
        }
        isServerRunning = false;
        if (mGattServer != null) {
            mGattServer.close();
            if (mGattServer != null) {
                mGattServer.clearServices();
            }
            //mGattServer = null;
        }
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    public void setCredential(String ssid, String password) {
        this.ssid = ssid;
        this.password = password;
    }

    // Gatt server disconnect is broken internally (System fault)
    public void disconnectDevice(BluetoothDevice device) {
        if (mGattServer != null) {
            MeshLog.i(PREFIX_TAG + " Sever side disconnection called");
            mGattServer.connect(device, false);
            mGattServer.cancelConnection(device);
            //mGattServer.close();
        }
    }

    /*
     * Advertise callback
     * */

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            MeshLog.v(PREFIX_TAG + " Peripheral advertising started. ");
            isServerRunning = true;
            if (bleDataListener != null) {
                bleDataListener.onGetServerServerStartStatus(true);
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            MeshLog.v(PREFIX_TAG + " Peripheral advertising failed: " + errorCode);
            isServerRunning = false;
            if (bleDataListener != null) {
                bleDataListener.onGetServerServerStartStatus(false);
            }
        }
    };

    private void addBleNewDevice(BluetoothDevice device) {
        if (mDevices == null) {
            mDevices = new ConcurrentLinkedQueue<>();
        }

        BluetoothDevice oldDevice = null;
        for (BluetoothDevice existsDevice : mDevices) {
            if (device != null && existsDevice.getAddress().equals(device.getAddress())) {
                oldDevice = existsDevice;
                break;
            }
        }

        if (oldDevice != null) {
            mDevices.remove(oldDevice);
        }

        mDevices.add(device);
    }

    private void removeBleDevice(BluetoothDevice device) {
        BluetoothDevice oldDevice = null;
        if (mDevices != null && !mDevices.isEmpty()) {
            for (BluetoothDevice existsDevice : mDevices) {
                if (existsDevice.getAddress().equals(device.getAddress())) {
                    oldDevice = existsDevice;
                    break;
                }
            }

            if (oldDevice != null) {
                mDevices.remove(oldDevice);
            }
        }
    }



    /*
     * Gatt server callback.
     * */

    @Override
    public void addDevice(BluetoothDevice device) {
        addBleNewDevice(device);
        if (bleDataListener != null) {
            bleDataListener.onNodeUpdate(device);
        }

    }

    @Override
    public void removeDevice(BluetoothDevice device) {
        removeBleDevice(device);
        if (bleDataListener != null) {
            bleDataListener.onNodeDisconnected(device);
        }
    }

    @Override
    public void addClientConfiguration(BluetoothDevice device, byte[] value) {
        //Ignoring now because we are not save particular configuration
    }

    @Override
    public void sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        mGattServer.sendResponse(device, requestId, status, 0, null);
    }

    @Override
    public void notifyCharacteristicEcho(BluetoothDevice device, byte[] value) {
      /*  if (bleDataListener != null) {
            bleDataListener.onGetCredential(new String(value), password);
        }*/

        /*String data = new String(value);
        boolean isForce;
        if (data.equals(BleConstant.FORCE)) {
            isForce = true;
        } else {
            isForce = false;
        }

        if (bleDataListener != null) {
            bleDataListener.onGetMyMode(true, isForce);
        }*/


        if (messageCallback != null) {
            MeshLog.v(PREFIX_TAG + " Data received in server side");
            messageCallback.onGetRawMessage(device, value);
        }

    }

    @Override
    public void serviceAdded() {

    }

    @Override
    public void onMtuChange(BluetoothDevice device, int mtu) {
        MeshLog.v(PREFIX_TAG + " MTU updated. ANd The value is: " + mtu);

        if (bleDataListener != null) {
            //bleDataListener.onGetMyMode(true, false);
            bleDataListener.onGetDiscover(true, device);
        }
    }

    @Override
    public void onReadRequestFound(BluetoothDevice device, int requestId, int gattSuccess, int offset, byte[] value) {
        // We have ssid and password. Here we will prepare data and send it.
        /*String data = this.password + this.ssid;
        byte[] value = data.getBytes();*/
        mGattServer.sendResponse(device, requestId, gattSuccess, offset, value);
    }

    @Override
    public void onMessageSendSuccess(boolean isSuccess) {
        if (messageCallback != null) {
            messageCallback.onGetMessageSendResponse(isSuccess);
        }
    }

}
