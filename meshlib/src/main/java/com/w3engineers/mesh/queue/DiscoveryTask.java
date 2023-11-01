package com.w3engineers.mesh.queue;

import com.w3engineers.mesh.httpservices.MeshHttpServer;

import org.json.JSONObject;

/**
 * Created by Azizul Islam on 11/17/20.
 */
public abstract class DiscoveryTask implements Runnable {
    public interface DataPuller {
        byte[] getData();
    }
    public int mTimeOut = MeshHttpServer.DEFAULT_CONNECTION_TIMEOUT;
    public int messageInternalId;
    public String title;
    public String senderId;
    public int retryCount = 0;
    public int maxRetryCount = 2;
    public boolean isHelloPacket;

    public String messagePublicId;
    public String ipOrAddress; // For ble it will be node id and for wifi will be ip address
    public byte[] messageData;

    public int type;
    public JSONObject jsonObject;

    public DiscoveryTask(String sender, int type, JSONObject jsonObject) {
        this.senderId = sender;
        this.type = type;
        this.jsonObject = jsonObject;
    }


    public DiscoveryTask(String messageId, String ipOrAddress, byte[] messageData) {
        this.messagePublicId = messageId;
        this.ipOrAddress = ipOrAddress;
        this.messageData = messageData;
    }

    //For ble

    public String receiverId;
    public DataPuller puller;

    public DiscoveryTask(String receiverId, DataPuller puller) {
        this.receiverId = receiverId;
        this.puller = puller;
    }

    public DiscoveryTask(String receiverId, DataPuller puller, boolean isHelloPacket) {
        this.receiverId = receiverId;
        this.puller = puller;
        this.isHelloPacket = isHelloPacket;
    }

}
