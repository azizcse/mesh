package com.w3engineers.hardwareoff;

public interface Callback {
    void onGetBluetoothOffCallback();

    void onGetWifiOffCallback();

    void onGetWifiHotspotCallback();

    void onGetBlueToothEnableCallback();

    void onGetWifiEnableCallback();
}
