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
 *  * Created by : Md Tariqul Islam on 8/2/2019 at 12:29 PM.
 *  * Email : tariqul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md Tariqul Islam on 8/2/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */


public class WifiHotSpotReceiver extends BroadcastReceiver {
    private Callback mCallback;
    private HardwareStateManager hardwareStateManager;
    private final String TAG = "HardwareTest";

    public WifiHotSpotReceiver(Callback mCallback, HardwareStateManager manager) {
        this.mCallback = mCallback;
        this.hardwareStateManager = manager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {

            // get Wi-Fi Hotspot state here
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            if (WifiManager.WIFI_STATE_DISABLED == state % 10) {
                // Wifi is enabled
                Log.d(TAG, "Receiver: WIFI hotspot off");
                if (mCallback != null && hardwareStateManager.isHotspotOff()) {
                    mCallback.onGetWifiHotspotCallback();
                    hardwareStateManager.unregisterHotspotReceiver();
                }
            }
        }
    }
}
