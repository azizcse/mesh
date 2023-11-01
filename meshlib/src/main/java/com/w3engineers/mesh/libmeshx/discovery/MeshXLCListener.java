package com.w3engineers.mesh.libmeshx.discovery;

import com.w3engineers.mesh.db.routing.RoutingEntity;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Contains method to communicate at App layer
 */
public interface MeshXLCListener {

    /**
     * When a new Peer receive
     *
     */
    void onConnectWithGO(String ssid, boolean isSpecialConnection);

    /**
     * Called when current device is GO, still somehow connects with a GO
     */
    void onConnectWithGOBeingGO(boolean wasDisconnected);

    void onConnectWithAdhocPeer(String ip, int port);

    /**
     * on remove a peer
     */
    void onDisconnectWithGO(String disconnectedFrom);

    void onDisconnectedWithAdhoc(String address);

    void onWifiUserDisconnected(List<RoutingEntity> entityList);

    void onConnectedWithTargetNode(String nodeId);

    void onGetLegacyUser(ConcurrentLinkedQueue<RoutingEntity> allWifiUserQueue);

    void onSendDisconnectMessageToBle(String address);

    void disconnectUserPassToUi(String userId);
}

