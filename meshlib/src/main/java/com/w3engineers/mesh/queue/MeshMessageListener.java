package com.w3engineers.mesh.queue;

/**
 * Mesh Message listener
 */
public interface MeshMessageListener extends MessageListener {
    void onWifiDirectMessageSend(String messageId, boolean status);
    void onWifiHelloMessageSend(String ip, boolean status);
}
