package com.w3engineers.mesh.bluetooth;

/**
 * <h1>Access point availability notified here</h1>
 */
public interface APListener {
    void onAPAvailable(String ssid, String preSharedKey);
}
