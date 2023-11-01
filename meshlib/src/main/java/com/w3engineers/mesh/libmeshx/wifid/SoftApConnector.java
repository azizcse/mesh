/*
package com.w3engineers.mesh.libmeshx.wifid;

import android.content.Context;

import com.w3engineers.mesh.libmeshx.wifi.WiFiClient;
import com.w3engineers.mesh.libmeshx.wifi.WiFiConnectionHelper;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WiFiUtil;

import java.util.HashMap;
import java.util.Map;

*/
/**
 * <h1>Soft ap connector class<h1/>
 * <p>Attempt only one at a time and twice per credential.
 * Check connectivity and make decision.
 * Maintain a Credential map if multiple credential found and attempt to connect one by one<p/>
 *//*

public class SoftApConnector {
    public interface ApConnectionListener {
        void onAttemptFinish(APCredential credential);
    }

    private Map<String, APCredential> apCredentialMap;
    private Context context;
    private ApConnector apConnector;
    private final int SOFTAP_ATTEMPT_COUNT = 2;
    private WiFiClient.ConneectionListener conneectionListener;
    public SoftApConnector(Context context, WiFiClient.ConneectionListener conneectionListener) {
        this.context = context;
        this.apCredentialMap = new HashMap<>();
        apConnector = new ApConnector(context, apConnectionListener);
        this.conneectionListener = conneectionListener;
    }

    public void makeConnection(APCredential credential) {
        if (P2PUtil.isConnectedWithPotentialGO(context)) {
            MeshLog.v("P2P SSID: Device already connected with SoftAp :" + WiFiUtil.getConnectedSSID(context));
            apCredentialMap.clear();
            return;
        }

        if (!apConnector.isConnecting()) {
            credential.attemptCount += 1;
            apConnector.connect(credential);
        }

        APCredential existCredential = apCredentialMap.get(credential.mSSID);

        if (existCredential == null) {
            apCredentialMap.put(credential.mSSID, credential);
        }

    }

    public void stopApConnector(){
        apConnector.stopConnector();
    }


    private ApConnectionListener apConnectionListener = new ApConnectionListener() {
        @Override
        public void onAttemptFinish(APCredential credential) {

            MeshLog.v("P2P SSID: Ap connection attempt count :" + credential.attemptCount);

            if (credential.attemptCount >= SOFTAP_ATTEMPT_COUNT) {
                apCredentialMap.remove(credential.mSSID);
                if (!apCredentialMap.isEmpty()) {
                    Map.Entry<String, APCredential> entry = apCredentialMap.entrySet().iterator().next();
                    APCredential value = entry.getValue();
                    MeshLog.v("P2P SSID: attempt with new ap :" + value.mSSID);
                    makeConnection(value);
                }else {
                    if(!P2PUtil.isConnectedWithPotentialGO(context)) {
                        conneectionListener.onTimeOut();
                    }
                }

            } else {
                makeConnection(credential);
            }
        }
    };

}

class ApConnector implements Runnable {

    private Thread thread;
    private boolean isRunning;
    private APCredential apCredential;
    private WiFiConnectionHelper connectionHelper;
    private SoftApConnector.ApConnectionListener listener;

    public ApConnector(Context context, SoftApConnector.ApConnectionListener listener) {
        this.connectionHelper = new WiFiConnectionHelper(context);
        this.listener = listener;
    }

    public boolean isConnecting() {
        return isRunning;
    }

    public boolean stopConnector() {
        if (!isRunning) return false;
        thread.interrupt();
        isRunning = false;
        return true;
    }

    public boolean connect(APCredential credential) {
        if (isRunning) return false;
        isRunning = true;
        apCredential = credential;
        thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        return true;
    }

    @Override
    public void run() {
        MeshLog.v("P2P SSID: Hs connection attempt in progress ");
        connectionHelper.connect(apCredential.mSSID, apCredential.mPassPhrase);
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isRunning = false;
        listener.onAttemptFinish(apCredential);

    }
}

*/
