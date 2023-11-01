package com.w3engineers.mesh.util;

/**
 * Created by Azizul Islam on 1/4/21.
 */
public interface MessageCallback {
    default void onMessageSend(boolean isSuccess){}
    default void onBleMessageSend(String messageId, boolean isSuccess){}
}
