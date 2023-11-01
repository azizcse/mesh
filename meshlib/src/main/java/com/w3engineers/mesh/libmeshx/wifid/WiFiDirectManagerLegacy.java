package com.w3engineers.mesh.libmeshx.wifid;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.controller.DriverManager;
import com.w3engineers.mesh.controller.ManagerStateListener;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.httpservices.MeshHttpServer;
import com.w3engineers.mesh.libmeshx.adhoc.nsd.NSDHelper;
import com.w3engineers.mesh.libmeshx.discovery.MeshXAPListener;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLCListener;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLogListener;
import com.w3engineers.mesh.libmeshx.wifi.WiFiClient;
import com.w3engineers.mesh.libmeshx.wifi.WiFiConnectionHelper;
import com.w3engineers.mesh.libmeshx.wifi.WiFiStateMonitor;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WiFiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * For all WiFi direct or WiFiP2P related tasks. Support Legacy Client
 * connectivity
 */
public class WiFiDirectManagerLegacy {


    public static final int SEARCHING_SCHEDULED_MIN = P2PServiceSearcher.SCAN_SLOT_INTERNAL_MAX;
    public static final int SEARCHING_SCHEDULED_MAX = SEARCHING_SCHEDULED_MIN + 15;
    private final int SEARCHER_BUSY_GO_BUFFERED_TIME = (int) ((P2PServiceSearcher.SEARCHER_RESTART_INTERVAL
            + 1.5) * 1000);
    //    private final long WIFI_OFF_DECIDER_DELAY = 1500;
    private final long WIFI_OFF_DECIDER_DELAY = 0;
    private final int START_TASK_ONLY_AP = 1;
    private final int START_TASK_ONLY_SEARCH = 2;
    private final int START_TASK_ALL = 3;
    private final long SOFT_DELAY_TO_START_P2P_SERVICES = 1000;
    private final int INITIAL_AP_SEARCH_DELAY_MIN = 5;
    private final int INITIAL_AP_SEARCH_DELAY_MAX = 10;

    private WiFiStateMonitor mWiFiStateMonitor;
    public WiFiMeshConfig mWiFiMeshConfig;
    private Context mContext;
    private WiFiConnectionHelper mWiFiConnectionHelper;
    private volatile static WiFiDirectManagerLegacy sWiFiDirectManagerLegacy;
    private volatile SoftAccessPoint mSoftAccessPoint;
    private volatile SoftAccessPointSearcher mSoftAccessPointSearcher;
    private WiFiClient mWiFiClient;
    private MeshXLogListener mMeshXLogListener;
    private MeshXAPListener mMeshXAPListener;
    private MeshXLCListener mMeshXLCListener;
    private String mNetworkName;
    private WiFiStateMonitor.WiFiAdapterStateListener mWiFiAdapterStateListener;
    public boolean mHasForcedOff;
    //    private SoftApConnector softApConnector;
    private String mBroadCastInfo;
    private volatile boolean mIsConnectingWithSearchedSubnet;

    private APCredential mSoftApCredential;
    private static ManagerStateListener managerStateListener;
    private volatile boolean mIsSearchingNode;
    private volatile boolean isForceConnectionByCredential;

    private Runnable mRestartScheduledTask = this::restartScheduledTask;

    /**
     * Maintain temporary state of connecting ssid. So that we can track whether system is in
     * connecting state or not. e.g: saved network connection. Take no decision at this moment
     * except a read only value
     */

    private volatile String mConnectingSSID;
    private volatile boolean mIsConnectivityPause;
    public WiFiStateListener mWiFiStateListener;

