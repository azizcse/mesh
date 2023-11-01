package com.w3engineers.mesh.queue.messages;


import com.w3engineers.mesh.bluetooth.BleLink;

import org.json.JSONObject;

/**
 * Bluetooth Message manager
 */
public class BTMessage extends BaseMeshMessage {

    public BleLink mBleLink;
    //public Frames.Frame mFrame;
    public JSONObject jsonObject;
    @Override
    public int send() {
        if (mBleLink != null) {
            return mBleLink.sendMeshMessage(mData);
        }
        return MESSAGE_STATUS_FAILED;
    }

    @Override
    public void receive() {
        this.mBleLink.processReceiveMessage(jsonObject);
        //this.mBleLink.processReceiveMessage(mFrame);
    }
}
