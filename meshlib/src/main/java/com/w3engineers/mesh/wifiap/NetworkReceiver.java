/*
 *  ****************************************************************************
 *  * Created by : Monir Zzaman on 16-Feb-17 at 7:21 PM.
 *  * Email : moniruzzaman@w3engineers.com
 *  *
 *  * Last edited by : Monir Zzaman on 16-Feb-17.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */
package com.w3engineers.mesh.wifiap;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.w3engineers.mesh.util.PermissionUtil;

import java.util.List;

/**
 * Wifi Connection update manages here
 */
public class NetworkReceiver extends BroadcastReceiver {

    public interface Listener {
        void onScanResultFound(List<ScanResult> results);
    }

    private Listener wifiListener;

    public NetworkReceiver(Listener listener) {
        wifiListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (PermissionUtil.init(context).isAllowed(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            String action = intent.getAction();

            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {

                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                if (wifiManager != null) {

                    List<ScanResult> results = wifiManager.getScanResults();
                    wifiListener.onScanResultFound(results);

                }

            }
        }
    }

}