    private SoftAccessPoint.SoftAPStateListener mSoftAPStateListener = new SoftAccessPoint.SoftAPStateListener() {
        @Override
        public void onSoftAPChanged(boolean isEnabled, String ssidName, String password) {
            if (Text.isNotEmpty(ssidName) && !TextUtils.isEmpty(password)) {
                mNetworkName = ssidName;
                MeshLog.v("P2P SSID: Go created ssid :" + ssidName);
                mSoftApCredential = new APCredential(ssidName, password, 0);
                WifiCredential.ssid = ssidName;
                WifiCredential.password = password;
                /*if (managerStateListener != null) {
                    managerStateListener.onSoftApCreated(mSoftApCredential);
                } else {
                    Log.e("Go_created", "listener null");
                }*/
            }
            if (mMeshXAPListener != null) {
                mMeshXAPListener.onSoftAPStateChanged(isEnabled, ssidName);
            }

            if (mSoftAccessPointSearcher != null) {
                if (isEnabled) {
                    mSoftAccessPointSearcher.addBlockedList(ssidName);
                } else {
                    mSoftAccessPointSearcher.removeFromBlockedList(ssidName);
                }
            }

            MeshLog.v("[GO] State changed to:" + isEnabled);
        }

        @Override
        public void onSoftApConnectedWithNodes(Collection<WifiP2pDevice> wifiP2pDevices) {
            if (mMeshXAPListener != null) {
                mMeshXAPListener.onGOConnectedWith(wifiP2pDevices);
            }

            if (mSoftAccessPointSearcher != null) {
                mSoftAccessPointSearcher.stop();
            }
        }

        @Override
        public void onSoftApDisConnectedWithNodes(String ip) {
            if (mMeshXAPListener != null) {

                mMeshXAPListener.onGODisconnectedWith(ip);

            }
        }
    };

    private SoftAccessPointSearcher.ServiceFound mServiceFound = new SoftAccessPointSearcher.ServiceFound() {
        @Override
        public void onServiceFoundSuccess(String ssid, String passPhrase, String searchMeta, WifiP2pDevice p2pDevice) {
            MeshLog.i("[WDC]onServiceFoundSuccess:" + ssid);
            if (mSoftAccessPointSearcher != null && !mSoftAccessPointSearcher.mIsPauseConnectivity
                    && P2PUtil.isPotentialGO(ssid)) {
                mConnectingSSID = ssid;

                if (mMeshXLogListener != null) {
                    mMeshXLogListener.onLog("[FOUND] SSID - " + ssid + "::Passphrase - " + passPhrase);
                }

                //As service found, so remove any pending GO generation request
                AndroidUtil.removeBackground(mStartSearchingWithGO);
                AndroidUtil.removeBackground(mStartGO);
                MeshLog.v("[p2p_process] remove Go, Lc schedule task onServiceFoundSuccess");
                AndroidUtil.removeBackground(mRestartScheduledTask);
                if (mSoftAccessPoint != null) {
                    mSoftAccessPoint.stop();
                    mSoftAccessPoint = null;
                }

                mSoftAccessPointSearcher.stop();
                mSoftAccessPointSearcher = null;

                if (mWiFiStateListener != null) {
                    mWiFiStateListener.onServiceFound(ssid, searchMeta);
                }

                mIsConnectingWithSearchedSubnet = Text.isNotEmpty(searchMeta);

                if (mWiFiMeshConfig.mIsGroupOwner && mSoftAccessPoint != null &&
                        mSoftAccessPoint.isGoAlive()) {
                    int delay = 1000;

                    AndroidUtil.post(() -> {
                        connectWithSoftAp(ssid, passPhrase, false);
                    }, delay);
                } else {
                    connectWithSoftAp(ssid, passPhrase, false);
                }
            }

            if (mWiFiMeshConfig != null && CollectionUtil.hasItem(mWiFiMeshConfig.mSearchList)) {
                mWiFiMeshConfig.mSearchList.remove(searchMeta);
            }
        }

        @Override
        public void onP2pAlreadyConnected(String ssid) {
            boolean isWifiConnected = RouteManager.getInstance().isWifiUserConnected();
            if (mMeshXLCListener != null && !isWifiConnected) {
                mMeshXLCListener.onConnectWithGO(ssid, false);
            }
        }
    };

