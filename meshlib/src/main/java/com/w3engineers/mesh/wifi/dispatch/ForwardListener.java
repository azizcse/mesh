package com.w3engineers.mesh.wifi.dispatch;

/**
 * <p>To notify App layer, or to count that
 * the current user are using to forward other message (Data)</p>
 * Like act as a Data forwarder.
 * By this listener app can show the count of forward time
 * or award as message forwarder
 */
public interface ForwardListener {
    /**
     * Current forwarded message information
     *
     * @param sender    Sender id
     * @param receiver  receiver id
     * @param messageId message id
     * @param frameData Main data
     */
    void onMessageForwarded(String sender, String receiver, String messageId,int transferId,
                            byte[] frameData);
}
