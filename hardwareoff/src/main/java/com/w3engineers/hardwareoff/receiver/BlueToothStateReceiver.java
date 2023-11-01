package com.w3engineers.hardwareoff.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.w3engineers.hardwareoff.Callback;
import com.w3engineers.hardwareoff.HardwareStateManager;

/*
 *  ****************************************************************************
 *  * Created by : Md Tariqul Islam on 8/2/2019 at 11:23 AM.
 *  * Email : tariqul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md Tariqul Islam on 8/2/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */


public class BlueToothStateReceiver extends BroadcastReceiver {
    private final String TAG = "HardwareTest";
    private Callback mCallback;
    private HardwareStateManager hardwareStateManager;

    public BlueToothStateReceiver(Callback mCallback,HardwareStateManager manager) {
        this.mCallback = mCallback;
        this.hardwareStateManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

            if (state == BluetoothAdapter.STATE_OFF) {
                if (mCallback != null &&
                        hardwareStateManager.isBlueToothOff()
                        && !hardwareStateManager.isBluetoothCalled()) {
                    Log.d(TAG, "Receiver: blutooth off");
                    mCallback.onGetBluetoothOffCallback();

                }
            } else if (state == BluetoothAdapter.STATE_ON) {
                if (hardwareStateManager.isReEnableCall() && mCallback != null) {
                    Log.e(TAG, "Receiver: Bluetooth enable");
                    mCallback.onGetBlueToothEnableCallback();
                    mCallback = null;
                    hardwareStateManager.unregisterBleReceiver();
                }
            }
        }

    }
}