    private WiFiClient.ConneectionListener mConnectionListener = new WiFiClient.ConneectionListener() {

        /**
         * We are connected. Check whether we are connected with any GO. If not then we disconnect
         * and in turns re attemp for connection
         * @param wifiConnectionInfo
         */
        @Override
        public void onConnected(final WifiInfo wifiConnectionInfo) {
            isForceConnectionByCredential = false;
            mIsConnectingWithSearchedSubnet = false;
            String ssidName = wifiConnectionInfo.getSSID();
            MeshLog.v("[p2p_process] WiFiClient.ConnectionListener onConnected() with:" + ssidName
                    + ":" + mWiFiMeshConfig);

            if (mSoftAccessPointSearcher != null && mSoftAccessPointSearcher.isBlocked(ssidName)) {

                MeshLog.v("[p2p_process] Connected with blocked SSID:" + ssidName);
                if (mWiFiClient != null) {
                    mWiFiClient.disConnect("onConnected() blocked ssid");
                }
                return;
            }
            boolean isPotentialGO = P2PUtil.isPotentialGO(ssidName);

            mSoftApCredential = mWiFiClient.getConnectedGoCredential(ssidName);

            if (mSoftApCredential != null) {
                WifiCredential.ssid = mSoftApCredential.mSSID;
                WifiCredential.password = mSoftApCredential.mPassPhrase;
            } else {
                //WifiCredential.ssid = null;
            }

            if (mSoftApCredential != null)
                MeshLog.e("[p2p_process] I,m connected with ssid :" + mSoftApCredential.mSSID + " pass :" + mSoftApCredential.mPassPhrase);
            //If my role was dual and GO is closed, then we check physically the p2p IP
            if (isPotentialGO && mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                //If me is a GO then we do not allow any connection with any other GO

                MeshLog.e("[p2p_process] Connected with:" + ssidName + " being a GO");
                //We sent an interruption to app layer.
                mMeshXLCListener.onConnectWithGOBeingGO(mWiFiClient.disConnect("being a GO"));

            } else if (mWiFiMeshConfig.mIsClient || mIsSearchingNode) {//Works iff me contains a client mode

                MeshLog.e("On P2P connected");

                if (mSoftAccessPointSearcher != null) {
                    mSoftAccessPointSearcher.stop();
                }


                mNetworkName = ssidName;

                if (isPotentialGO) {
                    MeshLog.v("[p2p_process] after connection, commectedSSID: " + mConnectingSSID + " foundSSID: " + ssidName);
                    if (WiFiUtil.isSameSSID(mConnectingSSID, ssidName)) {
                        MeshLog.e("On connected with valid GO");
                        if (mMeshXLCListener != null) {
                            mMeshXLCListener.onConnectWithGO(wifiConnectionInfo.getSSID(), false);
                        } else {
                            MeshLog.e("mMeshXLCListener null");
                        }

                        mIsSearchingNode = mWiFiMeshConfig.mIsGroupOwner = false;
                        mWiFiMeshConfig.mIsClient = true;

                        //As service found, so remove any pending GO generation request
                        AndroidUtil.removeBackground(mStartSearchingWithGO);
                        AndroidUtil.removeBackground(mStartGO);
                        MeshLog.v("[p2p_process] remove Go, Lc schedule task onConnected");
                        AndroidUtil.removeBackground(mRestartScheduledTask);
                        if (mSoftAccessPoint != null) {
                            mSoftAccessPoint.stop();
                            mSoftAccessPoint = null;
                        }
                        //--------------------------------------------

                        MeshLog.v("[p2p_process] mConnectingSSID set null from  onConnected()");
                        mConnectingSSID = null;

                        //TODO Connecting queue should be refreshed

                    } else {
                        //Unexpected P2P network
                        //Disconnect from the network and restart required services
                        MeshLog.e("On connected with previously saved GO. Expected:" +
                                mConnectingSSID + "--found:" + ssidName);
                        /*if (mWiFiClient.isConnected()) {
                            MeshLog.v("Is wifi connection call disconnected");
                            mWiFiConnectionHelper.removeNetwork(wifiConnectionInfo.getNetworkId());
                            mWiFiClient.disConnect("mConnectionListener");
                        }*/
                    }
                } else {//Adhoc
                    MeshLog.e("Connected With invalid GO ");
                }

                /*if (TransportManagerX.getInstance().isHighBandEnabled()) {
                    setHighBandMode();
                }*/
            } else {
                MeshLog.v("[p2p_process] After connection nothing happen");
            }

            if (mWiFiStateListener != null) {
                mWiFiStateListener.onConnected(ssidName);
            }
        }

        @Override
        public void onTimeOut() {
            MeshLog.v("[p2p_process] WiFiClient.ConneectionListener onTimeOut()");

            String ssid = mConnectingSSID;

            MeshLog.v("[p2p_process] mConnectingSSID set null from  onTimeOut()");
            mConnectingSSID = null;
            if (mWiFiMeshConfig.mIsClient) {
                MeshLog.v("[Meshx][onTimeOut]");

                if (mWiFiClient != null && !mWiFiClient.isConnected()) {

                    if (mMeshXLogListener != null) {
                        mMeshXLogListener.onLog("[OnTimeOut]");
                    }

                    if (mWiFiMeshConfig != null && mWiFiMeshConfig.mIsClient) {
                        reAttemptServiceDiscovery();
                    }
                }
            }
            mIsConnectingWithSearchedSubnet = false;

            if (mWiFiStateListener != null) {
                mWiFiStateListener.onTimeout(ssid);
            }
        }

        /**
         * We are connected so enabling GO and Service searcher
         */
        @Override
        public void onDisconnected() {
            MeshLog.v("[p2p_process]: WiFiClient.ConneectionListener onDisconnected()");
            if (isForceConnectionByCredential) {
                isForceConnectionByCredential = false;
                return;
            }
            if (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                MeshLog.v("Disconnected being a valid GO so returning without any further" +
                        "processing");
                return;
            }

            if (mMeshXLCListener != null) {
               // mMeshXLCListener.onDisconnectWithGO(mNetworkName);
            }

            if (mMeshXLogListener != null) {
                mMeshXLogListener.onLog("[onDisconnected]");
            }

            if (mWiFiStateMonitor != null && mWiFiClient != null && mWiFiClient.isWiFiOn()) {
                //Due to forceful reset of WiFi we do not want to have false alarm by state monitor
                mWiFiStateMonitor.destroy();
            }

            if (!mIsConnectingWithSearchedSubnet) {

                MeshLog.v("[p2p_process] mConnectingSSID set null from  onDisconnected()");
                mConnectingSSID = null;
                reAttemptServiceDiscovery();
            }

            if (mWiFiStateListener != null) {
                mWiFiStateListener.onDisConnected(mNetworkName);
            }

            WifiCredential.ssid = null;
        }

        @Override
        public void connectionAttemptFinish() {
            if (mWiFiStateListener != null) {
                mWiFiStateListener.connectionAttemptFinish();
            }
        }
    };


