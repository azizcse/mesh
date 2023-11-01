package com.w3engineers.mesh;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.RemoteException;
import android.text.TextUtils;

import com.balsikandar.crashreporter.CrashReporter;
import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.Adhoc.protocol.WifiDetector;
import com.w3engineers.mesh.ble.BleTransport;
import com.w3engineers.mesh.bluetooth.BTManager;
import com.w3engineers.mesh.bluetooth.BTStateReceiver;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.controller.DriverManager;
import com.w3engineers.mesh.datasharing.database.DatabaseService;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.peers.PeersEntity;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.discovery.MeshXAPListener;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLCListener;
import com.w3engineers.mesh.libmeshx.wifi.WiFiStateMonitor;
import com.w3engineers.mesh.libmeshx.wifid.Pinger;
import com.w3engineers.mesh.libmeshx.wifid.WifiCredential;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.model.DisconnectionModel;
import com.w3engineers.mesh.premission.MeshSystemRequestActivity;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.queue.DispatcherHelper;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.tunnel.RemoteTransport;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.MessageCallback;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.TransportStateListener;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;
import com.w3engineers.mesh.wifidirect.CurrentRole;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import timber.log.Timber;

/**
 * <h1>Manages P2P connection  related update</h1>
 * child class of
 *
 * @see TransportManager
 */
public class TransportManagerX extends TransportManager implements TransportStateListener {

    private final long HIGH_BAND_ROLE_MAX_DURATION = 60 * 1000;
    public static final int MODE_BT = 3, MODE_CLIENT = 1, MODE_MASTER = 2, MODE_ADHOC = 4,
            MODE_P2P = 5;

    private volatile String fileReceiverId;

    /**
     * This user ID may sender or receiver.
     */
    private volatile String fileUserId = "";
    private final long FORCE_CONNECTION_TRACKER_TIME = 180 * 1000;

    public interface NodeConnectivityChangeListener {
        void onNodeConnectionChanged(boolean isNewNode);
    }

    /**
     * Maintain app's port internally to the SDK
     */
    public static int APP_PORT;
    private MeshTransport mMeshTransport;
    private volatile boolean mWiFiEnabled;

    private static TransportManagerX meshManager;
    private static final Object lock = new Object();

    private List<String> fileRequestSenderMap = new ArrayList<>();


    private WiFiStateMonitor.WiFiAdapterStateListener mWiFiAdapterStateListener = isEnabled -> {
        if (!isEnabled) {
            mWiFiEnabled = false;
            if (mLinkStateListener != null) {
                mLinkStateListener.onInterruption(LinkStateListener.USER_DISABLED_WIFI);
            }
        }
    };

    /**
     * Maintain the lifecycle of mesh library stopping
     */
    private volatile boolean mIsStoppingMesh;
    private MeshLibMessageEventQueue discoveryEventQueue;

    private TransportManagerX(Context context, int appPort, String address, String publicKey,
                              String networkPrefix, String multiverseUrl,
                              LinkStateListener linkStateListener) {
        super(context, appPort, address, publicKey, networkPrefix, multiverseUrl, linkStateListener);
        mBTManager.mBTStateListener = mBTStateListener;
        myNodeId = address;

        mDriverManager = DriverManager.getInstance(context, myNodeId);
        mDriverManager.initWifiDirectManagerListener(mMeshXAPListener, mMeshXLCListener);
    }


    /**
     * <p>
     * Create Thread safe single object
     * Protect multiple object if multiple thread attempt to create
     * object only one thread can able to create object
     * </p>
     *
     * @param context : application context
     * @return : {@link TransportManager} object
     */
    public static TransportManagerX on(Context context, int appPort, String address, String publicKey, String networkPrefix,
                                       String multiverseUrl, LinkStateListener linkStateListener) {
        TransportManagerX instance = meshManager;
        if (instance == null) {
            synchronized (lock) {
                instance = meshManager;
                if (instance == null) {
                    instance = meshManager = new TransportManagerX(context, appPort, address,
                            publicKey, networkPrefix, multiverseUrl, linkStateListener);
                }
            }
        }
        return instance;
    }

    public static TransportManagerX getInstance() {
        return meshManager;
    }


    public void setFileSendingMode(boolean isInFileSharingMode) {
        this.isFileSendingMode = isInFileSharingMode;
    }

    public boolean isFileSendingMode() {
        return isFileSendingMode;
    }

    public boolean isFileReceivingMode() {
        return isFileReceivingMode;
    }

    public void setFileReceivingMode(boolean fileReceivingMode) {
        isFileReceivingMode = fileReceivingMode;
    }

    public String getFileReceiverId() {
        return fileReceiverId;
    }

    public void setFileReceiverId(String fileReceiverId) {
        this.fileReceiverId = fileReceiverId;
    }

    public String fileUserId() {
        return fileUserId;
    }

    public boolean hasAnyFileRequest() {
        return !fileRequestSenderMap.isEmpty();
    }


    public String getFileRequestSenderWhenBusy() {
        if (fileRequestSenderMap.isEmpty()) {
            return null;
        } else {
            return fileRequestSenderMap.get(0);
        }
    }

    public void setFileRequestSenderWhenBusy(String fileRequestSenderWhenBusy) {
        if (!fileRequestSenderMap.contains(fileRequestSenderWhenBusy)) {
            fileRequestSenderMap.add(fileRequestSenderWhenBusy);
        }
    }

