package com.w3engineers.mesh.Adhoc;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.Adhoc.protocol.JmdResolver;
import com.w3engineers.mesh.Adhoc.protocol.WifiDetector;
import com.w3engineers.mesh.Adhoc.protocol.WifiResolver;
import com.w3engineers.mesh.Adhoc.util.IpFilterUtil;
import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.bluetooth.BTManager;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.datasharing.util.NotificationUtil;
import com.w3engineers.mesh.datasharing.util.PurchaseConstants;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.httpservices.MeshHttpServer;
import com.w3engineers.mesh.libmeshx.wifid.Pinger;
import com.w3engineers.mesh.libmeshx.wifid.WiFiDirectManagerLegacy;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.queue.DispatcherHelper;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BTDiscoveryMessage;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.queue.messages.WiFiDiscoverMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.util.log.DeviceInfo;
import com.w3engineers.mesh.util.log.MyTextSpeech;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.ServiceEvent;

import static com.w3engineers.mesh.util.Constant.KEY_USER_NAME;
import static com.w3engineers.mesh.util.Constant.MASTER_IP_ADDRESS;

/**
 * <h1> Zeroconf transport</h1>
 * <p> All types of Zeroconf communication manages here </p>
 * <p> Communication with other transport also maintains </p>
 * <p> </p>
 */
