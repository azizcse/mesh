package com.w3engineers.mesh.controller;

import android.content.Context;

import com.w3engineers.mesh.ble.BLEDataListener;
import com.w3engineers.mesh.ble.BleManager;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.discovery.MeshXAPListener;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLCListener;
import com.w3engineers.mesh.libmeshx.wifi.WiFiStateMonitor;
import com.w3engineers.mesh.libmeshx.wifid.WiFiDirectManagerLegacy;
import com.w3engineers.mesh.libmeshx.wifid.WiFiMeshConfig;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifidirect.CurrentRole;
import com.w3engineers.mesh.wifidirect.WiFiDirectController;

import java.util.List;
import java.util.Random;

public class DriverManager implements BLEDataListener {

    /**
     * Scheduler minimum times as we expect fast connection because of the hints from BLE. If can
     * not, then we discard the attempt.
     */
    public static final int BLE_BASED_WIFI_CONNECTIVITY_TIMEOUT =
            (int) (WiFiDirectManagerLegacy.SEARCHING_SCHEDULED_MIN * 2.25 / 3);
    private static final String PREFIX_TAG = "[BLE_PROCESS]";
    private static DriverManager mDriverManager;
    private Context mContext;
    /**
     * Is true when searching started. Is false when search accomplished or timed out
     */
    private volatile boolean mIsSearching;

    private BleManager mBleManager;
    private volatile WiFiDirectManagerLegacy mWiFiDirectManagerLegacy;

    private MeshXAPListener apListener;
    private MeshXLCListener clientListener;

    private String myUserId;
    private final int BLE_RANDOM_MIN = 3;
    private final int BLE_RANDOM_MAX = 5;
    private boolean isWifiConnecting;
    private WiFiDirectController wiFiDirectController;

    /**
     * Represent planned disconnection of WiFi
     */
    // FIXME: 10/16/2020
    public volatile boolean mIsDisconnecting;

    /*private WiFiDirectManagerLegacy.WiFiStateListener mWiFiStateListener = new
            WiFiDirectManagerLegacy.WiFiStateListener() {
                @Override
                public void onServiceFound(String ssid, String broadCastInfo) {
                    //As service found so temporarily turned off BLE scan
                    //If connected GATT server will be opened
                    MeshLog.v("[lazy] BLE stop from mWiFiStateListener");
                    if (RouteManager.getInstance().getBleUsers().size() <= 0) {
                        MeshLog.i("[BLE_PROCESS] BLE connection not exist when service found");
                        stopBLE();
                    } else {
                        MeshLog.i("[BLE_PROCESS] BLE connection exist when service found");
                    }
                }

                @Override
                public void onTimeout(String ssid) {
                    //Temporarily paused for now
//                    startBLE(false, "onTimeout");
                }

                @Override
                public void onConnected(String ssid) {
                }

                @Override
                public void onDisConnected(String ssid) {
                    //Temporarily paused for now
//                    startBLE(false, "onDisConnected");
                }

                @Override
                public void connectionAttemptFinish() {
                    setWifiConnecting(false);
                }
            };*/


    public static DriverManager getInstance(Context context, String userId) {
        if (mDriverManager == null) {
            synchronized (DriverManager.class) {
                mDriverManager = new DriverManager(context, userId);
            }
        }
        return mDriverManager;
    }

    private DriverManager(Context context, String userId) {
        this.mContext = context;
        this.myUserId = userId;


        // Now initiated ble manager
        mBleManager = BleManager.getInstance(context, myUserId, this);
        wiFiDirectController = WiFiDirectController.on(context, myUserId, null);

        mBleManager.initializeObject(wiFiDirectController);
        wiFiDirectController.initializeObject(mBleManager);

        //Now initiated WifiDirectManagerLegacy
       /* mWiFiDirectManagerLegacy = WiFiDirectManagerLegacy.getInstance(mContext, mMeshXAPListener,
                mMeshXLCListener, new WiFiMeshConfig(), String.valueOf(AddressUtil.getHash(myUserId)));
        Log.e("Go_created", "Init manager listener");
        mBleManager.initListener(managerStateListener);
        mWiFiDirectManagerLegacy.initListener(managerStateListener);*/
    }

    public void initWifiDirectManagerListener(MeshXAPListener apListener, MeshXLCListener clientListener) {
        this.apListener = apListener;
        this.clientListener = clientListener;
        wiFiDirectController.initializeObject(clientListener);
    }

    public void startSpecialSearch(String receiverId) {
        wiFiDirectController.startSpecialSearch(receiverId);
    }

