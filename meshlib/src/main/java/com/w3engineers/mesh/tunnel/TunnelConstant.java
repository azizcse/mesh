package com.w3engineers.mesh.tunnel;

/**
 * Contain all tunnel related constant
 */
public class TunnelConstant {
    public static final String dotRemoteUrl = "sish.telemesh.net";
    // public static final int serverSSHPort = 2888;
    public static final int serverSSHPort = 4000;
    // public static final int serverHTTPPort = 90;
    public static final int serverHTTPPort = 1777;
    public static final int serverConnected = 1;
    public static final int serverDisconnected = 2;
    public static final int maxCounterReconnect = 10; // 10 times
    public static final int reconnectDelay = 20 * 1000; // 5 second
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    public static final int DISCONNECTED = 1;

}