    public void connectWithSoftAp(String ssid, String password, boolean isForceConnection) {

        this.isForceConnectionByCredential = isForceConnection;

        if (isForceConnection) {
            mWiFiClient.disConnect("connectWithSoftAp()");
        }

        stopSearching();

        // stop GO and broadcasting
        if (mSoftAccessPoint == null) {
            mSoftAccessPoint = new SoftAccessPoint(mContext, mSoftAPStateListener, mBroadCastInfo);
            mSoftAccessPoint.setMeshXLogListener(mMeshXLogListener);
        }
        mSoftAccessPoint.stop();
        if (mSoftAccessPointSearcher != null) {
            mSoftAccessPointSearcher.stop();
        }
        mWiFiMeshConfig.mIsClient = true;
        mWiFiMeshConfig.mIsGroupOwner = false;

        MeshLog.v("[p2p_process] remove Go, Lc schedule task connectWithSoftAp");
        AndroidUtil.removeBackground(mRestartScheduledTask);


        HandlerUtil.postBackground(() -> {
            mConnectingSSID = ssid;
            mWiFiClient.softApConnection(new APCredential(ssid, password, System.currentTimeMillis()));
        }, 1000);


    }

    private Runnable mStartSearchingWithGO = () -> startSearching(false);
    private Runnable mStartGO = this::startGO;

    /**
     * It considers client connection state of GO part. Make sure we are not re initiating GO
     * while it has any valid client connected
     */
    public void reAttemptServiceDiscovery() {
        if (mMeshXLogListener != null) {
            mMeshXLogListener.onLog("[reAttemptServiceDiscovery]");
        }
        MeshLog.v("[Meshx][reAttemptServiceDiscovery]::" + mWiFiMeshConfig);


        if (mWiFiStateMonitor != null) {

            mWiFiStateMonitor.destroy();
            mWiFiStateMonitor = null;
        }
        AndroidUtil.postDelay(this::start, SOFT_DELAY_TO_START_P2P_SERVICES);
    }

    public synchronized static WiFiDirectManagerLegacy getInstance(Context context,
                                                                   MeshXAPListener meshXAPListener,
                                                                   MeshXLCListener meshXLCListener,
                                                                   WiFiMeshConfig wiFiMeshConfig,
                                                                   String broadcastInfo) {
        MeshLog.v("WiFiDirectManagerLegacy called");
        if (sWiFiDirectManagerLegacy == null) {
            synchronized (WiFiDirectManagerLegacy.class) {
                if (sWiFiDirectManagerLegacy == null) {
                    MeshLog.v("WiFiDirectManagerLegacy setting new configs");
                    sWiFiDirectManagerLegacy = new WiFiDirectManagerLegacy(context, meshXAPListener,
                            meshXLCListener, wiFiMeshConfig, broadcastInfo);
                }
            }
        }
        return sWiFiDirectManagerLegacy;
    }