    public void startP2pTask(int taskType) {
        wiFiDirectController.startTaskImmediately("startP2pTask", taskType);
    }


    public boolean isWifiConnecting() {
        return isWifiConnecting;
    }

    public void setWifiConnecting(boolean wifiConnecting) {
        isWifiConnecting = wifiConnecting;
    }

    public void disconnectWifi() {
        wiFiDirectController.disconnectWifi("DriverManager");
    }

    /*public WiFiDirectManagerLegacy getWifiLegacy() {
        return mWiFiDirectManagerLegacy;
    }*/

    /**
     * Start all indicate start GO, GATT server, and their both search
     */
    /*public void startAll(boolean isForcefulConnection) {
        startWifiLegacy();
        startBLE(isForcefulConnection, "startAll");
    }*/
    public void removeBLEStartRunnable() {
        HandlerUtil.removeBackground(bleStartRunnable);
    }

    /**
     * Stop all GO,GATT server, and their client
     */
    /*public void stopAll() {
        wiFiDirectController.stopAllP2pProcess();
        stopBLE();
    }*/
    public void startSearching(WiFiStateMonitor.WiFiAdapterStateListener mWiFiAdapterStateListener) {

        boolean isWiFiEnabled = WiFiUtil.isWiFiOn(mContext);
        if (!isWiFiEnabled) {
            mWiFiAdapterStateListener.onStateChanged(isWiFiEnabled);
        }
        boolean isGO = new Random().nextBoolean();
        MeshLog.e("Started go lc scheduler roll go: " + isGO);

        wiFiDirectController.startTaskImmediately("startSearching", isGO ? CurrentRole.GO : CurrentRole.LC);
        int randValue = new Random().nextInt((BLE_RANDOM_MAX - BLE_RANDOM_MIN) + 1) +
                BLE_RANDOM_MIN;
        HandlerUtil.postBackground(bleStartRunnable, randValue * 1000);

    }

    private Runnable bleStartRunnable = () -> startBLE(false, "startSearching");


