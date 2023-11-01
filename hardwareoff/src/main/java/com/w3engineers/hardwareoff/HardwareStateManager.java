package com.w3engineers.hardwareoff;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.w3engineers.hardwareoff.callback.AllHardwareOffListener;
import com.w3engineers.hardwareoff.callback.BluetoothCallback;
import com.w3engineers.hardwareoff.callback.HardwareEnableListener;
import com.w3engineers.hardwareoff.callback.ResetListener;
import com.w3engineers.hardwareoff.callback.WifiCallback;
import com.w3engineers.hardwareoff.callback.WifiHotspotCallback;
import com.w3engineers.hardwareoff.receiver.BlueToothStateReceiver;
import com.w3engineers.hardwareoff.receiver.WifiHotSpotReceiver;
import com.w3engineers.hardwareoff.receiver.WifiStateReceiver;

public class HardwareStateManager implements Callback {
    private final String TAG = "HardwareTest";
    private static HardwareStateManager sInstance;
    private BluetoothStateTracker BLETracker;
    private WifiStateTracker wifiStateTracker;
    private Context mContext;
    public static final String AP_SSID = "mesh", AP_PRESHARED_KEY = "belowNougat";

    private final long DELAY_TIME = 500;

    private boolean isResetStart;

    private boolean isBleEnabled, isWifiEnabled;

    /**
     * Hardware off tracker element for track all hardware
     * off or not
     */
    private boolean isBluetoothOff;
    private boolean isWifiOff;
    private boolean isHotspotOff;

    // Callback initializer
    private BluetoothCallback mBleCallback;
    private WifiCallback mWifiCallback;
    private WifiHotspotCallback mWifiHotspotCallback;
    private HardwareEnableListener mEnableListener;
    private AllHardwareOffListener mHardwareOffListener;
    private ResetListener mResetListener;


    private BlueToothStateReceiver bleReceiver;
    private WifiHotSpotReceiver wifiHotSpotReceiver;
    private WifiStateReceiver wifiStateReceiver;

    /**
     * It is need to track when re enable call cause first disable call
     * if re enable call the we call enable all.
     * We are re-using same methods.
     */
    private boolean isReEnableCall;

    /**
     * These below two variables arse also same for
     * tracking when hardware (BLE or WIFI) off then
     * enable will be called
     */
    private boolean isResetWifiCall;
    private boolean isResetBluetoothCall;

    /**
     * Extra checking for enabling wifi or reset
     * wifi that hotspot is enable or not
     */
    private boolean wifiEnableForHotspot;


   /* public static HardwareStateManager getInstance() {
        if (sInstance == null) {
            sInstance = new HardwareStateManager();
        }

        return sInstance;
    }*/


