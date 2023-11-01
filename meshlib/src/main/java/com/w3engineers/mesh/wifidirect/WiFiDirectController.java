package com.w3engineers.mesh.wifidirect;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.ble.BleManager;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLCListener;
import com.w3engineers.mesh.libmeshx.wifid.APCredential;
import com.w3engineers.mesh.libmeshx.wifid.WifiCredential;
import com.w3engineers.mesh.model.DisconnectionModel;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.MessageCallback;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifidirect.connector.WiFiConnector;
import com.w3engineers.mesh.wifidirect.listener.WiFiDirectStatusListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by Azizul Islam on 11/30/20.
 * <h1>
 * This class is responsible for WiFi-direct GO(Group owner) and LC(Legacy Client)
 * control. An autonomous scheduler running whole life cycle and control
 * this Go, LC periodically. Tracking node connectivity and take decision for GO, LC
 * search.
 * <p>
 * There are two types of searching general and special search.
 * </h1>
 */
public class WiFiDirectController {
    enum TestMode {SKIP, GO, LC}

    private final int SEARCHING_SCHEDULED_MIN = 40;
    private final int SEARCHING_SCHEDULED_MAX = SEARCHING_SCHEDULED_MIN + 15;
    private final int SPECIAL_GO_OR_LC = 130;
    private final int IDEAL_TIME = 90;
    private final int WIFI_CONNECTING = 10;

    private final Context mContext;
    private static WiFiDirectController wiFiDirectController;
    private final WiFiDirectService wiFiDirectService;
    private final WiFiDirectClient wiFiDirectClient;
    private APCredential myApCredential;
    private final WiFiConnector wiFiConnector;
    private final WifiTransPort wifiTransPort;
    private BleManager bleManager;
    private MeshXLCListener meshXLCListener;
    private static final Object object = new Object();

    private volatile boolean GO_SILENT_DISCONNECTION = false;

    private static String specialGoNodeId;
    private String myNodeId;


    private void setSpecialGoNodeId(String nodeId) {
        specialGoNodeId = nodeId;

        if (nodeId != null) {
            startSpecialGoSearchTracker();
        }
    }

    private String getSpecialGoNodeId() {
        return specialGoNodeId;
    }

