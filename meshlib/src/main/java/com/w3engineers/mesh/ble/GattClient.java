package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.widget.Toast;

import com.w3engineers.mesh.ble.message.BleDataListener;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.IdentifierUtility;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GattClient implements GattClientActionListener {

    private final String PREFIX_TAG = "[BLE_PROCESS]";

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter mAdapter;
    private BluetoothGatt mBluetoothGatt;

    private BLEDataListener bleDataListener;
    private BleDataListener bleMessageCallback;

    private ConcurrentHashMap<String, BluetoothDevice> mScanResults;
    private Context mContext;
    private boolean isScanning;

    private boolean isConnected;
    private boolean hasWritePermission;

    private final int DELAY_MIN = 2; // Define as seconds
    private final int DELAY_MAX = 6; // Define as seconds

    // We will scan maximum two times depend on device found or not
    private final int MAX_SCAN = 2;

    private final long SCAN_PERIOD = 40 * 1000; // If same scan record found then rescan after 30 seconds
    private final long RESCAN_DELAY = 15 * 1000;
    private final long SCAN_PERIOD_AFTER_ERROR = 30 * 1000; //If any scan error occurred then rescan will trigger after 30 seconds
    private final int PASSWORD_LENGTH = 8;

    private final long GATT_133_TIMEOUT = 600L;

    //Todo We have to fix it for multiple client server connection
    private BluetoothDevice connectedDevice;


    private String serviceUUIDString;

    private String[] userFilterList;

    private boolean isForceConnection = false;
    private volatile String mLastEthereumAddress;

    private ConcurrentHashMap<BluetoothDevice, BluetoothGatt> serverConnectionMap;


    public GattClient(BluetoothAdapter adapter, Context context, BLEDataListener listener, BleDataListener callback) {
        this.mAdapter = adapter;
        this.mContext = context;
        bluetoothLeScanner = mAdapter.getBluetoothLeScanner();
        mScanResults = new ConcurrentHashMap<>();

        this.bleDataListener = listener;
        this.bleMessageCallback = callback;

    }

    public void startScan(boolean isForceConnection) {

        mScanResults.clear();
        mLastEthereumAddress = null;

        if (!isForceConnection) {
            userFilterList = null;
        }

        MeshLog.v(PREFIX_TAG + " Scan started....");
        this.isForceConnection = isForceConnection;

        long randomDelay = generateRandomDelayTime(DELAY_MAX, DELAY_MIN);

        //First remove rescan runnable then scan
        HandlerUtil.removeBackground(reScanRunnable);
        HandlerUtil.removeBackground(scanTimeTrackingRunnable);

        HandlerUtil.postBackground(scannerRunnable, randomDelay);

    }


    private Runnable scannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                //MeshLog.v(PREFIX_TAG + " Scan already running");
                //MeshLog.v(PREFIX_TAG + " Stop previous scan");
                stopScan();
                disconnectGattServer(false);
            }

            MeshLog.v(PREFIX_TAG + " BLE Scan start..");


            if (mAdapter == null) {
                mAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            bluetoothLeScanner = mAdapter.getBluetoothLeScanner();


            String randomId = BleConstant.SERVICE_STRING;
            UUID serviceUUID = BleUUIDHelper.generateUUID(randomId);
            //UUID serviceUUID = BleUUIDHelper.generateUUID(otherUserId);
            serviceUUIDString = serviceUUID.toString();

            disconnectGattServer(false);

            isScanning = true;

            // Commented out below line for infinite scan
            trackScanTime();

            List<ScanFilter> filters = new ArrayList<>();

            if (userFilterList != null && userFilterList.length > 0) {
                //It is for specific search
                addScanFilter(filters);
            } else {

                byte[] dataBytes = BleConstant.MANUFACTURE_DATA.getBytes();
                byte[] maskBytes = getMaskedBytes(dataBytes);
                //It is for general search
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setManufacturerData(BleConstant.MANUFACTURE_ID, dataBytes, maskBytes)
                        .build();
                filters.add(scanFilter);

            }

            ScanSettings.Builder builder = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    /*.setReportDelay(GATT_133_TIMEOUT)*/;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
            }

            ScanSettings settings = builder.build();

            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(filters, settings, leScanCallback);
            } else {
                MeshLog.e(PREFIX_TAG + " Maybe you Bluetooth off bro");
                HandlerUtil.postForeground(() -> Toast.makeText(mContext, "Bluetooth Off", Toast.LENGTH_SHORT).show());

            }
        }
    };

    public void stopScan() {
        MeshLog.i(PREFIX_TAG + " BLE scan stop...");
        isScanning = false;
        HandlerUtil.removeBackground(scanTimeTrackingRunnable);
        HandlerUtil.removeBackground(scannerRunnable);
        HandlerUtil.removeBackground(reScanRunnable);
        if (bluetoothLeScanner != null && mAdapter.getState() == BluetoothAdapter.STATE_ON) {
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }


    public boolean sendData(byte[] data) {
        if (isConnected && hasWritePermission) {
            BluetoothGattCharacteristic characteristic = BluetoothUtils.findEchoCharacteristic(mBluetoothGatt, serviceUUIDString);

            if (characteristic == null) {
                //MeshLog.v(PREFIX_TAG + " Unable to find echo characteristic");
                disconnectGattServer(false);
                return false;
            }

            // MeshLog.v(PREFIX_TAG + " Client before sending characteristics: " + characteristic.getUuid().toString());

            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(data);
            boolean isSuccess = mBluetoothGatt.writeCharacteristic(characteristic);
            MeshLog.v(PREFIX_TAG + " Send data from client. Is success: " + isSuccess);

            return isSuccess;
        }

        //MeshLog.e(PREFIX_TAG + " Client side cannot send data");

        return false;
    }

    public void sendCredentialRequest() {
        if (isConnected && hasWritePermission) {
            BluetoothGattCharacteristic characteristic = BluetoothUtils.findEchoCharacteristic(mBluetoothGatt, serviceUUIDString);

            if (characteristic == null) {
                MeshLog.v(PREFIX_TAG + " Unable to find echo characteristic");
                disconnectGattServer(false);
                return;
            }

            boolean isSuccess = mBluetoothGatt.readCharacteristic(characteristic);

            MeshLog.v(PREFIX_TAG + " Send read request  from client. Is success: " + isSuccess);
        }
    }

    public void setScanFilter(String... userIds) {
        this.userFilterList = userIds;
    }

    public boolean hasForceConnection() {
        return isForceConnection;
    }

    public void disconnectDevice(BluetoothDevice device) {
        //Todo disconnect specific gatt by devices
        mBluetoothGatt.disconnect();
    }


    private void addScanFilter(List<ScanFilter> filterList) {
        if (userFilterList != null && userFilterList.length > 0) {
            for (String id : userFilterList) {
                if (AddressUtil.isValidEthAddress(id)) {

                    UUID uuid = IdentifierUtility.getUUIDFromEthereumAddress(id);
                    byte[] manufactureData = IdentifierUtility.append(BleConstant.MANUFACTURE_DATA,
                            IdentifierUtility.getLast4bytesFromEthereumAddress(id));

                    byte[] manufactureDataSpecial = IdentifierUtility.append(BleConstant.MANUFACTURE_DATA,
                            IdentifierUtility.getLast4bytesFromEthereumAddress(id));

                    ScanFilter scanFilter = new ScanFilter.Builder()
                            .setServiceUuid(new ParcelUuid(uuid))
                            .setManufacturerData(BleConstant.MANUFACTURE_ID, manufactureData)

                            .build();

                    // This filer is used to find a ble user who has already connected to other
                    ScanFilter scanFilterSpecial = new ScanFilter.Builder()
                            .setServiceUuid(new ParcelUuid(uuid))
                            .setManufacturerData(BleConstant.MANUFACTURE_ID_SPECIAL, manufactureDataSpecial)

                            .build();

                    filterList.add(scanFilter);
                    filterList.add(scanFilterSpecial);
                }
            }
        }
    }


    private void connectWithServer() {
        for (String deviceAddress : mScanResults.keySet()) {
            BluetoothDevice device = mScanResults.get(deviceAddress);

            GattClientCallback gattCallback = new GattClientCallback(this, serviceUUIDString);

            if (device != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mBluetoothGatt = device.connectGatt(mContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    mBluetoothGatt = device.connectGatt(mContext, false, gattCallback);
                }

                connectedDevice = device;

                mScanResults.clear();
                // We are connecting only one devices
                return;
            }
        }
    }

    private void connectWithServer(BluetoothDevice device) {
        GattClientCallback gattCallback = new GattClientCallback(this, serviceUUIDString);

        if (device != null) {
            //Todo we have to manage multiple connection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = device.connectGatt(mContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = device.connectGatt(mContext, false, gattCallback);
            }

            connectedDevice = device;

        }
    }

    private boolean checkNodeExistence(int hash) {
        return RouteManager.getInstance().isUserHashExists(hash);
    }

    private void trackScanTime() {
        HandlerUtil.postBackground(scanTimeTrackingRunnable, SCAN_PERIOD);
    }

    private Runnable scanTimeTrackingRunnable = () -> {
        MeshLog.v(PREFIX_TAG + " Ble stop from after time period end");
        stopScan();

        scheduleRescan();

        // Try to connect server
        // We are now comment out the connect function here. We will use it from one shot
        //connectWithServer();
    };

    /*
     * Rescan runnable is used when a user find in ble that already connected via WIFI
     * So we will wait 10 seconds and restart scan.
     * This will help to reduce same user find from scan
     * */
    private Runnable reScanRunnable = () -> {
        startScan(isForceConnection);
    };

    private void scheduleRescan() {
        HandlerUtil.postBackground(reScanRunnable, RESCAN_DELAY);
    }

    private long generateRandomDelayTime(int max, int min) {
        Random random = new Random();
        int randValue = random.nextInt((max - min) + 1) + min;
        return randValue * 1000L;
    }


    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {

                    BluetoothDevice btDevice = result.getDevice();


                    // MeshLog.v(PREFIX_TAG + " New device found: Name: " + btDevice.getName());
                    //MeshLog.v(PREFIX_TAG + " New device found: Address " + btDevice.getAddress());

                    ScanRecord record = result.getScanRecord();

                    mScanResults.put(btDevice.getAddress(), btDevice);

                    if (record != null) {
                        //MeshLog.v(PREFIX_TAG + "  Name: " + record.toString());
                      /*  if (record.getServiceUuids() != null && !record.getServiceUuids().isEmpty()) {
                            String foundServiceUUID = record.getServiceUuids().get(0).getUuid().toString();
                            if (BleUUIDHelper.isDesireUUID(foundServiceUUID, otherUserId)) {

                            }
                        }*/

                        // So here we stop the scan.
                        //stopScan();


                        UUID uuid = null;
                        List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
                        for (ParcelUuid parcelUuid : parcelUuids) {
                            uuid = parcelUuid.getUuid();
                            break;
                        }

                        byte[] manufactureData = result.getScanRecord().
                                getManufacturerSpecificData(BleConstant.MANUFACTURE_ID);

                        if (manufactureData == null) {
                            manufactureData = result.getScanRecord().
                                    getManufacturerSpecificData(BleConstant.MANUFACTURE_ID_SPECIAL);
                        }

                        byte[] ethereumAddressRemainedBytes = new byte[4];
                        System.arraycopy(manufactureData,
                                1, ethereumAddressRemainedBytes, 0,
                                ethereumAddressRemainedBytes.length);
                        mLastEthereumAddress = IdentifierUtility.getEthereumAddressFrom(uuid,
                                ethereumAddressRemainedBytes);

                       /* MeshLog.v(" Name: " + result.getScanRecord().toString()
                                + ":Obtained Ethereum address:" + mLastEthereumAddress);*/

                    }

                    // Check this user already in online or not for handle redundant connection

                    //RouteManager.getInstance().isWifiUserOnline(mLastEthereumAddress)

                    // We are checking that this user already connected or not.
                    // If connected then not need to connect in BLE again
                    if (RouteManager.getInstance().isLocallyOnline(mLastEthereumAddress)) {
                        if (isForceConnection) {
                            MeshLog.v(PREFIX_TAG + " Stop and connect when get force connection");
                            stopScan();
                            HandlerUtil.postBackground(() -> connectWithServer(btDevice), GATT_133_TIMEOUT);
                        } else {
                            //MeshLog.i(PREFIX_TAG + "  BLE device found that already connected by wifi. " + mLastEthereumAddress);

                            // We are not rescan again.

                            // Start rescan again.
                            // HandlerUtil.postBackground(reScanRunnable, SCAN_PERIOD);
                        }

                    } else {
                        // MeshLog.v(PREFIX_TAG + " Stop and connect when get device");
                        stopScan();
                        MeshLog.v(PREFIX_TAG + " BLE device found. Now connecting");
                        // try to one shot connection
                        HandlerUtil.postBackground(() -> connectWithServer(btDevice), GATT_133_TIMEOUT);
                    }

                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    //MeshLog.v(PREFIX_TAG + " onBatchScanResults list found " + results.size());

                    for (ScanResult result : results) {
                        BluetoothDevice btDevice = result.getDevice();

                        //MeshLog.v(PREFIX_TAG + " New device found: Name: " + btDevice.getName());
                        //MeshLog.v(PREFIX_TAG + " New device found: Address " + btDevice.getAddress());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    //Todo if error code 2 the we can notify that or bluetooth make on or off
                    //MeshLog.v(PREFIX_TAG + " Scan failed code : " + errorCode + " rescan after sometimes");

                    HandlerUtil.postBackground(reScanRunnable, SCAN_PERIOD_AFTER_ERROR);
                }
            };



    /*
     * Gatt Client callback
     * */

    @Override
    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    @Override
    public void initializeEcho() {
        if (mBluetoothGatt != null) {
            boolean isRequestSuccess = mBluetoothGatt.requestMtu(500);
            //  MeshLog.v(PREFIX_TAG + " MTU request send. " + isRequestSuccess);
        }
    }

    @Override
    public void disconnectGattServer(boolean isRescanNeed) {
        isScanning = false;
        isConnected = false;
        hasWritePermission = false;

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }


        if (isRescanNeed) {

            boolean hasBleUser = RouteManager.getInstance().getBleUsers().size() > 0;

            if (hasBleUser) {
                if (isForceConnection) {
                    // Remove rescan runnable if it already triggered
                    HandlerUtil.postBackground(reScanRunnable, SCAN_PERIOD_AFTER_ERROR);

                    // Start scan again if it is need
                    startScan(isForceConnection);
                }
            } else {
                // Remove rescan runnable if it already triggered
                HandlerUtil.postBackground(reScanRunnable, SCAN_PERIOD_AFTER_ERROR);

                // Start scan again if it is need
                startScan(isForceConnection);
            }

        } else {
            bleDataListener.onClientSideDisconnected();
        }
    }

    @Override
    public void onGetMessage(BluetoothDevice device, byte[] message) {
        // Here we get message we have to pass to BLE manager
      /*  String ssid = "";
        String password = "";
        if (message.length() > PASSWORD_LENGTH) {
            password = message.substring(0, PASSWORD_LENGTH);
            ssid = message.substring(PASSWORD_LENGTH);

            if (bleDataListener != null) {
                bleDataListener.onGetCredential(ssid, password);
            }
        }*/

        // We got the message
        if (bleMessageCallback != null) {
            MeshLog.v(PREFIX_TAG + " Data received in client side");
            bleMessageCallback.onGetRawMessage(device, message);
        }
    }

    @Override
    public void onMtuChanged(int status, int mtu, BluetoothDevice device) {
        //MeshLog.v(PREFIX_TAG + " MTU response found status: " + status + " mtu: " + mtu);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            hasWritePermission = true;

            // From here we are sending a credential request from here
            //sendCredentialRequest();
           /* String data;
            if (isForceConnection) {
                data = BleConstant.FORCE;
            } else {
                data = BleConstant.NORMAL;
            }*/

            if (bleDataListener != null && mLastEthereumAddress != null) {
                //bleDataListener.onGetMyMode(false, false);
                // this method is need for initial eth message send;
                bleDataListener.onGetDiscover(false, device);

                //bleDataListener.onGetNewNode(mLastEthereumAddress, connectedDevice, false);
                bleDataListener.onGetClientSideDiscover(mLastEthereumAddress, connectedDevice, isForceConnection);
            }

            //sendData(data.getBytes());
        }
    }

    @Override
    public void onMessageSendSuccess(boolean isSuccess) {

        if (bleMessageCallback != null) {
            bleMessageCallback.onGetMessageSendResponse(isSuccess);
        }


        if (bleDataListener != null && !isSuccess) {
            // Mode section is now off
            //  bleDataListener.onGetMyMode(false, false);
            //Todo we have to manage multiple device here
            bleDataListener.onNodeDisconnected(connectedDevice);
        }

    }

    byte[] getMaskedBytes(byte[] dataBytes) {
        if (dataBytes == null || dataBytes.length < 1) {
            return null;
        }

        byte[] bytes = new byte[dataBytes.length];

        Arrays.fill(bytes, (byte) 127);
        return bytes;
    }
}