    public <T> void initListener(T type) {
        if (type instanceof ManagerStateListener) {
            managerStateListener = (ManagerStateListener) type;
        } else {
            Log.e("Go_created", "not match");
        }
    }

    /**
     * You must ensure to call {@link #getInstance(Context, MeshXAPListener, MeshXLCListener, WiFiMeshConfig, String)} before this method. Otherwise it will
     * return null
     *
     * @return
     */
    public static WiFiDirectManagerLegacy getInstance() {
        return sWiFiDirectManagerLegacy;
    }

    private WiFiDirectManagerLegacy(Context context, MeshXAPListener meshXAPListener,
                                    MeshXLCListener meshXLCListener, WiFiMeshConfig wiFiMeshConfig,
                                    String broadcastInfo) {

        mWiFiMeshConfig = wiFiMeshConfig;
        mContext = context;
        //mWiFiConnectionHelper = new WiFiConnectionHelper(mContext);
        mMeshXAPListener = meshXAPListener;
        this.mMeshXLCListener = meshXLCListener;
//        softApConnector = new SoftApConnector(context, mConnectionListener);
        mBroadCastInfo = broadcastInfo;
        mWiFiClient = new WiFiClient(mContext);
    }

    public void start() {
        MeshLog.v("[p2p_process] mConnectingSSID set null from  start()");
        mConnectingSSID = null;
        //make sure the instance is alive
        if (sWiFiDirectManagerLegacy != null && !mHasForcedOff) {

            // TODO: 11/20/2019 We should only clear service rather restarting WiFi client
            /*HardwareStateManager hardwareStateManager = new HardwareStateManager();
            hardwareStateManager.init(mContext);
            mIsRequestedForceOff = true;
            MeshLog.v("[WIFI]Resetting WiFi off");
            mIsRequestedForceOff = true;
            hardwareStateManager.resetWifi(isEnable -> {
                if (isEnable)*/
            {

                if (mWiFiStateMonitor == null) {
                    mWiFiStateMonitor = new WiFiStateMonitor(mContext, isEnabled -> {
                        MeshLog.w("p2p_process mWiFiStateMonitor-isEnabled:" + isEnabled + "-");
                        if (!isEnabled) {//Somehow WiFi turned off
//                                mWiFiStateMonitor.destroy();//Would start receive upon reenable
//                                mWiFiStateMonitor = null;
//                                start(mContext);//reenabling WiFi

                            boolean isWiFiEnabled = WiFiUtil.isWiFiOn(mContext);
                            MeshLog.v("p2p_process turned off Broadcast received. isWiFiEnabled:"
                                    + isWiFiEnabled);
                            if (!isWiFiEnabled) {
                                mHasForcedOff = true;
                                //disabling wifi related portions
                                if (mWiFiAdapterStateListener != null) {
                                    mWiFiAdapterStateListener.onStateChanged(isEnabled);
                                }
                                stop();
                            }
                        }
                    });
                    mWiFiStateMonitor.init();
                }

//                    mWiFiConnectionHelper.disconnect();
                // FIXME: 11/19/2019 for testing purpose only
//                    mWiFiConnectionHelper.disableAllConfiguredWiFiNetworks();

                int task = -1;
                if (mWiFiMeshConfig.mIsClient && mWiFiMeshConfig.mIsGroupOwner) {
                    task = START_TASK_ALL;

                } else if (mWiFiMeshConfig.mIsGroupOwner) {
                    task = START_TASK_ONLY_AP;

                } else if (mWiFiMeshConfig.mIsClient) {
                    task = START_TASK_ONLY_SEARCH;
                }

                start(task);
            }
//            });
        }
    }

