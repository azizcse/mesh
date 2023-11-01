package com.w3engineers.mesh;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.util.CollectionUtils;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.hardwareoff.HardwareStateManager;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.ble.BleTransport;
import com.w3engineers.mesh.ble.message.BleMessageHelper;
import com.w3engineers.mesh.bluetooth.BTManager;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.controller.DriverManager;
import com.w3engineers.mesh.datasharing.database.DatabaseService;
import com.w3engineers.mesh.datasharing.database.message.Message;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.wifid.Pinger;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.model.PendingMessage;
import com.w3engineers.mesh.premission.LocationProviderStateBroadcastReceiver;
import com.w3engineers.mesh.premission.MeshSystemRequestActivity;
import com.w3engineers.mesh.premission.PermissionHelper;
import com.w3engineers.mesh.queue.MeshMessageListener;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.MessageListener;
import com.w3engineers.mesh.queue.messages.BTMessage;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.tunnel.RemoteTransport;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;
import com.w3engineers.mesh.wifiap.BluetoothDiscoveryReceiver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * <h1>Mesh manager</h1>
 * <p>
 * This class is responsible to start and manage mesh connectivity
 * <b>WifiTransPort :</b> Responsible for wifi connectivity and communication {@link WifiTransPort}
 * <b>BluetoothTransport :</b> Responsible for bluetooth connectivity and communication {@link BluetoothTransport}
 * </p>
 * <h1>Connectivity check</h1>
 *
 * <p>
 * Manage broadcast receiver for wifi, wifip2p and bluetooth
 * Connectivity status check and make decision to change device mode
 * </p>
 *
 * <p>
 * Device mode (Master, Client) change based on autonomous algorithm
 * </p>
 *
 * <h1>Connect device wifi, p2p and ble</h1>
 *
 * <p>
 * Established Wifi, p2p, ble connection
 * </p>
 */
public abstract class TransportManager implements BluetoothDiscoveryReceiver.BTDiscoveryListener, MeshMessageListener {

    protected volatile String myNodeId;
    public final String[] modeString = new String[]{"MODE_CLIENT", "MODE_MASTER", "MODE_BT",
            "MODE_ADHOC", "MODE_P2P"};
    protected Context mContext;
    WifiManager wifiManager;
    protected BluetoothTransport bluetoothTransport;
    protected WifiTransPort wifiTransPort;
    protected RemoteTransport remoteTransport;
    protected AdHocTransport adhocTransport;
    protected BleTransport bleTransport;

    private String bleName;
    protected ConnectionLinkCache connectionLinkCache;
    LinkStateListener mLinkStateListener;
    protected BTManager mBTManager;
    LinkStateListener paymentLinkStateListener;

    protected volatile boolean isFileSendingMode;
    protected volatile boolean isFileReceivingMode;
    private Map<String, Message> waitingMessageMap = new ConcurrentHashMap();


    protected DriverManager mDriverManager;

    public void setMessageListener(MessageListener messageListener) {
        mMessageListener = messageListener;
    }

    protected MessageListener mMessageListener;

    private volatile Queue<BluetoothDevice> mBluetoothDevices;

    private volatile boolean isBluetoothConnecting;

    //    private String currentSellerId;
    private LocationProviderStateBroadcastReceiver mLocationProviderStateBroadcastReceiver =
            new LocationProviderStateBroadcastReceiver(
                    new LocationProviderStateBroadcastReceiver.LocationProviderStateListener() {
                        @Override
                        public void onDisabled() {
                            if (linkStateListener != null) {
                                linkStateListener.onInterruption(LinkStateListener.LOCATION_PROVIDER_OFF);
                            }
                        }

                        @Override
                        public void onEnabled() {

                        }
                    });


    private DatabaseService databaseService;
    protected MessageDispatcher mMessageDispatcher;

    /**
     * To handle some redundant use cases
     */
    public volatile boolean isBtEnabled = false;
    public PermissionHelper mPermissionHelper = new PermissionHelper();

    private int APP_PORT;
    public ForwardListener mForwardListener;

    protected abstract void startTransport();

    protected abstract void stopTransport();

    protected abstract void onHelloPacketSend(String ip, boolean isSuccess);

    /**
     * <p>Private constructor</p>
     * <p>Initialize pre compulsory component</p>
     *
     * @param context : {@link Context}
     */
    protected TransportManager(Context context, int appPort, String address, String publicKey,
                               String networkPrefix, String multiverseUrl, LinkStateListener linkStateListener) {
        mContext = context;
        APP_PORT = appPort;
        MeshLog.v("Addr :" + address + " ssid: " + networkPrefix + " port: " + appPort);
        SharedPref.write(Constant.KEY_USER_ID, address);
        SharedPref.write(Constant.KEY_PUBLIC_KEY, publicKey);
        mBTManager = BTManager.getInstance(mContext);
        mLinkStateListener = linkStateListener;
        MeshLog.initListener(linkStateListener);
        prepareNetworkPrefixAndSave(networkPrefix);

        SharedPref.write(Constant.KEY_MULTIVERSE_URL, multiverseUrl);

        mBTManager.registerDiscReceiver(this);

        databaseService = DatabaseService.getInstance(mContext);


        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        mContext.registerReceiver(mLocationProviderStateBroadcastReceiver, filter);
    }


    public void saveUserInfo(String userInfo) {
        if (TextUtils.isEmpty(userInfo)) {
            return;
        }
        GsonUtil.setUserInfo(userInfo);
    }

    private void prepareNetworkPrefixAndSave(String networkPrefix) {


        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (TextUtils.isEmpty(networkPrefix)) {
            throw new NullPointerException("Network prefix can't be null or empty");
        } else if (networkPrefix.length() < 3) {
            throw new IllegalArgumentException("Network prefix can't be smaller than 3");
        } else if (networkPrefix.contains(".")) {
            throw new IllegalArgumentException("Network prefix can't contain dot(.)");
        }

        if (networkPrefix.length() > 3) {
            networkPrefix = networkPrefix.substring(0, 2);
        }

        networkPrefix = networkPrefix.toLowerCase();

        String btName = networkPrefix;

        SharedPref.write(Constant.NETWORK_SSID, networkPrefix);

        String p2pPrefix = networkPrefix + ".m";
        SharedPref.write(Constant.KEY_NETWORK_PREFIX, p2pPrefix);

        //String userName = SharedPref.read(Constant.KEY_USER_NAME);

        // for ble name we will concat last 3 digit of user eth address

        String myUserId = SharedPref.read(Constant.KEY_USER_ID);

        bleName = btName + "-" + (TextUtils.isEmpty(myUserId) ? "" : AddressUtil.makeShortAddress(myUserId) + "-") +
                getRandomString();
        MeshLog.i("Final BT classic name: " + bleName);

        SharedPref.write(Constant.KEY_DEVICE_BLE_NAME, bleName);
    }


