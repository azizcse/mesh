package com.w3engineers.mesh.wifidirect;

/**
 * <h1>Wifi direct error type
 *    Wifi direct has some error type, like not supported , Bussy and error
 *    System only provide integer value for this error. This class
 *    is responsible for indicate proper.
 * </h1>
 *
 */
public enum WiFiP2PError {

    ERROR(0), P2P_NOT_SUPPORTED(1), BUSSY(2);

    private int reason;

    WiFiP2PError(int reason) {
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }

    public static WiFiP2PError fromReason(int reason) {
        for (WiFiP2PError wiFiP2PError : WiFiP2PError.values()) {
            if (reason == wiFiP2PError.reason) {
                return wiFiP2PError;
            }
        }

        return null;
    }
}
