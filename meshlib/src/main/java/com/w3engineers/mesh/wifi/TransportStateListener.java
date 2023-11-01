package com.w3engineers.mesh.wifi;

import com.w3engineers.mesh.LinkMode;

/**
 * Bluetooth connection type manages here
 */
public interface TransportStateListener {
    void onBluetoothChannelConnect(LinkMode linkMode);

    void onWifiP2pUserConnected(boolean isLastHello, String senderAddress);

    void onReceiveHelloFromClient(String userId);

    void onBleUserConnect(boolean isServer, String directNodeId);

    void onRemoveRedundantBleConnection(String userId);

    void onReceiveSoftApCredential(String ssid, String password, String goNodeId);

    void onMaximumLcConnect();

    void onGetForceConnectionRequest(String receiverId, String ssid, String password,
                                     boolean isRequest, boolean isBle, boolean isAbleToReceive);

    void onGetFileFreeModeMessage(String senderId, String receiverId, boolean isAvailable);

    void onReceivedNetworkFullResponseFromGo(String senderId);
}