public class AdHocTransport implements ConnectionStateListener, WifiResolver.Listener, MeshTransport,
        WifiDetector.Listener {

    private String myNodeId;
    private Context context;
    private LinkStateListener linkStateListener;
    private BluetoothTransport bluetoothTransport;
    private WifiTransPort wifiTransPort;
//    private InternetTransport internetTransport;
    private String myIpAddress;
    private ConnectionLinkCache connectionLinkCache;
    private TransportManagerX.NodeConnectivityChangeListener mNodeConnectivityChangeListener;
    private DispatchQueue outputThread = new DispatchQueue();
    private MessageDispatcher messageDispatcher;
    private MyTextSpeech mMyTextSpeech;
    private WifiResolver jmdnsResolver;
    private DispatchQueue queue;
    private boolean running = false;
    private String serviceType;
    private Map<String, String> discoveredServiceMap = new ConcurrentHashMap<>();
    private final String TAG = "Jmdnslog";
    private IpFilterUtil ipFilterUtil;
    private WifiDetector wifiDetector;
    public static final int ADHOC_PING_TIMEOUT = 1 * 1000;
    public static final int ADHOC_PING_NUMBER_OF_RETRY = 2;
    private ExecutorService mPingExecutor;
    private ForwardListener forwardListener;
    /**
     * hold discovery sending message id. Reset upon receiving response from other Adhoc peer or
     * upon disconnecting. As this value always overlaps earlier id and we manage queue
     * for message sending so we will receive ack for last message at last and consumer of
     * this variable will be able to track always last value.
     */
    public volatile int mQueryingPeerMessageId = BaseMeshMessage.DEFAULT_MESSAGE_ID;

    /**
     * <p>Constructor to init wifi transport</p>
     *
     * @param context             : android context
     * @param appId               : int used as tcp port
     * @param nodeId              : String used as node id
     * @param connectionListener  : app layer listener {@link LinkStateListener}
     * @param connectionLinkCache : Bluetooth link reference
     */
    public AdHocTransport(Context context, int appId, String nodeId,
                          LinkStateListener connectionListener, ConnectionLinkCache connectionLinkCache,
                          MessageDispatcher messageDispatcher) {
        this.queue = new DispatchQueue();

        this.myNodeId = nodeId;
        this.context = context.getApplicationContext();
        this.linkStateListener = connectionListener;
        this.connectionLinkCache = connectionLinkCache;
        this.messageDispatcher = messageDispatcher;
        this.serviceType = "_mesh" + appId + "._tcp.";
        mMyTextSpeech = new MyTextSpeech(context);
        wifiDetector = new WifiDetector(this, queue, context);
        jmdnsResolver = new JmdResolver(serviceType, nodeId, this, queue, context);
        this.mPingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void start() {
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            Log.e(TAG, "Adhoc transport started");

            MeshHttpServer.on().setListener(this);
            if (running)
                return;
            running = true;
            jmdnsResolver.stop();
            wifiDetector.start();
            discoveredServiceMap.clear();
        }
    }

    @Override
    public void stop() {
        //jmdnsResolver.stop();
        stopIpFiltering();
        running = false;
        wifiDetector.stop();
        discoveredServiceMap.clear();
        stopAdHocDiscovery();
    }


    @Override
    public void onAdhocEnabled(InetAddress address) {
        // FIXME: 10/13/2020 Adhoc was getting called although it's a GO. Commented temporarily
        //for Lazy Mesh
        MeshLog.v("[MeshX]onAdhocEnabled:" + address);
        /*if (!P2PUtil.isConnectedWithPotentialGO(context)) {
            running = true;
            MeshLog.i(" Connected With AdHoc Network ");
            startAdHocDiscovery();
        } else {
            InetAddress inetAddress = WifiDetector.determineAddress(context);
            if (inetAddress != null) {
                wifiTransPort.scanSubnet(inetAddress.getHostAddress(), WifiTransPort.P2P_MASTER_IP_ADDRESS);
            }
        }*/
    }

    @Override
    public void onAdhocDisabled() {
        MeshLog.e("AdHoc disabled!!!");

        // TODO: 1/13/2020 this should be done from a centralized monitoring place to attain
        //  consistency
        //Simplistic remove of all nodes from UI and other necessary tasks
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getAdhocUser();
        if (CollectionUtil.hasItem(routingEntities)) {
            for (RoutingEntity routingEntity : routingEntities) {
                TransportManagerX.getInstance().mMeshXLCListener.onDisconnectedWithAdhoc(routingEntity.getAddress());
            }
        }

        stopAdHocDiscovery();
    }

    @Override
    public void onWiFiEnabled() {


    }

    /**
     * startAdHocDiscovery
     */

    public synchronized void startAdHocDiscovery() {

        if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER || jmdnsResolver.isRunning())
            return;

        HandlerUtil.postBackground(() -> {
            try {
                if (jmdnsResolver.isRunning()) return;

                String wifiIp = AddressUtil.getWiFiIpAddress();
                //String wifiIp2 = AddressUtil.getLocalIpAddress();
                //MeshLog.e("Adhoc IP address " + wifiIp2);
                MeshLog.e("Adhoc IP address null" + wifiIp);
                if (wifiIp == null) {
                    MeshLog.e("Adhoc IP address null");
                    return;
                }
                MeshLog.e("Jmdnslog" + wifiIp);
                InetAddress address = InetAddress.getByName(wifiIp);
                if (!running)
                    return;
                if (address != null) {
                    myIpAddress = address.getHostAddress();
                    MeshLog.v("AdHoc Discovery Started:" + myIpAddress);
                    jmdnsResolver.start(address, TransportManagerX.APP_PORT);
                    filterActiveIpAddressFromSubnet(myIpAddress);
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        });


    }

    /**
     * stopIpFiltering
     */

    private void stopIpFiltering() {
        if (ipFilterUtil != null) {
            ipFilterUtil.stopFiltering();
        }
    }

    /**
     * prepareAllIPAddress
     *
     * @param myIp own ip address
     */
    private void filterActiveIpAddressFromSubnet(String myIp) {

        if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER) return;

        stopIpFiltering();
        ipFilterUtil = new IpFilterUtil(myIp, TransportManagerX.APP_PORT, activeIpList -> {

            activeIpList.remove(myIpAddress);
            //allLocalIpAddress.addAll(activeIpQueue);
            //Log.e("Jmdnslog", "Missed ip =" + activeIpList.toString());
            MeshLog.i("After all filter active ip =" + activeIpList.toString());
            for (String item : activeIpList) {
                if (TextUtils.isEmpty(item)) continue;

                //Softdisable BT
                if (!RouteManager.getInstance().isBtUserConnected() &&
                        DispatcherHelper.getDispatcherHelper().isNotBTConnecting()) {//No BT connected
                    MeshLog.i(" BT SOFT diasble called from filterActiveIpAddressFromSubnet");
                    BTManager.getInstance(App.getContext()).softDisable();
                }
                if (WiFiDirectManagerLegacy.getInstance() != null) {
                    WiFiDirectManagerLegacy.getInstance().pauseConnectivity();
                }

                mQueryingPeerMessageId = sendRequest(myIpAddress, item);
            }
        });

    }


    public void stopAdHocDiscovery() {
        if (!running) return;

        jmdnsResolver.stop();
        stopIpFiltering();
        running = false;
    }


    @Override
    public void onBonjourServiceResolved(String userAddress, String ip, int port) {
        if (discoveredServiceMap.containsKey(ip)) {
            return;
        }
        jmdnsResolver.stopServiceListening();
        discoveredServiceMap.put(ip, userAddress);
        MeshLog.v("Service name =" + userAddress + " Address =" + ip + " port =" + port);
        Log.e("Jmdnslog", "Service discovered ip=" + ip);

        if (!RouteManager.getInstance().isAdhocUserOnline(userAddress)) {
            //Softdisable BT
            if (!RouteManager.getInstance().isBtUserConnected() &&
                    DispatcherHelper.getDispatcherHelper().isNotBTConnecting()) {//No BT connected
                MeshLog.i(" BT SOFT diasble called from onBonjourServiceResolved");
                BTManager.getInstance(App.getContext()).softDisable();
            }
            if (WiFiDirectManagerLegacy.getInstance() != null) {
                WiFiDirectManagerLegacy.getInstance().pauseConnectivity();
            }

            mQueryingPeerMessageId = sendMyInfo(myIpAddress, ip);


        }
    }

    @Override
    public void serviceLost(ServiceEvent serviceEvent) {
        MeshLog.e("Service Removed::" + serviceEvent.toString());
        //TransportManagerX.getInstance().mMeshXLCListener.onDisconnectedWithAdhoc(serviceEvent.getName());
    }

    /**
     * <p>Inject dependent transport</p>
     *
     * @param transPorts :
     */
    public <T> void setInterTransport(T... transPorts) {
        for (T item : transPorts) {
            if (item instanceof BluetoothTransport) {
                this.bluetoothTransport = (BluetoothTransport) item;
            } /*else if (item instanceof InternetTransport) {
                this.internetTransport = (InternetTransport) item;
            }*/ else if (item instanceof WifiTransPort) {
                this.wifiTransPort = (WifiTransPort) item;
            } else if (item instanceof ForwardListener) {
                this.forwardListener = (ForwardListener) item;
            }
        }
    }


    public int sendAdhocMessage(String ip, byte[] data) {
        return messageDispatcher.addSendMessage(MessageBuilder.buildMeshWiFiMessage(ip, data));
    }

    public void passUserInfo(String ip, byte[] data) {
        messageDispatcher.addSendMessage(MessageBuilder.buildWiFiDiscoveryMeshMessage(ip, () -> data));
    }

    public int sendPayMessage(String ip, byte[] data) {
        return messageDispatcher.addSendMessage(MessageBuilder.buildMeshWiFiMessage(ip, data));
    }


    @Override
    public void onReceivedDirectUserFromAdHocListener(String senderInfo, String btUser, String btMeshUser) {
        NodeInfo senderNodeInfo = GsonUtil.on().itemFromJson(senderInfo);
        MeshLog.v("onReceivedDirectUserFromAdHocListener::" + senderNodeInfo);

        if (senderNodeInfo == null) return;

        if (myNodeId.equals(senderNodeInfo.getUserId())) {
            MeshLog.e("Connect With Own GO !!! LC will be disconnected on Time out");
            MeshLog.e("Self nodeId::" + myNodeId);
            mMyTextSpeech.speak("Warning. Connect With Own GO !!! ");
            return;
        }


        //Softdisable BT
        if (!RouteManager.getInstance().isBtUserConnected() &&
                DispatcherHelper.getDispatcherHelper().isNotBTConnecting()) {//No BT connected
            MeshLog.i(" BT SOFT diasble called from onReceivedDirectUserFromAdHocListener");
            BTManager.getInstance(App.getContext()).softDisable();
        }
        if (WiFiDirectManagerLegacy.getInstance() != null) {
            WiFiDirectManagerLegacy.getInstance().pauseConnectivity();
        }

       /* NSDHelper nsdHelper = NSDHelper.getInstance(App.getContext());
        if (nsdHelper != null) {
            nsdHelper.stopDiscovery();
        }*/
        // Check cycle with direct BT
        // if that happens then
        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(senderNodeInfo.getUserId());

        if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.INTERNET) {
            // previously internet user
            // process next
            MeshLog.i("InternetMsg RemovePeerConnection for previous Internet User from adhoc listener");
//            PeerConnectionHolder.closeSingleConnection(routingEntity.getAddress());
        }

        if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.BT) {
            MeshLog.i("ADHOC cycle with direct BT connection");
            mQueryingPeerMessageId = sendHelloResponseToClient(senderNodeInfo, GsonUtil.getUserInfo(), true);
        } else {
            mQueryingPeerMessageId = sendHelloResponseToClient(senderNodeInfo, GsonUtil.getUserInfo(), false);
        }


        // pass these users to bt
        ConcurrentLinkedQueue<NodeInfo> btUserNodeInfoList = GsonUtil.on().queueFromJson(btUser);
        ConcurrentLinkedQueue<NodeInfo> btMeshUserNodeInfoList = GsonUtil.on().queueFromJson(btMeshUser);
        ConcurrentLinkedQueue<NodeInfo> allUser = new ConcurrentLinkedQueue<>();

        allUser.add(senderNodeInfo);


        if (CollectionUtil.hasItem(btUserNodeInfoList)) {
            MeshLog.e(" Connected btUserNodeInfoList List :" + btUserNodeInfoList.toString());
            allUser.addAll(btUserNodeInfoList);
        }
        if (CollectionUtil.hasItem(btMeshUserNodeInfoList)) {
            MeshLog.e(" Connected btMeshUserNodeInfoList List :" + btMeshUserNodeInfoList.toString());
            allUser.addAll(btMeshUserNodeInfoList);
        }

        boolean isSenderAlive = RouteManager.getInstance().isAdhocUserOnline(senderNodeInfo.getUserId());

        if (isSenderAlive) {
            // make decision based on exist list and current list
            List<RoutingEntity> connectedUsers = RouteManager.getInstance().getConnectedNodesByAddress(senderNodeInfo.getUserId());
            if (connectedUsers != null) {
                if (connectedUsers.size() == allUser.size()) {
                    MeshLog.e("Connected user's list  and new user's list  same");
                    // update nodeInfo as new session bt name wold be different
                    addNodeInformation(senderNodeInfo.getUserId(), senderNodeInfo);
                    // just return
                    return;
                } else if (connectedUsers.size() > allUser.size()) {
                    // TODO: 11/6/19
                    MeshLog.e("Connected user's list  greater than all user's list  ");
                } else {
                    // TODO: 11/6/19
                    MeshLog.e("Connected user's list  less than all user's list");
                }
            }
        }

        processNodeInfo(allUser, senderNodeInfo.getUserId());
        // received userList update
        updateRoutingTableAndUI(btUserNodeInfoList);
        updateRoutingTableAndUI(btMeshUserNodeInfoList);

        passTheseUserToOtherTransport(senderInfo, allUser);

        passTheseUsersToAdhoc(allUser, senderInfo);

        // sender update
        updateRoutingTableAndUIForSender(senderNodeInfo);


        if (mNodeConnectivityChangeListener != null) {
            mNodeConnectivityChangeListener.onNodeConnectionChanged(true);
        }

    }

    /**
     * passTheseUserToOtherTransport
     *
     * @param senderInfo senderID
     * @param allUser    connectedUses
     */

    private void passTheseUserToOtherTransport(String senderInfo, ConcurrentLinkedQueue<NodeInfo> allUser) {
        if (RouteManager.getInstance().isWifiUserConnected()) {
            // passThese Users To WifiClients
            passTheseUsersToWifiClients(allUser, senderInfo);
        }
        if (RouteManager.getInstance().isBtUserConnected()) {
            // pass These Users To BleUser
            passTheseUsersToBleUser(allUser);
        }


    }


    @Override
    public void onReceivedDirectUserFromAdHocBroadcaster(String senderInfo, String btUser,
                                                         String btMeshUser, String wifiUser,
                                                         String wifiMeshUser) {
        NodeInfo senderNodeInfo = GsonUtil.on().itemFromJson(senderInfo);
        if (senderInfo == null) return;

        if (TextUtils.isEmpty(senderNodeInfo.getUserId())) return;

        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(senderNodeInfo.getUserId());

        if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.INTERNET) {
            // previously internet user
            // process next
            MeshLog.i("InternetMsg RemovePeerConnection for previous Internet User from adhoc broadcaster");
//            PeerConnectionHolder.closeSingleConnection(routingEntity.getAddress());
        }
        // sender update
        updateRoutingTableAndUIForSender(senderNodeInfo);

        MeshLog.i("onReceivedDirectUserFromAdHocBroadcaster from:: " + senderNodeInfo.toString());

        ConcurrentLinkedQueue<NodeInfo> btUserNodeInfoList = GsonUtil.on().queueFromJson(btUser);
        ConcurrentLinkedQueue<NodeInfo> btMeshUserNodeInfoList = GsonUtil.on().queueFromJson(btMeshUser);
        ConcurrentLinkedQueue<NodeInfo> wifiUserNodeInfoList = GsonUtil.on().queueFromJson(wifiUser);
        ConcurrentLinkedQueue<NodeInfo> wifiMeshUserNodeInfoList = GsonUtil.on().queueFromJson(wifiMeshUser);

        ConcurrentLinkedQueue<NodeInfo> allUser = new ConcurrentLinkedQueue<>();

        if (CollectionUtil.hasItem(btUserNodeInfoList)) {
            MeshLog.e(" Connected btUserNodeInfoList List :" + btUserNodeInfoList.toString());
            allUser.addAll(btUserNodeInfoList);
        }
        if (CollectionUtil.hasItem(btMeshUserNodeInfoList)) {
            MeshLog.e(" Connected btMeshUserNodeInfoList List :" + btMeshUserNodeInfoList.toString());
            allUser.addAll(btMeshUserNodeInfoList);
        }
        if (CollectionUtil.hasItem(wifiUserNodeInfoList)) {
            MeshLog.e(" Connected adhocUserNodeInfoList List :" + wifiUserNodeInfoList.toString());
            allUser.addAll(wifiUserNodeInfoList);
        }
        if (CollectionUtil.hasItem(wifiMeshUserNodeInfoList)) {
            MeshLog.e(" Connected adhocMeshUserNodeInfoList List :" + wifiMeshUserNodeInfoList.toString());
            allUser.addAll(wifiMeshUserNodeInfoList);
        }

        processNodeInfo(allUser, senderNodeInfo.getUserId());

        // received userList update
        updateRoutingTableAndUI(allUser);

        allUser.add(senderNodeInfo);

        passTheseUserToOtherTransport(senderInfo, allUser);


        if (mNodeConnectivityChangeListener != null) {
            //To trigger search immediately
            mNodeConnectivityChangeListener.onNodeConnectionChanged(true);
        }

        //Got response from master so querying is done
        resetPeerQueryState();
    }

    /**
     * passTheseUsersToWifiClients
     *
     * @param allUser    connected users
     * @param senderInfo senderID
     */
    private void passTheseUsersToWifiClients(ConcurrentLinkedQueue<NodeInfo> allUser, String senderInfo) {

        if (!CollectionUtil.hasItem(allUser)) return;

        if (TextUtils.isEmpty(senderInfo)) return;

        MeshLog.i("passTheseUsersToWifiClients :: " + allUser.toString());
        Iterator it = allUser.iterator();
        NodeInfo nodeInfo;
        while (it.hasNext()) {
            nodeInfo = (NodeInfo) it.next();
            nodeInfo.setHopId(myNodeId);
            nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
        }

        String userListJson = GsonUtil.on().toJsonFromQueue(allUser);

        WiFiDiscoverMessage.DataPuller dataPuller = () -> JsonDataBuilder.buildMeshMessage(myNodeId, myNodeId, userListJson);
        List<RoutingEntity> liveWifiConnectionList = RouteManager.getInstance().getWifiUser();
        MeshLog.i(" Live Wifi ConnectionList  ::" + liveWifiConnectionList.toString());
        if (CollectionUtil.hasItem(liveWifiConnectionList)) {
            if (P2PUtil.isMeGO()) {
                for (RoutingEntity rEntity : liveWifiConnectionList) {
                    // exclude the recent sender
                    if (rEntity.getAddress().equals(senderInfo)) continue;
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(rEntity.getIp(), dataPuller);
                    messageDispatcher.addSendMessage(baseMeshMessage);
                }
            } else {
                BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(Constant.MASTER_IP_ADDRESS, dataPuller);
                messageDispatcher.addSendMessage(baseMeshMessage);
            }

        }
    }


    /**
     * passTheseUsersToAdhoc
     *
     * @param allUser    connected users
     * @param senderInfo senderID
     */
    private void passTheseUsersToAdhoc(ConcurrentLinkedQueue<NodeInfo> allUser, String senderInfo) {

        if (!CollectionUtil.hasItem(allUser)) return;

        if (TextUtils.isEmpty(senderInfo)) return;

        MeshLog.i("passTheseUsersToWifiClients :: " + allUser.toString());
        Iterator it = allUser.iterator();
        NodeInfo nodeInfo;
        while (it.hasNext()) {
            nodeInfo = (NodeInfo) it.next();
            nodeInfo.setUserType(RoutingEntity.Type.HB);
        }

        String userListJson = GsonUtil.on().toJsonFromQueue(allUser);

        WiFiDiscoverMessage.DataPuller dataPuller = () -> JsonDataBuilder.buildMeshMessage(myNodeId, myNodeId, userListJson);
        List<RoutingEntity> liveAdhocConnectionList = RouteManager.getInstance().getAdhocUser();
        MeshLog.i(" Live Adhoc ConnectionList  ::" + liveAdhocConnectionList.toString());
        if (CollectionUtil.hasItem(liveAdhocConnectionList)) {
            if (P2PUtil.isMeGO()) {
                for (RoutingEntity rEntity : liveAdhocConnectionList) {
                    // exclude the recent sender
                    if (rEntity.getAddress().equals(senderInfo)) continue;
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(rEntity.getIp(), dataPuller);
                    messageDispatcher.addSendMessage(baseMeshMessage);
                }
            } else {
                BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(Constant.MASTER_IP_ADDRESS, dataPuller);
                messageDispatcher.addSendMessage(baseMeshMessage);
            }

        }
    }

    /**
     * sendRequest
     *
     * @param myId      device Own ID
     * @param missingIp data missed IP
     */
    public int sendRequest(String myId, String missingIp) {
        MeshLog.i("sendRequest through IP on:: " + missingIp);
        BaseMeshMessage meshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(
                missingIp, () -> JsonDataBuilder.prepareWifiHelloPacketAsServiceListener(myNodeId,
                        GsonUtil.getUserInfo(), myId, JsonDataBuilder.INFO_REQUEST));
        meshMessage.mMaxRetryCount = 2;
        return messageDispatcher.addSendMessage(meshMessage);
    }

    /**
     * sendMyInfo
     *
     * @param selfIp        device Own IP
     * @param masterAddress Connected master's ID
     * @return
     */

    public int sendMyInfo(String selfIp, String masterAddress) {

        if (AddressUtil.isValidIPAddress(selfIp) && AddressUtil.isValidIPAddress(masterAddress)) {
            MeshLog.v("My nodeID:: " + myNodeId);
            MeshLog.i(" Start user scan subnet; Self IP:: " + selfIp + " Target IP::" + masterAddress);
            BaseMeshMessage meshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(
                    masterAddress, () -> JsonDataBuilder.prepareWifiHelloPacketAsServiceListener(myNodeId,
                            GsonUtil.getUserInfo(), selfIp, JsonDataBuilder.ADHOC_HELLO_MASTER));
            int mQueryingPeerMessageId = messageDispatcher.addSendMessage(meshMessage);

            return mQueryingPeerMessageId;
        } else {
            Log.e("Jmdnslog", "Invalid ip=" + masterAddress);
        }
        return BaseMeshMessage.DEFAULT_MESSAGE_ID;
    }


    public void onDirectUserDisconnect(RoutingEntity disconnectedEntity) {


        if (disconnectedEntity != null && disconnectedEntity.getType() == RoutingEntity.Type.HB) {

            MeshLog.i("[AdHoc] onDirectUserDisconnect::" + disconnectedEntity.toString());

            List<RoutingEntity> offlineEntities = RouteManager.getInstance().updateNodeAsOffline(null, disconnectedEntity.getAddress());


            // remove from own end
            if (CollectionUtil.hasItem(offlineEntities)) {
                // update BT name
                if (mNodeConnectivityChangeListener != null) {
                    mNodeConnectivityChangeListener.onNodeConnectionChanged(false);
                }

                removeOfflineEntitiesFromUI(offlineEntities);

                passOfflineNodesToWifi(offlineEntities);

                passOfflineEntitiesToBT(offlineEntities);

                passOfflineEntitiesToAdhocUser(offlineEntities);

            }


        } else {
            List<RoutingEntity> offlineEntities = RouteManager.getInstance().updateNodeAsOffline(null, disconnectedEntity.getAddress());
            if (CollectionUtil.hasItem(offlineEntities)) {
                MeshLog.i("BT Mesh offline fails:" + offlineEntities.toString());

            }
        }
    }


    @Override
    public void onMeshLinkFound(String senderId, String hopNodeId, String userListJson) {

        ConcurrentLinkedQueue<NodeInfo> nodesInfo = GsonUtil.on().queueFromJson(userListJson);
        MeshLog.e("onMeshLinkFound ::" + "Sender::" + AddressUtil.makeShortAddress(senderId) + "Hop:: " + AddressUtil.makeShortAddress(hopNodeId));

        if (!CollectionUtil.hasItem(nodesInfo)) {
            MeshLog.e("onMeshUsersConnect called with empty data list");
            return;
        }

        processNodeInfo(nodesInfo, senderId);

        MeshLog.i("[AdHoc] onMeshLinkFound :: " + nodesInfo.toString());
        if (CollectionUtil.hasItem(nodesInfo)) {
            updateRoutingTableAndUI(nodesInfo);
            //<<<<-----
            passTheseUsersToBleUser(nodesInfo);
            if (P2PUtil.isMeGO()) {
                MeshLog.i(" This device is in master mode, update client with mesh-connect");
                passThisUserToWifiUsers(myNodeId, nodesInfo);
            }

        } else {
            MeshLog.e("[Adhoc] User's hop is dead; hop" + AddressUtil.makeShortAddress(senderId));
        }


    }

    @Override
    public void onMeshLinkDisconnect(String nodeIds, String forwarderId) {


        MeshLog.i("[Disconnect] On Adhoc MeshLink" + nodeIds + "forwarder::" +
                AddressUtil.makeShortAddress(forwarderId));
        List<RoutingEntity> offlineRoutingEntities = new ArrayList<>();
        List<RoutingEntity> updateNodeAsOffline = new ArrayList<>();
        // remove from my end
        try {
            JSONArray jsonArray = new JSONArray(nodeIds);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = (JSONObject) jsonArray.get(i);
                String meshId = jo.getString("id");
                MeshLog.w("Disconnected Adhoc Mesh ->>" + AddressUtil.makeShortAddress(meshId));
                RoutingEntity offlineNode = RouteManager.getInstance().getRoutingEntityByAddress(meshId);
                if (offlineNode != null) {
                    MeshLog.i("offlineNode details::" + offlineNode.toString());
                    if (offlineNode.getType() == RoutingEntity.Type.HB) {
                        MeshLog.e("Adhoc direct connection leave");
                        // direct connection leave
                        updateNodeAsOffline = RouteManager.getInstance().updateNodeAsOffline("", meshId);
                    } else if (offlineNode.getType() == RoutingEntity.Type.HB_MESH) {
                        MeshLog.i(" Adhoc Mesh connection leave");
                        // Mesh connection leave
                        updateNodeAsOffline = RouteManager.getInstance().updateNodeAsOffline(forwarderId, meshId);
                    } else {
                        MeshLog.e("[INVALID] connect leave");
                    }
                }

                if (updateNodeAsOffline != null && !updateNodeAsOffline.isEmpty()) {
                    MeshLog.i("[Adhoc] updateNodeAsOffline list ::" + "" + updateNodeAsOffline.size());

                } else {
                    MeshLog.e("[Adhoc] EMPTY LIST");
                }
                if (CollectionUtil.hasItem(updateNodeAsOffline)) {
                    offlineRoutingEntities.addAll(updateNodeAsOffline);
                }
                if (!offlineRoutingEntities.isEmpty()) {
                    MeshLog.i("[Adhoc] Offline list ::" + "" + offlineRoutingEntities.size());
                }

                /*if (internetTransport != null) {
                    internetTransport.onBuyerDisconnected(meshId);
                }*/
            }
        } catch (JSONException e) {
            MeshLog.e("JSON EXCEPTION " + e.getMessage());
        }
        removeOfflineEntitiesFromUI(offlineRoutingEntities);

        passOfflineNodesToWifi(offlineRoutingEntities);

        passOfflineEntitiesToBT(offlineRoutingEntities);

    }

    private void removeOfflineEntitiesFromUI(List<RoutingEntity> offlineRoutingEntities) {
        for (RoutingEntity routingEntity : offlineRoutingEntities) {
            linkStateListener.onUserDisconnected(routingEntity.getAddress());
            linkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());
            connectionLinkCache.removeNodeInfo(routingEntity.getAddress());
            /*if (internetTransport != null) {
                outputThread.dispatch(() -> internetTransport.onBuyerDisconnected(routingEntity.getAddress()));
            }*/
        }
    }

    @Override
    public void onMessageReceived(String sender, String receiver, String messageId, byte[] frameData, String ipAddress, String immediateSender) {
        if (receiver.equals(myNodeId)) {

            try {
                JSONObject js = new JSONObject(new String(frameData));
                byte[] msg_data = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_DATA).getBytes();

                if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
//                    internetTransport.processInternetOutgoingMessage(sender, originalReceiver, messageId, msg_data);
                } else if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE) {
                    String sellerId = js.getString(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS);
                    String ackBody = Util.buildInternetSendingAckBody(sender);
                    outputThread.dispatch(() -> linkStateListener.onMessageReceived(sender, msg_data));

                    sendMessageAck(receiver, sellerId, messageId, Constant.MessageStatus.RECEIVED, ackBody);
                } else {
                    outputThread.dispatch(() -> linkStateListener.onMessageReceived(sender, msg_data));

                    MeshLog.v("(-) MSG ACK SEND" + sender.substring(sender.length() - 3) + "-> "
                            + receiver.substring(receiver.length() - 3) + "-->" + messageId);

                    if (!TextUtils.isEmpty(messageId)) {
                        String ackBody = Util.buildLocalAckBody();
                        sendMessageAck(receiver, sender, messageId, Constant.MessageStatus.RECEIVED, ackBody);
                    }

                }
            } catch (JSONException e) {
                MeshLog.p("JSONException  " + e.getMessage());
            }
        } else {
            int transferId = sendMessageToOtherMesh(sender, receiver, messageId, frameData);

              /*
              This listener is used to get the middle layer data.
              Mean the persons data who are currently forwarding the data
              ANd fo this we are parsing main Data from FrameData object.
              lIKE self data

              Another benefit is the user can get count or other appreciation by listening

              he is currently played forwarder role
             */
            if (forwardListener != null) {
                try {
                    JSONObject js = new JSONObject(new String(frameData));
                    byte[] msg_data = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_DATA).getBytes();
                    forwardListener.onMessageForwarded(sender, receiver, messageId, transferId, msg_data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Message send to other mesh
     *
     * @param sender    sender Address
     * @param receiver  receiver Address
     * @param messageId messageID
     * @param data      sending data
     */
    private int sendMessageToOtherMesh(String sender, String receiver, String messageId, byte[] data) {
        int transferId = 0;
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        if (routingEntity == null) {
            MeshLog.e("Routing Entity NULL on sendMessageToOtherMesh");
            return transferId;
        }

        byte[] message = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);

        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            transferId = wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            transferId = sendAdhocMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                transferId = messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
            } else {
                MeshLog.w(" BLE LINK NOT FOUND FOR SENDING MSG IN MESH");
            }
        } else {
            MeshLog.v("WT User not found to send the local message");

            // Internet message process part is above
//            internetTransport.processInternetOutgoingMessage(sender, receiver, messageId, data);
        }

        return transferId;
    }


    private void passOfflineNodesToWifi(List<RoutingEntity> offlineRoutingEntities) {
        if (!RouteManager.getInstance().isWifiUserConnected()) return;
        if (CollectionUtil.hasItem(offlineRoutingEntities)) {
            String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);

            // send to wifi users if master
            if (P2PUtil.isMeGO()) {
                MeshLog.i(" This device is in master mode, update client with mesh-disconnect");
                List<RoutingEntity> liveWifiConnectionList = RouteManager.getInstance().getWifiUser();
                MeshLog.i(" LiveWifiConnectionList  -->" + liveWifiConnectionList.toString());
                if (CollectionUtil.hasItem(liveWifiConnectionList)) {
                    for (RoutingEntity rEntity : liveWifiConnectionList) {
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(rEntity.getIp(), () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList, myNodeId));
                        messageDispatcher.addSendMessage(baseMeshMessage);
                    }
                }
            }
            // send to master if client
            else {
                BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(MASTER_IP_ADDRESS, () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                messageDispatcher.addSendMessage(baseMeshMessage);
            }
        }
    }

    private String buildNodeIdListJson(List<RoutingEntity> routingEntities) {
        try {
            if (CollectionUtil.hasItem(routingEntities)) {
                JSONArray jsonArray = new JSONArray();
                for (RoutingEntity routingEntity : routingEntities) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", routingEntity.getAddress());
                    jsonArray.put(jsonObject);
                }
                return jsonArray.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void passOfflineEntitiesToBT(List<RoutingEntity> offlineRoutingEntities) {
        if (!RouteManager.getInstance().isBtUserConnected()) return;
        String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);
        // send to ble users
        List<BleLink> bleDirectLinks = connectionLinkCache.getDirectBleLinks();
        if (CollectionUtil.hasItem(offlineRoutingEntities)) {
            MeshLog.i(" Leave node list send :: " + leaveNodeList);
            MeshLog.i(" adhoc Node leaved notify to ble user =" + bleDirectLinks.size());
            BTDiscoveryMessage btDiscoveryMessage;
            for (BleLink link : bleDirectLinks) {
                btDiscoveryMessage = MessageBuilder.buildMeshBtDiscoveryMessage(link, () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                messageDispatcher.addSendMessage(btDiscoveryMessage);
            }
        }
    }

    /**
     * Pass These Users To Adhoc User
     *
     * @param offlineRoutingEntities offline Entities
     */

    private void passOfflineEntitiesToAdhocUser(List<RoutingEntity> offlineRoutingEntities) {
        if (!RouteManager.getInstance().isAdhocUserConneted()) return;
        String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);

        List<RoutingEntity> routingEntities = RouteManager.getInstance().getAdhocUser();

        if (CollectionUtil.hasItem(routingEntities)) {

            for (RoutingEntity routingEntity : routingEntities) {
                //TransportManager.getInstance().isUserAvailable(routingEntity.getAddress());
                BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(),
                        () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                messageDispatcher.addSendMessage(baseMeshMessage);
            }
        }


    }

    @Override
    public void onReceivedMsgAck(String sender, String receiver, String messageId, int status, String ackBody, String ipAddress) {
        MeshLog.v("(-) MSG ACK RECEIVED " + sender.substring(sender.length() - 3) + "-->  " + receiver.substring(receiver.length() - 3) + "--> " + messageId + "status : " + status);
        if (receiver.equals(myNodeId)) {
            try {
                JSONObject js = new JSONObject(ackBody);

                if (js.getInt(PurchaseConstants.JSON_KEYS.ACK_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND_ACK) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
//                    internetTransport.sendBuyerReceivedAck(sender, originalReceiver, messageId, status);
                } else {
                    outputThread.dispatch(() -> linkStateListener.onMessageDelivered(messageId, status));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            sendMessageAckToOtherMesh(sender, receiver, messageId, status, ackBody);
        }

    }


    @Override
    public void onPaymentDataReceived(String senderId, String receiver, String messageId, byte[] payMesg) {

        MeshLog.p("onPaymentDataReceived");
        if (receiver.equals(myNodeId)) {
            MeshLog.v("my message");
            outputThread.dispatch(() -> linkStateListener.onMessagePayReceived(senderId, payMesg));
            sendPayMessageAck(receiver, senderId, messageId);
        } else {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (routingEntity != null) {
                MeshLog.v("(P) RoutingEntity" + routingEntity.toString());
                byte[] msgBody = JsonDataBuilder.buildPayMessage(senderId, receiver, messageId, payMesg);

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("(P) sendMessage Wifi user");
                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), msgBody);
                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    sendPayMessage(routingEntity.getIp(), msgBody);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    MeshLog.v("(P) sendMessage ble user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, msgBody));
                    } else {
                        MeshLog.v("(P) BLE LINK NOT FOUND");
                    }
                }
            } else {
                MeshLog.v(" (P) sendMessage User does not exist in routing table");
            }
        }
    }

    @Override
    public void onPaymentAckReceived(String sender, String receiver, String messageId) {
        MeshLog.v("onPaymentDataReceived");
        if (receiver.equals(myNodeId)) {
            MeshLog.v("my message ack");
            outputThread.dispatch(() -> linkStateListener.onPayMessageAckReceived(sender, receiver, messageId));
        } else {
            sendPayMessageAck(sender, receiver, messageId);
        }
    }

    private void sendPayMessageAck(String sender, String receiver, String messageId) {

        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        if (routingEntity != null) {
            MeshLog.v("(P ack) RoutingEntity" + routingEntity.toString());
            byte[] msgBody = JsonDataBuilder.buildPayMessageAck(sender, receiver, messageId);
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                MeshLog.v("(P) sendMessage Wifi user");
                wifiTransPort.sendMeshMessage(routingEntity.getIp(), msgBody);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendPayMessage(routingEntity.getIp(), msgBody);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                MeshLog.v("(P) sendMessage ble user");
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, msgBody));
                } else {
                    MeshLog.v("(P) BLE LINK NOT FOUND");
                }
            }
        } else {
            MeshLog.v(" (P) sendMessage User does not exist in routing table");
        }
    }


    /**
     * Send Message Ack To Other Mesh
     *
     * @param sender    sender Address
     * @param receiver  receiver Address
     * @param messageId message ID
     * @param status    status of the message
     * @param ackBody   acknowledgement details
     */
    private void sendMessageAckToOtherMesh(String sender, String receiver, String messageId, int status, String ackBody) {
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        if (routingEntity == null) {
            MeshLog.e("Routing Entity NULL on sendMessageToOtherMesh");
            return;
        }
        byte[] data = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);
        MeshLog.i(" Send message =" + routingEntity.toString());
        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            wifiTransPort.sendMeshMessage(routingEntity.getIp(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            sendAdhocMessage(routingEntity.getIp(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, data));
            } else {
                MeshLog.w(" BLE LINK NOT FOUND");
            }
        } else {
//            internetTransport.sendBuyerReceivedAck(sender, receiver, messageId, status);
        }

    }


    @Override
    public void onReceiverInternetUserLocally(String sender, String receiver, String sellerId, String userList) {
        MeshLog.v("Adhoc onReceiverInternetUserLocally =" + receiver);
        if (receiver.equalsIgnoreCase(myNodeId)) {

//            outputThread.dispatch(() -> linkStateListener.onCurrentSellerId(sellerId));
            /*if (internetTransport.amIDirectUser()) {
                return;
            }*/

            ConcurrentLinkedQueue<NodeInfo> userNodeInfoList = GsonUtil.on().queueFromJson(userList);
            //MeshLog.v("Wifi transport interent user ids =" + idArray);

            for (NodeInfo item : userNodeInfoList) {

                if (TextUtils.isEmpty(item.getUserId()) || item.equals(myNodeId)) {
                    continue;
                }


                if (RouteManager.getInstance().isOnline(item.getUserId())) {
                    outputThread.dispatch(() -> linkStateListener.onRemoteUserConnected(item.getUserId(), item.getPublicKey()));
                    continue;
                }
                RoutingEntity routingEntity = new RoutingEntity(item.getUserId());
                routingEntity.setOnline(true);
                routingEntity.setType(RoutingEntity.Type.INTERNET);
                routingEntity.setHopAddress(sellerId);
                boolean updated = RouteManager.getInstance().updateRoute(routingEntity);
                if (updated) {
                    //NodeInfo nodeInfo = new NodeInfo(item, "interNetSSID", "internetLink", "public key", PreferencesHelper.MESH_USER, Constant.UserTpe.INTERNET);
                    item.setUserType(RoutingEntity.Type.INTERNET);
                    //item.setBleName("");
                    //item.setSsidName("");
                    addNodeInformation(item.getUserId(), item);
                    MeshLog.v("Adhoc  transport internet user ids send success=" + item);

                    outputThread.dispatch(() -> linkStateListener.onRemoteUserConnected(item.getUserId(), item.getPublicKey()));
                }
            }
        } else {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (routingEntity != null) {
                byte[] internetIdList = JsonDataBuilder.buildInternetUserIds(myNodeId, receiver, sellerId, userList);

                MeshLog.v("(-) RoutingEntity" + routingEntity.toString());
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("senUserList Wifi user");
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> internetIdList);
                    messageDispatcher.addSendMessage(baseMeshMessage);

                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    sendAdhocMessage(routingEntity.getIp(), internetIdList);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    MeshLog.v("senUserList ble user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> internetIdList));
                    } else {
                        MeshLog.v("senUserList BLE LINK NOT FOUND");
                    }
                }
            } else {
                MeshLog.v("senUserList User does not exist in routing table");
            }
        }
    }

    @Override
    public void onInternetUserLeave(String sender, String receiver, String userList) {
        if (receiver.equals(myNodeId)) {

            /*if (internetTransport.amIDirectUser())
                return;*/

            String[] userIdArray = TextUtils.split(userList, "@");

            for (String id : userIdArray) {
                linkStateListener.onUserDisconnected(id);
                //RouteManager.getInstance().updateNodeAsOffline(id);

                connectionLinkCache.removeNodeInfo(id);
            }

            RouteManager.getInstance().makeUserOfline(Arrays.asList(userIdArray));

        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);

            if (routingEntity != null) {
                byte[] userListMessage = JsonDataBuilder.prepareInternetLeaveMessage(sender, receiver, userList);
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.mm("[Internet] senUserList Wifi user");
                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), userListMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    sendAdhocMessage(routingEntity.getIp(), userListMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {

                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        MeshLog.mm("[Internet] senUserList ble user");
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> userListMessage));
                        //sendEventQueue.execute(() -> bleLink.sendMeshMessage(userListMessage));
                    } else {
                        MeshLog.mm("[Internet] senUserList BLE LINK NOT FOUND");
                    }
                }
            }
        }
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
        String receiver = handshakeInfo.getReceiverId();

        if (myNodeId.equals(receiver)) {
            linkStateListener.onHandshakeInfoReceived(handshakeInfo);

        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (routingEntity == null) {
                MeshLog.e("Routing Entity NULL on sendMessageToOtherMesh");
                return;
            }

            String handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);
            byte[] message = handshakeInfoText.getBytes();
            sendHopMessageToOthers(routingEntity, message);
        }
    }

    private void sendHopMessageToOthers(RoutingEntity routingEntity, byte[] message) {
        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            sendAdhocMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
            } else {
                MeshLog.w(" BLE LINK NOT FOUND FOR SENDING MSG IN MESH");
            }
        } else {
            MeshLog.v("WT User not found to send the local message");
        }
    }


    /**
     * Pass This User To Wifi Users
     *
     * @param sender       sender address
     * @param nodeInfoList connected node infoList
     */
    private void passThisUserToWifiUsers(String sender, ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        if (!CollectionUtil.hasItem(nodeInfoList)) return;
        MeshLog.i(" passThisUserToWifiUsers:: " + nodeInfoList.toString());
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getWifiUser();
        Iterator it = nodeInfoList.iterator();
        NodeInfo nodeInfo;
        while (it.hasNext()) {
            nodeInfo = (NodeInfo) it.next();
            nodeInfo.setHopId(sender);
            nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
        }
        String jsonString = GsonUtil.on().toJsonFromQueue(nodeInfoList);
        if (routingEntities != null && !TextUtils.isEmpty(jsonString)) {
            for (RoutingEntity routingEntity : routingEntities) {
                if (routingEntity != null) {
                    if (routingEntity.getAddress().equals(sender)) continue;
                    messageDispatcher.addSendMessage(MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(),
                            () -> JsonDataBuilder.buildMeshMessage(myNodeId, sender, jsonString)));
                }
            }
        } else {
            MeshLog.e("BT DIRECT CONNECTION NOT EXIST");
        }
    }

    @Override
    public void onGetInfoRequest(String senderInfo, String btUsers, String btMeshUsers) {
        NodeInfo nodeInfo = GsonUtil.on().itemFromJson(senderInfo);
        Log.e("Jmdnslog", "Received info request");
        if (nodeInfo == null) {
            Log.e("Jmdnslog", "Node info null =" + senderInfo);
            return;
        }

        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(nodeInfo.getUserId());

        if (routingEntity == null) {
            onReceivedDirectUserFromAdHocListener(senderInfo, btUsers, btMeshUsers);
            /*updateRoutingTableAndUIForSender(nodeInfo);
            passTheseUserToWifiClients(nodeInfo);
            passTheseUserToBleUser(nodeInfo);*/
        } else {
            sendMyInfo(myIpAddress, nodeInfo.getIpAddress());
        }
        Log.e("Jmdnslog", "Send response");
        //sendMyInfo(myIpAddress, nodeInfo.getIpAddress());
    }

    /**
     * updateRoutingTableAndUI
     *
     * @param nodeInfoList connected node info list
     */
    private void updateRoutingTableAndUI(ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        if (CollectionUtil.hasItem(nodeInfoList)) {
            MeshLog.i("GET ::" + nodeInfoList.toString());

            for (NodeInfo nodeInfo : nodeInfoList) {

                boolean isUpdated = updateRouteWithCurrentTime(nodeInfo);

                if (isUpdated && RouteManager.getInstance().isOnline(nodeInfo.getUserId())) {
                    addNodeInformation(nodeInfo.getUserId(), nodeInfo);
                    outputThread.dispatch(() -> linkStateListener.onLocalUserConnected(nodeInfo.getUserId(), nodeInfo.getPublicKey()));
                }
            }
        }

    }

    /**
     * sendMessageAck
     *
     * @param sender    sender address
     * @param receiver  receiver address
     * @param messageId message ID
     * @param status    status of the message
     * @param ackBody   acknowledgement of the message
     */

    public void sendMessageAck(String sender, String receiver, String messageId, int status, String ackBody) {
        byte[] ackMessage = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        if (routingEntity != null) {
            if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendAdhocMessage(routingEntity.getIp(), ackMessage);
            } else {
                MeshLog.e("[AdHoc] send Message Ack failed Routing type mismatch::" + routingEntity.toString());
            }
        } else {
            MeshLog.e("[AdHoc] send Message Ack failed Routing entity null");
        }
    }

    /**
     * updateRouteWithCurrentTime
     *
     * @param nodeInfo device node information
     */
    private boolean updateRouteWithCurrentTime(NodeInfo nodeInfo) {

        RoutingEntity routingEntity = new RoutingEntity(nodeInfo.getUserId());
        routingEntity.setOnline(true);
        routingEntity.setHopAddress(nodeInfo.getHopId());
        routingEntity.setIp(nodeInfo.getIpAddress());
        routingEntity.setType(nodeInfo.getUserType());
        //routingEntity.setNetworkName(nodeInfo.getSsidName());
        routingEntity.setTime( nodeInfo.mGenerationTime);
        return RouteManager.getInstance().updateRoute(routingEntity);

    }


    /**
     * addNodeInformation
     *
     * @param userId   user's address
     * @param nodeInfo user's node details
     */
    private void addNodeInformation(String userId, NodeInfo nodeInfo) {
        if (!TextUtils.isEmpty(userId)) {
            if (connectionLinkCache != null) {
                connectionLinkCache.addNodeInfo(userId, nodeInfo);
            } else {
                MeshLog.e("ConnectionLink cache NULL! in add info");
            }

        } else {
            MeshLog.e("UserId empty in add info");
        }

    }

    /**
     * Update Routing Table And UI For Sender
     *
     * @param sender sender's node info
     */
    private void updateRoutingTableAndUIForSender(NodeInfo sender) {

        WiFiDirectManagerLegacy wiFiDirectManagerLegacy = WiFiDirectManagerLegacy.getInstance();
        //Update routing table
        RoutingEntity routingEntity = new RoutingEntity(sender.getUserId());
        routingEntity.setHopAddress(null);
        routingEntity.setIp(sender.getIpAddress());
        routingEntity.setOnline(true);
        routingEntity.setType(RoutingEntity.Type.HB);

        boolean isUpdated = RouteManager.getInstance().updateRoute(routingEntity);

        if (isUpdated && RouteManager.getInstance().isOnline(sender.getUserId())) {
            addNodeInformation(sender.getUserId(), sender);
            outputThread.dispatch(() -> linkStateListener.onLocalUserConnected(sender.getUserId(), sender.getPublicKey()));
        }
    }


    /**
     * Process node info
     * <p>
     * Observe if the the connected node is a new connection or exist connection;
     * make decision based on this
     * </p>
     *
     * @param nodesInfo connected nodes' nodeInfo
     * @return close if needed
     */

    private void processNodeInfo(ConcurrentLinkedQueue<NodeInfo> nodesInfo, String senderId) {

        StringBuilder cycleWith = new StringBuilder();
        boolean isPossibleWrongCycle = false;

        for (NodeInfo nodeInfo : nodesInfo) {

            MeshLog.i("[Connect] meshId through ADHOC " +
                    AddressUtil.makeShortAddress(nodeInfo.getUserId()));
            //Routing table update
            if (!myNodeId.equals(nodeInfo.getUserId())) {

                //Validate cycle data
                RoutingEntity existingEntity = RouteManager.getInstance().getLocalOnlyRoutingEntityByAddress(
                        nodeInfo.getUserId());


                if (existingEntity == null) {//New Node. send new node info to all and update db

                } else if (existingEntity.getType() == RoutingEntity.Type.HB) {
                    if (nodeInfo.getUserType() == RoutingEntity.Type.HB_MESH) {
                        mPingExecutor.execute(new Pinger(nodeInfo.getIpAddress(), (ip, isReachable) -> {
                            if (isReachable) {
                                MeshLog.i(" [ADHOC] Redundant data in ADHOC ");
                                MeshLog.i("[Cycle-Adhoc] Existing connected node details :: " + existingEntity.toString());
                                MeshLog.i("[Cycle-Adhoc] New connected node details :: " + nodeInfo.toString());

                                nodesInfo.remove(nodeInfo);

                                return;
                            }
                        }, ADHOC_PING_NUMBER_OF_RETRY,
                                ADHOC_PING_TIMEOUT));
                    }
                } else if (existingEntity.getType() == RoutingEntity.Type.WiFi) {
                    //If the node was in WiFi, then we accept it over new channel as latest data
                    //true always. All earlier WiFi users would receive this node as WiFi
                    // mesh. So that, no counter measurement required.
                } else if (TextUtils.isEmpty(existingEntity.getHopAddress())) {
                    //If earlier entry was in WiFi direct then we accept that connection as WiFi
                    // direct might delay to report a connection. So not allowing to process that in
                    // cycle prevention

                    //Here we need to send WiFi connected nodes a route update message from WiFi
                    //to WiFi mesh
                    // FIXME: 1/21/2020


                    cycleWith.append(AddressUtil.makeShortAddress(existingEntity.getAddress())).
                            append(",");

                    //Existing direct connected node. send disconnect message and update db
                    MeshLog.i("[Cycle-Adhoc]" +
                            AddressUtil.makeShortAddress(existingEntity.getAddress())
                            + " is Existing direct connected node. Received from:" +
                            AddressUtil.makeShortAddress(senderId));
                    MeshLog.i("[Cycle-Adhoc] Existing connected node details :: " + existingEntity.toString());
                    MeshLog.i("[Cycle-Adhoc] New connected node details :: " + nodeInfo.toString());

                    if (nodesInfo.remove(nodeInfo)) {
                        updateRouteWithCurrentTime(nodeInfo);
                    }

                    switch (existingEntity.getType()) {
                        case RoutingEntity.Type.BT:
                            MeshLog.i("[Cycle-Adhoc] Disabling BT as:" +
                                    AddressUtil.makeShortAddress(existingEntity.getAddress()) +
                                    " received as mesh node");
                            //Delay as we expect the connection to be re-sync by other end so that no one
                            //receive and feel real disconnection at app layer
                            AndroidUtil.post(() -> BTManager.getInstance(context).disconnect(),
                                    Constant.Cycle.DISCONNECTION_MESSAGE_TIMEOUT);
                            observeMeshConnection(RoutingEntity.Type.BtMesh);

                            break;

                        case RoutingEntity.Type.WiFi:
                            //The case could be Me is GO and a node might disconnected from GO and
                            //again joined with currently connected adhoc (GO could not trace it's
                            // node release). Just update with latest Adhoc path path
                            break;

                        default:
                            isPossibleWrongCycle = disconnectMessagePropagate(senderId, isPossibleWrongCycle, nodeInfo, existingEntity);
                            break;
                    }
                } else {
                    MeshLog.i("[Cycle-Adhoc] Hopped node; disconnect hopped node");
                    //Hopped node. send route update message to only earlier hop and update db
                    RoutingEntity hoppedEntity = RouteManager.getInstance().getRoutingEntityByAddress
                            (existingEntity.getHopAddress());
                    if (hoppedEntity != null) {
                        MeshLog.i("[Cycle-Adhoc] Hopped node details: " + hoppedEntity.toString());
                        // disconnectMessagePropagate(senderId, isPossibleWrongCycle, nodeInfo, hoppedEntity);
                    }
                }
            } else {
                MeshLog.w(" Self id Discovered in WIFI mesh !!!!!!!!!! " +
                        AddressUtil.makeShortAddress(nodeInfo.getUserId()));
                nodesInfo.remove(nodeInfo);
            }

        }

        if (Text.isNotEmpty(cycleWith) && !isPossibleWrongCycle) {
            cycleLog(cycleWith.toString());
        }

    }

    private boolean disconnectMessagePropagate(String senderId, boolean isPossibleWrongCycle, NodeInfo nodeInfo, RoutingEntity existingEntity) {
        if (P2PUtil.isMeGO()) {
            //Ideally earlier connection is definitely WiFi as BT has this new single connection
            //only
            if (existingEntity.getType() == RoutingEntity.Type.WiFi) {
                String logText = "[Cycle-Adhoc] Disconnect message send from Master:" +
                        AddressUtil.makeShortAddress(myNodeId) + "-to:" +
                        AddressUtil.makeShortAddress(existingEntity.getAddress());
                MeshLog.i(logText);
                observeMeshConnection(RoutingEntity.Type.WifiMesh);
                //Delay as we expect the connection to be re-sync by other end so that no one
                //receive and feel real disconnection at app layer
                AndroidUtil.post(() -> {
                            BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(existingEntity.getIp(), JsonDataBuilder::buildDisconnectMessage);
                            messageDispatcher.addSendMessage(baseMeshMessage);
                        },
                        Constant.Cycle.DISCONNECTION_MESSAGE_TIMEOUT);
                // get connected wifi user
                List<RoutingEntity> connectedWifiList = RouteManager.getInstance().getAllWifiUsers();
                // build route-info update message
                byte[] data = JsonDataBuilder.buildRouteInfoUpdateMessage(nodeInfo.getUserId(), nodeInfo.getHopId(), nodeInfo.mGenerationTime);
                if (data != null && data.length > 0) {
                    for (RoutingEntity routingEntity : connectedWifiList) {
                        if (routingEntity.getAddress().equals(existingEntity.getAddress()) || routingEntity.getAddress().equals(senderId))
                            continue;

                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildMeshWiFiMessage(routingEntity.getIp(), data);

                        messageDispatcher.addSendMessage(baseMeshMessage);
                    }
                }
            }

        } else {
            isPossibleWrongCycle = true;
            MeshLog.w("[Cycle-Adhoc] Cycle has no effect as this is client. Possible " +
                    "wrong message received");
        }
        return isPossibleWrongCycle;
    }

    /**
     * Log cycle event
     */
    private void cycleLog(String cycleWith) {

        if (BuildConfig.DEBUG && Text.isNotEmpty(cycleWith)) {
            //speak
            String speechText = "Adhoc Cycle Formed for " + SharedPref.read(KEY_USER_NAME) + ". Model " +
                    DeviceInfo.getDeviceName();
            mMyTextSpeech.speak(speechText);

            //notification
            String cycleItems = cycleWith;
            cycleItems = cycleItems.substring(0, cycleItems.length() - 1);
            NotificationUtil.showNotification(context, "Adhoc Cycle", "Cycle formed with:" +
                    cycleItems);
        }
    }

    private void observeMeshConnection(int connectionType) {
        List<RoutingEntity> routingEntityList = RouteManager.getInstance().getUsersByType(connectionType);
        if (routingEntityList != null) {
            MeshLog.e("Observed list details:: " + routingEntityList.toString());
        }
    }


    /**
     * Send Hello Response To Client
     *
     * @param sender client info
     */
    private int sendHelloResponseToClient(NodeInfo sender, String userInfo, boolean shouldAvoidBtlist) {

        int messageId = messageDispatcher.addSendMessage(MessageBuilder.buildWiFiDiscoveryMeshMessage(
                sender.getIpAddress(), () -> JsonDataBuilder.prepareWifiHelloPacketAsAdHocBroadcaster(myNodeId,
                        userInfo, AddressUtil.getWiFiIpAddress(), sender.getUserId(), shouldAvoidBtlist)));
        MeshLog.i("prepareadhocHelloPacketAsMaster :: to::" + AddressUtil.makeShortAddress(
                sender.getUserId()));
        return messageId;
    }


    /**
     * pass These Users To Ble User
     *
     * @param nodeInfoList connected node info list
     */

    private void passTheseUsersToBleUser(ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        if (!CollectionUtil.hasItem(nodeInfoList)) return;
        MeshLog.i(" passThisUserToBleUser:: " + nodeInfoList.toString());
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getBtUsers();
        Iterator it = nodeInfoList.iterator();
        NodeInfo nodeInfo;
        while (it.hasNext()) {
            nodeInfo = (NodeInfo) it.next();
            nodeInfo.setHopId(myNodeId);
            nodeInfo.setUserType(RoutingEntity.Type.BtMesh);
        }
        String jsonString = GsonUtil.on().toJsonFromQueue(nodeInfoList);
        if (routingEntities != null && routingEntities.size() == 1 && !TextUtils.isEmpty(jsonString)) {
            for (RoutingEntity routingEntity : routingEntities) {
                BleLink link = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (link != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage(link,
                            () -> JsonDataBuilder.buildMeshMessage(myNodeId, myNodeId, jsonString)));
                }
            }
        } else {
            MeshLog.e("BT DIRECT CONNECTION NOT EXIST");
        }

    }

    /**
     * pass These Users To Ble User
     */


    public void pauseDiscovery() {
        MeshHttpServer.on().pauseAdhocDiscovery();
    }


    public void resetPeerQueryState() {
        mQueryingPeerMessageId = BaseMeshMessage.DEFAULT_MESSAGE_ID;
    }

    public boolean isQueryingPeer() {
        return mQueryingPeerMessageId != BaseMeshMessage.DEFAULT_MESSAGE_ID;
    }

    public boolean isAdhocEnabled() {
        return WiFiUtil.isConnectedWithAdhoc(context) && jmdnsResolver.isRunning();
    }

    /**
     * @return true if any how it is connecting or trying ot connect with any Broadcaster
     * or Searcher
     */
    public boolean isConnecting() {
        return mQueryingPeerMessageId != BaseMeshMessage.DEFAULT_MESSAGE_ID;
    }


    @Override
    public void onReceiveNewRole(String sender, String receiver, int role) {
        Log.e("User_role", "Receive user role in adhoc layer");
        if (myNodeId.equals(receiver)) {
            int previousRole = connectionLinkCache.setNewUserRole(sender, role);
            linkStateListener.onUserModeSwitch(sender, role, previousRole);
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (routingEntity == null) return;

            byte[] message = JsonDataBuilder.buildUserRoleSwitchMessage(sender, receiver, role);

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendAdhocMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                }
            }
        }
    }

    @Override
    public void onFileMessageReceived(String sender, String receiver, String messageId,String message, String immediateSender) {
        linkStateListener.onFileMessageReceived(sender, message);
    }

    @Override
    public void onBroadcastReceived(Broadcast broadcast) {
        if (linkStateListener != null) {

            if (broadcast.getReceiverId().equals(myNodeId)) {

                boolean isExist = linkStateListener.onBroadcastSaveAndExist(broadcast);

                if (isExist) {
                    linkStateListener.onReceivedAckSend(broadcast.getBroadcastId(), broadcast.getSenderId());
                } else {
                    if (TextUtils.isEmpty(broadcast.getContentPath())) {
                        linkStateListener.onBroadcastMessageReceive(broadcast);
                    } else {
                        linkStateListener.onBroadcastContentDetailsReceived(broadcast.getSenderId(), broadcast.getBaseMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {
        if (broadcastAck.getReceiverId().equals(myNodeId)) {
            linkStateListener.onBroadcastACKMessageReceived(broadcastAck);
        }
    }

    @Override
    public void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid, String password, int wifiNodeCount, boolean isFirstMessage) {

    }
}