    private void configureDiscoveryQueue() {
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("NsdLink " + this.hashCode() + " Output");
                thread.setDaemon(true);
                return thread;
            }
        });

        discoveryEventQueue = new MeshLibMessageEventQueue(pool);
    }

    @Override
    public void configTransport(String nodeId, String publicKey, int appId) {
        CombineTransport combineTransport = new CombineTransport();
        configureDiscoveryQueue();
        connectionLinkCache = ConnectionLinkCache.getInstance().initConnectionLinkCache(mLinkStateListener, nodeId);

        //mReceiveEventQueue = DispatchQueue.newSerialExecutor("receive_event");

        mMessageDispatcher = new MessageDispatcher(TransportManagerX.this, newSerialExecutor());


        wifiTransPort = new WifiTransPort(mContext, nodeId, linkStateListener, this,
                connectionLinkCache, mMessageDispatcher);

       /* bluetoothTransport = new BluetoothTransport(mContext, appId, nodeId, linkStateListener, this,
                connectionLinkCache, mMessageDispatcher);*/

        /*internetTransport = new InternetTransport(mContext, nodeId, connectionLinkCache, linkStateListener,
                mMessageDispatcher);*/

        remoteTransport = new RemoteTransport(mContext, appId, nodeId, connectionLinkCache, linkStateListener,
                mMessageDispatcher);

       /* adhocTransport = new AdHocTransport(mContext, appId, nodeId, linkStateListener, connectionLinkCache,
                mMessageDispatcher);
*/
        bleTransport = new BleTransport(mContext, nodeId, linkStateListener, this, connectionLinkCache);


        wifiTransPort.setInterTransport(/*bluetoothTransport, */remoteTransport, mForwardListener,
                bleTransport, discoveryEventQueue, TransportManagerX.this);
        bleTransport.setInterTransport(wifiTransPort, remoteTransport,/*adhocTransport, bluetoothTransport,*/
                discoveryEventQueue, TransportManagerX.this);

        remoteTransport.setInterTransport(wifiTransPort, bleTransport, /*bluetoothTransport, */mForwardListener, discoveryEventQueue);

        //bluetoothTransport.setInterTransport(wifiTransPort, adhocTransport,remoteTransport, mForwardListener);

        // internetTransport.setInterTransport(wifiTransPort, adhocTransport, bleTransport, mForwardListener);

        /*adhocTransport.setInterTransport(bluetoothTransport, wifiTransPort, mForwardListener);*/


        //combineTransport.addTransport(bleTransport, wifiTransPort, internetTransport, adhocTransport);
        combineTransport.addTransport(/*bluetoothTransport,*/ wifiTransPort, remoteTransport, bleTransport);

        mMeshTransport = combineTransport;

        APP_PORT = appId;

        //wifiTransPort.setNodeConnectivityChangeListener(mNodeConnectivityChangeListenerWiFi);
        //bluetoothTransport.setNodeConnectivityChangeListener(mNodeConnectivityChangeListenerBT);


    }

    public void startMesh() {

        MeshLog.v("startMesh");
        //If internet user no specified hardware required
        if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER) {

            initMeshProcess();
            MeshLog.v("1");
        } else if (!mPermissionHelper.hasPermissions(mContext, MeshSystemRequestActivity.MESH_PERMISSIONS) &&
                mPermissionHelper.isLocationProviderEnabled(mContext)) {

            //If process has no permission then we sent call back to app for further ui/ux works
            MeshLog.v("2");
            List<String> notGrantedPermissions = mPermissionHelper.getNotGrantedPermissions(mContext,
                    MeshSystemRequestActivity.MESH_PERMISSIONS);

            MeshLog.v(notGrantedPermissions.toString());

            if (CollectionUtil.hasItem(notGrantedPermissions)) {
                MeshLog.v("4");
                this.linkStateListener.onInterruption(notGrantedPermissions);

            } else {
                MeshLog.v("5");
                this.linkStateListener.onInterruption(LinkStateListener.LOCATION_PROVIDER_OFF);
            }
        } else {
            MeshLog.v("3");
            //If all prerequisite set then we go for BT or location provider on related check which
            // in turn call initMeshProcess to start mesh process
            //startPermissionActivity(null);

            initMeshProcess();
        }
    }

    public void restart(int newRole) {
        MeshLog.i("restart sellerMode tm " + newRole);
        PreferencesHelper.on().setDataShareMode(newRole);
        stopMesh();
        if (newRole != PreferencesHelper.MESH_STOP) {
            initMeshProcess();
        } else {
            stopHardWare();
        }
    }


    @Override
    public void initMeshProcess() {

        super.initMeshProcess();

        if (mMeshTransport == null) {
            return;
        }
        mIsStoppingMesh = false;
        MeshLog.i("Starting Mesh Lib");
        List<RoutingEntity> offlineList = RouteManager.getInstance().resetDb();
        // send to UI
        if (CollectionUtil.hasItem(offlineList)) {
            for (RoutingEntity entity : offlineList) {
                if (linkStateListener != null) {
                    linkStateListener.onUserDisconnected(entity.getAddress());
                }
            }
        }

        RouteManager.getInstance().deleteRoutingTableEntity();
        //RouteManager.getInstance().deleteNodeInfoEntity();


        mMeshTransport.start();//starts only bt transport
        CrashReporter.initialize(mContext, createCrashReportPath().getPath());

        initiateP2P();

        // Master/ Client/Bt
        /*int randomState;
        if (!WiFiUtil.isWifiConnected(mContext) || P2PUtil.isConnectedWithPotentialGO(mContext)) {
            MeshLog.v("[p2p_process] device mode p2p");
            randomState = MODE_P2P;
        } else {

            MeshLog.v("[p2p_process] device mode adhoc");
            // Connected with AdHoc network
            randomState = MODE_ADHOC;
            MeshLog.i(" BT SOFT disable called from initMeshProcess");
            if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
                stopBtDiscovery();
                stopP2pDiscovery();
                startAdHocDiscovery();
            }

        }

        SharedPref.write(Constant.RANDOM_STATE, randomState);

        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            MeshLog.v("[p2p_process] device mode select: " + randomState);
            // Start AS a Client
            if (randomState == MODE_BT) {
                if (BTManager.getInstance(mContext).isEnable()) {
                    initiateBtSearch();
                }
            } else if (randomState == MODE_P2P) {
                initiateP2P();
            }
        } else {
            if (!WiFiUtil.isConnectedWithAdhoc(mContext)) {
                HardwareStateManager hardwareStateManager = new HardwareStateManager();
                hardwareStateManager.init(mContext);
                hardwareStateManager.disableAll(() ->
                        MeshLog.v("[WIFI]Requested to turn off all hardware. Disabling done."));
            }
        }*/
    }

    public void initiateP2P() {
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            startAsP2P();
        } else {
            MeshLog.e("User select internet only mode");
            mDriverManager.stopAllP2pProcess();
        }
    }

    public void releaseHighBandMode() {

      /*  HandlerUtil.postBackground(mResetHighBand);
        if (WiFiDirectManagerLegacy.getInstance() != null) {
            WiFiDirectManagerLegacy.getInstance().releaseHighBandMode();
        }

        if (adhocTransport != null) {
            //Release hardware lock, tdls if any
        }*/
    }

    // FIXME: 10/11/2019 patch given. This API should be async and app layer should receive
    //  callback accordingly
    @Override
    public void stopMesh() {
        super.stopMesh();
        mIsStoppingMesh = true;

        MeshLog.i("Stopping Mesh Lib");

        if (mMessageDispatcher != null) {
            mMessageDispatcher.shutdown();
        }

        if (mMeshTransport != null && ((CombineTransport) mMeshTransport).isRunning()) {
            mMeshTransport.stop();
        }
        //Stop Go LC
        mDriverManager.stopAllP2pProcess();
        //Stop GO, LC scheduler
        mDriverManager.stopGoLcScheduler();

        // stop BLE
        mDriverManager.shutDownBle();

        if (mBTManager != null) {
            mBTManager.cancelDiscovery();
        }
        if (connectionLinkCache != null) {
            connectionLinkCache.clearAllLinks();
        }
        if (mMessageDispatcher != null) {
            mMessageDispatcher.shutdown();
        }

        /*if (bluetoothTransport != null) {
            bluetoothTransport.stop();
        }*/

        if (bleTransport != null) {
            bleTransport.stop();
        }

        //Shutdown discovery queue
        if (discoveryEventQueue != null) {
            discoveryEventQueue.shutdown();
        }

        wifiTransPort = null;
        bluetoothTransport = null;
//        internetTransport = null;
        //bluetoothTransport = null;
        mMeshTransport = null;
        remoteTransport = null;
        bleTransport = null;
    }

    @Override
    protected void startTransport() {
        if (mMeshTransport != null) {
            mMeshTransport.start();
        }

    }

    @Override
    protected void stopTransport() {
        if (mMeshTransport != null) {
            mMeshTransport.stop();
            //RouteManager.getInstance().deleteRoutingTableEntity();
            //RouteManager.getInstance().deleteNodeInfoEntity();
        }
    }

    @Override
    protected void onHelloPacketSend(String ip, boolean isSuccess) {
        MeshLog.e("[p2p_process] hello packet send status :" + isSuccess + " ip: " + ip);
        int taskType = CurrentRole.IDEAL;
        if (!isSuccess) {
            if (ip.equals(WifiTransPort.P2P_MASTER_IP_ADDRESS)) {
                taskType = CurrentRole.GO;
            } else {
                taskType = CurrentRole.LC;
            }

            mMeshXAPListener.onGODisconnectedWith(ip);
        }
        mDriverManager.startP2pTask(taskType);
    }

    @Override
    public void onWifiP2pUserConnected(boolean isLastHello, String senderAddress) {
        MeshLog.v("[lazy] BLE server running after discover");
        MeshLog.v("[BLE_PROCESS] BLE server running after discover");

        if (isLastHello) {
            // we have to check we have any duplicate connection (Multiple interface)
            // with the same senderAddress or not
            // Multiple Interface means Wifi and BLE
            // Basically this senderAddress now connected with Wifi So we can remove BLE connection

            RoutingEntity bleRoute = RouteManager.getInstance().getSingleUserInfoByType(senderAddress,
                    RoutingEntity.Type.BLE);

            if (bleRoute != null && bleRoute.isOnline()) {
                MeshLog.i("[Ble_Process] multiple interface connection occurred");
                // now disconnect the BLE connection
                mDriverManager.removeBleConnectionById(bleRoute.getAddress());
            }
        }

        if (RouteManager.getInstance().getBleUsers().size() < 1) {

            mDriverManager.removeBLEStartRunnable();
            mDriverManager.stopBLEScan();
            //mDriverManager.startBLE(false,"onWifiP2pUserConnected");
            mDriverManager.stopBLE();
            //mDriverManager.startBleServer();
            mDriverManager.startBLE(false, "onWifiP2pUserConnected");
        }

    }

    @Override
    public void onReceiveHelloFromClient(String userId) {
        if (getFileReceiverId() != null && getFileReceiverId().equals(userId)) {
            if (isFileSendingMode()) {
                MeshLog.v("[FILE_PROCESS] received desired node ID for file sending");
                mMessageListener.onConnectedWithTargetNode(userId);
            }
        } else {
            //setFileReceiverId(null);
        }
    }

    @Override
    public void onBleUserConnect(boolean isServer, String directNodeId) {
        mDriverManager.stopBLEScan();
        if (isServer) {
            mDriverManager.restartAdvertise();
        } else {
            mDriverManager.startBleServer();
        }

        if (!isServer) {
            // start BT classic connection
            //BTManager.getInstance().startSpecialSearch(directNodeId);
        }

        BTManager.getInstance().startSpecialSearch(directNodeId);
    }

    @Override
    public void onRemoveRedundantBleConnection(String userId) {
        mDriverManager.removeBleConnectionById(userId);
    }

    @Override
    public void onReceiveSoftApCredential(String ssid, String password, String goNodeId) {
        if (isFileReceivingMode() || isFileSendingMode()) {
            MeshLog.v("[BLE_PROCESS] Received credential from other for subnet merge. But I'm in file sending mode");
            return;
        }
        mDriverManager.connectWithReceivedSoftAP(ssid, password, goNodeId);
    }

    @Override
    public void onMaximumLcConnect() {
        mDriverManager.stopGoBroadcast();
    }

    @Override
    public void onGetForceConnectionRequest(String receiverId, String ssid, String password,
                                            boolean isRequest, boolean isBle, boolean isAbleToReceive) {

        if (isRequest) {
            //The below line will return local connected user
            RoutingEntity routingEntity;
            if (isBle) {
                routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiverId, RoutingEntity.Type.BLE);
            } else {
                routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiverId, RoutingEntity.Type.WiFi);
            }

            if (!isFileSendingMode() && !isFileReceivingMode()) {

                fileUserId = receiverId;

                // Set file sending mode and start other process
                setFileReceivingMode(true);
                startForceConnectionTracker(true);
                // First check the SSID and password not empty. The reason is if the credential not empty

                if (isBle) {
                    mDriverManager.stopBLEScan();
                }

                if (Text.isNotEmpty(ssid) && Text.isNotEmpty(password)) {
                    // before the connection just check that it has BLE connection with that device or not
                    if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        // Then disconnect this BLE user
                        mDriverManager.removeBleConnectionById(receiverId);
                    }
                    MeshLog.v("[P2p_process] start special search in BLE and wifi for file");
                    mDriverManager.startSpecificBLESearch(receiverId);
                    mDriverManager.startSpecialSearch(receiverId);
                } else {
                    // Check I'm GO or LC
                    String mySSID = "";
                    String myPassword = "";
                    if (P2PUtil.isMeGO()) {
                        if (WifiCredential.ssid != null) {
                            mySSID = WifiCredential.ssid;
                            myPassword = WifiCredential.password;
                            wifiTransPort.setSpecificNodeIdWhoWantsToConnect(receiverId);
                        }
                        mDriverManager.requestGOScheduledBroadCast();
                    }

                    String finalMySSID = mySSID;
                    String finalMyPassword = myPassword;

                    byte[] data = JsonDataBuilder.prepareForceConnectionMessage(myNodeId, receiverId,
                            mySSID, myPassword, false, true);

                    if (routingEntity.getType() == RoutingEntity.Type.WiFi
                            && !TextUtils.isEmpty(routingEntity.getIp())) {
                        wifiTransPort.sendMessageAndGetCallBack(routingEntity.getIp(), data, new MessageCallback() {
                            @Override
                            public void onMessageSend(boolean isSuccess) {
                                MeshLog.v("[BLE_PROCESS] force connection reply send from wifi " + isSuccess);
                                if (isSuccess && TextUtils.isEmpty(finalMySSID) || TextUtils.isEmpty(finalMyPassword)) {
                                    sendDisconnectionMessageAndStartGoLc(true, receiverId);
                                }
                            }
                        });

                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        bleTransport.sendConfirmationMessage(routingEntity.getAddress(), data, new MessageCallback() {
                            @Override
                            public void onMessageSend(boolean isSuccess) {
                                MeshLog.v("[BLE_PROCESS] force connection reply send from BLE " + isSuccess);
                                if (isSuccess && TextUtils.isEmpty(finalMySSID) || TextUtils.isEmpty(finalMyPassword)) {
                                    sendDisconnectionMessageAndStartGoLc(true, receiverId);
                                }
                            }
                        });
                    }
                    //sendMessageByType(data, routingEntity);
                }

            } else {
                MeshLog.v("[BLE_PROCESS] I'm busy. I cannot receive file at this moment");

                byte[] data = JsonDataBuilder.prepareForceConnectionMessage(myNodeId, receiverId,
                        null, null, false, false);

                setFileRequestSenderWhenBusy(receiverId);
                sendMessageByType(data, routingEntity);
            }

        } else {
            // It is a reply message. Some times it will not come
            MeshLog.v("[FILE_PROCESS] We have got force connection reply");
            if (isAbleToReceive) {
                fileUserId = receiverId;

                //Start time out tracking for connection
                startForceConnectionTracker(true);


                // before the connection just check that it has BLE connection with that device or not
                RoutingEntity entity = RouteManager.getInstance().getShortestPath(receiverId);
                if (entity != null && entity.getType() == RoutingEntity.Type.BLE) {
                    // Then disconnect this BLE user
                    mDriverManager.removeBleConnectionById(receiverId);
                }

                if (Text.isNotEmpty(ssid) && Text.isNotEmpty(password)) {
                    mDriverManager.startSpecificBLESearch(receiverId);
                    mDriverManager.startSpecialSearch(receiverId);
                } else {
                    if (!RouteManager.getInstance().getWifiUser().isEmpty()) {
                        //nothing need to do
                        //Password already send and receiver device will try to connect.
                    } else {
                        sendDisconnectionMessageAndStartGoLc(false, receiverId);
                    }
                }
            } else {
                //Todo notify user or handle queue that receiver unable to receive file at this moment
                setFileSendingMode(false);
                startForceConnectionTracker(false);
                MeshLog.v("[BLE_PROCESS] This user is busy for receiving file message");
                executeNextFileItemInQueue(receiverId);
            }

        }
    }

    /**
     * The reason behind of checking I'm already in file sending mode or receiving mode
     * because if my mode is receiving  then after file receive complete or
     * not the next queue will execute from that particular callback
     * <p>
     * Same thing for file sending mode. If file send success or not or got busy message
     * the next file execute from that particular callback
     *
     * @param senderId    Main file receiver ID
     * @param receiverId  My id if match
     * @param isAvailable confirmation
     */
    @Override
    public void onGetFileFreeModeMessage(String senderId, String receiverId, boolean isAvailable) {
        if (myNodeId.equals(receiverId)) {
            // We can check that we have file sending/receiving mode or not
            // If we are file receiving or sending  mode then we will not execute queue

            if (isFileReceivingMode()) {
                MeshLog.v("[FILE_PROCESS] Already file receiving. The queue will execute from that state");
            } else {
                if (mMessageListener != null) {
                    mMessageListener.onGetFileFreeModeToSend(senderId);
                }
            }


        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(senderId, receiverId, RoutingEntity.Type.WiFi);
            if (routingEntity == null || !routingEntity.isOnline()) {
                MeshLog.v("[FILE_PROCESS] File free message not send dut to routing null or offline");
                return;
            }
            byte[] data = JsonDataBuilder.prepareFreeMessageForRequestNode(senderId, receiverId, isAvailable);
            sendMessageByType(data, routingEntity);
        }
    }

    @Override
    public void onReceivedNetworkFullResponseFromGo(String senderId) {
        mDriverManager.triggerWifiDisconnectionAndSearch(senderId);
    }


    private void sendDisconnectionMessageAndStartGoLc(boolean isNeedToStartGo, String fileReceiverId) {
        List<RoutingEntity> wifiUsers = RouteManager.getInstance().getWifiUser();
        if (CollectionUtil.hasItem(wifiUsers)) {
            RoutingEntity myEntity = new RoutingEntity(myNodeId);
            myEntity.setType(RoutingEntity.Type.WiFi);

            List<RoutingEntity> myEntityList = new ArrayList<>();
            myEntityList.add(myEntity);
            String nodeIdJson = buildNodeIdListJson(myEntityList, RoutingEntity.Type.WiFi);
            byte[] data = JsonDataBuilder.buildNodeLeaveEvent(nodeIdJson);
            Queue<RoutingEntity> entityQueue = new LinkedList<>();
            entityQueue.addAll(wifiUsers);
            sendDisconnectionMessageOrStartGoLc(entityQueue, data, isNeedToStartGo, fileReceiverId);
        } else {
            MeshLog.v("No WiFi user connected");
            sendDisconnectionMessageOrStartGoLc(null, null, isNeedToStartGo, fileReceiverId);
        }

    }

    /**
     * <h1>Send disconnection one after another
     * When queue is empty then start GO or LC
     * </h1>
     *
     * @param entityQueue     (routing entity queue)
     * @param data            (byte array)
     * @param isNeedToStartGo (GO, LC)
     * @param receiverId      (String ID)
     */
    private void sendDisconnectionMessageOrStartGoLc(Queue<RoutingEntity> entityQueue, byte[] data, boolean isNeedToStartGo, String receiverId) {
        if (entityQueue == null || entityQueue.isEmpty()) {
            MeshLog.v("[p2p_process] queue empty start go lc :" + isNeedToStartGo);
            List<RoutingEntity> offlineEntities = RouteManager.getInstance().resetDbForWifiNode();
            if (CollectionUtil.hasItem(offlineEntities)) {
                for (RoutingEntity entity : offlineEntities) {
                    linkStateListener.onUserDisconnected(entity.getAddress());
                }
            }
            if (isNeedToStartGo) {
                mDriverManager.stopBLEScan();
                mDriverManager.startGoForForceConnection();
                wifiTransPort.setSpecificNodeIdWhoWantsToConnect(receiverId);

                // before the connection just check that it has BLE connection with that device or not
                RoutingEntity entity = RouteManager.getInstance().getShortestPath(receiverId);
                if (entity != null && entity.getType() == RoutingEntity.Type.BLE) {
                    // Then disconnect this BLE user
                    mDriverManager.removeBleConnectionById(receiverId);
                }

            } else {
                MeshLog.v("[P2p_process] start special search in BLE and wifi for file");
                mDriverManager.startSpecificBLESearch(receiverId);
                mDriverManager.startSpecialSearch(receiverId);
            }

        } else {
            RoutingEntity entity = entityQueue.poll();
            wifiTransPort.sendMessageAndGetCallBack(entity.getIp(), data, new MessageCallback() {
                @Override
                public void onMessageSend(boolean isSuccess) {
                    sendDisconnectionMessageOrStartGoLc(entityQueue, data, isNeedToStartGo, receiverId);
                }
            });
        }
    }


    @Override
    public void onBluetoothChannelConnect(LinkMode linkMode) {

       /* MeshLog.i("onBluetoothChannelConnect MODE:: " + linkMode.toString());
        // Random mode
        int existingMode = SharedPref.readInt(Constant.RANDOM_STATE);
        //
        if (existingMode == MODE_ADHOC && RouteManager.getInstance().isAdhocUserConneted()) {
            MeshLog.i("Bluetooth connection with ADHOC");
            return;
        }

        // if BT server, start as p2p legacy client
        if (!mIsStoppingMesh) {

            // if BT server, start as p2p legacy client

            if (linkMode == LinkMode.SERVER) {
                if (!RouteManager.getInstance().isWifiUserConnected()) {
                    MeshLog.i(" BT CONN MODE: " + linkMode + "-> p2p master mode picked");
                    SharedPref.write(Constant.RANDOM_STATE, MODE_MASTER);
                    stopP2pDiscovery();
                }
            }
            // if BT client, start as p2p group owner
            else if (linkMode == LinkMode.CLIENT) {
                if (!RouteManager.getInstance().isWifiUserConnected()) {
                    MeshLog.i(" BT CONN MODE: " + linkMode + "-> p2p client mode picked");
                    SharedPref.write(Constant.RANDOM_STATE, MODE_CLIENT);
                    stopP2pDiscovery();
                }
            }
        } else {
            MeshLog.e("Connection not allowed in BT");
        }*/

    }

    private BTStateReceiver.BTStateListener mBTStateListener = isOn -> {
        //Upon turning off BT now we turn all BT process off and show a toast to user
        if (!isOn && !mBTManager.mIsBtTurningOff) {

            if (mLinkStateListener != null) {
                mLinkStateListener.onInterruption(LinkStateListener.USER_DISABLED_BT);
            }
        }
    };

    /**
     * For multiple path we have to ensure that user is available from any path or not
     *
     * @param userId
     */
    public void checkUserDisconnection(String userId) {
        RoutingEntity routingEntity = RouteManager.getInstance().getEntityByAddress(userId);
        if (routingEntity == null || !routingEntity.isOnline()) {
            mLinkStateListener.onUserDisconnected(userId);
            if (paymentLinkStateListener != null) {
                paymentLinkStateListener.onUserDisconnected(userId);
                paymentLinkStateListener.onProbableSellerDisconnected(userId);//TODO check if this is needed
            }
            remoteTransport.onBuyerDisconnected(userId);
        }
    }

    public final MeshXAPListener mMeshXAPListener = new MeshXAPListener() {
        // TODO: 7/26/2019 We will deal with WiFiTransport and BTTransport only, other responsibilities will be dealt by them
        @Override
        public void onSoftAPStateChanged(boolean isEnabled, String ssidName) {
            if (isEnabled) {
                SharedPref.write(Constant.KEY_DEVICE_AP_MODE, true);
            } else {
                SharedPref.write(Constant.KEY_DEVICE_AP_MODE, false);

                //GO disabled
                List<RoutingEntity> connectedEntities = RouteManager.getInstance().getConnectedDirectWifiUsers();
                if (CollectionUtil.hasItem(connectedEntities)) {
                    for (RoutingEntity routingEntity : connectedEntities) {
                        onGODisconnectedWith(routingEntity.getIp());
                    }
                }
            }
            if (!TextUtils.isEmpty(ssidName)) {
                SharedPref.write(Constant.KEY_DEVICE_SSID_NAME, ssidName);
            }
        }

        @Override
        public void onGOConnectedWith(Collection<WifiP2pDevice> wifiP2pDevices) {
            Timber.d("GO_Test:%s", wifiP2pDevices.toString());
        }

        @Override
        public void onGODisconnectedWith(String ip) {
            MeshLog.e("[p2p_process] GO DISCONNECTED FROM CLIENT " + ip);
            if (AddressUtil.isValidIPAddress(ip)) {
                RoutingEntity routingEntity = RouteManager.getInstance().getWiFiEntityByIp(ip);
                if (routingEntity != null) {
                    String disconnectedAddress = routingEntity.getAddress();
                    MeshLog.w("Disconnected Wifi dir ->>" + AddressUtil.makeShortAddress(disconnectedAddress));
                    List<RoutingEntity> offlineEntities = RouteManager.getInstance().updateNodeAsOffline(null, disconnectedAddress);

                    // First remove buyers Internet user
                    List<RoutingEntity> buyersInternetOfflineUserList = new ArrayList<>();
                    for (RoutingEntity entity1 : offlineEntities) {
                        if (entity1.getType() == RoutingEntity.Type.INTERNET) {
                            buyersInternetOfflineUserList.add(entity1);
                        }
                    }
                    offlineEntities.removeAll(buyersInternetOfflineUserList);


                    if (ip.equals(WifiTransPort.P2P_MASTER_IP_ADDRESS)) {
                        List<RoutingEntity> allOfflineWifiList = RouteManager.getInstance().resetDbForWifiNode();
                        if (allOfflineWifiList != null && !allOfflineWifiList.isEmpty()) {
                            offlineEntities.addAll(allOfflineWifiList);
                        }
                    }
                    List<RoutingEntity> validOfflineNodeList = new ArrayList<>();
                    // clear from own end
                    if (mLinkStateListener != null && CollectionUtil.hasItem(offlineEntities) && Text.isNotEmpty(disconnectedAddress)) {
                        for (RoutingEntity rEntity : offlineEntities) {
                            MeshLog.w("Disconnected Wifi hopped ->>" + AddressUtil.
                                    makeShortAddress(rEntity.getAddress()));
                            //connectionLinkCache.removeNodeInfo(rEntity.getAddress());
                            if (!disconnectedAddress.equals(rEntity.getAddress())) {
                                checkUserDisconnection(rEntity.getAddress());
                            }
                        }

                        checkUserDisconnection(disconnectedAddress);

                        for (RoutingEntity entity : offlineEntities) {
                            RoutingEntity validEntity = RouteManager.getInstance().getEntityByAddress(entity.getAddress());
                            if (validEntity == null || !validEntity.isOnline()) {
                                validOfflineNodeList.add(entity);
                            }
                        }

                    }

                    // Send offline entites to WiFi user. No need to filter
                    if (CollectionUtil.hasItem(offlineEntities)) {
                        String nodeIdJsonForWifi = buildNodeIdListJson(offlineEntities, RoutingEntity.Type.WiFi);
                        passOfflineEntitiesToWifiClients(nodeIdJsonForWifi);
                        //passOfflineEntitiesToAdhoc(nodeIdJson);
                    }


                    // For sending offline user to BLE we have to check that
                    // the ble user is connected by other path or not
                    // If ble user has only single path then we will send totally offline user
                    // if ble user has other path to communicate we will send current
                    // offline user though it is online from other path

                    List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();
                    for (RoutingEntity bleEntity : bleUserList) {

                        List<RoutingEntity> allPossiblePath = RouteManager.getInstance().getAllPossibleOnlinePathById(bleEntity.getAddress());

                        if (allPossiblePath.size() > 1) {

                            if (CollectionUtil.hasItem(offlineEntities)) {

                                String nodeIdJsonForBle = buildNodeIdListJson(offlineEntities, RoutingEntity.Type.BLE);
                                byte[] data = JsonDataBuilder.buildNodeLeaveEvent(nodeIdJsonForBle);
                                List<RoutingEntity> bleUser = new ArrayList<>();
                                bleUser.add(bleEntity);
                                passOfflineEntitiesToBLE(data, bleUser);
                            }
                        } else {
                            if (CollectionUtil.hasItem(validOfflineNodeList)) {
                                String nodeIdJsonForBle = buildNodeIdListJson(validOfflineNodeList, RoutingEntity.Type.BLE);
                                byte[] data = JsonDataBuilder.buildNodeLeaveEvent(nodeIdJsonForBle);
                                List<RoutingEntity> bleUser = new ArrayList<>();
                                bleUser.add(bleEntity);
                                // send bto ble users
                                passOfflineEntitiesToBLE(data, bleUser);

                            }
                        }
                    }


                }
            }

            if (!mIsStoppingMesh) {
                List<RoutingEntity> routingEntities = RouteManager.getInstance().
                        getConnectedDirectWifiUsers();
                if (CollectionUtil.hasItem(routingEntities)) {
                    if (routingEntities.size() < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                        if (mDriverManager != null) {
                            mDriverManager.requestGOScheduledBroadCast();
                        }
                    }
                } else {
                    initiateP2P();
                }
            }
        }

        @Override
        public void onReceiveCredentialFromBle(String ssid, String password, String userId) {
            wifiTransPort.prepareAndSendCredentialMessage(ssid, password, userId);
        }

        @Override
        public void onGetDisconnectedList(List<RoutingEntity> offlineList) {
            if (CollectionUtil.hasItem(offlineList)) {
                MeshLog.i("Subnet merge disconnected user send to UI");
                for (RoutingEntity entity : offlineList) {
                    linkStateListener.onUserDisconnected(entity.getAddress());
                }
            }
        }

        private void passOfflineEntitiesToWifiClients(String nodeIdJson) {

            List<RoutingEntity> liveWifiConnectionList = RouteManager.getInstance().getWifiUser();
            MeshLog.i(" LiveWifiConnectionList  -->" + liveWifiConnectionList.toString());
            if (CollectionUtil.hasItem(liveWifiConnectionList)) {
                for (RoutingEntity rEntity : liveWifiConnectionList) {
                    wifiTransPort.addDiscoveryTaskInQueue("Node leave from WIFI ", false, false, rEntity.getIp(),
                            () -> JsonDataBuilder.buildNodeLeaveEvent(nodeIdJson));
                }
            }
        }
    };

    private void passOfflineEntitiesToBLE(byte[] data, List<RoutingEntity> bleUsers) {
        if (bleTransport == null) return;

        MeshLog.i("[BLE_PROCESS] send the disconnected node to BLE user from TransportManagerX");
        for (RoutingEntity entity : bleUsers) {
            bleTransport.addDiscoveryTaskInQueue(false, entity.getAddress(), () -> data);
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

    public MeshXLCListener mMeshXLCListener = new MeshXLCListener() {

        @Override
        public void onConnectWithGO(String ssid, boolean isSpecialConnection) {
            MeshLog.e("[p2p_process] onConnectWithGO:" + ssid);
            if (!mIsStoppingMesh) {
                InetAddress inetAddress = WifiDetector.determineAddress(mContext);
                if (inetAddress != null && inetAddress.getHostAddress() != null && wifiTransPort != null) {
                    byte[] data = JsonDataBuilder.prepareP2pHelloPacketAsClient(myNodeId, inetAddress.getHostAddress(), RoutingEntity.Type.WiFi, isSpecialConnection);

                    wifiTransPort.sendMessageAndGetCallBack(WifiTransPort.P2P_MASTER_IP_ADDRESS, data, new MessageCallback() {
                        @Override
                        public void onMessageSend(boolean isSuccess) {
                            MeshLog.v("[p2p_process] hello packet send status :" + isSuccess);
                            if (isSuccess) {
                                if (isSpecialConnection) {
                                    setFileSendingMode(true);
                                }
                                mDriverManager.writeGoLcSchedulerState("HelloSend", CurrentRole.IDEAL);
                            } else {
                                mDriverManager.triggerWifiDisconnectionAndSearch("Hello not send");
                            }
                        }
                    });
                    //wifiTransPort.scanSubnet(inetAddress.getHostAddress(), WifiTransPort.P2P_MASTER_IP_ADDRESS);
                    //SharedPref.write(Constant.KEY_USER_IP, inetAddress.getHostAddress());

                } else {
                    mDriverManager.disconnectWifi();
                    MeshLog.e("NPE - inetAddress:" + inetAddress + " - inetAddress.getHostAddress():" +
                            inetAddress.getHostAddress() + "- wifiTransPort:" + wifiTransPort);
                }
            }
        }

        @Override
        public void onConnectWithGOBeingGO(boolean wasDisconnected) {
            if (!wasDisconnected) {
                mLinkStateListener.onInterruption(LinkStateListener.CONNECTED_WITH_GO_BEING_GO);
            }
        }

        @Override
        public void onConnectWithAdhocPeer(String ip, int port) {
            //TODO Exist node verification needed
            if (!TextUtils.isEmpty(ip)) {
                RoutingEntity routingEntity = RouteManager.getInstance().getNodeDetailsByIP(ip);
                if (routingEntity == null) {
                    startAdHocConnection(ip, port);
                } else {
                    MeshLog.i("Already Connected in ADHOC::" + routingEntity.toString());
                }
            }


        }

        private void removeConnections(List<RoutingEntity> offlineEntities) {
            if (CollectionUtil.hasItem(offlineEntities)) {

                List<RoutingEntity> buyersInternetOfflineUserList = new ArrayList<>();
                for (RoutingEntity entity1 : offlineEntities) {
                    if (entity1.getType() == RoutingEntity.Type.INTERNET) {
                        buyersInternetOfflineUserList.add(entity1);
                    }
                }
                offlineEntities.removeAll(buyersInternetOfflineUserList);


                List<RoutingEntity> validOfflineList = new ArrayList<>();
                for (RoutingEntity entity : offlineEntities) {
                    RoutingEntity validEntity = RouteManager.getInstance().getEntityByAddress(entity.getAddress());
                    if (validEntity == null || !validEntity.isOnline()) {
                        validOfflineList.add(entity);
                    }
                }

                if (!CollectionUtil.hasItem(validOfflineList)) return;
                // send to bt users
                String leaveNodeListForWifi = buildNodeIdListJson(validOfflineList, RoutingEntity.Type.WiFi);

                String leaveNodeListForBle = buildNodeIdListJson(validOfflineList, RoutingEntity.Type.BLE);

                byte[] data = JsonDataBuilder.buildNodeLeaveEvent(leaveNodeListForBle);

                List<RoutingEntity> adhocEntites = RouteManager.getInstance().getAdhocUser();

                MeshLog.i(" Leave node list send :: " + leaveNodeListForWifi);


                List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();
                MeshLog.i("[BLE_PROCESS] offline user notify to BLE user. User size " + bleUserList.size());

                for (RoutingEntity entity : bleUserList) {
                    bleTransport.sendMessage(entity.getAddress(), data);
                }


                if (CollectionUtil.hasItem(adhocEntites)) {
                    for (RoutingEntity rEntity : adhocEntites) {
                        /*BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(rEntity.getIp(),
                                () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                        mMessageDispatcher.addSendMessage(baseMeshMessage);*/
                        wifiTransPort.sendMeshMessage(rEntity.getIp(), JsonDataBuilder.buildNodeLeaveEvent(leaveNodeListForWifi));
                    }
                }

                MeshLog.v("[MeshX] offlineEntities:" + offlineEntities.toString());

                if (mLinkStateListener != null) {
                    for (RoutingEntity routingEntity : offlineEntities) {
                        // connectionLinkCache.removeNodeInfo(routingEntity.getAddress());
                        checkUserDisconnection(routingEntity.getAddress());
                    }
                }
            }

        }

        @Override
        public void onDisconnectWithGO(String disconnectedFrom) {
            MeshLog.e("[p2p_process] DEVICE DISCONNECTED FROM GO: " + disconnectedFrom);
            // remove from My end
            List<RoutingEntity> offlineEntities = RouteManager.getInstance().updateWiFiNodeAsOffline();

            removeConnections(offlineEntities);

            //As disconnected from WiFi network so we do not wait anymore for query
            //wifiTransPort.resetPeerQueryState();

            // first check have any nodes.If no node alive then we can restart BLE here
            boolean hasAnyNodeAlive = RouteManager.getInstance().getAllOnlineNodeIds().size() > 0;
            if (!hasAnyNodeAlive) {
                if (mDriverManager != null && !mDriverManager.isWifiConnecting()) {
                    MeshLog.i("[BLE_PROCESS] restart BLE from disconnect with GO when node alive");
                    mDriverManager.startBLE(false, "onDisconnectWithGO");
                }
            }
        }


        @Override
        public void onDisconnectedWithAdhoc(String address) {
            mWiFiEnabled = true;
            RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(address);
            if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.HB) {
                String disconnectedAddress = routingEntity.getAddress();
                if (Text.isNotEmpty(disconnectedAddress)) {
                    List<RoutingEntity> offlineEntities = RouteManager.getInstance().
                            updateNodeAsOffline(null, disconnectedAddress);

                    removeConnections(offlineEntities);
                }
            }

            // TODO: 11/20/2019
            //Reset peer query state and similar type mode switching might require for Adhoc
        }

        @Override
        public void onWifiUserDisconnected(List<RoutingEntity> entityList) {
            if (CollectionUtil.hasItem(entityList)) {
                for (RoutingEntity item : entityList) {
                    linkStateListener.onUserDisconnected(item.getAddress());
                }

                List<RoutingEntity> validOfflineNodes = new ArrayList<>();

                for (RoutingEntity entity : entityList) {
                    RoutingEntity validEntity = RouteManager.getInstance().getEntityByAddress(entity.getAddress());
                    if (validEntity == null || !validEntity.isOnline()) {
                        validOfflineNodes.add(entity);
                    }
                }

                List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();

                for (RoutingEntity bleEntity : bleUserList) {
                    List<RoutingEntity> allPossiblePath = RouteManager.getInstance().getAllPossibleOnlinePathById(bleEntity.getAddress());
                    if (allPossiblePath.size() > 1) {

                        if (CollectionUtil.hasItem(entityList)) {
                            String nodeIdJson = buildNodeIdListJson(entityList, RoutingEntity.Type.BLE);
                            byte[] data = JsonDataBuilder.buildNodeLeaveEvent(nodeIdJson);

                            List<RoutingEntity> bleUser = new ArrayList<>();
                            bleUser.add(bleEntity);
                            passOfflineEntitiesToBLE(data, bleUser);
                        }
                    } else {

                        if (!CollectionUtil.hasItem(validOfflineNodes)) return;

                        String nodeIdJson = buildNodeIdListJson(validOfflineNodes, RoutingEntity.Type.BLE);
                        byte[] data = JsonDataBuilder.buildNodeLeaveEvent(nodeIdJson);

                        List<RoutingEntity> bleUser = new ArrayList<>();
                        bleUser.add(bleEntity);
                        // send bto ble users
                        passOfflineEntitiesToBLE(data, bleUser);
                    }
                }

            }
        }

        @Override
        public void onConnectedWithTargetNode(String nodeId) {
            MeshLog.e("[p2p_process] ***connected with special node*** :" + AddressUtil.makeShortAddress(nodeId));
            if (mMessageListener != null) {
                // HandlerUtil.postBackground(() -> mMessageListener.onConnectedWithTargetNode(nodeId), 5000);
            }
        }

        @Override
        public void onGetLegacyUser(ConcurrentLinkedQueue<RoutingEntity> allWifiUserQueue) {
            // This section must have at least one user
            RoutingEntity entity = allWifiUserQueue.poll();
            if (entity != null && entity.isOnline()) {
                isLCAlive(entity.getAddress(), allWifiUserQueue);
            }
        }

        @Override
        public void onSendDisconnectMessageToBle(String address) {
            byte[] disconnectMsg = JsonDataBuilder.buildDisconnectMessage();
            bleTransport.sendConfirmationMessage(address, disconnectMsg, new MessageCallback() {
                @Override
                public void onBleMessageSend(String messageId, boolean isSuccess) {
                    if (isSuccess) {
                        RouteManager.getInstance().resetDb();
                    }
                }
            });
            //bleTransport.sendMessage(address, disconnectMsg);
        }

        @Override
        public void disconnectUserPassToUi(String userId) {
            linkStateListener.onUserDisconnected(userId);
        }
    };


    private void isLCAlive(String userId, ConcurrentLinkedQueue<RoutingEntity> allWifiUserQueue) {
        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(userId);
        if (routingEntity != null
                && (routingEntity.getType() == RoutingEntity.Type.HB
                || routingEntity.getType() == RoutingEntity.Type.WiFi)) {
            MeshLog.v("[P2P_Process] wifi ping send.");
            new Thread(new Pinger(routingEntity.getIp(), (ip, isReachable) -> {
                if (!isReachable && routingEntity.getType() == RoutingEntity.Type.HB) {

                    adhocTransport.onDirectUserDisconnect(routingEntity);

                } else if (!isReachable && routingEntity.getType() == RoutingEntity.Type.WiFi) {
                    MeshLog.e("[P2P_Process] Ping failed. node disconnected: ");
                    TransportManagerX.getInstance().mMeshXAPListener.onGODisconnectedWith(routingEntity.getIp());
                }

                RoutingEntity entity = allWifiUserQueue.poll();

                if (entity != null && entity.isOnline()) {
                    isLCAlive(entity.getAddress(), allWifiUserQueue);
                }
            }, 3)
            ).start();

        }
    }

    private void startAsP2P() {
        mDriverManager.startSearching(mWiFiAdapterStateListener);
    }


    @Override
    protected void startBTDiscovery() {
        super.startBTDiscovery();

        mBTManager.startDiscovery();

        MeshLog.v("Bt discover started from TransportManagerX");

       /* int existingMode = SharedPref.readInt(Constant.RANDOM_STATE);
        boolean isBTConnected = RouteManager.getInstance().isBtUserConnected();
        boolean isBTDiscovering = mBTManager.isDiscovering();
        if (!isBTConnected && !isBTDiscovering && existingMode == MODE_BT) {
            Timber.d("[File][Speed]Start BT Scan called, highBand:%s", mIsHighBandEnabled);
            if (!mIsHighBandEnabled) {
                MeshLog.v("[DC-Issue]Triggered BLE device search");
                mBTManager.startDiscovery();
            }
        } else {
            if (existingMode == MODE_ADHOC) {
                mBTManager.btServerEnableIfDisable();
            } else {
                MeshLog.w("Could not start BT search. existingMode:" +
                        (existingMode > 0 && existingMode <= modeString.length ?
                                modeString[existingMode - 1] : "null") +
                        "-isBTConnected:" + isBTConnected + "-isBTDiscovering:" + isBTDiscovering);
            }

        }*/
    }

    /**
     * Cancel BT discovery, clear discovered BT device queue, close any socket which is not at
     * connected state
     */
    private void stopBtDiscovery() {
        MeshLog.v("[DC-Issue]stop BT");
        if (mBTManager != null) {
            boolean isCancelled = mBTManager.cancelDiscovery();
            MeshLog.v(" Stopped BLE device discovery:" + isCancelled);
        }
        super.stopBTDiscovery();
    }


    private void startAdHocDiscovery() {
        if (adhocTransport != null) {
            adhocTransport.startAdHocDiscovery();
        }
    }

    private void stopAdhocDiscovery() {
        if (adhocTransport != null) {
            adhocTransport.stopAdHocDiscovery();
        }
    }

    private void stopP2pDiscovery() {
        MeshLog.v("[DC-Issue]stop P2P");
        if (mDriverManager != null) {
            //If WiFi forced stop we want the state to be retained as long the session continue
            if (!mDriverManager.mHasForcedOff()) {
                MeshLog.v(" Stopped mWiFiDirectManagerLegacy");
                //mDriverManager.stopWifiLegacy();
            }
        }

    }


    public int getExistingRole() {
        return SharedPref.readInt(Constant.RANDOM_STATE);
    }


    public RemoteTransport getRemoteTransport() {
        return remoteTransport;
    }

    private Executor newSerialExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("message");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    public void restart() {
        super.restart();
        mBTManager.mBTStateListener = mBTStateListener;
    }

    @Override
    public void onMessageSend(int messageId, String ipAddress, boolean messageStatus) {
        MeshLog.i("Message send status X =" + messageStatus + " ip: " + ipAddress);
        if (!messageStatus) {
            mMeshXAPListener.onGODisconnectedWith(ipAddress);
        }
        /*int wifiUserCount = RouteManager.getInstance().getWifiUser().size();
        RoutingEntity routingEntity = RouteManager.getInstance().getWiFiEntityByIp(ipAddress);
        if (!messageStatus) {
            RouteManager.getInstance().makeUserOffline(routingEntity);
            if (ipAddress.equals(WifiTransPort.P2P_MASTER_IP_ADDRESS)) {
                if (wifiUserCount > 1) {

                } else {
                }
            } else {
            }
            linkStateListener.onUserDisconnected(routingEntity.getAddress());
        }*/
        /*if (wifiTransPort != null && wifiTransPort.isQueryingPeer() &&
                wifiTransPort.mQueryingPeerMessageId == messageId) {
            // Not is stopping mode and
            // either message sending was FAILED or my role is GO
            boolean isMeGo = false;
            if ((!mIsStoppingMesh && (!messageStatus || (isMeGo = P2PUtil.isMeGO())))) {
                // For GO we do not wait for any further response from client
                if (isMeGo) {
                    wifiTransPort.resetPeerQueryState();
                }
                // As GO got response for it's sending response so resuming Adhoc discovery
                // Or if failed message then resume Adhoc discovery

            } // else we wait for GO to send response
        } else if (adhocTransport != null && adhocTransport.isQueryingPeer() &&
                adhocTransport.mQueryingPeerMessageId == messageId) {

            if (!mIsStoppingMesh) {
                if (!messageStatus) {
                    //For WiFi and Adhoc client we expect this to reset upon response from Master.
                    //This approach is properly effective for P2P but less in Adhoc the way as we
                    // are now manipulating it
                    adhocTransport.resetPeerQueryState();
                }

            }

        } else if (bluetoothTransport != null && bluetoothTransport.isQueryingPeer() && bluetoothTransport.mQueryingPeerMessageId == messageId) {
            //If hello message failed then resume state
            if (!mIsStoppingMesh && !messageStatus) {
                bluetoothTransport.resetPeerQueryState();
                if (mDriverManager != null) {
                    //mDriverManager.resumeWifiConnectivity();
                }
                if (adhocTransport != null) {
                    //adhocTransport.resumeDiscovery();
                }
            }
        } else {
            super.onMessageSend(messageId,ipAddress, messageStatus);
        }*/
    }

    @Override
    public void onScanFinished() {
        MeshLog.i("Bt scan finished. Restarting scan");
        // We call this to enable multiple scan in a single BT scheduling
        if(!bleTransport.isBtConnected()) {
            startBTDiscovery();
        }
    }

    public BluetoothTransport getBlueToothTransport() {
        if (bluetoothTransport != null) {
            return bluetoothTransport;
        }
        return null;
    }

    public AdHocTransport getAdHocTransport() {
        if (adhocTransport != null) {
            return adhocTransport;
        }
        return null;
    }


    private void startAdHocConnection(String ip, int port) {

        if (!mIsStoppingMesh) {
            // Stop BT if no BT connection available
            if (!RouteManager.getInstance().isBtUserConnected() &&
                    DispatcherHelper.getDispatcherHelper().isNotBTConnecting()) {//No BT connected
                MeshLog.i(" BT SOFT diasble called from startAdHocConnection");
                BTManager.getInstance(App.getContext()).softDisable();
            }


            InetAddress inetAddress = WifiDetector.determineAddress(mContext);
            if (inetAddress != null && inetAddress.getHostAddress() != null && wifiTransPort != null) {
                adhocTransport.sendMyInfo(inetAddress.getHostAddress(), ip);

                SharedPref.write(Constant.KEY_USER_IP, inetAddress.getHostAddress());
            } else {
                MeshLog.e("NPE - inetAddress:" + inetAddress + " - inetAddress.getHostAddress():" +
                        inetAddress.getHostAddress() + "- adhocTransport:" + adhocTransport);
            }
        }
    }


    public String getMyNodeId() {
        return myNodeId;
    }

    public String getUserPublicKey(String address) throws RemoteException {

        try {

            String pubKey = DatabaseService.getInstance(mContext).getPeersPublicKey(address);

            if (TextUtils.isEmpty(pubKey)) {
                pubKey = DatabaseService.getInstance(mContext).getPublicKeyById(address);
            }

            return pubKey;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public PeersEntity getUserNameByAddress(String address, String appTokenName) {

        try {
            PeersEntity peersEntity = DatabaseService.getInstance(mContext).getPeersById(address, appTokenName);

            if (peersEntity == null) {
                peersEntity = DatabaseService.getInstance(mContext).getFirstPeersById(address);
            }

            return peersEntity;

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
        //TODO need to give a solution
    }


    public void processForceConnection(String receiverId) {
        MeshLog.v("[msg_process] process force connection called");

        String ssid = "";
        String password = "";
        boolean isNeedToSearch = false;
        if (P2PUtil.isMeGO() && !RouteManager.getInstance().getWifiUser().isEmpty()) {
            //int wifiUserCount = RouteManager.getInstance().getWifiUser().size();
            //if (wifiUserCount > 1 && wifiUserCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
            ssid = WifiCredential.ssid;
            password = WifiCredential.password;

            //Set target nodeid in wifitransport to check expceted node
            wifiTransPort.setSpecificNodeIdWhoWantsToConnect(receiverId);
            mDriverManager.requestGOScheduledBroadCast();
            /*} else {
                isNeedToSearch = true;
            }*/
        } else {
            isNeedToSearch = true;
        }

        setFileSendingMode(true);

        byte[] data = JsonDataBuilder.prepareForceConnectionMessage(myNodeId, receiverId,
                ssid, password, true, false);

        RoutingEntity routingEntity = RouteManager.getInstance()
                .getNextNodeEntityByReceiverAddress("", receiverId, RoutingEntity.Type.WiFi);


        setFileReceiverId(receiverId);

        fileUserId = receiverId;
        startForceConnectionTracker(true);

        if (routingEntity == null) {
            if (isNeedToSearch) {
                //Todo We have to take decision upon current queue that we have any pending file
                // that not need force connection. May be we have to handle Busy state also
                mDriverManager.startSpecialSearch(receiverId);
            }
            mDriverManager.startSpecificBLESearch(receiverId);
            return;
        }

        sendMessageByType(data, routingEntity);
    }

    /**
     * This method is used for sending file free mode message
     * That the receiver now able to or ready to receive file
     */
    public void sendFileFreeModeMessage() {
        MeshLog.v("[file_process] sending file free mode message");

        String receiverId = getFileRequestSenderWhenBusy();

        if (receiverId == null || TextUtils.isEmpty(receiverId)) {
            return;
        }

        // Remove the request sender from map
        fileRequestSenderMap.remove(receiverId);

        RoutingEntity routingEntity = RouteManager.getInstance()
                .getNextNodeEntityByReceiverAddress("",
                        receiverId, RoutingEntity.Type.WiFi);

        if (routingEntity == null || !routingEntity.isOnline()) {

            // User not connected we will proceed further
            if (hasAnyFileRequest()) {
                MeshLog.v("[file_process] sending file free mode message 2");
                sendFileFreeModeMessage();
            }

        } else {

            byte[] data = JsonDataBuilder.prepareFreeMessageForRequestNode(myNodeId, receiverId, true);
            sendMessageByType(data, routingEntity);
        }

    }

    /**
     * This method will communicate with file mesh manager
     * for execute next file queue.
     * This method will call from when a node get busy message from other
     */
    public void executeNextFileItemInQueue(String userId) {
        if (mMessageListener != null) {
            mMessageListener.onGetFileFileUserNotFound(userId, true);
        }
    }


    private void sendMessageByType(byte[] data, RoutingEntity routingEntity) {
        if (routingEntity.getType() == RoutingEntity.Type.WiFi
                && !TextUtils.isEmpty(routingEntity.getIp())) {
            wifiTransPort.addDiscoveryTaskInQueue("processForceConnection", false,
                    true, routingEntity.getIp(), () -> data);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB
                && !TextUtils.isEmpty(routingEntity.getIp())) {
            adhocTransport.sendAdhocMessage(routingEntity.getIp(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
            bleTransport.addDiscoveryTaskInQueue(true,
                    routingEntity.getAddress(), () -> data);
        } else {
            MeshLog.e("[FILE_PROCESS] the user not exists!!!");
        }
    }

    private void startForceConnectionTracker(boolean isStart) {
        if (isStart) {
            HandlerUtil.postBackground(forceConnectionTracker, FORCE_CONNECTION_TRACKER_TIME);
        } else {
            HandlerUtil.removeBackground(forceConnectionTracker);
        }
    }

    /**
     * This is timeout tracking for WiFi/BLE connection for both file sender and receiver.
     * Note: The time limit is 2 minutes. If the user not connected by
     * either BLE or wifi after two minutes it will execute next file queue if available.
     * And remove the current one
     */
    private final Runnable forceConnectionTracker = () -> {

        RoutingEntity routingEntity = RouteManager.getInstance().getEntityByAddress(fileUserId);
        if (routingEntity != null && routingEntity.isOnline()) {
            MeshLog.v("[FILE_PROCESS] We are connected the desire node");
        } else {
            setFileSendingMode(false);

            // The current user cannot connect (p2p) with target node so their current mode
            // need to be reset. And they will execute next map by informing meshFile module
            // by using onGetFileFileUserNotFound() method

            // So here a receiver only can receive one file. No file in in progress state at this moment

            setFileReceivingMode(false);

            wifiTransPort.setSpecificNodeIdWhoWantsToConnect(null);

            boolean isBleUserAvailable = RouteManager.getInstance().getBleUsers().size() > 0;
            if (!isBleUserAvailable) {
                mDriverManager.startBLE(false, "Force connection tracker");
            }


            if (mMessageListener != null) {
                mMessageListener.onGetFileFileUserNotFound(fileUserId, false);
            }
        }
    };

    public void setSpecificNodeIdWhoWantsToConnect(String receiverId) {
        wifiTransPort.setSpecificNodeIdWhoWantsToConnect(receiverId);
    }

    /**
     * <p>Pass the role switch info to all connected users</p>
     *
     * @param newRole (required ) : Integer
     */
    public void sendNewRoleToLocalConnectedUser(int newRole) {
        int existUserMode = PreferencesHelper.on().getDataShareMode();

        // if new role internet then we will restart mesh
        // And if existing role is Internet then we will restart mesh again
        if (existUserMode == PreferencesHelper.INTERNET_USER || newRole == PreferencesHelper.INTERNET_USER) {
            //stopTransport();
            //startTransport();

            stopMesh();

            PreferencesHelper.on().setDataShareMode(newRole);

            startMesh();
        }

        PreferencesHelper.on().setDataShareMode(newRole);

        List<RoutingEntity> routingEntities = RouteManager.getInstance().getAllConnectedNodes();
        if (routingEntities == null || routingEntities.isEmpty()) {
            returnRoleSetSuccess();
            return;
        }


        String myNodeId = SharedPref.read(Constant.KEY_USER_ID);


/*
        NOTE: THis code is no longer needed, task transferred to class dataplanmanager after calling this function.
        //Remove internet user if I am not direct (my own internet) user
        //TODO call this method only when I'm buyer and have no internet connection.
        makeInternetUsersOffline(null);
*/

        for (RoutingEntity receiverEntity : routingEntities) {
            if (receiverEntity.getType() != RoutingEntity.Type.INTERNET) {
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverEntity.getAddress());

                if (routingEntity == null) continue;

                byte[] roleChangeMessage = JsonDataBuilder.buildUserRoleSwitchMessage(myNodeId, receiverEntity.getAddress(), newRole);

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("[p2p_process] Send my role to other via WIFI");
                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), roleChangeMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    MeshLog.v("[p2p_process] Send my role to other via BLE");
                    bleTransport.sendMessage(routingEntity.getAddress(), roleChangeMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        mMessageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, roleChangeMessage));
                    }
                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    adhocTransport.sendAdhocMessage(routingEntity.getIp(), roleChangeMessage);
                }
            }
        }
        returnRoleSetSuccess();

    }

    // This method only test purpose
    public void disconnectBle() {
        List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();
        if (bleUserList != null && !bleUserList.isEmpty()) {
            RoutingEntity entity = bleUserList.get(0);
            mDriverManager.removeBleConnectionById(entity.getAddress());
        }
    }

}


