package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.LinkMode;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.datasharing.util.NotificationUtil;
import com.w3engineers.mesh.datasharing.util.PurchaseConstants;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.wifid.Pinger;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.tunnel.RemoteTransport;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.util.log.DeviceInfo;
import com.w3engineers.mesh.util.log.MyTextSpeech;
import com.w3engineers.mesh.wifi.TransportStateListener;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.w3engineers.mesh.util.Constant.KEY_USER_NAME;
import static com.w3engineers.mesh.util.Constant.MASTER_IP_ADDRESS;

/**
 * <h1>The Bluetooth Transport layer, which will manage all types Bluetooth communication</h1>
 * <p>Also managed communication with other transport also</p>
 */
public class BluetoothTransport implements ConnectionStateListener, MeshTransport {
    public static final String SECONDARY_BT_NAME = "DEF";
    private int appId;
    private String myNodeId;
    private LinkStateListener linkStateListener;
    public BluetoothClient bluetoothClient;
    private BluetoothServer bluetoothServer;
    private WifiTransPort wifiTransPort;
    private TransportStateListener transportStateListener;
    private ConnectionLinkCache connectionLinkCache;
//    private InternetTransport internetTransport;

    private RemoteTransport remoteTransport;
    private AdHocTransport adHocTransport;
    private TransportManagerX.NodeConnectivityChangeListener mNodeConnectivityChangeListener;
    private MessageDispatcher messageDispatcher;
    private MyTextSpeech mMyTextSpeech;
    private ExecutorService mPingExecutor;
    private TransportManagerX mRoleManager;
    private ForwardListener forwardListener;
//    private String myUserInfo;

    //private DispatchQueue outputThread = new DispatchQueue();
    /**
     * hold discovery sending message id. Reset upon receiving response from Master or Client or
     * disconnecting from BT network.
     */
    public volatile int mQueryingPeerMessageId = BaseMeshMessage.DEFAULT_MESSAGE_ID;

    public void setNodeConnectivityChangeListener(TransportManagerX.NodeConnectivityChangeListener
                                                          nodeConnectivityChangeListener) {
        mNodeConnectivityChangeListener = nodeConnectivityChangeListener;
    }

    public BluetoothTransport(Context context, int appId, String nodeId, LinkStateListener linkStateListener,
                              TransportStateListener listener, ConnectionLinkCache connectionLinkCache,
                              MessageDispatcher messageDispatcher) {
        this.appId = appId;
        this.myNodeId = nodeId;
        this.transportStateListener = listener;
        this.linkStateListener = linkStateListener;
        bluetoothServer = new BluetoothServer(nodeId, this/*, messageDispatcher*/);
        bluetoothClient = new BluetoothClient(nodeId, this, bluetoothServer/*, messageDispatcher*/);
        this.connectionLinkCache = connectionLinkCache;
        this.messageDispatcher = messageDispatcher;
        mMyTextSpeech = new MyTextSpeech(context);
        this.mPingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        mRoleManager = TransportManagerX.getInstance();
    }

    /********************************************/
    /******** Transport callback ****************/
    /********************************************/
    @Override
    public void start() {

        // Start BT server thread when the user role not INTERNET_USER

        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            bluetoothServer.starListenThread();
        }

