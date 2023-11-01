package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.w3engineers.mesh.util.MeshLog;

/**
 * <h1>Bluetooth connection related callback is manged here</h1>
 */
public class BTStateReceiver extends BroadcastReceiver {

    public interface BTStateListener {
        /**
         *
         * @param isOn true if turned on. false if it is off
         */
        void onBTStateChange(boolean isOn);
    }
    public BTStateListener mBTStateListener;

    public BTStateReceiver(BTStateListener BTStateListener) {
        mBTStateListener = BTStateListener;
    }

    public BTStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    MeshLog.i("[BTState]Bluetooth off");
                    if(mBTStateListener != null) {
                        mBTStateListener.onBTStateChange(false);
                    }
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    MeshLog.i("[BTState]Turning Bluetooth off...");
                    break;
                case BluetoothAdapter.STATE_ON:
                    MeshLog.i("[BTState]Bluetooth on");
                    if(mBTStateListener != null) {
                        mBTStateListener.onBTStateChange(true);
                    }
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    MeshLog.i("[BTState]Turning Bluetooth on...");
                    break;
            }
        } else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
            int prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
            int now = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
            MeshLog.i("BT-Log-prev:"+prev+":now:"+now);
        }
    }
}