    private WiFiDirectController(Context context, String myNodeId, WifiTransPort wifiTransPort) {
        this.mContext = context;
        this.myNodeId = myNodeId;
        this.wifiTransPort = wifiTransPort;
        wiFiDirectService = new WiFiDirectService(context, myNodeId, wiFiDirectStatusListener);
        wiFiDirectClient = new WiFiDirectClient(context, wiFiDirectStatusListener, myNodeId);
        //wifiStateReceiver = new WifiStateReceiver(context, dispatchQueue, wiFiDirectStatusListener);
        //wifiStateReceiver.register();
        wiFiConnector = new WiFiConnector(context, wiFiDirectStatusListener);
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            wiFiConnector.initialWifiDisconnection("Initialize");
        }
        //Set testmode
        setTestMode(TestMode.GO);
    }

    private void setTestMode(TestMode testMode) {
        SharedPref.write(Constant.KEY_TEST_MODE, testMode.ordinal());
    }

    private int getTestMode() {
        return SharedPref.readInt(Constant.KEY_TEST_MODE);
    }

    public static WiFiDirectController on(Context context, String myNodeId, WifiTransPort wifiTransPort) {

        WiFiDirectController controller = wiFiDirectController;
        if (controller == null) {
            synchronized (object) {
                controller = wiFiDirectController;
                if (controller == null) {
                    controller = wiFiDirectController = new WiFiDirectController(context, myNodeId, wifiTransPort);
                }
            }
        }
        return controller;
    }

    public static WiFiDirectController getInstance() {
        return wiFiDirectController;
    }

    public <T> void initializeObject(T type) {
        if (type instanceof BleManager) {
            bleManager = (BleManager) type;
        } else if (type instanceof MeshXLCListener) {
            meshXLCListener = (MeshXLCListener) type;
        }
    }

    public APCredential getMyApCredential() {
        return myApCredential;
    }

    public void disconnectWifi(String from) {
        wiFiConnector.disConnect(from);
    }

    public void createGo() {
        stopGoSearch();
        List<RoutingEntity> wifiUserList = RouteManager.getInstance().getWifiUser();

        if (WiFiUtil.isWifiConnected(mContext) && wifiUserList.isEmpty()) {
            wiFiConnector.disConnect("When attempt to create Go");
        }

        HandlerUtil.postBackground(() -> wiFiDirectService.registerService(), 2000);
    }

    public void searchGo() {
        stopGo();
        //Delay given here to perform above stop task successfully
        HandlerUtil.postBackground(() -> wiFiDirectClient.initializeServicesDiscovery(), 2000);

    }

    public void stopGo() {
        wiFiDirectService.stopGoAllEvent();
    }

    public void startSpecialSearch(String searchId) {
        MeshLog.v("[p2p_process] triggered special GO search Id: " + AddressUtil.makeShortAddress(searchId));
        setSpecialGoNodeId(searchId);
        wiFiDirectClient.startSpecialSearch(searchId);

        //Write Current role and Next role both as SPECIAL_LC
        //So that where actions are checked with current role

        writeCurrentRole(CurrentRole.SPECIAL_LC);
        startTaskImmediately("startSpecialSearch", CurrentRole.SPECIAL_LC);

        //wiFiConnector.disConnect("startSpecialSearch");
    }

    public void startSpecialGO() {
        startTaskImmediately("startSpecialGO", CurrentRole.SPECIAL_GO);
        wiFiConnector.disConnect("startSpecialGO");
    }


    public void stopGoSearch() {
        wiFiDirectClient.stopServiceDiscovery();
    }

    public void stopAllP2pProcess() {
        HandlerUtil.removeBackground(parentGoLcScheduler);
        stopGo();
        stopGoSearch();
        wiFiConnector.disConnect("stopAllP2pProcess");
    }

    public void startGoScheduleBroadcast() {
        MeshLog.v("[p2p_process] start Go schedule broadcast");
        wiFiDirectService.requestGOScheduledBroadCast();
    }

    public void startAll() {
        wiFiDirectService.registerService();
        wiFiDirectClient.initializeServicesDiscovery();
    }

    public void stopGoAdvertise() {
        MeshLog.v("[p2p_process] Stop Go schedule broadcast");
        wiFiDirectService.clearLocalServices();
    }


    public int getCurrentRole() {
        return SharedPref.readInt(Constant.KEY_RUNNING_ROLE);
    }

    public int getNextRole() {
        return SharedPref.readInt(Constant.KEY_NEXT_ROLE);
    }

    public void startTaskImmediately(String from, int task) {
        writeNextRoleForceFully(from, task);
        //HandlerUtil.postBackground(parentGoLcScheduler);
    }

    public void startTaskConditionally(String from, int task) {
        writeNextRole(from, task);
        HandlerUtil.postBackground(parentGoLcScheduler);
    }

    public void stopGoLcScheduler() {
        HandlerUtil.removeBackground(parentGoLcScheduler);
    }

    public void writeNextRoleForceFully(String from, int nextTask) {
        MeshLog.e("[p2p_process] Write next role called from: " + from + " role: " + getModeType(nextTask));
        SharedPref.write(Constant.KEY_NEXT_ROLE, nextTask);
        HandlerUtil.postBackground(parentGoLcScheduler);
    }

    public void writeNextRole(String from, int nextTask) {
        if (SharedPref.readInt(Constant.KEY_NEXT_ROLE) != CurrentRole.WIFI_CONNECTING) {
            SharedPref.write(Constant.KEY_NEXT_ROLE, nextTask);
            MeshLog.e("[p2p_process] Write next role called from: " + from + " role: " + getModeType(nextTask));
        } else {
            MeshLog.e("[p2p_process] Write next role failed due to wifi connecting");
        }
    }

    public void writeRoleAsConnecting() {
        if (getCurrentRole() != CurrentRole.WIFI_CONNECTING) {
            SharedPref.write(Constant.KEY_NEXT_ROLE, CurrentRole.WIFI_CONNECTING);
            SharedPref.write(Constant.KEY_RUNNING_ROLE, CurrentRole.WIFI_CONNECTING);
        } else {
            MeshLog.e("Wifi is in connecting state");
        }
    }

    private void writeCurrentRole(int role) {
        SharedPref.write(Constant.KEY_RUNNING_ROLE, role);
    }


    public void attemptToConnectWithCredential(String ssid, String password, String userId) {
        stopAllP2pProcess();
        //writeNextRole("attemptToConnectWithCredential", CurrentRole.WIFI_CONNECTING);
        writeRoleAsConnecting();

        wiFiConnector.disConnect("attemptToConnectWithCredential");
        wiFiConnector.softApConnection(new APCredential(userId, ssid, password, System.currentTimeMillis()));
    }

    public void makeSpecialSoftApConnection(String targetNode, String ssId, String password) {
        setSpecialGoNodeId(targetNode);

        stopAllP2pProcess();
        //writeNextRole("attemptToConnectWithCredential", CurrentRole.WIFI_CONNECTING);
        writeRoleAsConnecting();
        wiFiConnector.disConnect("attemptToConnectWithCredential");
        wiFiConnector.softApConnection(new APCredential(targetNode, ssId, password, System.currentTimeMillis()));
    }

    public void setHighBandMode() {
        if (wiFiConnector != null) {
            wiFiConnector.setHighBandMode();
        }
    }

    public void releaseHighBandMode() {
        if (wiFiConnector != null) {
            wiFiConnector.releaseHighBandMode();
        }
    }


    private void disconnectCycleNode(String nodeId) {
        if (TextUtils.isEmpty(nodeId)) return;

        RoutingEntity routingEntity = RouteManager.getInstance().getEntityByAddress(nodeId);
        if (routingEntity == null || !routingEntity.isOnline() || routingEntity.getHopAddress() == null)
            return;

        RoutingEntity hopEntity = RouteManager.getInstance().getEntityByAddress(routingEntity.getHopAddress());

        if (hopEntity == null) {
            MeshLog.v("Hop entity null after wifi connection for BLE");
            return;
        }
        if (hopEntity.getType() == RoutingEntity.Type.BLE) {
            meshXLCListener.onSendDisconnectMessageToBle(hopEntity.getAddress());
            //List<RoutingEntity> entityList = RouteManager.getInstance().updateNodeAsOffline("", hopEntity.getAddress());
            //MeshLog.v("Cycle before sending hello packet: offline: " + entityList.size());
            MeshLog.v("Rest db called due to intentional cycle");
            //RouteManager.getInstance().resetDb();
        }
    }

    private void attemptToConnect(APCredential credential) {
        stopAllP2pProcess();
        writeRoleAsConnecting();
        wiFiConnector.softApConnection(credential);
    }

    private final WiFiDirectStatusListener wiFiDirectStatusListener = new WiFiDirectStatusListener() {
        @Override
        public void onGoCreated(String ssid, String password) {
            myApCredential = new APCredential(ssid, password, System.currentTimeMillis());
            WifiCredential.ssid = ssid;
            WifiCredential.password = password;
        }

        @Override
        public void onGoFound(APCredential credential) {
            List<RoutingEntity> wifiUsers = RouteManager.getInstance().getWifiUser();
            if (credential.isSpecialSearch && !wifiUsers.isEmpty()) {
                prepareAndSendDisconnectionMessage(credential, wifiUsers);
            } else {
                attemptToConnect(credential);
            }
        }

        @Override
        public void onWifiConnect(WifiInfo wifiInfo) {
            String ssid = wifiInfo.getSSID();

            if (wifiInfo == null || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER) {
                if (P2PUtil.isPotentialGO(ssid)) {
                    wiFiConnector.disConnect("Internet only");
                }
                return;
            }


            MeshLog.v("[p2p_process] connected ssid: " + ssid);

            if (P2PUtil.isPotentialGO(ssid) && meshXLCListener != null && !P2PUtil.isMeGO()) {

                myApCredential = wiFiConnector.getConnectedGoCredential(ssid);

                String specialSearchId = getSpecialGoNodeId();

                if (myApCredential != null) {

                    //Disconnect if I,m connected with my self
                    if (myApCredential.ethId.equals(myNodeId)) {
                        wiFiConnector.disConnect("Connect on own Go");
                        return;
                    }

                    MeshLog.v("[p2p_process] WiFi connected with : " + AddressUtil.makeShortAddress(myApCredential.ethId) + " Special: " + specialSearchId);
                    if (myApCredential.ethId.equals(specialSearchId)) {
                        meshXLCListener.onConnectedWithTargetNode(specialSearchId);
                    } else {
                        if (specialSearchId != null) {
                            wiFiConnector.disConnect("special id not matched");
                        }
                    }

                    WifiCredential.ssid = myApCredential.mSSID;
                    WifiCredential.password = myApCredential.mPassPhrase;

                    //disconnectCycleNode(myApCredential.ethId);
                    meshXLCListener.onConnectWithGO(ssid, myApCredential.isSpecialSearch);

                } else {
                    wiFiConnector.disConnect("onWifiConnect -> auto connect");
                }
                MeshLog.i("[p2p_process] wifi connected start user discover");

            } else {
                MeshLog.i("[p2p_process] wifi connected but i,m GO ");
                if (wiFiConnector != null) {
                    if (P2PUtil.isMeGO() && !RouteManager.getInstance().getWifiUser().isEmpty()) {
                        MeshLog.v("Go silent disconnection");
                        GO_SILENT_DISCONNECTION = true;
                    }
                    wiFiConnector.disConnect("onWifiConnect");
                    writeNextRoleForceFully("FalseGO", CurrentRole.LC);
                }
            }
        }

        @Override
        public void onWifiDisconnect() {
            myApCredential = null;
            //If user is in internet only
            if (GO_SILENT_DISCONNECTION || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER) {
                GO_SILENT_DISCONNECTION = false;
                MeshLog.v("[p2p_process] silent disconnection");
                return;
            }


            // disconnect child node also if it is come form wifi.

            List<RoutingEntity> wifiUserList = RouteManager.getInstance().getAllWifiUsers();

            if (wifiUserList != null && !wifiUserList.isEmpty()) {
                List<RoutingEntity> offlineUserList = new ArrayList<>();
                for (RoutingEntity entity : wifiUserList) {
                    List<RoutingEntity> offlines =
                            RouteManager.getInstance().updateNodeAsOffline("", entity.getAddress());
                    if (offlines != null && !offlines.isEmpty()) {
                        offlineUserList.addAll(offlines);
                    }

                }

                if (meshXLCListener != null) {
                    meshXLCListener.onWifiUserDisconnected(offlineUserList);
                }
            }


            // RouteManager.getInstance().resetDbForWifiNode();

            int currentRole = getCurrentRole();

            if (currentRole != CurrentRole.WIFI_CONNECTING
                    && currentRole != CurrentRole.SPECIAL_GO
                    && currentRole != CurrentRole.SPECIAL_LC) {

                startTaskConditionally("WifiDisconnect", CurrentRole.LC);

                //There is no any connected alive node
                //So start ble process from here

                boolean isAliveNodeEmpty = RouteManager.getInstance().getAllOnlineNodeIds().isEmpty();
                if (isAliveNodeEmpty && bleManager != null) {
                    bleManager.initAllProcess(bleManager.hasForceConnection());
                }
            }
        }

        @Override
        public void onWifiAlreadyConnected() {
            wiFiConnector.disConnect("WiFiDirectStatusListener");
        }

        @Override
        public void onConnectionAttemptTimeout(boolean isWifiConnected) {
            //If it is running in Special Go or LC mode then
            if (!isWifiConnected && (getCurrentRole() == CurrentRole.SPECIAL_GO || getCurrentRole() == CurrentRole.SPECIAL_LC)) {
                MeshLog.v("WiFi not connected but in Special Go or LC");
                return;
            }
            MeshLog.v("WiFi connection  status :" + isWifiConnected);
            //setSpecialGoNodeId(null);
            if (!isWifiConnected) {
                startTaskImmediately("onConnectionAttemptTimeout", CurrentRole.LC);
            } else {
                Log.e("p2p_process", "Role after connection : " + getModeType(getNextRole()));

                //Autonomous search process will enter ideal state after Hello packet send

                if (getCurrentRole() == CurrentRole.WIFI_CONNECTING) {
                    // HandlerUtil.postBackground(() -> writeNextRoleForceFully("onConnectionAttemptTimeout", CurrentRole.IDEAL), 2000);
                    HandlerUtil.postBackground(() -> {
                        SharedPref.write(Constant.KEY_RUNNING_ROLE, CurrentRole.IDEAL);
                    }, 2000);
                }
            }
        }

        @Override
        public void onConnectedWithSelfGo() {
            stopGo();
        }
    };


    //No LC node connected but remain in ideal mode block state resolve
    int idealStateWithNoLcNodeConnected = 0;

    private final Runnable parentGoLcScheduler = new Runnable() {
        @Override
        public void run() {
            int roleNeedsToExecute = SharedPref.readInt(Constant.KEY_NEXT_ROLE);


            int sdkTestMode = getTestMode();
            if (sdkTestMode != TestMode.SKIP.ordinal()) {
                if (sdkTestMode == TestMode.GO.ordinal()) {
                    roleNeedsToExecute = CurrentRole.GO;
                } else {
                    roleNeedsToExecute = CurrentRole.LC;
                }
            }

            int wifiUser = RouteManager.getInstance().getWifiUser().size();

            if ((roleNeedsToExecute == CurrentRole.GO /*|| dataShareMode == PreferencesHelper.DATA_SELLER*/) && wifiUser == 0) {
                roleNeedsToExecute = CurrentRole.GO;
                createGo();

            } else if (roleNeedsToExecute == CurrentRole.LC) {
                if (wifiUser == 0) {
                    wiFiDirectClient.startSpecialSearch(null);
                    setSpecialGoNodeId(null);
                    searchGo();
                }
            } else if (roleNeedsToExecute == CurrentRole.SPECIAL_GO) {
                stopAllP2pProcess();
                HandlerUtil.postBackground(() -> createGo(), 1500);

            } else if (roleNeedsToExecute == CurrentRole.SPECIAL_LC) {
                searchGo();

            }
            long delay = getRandomDelay(roleNeedsToExecute);

            if (wifiUser > 0) {
                delay = getRandomDelay(CurrentRole.IDEAL);
            }

            MeshLog.e("[p2p_process] Scheduler current role: " + getModeType(roleNeedsToExecute)
                    + " with time: " + delay + " lc: " + wifiUser);

            //Schedule for next called
            HandlerUtil.postBackground(this, delay);

            writeCurrentRole(roleNeedsToExecute);

            int nextRole = CurrentRole.IDEAL;
            if (roleNeedsToExecute == CurrentRole.GO && wifiUser == 0) {
                nextRole = CurrentRole.LC;
            } else if (roleNeedsToExecute == CurrentRole.LC && wifiUser == 0) {
                nextRole = CurrentRole.GO;
            } else if (roleNeedsToExecute == CurrentRole.SPECIAL_GO) {
                nextRole = CurrentRole.LC;
            } else if (roleNeedsToExecute == CurrentRole.SPECIAL_LC) {
                nextRole = CurrentRole.GO;
            } else if (roleNeedsToExecute == CurrentRole.WIFI_CONNECTING) {
                nextRole = CurrentRole.WIFI_CONNECTING;
            }

            if (nextRole == CurrentRole.IDEAL && wifiUser == 0) {
                idealStateWithNoLcNodeConnected++;
            } else {
                idealStateWithNoLcNodeConnected = 0;
            }

            if (idealStateWithNoLcNodeConnected >= 2) {
                nextRole = CurrentRole.LC;
                idealStateWithNoLcNodeConnected = 0;
            }

            if (nextRole == CurrentRole.IDEAL && P2PUtil.isMeGO()) {
                List<RoutingEntity> allWifiUserList = RouteManager.getInstance().getWifiUser();

                if (!allWifiUserList.isEmpty()) {
                    ConcurrentLinkedQueue<RoutingEntity> allWifiUserQueue = new ConcurrentLinkedQueue<>(allWifiUserList);
                    meshXLCListener.onGetLegacyUser(allWifiUserQueue);
                }
            }

            writeNextRole("scheduler", nextRole);
        }
    };


    private long getRandomDelay(int currentState) {
        if (currentState == CurrentRole.SPECIAL_GO || currentState == CurrentRole.SPECIAL_LC) {
            return SPECIAL_GO_OR_LC * 1000;
        } else if (currentState == CurrentRole.IDEAL) {
            return IDEAL_TIME * 1000;
        } else if (currentState == CurrentRole.WIFI_CONNECTING) {
            return WIFI_CONNECTING * 1000;
        } else {
            int randValue = (new Random().nextInt((SEARCHING_SCHEDULED_MAX + 1) -
                    SEARCHING_SCHEDULED_MIN) + SEARCHING_SCHEDULED_MIN) * 1000;
            return randValue;
        }
    }

    private String getModeType(int currentRole) {
        if (currentRole == CurrentRole.GO) {
            return "GO";
        } else if (currentRole == CurrentRole.LC) {
            return "LC";
        } else if (currentRole == CurrentRole.WIFI_CONNECTING) {
            return "WiFi-Connecting";
        } else if (currentRole == CurrentRole.BLE_CONNECTING) {
            return "BLE-Connecting";
        } else if (currentRole == CurrentRole.SPECIAL_LC) {
            return "Special-LC";
        } else if (currentRole == CurrentRole.SPECIAL_GO) {
            return "Special-GO";
        } else if (currentRole == CurrentRole.IDEAL) {
            return "IDEAL";
        }
        return "Undefined";
    }


    private final Runnable specialGOSearchTracker = () -> setSpecialGoNodeId(null);

    private void startSpecialGoSearchTracker() {
        HandlerUtil.postBackground(specialGOSearchTracker, SPECIAL_GO_OR_LC * 1000);
    }

    private void prepareAndSendDisconnectionMessage(APCredential credential, List<RoutingEntity> routingEntities) {
        RoutingEntity myEntity = new RoutingEntity(myNodeId);
        myEntity.setType(RoutingEntity.Type.WiFi);

        List<RoutingEntity> myEntityList = new ArrayList<>();
        myEntityList.add(myEntity);
        String nodeIdJson = buildNodeIdListJson(myEntityList, RoutingEntity.Type.WiFi);
        byte[] data = JsonDataBuilder.buildNodeLeaveEvent(nodeIdJson);
        Queue<RoutingEntity> routingEntityQueue = new LinkedList<>();
        routingEntityQueue.addAll(routingEntities);
        sendDisconnectionMessage(credential, routingEntityQueue, data);
    }

    private void sendDisconnectionMessage(APCredential credential, Queue<RoutingEntity> entityQueue, byte[] data) {
        if (entityQueue.isEmpty()) {
            List<RoutingEntity> offlineEntities = RouteManager.getInstance().resetDbForWifiNode();

            if (CollectionUtil.hasItem(offlineEntities)) {
                for (RoutingEntity entity : offlineEntities) {
                    meshXLCListener.disconnectUserPassToUi(entity.getAddress());
                }
            }
            attemptToConnect(credential);
        } else {
            RoutingEntity routingEntity = entityQueue.poll();
            wifiTransPort.sendMessageAndGetCallBack(routingEntity.getIp(), data, new MessageCallback() {
                @Override
                public void onMessageSend(boolean isSuccess) {
                    sendDisconnectionMessage(credential, entityQueue, data);
                }
            });
        }
    }

    private String buildNodeIdListJson(List<RoutingEntity> routingEntities, int type) {
        if (CollectionUtil.hasItem(routingEntities)) {

            List<DisconnectionModel> disconnectionModelList = new ArrayList<>();
            for (RoutingEntity entity : routingEntities) {
                DisconnectionModel model = new DisconnectionModel();
                model.setNodeId(entity.getAddress());

                if (type == RoutingEntity.Type.BLE) {
                    model.setUserType(RoutingEntity.Type.BLE_MESH);
                } else {
                    if (entity.getType() == RoutingEntity.Type.WiFi) {
                        model.setUserType(RoutingEntity.Type.WiFi);
                    } else {
                        model.setUserType(RoutingEntity.Type.WifiMesh);
                    }
                }
                //Todo we have to set other type when we will work on other transport

                disconnectionModelList.add(model);
            }


            return GsonUtil.on().toJsonFromDisconnectionList(disconnectionModelList);
        }
        return null;
    }


}
