package com.w3engineers.mesh.queue;

/**
 * Provides messageID and Status
 */
public interface MessageListener {
    /**
     * Response upon any confirmation (either fail or success) to immediate next node. This always
     * may not be the destination.
     *
     * @param messageId
     * @param messageStatus
     */
    void onMessageSend(int messageId, String ipAddress, boolean messageStatus);

    default void onConnectedWithTargetNode(String targetNodeId) {
    }

    default void onGetFileFreeModeToSend(String targetNodeId) {
    }

    /**
     * This call back will execute when a target node not found.
     * <p>
     * Target not found may call when target node is busy or target actually
     * not reachable or timeout for connection
     *
     * @param targetNodeId
     */
    default void onGetFileFileUserNotFound(String targetNodeId,boolean isFromBusyState) {
    }


}
