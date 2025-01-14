package com.w3engineers.purchase.manager;

public interface PayEventListener {
     void onMessageReceived(String sender, byte[] paymentData);
     void onPayMessageAckReceived(String sender, String receiver, String messageId);
     void onUserConnected(String nodeId);
     void onUserDisconnected(String nodeId);
}