    private String getRandomString() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(uuid.length() - 5);
    }

    /*
     *//**
     * <p>
     * Create Thread safe single object
     * Protect multiple object if multiple thread attempt to create
     * object only one thread can able to create object
     * </p>
     *
     * @param context : application context
     * @return : {@link TransportManager} object
     *//*
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
    }*/


    /**
     * <h1>Mesh config</h1>
     * Configures combine {@link CombineTransport} that supports communication through given protocols.
     * It must be started via {@link MeshTransport#start()} before use.
     * <p>Purpose to config mesh and start device search</p>
     *
     * @param appId  : int application port
     * @param nodeId : String eth node id
     */
    abstract public void configTransport(String nodeId, String publicKey, int appId);

    /**
     * Start mesh with manual or autonomous setting
     */
    public void initMeshProcess() {
        MeshLog.v("[p2p_process] initMeshProcess called");

        String walletAddress = SharedPref.read(Constant.KEY_USER_ID);
        String publicKey = SharedPref.read(Constant.KEY_PUBLIC_KEY);
        MeshLog.i(" Mesh library started successfully");

        configTransport(walletAddress, publicKey, APP_PORT);

        mLinkStateListener.onTransportInit(walletAddress, publicKey, TransportState.SUCCESS, "Success");
        if (paymentLinkStateListener != null) {
            paymentLinkStateListener.onTransportInit(walletAddress, publicKey, TransportState.SUCCESS, "Success");
        }
    }

    // Bluetooth renaming case
    protected void startBTDiscovery() {
        MeshLog.i("startBTDiscovery");
        if (mBluetoothDevices == null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                mBluetoothDevices = new PriorityQueue<>((o1, o2) -> {

                    String NETWORK_SSID = SharedPref.read(Constant.NETWORK_SSID);

                    int value;
                    String btName;

                    boolean o1StartsWith = false;

                    if (o1 != null) {
                        btName = o1.getName();
                        o1StartsWith = Text.isNotEmpty(btName) &&
                                btName.startsWith(NETWORK_SSID);
                    }

                    boolean o2StartsWith = false;

                    if (o2 != null) {
                        btName = o2.getName();
                        o2StartsWith = Text.isNotEmpty(btName) &&
                                btName.startsWith(NETWORK_SSID);
                    }

                    value = o1StartsWith && !o2StartsWith ? -1 :
                            (!o1StartsWith && o2StartsWith ? 1 : 0);

                    return value;
                });
            } else {
                mBluetoothDevices = new LinkedList<>();
            }
        }
    }

    protected void stopBTDiscovery() {
        /*MeshLog.i("stopBTDiscovery");
        mBluetoothDevices = null;
        if (bluetoothTransport != null) {
            bluetoothTransport.stopConnectionProcess();
        }*/
    }

    /**
     * Stop mesh user discovery
     */
    public void stopMesh() {

        /*stopBTDiscovery();

        if (mBTManager != null) {
            mBTManager.destroy();
        }*/
    }

    public void stopHardWare() {

        HardwareStateManager hardwareStateManager = new HardwareStateManager();
        hardwareStateManager.init(mContext);
        MeshLog.i(" Mesh library started successfully 3");
        hardwareStateManager.disableAll(() -> {
            MeshLog.i(" Every hardware off");
            MeshLog.v("[WIFI]Requested to turn off. Disabling done.");
        });

    }

    public void requestPermission() {

        requestPermission(null);
    }

    public void requestPermission(List<String> permissions) {

        startPermissionActivity(permissions);
    }

    private void startPermissionActivity(List<String> permissions) {

        MeshLog.v("startPermissionActivity");
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MeshSystemRequestActivity.PERMISSION_REQUEST,
                (ArrayList<String>) permissions);
        intent.setClass(mContext, MeshSystemRequestActivity.class);
        intent.setAction(MeshSystemRequestActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

    }

  /*  public void restart(int newRole) {
        MeshLog.i("restart");
        MeshLog.e("sellerMode tm " + newRole);
        PreferencesHelper.on().setDataShareMode(newRole);
        if (connectionLinkCache != null) {
            connectionLinkCache.clearAllLinks();
        }
        stopMesh();
//        prepareNetworkPrefixAndSave(networkPrefix);
        if (newRole != PreferencesHelper.MESH_STOP) {
            initMeshProcess();
        } else {
            stopHardWare();
        }
    }*/


    public void restart() {
        if (connectionLinkCache != null) {
            connectionLinkCache.clearAllLinks();
        }
        stopMesh();
        initMeshProcess();
        mBTManager = BTManager.getInstance(mContext);
        mBTManager.registerDiscReceiver(this);
    }

    @Override
    public void onBluetoothFound(List<BluetoothDevice> bluetoothDevices) {
        if (!CollectionUtil.hasItem(bluetoothDevices)) {
            MeshLog.e("BT deice ");
            return;
        }

        if (bleTransport != null) {

            List<BluetoothDevice> toRemove = new ArrayList<>();
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                if (connectionLinkCache.isBtNameExistInConnectedSet(bluetoothDevice.getName())) {
                    MeshLog.i(" BT device is already connected -> " + connectionLinkCache.getConnectedBtSet());
                    toRemove.add(bluetoothDevice);
                }
            }
            bluetoothDevices.removeAll(toRemove);

        }


        MeshLog.e("[BT-Classic] Found BT device list --> : " + bluetoothDevices.size());

        if (mBluetoothDevices == null) {
            mBluetoothDevices = new LinkedList<>();
        }

        if (mBluetoothDevices != null) {
            mBluetoothDevices.addAll(bluetoothDevices);
        }

        MeshLog.e("[BT-Classic] Found BT device list: " + mBluetoothDevices.size());

        createBluetoothConnection();

    }

    private void createBluetoothConnection() {
        MeshLog.e("Create BluetoothConnection called:: queue size :: " +
                (CollectionUtil.hasItem(mBluetoothDevices) ? mBluetoothDevices.size() : "null"));

        // boolean isBtUserConnected = RouteManager.getInstance().isBtUserConnected();

        int existingMode = SharedPref.readInt(Constant.RANDOM_STATE);
        //if (!isBluetoothConnecting && !isBtUserConnected && (existingMode == MODE_BT || existingMode == MODE_ADHOC)) {
        if (!isBluetoothConnecting) {
            MeshLog.e("[BT-Classic] Found BT device list before connection: " + mBluetoothDevices.size());
            if (mBluetoothDevices != null && mBluetoothDevices.size() > 0) {
                BluetoothDevice bluetoothDevice = mBluetoothDevices.poll();
                assert bluetoothDevice != null;
                String btName = bluetoothDevice.getName();
                String allNames = AndroidUtil.getBTNames(Build.VERSION.SDK_INT >
                        Build.VERSION_CODES.M ? new PriorityQueue<>(mBluetoothDevices) :
                        new LinkedList<>(mBluetoothDevices));

                MeshLog.i("[BT] picked:" + btName + "-total:" + allNames);
                if (bleTransport != null) {
                    if (BluetoothTransport.SECONDARY_BT_NAME.equals(bluetoothDevice.getName())) {
                        //From our experience we saw during scanning time name can change dynamically
                        createBluetoothConnection();
                    } else {
                        MeshLog.e("BT connecting with:" + bluetoothDevice.getName() + " - mac:" +
                                bluetoothDevice.getAddress());
                        setBtConnecting(true);
                        if (mDriverManager != null) {
                            //mDriverManager.pauseWifiConnectivity();
                        }
                        if (adhocTransport != null) {
                            //adhocTransport.pauseDiscovery();
                        }

                        MeshLog.e("Bluetooth-dis", "attempt to bluetooth connect: " + bluetoothDevice.getName());

                        //mLinkStateListener.onClientBtMsgSocketConnected(bluetoothDevice);

                        bleTransport.connectBleDevice(bluetoothDevice, (messageId, name) -> {
                            isBluetoothConnecting = false;
                            MeshLog.e("Bluetooth Connection Result: " + (messageId == BaseMeshMessage.DEFAULT_MESSAGE_ID));
                            if (messageId == BaseMeshMessage.DEFAULT_MESSAGE_ID) {
                                createBluetoothConnection();
                            } else {
                                // bluetoothTransport.mQueryingPeerMessageId = messageId;
                            }
                        });
                    }
                }

            } else {
                //If failed to achieve connectivity then resume WiFi side
                if (mDriverManager != null) {
                    //mDriverManager.resumeWifiConnectivity();
                }
                //If failed to achieve connectivity then resume adhoc side
                if (adhocTransport != null) {
                    //adhocTransport.resumeDiscovery();
                }
                if (!bleTransport.isBtConnected()) {
                    //We are forcing BT restart
                    startBTDiscovery();
                }
            }
        } else {
            //Log.e("Bluetooth-dis", "Condition not satisfied to attempt connect");
            MeshLog.i("[BT] Could not start bt connection. isBluetoothConnecting:" +
                    isBluetoothConnecting + "-isBtUserConnected:" + "-existingMode:"
                    + (existingMode > 0 && existingMode <= modeString.length ?
                    modeString[existingMode - 1] : "null"));
        }

    }

    File createCrashReportPath() {
        Constant.Directory directoryContainer =  new Constant.Directory();
        String sdCard = directoryContainer.getParentDirectory() + Constant.Directory.MESH_SDK_CRUSH;
        File directory = new File(sdCard);
        if (!directory.exists()) {
            //Todo we have to check for android 11
            directory.mkdirs();
        }
        return directory;
    }


    public List<String> getWifiLinkIds() {
        if (connectionLinkCache == null) return null;
        return connectionLinkCache.getDirectWifiNodeIds();
    }


    public List<String> getBleLinkIds() {
        return connectionLinkCache.getDirectBleNodeIds();
    }


    public void sendPayMessage(String receiverId, String message, String messageId) {

        MeshLog.p("sendPayMessage receiver " + receiverId + "message " + message);
        String myUserId = SharedPref.read(Constant.KEY_USER_ID);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);
        if (routingEntity != null) {
            MeshLog.v("(P) RoutingEntity" + routingEntity.toString());
            byte[] msgBody = JsonDataBuilder.buildPayMessage(myUserId, receiverId, messageId, message.getBytes());

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                MeshLog.v("(P) sendMessage Wifi user");
                wifiTransPort.sendAppMessage(routingEntity.getIp(), msgBody);
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                bleTransport.sendAppMessage(routingEntity.getAddress(), msgBody);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                MeshLog.v("(P) sendMessage adhoc user");
                adhocTransport.sendPayMessage(routingEntity.getIp(), msgBody);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {

                BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    MeshLog.v("(P) sendMessage ble user");
                    BTMessage btMessage = MessageBuilder.buildMeshBtDiscoveryMessage(bleLink, () -> msgBody);
                    mMessageDispatcher.addSendMessage(btMessage);
                } else {
                    MeshLog.v("(P) BLE LINK NOT FOUND");
                }
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                bleTransport.sendAppMessage(routingEntity.getAddress(), msgBody);
            }
        } else {
            MeshLog.v(" (P) sendMessage User does not exist in routing table");
        }
    }

    public void sendHandshakeInfo(HandshakeInfo handshakeInfo, boolean isAllowInternet) {
        String receiverId = handshakeInfo.getReceiverId();
//        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiverId, 0);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);
        if (routingEntity == null) {

            MeshLog.e("Routing Entity NULL in Sending HS Message");
            RoutingEntity receiverAddress = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);
            if (receiverAddress == null) {
                linkStateListener.onUserDisconnected(receiverId);
            }

        } else {
            if (!isAllowInternet && routingEntity.getType() == RoutingEntity.Type.INTERNET) return;

            String handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);

            Log.v("PING_PROCESS - ", "Data: " + handshakeInfoText);

            byte[] messageData = handshakeInfoText.getBytes();

            if (RouteManager.getInstance().getLinkTypeById(receiverId) == RoutingEntity.Type.INTERNET) {

                if (remoteTransport != null && RouteManager.getInstance().isDirectlyConnected(routingEntity.getAddress())) {
                    remoteTransport.sendAppMessage(routingEntity.getAddress(), messageData);
                } else {
                    if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_BUYER) {

                        handshakeInfo.setReceiverId(routingEntity.getAddress());

                        handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);
                        messageData = handshakeInfoText.getBytes();

                        handshakeLocalRouting(routingEntity.getAddress(), messageData);
                    } else {
                        MeshLog.v("Internet user not found");
                    }
                }

            } else {
                handshakeLocalRouting(receiverId, messageData);
            }
        }
    }

    private void handshakeLocalRouting(String receiverId, byte[] messageData) {
        RoutingEntity routingEntity;
        routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);

        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            MeshLog.v("PING_PROCESS (32) Wifi: " + receiverId);
            MeshLog.v("HS Message Send Wifi Receiver : " + receiverId);
            RoutingEntity realEntity = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);
            if (realEntity != null) {
                wifiTransPort.sendAppMessage(routingEntity.getIp(), messageData);
            }

        } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
            MeshLog.v("PING_PROCESS (32) BLE: " + receiverId);
            MeshLog.v("HS Message Send BLE Receiver : " + receiverId);
            bleTransport.sendAppMessage(routingEntity.getAddress(), messageData);

        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            MeshLog.v("PING_PROCESS (32) BT: " + receiverId);
            MeshLog.v("HS Message Send BT Receiver : " + receiverId);
            BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                mMessageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, messageData));
            } else {
                MeshLog.v("(!) BT LINK NOT FOUND");
            }

        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            MeshLog.v("PING_PROCESS (32) HB: " + receiverId);
            MeshLog.v("HS Message Send AdHoc Receiver : " + receiverId);
            RoutingEntity realEntity = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);
            if (realEntity != null) {
                adhocTransport.sendAdhocMessage(routingEntity.getIp(), messageData);
            }

        } else {
            MeshLog.v("PING_PROCESS (32) Null: " + receiverId);
            MeshLog.v("TM User not found to send the local HS message");
        }
    }

    /**
     * Send message to specified receiver if online. This API is unreliable, means it never retries
     * to send message if failed upon certain attempt.
     *
     * @param receiverAddress receiverAddress
     * @param message         data
     * @return identifier of the message with which message status is reported through
     * {@link MeshMessageListener#(int, boolean)}
     */
    public int sendMessage(String receiverAddress, byte[] message) {

        int messageId = BaseMeshMessage.MESSAGE_STATUS_FAILED;

        if (message != null && AddressUtil.isValidEthAddress(receiverAddress)) {
            //pick immediate destination
            RoutingEntity routingEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(receiverAddress);

            if (routingEntity != null) {

                MeshLog.v("Selected route: " + routingEntity.toString());

                switch (routingEntity.getType()) {

                    /*case RoutingEntity.Type.BT:
                        BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildMeshBtMessage(
                                bleLink, message);
                        MeshLog.e("FileMessageTest", "message sent");
                        messageId = mMessageDispatcher.addSendMessage(baseMeshMessage);
                        break;*/

                    case RoutingEntity.Type.WiFi:
                        messageId = wifiTransPort.sendAppMessage(routingEntity.getIp(), message);
                        break;

                    case RoutingEntity.Type.HB:
                        messageId = adhocTransport.sendAdhocMessage(routingEntity.getIp(), message);
                        break;
                    case RoutingEntity.Type.BLE:

                        /*boolean isSuccess = bluetoothTransport.sendMeshMessage(receiverAddress, message);
                        if (!isSuccess) {
                            MeshLog.v("BT failed, trying BLE");
                            bleTransport.sendFileMessage(routingEntity.getAddress(), message);
                        }*/
                        //bleTransport.addFileMessageInQueue(routingEntity.getAddress(),null, message);
                        bleTransport.addAppMessageInQueue(routingEntity.getAddress(), null, message);
                        messageId = BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        break;
                    case RoutingEntity.Type.INTERNET:
                        remoteTransport.sendMessage(routingEntity.getAddress(), message);
                        messageId = BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        break;

                }
            }
        }

        return messageId;
    }

    public int sendMessageForBuyer(String receiverAddress, byte[] message, String immediateSender, String buyerMessageId) {

        int messageId = BaseMeshMessage.MESSAGE_STATUS_FAILED;

        if (message != null && AddressUtil.isValidEthAddress(receiverAddress)) {
            //pick immediate destination
            RoutingEntity routingEntity = null;
            if (!TextUtils.isEmpty(immediateSender)) {
                routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(immediateSender);
            } else {
                MeshLog.v("TransportManager immediateSender null");
            }

            if (routingEntity == null) {
                MeshLog.v("TransportManager routingEntity null");
                routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverAddress);
            }


            if (routingEntity != null) {

                MeshLog.v("Selected route: " + routingEntity.toString());

                switch (routingEntity.getType()) {

//                    case RoutingEntity.Type.BT:
//                        BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
//                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildMeshBtMessage(
//                                bleLink, message);
//                        MeshLog.e("FileMessageTest", "message sent");
//                        messageId = mMessageDispatcher.addSendMessage(baseMeshMessage);
//                        break;

                    case RoutingEntity.Type.WiFi:
                        messageId = wifiTransPort.sendAppMessage(buyerMessageId, routingEntity.getIp(), message);
                        break;

//                    case RoutingEntity.Type.HB:
//                        messageId = adhocTransport.sendAdhocMessage(routingEntity.getIp(), message);
//                        break;
                    case RoutingEntity.Type.BLE:
                        bleTransport.addFileMessageInQueue(routingEntity.getAddress(), buyerMessageId, message);
                        messageId = BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        break;
                    case RoutingEntity.Type.INTERNET:
                        remoteTransport.sendMessage(routingEntity.getAddress(), message);
                        messageId = BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        break;

                }
            }
        }

        return messageId;
    }

    /**
     * We are ignoring queue here. Because we don't need the broadcast file message in queue
     * and we don't need to try multiple path.
     *
     * @param receiverAddress destination receiver path
     * @param message         Broadcast data
     * @return ID of success
     */
    public String sendFileToBle(String receiverAddress, byte[] message) {
        if (message != null && AddressUtil.isValidEthAddress(receiverAddress)) {
            //pick immediate destination. Priority for wifi and direct connection
            RoutingEntity routingEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress("", receiverAddress, RoutingEntity.Type.WiFi);

            if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.BLE) {

                byte[] broadcastFileData = JsonDataBuilder.buildBroadcastFileMessage(myNodeId, routingEntity.getAddress(), message);

                boolean isSuccess = bleTransport.sendMessageViaBt(broadcastFileData);
                if (!isSuccess) {
                    return bleTransport.sendFileMessage(receiverAddress, broadcastFileData);
                } else {
                    return "Success";
                }
            }
        }
        return null;
    }


    void returnRoleSetSuccess() {
        if (paymentLinkStateListener != null) {
            String walletAddress = SharedPref.read(Constant.KEY_USER_ID);
            String publicKey = SharedPref.read(Constant.KEY_PUBLIC_KEY);
            paymentLinkStateListener.onTransportInit(walletAddress, publicKey, TransportState.SUCCESS, "Success");
        }
    }

    public int sendMultihopFileMessage(String receiverId, String messageId, byte[] message) {
        byte[] data = JsonDataBuilder.buildFiledMessage(myNodeId, receiverId, messageId, message);
        cacheSendingFileMessageForResend(myNodeId, receiverId, messageId, data);
        return sendMultihopFileMessageToTransport(receiverId, messageId, data);
    }

    private void cacheSendingFileMessageForResend(String sender, String receiver, String messageId, byte[] data) {
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);
        List<RoutingEntity> otherReachablePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
        PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, sender, data, routingEntity);
        pendingMessage.routeQueue.addAll(otherReachablePath);
        connectionLinkCache.addPendingMessage(messageId, pendingMessage);
    }


    public int sendMultihopFileMessageToTransport(String receiverAddress, String fileMessageId, byte[] message) {

        int messageId = BaseMeshMessage.MESSAGE_STATUS_FAILED;

        if (message != null && AddressUtil.isValidEthAddress(receiverAddress)) {
            //pick immediate destination
            RoutingEntity routingEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(receiverAddress);

            if (routingEntity != null) {

                MeshLog.v("Selected route: " + routingEntity.toString());

                switch (routingEntity.getType()) {
                    case RoutingEntity.Type.WiFi:
                        messageId = wifiTransPort.sendAppMessage(fileMessageId, routingEntity.getIp(), message);
                        break;

                    case RoutingEntity.Type.HB:
                        messageId = adhocTransport.sendAdhocMessage(routingEntity.getIp(), message);
                        break;
                    case RoutingEntity.Type.BLE:
                        bleTransport.addFileMessageInQueue(routingEntity.getAddress(), fileMessageId, message);
                        messageId = BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        break;
                    case RoutingEntity.Type.INTERNET:
                        remoteTransport.sendMessage(routingEntity.getAddress(), message);
                        messageId = BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        break;

                }
            }
        }

        return messageId;
    }


    /**
     * This method is used for only sending file
     * In this method we wrapped th original message like sending text message in locally
     * then call to @{sendMessage } methods
     * <p>
     * N.B:: Here we are using two layer Json wrapper.
     * May be we can minimize it
     *
     * @param receiverAddress String main receiver address
     * @param message         File message byte
     * @return message id
     */
    public int sendFileMessage(String receiverAddress, byte[] message) {
        //String messageDataLocal = Util.buildLocalMessage(message);
        //assert messageDataLocal != null;
        message = JsonDataBuilder.builFiledMessage(myNodeId, receiverAddress, message);
        return sendMessage(receiverAddress, message);
    }

    /**
     * This method is used for only sending broadcasting file
     *
     * @param receiverAddress String main receiver address
     * @param message         File message byte
     * @return message id
     */
    public int sendBroadcastMessage(String receiverAddress, String actualReceiver,
                                    String broadcastContentId, String textData, String contentPath,
                                    long expireTime, String appToken, byte[] message, double latitude,
                                    double longitude, double range) {
        message = JsonDataBuilder.buildBroadcastData(myNodeId, receiverAddress, actualReceiver, broadcastContentId,
                textData, contentPath, expireTime, appToken, message, latitude, longitude, range);
        return sendMessage(receiverAddress, message);
    }

    public int sendBroadcastMessage(Broadcast broadcast) {
        byte[] broadcastData = GsonUtil.on().broadcastToString(broadcast).getBytes();
        return sendMessage(broadcast.getReceiverId(), broadcastData);
    }


    public void sendBuyerFileMessage(String receiverId, byte[] data, int fileMessageType, String immediateSender, String messageId) {

        //TODO implement logic for buyer if buyer directly connected to internet.

        if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_SELLER
                || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER
                || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.MESH_USER) {
            byte[] messageToSend = JsonDataBuilder.buildBuyerFileMessage(myNodeId, receiverId, data, fileMessageType, messageId);
            sendMessageForBuyer(receiverId, messageToSend, immediateSender, messageId);

        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);
            if (routingEntity == null) return;


