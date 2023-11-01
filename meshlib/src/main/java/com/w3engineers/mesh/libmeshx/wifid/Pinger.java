package com.w3engineers.mesh.libmeshx.wifid;

import android.util.Patterns;

import com.w3engineers.ext.strom.util.Text;

import com.w3engineers.mesh.httpservices.MeshHttpServer;
import com.w3engineers.mesh.libmeshx.wifi.WiFiClient;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.JsonDataBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

/**
 * Generic class to check whether given IP has the app alive or not. Respond using callback
 * {@link PingListener}
 */
public class Pinger implements Runnable {

    //Use 2 seconds extra then of disconnection timeout
    private static final int PING_TIMEOUT_IN_MILLIS = (int) (WiFiClient.WIFI_DISCONNECTION_TIMEOUT + 2000);
    private int mMaxNumberOfRetry;

    public interface PingListener {
        void onPingResponse(String ip, boolean isReachable);
    }

    private String mIp;
    private PingListener mPingListener;
    private byte[] PING_MSG;
    private int mPingTimeOut;

    public Pinger(String toIp, PingListener pingListener, int maxNumberOfRetry) {
        this(toIp, pingListener, maxNumberOfRetry, PING_TIMEOUT_IN_MILLIS);
    }

    public Pinger(String toIp, PingListener pingListener, int maxNumberOfRetry,
                  int pingTimeOutInSeconds) {
        this.mPingTimeOut = pingTimeOutInSeconds;
        this.mIp = toIp;
        this.mPingListener = pingListener;
        this.mMaxNumberOfRetry = maxNumberOfRetry;
        JSONObject jo = new JSONObject();
        try {
            jo.put(JsonDataBuilder.KEY_MESSAGE_TYPE, JsonDataBuilder.HEART_BEAT);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        PING_MSG = jo.toString().getBytes();
    }

    @Override
    public void run() {

        if(Text.isNotEmpty(mIp) && Patterns.IP_ADDRESS.matcher(mIp).matches()) {

            boolean isSuccess = false;
            int attemptCount = 0;

            do {

                try {

                    isSuccess = MeshHttpServer.on().sendMessage(mIp, PING_MSG, mPingTimeOut)
                            == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    MeshLog.e("[MeshX]Ping failed for:" + mIp + "-" + e.getMessage());
                }

            } while (++attemptCount < mMaxNumberOfRetry && !isSuccess);

            if (mPingListener != null) {
                mPingListener.onPingResponse(mIp, isSuccess);
            }
        }

    }
}