    public void init(Context context) {
        this.mContext = context;
        BLETracker = new BluetoothStateTracker();
        wifiStateTracker = WifiStateTracker.getInstance();


        // register receiver

        bleReceiver = new BlueToothStateReceiver(this, this);
        wifiHotSpotReceiver = new WifiHotSpotReceiver(this, this);
        wifiStateReceiver = new WifiStateReceiver(this, this);

        context.registerReceiver(bleReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        context.registerReceiver(wifiHotSpotReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver, filter1);

        isReEnableCall = false;

        initBlueToothTracker();
        initWifiStateTracker();

        //startPermissionActivity();
    }

    private void initBlueToothTracker() {
        BLETracker.initAdapter(this);
    }

    private void initWifiStateTracker() {
        wifiStateTracker.initWifiStateTracker(mContext, this);

    }

    private void startPermissionActivity() {
        Intent startActivity = new Intent();
        startActivity.setClass(mContext, PermissionActivity.class);
        startActivity.setAction(PermissionActivity.class.getName());
        startActivity.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mContext.startActivity(startActivity);
    }

    private void clearHardwareTracker() {
        isResetStart = false;

        isWifiOff = false;
        isBluetoothOff = false;
        isHotspotOff = false;
    }


    // for extra checking
    public boolean isBlueToothOff() {
        return BLETracker.isBluetoothOff();
    }

    public boolean isWifiOff() {
        return wifiStateTracker.isWifiOff();
    }

    public boolean isHotspotOff() {
        return !wifiStateTracker.isHotspotEnabled();
    }

    //getter

    public boolean isReEnableCall() {
        return isReEnableCall;
    }

    public boolean isBluetoothCalled() {
        return isBluetoothOff;
    }

    public boolean isWifiOffCalled() {
        return isWifiOff;
    }

    /*
     * Public api start
     *
     * */


    public void disableBluetooth(BluetoothCallback callback) {
        this.mBleCallback = callback;
        BLETracker.offBlueTooth();
    }


    public void disableWifi(WifiCallback callback) {
        this.mWifiCallback = callback;
        wifiStateTracker.offWifi();
    }


    public void disableHotspot(WifiHotspotCallback wifiHotspotCallback) {
        wifiStateTracker.setWifiHotspotCallback(wifiHotspotCallback);
        wifiStateTracker.disableHotspot();
    }

    public void enableHotspot(WifiHotspotCallback wifiHotspotCallback) {
        wifiStateTracker.setWifiHotspotCallback(wifiHotspotCallback);
        wifiStateTracker.enableHotSpot();
    }

    public void enableBluetooth(BluetoothCallback callback) {
        isReEnableCall = true;
        this.mBleCallback = callback;
        BLETracker.enableBlueTooth();
    }

    public void enableWifi(WifiCallback callback) {
        Log.d(TAG, "Enable wifi on call");
        isReEnableCall = true;
        this.mWifiCallback = callback;
        if (wifiStateTracker.isHotspotEnabled()) {
            wifiEnableForHotspot = true;
            Log.d(TAG, "Hotspot on");
            disableHotspot(null);
        } else {
            wifiStateTracker.onWifi();
        }
    }

    /**
     * This method shutdown the all hardware.
     * and you will get different callback if you want
     * for bluetooth you will get {@link BluetoothCallback}
     * for Wifi you will get {@link WifiCallback}
     * for Wifi Hotspot you will get {@link WifiHotspotCallback}
     * <p>
     * And last yu will get {@link AllHardwareOffListener}
     *
     * @return this
     */
    public void disableAll(AllHardwareOffListener listener) {
        this.mHardwareOffListener = listener;
        disableBluetooth(this.mBleCallback);
        disableWifi(this.mWifiCallback);
        disableHotspot(this.mWifiHotspotCallback);
    }

    public void disableAll(AllHardwareOffListener listener, WifiCallback wifiCallback, BluetoothCallback bleCallback, WifiHotspotCallback hotspotCallback) {
        this.mHardwareOffListener = listener;
        mBleCallback = bleCallback;
        mWifiCallback = wifiCallback;
        mWifiHotspotCallback = hotspotCallback;
        disableBluetooth(this.mBleCallback);
        disableWifi(this.mWifiCallback);
        disableHotspot(this.mWifiHotspotCallback);
    }

    /**
     * It is ver important to call {@link #enableAll}.
     * Warning: Don`t call {@link #enableAll} method after {@link #disableAll(AllHardwareOffListener)} ()}
     * immediately .
     * Call {@link #enableAll} when you get {@link AllHardwareOffListener} callback
     * <p>
     * And you get re-enable callback by using {@link HardwareEnableListener}
     */
    public void enableAll(final HardwareEnableListener listener) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isReEnableCall = true;
                mEnableListener = listener;
                enableBluetooth(mBleCallback);
                enableWifi(mWifiCallback);
            }
        }, DELAY_TIME);
    }

    public void enableAll(final HardwareEnableListener listener, final WifiCallback wifiCallback, final BluetoothCallback bleCallback) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isReEnableCall = true;
                mEnableListener = listener;
                mBleCallback = bleCallback;
                mWifiCallback = wifiCallback;
                enableBluetooth(mBleCallback);
                enableWifi(mWifiCallback);
            }
        }, DELAY_TIME);
    }

    /**
     * This method is responsible for restat the hardware.
     * First shutdown the hardware and second re-enable the Wifi and Bluetooth.
     * And you will get two callback
     * 1.  {@link AllHardwareOffListener} callback
     * 2. {@link HardwareEnableListener}
     */
    public void reset(ResetListener listener) {
        isResetStart = true;
        this.mResetListener = listener;
        disableAll(mHardwareOffListener);
    }

    public void reset(ResetListener listener, WifiCallback wifiCallback, BluetoothCallback bleCallback) {
        isResetStart = true;
        this.mResetListener = listener;
        mBleCallback = bleCallback;
        mWifiCallback = wifiCallback;
        disableAll(mHardwareOffListener);
    }

    public void resetWifi(WifiCallback wifiCallback) {
        Log.d(TAG, "reset wifi call");
        this.mWifiCallback = wifiCallback;
        isResetWifiCall = true;
        disableWifi(mWifiCallback);
    }

    public void resetBluetooth(BluetoothCallback bleCallback) {
        mBleCallback = bleCallback;
        isResetBluetoothCall = true;
        disableBluetooth(mBleCallback);
    }

    /*
     * unregister receiver
     * */
    public void unregisterBleReceiver() {
        mContext.unregisterReceiver(bleReceiver);
    }

    public void unregisterWifiReceiver() {
        try {

            mContext.unregisterReceiver(wifiStateReceiver);

        } catch (IllegalArgumentException illegalArgumentException) {

            illegalArgumentException.printStackTrace();
        }
    }

    public void unregisterHotspotReceiver() {
        mContext.unregisterReceiver(wifiHotSpotReceiver);
    }

    // Callback
    @Override
    public void onGetBluetoothOffCallback() {

        if (mBleCallback != null) {
            mBleCallback.onGetBluetoothEnableCallback(false);
        }

        if (isResetBluetoothCall) {
            isResetBluetoothCall = false;
            enableBluetooth(mBleCallback);
        }

        isBluetoothOff = true;
        if (isWifiOff && isBluetoothOff && isHotspotOff) {

            if (mHardwareOffListener != null) {
                mHardwareOffListener.onGetAllHardwareOff();
            }

            if (isResetStart) {
                enableAll(this.mEnableListener);
            }

            clearHardwareTracker();
        }
    }

    @Override
    public void onGetWifiOffCallback() {
        isWifiEnabled = false;
        if (mWifiCallback != null) {
            mWifiCallback.onGetWifiEnableCallback(false);
        }

        if (isResetWifiCall) {
            isResetWifiCall = false;
            //HardwareTrackerUtils.isEnableCalled = false;
            enableWifi(mWifiCallback);
        }

        HardwareTrackerUtils.isDisableCalled = false;

        isWifiOff = true;
        if (isWifiOff && isBluetoothOff && isHotspotOff) {

            if (mHardwareOffListener != null) {
                mHardwareOffListener.onGetAllHardwareOff();
            }

            if (isResetStart) {
                enableAll(this.mEnableListener);
            }

            clearHardwareTracker();

        }
    }

    @Override
    public void onGetWifiHotspotCallback() {

        if (wifiEnableForHotspot) {
            wifiEnableForHotspot = false;
            Log.d(TAG, "wifi on call");
            enableWifi(mWifiCallback);
        }

        if (mWifiHotspotCallback != null) {
            mWifiHotspotCallback.onDisabledHotspot();
        }

        isHotspotOff = true;
        if (isWifiOff && isBluetoothOff && isHotspotOff) {
            if (mHardwareOffListener != null) {
                mHardwareOffListener.onGetAllHardwareOff();
            }

            if (isResetStart) {
                enableAll(this.mEnableListener);
            }

            clearHardwareTracker();
        }
    }

    @Override
    public void onGetBlueToothEnableCallback() {

        if (mBleCallback != null) {
            mBleCallback.onGetBluetoothEnableCallback(true);
        }

        isBleEnabled = true;
        if (isBleEnabled && isWifiEnabled) {
            isBleEnabled = false;
            isWifiEnabled = false;

            isReEnableCall = false;

            if (mEnableListener != null) {
                mEnableListener.onGetHardwareReEnable();
            }

            if (mResetListener != null) {
                mResetListener.onGetResetListener();
            }
        }
    }

    @Override
    public void onGetWifiEnableCallback() {

        if (mWifiCallback != null && !isWifiEnabled) {
            HardwareTrackerUtils.isEnableCalled = false;
            mWifiCallback.onGetWifiEnableCallback(true);
        }


        isWifiEnabled = true;
        if (isBleEnabled && isWifiEnabled) {
            isBleEnabled = false;
            isWifiEnabled = false;

            isReEnableCall = false;

            if (mEnableListener != null) {
                mEnableListener.onGetHardwareReEnable();
            }

            if (mResetListener != null) {
                mResetListener.onGetResetListener();
            }

        }
    }

}
