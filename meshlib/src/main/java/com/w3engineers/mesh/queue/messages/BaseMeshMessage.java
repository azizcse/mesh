package com.w3engineers.mesh.queue.messages;

/**
 * POJO for Base message
 */
public abstract class BaseMeshMessage {

    public static final int DEFAULT_MESSAGE_ID = -1;

    public static final int DEFAULT_MESSAGE_STATUS = 0;
    public static final int MESSAGE_STATUS_FAILED = -1;
    public static final int MESSAGE_STATUS_SUCCESS = 1;

    public int mMaxRetryCount;
    public int mRetryCount;
    public int mInternalId = DEFAULT_MESSAGE_ID;
    public String messageId;
    public byte[] mData;
    public abstract int send();
    public abstract void receive();

}