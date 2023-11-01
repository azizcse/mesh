package com.w3engineers.mesh.helper;

/**
 * Created by Azizul Islam on 2/24/21.
 */
public interface MiddleManMessageStatusListener {
    void onMiddleManMessageSendStatusReceived(String messageId, boolean isSuccess);
}
