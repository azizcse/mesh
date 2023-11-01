package com.w3engineers.mesh.ble.message;

public interface BleAppMessageListener {
    void onReceiveMessage(String senderId, byte[] message);

    void onMessageSendingStatus(String messageId, boolean isSuccess);

    void onReceivedFilePacket(String sender, byte[] data);
}
