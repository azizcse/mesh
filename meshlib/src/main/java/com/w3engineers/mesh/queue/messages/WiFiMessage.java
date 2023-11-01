package com.w3engineers.mesh.queue.messages;

import com.w3engineers.mesh.httpservices.MeshHttpServer;
import com.w3engineers.mesh.util.MeshLog;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Wifi Message provider
 */
public class WiFiMessage extends BaseMeshMessage implements Comparable {

    public String mIp;
    //public Frames.Frame mFrame;
    public JSONObject jsonObject;
    int mTimeOut = MeshHttpServer.DEFAULT_CONNECTION_TIMEOUT;

    @Override
    public int send() {
        if (mData != null && mData.length > 0) {
            try {
                int result = MeshHttpServer.on().sendMessage(mIp, mData, mTimeOut);
                return result;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            MeshLog.e("Empty message over WiFi. Data:" + mData);
        }
        return MESSAGE_STATUS_FAILED;
    }

    @Override
    public void receive() {
        MeshLog.i("WiFiMessage receive");
        //MeshHttpServer.on().parseReceivedData(mFrame, mIp);
        MeshHttpServer.on().parseReceivedData(jsonObject, mIp);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public String toString() {
        return "WiFiMessage{" +
                "mIp='" + mIp + '\'' +
                ", jsonObject=" + jsonObject +
                ", mTimeOut=" + mTimeOut +
                ", mMaxRetryCount=" + mMaxRetryCount +
                ", mRetryCount=" + mRetryCount +
                ", mInternalId=" + mInternalId +
                ", messageId='" + messageId + '\'' +
                ", mData=" + new String(mData) +
                '}';
    }
}
