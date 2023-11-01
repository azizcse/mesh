package com.w3engineers.mesh.model;

import com.w3engineers.mesh.db.routing.RoutingEntity;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Azizul Islam on 2/24/21.
 */
public class PendingMessage {
    /**
     * Instance variable
     */
    public String messageId;
    public String actualSender;
    public String actualReceiver;
    public String previousSender;
    public byte[] messageData;

    public Queue<RoutingEntity> routeQueue = new ConcurrentLinkedQueue<>();

    public RoutingEntity previousAttemptEntity;

    public PendingMessage() {
    }

    public PendingMessage(String messageId, String actualSender, String actualReceiver,
                          String immediateSender, byte[] messageData, RoutingEntity entity) {

        this.messageId = messageId;
        this.actualSender = actualSender;
        this.actualReceiver = actualReceiver;
        this.previousSender = immediateSender;
        this.messageData = messageData;
        this.previousAttemptEntity = entity;
    }
}
