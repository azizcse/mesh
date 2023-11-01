package com.w3engineers.mesh.wifidirect.connector;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.w3engineers.mesh.libmeshx.wifi.WiFiClientState;
import com.w3engineers.mesh.libmeshx.wifi.WiFiClientStateReceiver;
import com.w3engineers.mesh.libmeshx.wifi.WiFiConnectionHelper;
import com.w3engineers.mesh.libmeshx.wifid.APCredential;
import com.w3engineers.mesh.libmeshx.wifid.CustomPriorityQueue;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifidirect.listener.WiFiDirectStatusListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Azizul Islam on 12/9/20.
 */
public class WiFiConnector implements WiFiClientState, Android10ConnectState {


    public interface SoftApConnectionListener {
        void onAttemptFinish(APCredential credential);
    }

    private final long WIFI_CONNECTION_TIMEOUT = 32 * 1000L;
    private final int SOFTAP_ATTEMPT_COUNT = 2;
    private Context mContext;
    private WiFiDirectStatusListener statusListener;
    private WiFiClientStateReceiver mWiFiClientStateReceiver;
    private WiFiConnectionHelper mWiFiHelper;
    private Map<String, APCredential> allDiscoveredApCredential;
    private CustomPriorityQueue<APCredential> customPriorityQueue;
    private Connector apConnector;
    private volatile boolean isWifiConnected;

    public WiFiConnector(Context context, WiFiDirectStatusListener listener) {
        this.mContext = context;
        this.statusListener = listener;
        allDiscoveredApCredential = new HashMap<>();
        mWiFiHelper = new WiFiConnectionHelper(context, this);
        customPriorityQueue = new CustomPriorityQueue<APCredential>(50, (item1, item2) -> {
            if (item1.discoverTime > item2.discoverTime) {
                return -1;
            } else if (item1.discoverTime < item2.discoverTime) {
                return 1;
            }
            return 0;
        });
        apConnector = new Connector(apConnectionListener, mWiFiHelper);

        mWiFiClientStateReceiver = new WiFiClientStateReceiver(context, this);
    }

    private Runnable connectionAttemptTimeOut = new Runnable() {
        @Override
        public void run() {
            if (!isWifiConnected) {
                statusListener.onConnectionAttemptTimeout(isWifiConnected);
            }
        }
    };

    @Override
    public void onConnected() {
        MeshLog.i("Android_10 [MeshX]In onConnected");

        if (!isWifiConnected) {
            isWifiConnected = true;
            AndroidUtil.removeBackground(connectionAttemptTimeOut);

            statusListener.onWifiConnect(mWiFiHelper.getConnectionInfo());

            statusListener.onConnectionAttemptTimeout(isWifiConnected);

            if (apConnector != null) {
                apConnector.stopConnector();
            }
        }

    }

    @Override
    public void onDisconnected() {
        MeshLog.i("Android_10 wifi disconnect");
        isWifiConnected = false;
        statusListener.onWifiDisconnect();
    }

    /**
     * This method develop called only from mesh initialization
     * Developer should not called this method any where from the project
     * <p>
     * Instead called this developer should called only  public boolean disConnect(String from)
     * method
     */
    public void initialWifiDisconnection(String from) {
        MeshLog.e("[p2p_process]: initialWifiDisconnection called from--- :" + from);
        if (mWiFiHelper.disconnect()) {
            MeshLog.v("[p2p_process] initialization other network disconnected");
            mWiFiHelper.disableConfiguredWiFiNetwork(mWiFiHelper.getConnectedNetworkId());
        } else {
            MeshLog.e("[p2p_process] initialization other network disconnection failed");
        }
    }

    /**
     * Disassociate from currently active network.
     * Works well upto API 28.
     * Also disable the network which is connected.
     *
     * @return
     */
    public boolean disConnect(String from) {
        MeshLog.e("[p2p_process]: wifi disconnect called from--- :" + from + " IsConnected :" + isWifiConnected);
        if (isWifiConnected) {

            if (mWiFiHelper.disconnect()) {
                mWiFiHelper.disableConfiguredWiFiNetwork(mWiFiHelper.getConnectedNetworkId());
                return true;
            }

            if (apConnector != null) {
                apConnector.stopConnector();
            }
        }

        return false;
    }