    public void start(int startTask) {

        MeshLog.v("[DC-Issue] task starting with " + startTask);

        //Irrespective of GO or LC we monitor WiFi connectivity status
        if (mWiFiClient == null) {
            mWiFiClient = new WiFiClient(mContext);
        }

        mWiFiClient.disConnect("start(int startTask)");
        //Always set the listener
        mWiFiClient.setConnectionListener(mConnectionListener);
        mWiFiClient.setMeshXLogListener(mMeshXLogListener);
        if (startTask == START_TASK_ONLY_AP || startTask == START_TASK_ALL) {

            if (mWiFiMeshConfig.mIsGroupOwner) {

                startGO();
            }

            if (startTask == START_TASK_ONLY_AP) {
                if (mSoftAccessPointSearcher == null) {
                    mSoftAccessPointSearcher = new SoftAccessPointSearcher(mContext);
                    mSoftAccessPointSearcher.setServiceFound(mServiceFound);
                }

                mSoftAccessPointSearcher.stop();
            }
        }

        if (startTask == START_TASK_ONLY_SEARCH || startTask == START_TASK_ALL) {


            if (startTask == START_TASK_ALL) {

                //For first time searching we randomly wait for 5 to 10 seconds. As several devices
                //can start service at uncertain time
                int delay = (new Random().nextInt(INITIAL_AP_SEARCH_DELAY_MAX -
                        INITIAL_AP_SEARCH_DELAY_MIN + 1) + INITIAL_AP_SEARCH_DELAY_MIN) * 1000;

                AndroidUtil.postBackground(mStartSearchingWithGO, delay);
            } else {

                startSearching(true);
            }

            if (startTask == START_TASK_ONLY_SEARCH) {
                if (mSoftAccessPoint == null) {
                    mSoftAccessPoint = new SoftAccessPoint(mContext, mSoftAPStateListener, mBroadCastInfo);
                }

                mSoftAccessPoint.stop();
            }
        }
    }

    public boolean startScheduledBroadcastSearch(String from) {
        boolean isStarted = false;

        if (mWiFiMeshConfig != null) {


            //Initial swap of role as scheduled method always toggle roles
            mWiFiMeshConfig.mIsClient = !mWiFiMeshConfig.mIsClient;
            mWiFiMeshConfig.mIsGroupOwner = !mWiFiMeshConfig.mIsGroupOwner;

            MeshLog.v("[p2p_process] startScheduledBroadcastSearch: " + from);
            restartScheduledTask();

            isStarted = true;
        }

        return isStarted;
    }

    public void restartCurrentRole() {

        mWiFiMeshConfig.mIsClient = !mWiFiMeshConfig.mIsClient;
        mWiFiMeshConfig.mIsGroupOwner = !mWiFiMeshConfig.mIsGroupOwner;

        restartScheduledTask();
    }

    public void restartScheduledTask() {
        int randValue = (new Random().nextInt((SEARCHING_SCHEDULED_MAX + 1) -
                SEARCHING_SCHEDULED_MIN) + SEARCHING_SCHEDULED_MIN) * 1000;
        AndroidUtil.postBackground(mRestartScheduledTask, randValue);

        if (mSoftAccessPointSearcher == null) {
            mSoftAccessPointSearcher = new SoftAccessPointSearcher(mContext);
            mSoftAccessPointSearcher.setServiceFound(mServiceFound);
        }
        mSoftAccessPointSearcher.stop();

        if (mSoftAccessPoint == null) {
            mSoftAccessPoint = new SoftAccessPoint(mContext, mSoftAPStateListener, mBroadCastInfo);
            mSoftAccessPoint.setMeshXLogListener(mMeshXLogListener);
        }
        mSoftAccessPoint.stop();

        if (mWiFiMeshConfig == null) {

            MeshLog.v("[p2p_process]Scheduled for next iteration but meshConfig is null");
        } else {
            mWiFiMeshConfig.mIsClient = !mWiFiMeshConfig.mIsClient;
            mWiFiMeshConfig.mIsGroupOwner = !mWiFiMeshConfig.mIsGroupOwner;

            MeshLog.v("[p2p_process] Starting with:" + mWiFiMeshConfig + " for " + (randValue / 1000) + "seconds");
            start();
        }
    }

    // TODO: 10/5/2020 To gain more fine grain control this cancellation should have concurrency

    /**
     * If currently WiFi running on scheduled role then calling this API turn rolling postponed for
     * {@link DriverManager#BLE_BASED_WIFI_CONNECTIVITY_TIMEOUT} seconds
     */
    public void postPoneScheduledRole() {
        AndroidUtil.postBackground(mRestartScheduledTask,
                DriverManager.BLE_BASED_WIFI_CONNECTIVITY_TIMEOUT * 1000);
    }

