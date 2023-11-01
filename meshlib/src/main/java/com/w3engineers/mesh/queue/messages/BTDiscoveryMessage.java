package com.w3engineers.mesh.queue.messages;

import com.w3engineers.mesh.queue.DispatcherHelper;
import com.w3engineers.mesh.util.MeshLog;

/**
 * Bluetooth Discovery message
 */
public class BTDiscoveryMessage extends BTMessage {

    public interface DataPuller {
        byte[] getData();
    }

    //All BT and WiFi discover messages are timely mutually exclusive so put logs to check whether
    // they ever contradict before starting and ending of either sending and receiving any of that

    private DataPuller mDataPuller;

    public BTDiscoveryMessage(DataPuller dataPuller) {
        mDataPuller = dataPuller;
    }

    public BTDiscoveryMessage() {
    }

    @Override
    public int send() {
        mData = mDataPuller.getData();
        int responseCode = super.send();
        DispatcherHelper dispatcherHelper = DispatcherHelper.getDispatcherHelper();
        synchronized (dispatcherHelper.lock) {
            dispatcherHelper.mCountBTDiscovering--;
        }
        return responseCode;
    }

    @Override
    public void receive() {
        MeshLog.i("[BT-Discover] Start of receiving...");
        super.receive();
        DispatcherHelper dispatcherHelper = DispatcherHelper.getDispatcherHelper();
        synchronized (dispatcherHelper.lock) {
            dispatcherHelper.mCountBTDiscovering--;
        }
        MeshLog.i("[BT-Discover] End of receiving.");
    }
}
