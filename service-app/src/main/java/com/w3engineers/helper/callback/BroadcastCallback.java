package com.w3engineers.helper.callback;

import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.models.BroadcastData;

public interface BroadcastCallback {

    void sendDataToApp(BroadcastData broadcastData, boolean isReceivedMode);
    TransportManagerX getTransPort();
    int getPeerConnectionType(String peerId);
    boolean isDirectConnected(String peerId);
}
