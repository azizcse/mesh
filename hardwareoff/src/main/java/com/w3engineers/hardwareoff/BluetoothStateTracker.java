package com.w3engineers.hardwareoff;


import android.bluetooth.BluetoothAdapter;
import android.util.Log;

class BluetoothStateTracker {
    private boolean isBlueToothOpen;
    private BluetoothAdapter mAdapter;
    private Callback mCallback;

    private final String TAG = "HardwareTest";

    void initAdapter(Callback callback) {
        mCallback = callback;
        mAdapter = BluetoothAdapter.getDefaultAdapter();


        // offBlueTooth();
    }

    void offBlueTooth() {
        if (mAdapter.isEnabled()) {
            boolean isDisable = mAdapter.disable();
            Log.d(TAG, "isDisable: " + isDisable);
           /* if (isDisable && mCallback != null) {
                mCallback.onGetBluetoothOffCallback();
            }*/
        } else {
            if (mCallback != null) {
                mCallback.onGetBluetoothOffCallback();
            }
        }
    }

    boolean isBluetoothOff() {
        if (mAdapter != null) {
            return !mAdapter.isEnabled();
        }
        return false;
    }

    void enableBlueTooth() {
        if (mAdapter != null) {
            if (mAdapter.isEnabled()) {
                mCallback.onGetBlueToothEnableCallback();
            } else {
                mAdapter.enable();
            }
        }
    }

}
