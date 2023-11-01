package com.w3engineers.mesh.util.log;

import android.os.Build;

/**
 * Provides Device Info
 */
public class DeviceInfo {

    /** Returns the consumer friendly device name */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        }
        return manufacturer + " " + model;
    }

}
