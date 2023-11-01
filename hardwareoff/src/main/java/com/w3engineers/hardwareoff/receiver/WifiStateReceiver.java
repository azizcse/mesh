package com.w3engineers.hardwareoff.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.w3engineers.hardwareoff.Callback;
import com.w3engineers.hardwareoff.HardwareStateManager;

/*
 *  ****************************************************************************
 *  * Created by : Md Tariqul Islam on 8/2/2019 at 12:27 PM.
 *  * Email : tariqul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md Tariqul Islam on 8/2/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */


public class WifiStateReceiver extends BroadcastReceiver {
    private Callback mCallback;
    private HardwareStateManager hardwareStateManager;
    private final String TAG = "HardwareTest";

    public WifiStateReceiver(Callback mCallback,HardwareStateManager manager) {
        this.mCallback = mCallback;
        this.hardwareStateManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int info = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            int prevInfo = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, -1);

            //Log.d(TAG, "Receiver: State: " + info);
            // Log.d(TAG, "Receiver: Prev State: " + prevInfo);

            if (WifiManager.WIFI_STATE_DISABLED == info) {
                if (mCallback != null && !hardwareStateManager.isWifiOffCalled()) {
                    Log.d(TAG, "Receiver: WIFI off");
                    mCallback.onGetWifiOffCallback();

                }
            } else if (WifiManager.WIFI_STATE_ENABLED == info) {
                if (hardwareStateManager.isReEnableCall() && mCallback != null) {
                    Log.e(TAG, "Receiver: WIFI Enable");
                    mCallback.onGetWifiEnableCallback();
                    mCallback = null;
                    hardwareStateManager.unregisterWifiReceiver();
                }
            }

        }
    }
}