        /*if (!connectionLinkCache.isBleUserConnected()
                && !RouteManager.getInstance().isBtUserConnected()
                && PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            // Random mode
            int existingMode = SharedPref.readInt(Constant.RANDOM_STATE);

            // On adhoc mode BT server will on after ADHOC process completion
            if (existingMode != TransportManagerX.MODE_ADHOC) {
                MeshLog.i(" BT server called from BT transport");
                bluetoothServer.starListenThread();
            } else {
                HandlerUtil.postBackground(() -> bluetoothServer.starListenThread(), 5000);
            }
        }*/
    }

    @Override
    public void stop() {
        if (bluetoothServer != null) {
            bluetoothServer.stopListenThread();
        }
        if (bluetoothClient != null) {
            bluetoothClient.stop();
        }
        //BTManager.getInstance(App.getContext()).disable();

        if (mPingExecutor != null) {
            mPingExecutor.shutdown();
        }
    }

    public void stopConnectionProcess() {
        if (bluetoothClient != null) {
            bluetoothClient.stopConnectionProcess();
        }
    }


    /********************************************/
    /******** end Transport callback ************/
    /********************************************/

    public <T> void setInterTransport(T... transPorts) {
        for (T item : transPorts) {
            if (item instanceof WifiTransPort) {
                this.wifiTransPort = (WifiTransPort) item;
            } /*else if (item instanceof InternetTransport) {
                this.internetTransport = (InternetTransport) item;
            }*/ else if (item instanceof AdHocTransport) {
                this.adHocTransport = (AdHocTransport) item;
            } else if (item instanceof ForwardListener) {
                this.forwardListener = (ForwardListener) item;
            }else if (item instanceof RemoteTransport) {
                this.remoteTransport = (RemoteTransport) item;
            }
        }
    }

    public void connectBleDevice(BluetoothDevice device, ConnectionState state) {
        bluetoothClient.createConnection(device, state);
    }


    public void disconnectBtMessageSocket() {
        connectionLinkCache.clearAllLinks();
    }

    /**
     * Received UserList from BT channel
     *
     * @param senderInfo       sender details
     * @param wifiUserList     senders connected wifi-list details
     * @param wifiMeshUserList senders connected wifi-mesh-list details
     * @param link             sender's connected linkNode
     * @param linkMode         sender's connected mode
     */
    @Override
    public void onReceivedDirectUserListInBt(String senderInfo, String wifiUserList,
                                             String wifiMeshUserList, BleLink link, LinkMode linkMode) {
        bluetoothServer.stopListenThread();

        NodeInfo sender = GsonUtil.on().itemFromJson(senderInfo);
        if (sender == null || myNodeId.equalsIgnoreCase(sender.getUserId())) return;

        MeshLog.i(" BLE CONNECTION TYPE:" + linkMode);

        //Validate redundant data receive from same source data
        if (checkSourceAndDestination(sender)) return;

        if (connectionLinkCache.isBleUserConnected() && RouteManager.getInstance().isBtUserConnected()) {
            MeshLog.e("[BT 2] Intentional disconnect occurred in BT");
            intentionalDisconnectId = sender.getUserId();
            link.disconnect();
            return;
        }

        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(sender.getUserId());

        if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.INTERNET) {
            // previously internet user
            // process next
            MeshLog.i("InternetMsg RemovePeerConnection for previous Internet User Bluetooth transport");
//            PeerConnectionHolder.closeSingleConnection(routingEntity.getAddress());
        }

        // update BT name
        if (mNodeConnectivityChangeListener != null) {
            mNodeConnectivityChangeListener.onNodeConnectionChanged(true);
        }

        StringBuilder log = new StringBuilder();

        processNodeInfo(sender, sender.getUserId(), log, false);

        if (Text.isNotEmpty(log)) {
            cycleLog(log.toString());
        }

        updateRoutingAndUI(link, sender);

        ConcurrentLinkedQueue<NodeInfo> allUser = new ConcurrentLinkedQueue<>();

        ConcurrentLinkedQueue<NodeInfo> wifiUserNodes = GsonUtil.on().queueFromJson(wifiUserList);
        if (CollectionUtil.hasItem(wifiUserNodes)) {
            allUser.addAll(wifiUserNodes);
            MeshLog.e(" Connected wifiUserNodeInfoList List :" + wifiUserNodes.toString());
        }

        ConcurrentLinkedQueue<NodeInfo> wifiMeshUserNodes = GsonUtil.on().queueFromJson(wifiMeshUserList);

        if (CollectionUtil.hasItem(wifiMeshUserNodes)) {
            allUser.addAll(wifiMeshUserNodes);
            MeshLog.e(" Connected wifiMeshUserNodeInfoList List :" + wifiMeshUserNodes.toString());
        }

        processNodeInfo(allUser, sender.getUserId());

        if (transportStateListener != null) {
            transportStateListener.onBluetoothChannelConnect(linkMode);
        } else {
            MeshLog.i("TransportStateListener NULL");
        }

        updateNewNodesInternally(sender, wifiUserNodes, wifiMeshUserNodes);

    }

    private void updateNewNodesInternally(NodeInfo senderInfo,
                                          ConcurrentLinkedQueue<NodeInfo> wifiUserNodes,
                                          ConcurrentLinkedQueue<NodeInfo> wifiMeshUserNodes) {

        //Adding all together
        ConcurrentLinkedQueue<NodeInfo> allNodeInfoList = new ConcurrentLinkedQueue<>();
        allNodeInfoList.add(senderInfo);
        if (CollectionUtil.hasItem(wifiUserNodes)) {
            allNodeInfoList.addAll(wifiUserNodes);
        }
        if (CollectionUtil.hasItem(wifiMeshUserNodes)) {
            allNodeInfoList.addAll(wifiMeshUserNodes);
        }

        updateNewNodesInternally(allNodeInfoList);

    }

    private void updateNewNodesInternally(ConcurrentLinkedQueue<NodeInfo> allNodeInfoList) {

        updateRoutingTableAndUI(allNodeInfoList);
        if (RouteManager.getInstance().isWifiUserConnected()) {
            passTheseUsersToWifiUser(allNodeInfoList);
        }
        if (RouteManager.getInstance().isAdhocUserConneted()) {

            passTheseUsersToAdhocUser(allNodeInfoList);
        }

    }

    private boolean checkSourceAndDestination(NodeInfo sender) {
        RoutingEntity existingEntity = RouteManager.getInstance().getLocalOnlyRoutingEntityByAddress(
                sender.getUserId());
        if (existingEntity != null && existingEntity.getType() == RoutingEntity.Type.BT
                && existingEntity.getType() == sender.getUserType()
                && existingEntity.getAddress().equals(sender.getUserId())) {
            MeshLog.e("[Bt] Same data has received from same connection ");
            return true;
        }
        return false;
    }

    public boolean isBtConnected() {
        return BleLink.getBleLink() != null;
    }

    public void resetPeerQueryState() {
        mQueryingPeerMessageId = BaseMeshMessage.DEFAULT_MESSAGE_ID;
    }

    public boolean isQueryingPeer() {
        return mQueryingPeerMessageId != BaseMeshMessage.DEFAULT_MESSAGE_ID;
    }


    private String intentionalDisconnectId;

    /**
     * Direct node is disconnected
     * 1. update UI and DB with the this node and depending nodes
     * 2. send nodeIds(direct + mesh) to wifi client; if master
     * 3. send nodeIds(direct + mesh) to master ; if client
     *
     * @param link disconnected link
     */

    @Override
    public void onDisconnectLink(Link link) {

        if (intentionalDisconnectId != null && intentionalDisconnectId.equals(link.getNodeId())) {
            intentionalDisconnectId = null;
            return;
        }
        MeshLog.i(" BT server called from onDisconnectLink");
        bluetoothServer.starListenThread();
        linkStateListener.onBTMessageSocketDisconnect(link.getNodeId());
        //Routing table update
        BleLink bleLink = (BleLink) link;
        String disconnectedAddress = bleLink.getNodeId();
        if (!TextUtils.isEmpty(disconnectedAddress)) {
            MeshLog.w("[Disconnect] BLE dir ->>" + AddressUtil.makeShortAddress(disconnectedAddress));
            RoutingEntity disconnectedEntity = RouteManager.getInstance().getRoutingEntityByAddress(disconnectedAddress);
            // Check disconnect node was a BT connection
            if (disconnectedEntity != null && disconnectedEntity.getType() == RoutingEntity.Type.BT) {

                List<RoutingEntity> offlineEntities = RouteManager.getInstance().updateNodeAsOffline(null, disconnectedAddress);


                // remove from own end
                if (CollectionUtil.hasItem(offlineEntities)) {
                    // update BT name
                    if (mNodeConnectivityChangeListener != null) {
                        mNodeConnectivityChangeListener.onNodeConnectionChanged(false);
                    }

                    removeOfflineEntitiesFromUi(offlineEntities);

                    passOfflineEntitiesToOthers(offlineEntities);
                    passOfflineEntitiesToAdhoc(offlineEntities);

                }

                //TODO if no problem occured following lines will be removed


                /*if (internetTransport != null) {
                    internetTransport.onBuyerDisconnected(link.getNodeId());
                }*/

            } else {
                if (disconnectedEntity != null) {
                    MeshLog.e("BT offline fails:" + disconnectedEntity.toString());
                }
            }

        }
    }

    private void passOfflineEntitiesToOthers(List<RoutingEntity> offlineEntities) {
        if (CollectionUtil.hasItem(offlineEntities)) {
            String leaveNodeList = buildNodeIdListJson(offlineEntities);
            List<RoutingEntity> liveWifiConnectionList = RouteManager.getInstance().getWifiUser();
            MeshLog.i(" LiveWifiConnectionList  -->" + liveWifiConnectionList.toString());
            MeshLog.i(" Leave node list send :: " + leaveNodeList);
            if (CollectionUtil.hasItem(liveWifiConnectionList)) {
                if (P2PUtil.isMeGO()) {
                    for (RoutingEntity rEntity : liveWifiConnectionList) {
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(rEntity.getIp(), () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                        messageDispatcher.addSendMessage(baseMeshMessage);
                    }
                } else {


                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(MASTER_IP_ADDRESS, () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                    messageDispatcher.addSendMessage(baseMeshMessage);
                }

            }
        }
    }


    private void passOfflineEntitiesToAdhoc(List<RoutingEntity> offlineEntities) {
        if (!RouteManager.getInstance().isAdhocUserConneted()) return;
        if (CollectionUtil.hasItem(offlineEntities)) {
            String leaveNodeList = buildNodeIdListJson(offlineEntities);
            List<RoutingEntity> liveWifiConnectionList = RouteManager.getInstance().getAdhocUser();
            MeshLog.i(" LiveAdhocConnectionList  -->" + liveWifiConnectionList.toString());
            MeshLog.i(" Leave node list send :: " + leaveNodeList);
            if (CollectionUtil.hasItem(liveWifiConnectionList)) {
                for (RoutingEntity rEntity : liveWifiConnectionList) {
                    //TransportManager.getInstance().isUserAvailable(rEntity.getAddress());
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(rEntity.getIp(), () -> JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
                    messageDispatcher.addSendMessage(baseMeshMessage);
                }
            }

        }
    }

    private void removeOfflineEntitiesFromUi(List<RoutingEntity> offlineEntities) {
        for (RoutingEntity routingEntity : offlineEntities) {
            //UI callback
            MeshLog.w("Disconnected BT:: ->>" + AddressUtil.makeShortAddress(
                    routingEntity.getAddress()));
            linkStateListener.onUserDisconnected(routingEntity.getAddress());
            linkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());

            MeshLog.e("BT disconnect; Node info removed ");
            connectionLinkCache.removeNodeInfo(routingEntity.getAddress());

            connectionLinkCache.removeDirectLink(routingEntity.getAddress());

            //TODO the following code should be in purchase manager
            /*if (internetTransport != null) {
                internetTransport.onBuyerDisconnected(routingEntity.getAddress());
            }*/

        }
    }

    /**
     * Mesh node is connected
     * 1. Update UI AND DB
     * 2. Pass This UserTo WifiUsers
     * ---> 1. If master send to all clients
     * ---> 2. If client send to Master
     *
     * @param sender       hopNodeId of discovered nodeId
     * @param userListJson userList in json
     */

    @Override
    public void onMeshLinkFound(String sender, String hopNodeId, String userListJson) {
        ConcurrentLinkedQueue<NodeInfo> nodesInfo = GsonUtil.on().queueFromJson(userListJson);
        if (!CollectionUtil.hasItem(nodesInfo)) {
            return;
        }
        MeshLog.i("[Connect] BLE MESH FOUND " + userListJson + " Hop =" +
                AddressUtil.makeShortAddress(sender));

        if (RouteManager.getInstance().isBtHopIdExist(sender)) {

            processNodeInfo(nodesInfo, sender);
            if (CollectionUtil.hasItem(nodesInfo)) {
                MeshLog.i("[BT] onMeshLinkFound :: " + nodesInfo.toString());

                updateNewNodesInternally(nodesInfo);
            }
        } else {
            MeshLog.e("[BT] User's hop is dead; hop" + sender.substring(sender.length() - 3));
        }
    }


    /**
     * Mesh node is disconnected
     * 1. update UI and DB with the nodeIds
     * 2. send nodeIds to wifi client; if master
     * 3. send nodeIds to master ; if client
     *
     * @param nodeIds     disconnect nodeIdList
     * @param forwarderId message forwarderId
     */

    @Override
    public void onMeshLinkDisconnect(String nodeIds, String forwarderId) {
        MeshLog.i("[Disconnect] On BLE MeshLink" + nodeIds + "forwarder::" +
                AddressUtil.makeShortAddress(forwarderId));
        List<RoutingEntity> offlineRoutingEntities = new ArrayList<>();
        // remove from my end
        try {
            JSONArray jsonArray = new JSONArray(nodeIds);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jo = (JSONObject) jsonArray.get(i);
                String meshId = jo.getString("id");
                MeshLog.w("Disconnected BLE Mesh ->>" + AddressUtil.makeShortAddress(meshId));
                List<RoutingEntity> updateNodeAsOffline = RouteManager.getInstance().updateNodeAsOffline(forwarderId, meshId);
                if (updateNodeAsOffline != null && !updateNodeAsOffline.isEmpty()) {
                    MeshLog.i("[BT] updateNodeAsOffline list ::" + "" + updateNodeAsOffline.size());

                } else {
                    MeshLog.e("[BT] EMPTY LIST");
                }
                if (CollectionUtil.hasItem(updateNodeAsOffline)) {
                    offlineRoutingEntities.addAll(updateNodeAsOffline);
                }
                if (!offlineRoutingEntities.isEmpty()) {
                    MeshLog.i("[BT] Offline list ::" + "" + offlineRoutingEntities.size());
                }


                /*if (internetTransport != null) {
                    internetTransport.onBuyerDisconnected(meshId);
                }*/
            }
        } catch (JSONException e) {
            MeshLog.e("JSON EXCEPTION " + e.getMessage());
        }
        for (RoutingEntity routingEntity : offlineRoutingEntities) {
            linkStateListener.onUserDisconnected(routingEntity.getAddress());
            linkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());
            connectionLinkCache.removeNodeInfo(routingEntity.getAddress());

            /*if (internetTransport != null) {
                internetTransport.onBuyerDisconnected(routingEntity.getAddress());
            }*/
        }

        passOfflinedNodesToWifi(offlineRoutingEntities);

        passOfflineEntitiesToAdhoc(offlineRoutingEntities);
    }

    private void passOfflinedNodesToWifi(List<RoutingEntity> offlineRoutingEntities) {
        if (CollectionUtil.hasItem(offlineRoutingEntities)) {
            String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);
            if (!RouteManager.getInstance().isWifiUserConnected()) return;

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

    /**
     * This method track any cycle for any node in the list and take corresponding counter
     * measurement
     *
     * @param nodesInfo
     * @param senderId
     */
    private void processNodeInfo(ConcurrentLinkedQueue<NodeInfo> nodesInfo, String senderId) {

        StringBuilder cycleWith = new StringBuilder();

        for (NodeInfo nodeInfo : nodesInfo) {
            if (processNodeInfo(nodeInfo, senderId, cycleWith, true)) {
                nodesInfo.remove(nodeInfo);
            }
        }

        if (Text.isNotEmpty(cycleWith)) {
            cycleLog(cycleWith.toString());
        }

    }

    /**
     * This method track any cycle for a node and take corresponding counter measurement
     *
     * @param nodeInfo
     * @param senderId
     * @param cycleWith
     * @param routeUpdatedStatus
     * @return whether this node should be removed from update list or not
     */
    private boolean processNodeInfo(NodeInfo nodeInfo, String senderId, StringBuilder cycleWith, boolean routeUpdatedStatus) {

        if (TextUtils.isEmpty(nodeInfo.getUserId())) {
            MeshLog.e(" Empty ID discovered in BT Mesh; return");
            return true;
        }

        if (nodeInfo.getUserId().equals(myNodeId)) {
            MeshLog.e(" Self ID discovered in BT Mesh; return");
            return true;
        }

        //Validate cycle data
        RoutingEntity existingEntity = RouteManager.getInstance().getLocalOnlyRoutingEntityByAddress(
                nodeInfo.getUserId());

        if (existingEntity == null) {//New Node. send new node info to all and update db
            //Nothing to do we process this new node
        } else {
            final String logText;
            if (TextUtils.isEmpty(existingEntity.getHopAddress())) {

                cycleWith.append(AddressUtil.makeShortAddress(existingEntity.getAddress())).
                        append(",");
                //Existing direct connected node. send disconnect message if GO and update db
                MeshLog.i("[Cycle-BT] Existing direct connected node. send disconnect " +
                        "message if GO and update db");
                MeshLog.i("[Cycle-BT] Existing connected node details :: " + existingEntity.toString());
                MeshLog.i("[Cycle-BT] New connected node details :: " + nodeInfo.toString());

                if (routeUpdatedStatus) {
                    updateRouteWithCurrentTime(nodeInfo);
                }

                switch (existingEntity.getType()) {
                    //This case should never happen. Kept to maintain similarity or track any
                    // unusual use case. Will remove it eventually
                    case RoutingEntity.Type.BT:
                        MeshLog.e("[Cycle-BT] Disabling BT as:" +
                                AddressUtil.makeShortAddress(existingEntity.getAddress())
                                + " received as mesh node. UNEXPECTED as BT received already " +
                                "direct BT connected node!!!");

                        //Delay as we expect the connection to be re-sync by other end so that no one
                        //receive and feel real disconnection at app layer
                        AndroidUtil.post(() -> BTManager.getInstance(App.getContext()).disconnect(),
                                Constant.Cycle.DISCONNECTION_MESSAGE_TIMEOUT);

                        //If anyhow this is to disconnect then we never count via nodes and
                        // expect those node to be sync by other hand
                        MeshLog.e("[Cycle-BT-sync]sender sent itself in meshlink. Sender:" +
                                AddressUtil.makeShortAddress(existingEntity.getHopAddress()));
                        cycleLog(cycleWith.toString());
                        observeMeshConnection(RoutingEntity.Type.BtMesh);
                        break;

                    //Other node would accept this connection if it has not seen yet, or
                    // disconnected from adhoc or.
                    //We are not visibly considering HB_Mesh although for any node if it is
                    // connected that will accept the connection as latest info, and the specified
                    // node with which it was connected by HB, BT or WiFi that will be taken into
                    //consideration by processing direct states
                    case RoutingEntity.Type.HB:
                        //We verify requesting node really is in Adhoc or not?
                        mPingExecutor.execute(new Pinger(nodeInfo.getIpAddress(),
                                (ip, isReachable) -> {
                                    if (isReachable) {
                                        MeshLog.e("BT disconnection processing as; isReachable:" + isReachable);
                                        //Within timeout*retry seconds count we will get know
                                        //requesting node still persist with the Adhoc
                                        //connection, so disconnect with this link to suppress
                                        //Adhoc
                                        BTManager.getInstance(App.getContext()).disconnect();
                                    } else {
                                        //Adhoc connection is really dead so accept the BT
                                        //connection
                                        ConcurrentLinkedQueue<NodeInfo> concurrentLinkedQueue =
                                                new ConcurrentLinkedQueue<>();
                                        concurrentLinkedQueue.add(nodeInfo);

                                        adHocTransport.onDirectUserDisconnect(
                                                existingEntity);
                                        updateNewNodesInternally(concurrentLinkedQueue);

                                        observeMeshConnection(RoutingEntity.Type.HB_MESH);

                                    }

                                }, AdHocTransport.ADHOC_PING_NUMBER_OF_RETRY,
                                AdHocTransport.ADHOC_PING_TIMEOUT));
                        //This timeout must be less than @link{BleLink#BT_TIMEOUT}

                        break;

                    default:
                        if (P2PUtil.isMeGO()) {
                            logText = "[Cycle-BT] Disconnect message send over Wifi: From Master:" +
                                    AddressUtil.makeShortAddress(myNodeId) + "-to:" +
                                    AddressUtil.makeShortAddress(existingEntity.getAddress());
                            //Ideally earlier connection is definitely WiFi as BT has this new single connection
                            //only
                            MeshLog.i(logText);
                            if (existingEntity.getType() == RoutingEntity.Type.WiFi && wifiTransPort != null) {
                                BaseMeshMessage baseMeshMessage = MessageBuilder.
                                        buildWiFiDiscoveryMeshMessage(existingEntity.getIp(),
                                                JsonDataBuilder::buildDisconnectMessage);
                                messageDispatcher.addSendMessage(baseMeshMessage);
                                observeMeshConnection(RoutingEntity.Type.WifiMesh);


                            }

                            // get connected wifi user
                            List<RoutingEntity> connectedWifiList = RouteManager.getInstance().getAllWifiUsers();
                            // build route-info update message
                            byte[] data = JsonDataBuilder.buildRouteInfoUpdateMessage(
                                    nodeInfo.getUserId(), nodeInfo.getHopId(),
                                    nodeInfo.mGenerationTime);

                            for (RoutingEntity routingEntity : connectedWifiList) {

                                if (routingEntity.getAddress().equals(
                                        existingEntity.getAddress()) ||
                                        routingEntity.getAddress().equals(senderId))
                                    continue;

                                BaseMeshMessage baseMeshMessage = MessageBuilder.
                                        buildMeshWiFiMessage(routingEntity.getIp(), data);

                                messageDispatcher.addSendMessage(baseMeshMessage);
                            }
                        }
                        break;
                }
                return true;
            }
        }

        return false;
    }

    private void observeMeshConnection(int connectionType) {
        List<RoutingEntity> routingEntityList = RouteManager.getInstance().getUsersByType(connectionType);
        if (routingEntityList != null) {
            MeshLog.e("Observed list details:: " + routingEntityList.toString());
        }
    }

    /**
     * Log cycle event
     */
    private void cycleLog(String cycleWith) {

        if (BuildConfig.DEBUG && Text.isNotEmpty(cycleWith)) {
            //speak
            String speechText = "BT Cycle Formed for " + SharedPref.read(KEY_USER_NAME) + ". Model " +
                    DeviceInfo.getDeviceName();
            mMyTextSpeech.speak(speechText);

            //notification
            String cycleItems = cycleWith;
            cycleItems = cycleItems.substring(0, cycleItems.length() - 1);
            NotificationUtil.showNotification(App.getContext(), "BT Cycle", "Cycle formed with:" +
                    cycleItems);
        }
    }

    private boolean updateRouteWithCurrentTime(NodeInfo nodeInfo) {

        RoutingEntity routingEntity = new RoutingEntity(nodeInfo.getUserId());
        routingEntity.setOnline(true);
        routingEntity.setHopAddress(nodeInfo.getHopId());
        routingEntity.setIp(nodeInfo.getIpAddress());
        routingEntity.setType(nodeInfo.getUserType());
        //routingEntity.setNetworkName(nodeInfo.getSsidName());
        routingEntity.setTime(nodeInfo.mGenerationTime);
        return RouteManager.getInstance().updateRoute(routingEntity);

    }

    private void passTheseUsersToWifiUser(ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        byte[] dataToSend = JsonDataBuilder.prepareBtUserToSendWifi(myNodeId, nodeInfoList);
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getWifiUser();
        MeshLog.i("passTheseUsersToWifiUser available. Size:" + (routingEntities == null ? 0 : routingEntities.size()));
        if (CollectionUtil.hasItem(routingEntities)) {
            if (P2PUtil.isMeGO()) {
                for (RoutingEntity routingEntity : routingEntities) {
                    wifiTransPort.passUserInfo(routingEntity.getIp(), dataToSend);
                    MeshLog.i("User passed to wifi:" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
                }
            } else {
                wifiTransPort.passUserInfo(MASTER_IP_ADDRESS, dataToSend);
            }

        }
    }

    private void passTheseUsersToAdhocUser(ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        byte[] dataToSend = JsonDataBuilder.prepareBtUserToSendAdhoc(myNodeId, nodeInfoList);
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getAdhocUser();
        MeshLog.i("passTheseUsersToAdhocUser available. Size:" + (routingEntities == null ? 0 : routingEntities.size()));
        if (CollectionUtil.hasItem(routingEntities)) {
            for (RoutingEntity routingEntity : routingEntities) {
                //TransportManager.getInstance().isUserAvailable(routingEntity.getAddress());
                adHocTransport.passUserInfo(routingEntity.getIp(), dataToSend);
                MeshLog.i("User passed to AdHoc:" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
            }
        }
    }

    @Override
    public void onMessageReceived(String sender, String receiver, String messageId,
                                  byte[] frameData, String ipAddress, String immediateSender) {
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
                    linkStateListener.onMessageReceived(sender, msg_data);
                    preparedAndSendMessageAck(receiver, sellerId, messageId, Constant.MessageStatus.RECEIVED, ackBody);
                } else {
                    linkStateListener.onMessageReceived(sender, msg_data);

                    MeshLog.v("(-) MSG ACK SEND" + sender.substring(sender.length() - 3) + "-> "
                            + receiver.substring(receiver.length() - 3) + "-->" + messageId);
                    if (!TextUtils.isEmpty(messageId)) {
                        String ackBody = Util.buildLocalAckBody();
                        preparedAndSendMessageAck(receiver, sender, messageId, Constant.MessageStatus.RECEIVED, ackBody);
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


    @Override
    public void onPaymentDataReceived(String senderId, String receiver, String messageId,
                                      byte[] payMesg) {

        MeshLog.p("onPaymentDataReceived ble");
        if (receiver.equals(myNodeId)) {
            MeshLog.v("my message ble");
            linkStateListener.onMessagePayReceived(senderId, payMesg);
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
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), msgBody);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    MeshLog.v("(P) sendMessage ble user");
                    BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, msgBody));
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
        MeshLog.v("onPaymentAckReceived");
        if (receiver.equals(myNodeId)) {
            MeshLog.v("my message ack");
            linkStateListener.onPayMessageAckReceived(sender, receiver, messageId);

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
                adHocTransport.sendAdhocMessage(routingEntity.getIp(), msgBody);
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

    @Override
    public void onReceivedMsgAck(String sender, String receiver, String messageId,
                                 int status, String ackBody, String ipAddress) {
        MeshLog.v("(-) MSG ACK RECEVED -> " + "Sender->" + sender.substring(sender.length() - 3) + "receiver ->  " + receiver.substring(receiver.length() - 3) + " -> " + messageId + " : status : " + status);
        if (receiver.equals(myNodeId)) {
            try {
                JSONObject js = new JSONObject(ackBody);

                if (js.getInt(PurchaseConstants.JSON_KEYS.ACK_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND_ACK) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
//                    internetTransport.processInternetOutgoingMessageACK(sender, originalReceiver, messageId, status);
//                    internetTransport.sendBuyerReceivedAck(sender, originalReceiver, messageId, status);
                } else {
                    linkStateListener.onMessageDelivered(messageId, status);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            MeshLog.i(" BT HOP MSG-ACK RCV:: OWNER -> " + receiver.substring(receiver.length() - 3)
                    + " -> SENDER ->" + sender.substring(sender.length() - 3));
            sendMessageAckToOtherMesh(sender, receiver, messageId, status, ackBody);
        }
    }

    @Override
    public void onReceiveRouteInfoUpdate(String routeInfoFor, String newHopAddress, long time) {
        if (Text.isNotEmpty(routeInfoFor) && Text.isNotEmpty(newHopAddress)) {
            MeshLog.i("[Cycle-BT] RouteInfo update request arrived for:" + routeInfoFor + " hop:" + newHopAddress);
            NodeInfo nodeInfo = connectionLinkCache.getNodeInfoById(routeInfoFor);
            if (nodeInfo != null) {
                ConcurrentLinkedQueue<NodeInfo> nodeInfoList = new ConcurrentLinkedQueue<>();
                nodeInfoList.add(nodeInfo);
                String jsonString = GsonUtil.on().toJsonFromQueue(nodeInfoList);
                onMeshLinkFound(newHopAddress, newHopAddress, jsonString);
            } else {
                MeshLog.e("[Cycle-BT] NODE info NULL in Route info update");
            }
        }
    }

    @Override
    public void onReceiveDisconnectRequest(String senderAddress) {
        MeshLog.i("[Cycle-BT] onReceiveDisconnectRequest:" +
                AddressUtil.makeShortAddress(senderAddress) + "-no action necessary");
        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(senderAddress);
        if (routingEntity != null) {
            if (routingEntity.getType() != RoutingEntity.Type.BT)
                MeshLog.w("[Cycle-BT] sender is not a bt node");
        }
    }

    private void preparedAndSendMessageAck(String sender, String receiver, String messageId,
                                           int status, String ackBody) {
        MeshLog.v("MSG ACK SEND" + "  sender -> " + sender.substring(sender.length() - 3) + "  receiver -> " + receiver.substring(receiver.length() - 3) + "-->" + messageId);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        byte[] ackMessage = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);
        if (routingEntity != null) {
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                wifiTransPort.sendMeshMessage(routingEntity.getIp(), ackMessage);
            } else {
                Link directLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (directLink == null) {
                    MeshLog.e("[BT] BT LINK NOT FOUND IN MSG ACK");
                    return;
                }
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) directLink, ackMessage));
            }
        } else {
            MeshLog.e("[Bt] prepared And Send Message Ack failed");
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


    private int sendMessageToOtherMesh(String sender, String receiver, String messageId,
                                       byte[] data) {

        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        int transferId = 0;
        if (routingEntity != null) {
            byte[] message = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {

                transferId = wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);

                MeshLog.v("(-) Send message to => " + routingEntity.toString());
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                transferId = adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    transferId = messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, message));
                } else {
                    MeshLog.v("(*) BT LINK NOT FOUND MSG TO OTR MESH");
                }
            } else {
                MeshLog.v("BT User not found to send the local message");

               /* MeshLog.mm("Internet processMultihopInternetMessage ");
                internetTransport.processInternetOutgoingMessage(sender, receiver, messageId, data);*/
            }
        } else {
            MeshLog.v("Router entity not found");
        }

        return transferId;
    }

    private void sendMessageAckToOtherMesh(String sender, String receiver, String messageId,
                                           int status, String ackBody) {
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
        byte[] data = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);
        if (routingEntity != null) {
            MeshLog.i(" Send message ack to other mesh  =>" + routingEntity.toString());
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                wifiTransPort.sendMeshMessage(routingEntity.getIp(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                adHocTransport.sendAdhocMessage(routingEntity.getIp(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, data));
                } else {
                    MeshLog.e(" BT LINK NOT FOUND TO MSG ACK OTR MESH");
                }
            } else {
                MeshLog.v("Internet processMultihopInternetMessage ");
//                internetTransport.sendBuyerReceivedAck(sender, receiver, messageId, status);
            }
        } else {
            MeshLog.e("RoutingEntity Null in sendMessageAckToOtherMesh in BTTransPort");
        }
    }


    @Override
    public void onReceiverInternetUserLocally(String sender, String receiver, String
            sellerId, String userList) {
        MeshLog.v("BT onReceiverInternetUserLocally =" + receiver);
        if (receiver.equalsIgnoreCase(myNodeId)) {
            //String[] idArray = userList.split("@");

//            linkStateListener.onCurrentSellerId(sellerId);
            /*if (internetTransport.amIDirectUser()) {
                MeshLog.v("direct user");
                return;
            }*/

            ConcurrentLinkedQueue<NodeInfo> userNodeInfoList = GsonUtil.on().queueFromJson(userList);

            for (NodeInfo item : userNodeInfoList) {
                if (TextUtils.isEmpty(item.getUserId()) || item.getUserId().equals(myNodeId)) {
                    continue;
                }

                if (RouteManager.getInstance().isOnline(item.getUserId())) {
                    linkStateListener.onRemoteUserConnected(item.getUserId(), item.getPublicKey());
                    continue;
                }

                RoutingEntity routingEntity = new RoutingEntity(item.getUserId());
                routingEntity.setOnline(true);
                routingEntity.setType(RoutingEntity.Type.INTERNET);
                routingEntity.setHopAddress(sellerId);
                boolean updated = RouteManager.getInstance().updateRoute(routingEntity);
                if (updated) {
                    //NodeInfo nodeInfo = new NodeInfo(item, "", "internetLink", "public key", PreferencesHelper.MESH_USER, Constant.UserTpe.INTERNET);
                    item.setUserType(RoutingEntity.Type.INTERNET);
                    //item.setBleName("");
                    //item.setSsidName("");
                    addNodeInformation(item.getUserId(), item);
                    MeshLog.v("BT transport interent user ids send success=" + item);
                    linkStateListener.onRemoteUserConnected(item.getUserId(), item.getPublicKey());

                }
            }

        } else {


            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (routingEntity != null) {
                byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, receiver, sellerId, userList);
                MeshLog.v("(-) RoutingEntity" + routingEntity.toString());
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("senUserList Wifi user");
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                    messageDispatcher.addSendMessage(baseMeshMessage);

                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                    messageDispatcher.addSendMessage(baseMeshMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    MeshLog.v("senUserList ble user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> userListMessage));
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
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), userListMessage);
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
                MeshLog.e("Router entity not found");
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

            MeshLog.v("(-) Send message to => " + routingEntity.toString());
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, message));
            } else {
                MeshLog.v("(*) BT LINK NOT FOUND MSG TO OTR MESH");
            }
        } else {
            MeshLog.v("BT User not found to send the local message");
        }
    }

    private void updateRoutingAndUI(BleLink link, NodeInfo sender) {
        BleLink bleLink = link;
        //Routing table update
        RoutingEntity routingEntity = new RoutingEntity(sender.getUserId());
        routingEntity.setOnline(true);
        routingEntity.setHopAddress(null);
        routingEntity.setIp(null);
        routingEntity.setType(RoutingEntity.Type.BT);
        //routingEntity.setNetworkName(sender.getBleName());
        boolean updated = RouteManager.getInstance().updateRoute(routingEntity);
        if (updated && RouteManager.getInstance().isOnline(sender.getUserId())) {
            if (connectionLinkCache != null) {
                connectionLinkCache.addDirectLink(sender.getUserId(), bleLink, sender);
            }
            linkStateListener.onLocalUserConnected(sender.getUserId(), sender.getPublicKey());
            MeshLog.i("Routing table updated for:: " + AddressUtil.makeShortAddress(routingEntity.getAddress()));
        } else {
            MeshLog.v("Routing table not updated ");
        }
    }

    private void updateRoutingTableAndUI(ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        if (CollectionUtil.hasItem(nodeInfoList)) {

            boolean isMeSeller = PreferencesHelper.on().getDataShareMode() ==
                    PreferencesHelper.DATA_SELLER;

            for (NodeInfo nodeInfo : nodeInfoList) {

                boolean isUpdated = updateRouteWithCurrentTime(nodeInfo);

                if (isUpdated && RouteManager.getInstance().isOnline(nodeInfo.getUserId())) {
                    addNodeInformation(nodeInfo.getUserId(), nodeInfo);
                    linkStateListener.onLocalUserConnected(nodeInfo.getUserId(), nodeInfo.getPublicKey());

                    /*if (nodeInfo.getUserMode() == PreferencesHelper.DATA_BUYER && isMeSeller) {

                        outputThread.dispatch(() -> internetTransport.onBuyerConnected(
                                nodeInfo.getUserId()));

                    }*/
                    MeshLog.i("Routing table updated for:" + AddressUtil.makeShortAddress(nodeInfo.getUserId()));
                } else {
                    MeshLog.i("Could not update routing for:" + AddressUtil.makeShortAddress(nodeInfo.getUserId()));
                }

            }
        }

    }

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

    public BluetoothServer getBluetoothServer() {
        if (bluetoothServer != null) {
            return bluetoothServer;
        }
        return null;
    }

    @Override
    public void onReceiveNewRole(String sender, String receiver, int role) {
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
                adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                }
            }
        }
    }

    @Override
    public void onFileMessageReceived(String sender, String receiver, String messageId, String message, String immediateSender) {
        if (receiver.equals(myNodeId)) {
            linkStateListener.onFileMessageReceived(sender, message);
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.BLE);

            if (routingEntity != null) {
                MeshLog.v("Multihop received in BT transport need to pass other user :" + AddressUtil.makeShortAddress(routingEntity.getAddress()));

                byte[] data = JsonDataBuilder.buildFiledMessage(sender, receiver, messageId, message.getBytes());
                if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                    wifiTransPort.sendAppMessage(routingEntity.getIp(), data);
                } else {
                    MeshLog.v("Peek Wrong entity to send message");
                    //bleTransport.addAppMessageInQueue(null, routingEntity.getAddress(), data);
                }
            } else {
                MeshLog.v("BT buyer file message route null");
            }
        }
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
    public void onClientBtMsgSocketConnected(BluetoothDevice bluetoothDevice) {
        Log.e("Bt-filesocket", "Bt Connected as client");
        //linkStateListener.onClientBtMsgSocketConnected(bluetoothDevice);
    }

    @Override
    public void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid, String password, int wifiNodeCount, boolean isFirstMessage) {

    }



    @Override
    public void onReceiveBuyerFileMessage(String sender, String receiver, String messageData, int fileMessageType, String immediateSender, String messageId) {
//        MeshLog.v("FILE_SPEED_TEST_6.5 " + Calendar.getInstance().getTime());
        try {
            if (receiver.equals(myNodeId)) {
                MeshLog.v("Buyer_file received in BT transport ");

                JSONObject js = new JSONObject(messageData);
                String messageString = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_DATA);

                if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);

                    if (fileMessageType == Constant.FileMessageType.FILE_ACK_MESSAGE || fileMessageType == Constant.FileMessageType.FILE_INFO_MESSAGE) {
                        byte[] message = JsonDataBuilder.buildBuyerFileMessage(sender, originalReceiver, messageString.getBytes(), fileMessageType, messageId);

                        if (RouteManager.getInstance().isDirectlyConnected(originalReceiver)) {
                            remoteTransport.sendAppMessage(originalReceiver, message);
                        } else {
                            RoutingEntity remoteUser = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(originalReceiver);
                            if (remoteUser != null) {
                                remoteTransport.sendAppMessage(remoteUser.getAddress(), message);
                            } else {
                                MeshLog.e("Routing Entity NULL");
                            }
                        }
                    } else {

                        remoteTransport.processInternetOutgoingMessage(sender, originalReceiver, messageId, messageString.getBytes(), true, fileMessageType);
                    }
                } else if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE) {
                    String sellerId = js.getString(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS);
                    JSONObject msgObject = new JSONObject(messageString);
                    msgObject.put(JsonDataBuilder.KEY_IMMEDIATE_SENDER, sellerId);
                    linkStateListener.onFileMessageReceived(sender, msgObject.toString());
                }
            } else {
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
                if (routingEntity != null) {
                    MeshLog.v("Buyer_file received in BLE transport need to pass other user :" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
                    byte[] data = JsonDataBuilder.buildBuyerFileMessage(sender, receiver, messageData.getBytes(), fileMessageType,messageId);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                        wifiTransPort.sendAppMessage(routingEntity.getIp(), data);
                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        //addAppMessageInQueue(null, routingEntity.getAddress(), data);
                        MeshLog.v("Received message in BT should not send in BLE again");
                    }
                } else {
                    MeshLog.v("BLE buyer file message route null");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public boolean sendMeshMessage(String receiverAddress, byte[] message) {
        BleLink bleLink = BleLink.getBleLink();
        if (bleLink != null) {
            bleLink.sendMeshMessage(message);
        } else {
            return false;
        }
        return true;
    }
}