    /**
     * Start BLE GATT server,advertising and scan together
     */
    public void startBLE(boolean isForcefulConnection, String from) {
        //Check user is i internet mode.
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            MeshLog.v("[lazy]Starting BLE  " + from);
            MeshLog.v(PREFIX_TAG + " Starting BLE  " + from);
            mBleManager.initAllProcess(isForcefulConnection);
        }
    }


    public void requestGOScheduledBroadCast() {
        wiFiDirectController.startGoScheduleBroadcast();
    }

    /**
     * This process is mainly used for sub network merge. Received other network credential
     * and connect
     *
     * @param ssid         Other network SSID
     * @param preSharedKey Other network password
     * @param goNodeId     The other network GO ID
     */
    public void connectWithReceivedSoftAP(String ssid, String preSharedKey, String goNodeId) {
        stopBLE();
        List<RoutingEntity> offlineList = RouteManager.getInstance().resetDbForWifiNode();
        if (apListener != null) {
            apListener.onGetDisconnectedList(offlineList);
        }

        // We stopping BLE now (Not fully stopped)
        // And wait for connection of wifi.
        // BLE scan stop because of avoiding mislead
        mBleManager.startOffWifiConnectionChecker(true);
        wiFiDirectController.attemptToConnectWithCredential(ssid, preSharedKey, goNodeId);
        /*setWifiConnecting(true);
        mWiFiDirectManagerLegacy.connectWithSoftAp(ssid, preSharedKey, true);*/

        //specificGoSearch(ssid);
    }


    /**
     * For connection desired node during file share.
     *
     * @param targetNode Desired node id
     * @param ssId       Desire GO SSID
     * @param password   Desire GO password
     */
    public void makeSpecialSoftApConnection(String targetNode, String ssId, String password) {
        stopBLE();
        RouteManager.getInstance().resetDbForWifiNode();
        mBleManager.startOffWifiConnectionChecker(true);
        wiFiDirectController.makeSpecialSoftApConnection(targetNode, ssId, password);
    }


    public void startGoForForceConnection() {
        wiFiDirectController.startSpecialGO();
    }

    public boolean mHasForcedOff() {
        if (mWiFiDirectManagerLegacy == null) return false;
        return mWiFiDirectManagerLegacy.mHasForcedOff;
    }

    public boolean isConnecting() {

        if (mWiFiDirectManagerLegacy == null) return false;
        return mWiFiDirectManagerLegacy.isConnecting();
    }


    /**
     * Some BLE own method
     * Stop GATT server and scan
     */
    public void stopBLE() {
        MeshLog.v("[lazy]Stopping BLE");
        MeshLog.v("[BLE_PROCESS] Stopping BLE");
        removeBLEStartRunnable();
        stopBLEScan();

        boolean hasBleUser = RouteManager.getInstance().getBleUsers().size() > 0;

        if (!hasBleUser) {
            MeshLog.v(PREFIX_TAG + " We stopping server and we have no ble user left");
            stopBleServer();
        }
    }

    public void restartAdvertise() {
        mBleManager.restartAdvertise();
    }

    public void stopBleAdvertise() {
        mBleManager.stopAdvertise();
    }

    public void stopBleServer() {
        mBleManager.stopServer();
    }

    public void startBleServer() {
        mBleManager.startServer();
    }

    public void stopBLEScan() {
        mBleManager.stopScan();
    }

    public void shutDownBle() {
        stopBLEScan();
        mBleManager.disconnect();
        stopBleServer();
    }

    public void startSpecificBLESearch(String... searchIds) {
        stopBleAdvertise();
        setScanFilter(searchIds);
        startBleScan(true);
    }

    public void startBleScan(boolean isForcefulConnection) {
        mBleManager.startScan(isForcefulConnection);
    }

    public void setBleCredential(String ssid, String password) {
        mBleManager.setCredential(ssid, password);
    }

    public void setScanFilter(String... userIds) {
        mBleManager.setScanFilter(userIds);
    }

    public void removeBleConnectionById(String userId) {
        if (mBleManager != null) {
            mBleManager.disconnectNode(userId);
        }
    }

    public void stopAllP2pProcess() {
        wiFiDirectController.stopAllP2pProcess();
    }

    /*
     * BLE Data listener
     * */

    /**
     * When cget credential from BLE-BLE for subnet merge the connection process start here
     * And transfer this credential to child nodes too.
     *
     * @param ssid     Other network SSID
     * @param password Other network Password
     * @param userId   Other network GO's user id
     */
    @Override
    public void onGetCredential(String ssid, String password, String userId) {
        // Connect to this wifi.

        MeshLog.v(PREFIX_TAG + " Received credential: ssid: " + ssid + " Password: " + password);
        //stopBLE();
        MeshLog.v("[p2p_process] ble Go credential received .... ssid :" + ssid + " pass: " + password);

        //Pass this credential to other wifi users
        //And we need to manage dynamic delay
        int delay = RouteManager.getInstance().getWifiUser().size();
        if (delay == 0) {
            delay = 1;
        }
        //The dynamic delay will prevent unnecessary disconnection

        if (apListener != null) {
            apListener.onReceiveCredentialFromBle(ssid, password, userId);
        }
        HandlerUtil.postBackground(() -> {
            if (RouteManager.getInstance().getBleUsers().size() <= 0) {
                MeshLog.i("[BLE_PROCESS] BLE connection not exist when  new wifi connection create");
                stopBLE();
                mBleManager.startOffWifiConnectionChecker(true);
            } else {
                MeshLog.i("[BLE_PROCESS] BLE connection exist when service found");
            }

            List<RoutingEntity> offlineNodeList = RouteManager.getInstance().resetDbForWifiNode();

            if (apListener != null) {
                apListener.onGetDisconnectedList(offlineNodeList);
            }

            setWifiConnecting(true);
            wiFiDirectController.attemptToConnectWithCredential(ssid, password, userId);
        }, delay * 2000L);

    }

    @Override
    public void onGetServerServerStartStatus(boolean isStarted) {
        //Todo from here we can send ssid and password by calling #setCredential(ssid,password)
    }

    @Override
    public void onStatSpecialSearch(String userId) {
        startSpecialSearch(userId);
    }

    @Override
    public void onGetMyMode(boolean isServer, boolean isForceConnection) {

        MeshLog.v(PREFIX_TAG + " My BLE mode: " + isServer + " isForceConnection: " + isForceConnection);

        if (isServer) {
            // If it is a server then we have to take some counter measurement.
            int wifiUserCount = RouteManager.getInstance().getWifiUser().size();

            MeshLog.v(PREFIX_TAG + " exists wifi user count: " + wifiUserCount + " limit: " + WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER);

            if (isForceConnection) {

                // TODO We restart the server here and optimize later

                /*if (wifiUserCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                    // Todo it is matter of distance if GO is far way. We will do it later

                    if (wifiUserCount == 0) {
                        makeDecisionForGoLc(true);
                    } else {
                       *//* if (mWiFiDirectManagerLegacy != null) {
                            mBleManager.sendSoftApCredential(mWiFiDirectManagerLegacy.getSoftApCredential());
                        }*//*

                        // Starting BLE server again
                        startBleServer();
                    }
                } else {
                    // It is a full connection
                    MeshLog.i(PREFIX_TAG + "Is legacy manager null::" + mWiFiDirectManagerLegacy);
                    stopWifiLegacy();

                    mIsDisconnecting = true;

                    makeDecisionForGoLc(true);

                    // Todo Turn on GO and send credential. Disconnect every node from here

                }*/

                // before starting special GO we must remove existing connection.
                // Before re creating GO we can check any node available not exists or not
                // If available we have to track in transport layer. Otherwise create GO

                MeshLog.v(PREFIX_TAG + " force connection found");

                stopBLE();
                startGoForForceConnection();

            } else {
                // So it is not a force connection
                // Check my mode
                if (wifiUserCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                    if (wifiUserCount == 0) {
                        //No node connected.
                        makeDecisionForGoLc(true);
                    } else {
                      /*  if (mWiFiDirectManagerLegacy != null) {
                            mBleManager.sendSoftApCredential(mWiFiDirectManagerLegacy.getSoftApCredential());
                        }*/

                        // Starting BLE server again
                        startBleServer();
                    }

                } else {
                    // GO is full and it is normal connection so don't need to send credential
                    // Starting BLE server again
                    startBleServer();
                }
            }
        } else {
            if (!isForceConnection) {
                makeDecisionForGoLc(false);
            } else {
                //Todo start special LC search
            }
        }


    }

    private void makeDecisionForGoLc(boolean isServer) {
        MeshLog.e("[p2p_process] ble decision message isServer :" +
                isServer + " current role: " + wiFiDirectController.getCurrentRole());
        if (wiFiDirectController.getCurrentRole() != CurrentRole.WIFI_CONNECTING
                && wiFiDirectController.getCurrentRole() != CurrentRole.SPECIAL_LC
                && wiFiDirectController.getCurrentRole() != CurrentRole.SPECIAL_GO) {
            wiFiDirectController.startTaskImmediately("makeDecisionForGoLc", isServer ? CurrentRole.GO : CurrentRole.LC);
        } else {
            MeshLog.i("[p2p_process] makeDecisionForGoLc() called but existing role not support");
        }
    }

    private void startSpecialGO() {
        wiFiDirectController.startTaskImmediately("startSpecialGO", CurrentRole.GO);
    }

    /**
     * Search this {@code ethereumAddress} using P2P search and BLE search
     *
     * @param ethereumAddress
     * @param isWiFiConnectionRequired
     */
    public void search(String ethereumAddress, boolean isWiFiConnectionRequired) {

        if (AddressUtil.isValidEthAddress(ethereumAddress)) {

            mIsSearching = true;

            if (mWiFiDirectManagerLegacy != null) {
                mWiFiDirectManagerLegacy.search(String.valueOf(AddressUtil.getHash(ethereumAddress)));

                //If no WiFi user connected then pause current role, only continue the search
                if (!RouteManager.getInstance().isWifiUserConnected()) {

                    mWiFiDirectManagerLegacy.postPoneScheduledRole();
                }
            }

            startSpecialSearch(ethereumAddress);

            //First remove runnable
           /* stopBLE();
            setScanFilter(ethereumAddress);*/

            stopBLE();
            startSpecificBLESearch(ethereumAddress);

        }
    }

    public boolean isConnectingWithSearchedSubnet() {
        return mWiFiDirectManagerLegacy != null && mWiFiDirectManagerLegacy.isConnectingWithSearchedSubnet();
    }

    public void stopGoBroadcast() {
        MeshLog.e("[p2p_process] max lc connected stop advertise");
        wiFiDirectController.stopGoAdvertise();
    }

    public void setHighBandMode() {
        if (wiFiDirectController != null) {
            wiFiDirectController.setHighBandMode();
        }
    }

    public void releaseHighBandMode() {
        if (wiFiDirectController != null) {
            wiFiDirectController.releaseHighBandMode();
        }
    }

    public void triggerWifiDisconnectionAndSearch(String fullNetworkGoSsid) {

        wiFiDirectController.disconnectWifi("Go_network_full "+fullNetworkGoSsid);
        writeGoLcSchedulerState("triggerWifiDisconnectionAndSearch", CurrentRole.LC);
        //wiFiDirectController.startTaskImmediately("triggerWifiDisconnectionAndSearch", CurrentRole.LC);
    }

    /**
     * <h1>remove Go Lc scheduler </h1>
     */
    public void stopGoLcScheduler() {
        wiFiDirectController.stopGoLcScheduler();
    }

    public void writeGoLcSchedulerState(String from, int state) {
        wiFiDirectController.writeNextRoleForceFully(from, state);
    }
}