//            if (currentSellerId != null) {
            String messageDataInternet = Util.buildInternetSendingMessage(myNodeId, receiverId, data);
            if (messageDataInternet != null) {
                byte[] messageToSend = JsonDataBuilder.buildBuyerFileMessage(myNodeId, routingEntity.getAddress(), messageDataInternet.getBytes(), fileMessageType, messageId);
                sendMessageForBuyer(routingEntity.getAddress(), messageToSend, immediateSender, messageId);
            }
//            } else {
//                MeshLog.v("No internet seller found to reach destination");
//            }
        }
    }


    public void sendTextMessage(String senderId, String receiverId, String messageId, byte[] data) {
        sendToTarget(senderId, receiverId, messageId, data);
    }


    private void sendToTarget(String senderId, String destinationID, String messageId, byte[] data) {

        //MeshLog.v("The message data to send 1: " + new String(data));

        if (Util.saveMessage(senderId, destinationID, messageId, data, mContext)) {

            //RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(senderId, destinationID, 0);
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(destinationID);
            if (routingEntity == null) return;

            if (RouteManager.getInstance().getLinkTypeById(destinationID) == RoutingEntity.Type.INTERNET) {

                if (remoteTransport != null && RouteManager.getInstance().isDirectlyConnected(routingEntity.getAddress())) {
                    byte[] messageData = JsonDataBuilder.buildMessage(senderId, destinationID, messageId, data);
                    MeshLog.e("send to target  ");

                    // Cache sending message for multi path try
                    cacheSendingMessageForResendInternet(senderId, destinationID, messageId, messageData);
                    remoteTransport.sendAppMessage(messageId, routingEntity.getAddress(), messageData);
                } else {
                    if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_BUYER) {
//                        if (currentSellerId != null) {
                        String messageDataInternet = Util.buildInternetSendingMessage(senderId, destinationID, data);
                        if (messageDataInternet != null) {
                            // Try to send message to seller (routingEntity.getAddress()) by using multiple path
                            cacheSendingMessageForResendBuyer(senderId, destinationID, messageId, messageDataInternet.getBytes());
                            sendLocalMessage(senderId, routingEntity.getAddress(), messageId, messageDataInternet.getBytes());
                        }
//                        } else {
//                            MeshLog.v("No internet seller found to reach destination");
//                        }
                    } else {
                        MeshLog.v("Internet user not found");
                    }
                }
            } else {
                boolean isWiFiConnectionRequired = data.length >
                        BleMessageHelper.MAX_DATA_SIZE_IN_BYTE;

                if (routingEntity == null) {
                    //Offline users, search the users network
                    if (!isFileSendingMode && !isFileReceivingMode) {
                        //Todo we think for text message we not need to specific BLE search.
                        // The node will connect naturally but need take time
                        //mDriverManager.search(destinationID, isWiFiConnectionRequired);
                    }

                } else {

                    if (isWiFiConnectionRequired && !RoutingEntity.isWiFiNode(destinationID)) {
                        MeshLog.e("[BLE_PROCESS] Routing entity null start ble force search");

                        //Search existing node over WiFi
                        // mDriverManager.search(destinationID, true);

                    } else {

                        String messageDataLocal = Util.buildLocalMessage(data);
                        if (messageDataLocal != null) {
                            cacheSendingMessageForResend(senderId, destinationID, messageId, messageDataLocal.getBytes());
                            sendLocalMessage(senderId, destinationID, messageId, messageDataLocal.getBytes());
                        }
                    }
                }
            }
        }
    }

    private void sendLocalMessage(String senderId, String receiverId, String messageId, byte[] data) {

        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(senderId, receiverId, RoutingEntity.Type.WiFi);
        if (routingEntity == null) {
            MeshLog.e("Routing Entity NULL in Sending Message");
            /*RoutingEntity receiverAddress = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);
            if (receiverAddress == null) {
                linkStateListener.onUserDisconnected(receiverId);
            }*/

            RoutingEntity arbitraryEntity = RouteManager.getInstance().getArbitraryEntityById(receiverId);
            if (arbitraryEntity == null) {
                forwardMessageAfterSomeDelay(senderId, receiverId, messageId, data);
            } else {
                linkStateListener.onUserDisconnected(receiverId);
            }

        } else {

            byte[] messageData = JsonDataBuilder.buildMessage(senderId, receiverId, messageId, data);

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {

                MeshLog.v("sendMessage Wifi user" + "receiver : " + receiverId);
                RoutingEntity realEntity = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);
                if (realEntity != null) {
                    wifiTransPort.sendAppMessage(messageId, routingEntity.getIp(), messageData);
                    //MeshLog.e("[BLE_PROCESS] Add message in queue from transport :" + result);
                    //MeshLog.v("The message data to send 2: " + new String(messageData));
                }

            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {

                MeshLog.v("sendMessage ble user" + " receive : " + receiverId);
                BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    mMessageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, messageData));
                    //bleLink.sendMeshMessage(userInfoMsg);
                } else {
                    MeshLog.v("(!) BLE LINK NOT FOUND");
                }

            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {

                MeshLog.v("sendMessage Adhoc user" + "receiver : " + receiverId);
                RoutingEntity realEntity = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);
                if (realEntity != null) {
                    adhocTransport.sendAdhocMessage(routingEntity.getIp(), messageData);
                }

            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {

                MeshLog.e("[BLE_PROCESS] send msg to BLE : " + routingEntity.getAddress());
                bleTransport.addAppMessageInQueue(messageId, routingEntity.getAddress(), messageData);
            } else {
                //TODO retry after 1 second if routing entity not found
                RoutingEntity arbitraryEntity = RouteManager.getInstance().getArbitraryEntityById(receiverId);
                if (arbitraryEntity == null) {
                    forwardMessageAfterSomeDelay(senderId, receiverId, messageId, data);
                } else {
                    MeshLog.v("TM User offline found to send the local message");
                }
            }

        }
    }

    private void forwardMessageAfterSomeDelay(String senderId, String receiverId, String messageId, byte[] data) {
        MeshLog.v("TM User null so make 2 second delay and try again");
        HandlerUtil.postBackground(() -> sendLocalMessage(senderId, receiverId, messageId, data), 2000L);
    }

    private void cacheSendingMessageForResend(String sender, String receiver, String messageId, byte[] data) {
        byte[] messageData = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);
        List<RoutingEntity> otherReachablePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
        PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, sender, messageData, routingEntity);
        pendingMessage.routeQueue.addAll(otherReachablePath);
        connectionLinkCache.addPendingMessage(messageId, pendingMessage);
    }

    private void cacheSendingMessageForResendInternet(String sender, String receiver, String messageId, byte[] data) {
        byte[] messageData = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        List<RoutingEntity> otherReachablePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
        PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, sender, messageData, routingEntity);
        pendingMessage.routeQueue.addAll(otherReachablePath);
        connectionLinkCache.addPendingMessage(messageId, pendingMessage);
    }

    private void cacheSendingMessageForResendBuyer(String sender, String receiver, String messageId, byte[] data) {

        List<RoutingEntity> allPossibleSellerPathList = RouteManager.getInstance()
                .getAllPossiblePathByIdAndType(receiver, RoutingEntity.Type.INTERNET);


        RoutingEntity currentSeller = allPossibleSellerPathList.get(0);

        List<RoutingEntity> allSellerPossiblePathList = new ArrayList<>();

        if (allPossibleSellerPathList.size() > 1) {
            for (int i = 1; i < allPossibleSellerPathList.size(); i++) {
                RoutingEntity nextSeller = allPossibleSellerPathList.get(i);
                allSellerPossiblePathList
                        .addAll(RouteManager.getInstance().getAllPossibleOnlinePathById(nextSeller.getAddress()));
            }
        }

        RoutingEntity routingEntity = RouteManager.getInstance()
                .getNextNodeEntityByReceiverAddress(sender, currentSeller.getAddress(), RoutingEntity.Type.WiFi);
        List<RoutingEntity> otherReachablePath = connectionLinkCache
                .filterShortestPathEntity(routingEntity, currentSeller.getAddress());

        allPossibleSellerPathList.addAll(otherReachablePath);
        // we have to filter out this allPossibleSellerPathList

        Collections.sort(allPossibleSellerPathList, (o1, o2) -> Integer.compare(o1.getHopCount(), o2.getHopCount()));

        MeshLog.i("All possible seller shortest path list: " + allPossibleSellerPathList.toString());
        byte[] messageData = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);
        PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, sender, messageData, routingEntity);
        pendingMessage.routeQueue.addAll(allPossibleSellerPathList);
        connectionLinkCache.addPendingMessage(messageId, pendingMessage);
    }

    public int getLinkTypeById(String nodeID) {
        return RouteManager.getInstance().getLinkTypeById(nodeID);
    }

    public boolean isOnline(String nodeId) {
        List<Integer> linkTypes = getMultipleLinkTypeById(nodeId);
        return linkTypes.size() > 0;
    }

    public int getTypeById(String nodeId) {

        RoutingEntity entity = RouteManager.getInstance().getShortestPath(nodeId);
        if (entity != null) {
            return entity.getType();
        }

        return 0;
    }

    public List<Integer> getMultipleLinkTypeById(String nodeID) {
        return RouteManager.getInstance().getMultipleLinkTypeById(nodeID);
    }

    public List<Integer> getMultipleLinkTypeByIdDebug(String nodeID) {
        return RouteManager.getInstance().getMultipleLinkTypeByIdDebug(nodeID);
    }

    public String getUserId() {
        return SharedPref.read(Constant.KEY_USER_ID);
    }

    public List<String> getInternetSellers() {
        try {
            List<String> localSellers = connectionLinkCache.getInternetSellers();
            //TODO This validation is not required anymore (tariqul, mimo, major arif)
//            return databaseService.getValidUserIds(localSellers);
            return localSellers;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public boolean isUserConnected(String id) {

//        NodeInfo info = connectionLinkCache.getNodeInfoById(id);
//        return info != null && info.getUserType() != RoutingEntity.Type.INTERNET;

        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(id);
        return routingEntity != null && routingEntity.getType() != RoutingEntity.Type.INTERNET;

    }

    public boolean isInternetSeller(String nodeId) {
        RoutingEntity entity = RouteManager.getInstance().getSellerById(nodeId);
        return entity != null;
    }

    /**
     * Listener is initialise here to get all kind of callback here
     * and after that send callback to app level
     */

    LinkStateListener linkStateListener = new LinkStateListener() {
        @Override
        public void onTransportInit(String nodeId, String publicKey, TransportState transportState, String msg) {
        }

        @Override
        public void onLocalUserConnected(String nodeId, String userInfo) {

            mLinkStateListener.onLocalUserConnected(nodeId, userInfo);
            if (paymentLinkStateListener != null) {
                MeshLog.v("onLocalUserConnected paymentLinkStateListener not null");
                paymentLinkStateListener.onLocalUserConnected(nodeId, userInfo);
            } else {
                MeshLog.v("onLocalUserConnected paymentLinkStateListener null");
            }
            //TODO open this code in release build
            //sendPendingMessage(nodeId);

            // mDriverManager.startBLE(nodeId, "sample", "paswword");
        }

        @Override
        public void onRemoteUserConnected(String nodeId, String userInfo) {
            mLinkStateListener.onRemoteUserConnected(nodeId, userInfo);
            if (paymentLinkStateListener != null) {
                paymentLinkStateListener.onRemoteUserConnected(nodeId, userInfo);
            }
        }

        @Override
        public void onUserDisconnected(String nodeId) {
            RoutingEntity entity = RouteManager.getInstance().getEntityByAddress(nodeId);
            if (entity != null && entity.isOnline()) {
                MeshLog.v("[P2p process] user actually not disconnected");
                return;
            }

            // This line was removed before. But is need to notify
            // UI that a node actually disconnected
            mLinkStateListener.onUserDisconnected(nodeId);

            if (paymentLinkStateListener != null) {
                paymentLinkStateListener.onUserDisconnected(nodeId);
            }
        }

        @Override
        public void onMessageReceived(String senderId, byte[] frameData) {
            mLinkStateListener.onMessageReceived(senderId, frameData);
        }

        @Override
        public void onProbableSellerDisconnected(String userId) {

//            RoutingEntity routingEntity = RouteManager.getInstance().getEntityByAddress(userId);
//            if (routingEntity == null || !routingEntity.isOnline()) {
//                if (paymentLinkStateListener != null) {
//                    //TODO check which user will be sent
//                    paymentLinkStateListener.onProbableSellerDisconnected(userId);
//                }
//            }
        }

        @Override
        public void onUserModeSwitch(String sendId, int newRole, int previousRole) {
            if (paymentLinkStateListener != null) {
                MeshLog.v("New user mode receive :: " + newRole);
                paymentLinkStateListener.onUserModeSwitch(sendId, newRole, previousRole);
            }
        }

        @Override
        public void onFileMessageReceived(String sender, String message) {
            mLinkStateListener.onFileMessageReceived(sender, message);
        }

        @Override
        public void onClientBtMsgSocketConnected(BluetoothDevice bluetoothDevice) {
            mLinkStateListener.onClientBtMsgSocketConnected(bluetoothDevice);
        }

        @Override
        public void onBTMessageSocketDisconnect(String userId) {
            // We will disconnect BT socket connection here
            // And we will fail file from the below callback

            setBtConnecting(false);

            BleLink link = BleLink.getBleLink();
            if (link != null) {
                link.disconnect();
            }

            mLinkStateListener.onBTMessageSocketDisconnect(userId);

        }

        @Override
        public void onBroadcastContentDetailsReceived(String sender, String message) {
            mLinkStateListener.onBroadcastContentDetailsReceived(sender, message);
        }

        @Override
        public void onMessagePayReceived(String sender, byte[] paymentData) {
            MeshLog.v("onMessagePayReceived trns " + sender);
            if (paymentLinkStateListener != null) {
                paymentLinkStateListener.onMessagePayReceived(sender, paymentData);
            }
        }

        @Override
        public void onPayMessageAckReceived(String sender, String receiver, String messageId) {
            MeshLog.v("onPayMessageAckReceived trns " + sender);
            if (paymentLinkStateListener != null) {
                paymentLinkStateListener.onPayMessageAckReceived(sender, receiver, messageId);
            }
        }

        @Override
        public void buyerInternetMessageReceived(String sender, String receiver, String messageId, String messageData, long dataLength, boolean isIncoming, boolean isFile) {
            if (paymentLinkStateListener != null) {
                paymentLinkStateListener.buyerInternetMessageReceived(sender, receiver, messageId, messageData, dataLength, isIncoming, isFile);
            }
        }

//        @Override
//        public void onCurrentSellerId(String sellerId) {
//            MeshLog.v("CurrentSellerId " + sellerId);
//            currentSellerId = sellerId;
//        }


        @Override
        public void onMessageDelivered(String messageId, int status) {
            MeshLog.i("onMessageDelivered is called =" + messageId);
            waitingMessageMap.remove(messageId);

            String token = "";
            try {
                Message message = databaseService.getMessageById(messageId);
                if (message != null) {
                    token = message.getAppToken();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            if (status == Constant.MessageStatus.RECEIVED) {
                databaseService.deleteMessage(messageId);

/*                int totalMsgSent = SharedPref.readInt(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT);
                SharedPref.write(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT, totalMsgSent + 1);*/
            }
            mLinkStateListener.onMessageDelivered(messageId, status, token);
        }

        @Override
        public void onLogTextReceive(String text) throws RemoteException {
            mLinkStateListener.onLogTextReceive(text);
        }


        @Override
        public void onInterruption(int details) {
            mLinkStateListener.onInterruption(details);
        }

        @Override
        public void onInterruption(List<String> missingPermissions) {
            mLinkStateListener.onInterruption(missingPermissions);
        }

        @Override
        public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
            mLinkStateListener.onHandshakeInfoReceived(handshakeInfo);
        }

        @Override
        public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {
            mLinkStateListener.onBroadcastACKMessageReceived(broadcastAck);
        }

        @Override
        public boolean onBroadcastSaveAndExist(Broadcast broadcast) {
            return mLinkStateListener.onBroadcastSaveAndExist(broadcast);
        }

        @Override
        public void onReceivedAckSend(String broadcastID, String senderId) {
            mLinkStateListener.onReceivedAckSend(broadcastID, senderId);
        }

        @Override
        public void onBroadcastMessageReceive(Broadcast broadcast) {
            mLinkStateListener.onBroadcastMessageReceive(broadcast);
        }

        @Override
        public void onReceivedFilePacket(byte[] data) {
            mLinkStateListener.onReceivedFilePacket(data);
        }
    };

    public void makeInternetUsersOffline(String sellerAddress) {

//        if (remoteTransport != null && remoteTransport.amIDirectUser()) {
//            return;
//        }
        List<RoutingEntity> internetUsers;
        if (sellerAddress == null) {
            internetUsers = RouteManager.getInstance().getInternetUsers();
        } else if (sellerAddress.equalsIgnoreCase("all_sellers")) {
            internetUsers = RouteManager.getInstance().selectOnlyHopedNodesForInternet();
        } else {
            internetUsers = RouteManager.getInstance().selectOnlyHopedNodesForInternetBySeller(sellerAddress);
        }

        if (internetUsers != null && !internetUsers.isEmpty()) {
            for (RoutingEntity routingEntity : internetUsers) {
                RouteManager.getInstance().makeUserOffline(routingEntity);

                // The below line actually redundant here.
                RoutingEntity onlineRoutingEntity = RouteManager.getInstance().getEntityByAddress(routingEntity.getAddress());

                if (onlineRoutingEntity == null || !onlineRoutingEntity.isOnline()) {
                    mLinkStateListener.onUserDisconnected(routingEntity.getAddress());
                    if (paymentLinkStateListener != null) {
                        paymentLinkStateListener.onUserDisconnected(routingEntity.getAddress());
                    }
                }
            }
        }
    }


    public Context getContext() {
        return mContext;
    }


    public List<Message> getAllIncomingPendingMessage(String appToken) {
        try {
            return databaseService.getInComingPendingMessage(appToken);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Message> getAllOutgoingPendingMessage(String nodeId) {
        try {
            return databaseService.getPendingMessage(nodeId);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void savePendingMessage(String senderId, byte[] data, String appToken) {
        Message message = new Message();
        message.senderId = senderId;
        message.messageId = UUID.randomUUID().toString();
        message.data = data;
        message.isIncoming = true;
        message.setAppToken(appToken);
        databaseService.insertMessage(message);
    }

    public void deletePendingMessage(String messageId) {
        databaseService.deleteMessage(messageId);
    }

    /**
     * This flag is used to track BT Classic socket connecting moment. If a connection establishment
     * is in progress we can handle other connection attempt by this flag
     *
     * @param isConnecting Boolean
     */
    public void setBtConnecting(boolean isConnecting) {
        this.isBluetoothConnecting = isConnecting;
    }

    @Override
    public void onMessageSend(int messageId, String ipAddress, boolean messageStatus) {
        MeshLog.i("Message send status =" + messageStatus + " ip: " + ipAddress);
        if (messageStatus) {
            MeshLog.i("Message send success");
        } else {
            MeshLog.i("Message send failed");
        }
        if (mMessageListener != null) {
            mMessageListener.onMessageSend(messageId, ipAddress, messageStatus);
        }

    }

    @Override
    public void onWifiDirectMessageSend(String messageId, boolean status) {
        MeshLog.i("onMessageDelivered is called");

        if (status) {
            databaseService.deleteMessage(messageId);
            mLinkStateListener.onMessageDelivered(messageId, Constant.MessageStatus.RECEIVED);
        }

    }

    @Override
    public void onWifiHelloMessageSend(String ip, boolean isSuccess) {
        onHelloPacketSend(ip, isSuccess);
        /*if (!TextUtils.isEmpty(ip)) {
            MeshLog.e("Failed response the client so connection closed with ::" + ip);
            RoutingEntity routingEntity = RouteManager.getInstance().getNodeDetailsByIP(ip);
            if (routingEntity != null) {
                if (routingEntity.getType() == RoutingEntity.Type.HB) {
                    MeshLog.i("Adhoc user Disconnected!!!");
                    adhocTransport.onDirectUserDisconnect(routingEntity);
                } else if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                    meshManager.mMeshXAPListener.onGODisconnectedWith(ip);
                }
            }
        }*/

    }

    public void isUserAvailable(String userId, UserState connected) {
        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(userId);
        if (routingEntity != null
                && (routingEntity.getType() == RoutingEntity.Type.HB
                || routingEntity.getType() == RoutingEntity.Type.WiFi)) {

            new Thread(new Pinger(routingEntity.getIp(), (ip, isReachable) -> {
                connected.onUserConnected(userId, isReachable);

                if (!isReachable && routingEntity.getType() == RoutingEntity.Type.HB) {

                    adhocTransport.onDirectUserDisconnect(routingEntity);

                } else if (!isReachable && routingEntity.getType() == RoutingEntity.Type.WiFi) {
                    if (P2PUtil.isMeGO()) {
                        MeshLog.e("[P2p] Process Ping failed. node disconnected: "
                                + AddressUtil.makeShortAddress(routingEntity.getAddress()));
                        TransportManagerX.getInstance().mMeshXAPListener.onGODisconnectedWith(routingEntity.getIp());
                    } else {
                        if (P2PUtil.isConnectedWithPotentialGO(this.mContext)) {
                            MeshLog.v("User connected to GO, but can't give message for Socket error");
                            HandlerUtil.postForeground(new Runnable() {
                                @Override
                                public void run() {
                                    Toaster.showShort("Network Layer issue, please try with opposite end user.");
                                }
                            });
                        } else {
                            TransportManagerX.getInstance().mMeshXLCListener.onDisconnectWithGO(routingEntity.getAddress());
                        }
                    }
                }
            }, 2)
            ).start();

        } else {
            connected.onUserConnected(userId, true);
        }
    }


//    public String getCurrentSellerId() {
//        return currentSellerId;
//    }

    public void setLinkStateListenerForPayment(LinkStateListener linkStateListener) {
        MeshLog.v("setLinkStateListenerForPayment");
        paymentLinkStateListener = linkStateListener;
    }


    public void initForwardListener(ForwardListener forwardListener) {
        this.mForwardListener = forwardListener;
    }

    public void setBtClassicMessage(String testMessage) {
        BleLink link = BleLink.getBleLink();
        if (link != null) {
            link.sendMeshMessage(testMessage.getBytes());
        }
    }
}
