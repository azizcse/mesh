package com.w3engineers.mesh.tunnel;

public class TunnelConnectionStatus {
    /*public enum ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED
    }*/
    private static int mConnectionStatus = 1;
    private static boolean IS_SSH_CONNECTED = false;

    public static int isConnectionStatus() {
        return mConnectionStatus;
    }

    public static void setConnectionStatus(int connectionStatus) {
        mConnectionStatus = connectionStatus;
    }

    public static void setSshTunnelConnected(boolean isTunnelConnected) {
        IS_SSH_CONNECTED = isTunnelConnected;
    }

    public static boolean isSshConnected() {
        return IS_SSH_CONNECTED;
    }
}
