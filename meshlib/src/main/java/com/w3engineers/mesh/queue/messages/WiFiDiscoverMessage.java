package com.w3engineers.mesh.queue.messages;

import com.w3engineers.mesh.util.MeshLog;

/**
 * WifiDiscovery message provider
 */
public class WiFiDiscoverMessage extends WiFiMessage {

    public static final int DISCOVERY_MESSAGE_TIMEOUT = 3 * 1000;
    public static final int DISCOVERY_MESSAGE_MAX_RETRY = 3;

    public interface DataPuller {
        byte[] getData();
    }

    public WiFiDiscoverMessage() {
        super();
        //Discovery message time out only 3 seconds for each iteration
        mTimeOut = DISCOVERY_MESSAGE_TIMEOUT;
        mMaxRetryCount = DISCOVERY_MESSAGE_MAX_RETRY;
    }

    private DataPuller mDataPuller;

    public WiFiDiscoverMessage(WiFiDiscoverMessage.DataPuller dataPuller) {
        this();
        mDataPuller = dataPuller;
    }

    //All BT and WiFi discover messages are timely mutually exclusive so put logs to check whether
    // they ever contradict before starting and ending of either sending and receiving any of that

    @Override
    public int send() {
        MeshLog.i("[WiFi-Discover] Attempt:"+mRetryCount+". Start of sending...");
        mData = mDataPuller.getData();
        int x = super.send();
        MeshLog.i("[WiFi-Discover] End of sending attempt:"+mRetryCount+".");
        return x;
    }

    @Override
    public void receive() {
        MeshLog.i("[WiFi-Discover] Start of receiving...");
        super.receive();
        MeshLog.i("[WiFi-Discover] End of receiving.");
    }

    @Override
    public String toString() {
        return "WiFiDiscoverMessage{" +
                "mDataPuller=" + mDataPuller +
                '}';
    }
}
