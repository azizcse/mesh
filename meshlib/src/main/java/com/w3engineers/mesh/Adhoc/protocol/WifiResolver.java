package com.w3engineers.mesh.Adhoc.protocol;

import java.net.InetAddress;

import javax.jmdns.ServiceEvent;

/**
 * <h1> Zeroconf service update is maintained here </h1>
 */
public interface WifiResolver {
    interface Listener{
        void onBonjourServiceResolved(String userAddress, String ip, int port);
        void serviceLost(ServiceEvent serviceEvent);
    }

    void start(InetAddress address, int port);

    void startPublishOnly(InetAddress address, int port);

    void startResolveOnly(InetAddress address);

    void stop();

    boolean isRunning();

    void stopServiceListening();

}
