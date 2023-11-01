package com.w3engineers.mesh.premission;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;


/**
 * Provides Location related broadcast event
 */
public class LocationProviderStateBroadcastReceiver extends BroadcastReceiver {

    public interface LocationProviderStateListener {
        void onDisabled();
        void onEnabled();
    }

    private LocationProviderStateListener mLocationProviderStateListener;

    public LocationProviderStateBroadcastReceiver(LocationProviderStateListener locationProviderStateListener) {
        mLocationProviderStateListener = locationProviderStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isGpsEnabled || isNetworkEnabled) {
                // Handle Location turned ON
                if(mLocationProviderStateListener != null) {
                    mLocationProviderStateListener.onEnabled();
                }
            } else {
                // Handle Location turned OFF
                if(mLocationProviderStateListener != null) {
                    mLocationProviderStateListener.onDisabled();
                }
            }
        }
    }
}
