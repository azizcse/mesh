package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.TransportManager;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.premission.BTDiscoveryTimeRequester;
import com.w3engineers.mesh.premission.MeshSystemRequestActivity;
import com.w3engineers.mesh.premission.PermissionHelper;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifiap.BluetoothDiscoveryReceiver;

import java.util.List;

import timber.log.Timber;

/**
 * <h1>Bluetooth connection or disconnection related staff managed here</h1>
 * <p> Bluetooth discovery initiates, soft disable or hard disable controlled here</p>
 */
public class BTManager {

    public static final String SEPARATOR = ":";
    private final static Object mLock = new Object();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDiscoveryReceiver mBluetoothDiscoveryReceiver;
    public BluetoothDiscoveryReceiver.BTDiscoveryListener mBTDiscoveryListener;
    private Context mContext;
    public BTStateReceiver.BTStateListener mBTStateListener;
    private BTStateReceiver mBTStateReceiver;
    private static BTManager sBTManager;
    public boolean mIsBtTurningOff;
    public volatile boolean mHasForcedOff;

    private String mSearchingId;

    private APListener mAPListener;

    private BTManager(Context context) {
        mIsBtTurningOff = false;
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDiscoveryReceiver = new BluetoothDiscoveryReceiver(new BluetoothDiscoveryReceiver.BTDiscoveryListener() {
            @Override
            public void onBluetoothFound(List<BluetoothDevice> bluetoothDevice) {
                if (mBTDiscoveryListener != null) {
                    mBTDiscoveryListener.onBluetoothFound(bluetoothDevice);
                }
            }

            @Override
            public void onScanFinished() {
                if (mBTDiscoveryListener != null) {
                    mBTDiscoveryListener.onScanFinished();
                }
            }
        });
        mBluetoothDiscoveryReceiver.setAPListener((ssid, preSharedKey) -> {
            if (mAPListener != null) {
                mAPListener.onAPAvailable(ssid, preSharedKey);
            }
        });

        mBTStateReceiver = new BTStateReceiver(isOn -> {
            if (mBTStateListener != null && TransportManagerX.getInstance().isBtEnabled) {
                mBTStateListener.onBTStateChange(isOn);
            }

            if (!isOn && !mIsBtTurningOff) {
                mHasForcedOff = true;
                stop();
            }
        });
        registerBTStateReceiver();

        PermissionHelper mPermissionHelper = new PermissionHelper();

        if (mPermissionHelper.hasPermissions(mContext, MeshSystemRequestActivity.MESH_PERMISSIONS)
                && PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            requestDiscoverableTimePeriod();
        }

    }

    private void registerBTStateReceiver() {
        if (mBTStateReceiver != null && mContext != null) {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            mContext.registerReceiver(mBTStateReceiver, filter);
        }
    }

    public synchronized static BTManager getInstance() {
        return sBTManager;
    }

    public synchronized static BTManager getInstance(Context context) {
        if (sBTManager == null) {
            synchronized (mLock) {
                if (sBTManager == null) {
                    sBTManager = new BTManager(context);
                }
            }
        }
        return sBTManager;
    }

    public void disable() {
       /* MeshLog.i("BT Disable ");
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            TransportManagerX.getInstance().isBtEnabled = false;
        }*/
    }

    public void softDisable() {
        /*MeshLog.i("BT softDisable ");
        BluetoothTransport bluetoothTransport = TransportManagerX.getInstance().getBlueToothTransport();
        if (bluetoothTransport != null) {
            BluetoothServer bluetoothServer = bluetoothTransport.getBluetoothServer();
            if (bluetoothServer != null) {
                bluetoothServer.stopListenThread();
            }
        }*/
    }

    public void btServerEnableIfDisable() {
        /*BluetoothTransport bluetoothTransport = TransportManagerX.getInstance().getBlueToothTransport();
        if (bluetoothTransport != null) {
            BluetoothServer bluetoothServer = bluetoothTransport.getBluetoothServer();
            if (bluetoothServer != null) {
                MeshLog.i(" BT server called from btServerEnableIfDisable");
                bluetoothServer.starListenThread();
            }
        }*/
    }


    public void disconnect() {
        /*MeshLog.i("BT disconnect ");
        ConnectionLinkCache connectionLinkCache = ConnectionLinkCache.getInstance();
        if (connectionLinkCache != null) {
            List<BleLink> btConnectionLinks = connectionLinkCache.getDirectBleLinks();
            for (BleLink link : btConnectionLinks) {
                MeshLog.e("BT disconnect call from BT MANGER ");
                link.disconnect();
            }
        }*/
    }

    public boolean isEnable() {
        return mBluetoothAdapter.isEnabled();
    }


    public String getSearchingId() {
        return mSearchingId;
    }


    public void startSpecialSearch(String specialId) {
        MeshLog.i("BT Classic special search started");
        this.mSearchingId = specialId;
        startDiscovery();
    }

    public void startDiscovery() {
        MeshLog.v("BT manager start discovery called");
        startDiscovery(SharedPref.read(Constant.KEY_DEVICE_BLE_NAME));
    }