    private void startGO() {
        AndroidUtil.removeBackground(mStartGO);

        if (mSoftAccessPointSearcher != null && mSoftAccessPointSearcher.mIsPreparingSearching) {
            MeshLog.w("GO starting buffered for " + (SEARCHER_BUSY_GO_BUFFERED_TIME / 1000) +
                    "seconds in a scheduled delay!!!");
            AndroidUtil.postBackground(mStartGO, SEARCHER_BUSY_GO_BUFFERED_TIME);
        } else {

            if (mWiFiMeshConfig.mIsGroupOwner) {
                if (mSoftAccessPoint == null) {
                    mSoftAccessPoint = new SoftAccessPoint(mContext, mSoftAPStateListener, mBroadCastInfo);
                    mSoftAccessPoint.setMeshXLogListener(mMeshXLogListener);
                }
                mSoftAccessPoint.restart();
            }
        }
    }

    private void startSearching(boolean isOnlySearching) {
        AndroidUtil.removeBackground(mStartSearchingWithGO);

        if (isOnlySearching) {
            requestGOBroadcastOff();
        }

        if (mSoftAccessPointSearcher == null) {
            mSoftAccessPointSearcher = new SoftAccessPointSearcher(mContext);
            mSoftAccessPointSearcher.setServiceFound(mServiceFound);

            if (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                mSoftAccessPointSearcher.addBlockedList(mSoftAccessPoint.mNetworkName);
            }
        }

        mSoftAccessPointSearcher.addSearchQueue(mWiFiMeshConfig.mSearchList);
        if (mSoftAccessPointSearcher.mIsAlive) {
            MeshLog.i("Searcher alive just added the filter:" + mSoftAccessPointSearcher.getSearchingQueue());
        } else {
            //If it is alive then searcher will carry it's own cycle
            mSoftAccessPointSearcher.startSearching();
        }

        if (mWiFiClient == null) {
            //So that upon getting AP, connectivity is easily accessible
            mWiFiClient = new WiFiClient(mContext);
        }
    }

    public boolean isMeMasterAlive() {
        return (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive());
    }

    /**
     * Return true iff trying to connect with a SSID
     *
     * @return
     */
    public boolean isConnecting() {
        return Text.isNotEmpty(mConnectingSSID);
    }

    public void destroy() {
        MeshLog.i("WiFiDirectManagerLegacy Destroy ");
        stop();
        sWiFiDirectManagerLegacy = null;

    }

    private void stop() {

        if (mWiFiMeshConfig != null && CollectionUtil.hasItem(mWiFiMeshConfig.mSearchList)) {
            mWiFiMeshConfig.mSearchList.clear();
        }

        MeshLog.v("[p2p_process] mConnectingSSID set null from  stop()");
        mConnectingSSID = null;
        mIsConnectingWithSearchedSubnet = false;

        AndroidUtil.removeBackground(mStartSearchingWithGO);
        AndroidUtil.removeBackground(mStartGO);
        MeshLog.v("[p2p_process] remove Go, Lc schedule task stop");
        AndroidUtil.removeBackground(mRestartScheduledTask);

        //FIXME: context null issue
        // mContext = null;

        //todo
        if (mMeshXAPListener != null && mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
            mMeshXAPListener.onSoftAPStateChanged(false, null);
        }

        if (mMeshXLCListener != null && sWiFiDirectManagerLegacy != null) {
            if (mWiFiClient != null && mWiFiClient.isConnected()) {
               // mMeshXLCListener.onDisconnectWithGO(mNetworkName);
            }
        }

        if (mSoftAccessPoint != null) {
            mSoftAccessPoint.stop();
        }

        if (mSoftAccessPointSearcher != null) {
            mSoftAccessPointSearcher.stop();
        }

        if (mWiFiClient != null) {
            mWiFiClient.destroy();
        }

        if (mWiFiStateMonitor != null) {
            mWiFiStateMonitor.destroy();
        }

        mWiFiAdapterStateListener = null;

        //stopNsd();
    }

    /*private void stopNsd() {

        NSDHelper nsdHelper = NSDHelper.getInstance(mContext);
        if (nsdHelper != null) {
            nsdHelper.stopDiscovery();
            nsdHelper.tearDown();
        }
    }*/

