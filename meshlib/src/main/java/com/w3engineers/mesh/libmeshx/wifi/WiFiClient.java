package com.w3engineers.mesh.libmeshx.wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;

import com.w3engineers.mesh.libmeshx.discovery.MeshXLogListener;
import com.w3engineers.mesh.libmeshx.wifid.APCredential;
import com.w3engineers.mesh.libmeshx.wifid.CustomPriorityQueue;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;


/**
 * Maintain WiFi client related scenarios
 */
public class WiFiClient implements WiFiClientState {

    private final long WIFI_CONNECTION_TIMEOUT = 30 * 1000L;

    // Don't change the value without team discussion
    public static final long WIFI_DISCONNECTION_TIMEOUT = 1 * 1000L;

    public interface ConneectionListener {
        void onConnected(WifiInfo wifiConnectionInfo);

        void onTimeOut();

        void onDisconnected();

        void connectionAttemptFinish();

    }

    public interface SoftApConnectionListener {
        void onAttemptFinish(APCredential credential);
    }

    private WiFiConnectionHelper mWiFiHelper;
    private WiFiClientStateReceiver mWiFiClientStateReceiver;
    private volatile boolean mIsConnected;
    private MeshXLogListener mMeshXLogListener;
    private ConneectionListener mConnectionListener;
    private Map<String, APCredential> allDiscoveredApCredential;

    //private Map<String, APCredential> apCredentialMap;
    private CustomPriorityQueue<APCredential> customPriorityQueue;
    private ApConnector apConnector;
    private final int SOFTAP_ATTEMPT_COUNT = 2;

    private Runnable mTimeOutTask = new Runnable() {
        @Override
        public void run() {
            if (!mIsConnected && mConnectionListener != null) {
                mConnectionListener.onTimeOut();
            }
            mConnectionListener.connectionAttemptFinish();

        }
    };

    public WiFiClient(Context context) {

        mWiFiClientStateReceiver = new WiFiClientStateReceiver(context, this);
        //mWiFiHelper = new WiFiConnectionHelper(context);
        allDiscoveredApCredential = new HashMap<>();
        //apCredentialMap = new HashMap<>();
        customPriorityQueue = new CustomPriorityQueue<APCredential>(50, (item1, item2) -> {
            if (item1.discoverTime > item2.discoverTime) {
                return -1;
            } else if (item1.discoverTime < item2.discoverTime) {
                return 1;
            }
            return 0;
        });
        apConnector = new ApConnector(apConnectionListener, mWiFiHelper);
    }

    public void setMeshXLogListener(MeshXLogListener meshXLogListener) {
        mMeshXLogListener = meshXLogListener;
    }

    /**
     * Start connecting with provided ssid with given passphrase. This method works only
     * if it is not connected with any network.
     *
     * @param ssid
     * @param passPhrase
     * @return
     */
    public boolean connect(String ssid, String passPhrase) {

        if (!mIsConnected) {
            if (mMeshXLogListener != null) {
                mMeshXLogListener.onLog("[CONNECTING] SSID - " + ssid + "::Passphrase - "
                        + passPhrase);
            }

            //mWiFiHelper.disconnect();
            /*if (mWiFiHelper.connect(ssid, passPhrase)) {
                AndroidUtil.postBackground(mTimeOutTask, WIFI_CONNECTION_TIMEOUT);
                return true;
            }*/
        }

        return true;
    }

    /**
     * Disassociate from currently active network.
     * Works well upto API 28.
     * Also disable the network which is connected.
     *
     * @return
     */
    public boolean disConnect(String from) {
        MeshLog.e("P2P SSID: wifi disconnect called from :" + from);
        if (mIsConnected) {

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

    public void setConnectionListener(ConneectionListener conneectionListener) {
        mConnectionListener = conneectionListener;
    }

    public void destroy() {
        disConnect("destroy()");
        if (mWiFiClientStateReceiver != null) {
            mWiFiClientStateReceiver.destroy();
        }

        if (apConnector != null) {
            apConnector.stopConnector();
        }
    }

    public boolean isConnected() {
        return mIsConnected;
    }


    public boolean isWiFiOn() {
        return mWiFiHelper.isWiFiOn();
    }

    @Override
    public void onConnected() {

        MeshLog.i("[DC-Issue][MeshX]In onConnected");
        AndroidUtil.removeBackground(mTimeOutTask);

        Timber.d("Connected to WiFi");

        if (!mIsConnected) {
            mIsConnected = true;
            if (mConnectionListener != null) {
                mConnectionListener.onConnected(mWiFiHelper.getConnectionInfo());
            }

            if (apConnector != null) {
                apConnector.stopConnector();
            }

            if (mMeshXLogListener != null) {
                mMeshXLogListener.onLog("[Connected]");
            }
        }
    }

    @Override
    public void onDisconnected() {
        MeshLog.v("[DC-IssueWiFi]onDisconnected");
        mIsConnected = false;

        MeshLog.v("Disconnected from WiFi");
        if (mConnectionListener != null) {
            mConnectionListener.onDisconnected();
        }
    }

    public void setHighBand() {
        if (mWiFiHelper != null) {
            mWiFiHelper.setHighBand();
        }
    }

    public void releaseHighBand() {
        if (mWiFiHelper != null) {
            mWiFiHelper.releaseHighBand();
        }
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
            //MeshLog.e("P2P SSID: ** ATTEMPT to connect with** =" + credential.mSSID);
            MeshLog.v("P2P SSID: Attempt to connect with ssid :" + credential.mSSID + " pass :" + credential.mPassPhrase);
            credential.attemptCount += 1;
            apConnector.connect(credential);

            AndroidUtil.postBackground(mTimeOutTask, WIFI_CONNECTION_TIMEOUT);
        }

        boolean isAdded = customPriorityQueue.add(credential);
        allDiscoveredApCredential.put(credential.mSSID, credential);

    }

    public void stopApConnector() {
        apConnector.stopConnector();
    }


    private SoftApConnectionListener apConnectionListener = new SoftApConnectionListener() {
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
        if (!mIsConnected) {
            MeshLog.v("P2P SSID: reattempt to connect with :" + credential.attemptCount);
            credential.attemptCount += 1;
            apConnector.connect(credential);
        } else {
            MeshLog.v("P2P SSID: Connection attempt success stop connector");
            stopApConnector();
            customPriorityQueue.clear();
        }
    }
}


class ApConnector implements Runnable {

    private Thread thread;
    private volatile boolean isRunning;
    private APCredential apCredential;
    private WiFiConnectionHelper connectionHelper;
    private WiFiClient.SoftApConnectionListener listener;

    public ApConnector(WiFiClient.SoftApConnectionListener listener, WiFiConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
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
        //connectionHelper.connect(apCredential.mSSID, apCredential.mPassPhrase);
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isRunning = false;
        listener.onAttemptFinish(apCredential);

    }
}