    /**
     * If BT On then start discovery immediately. Request and wait for BT turn on if BT off, then
     * start discovery
     */
    private void startDiscovery(String btName) {
//        if(!TransportManagerX.getInstance().isHighBandEnabled()) {

        if (TextUtils.isEmpty(btName)) {

            btName = SharedPref.read(Constant.KEY_DEVICE_BLE_NAME);
        }

        if (TextUtils.isEmpty(btName)) {
            MeshLog.e("BT Name empty in registerDiscReceiver in BTManager");
            return;
        }
        // bt server on if forcefully closed
        btServerEnableIfDisable();

        if (isEnable()) {
            if (!btName.equals(mBluetoothAdapter.getName())) {
                setName(btName);
            }

            boolean isScanningStarted = mBluetoothAdapter.startDiscovery();
            MeshLog.w("[File][Speed]BT Discovery started : " + isScanningStarted);
            registerDiscReceiver(null);

        } else if (!mHasForcedOff) {

            BTStateReceiver btStateReceiver = new BTStateReceiver();

            String finalBtName = btName;
            btStateReceiver.mBTStateListener = isOn -> {
                if (isOn) {
                    TransportManager transportManager = TransportManagerX.getInstance();
                    if (!transportManager.isBtEnabled) {
                        requestDiscoverableTimePeriod();
                    }
                    if (!finalBtName.equals(mBluetoothAdapter.getName())) {
                        setName(finalBtName);
                    }

                    boolean isScanningStarted = mBluetoothAdapter.startDiscovery();
                    MeshLog.w("[File][Speed]BT Discovery started:" + isScanningStarted);
                    registerDiscReceiver(null);

                    mContext.unregisterReceiver(btStateReceiver);
                } else {
                    MeshLog.e("[startDiscovery]Requested BT to turn on although did turned off");
                }
            };

            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            mContext.registerReceiver(btStateReceiver, filter);
            mBluetoothAdapter.enable();
        }
//        }
    }


    private void requestDiscoverableTimePeriod() {

        Intent startActivity = new Intent();
        startActivity.setClass(mContext, BTDiscoveryTimeRequester.class);
        startActivity.setAction(BTDiscoveryTimeRequester.class.getName());
        startActivity.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mContext.startActivity(startActivity);
    }

    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    public boolean cancelDiscovery() {
        try {
            mContext.unregisterReceiver(mBluetoothDiscoveryReceiver);

        } catch (IllegalArgumentException illegalArgumentException) {
            illegalArgumentException.printStackTrace();
        }
        boolean isCancelled = mBluetoothAdapter.cancelDiscovery();
        Timber.d("[File][Speed]%s", isCancelled);
        return false;
    }

    public boolean setName(String newName) {
        if (Text.isNotEmpty(newName)) {
            return mBluetoothAdapter.setName(newName);
        }
        return false;
    }

    public String getName() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getName();
        }
        return null;
    }

    /**
     * If BT turned on then register discovery receiver.
     * If turned off then turn BT on and wait for response and register discovery receiver upon BT
     * turned on
     */
    public void registerDiscReceiver(BluetoothDiscoveryReceiver.BTDiscoveryListener
                                             btDiscoveryListener) {
        if (btDiscoveryListener != null && mBluetoothDiscoveryReceiver != null) {
            mBTDiscoveryListener = btDiscoveryListener;
        }

        int userMode = PreferencesHelper.on().getDataShareMode();

        if (userMode == PreferencesHelper.INTERNET_USER) return;

        String btName = SharedPref.read(Constant.KEY_DEVICE_BLE_NAME);
        if (TextUtils.isEmpty(btName)) {
            MeshLog.e("BT Name empty in registerDiscReceiver in BTManager");
            return;
        }
        if (isEnable()) {
            if (!btName.equals(mBluetoothAdapter.getName())) {
                setName(btName);
            }

            registerBTDiscoveryReceiver();

        } else if (!mHasForcedOff) {

            BTStateReceiver btStateReceiver = new BTStateReceiver();
            btStateReceiver.mBTStateListener = isOn -> {
                if (isOn) {
                    setName(btName);
                    registerBTDiscoveryReceiver();
                    mContext.unregisterReceiver(btStateReceiver);
                } else {
                    MeshLog.e("[registerDiscReceiver]Requested BT to turn on although did turned off");
                }
            };


            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            mContext.registerReceiver(btStateReceiver, filter);
            mBluetoothAdapter.enable();
        }
    }

    private void registerBTDiscoveryReceiver() {
        if (mBluetoothDiscoveryReceiver != null) {
            MeshLog.i("Register:: ble device receiver");
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mBluetoothDiscoveryReceiver, intentFilter);
        }
    }

    private void stop() {
        cancelDiscovery();
        try {
            mContext.unregisterReceiver(mBTStateReceiver);

        } catch (IllegalArgumentException illegalArgumentException) {
            illegalArgumentException.printStackTrace();
        }
        mIsBtTurningOff = true;

        // Now it off for testing purpose
        //mBluetoothAdapter.disable();
    }

    public void destroy() {

        //stop();
        sBTManager = null;
    }

    public void advertiseAPCredentials(String ssid, String presharedKey) {
        if (Text.isNotEmpty(ssid) && Text.isNotEmpty(presharedKey)) {
            String advertiseString = getName() + SEPARATOR + ssid + SEPARATOR + presharedKey;
            setName(advertiseString);
            MeshLog.v("[Multi-hop] Advertising over BT:" + ssid + ":" + presharedKey);
        }
    }

    public void setAPListener(APListener APListener) {
        mAPListener = APListener;
    }

}