    public void setMeshXLogListener(MeshXLogListener meshXLogListener) {
        mMeshXLogListener = meshXLogListener;
    }

    /**
     * Currently we deal with single interface either GO or LC. So this method
     *
     * @return only active network name or null whether GO or LC
     */
    public String getCurrentNetworkName() {

        return mNetworkName;
    }

    public boolean pauseConnectivity() {
        MeshLog.w("WiFi SoftDisable pauseConnectivity:" + mIsConnectivityPause);
        boolean isPaused = false;
        if (!mIsConnectivityPause) {
            mIsConnectivityPause = true;
            if (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                //First pause from socket
                MeshHttpServer.on().pauseDirectDiscovery();
                isPaused = true;
            }

            if (mSoftAccessPointSearcher != null && mSoftAccessPointSearcher.mIsAlive) {
                mSoftAccessPointSearcher.pauseConnectivity();
                MeshLog.v("[p2p_process] mConnectingSSID set null from  pauseConnectivity()");
                mConnectingSSID = null;
                MeshHttpServer.on().pauseDirectDiscovery();

                isPaused = true;
            }
        }

        return isPaused;
    }

    public boolean resumeConnectivity() {
        MeshLog.w("SoftDisable resumeConnectivity:" + mIsConnectivityPause);
        boolean isResumed = false;

        if (mIsConnectivityPause) {
            if (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                //Resume socket
                MeshHttpServer.on().resumeDirectDiscovery();
                isResumed = true;
            }

            if (mSoftAccessPointSearcher != null && mSoftAccessPointSearcher.mIsAlive) {
                mSoftAccessPointSearcher.resumeConnectivity();
                MeshHttpServer.on().resumeDirectDiscovery();
                isResumed = true;
            }
        }

        return isResumed;
    }

    /**
     * Request GO to broadcast service in a scheduled fashion
     */
    public void requestGOScheduledBroadCast() {
        if (mSoftAccessPoint != null) {
            mSoftAccessPoint.requestScheduledServiceBroadcast();
        }
    }

    public void requestGOBroadcastOff() {
        if (mSoftAccessPoint != null) {
            mSoftAccessPoint.shutDownServiceBroadcasting();
        }
    }

    public void setWiFiAdapterStateListener(WiFiStateMonitor.WiFiAdapterStateListener
                                                    wiFiAdapterStateListener) {
        mWiFiAdapterStateListener = wiFiAdapterStateListener;
    }

    /**
     * Attempt to connect with an AP if not connected with any GO or Adhoc
     *
     * @param ssid
     * @param password
     * @return whether connection attempt possible or not
     */
    public boolean connectWithAP(String ssid, String password) {
        if (mWiFiMeshConfig.mIsClient || WiFiUtil.isWifiConnected(mContext)) {
            return false;
        }
        if (mWiFiClient == null) {
            mWiFiClient = new WiFiClient(mContext);
        }
        return mWiFiClient.connect(ssid, password);
    }

    public void setHighBandMode() {
        if (mWiFiClient != null) {
            mWiFiClient.setHighBand();
        }
    }

    public void releaseHighBandMode() {
        if (mWiFiClient != null) {
            mWiFiClient.releaseHighBand();
        }
    }

    public boolean stopSearching() {
        boolean isStopped = false;
        MeshLog.v("[p2p_process] remove Go, Lc schedule task stopSearching");
        AndroidUtil.removeBackground(mRestartScheduledTask);
        if (mSoftAccessPointSearcher != null) {

            mSoftAccessPointSearcher.stop();
            isStopped = true;
        }
        return isStopped;
    }

    public void search(String searchFor) {

        mIsSearchingNode = true;

        MeshLog.v("[Search]Search started for:" + searchFor);

        if (mWiFiMeshConfig.mSearchList == null) {
            mWiFiMeshConfig.mSearchList = new ArrayList<>(3);
        }
        mWiFiMeshConfig.mSearchList.add(searchFor);
        startSearching(true);
    }

    public boolean isConnectingWithSearchedSubnet() {
        return mIsConnectingWithSearchedSubnet;
    }

    public interface WiFiStateListener {
        void onServiceFound(String ssid, String broadCastInfo);

        void onTimeout(String ssid);

        void onConnected(String ssid);

        void onDisConnected(String ssid);

        void connectionAttemptFinish();
    }
}