    /**
     * Connection attempt and Soft ap map  manage
     */
    public APCredential getConnectedGoCredential(String ssId) {
        ssId = ssId.replace("\"", "");
        for (Map.Entry<String, APCredential> entry : allDiscoveredApCredential.entrySet()) {
            //MeshLog.e("P2P SSID:  ssid: "+ssId+" in map : "+entry.getKey());
            if (entry.getKey().equals(ssId)) {
                return entry.getValue();
            }
        }
        return null;
    }


    public void softApConnection(APCredential credential) {

        if (!apConnector.isConnecting()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                apConnector.connect(credential);
            } else {
                //MeshLog.e("P2P SSID: ** ATTEMPT to connect with** =" + credential.mSSID);
                //MeshLog.v("[p2p_process]: Attempt to connect with ssid :" + credential.mSSID + " pass :" + credential.mPassPhrase);
                credential.attemptCount += 1;
                //mWiFiHelper.connect(credential.mSSID,credential.mPassPhrase);
                apConnector.connect(credential);
                //mWiFiHelper.connect(credential.mSSID, credential.mPassPhrase);
                AndroidUtil.postBackground(connectionAttemptTimeOut, WIFI_CONNECTION_TIMEOUT);
            }
        }

        boolean isAdded = customPriorityQueue.add(credential);
        allDiscoveredApCredential.put(credential.mSSID, credential);
    }


    public void setHighBandMode() {
        if (mWiFiHelper != null) {
            mWiFiHelper.setHighBand();
        }
    }

    public void releaseHighBandMode() {
        if (mWiFiHelper != null) {
            mWiFiHelper.releaseHighBand();
        }
    }


    public void stopApConnector() {
        apConnector.stopConnector();
    }

    private WiFiConnector.SoftApConnectionListener apConnectionListener = new WiFiConnector.SoftApConnectionListener() {
        @Override
        public void onAttemptFinish(APCredential credential) {

            MeshLog.v("P2P SSID: Ap connection attempt count :" + credential.attemptCount);

            if (credential.attemptCount >= SOFTAP_ATTEMPT_COUNT) {
                customPriorityQueue.remove(credential);
                APCredential nextCredential = (APCredential) customPriorityQueue.peek();
                if (nextCredential != null) {
                    softApConnection(nextCredential);
                }
            } else {
                reattemptToConnect(credential);
            }
        }
    };

    private void reattemptToConnect(APCredential credential) {
        if (!isWifiConnected) {
            MeshLog.v("P2P SSID: reattempt to connect with :" + credential.attemptCount);
            credential.attemptCount += 1;
            apConnector.connect(credential);
        } else {
            MeshLog.v("P2P SSID: Connection attempt success stop connector");
            stopApConnector();
            customPriorityQueue.clear();
        }
    }

    @Override
    public void onAttemptFinish(boolean isSuccess) {
        MeshLog.e("Android 10 connection state : " + isSuccess);
        apConnector.resetConnectingState();

        if (!isSuccess) {
            HandlerUtil.postForeground(() -> Toast.makeText(mContext,
                    "Intermesh required wifi connection to run perfectly", Toast.LENGTH_SHORT).show());
        }
        statusListener.onConnectionAttemptTimeout(isSuccess);
    }

}


class Connector implements Runnable {

    private Thread thread;
    private volatile boolean isRunning;
    private APCredential apCredential;
    private WiFiConnectionHelper connectionHelper;
    private WiFiConnector.SoftApConnectionListener listener;

    public Connector(WiFiConnector.SoftApConnectionListener listener, WiFiConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
        this.listener = listener;
    }

    public boolean isConnecting() {
        return isRunning;
    }

    public void resetConnectingState() {
        isRunning = false;
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
        //MeshLog.v("[p2p_process]: Hs connection attempt in progress ");
        connectionHelper.forgetNetworks();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectionHelper.connect(apCredential);
        } else {
            connectionHelper.connect(apCredential);
            try {
                Thread.sleep(15 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isRunning = false;
            listener.onAttemptFinish(apCredential);
        }
    }

}



