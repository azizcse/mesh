package com.w3engineers.mesh.wifi;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.hardwareoff.HardwareStateManager;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.ble.BleTransport;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.datasharing.util.PurchaseConstants;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.httpservices.MeshHttpServer;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.DisconnectionModel;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.model.PendingMessage;
import com.w3engineers.mesh.queue.DiscoveryTask;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.queue.MeshMessageListener;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
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
import com.w3engineers.mesh.util.log.MyTextSpeech;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static com.w3engineers.mesh.util.Constant.MASTER_IP_ADDRESS;

/**
 * <h1>Wifi connectivity Transport</h1>
 * <p>
 * With other transport communication also manages from there
 * </p>
 */
public class WifiTransPort implements ConnectionStateListener, MeshTransport {

    public static final int GO_MAXIMUM_CLIENT_NUMBER = 1;

    private String myNodeId;
    private Context context;
    private LinkStateListener linkStateListener;
    private BluetoothTransport bluetoothTransport;
    private RemoteTransport remoteTransport;
    private AdHocTransport adHocTransport;
    private TransportStateListener transportStateListener;
    private ConnectionLinkCache connectionLinkCache;
    public static final String P2P_MASTER_IP_ADDRESS = "192.168.49.1";
    private MessageDispatcher messageDispatcher;
    private MyTextSpeech mMyTextSpeech;
    private ForwardListener forwardListener;
    private BleTransport bleTransport;
    private MeshLibMessageEventQueue messageEventQueue;
    int mTimeOut = MeshHttpServer.DEFAULT_CONNECTION_TIMEOUT;
    private MeshMessageListener meshMessageListener;

    private String specificNodeIdWantToConnect;

    /**
     * hold discovery sending message id. Reset upon receiving response from GO or LC or disconnecting
     * from GO. For GO as this value always overlaps earlier id and we manage queue for message
     * sending so we will receive ack for last message at last and consumer of this variable
     * will be able to track always last value for GO.
     */
  /*  public volatile int mQueryingPeerMessageId = BaseMeshMessage.DEFAULT_MESSAGE_ID;
    private Runnable mClientTimeOutTask = () -> {
        if (mTransportManagerX != null && !RouteManager.getInstance().isWifiUserConnected()) {
            MeshLog.w("[MeshX]Reattempting service discovery. LC - GO connection timeout:" +
                    (CLIENT_MASTER_CONNECTION_TIME_OUT / 1000) + " seconds.");
            resetPeerQueryState();
            mTransportManagerX.initiateP2P();
        }
    };*/

    /**
     * <p>Constructor to init wifi transport</p>
     *
     * @param context             : android context
     * @param nodeId              : String used as node id
     * @param connectionListener  : app layer listener {@link LinkStateListener}
     * @param stateListener       : library internal listener
     * @param connectionLinkCache
     */
    public WifiTransPort(Context context, String nodeId, LinkStateListener connectionListener,
                         TransportStateListener stateListener, ConnectionLinkCache connectionLinkCache,
                         MessageDispatcher messageDispatcher) {
        this.transportStateListener = stateListener;
        this.myNodeId = nodeId;
        this.context = context.getApplicationContext();
        this.linkStateListener = connectionListener;
        this.connectionLinkCache = connectionLinkCache;
        this.messageDispatcher = messageDispatcher;
        mMyTextSpeech = new MyTextSpeech(context);
    }


    @Override
    public void start() {
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            MeshHttpServer.on().setWifiDataListener(messageEventQueue, this);
        }
    }

    @Override
    public void stop() {
        MeshHttpServer.on().stop();
    }

    /**
     * <p></p>
     *
     * @param transPorts :
     * @param <T>
     */
    public <T> void setInterTransport(T... transPorts) {
        for (T item : transPorts) {
            if (item instanceof BluetoothTransport) {
                this.bluetoothTransport = (BluetoothTransport) item;
            } else if (item instanceof RemoteTransport) {
                this.remoteTransport = (RemoteTransport) item;
            } else if (item instanceof AdHocTransport) {
                this.adHocTransport = (AdHocTransport) item;
            } else if (item instanceof ForwardListener) {
                this.forwardListener = (ForwardListener) item;
            } else if (item instanceof BleTransport) {
                bleTransport = (BleTransport) item;
            } else if (item instanceof MeshLibMessageEventQueue) {
                messageEventQueue = (MeshLibMessageEventQueue) item;
            } else if (item instanceof MeshMessageListener) {
                meshMessageListener = (MeshMessageListener) item;
            }
        }
    }


    private void filterSelfEntity(ConcurrentLinkedQueue<RoutingEntity> itemList) {
        if (CollectionUtil.hasItem(itemList)) {
            for (RoutingEntity item : itemList) {
                if (item.getAddress().equals(myNodeId)) {
                    itemList.remove(item);
                    break;
                }
            }
        }
    }

    /**
     * <h1>
     * Send message and get confirmation is success
     *
     * <p>This method will send message without queuing message</p>
     * </h1>
     *
     * @param ipAddress       (required) String IP address
     * @param data            (required) byte array
     * @param messageCallback (required) Listener
     */
    public void sendMessageAndGetCallBack(String ipAddress, byte[] data, MessageCallback messageCallback) {
        new Thread(() -> {
            int retryCount = 0;
            boolean isSuccess = false;
            try {
                while (retryCount < 3) {
                    retryCount++;
                    int result = MeshHttpServer.on().sendMessage(ipAddress, data, mTimeOut);
                    isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                    if (isSuccess) {
                        break;
                    }
                }
                if (messageCallback != null) {
                    messageCallback.onMessageSend(isSuccess);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                if (messageCallback != null) {
                    messageCallback.onMessageSend(isSuccess);
                }
            }

        }).start();

    }

    public void setSpecificNodeIdWhoWantsToConnect(String nodeId) {
        this.specificNodeIdWantToConnect = nodeId;
        MeshLog.v("[p2p_process] Special connection needed id :" + nodeId);
    }

    @Override
    public void onV2ReceivedHelloFromClient(String senderInfo, String onlineMeshNodes, String offlineNodes, String dataId) {
        RoutingEntity senderEntity = GsonUtil.on().getEntityFromJson(senderInfo);

        if (senderEntity == null || senderEntity.getAddress().equals(myNodeId)) {
            return;
        }

        List<RoutingEntity> connectedWifiUsers = RouteManager.getInstance().getWifiUser();

        if (connectedWifiUsers.size() >= GO_MAXIMUM_CLIENT_NUMBER && !dataId.startsWith(Constant.FILE_CONNECTION)) {
            byte[] data = JsonDataBuilder.buildNetworkFullResponseMessage(myNodeId);
            sendMessageAndGetCallBack(senderEntity.getIp(), data, null);
            return;
        }

        /*if (connectedWifiUsers.size() == (GO_MAXIMUM_CLIENT_NUMBER - 1) && specificNodeIdWantToConnect != null) {
            if (!specificNodeIdWantToConnect.equals(senderEntity.getAddress())) {
                MeshLog.e("[p2p_process] waiting for:" + AddressUtil.makeShortAddress(specificNodeIdWantToConnect)
                        + " connection: " + AddressUtil.makeShortAddress(senderEntity.getAddress()));
                return;
            } else {
                setSpecificNodeIdWhoWantsToConnect(null);
            }
        } else if (specificNodeIdWantToConnect != null && specificNodeIdWantToConnect.equals(senderEntity.getAddress())) {
            setSpecificNodeIdWhoWantsToConnect(null);
        }*/

        //Data Id added in received link
        AddressUtil.addDataGenerationId(dataId);

        MeshLog.e("[BLE_PROCESS] wifi received hello from client");

        int x = GO_MAXIMUM_CLIENT_NUMBER - 2;
        MeshLog.v("[Mesh]Connected:" + connectedWifiUsers);
        if (connectedWifiUsers.size() > x) {
            transportStateListener.onMaximumLcConnect();
        }


        ConcurrentLinkedQueue<RoutingEntity> onlineMeshNode = GsonUtil.on().getEntityQueue(onlineMeshNodes);
        filterSelfEntity(onlineMeshNode);
        ConcurrentLinkedQueue<RoutingEntity> offLineMeshNode = GsonUtil.on().getEntityQueue(offlineNodes);
        filterSelfEntity(offLineMeshNode);

        ConcurrentLinkedQueue<RoutingEntity> allOnlines = new ConcurrentLinkedQueue<>();
        //allOnlines.add(senderEntity);
        if (CollectionUtil.hasItem(onlineMeshNode)) {
            allOnlines.addAll(onlineMeshNode);
        }

        printLog("hello master", senderEntity.getAddress(), allOnlines);

        //Check cycle connection
        //ConcurrentLinkedQueue<RoutingEntity> uniqueEntityList = filterUniqueEntityList(allOnlines);


        //Send response to client
        sendP2pHelloResponseToClient(senderEntity);

        ConcurrentLinkedQueue<RoutingEntity> directNodes = new ConcurrentLinkedQueue<>();
        directNodes.add(senderEntity);

        v2UpdateRoutingTableAndUiForMaster(directNodes, onlineMeshNode, 200L);

        v2UpdateRoutingTableForOfflineNodes(offLineMeshNode);


        ConcurrentLinkedQueue<RoutingEntity> onlineNodesWifiList = new ConcurrentLinkedQueue<>();
        onlineNodesWifiList.add(senderEntity);
        if (CollectionUtil.hasItem(allOnlines)) {
            onlineNodesWifiList.addAll(allOnlines);
        }

        HandlerUtil.postBackground(() -> {
            //Send to lc user
            v2SendDiscoveredUserToLcUsers(senderEntity.getAddress(), onlineNodesWifiList, offLineMeshNode, dataId);
        }, 1000L);


        ConcurrentLinkedQueue<RoutingEntity> onlineNodes = new ConcurrentLinkedQueue<>();
        onlineNodes.add(senderEntity);
        if (CollectionUtil.hasItem(allOnlines)) {
            onlineNodes.addAll(allOnlines);
        }
        HandlerUtil.postBackground(() -> {
            //Send to ble user
            v2SendDiscoveredNodeToBleUser(onlineNodes, offLineMeshNode, dataId);
        }, 1000L);

        /*



        if (cycleNodes == null || cycleNodes.isEmpty()) {

            //Todo for adhoc we have to send adhos info to lc too

            ConcurrentLinkedQueue<RoutingEntity> onlineNodesWifiList = new ConcurrentLinkedQueue<>();
            onlineNodesWifiList.add(senderEntity);
            if (CollectionUtil.hasItem(onlineMeshNode)) {
                onlineNodesWifiList.addAll(onlineMeshNode);
            }
            //Send discovered user to LC
            v2SendDiscoveredUserToLcUsers(senderEntity.getAddress(), onlineNodesWifiList, offLineMeshNode);

            //Send to ble user
            ConcurrentLinkedQueue<RoutingEntity> onlineNodes = new ConcurrentLinkedQueue<>();
            onlineNodes.add(senderEntity);
            if (CollectionUtil.hasItem(onlineMeshNode)) {
                onlineNodes.addAll(onlineMeshNode);
            }

            v2SendDiscoveredNodeToBleUser(onlineNodes, offLineMeshNode);

            //sendDiscoveredUsersToWifiClient(senderNodeInfo.getUserId(), allNodeInfoList, onlineUserList);
        } else {
            MeshLog.i("[BLE_PROCESS] Ble cycle formed send disconnect");
            //Todo for adhoc we have to send adhos info to lc too
            ConcurrentLinkedQueue<RoutingEntity> newOfflineList = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<RoutingEntity> newOnlineNodeList = new ConcurrentLinkedQueue<>();
            newOnlineNodeList.add(senderEntity);
            v2SendDiscoveredUserToLcUsers(senderEntity.getAddress(), newOnlineNodeList, newOfflineList);
        }*/
        //Send to wifi client
        if (transportStateListener != null) {
            transportStateListener.onWifiP2pUserConnected(true, senderEntity.getAddress());
            transportStateListener.onReceiveHelloFromClient(senderEntity.getAddress());
        }


    }


    private ConcurrentLinkedQueue<RoutingEntity> filterUniqueEntityList(ConcurrentLinkedQueue<RoutingEntity> onlineDirect) {
        ConcurrentLinkedQueue<RoutingEntity> uniqueList = new ConcurrentLinkedQueue<>();

        if (!CollectionUtil.hasItem(onlineDirect)) return uniqueList;

        for (RoutingEntity item : onlineDirect) {
            // If any data duplication occurred  in wifi side we will not send to other LC or BLE
            // This section is filtered duplicate data to send
            RoutingEntity routingEntity = RouteManager.getInstance().getEntityByAddress(item.getAddress());
            if (routingEntity == null || !routingEntity.isOnline()) {
                uniqueList.add(item);
            }
            return uniqueList;

        }
        return uniqueList;
    }


    private void v2UpdateRoutingTableAndUi(ConcurrentLinkedQueue<RoutingEntity> onlineDirect, ConcurrentLinkedQueue<RoutingEntity> onlineMesh, long delay) {
        if (CollectionUtil.hasItem(onlineDirect)) {
            for (RoutingEntity item : onlineDirect) {
                boolean isUpdated = RouteManager.getInstance().insertRoute(item);
                MeshLog.i("[BLE_PROCESS] direct user update status : " + isUpdated + " for: "
                        + AddressUtil.makeShortAddress(item.getAddress()));
                if (isUpdated) {
                    HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);

//                    if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER
//                            || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_SELLER) {
//                        remoteTransport.onBuyerConnected(item.getAddress());
//                    }

                }

            }
        }

        if (CollectionUtil.hasItem(onlineMesh)) {
            for (RoutingEntity item : onlineMesh) {

                // Here we are checking that the node has same hop or not.
                // If the node has same node so it is redundant to add again in routing table

                RoutingEntity oldRoutingEntity = RouteManager.getInstance()
                        .getEntityByDestinationAndHop(item.getAddress(), item.getHopAddress());

                if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {
                    if (oldRoutingEntity.getType() != RoutingEntity.Type.INTERNET) {
                        // So that mean this user already exists in locally.
                        // So we don't need to insert again
                        // we will update the user by latest type and hop count

                        oldRoutingEntity.setHopCount(item.getHopCount());
                        oldRoutingEntity.setType(item.getType());

                        RouteManager.getInstance().updateEntity(oldRoutingEntity);

                        HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);
                        continue;
                    }

                }

                MeshLog.v("[P2P_PROCESS] data info " + item.toString());

                item.setOnline(true);
                boolean isUpdated = RouteManager.getInstance().insertRoute(item);
                MeshLog.i("[BLE_PROCESS] mesh user update status : " + isUpdated + " for: "
                        + AddressUtil.makeShortAddress(item.getAddress()));

                if (isUpdated) {
                    HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);
                }

            }
        }
    }

    private String v2UpdateRoutingTableAndUiForMaster(ConcurrentLinkedQueue<RoutingEntity> onlineDirect, ConcurrentLinkedQueue<RoutingEntity> onlineMesh, long delay) {
        String myDirectBleNode = null;
        if (CollectionUtil.hasItem(onlineDirect)) {
            for (RoutingEntity item : onlineDirect) {

                //  Todo before insert we have to check the user already exists in BLE or not


                boolean isUpdated = RouteManager.getInstance().insertRoute(item);
                MeshLog.i("[BLE_PROCESS] direct user update status : " + isUpdated + " for: "
                        + AddressUtil.makeShortAddress(item.getAddress()));
                //addNodeInformation(item.getUserId(), item);
                HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);

//                if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER
//                        || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_SELLER) {
//                    remoteTransport.onBuyerConnected(item.getAddress());
//                }
            }
        }

        if (CollectionUtil.hasItem(onlineMesh)) {
            for (RoutingEntity item : onlineMesh) {

                RoutingEntity oldRoutingEntity = RouteManager.getInstance()
                        .getEntityByDestinationAndHop(item.getAddress(), item.getHopAddress());

                if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {
                    if (oldRoutingEntity.getType() != RoutingEntity.Type.INTERNET) {
                        // So that mean this user already exists in locally.
                        // So we don't need to insert again
                        // we will update the user by latest type and hop count

                        oldRoutingEntity.setHopCount(item.getHopCount());
                        oldRoutingEntity.setType(item.getType());

                        RouteManager.getInstance().updateEntity(oldRoutingEntity);


                        HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);
                        continue;
                    }

                }


                MeshLog.v("[P2P_PROCESS] data info " + item.toString());

                boolean isUpdated = RouteManager.getInstance().insertRoute(item);
                MeshLog.i("[BLE_PROCESS] mesh user update status : " + isUpdated + " for: "
                        + AddressUtil.makeShortAddress(item.getAddress()));
                //addNodeInformation(item.getAddress(), item);
                if (isUpdated) {
                    HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);
                }

            }
        }
        return myDirectBleNode;
    }

    private void v2UpdateRoutingTableForOfflineNodes(ConcurrentLinkedQueue<RoutingEntity> offLineNodes) {
        if (CollectionUtil.hasItem(offLineNodes)) {
            for (RoutingEntity item : offLineNodes) {
                //If node is already online in DB then do nothing
                if (RouteManager.getInstance().isOnline(item.getAddress())) {
                    continue;
                }
                /*RoutingEntity routingEntity = new RoutingEntity(item.getUserId());
                routingEntity.setHopAddress(item.getHopId());
                routingEntity.setIp(item.getIpAddress());
                routingEntity.setOnline(false);
                routingEntity.setType(item.getUserType());
                routingEntity.mTime = System.currentTimeMillis();*/
                boolean isUpdated = RouteManager.getInstance().updateRoute(item);
                //addNodeInformation(item.getAddress(), item);
            }
        }
    }


    private void v2SendDiscoveredUserToLcUsers(String senderId, ConcurrentLinkedQueue<RoutingEntity> onlineNodes,
                                               ConcurrentLinkedQueue<RoutingEntity> offlineNodes, String dataId) {

        List<RoutingEntity> routingEntities = RouteManager.getInstance().getWifiUser();
        byte[] data = JsonDataBuilder.v2BuildMeshNodePacketToSendWifiLc(myNodeId, onlineNodes, offlineNodes, dataId);
        for (RoutingEntity item : routingEntities) {
            //Continue if item is sender itself
            if (item.getAddress().equals(senderId)) continue;
            addDiscoveryTaskInQueue("WiFi master to client", false, false, item.getIp(), () -> data);
        }
    }


    private void v2SendDiscoveredNodeToBleUser(ConcurrentLinkedQueue<RoutingEntity> onlineNodes,
                                               ConcurrentLinkedQueue<RoutingEntity> offlineNodes, String dataId) {
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getBleUsers();
        if (routingEntities != null && routingEntities.size() >= 1) {
            byte[] data = JsonDataBuilder.v2BuildMeshNodePacketToSendBle(myNodeId, onlineNodes, offlineNodes, dataId);
            for (RoutingEntity routingEntity : routingEntities) {
                // before sending we have to check that node has any wifi connection or not.
                // If any wifi connection available we will send node into to that BLE node
                // Because this BLE node will be disconnected
                RoutingEntity wifiRoute = RouteManager.getInstance()
                        .getSingleUserInfoByType(routingEntity.getAddress(), RoutingEntity.Type.WiFi);

                if (wifiRoute == null || !wifiRoute.isOnline()) {
                    bleTransport.addDiscoveryTaskInQueue(false,
                            routingEntity.getAddress(), () -> data);
                }
            }
        } else {
            MeshLog.e("[BLE_PROCESS] BLE DIRECT CONNECTION NOT EXIST");
        }
    }

    @Override
    public void onV2ReceivedHelloFromMaster(String senderInfo, String onlineLcNodes, String onlineOtherNodes, String offlineNodes, String dataId) {
        RoutingEntity senderEntity = GsonUtil.on().getEntityFromJson(senderInfo);
        if (senderEntity == null) return;
        //Sender info update
        MeshLog.e("[BLE_PROCESS] received hello from master");

        //Data Id added in received link
        AddressUtil.addDataGenerationId(dataId);

        ConcurrentLinkedQueue<RoutingEntity> onlineLcUsers = GsonUtil.on().getEntityQueue(onlineLcNodes);
        filterSelfEntity(onlineLcUsers);

        ConcurrentLinkedQueue<RoutingEntity> onlineMeshUsers = GsonUtil.on().getEntityQueue(onlineOtherNodes);
        filterSelfEntity(onlineMeshUsers);

        ConcurrentLinkedQueue<RoutingEntity> offlineMeshUsers = GsonUtil.on().getEntityQueue(offlineNodes);
        filterSelfEntity(offlineMeshUsers);

        ConcurrentLinkedQueue<RoutingEntity> allDirectNodes = new ConcurrentLinkedQueue<>();
        allDirectNodes.add(senderEntity);

        if (CollectionUtil.hasItem(onlineLcUsers)) {
            allDirectNodes.addAll(onlineLcUsers);
        }
        printLog("hello client D", senderEntity.getAddress(), onlineLcUsers);
        printLog("hello client M", senderEntity.getAddress(), onlineMeshUsers);
        //ConcurrentLinkedQueue<RoutingEntity> uniqueNodes = filterUniqueEntityList(onlineMeshUsers);

        //Update Ui and routing table for online nodes
        v2UpdateRoutingTableAndUi(allDirectNodes, onlineMeshUsers, 2500L);

        //Update Ui and routing table for offline nodes
        v2UpdateRoutingTableForOfflineNodes(offlineMeshUsers);


        //sendDiscoveredUsersToBleUser(allNodeInfoList, onlineUserList);
        ConcurrentLinkedQueue<RoutingEntity> allOnlineNodes = new ConcurrentLinkedQueue<>();
        allOnlineNodes.add(senderEntity);
        if (CollectionUtil.hasItem(onlineLcUsers)) {
            allOnlineNodes.addAll(onlineLcUsers);
        }
        if (CollectionUtil.hasItem(onlineMeshUsers)) {
            allOnlineNodes.addAll(onlineMeshUsers);
        }

        HandlerUtil.postBackground(() -> v2SendDiscoveredNodeToBleUser(allOnlineNodes, offlineMeshUsers, dataId), 1000L);


        if (transportStateListener != null) {
            transportStateListener.onWifiP2pUserConnected(false, senderEntity.getAddress());
            transportStateListener.onReceiveHelloFromClient(senderEntity.getAddress());
        }

    }

    @Override
    public void onV2ReceivedMeshUsers(String sender, String onlineMeshNodes, String offlineMeshNodes, String dataId) {
        MeshLog.e("[BLE_PROCESS] mesh users received in WiFi");

        //Discard duplicate date
        if (AddressUtil.isDataGenerationIdExist(dataId)) {
            MeshLog.v("[wifi-" + "Duplicate data Id: " + dataId);
            return;
        } else {
            AddressUtil.addDataGenerationId(dataId);
            MeshLog.v("[wifi-" + "data received Id: " + dataId);
        }

        ConcurrentLinkedQueue<RoutingEntity> onlineNodes = GsonUtil.on().getEntityQueue(onlineMeshNodes);
        filterSelfEntity(onlineNodes);
        ConcurrentLinkedQueue<RoutingEntity> offlineNodes = GsonUtil.on().getEntityQueue(offlineMeshNodes);
        filterSelfEntity(offlineNodes);

        printLog("mesh user", sender, onlineNodes);
        // Filter unique list
        //ConcurrentLinkedQueue<RoutingEntity> uniqueNodes = filterUniqueEntityList(onlineNodes);

        //Update routing table and ui for online nodes
        v2UpdateRoutingTableAndUiForOnlineMeshNodes(onlineNodes);

        v2UpdateRoutingTableForOfflineNodes(offlineNodes);


        ConcurrentLinkedQueue<RoutingEntity> onLines = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<RoutingEntity> offLines = new ConcurrentLinkedQueue<>();

        //We will only send unique node. If duplicate found never pass the user to other
        if (CollectionUtil.hasItem(onlineNodes)) {
            onLines.addAll(onlineNodes);
        }
        if (CollectionUtil.hasItem(offlineNodes)) {
            offLines.addAll(offlineNodes);
        }

        if (CollectionUtil.hasItem(onLines)) {
            //pass this user to BLE user
            v2PassDiscoveredMeshNodesToBleUsers(onLines, offLines, dataId);
        }

    }

    /*private void v2PassDiscoveredMeshNodesToLcUsers(String sender, ConcurrentLinkedQueue<NodeInfo> onlineNodes,
                                                    ConcurrentLinkedQueue<NodeInfo> offlineNodes) {
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getWifiUser();
        if (CollectionUtil.hasItem(routingEntities)) {
            byte[] data = JsonDataBuilder.v2BuildMeshNodePacketToSendLcUsers(myNodeId, onlineNodes, offlineNodes);
            for (RoutingEntity routingEntity : routingEntities) {
                if (routingEntity != null) {
                    if (routingEntity.getAddress().equals(sender)) continue;
                }
                addDiscoveryTaskInQueue("Mesh user master to client ", false, false, routingEntity.getIp(), () -> data);
            }
        }
    }*/

    private void v2PassDiscoveredMeshNodesToBleUsers(ConcurrentLinkedQueue<RoutingEntity> onlineNodes,
                                                     ConcurrentLinkedQueue<RoutingEntity> offlineNodes, String dataId) {
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getBleUsers();
        if (routingEntities != null && !routingEntities.isEmpty()) {
            byte[] data = JsonDataBuilder.v2BuildMeshNodePacketToSendBle(myNodeId, onlineNodes, offlineNodes, dataId);
            for (RoutingEntity routingEntity : routingEntities) {
                bleTransport.addDiscoveryTaskInQueue(false, routingEntity.getAddress(), () -> data);
            }
        } else {
            MeshLog.e("[BLE_PROCESS] BLE DIRECT CONNECTION NOT EXIST");
        }
    }


    private void v2UpdateRoutingTableAndUiForOnlineMeshNodes(ConcurrentLinkedQueue<RoutingEntity> onlineMeshNodes) {
        if (CollectionUtil.hasItem(onlineMeshNodes)) {
            for (RoutingEntity item : onlineMeshNodes) {

                RoutingEntity oldRoutingEntity = RouteManager.getInstance()
                        .getEntityByDestinationAndHop(item.getAddress(), item.getHopAddress());

                if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {
                    if (oldRoutingEntity.getType() != RoutingEntity.Type.INTERNET) {
                        // So that mean this user already exists in locally.
                        // So we don't need to insert again
                        // we will update the user by latest type and hop count

                        oldRoutingEntity.setHopCount(item.getHopCount());
                        oldRoutingEntity.setType(item.getType());

                        RouteManager.getInstance().updateEntity(oldRoutingEntity);

                        HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), 1000L);
                        continue;
                    }

                }

                boolean isUpdated = RouteManager.getInstance().insertRoute(item);
                HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), 1000L);
            }
        }
    }

    @Override
    public void onV2CredentialReceived(String sender, String receiver, String ssid, String password, String goNodeId) {
        MeshLog.i("[BLE_PROCESS] credential message ssid: " + ssid + " password: " + password);
        transportStateListener.onReceiveSoftApCredential(ssid, password, goNodeId);
    }

    @Override
    public void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid,
                                                  String password, int wifiNodeCount, boolean isFirstMessage) {

        if (!receiver.equals(myNodeId)) {
            MeshLog.v("[BLE_PROCESS] Send ble mesh decision message in BLE");
            sendBleMeshDecisionMessage(sender, receiver, ssid, password, wifiNodeCount, isFirstMessage);
        }
    }

    @Override
    public void onV2ForceConnectionMessage(String sender, String receiver, String ssid,
                                           String password, boolean isRequest, boolean isAbleToReceive) {

        MeshLog.i("[p2p_process] wifi received ssid: " + ssid + " pass: " + password + " sender: " + AddressUtil.makeShortAddress(sender));
        if (receiver.equals(myNodeId)) {
            transportStateListener.onGetForceConnectionRequest(sender, ssid, password, isRequest, false, isAbleToReceive);
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);
            if (routingEntity != null) {
                byte[] data = JsonDataBuilder.prepareForceConnectionMessage(sender, receiver, ssid, password, isRequest, isAbleToReceive);
                sendMessageByType(data, routingEntity);
            } else {
                MeshLog.e("[BLE_PROCESS] force connection message the user node exists: " + AddressUtil.makeShortAddress(receiver));
            }
        }
    }

    @Override
    public void onV2GetFileFreeModeMessage(String sender, String receiver, boolean isAbleToReceive) {
        transportStateListener.onGetFileFreeModeMessage(sender, receiver, isAbleToReceive);
    }

    private void sendBleMeshDecisionMessage(String sender, String receiver, String ssid,
                                            String password, int wifiNodeCount, boolean isFirstMessage) {
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);
        if (routingEntity == null) {
            MeshLog.e("Routing Entity NULL on sendMessageToOtherMesh");
            return;
        }
        byte[] data = JsonDataBuilder.prepareBleMeshDecisionMessage(sender, receiver, ssid, password, wifiNodeCount, isFirstMessage);
        MeshLog.i(" Send message =" + routingEntity.toString());
        sendMessageByType(data, routingEntity);

    }

    private void sendMessageByType(byte[] data, RoutingEntity routingEntity) {
        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            sendMeshMessage(routingEntity.getIp(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            sendMeshMessage(routingEntity.getIp(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
            bleTransport.sendMessage(routingEntity.getAddress(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, data));
            } else {
                MeshLog.w(" BLE LINK NOT FOUND");
            }
        } else {
            // Not need to send internet user
        }
    }

    @Override
    public void onV2ReceivedGoNetworkFullResponse(String senderId) {
        transportStateListener.onReceivedNetworkFullResponseFromGo(senderId);
    }

    @Override
    public void onV2ReceiveSpecialDisconnectMessage(String receiverId, String duplicateId) {
        MeshLog.v("[P2P_Process] Received special disconnect message in wifi");
        if (receiverId.equals(myNodeId)) {
            RoutingEntity routingEntity = RouteManager.getInstance().getEntityByAddress(duplicateId);
            if (routingEntity != null && routingEntity.isOnline()) {
                if (routingEntity.getType() == RoutingEntity.Type.BLE_MESH) {
                    RoutingEntity hopeEntity = RouteManager.getInstance().getEntityByAddress(routingEntity.getHopAddress());
                    byte[] disconnectionMessageData = JsonDataBuilder.buildDisconnectMessage();
                    MeshLog.v("[P2P_Process] Send disconnect message an clear");
                    bleTransport.sendMessage(hopeEntity.getAddress(), disconnectionMessageData);
                    HandlerUtil.postBackground(() -> RouteManager.getInstance().updateNodeAsOffline(null, hopeEntity.getAddress()), 200L);
                } else {
                    MeshLog.e("[P2P_Process] This user not my ble mesh user: " + AddressUtil.makeShortAddress(duplicateId));
                }
            }
        } else {
            MeshLog.e("[P2P_Process] I am not actual receiver. Need to forward");
        }
    }

    @Override
    public void onV2ReceivedFailedMessageAck(String source, String destination, String hop, String messageId) {
        PendingMessage pendingMessage = connectionLinkCache.getPendingMessage(messageId);

        if (pendingMessage == null) return;

        RoutingEntity routingEntity = RouteManager.getInstance().getEntityByDestinationAndHop(destination, hop);
        if (routingEntity != null) {
            MeshLog.v("[p2p_process] message failed ack received. User ID: "
                    + AddressUtil.makeShortAddress(routingEntity.getAddress()));
        }

        List<RoutingEntity> offlineEntities = RouteManager.getInstance().makeUserOffline(routingEntity);
        if (CollectionUtil.hasItem(offlineEntities)) {
            for (RoutingEntity entity : offlineEntities) {
                // Todo may be we have to manage buyer disconnection section here
                linkStateListener.onUserDisconnected(entity.getAddress());
            }
        }

        if (pendingMessage.routeQueue.isEmpty()) {
            if (!myNodeId.equals(source)) {
                byte[] failedAck = JsonDataBuilder.buildFailedMessageAck(myNodeId, source, destination, messageId);
                RoutingEntity previousEntity = RouteManager.getInstance().getShortestPath(pendingMessage.previousSender);
                if (previousEntity.getType() == RoutingEntity.Type.WiFi) {
                    sendAppMessage(pendingMessage.messageId, previousEntity.getIp(), failedAck);
                } else if (previousEntity.getType() == RoutingEntity.Type.BLE) {
                    bleTransport.addAppMessageInQueue(pendingMessage.messageId, previousEntity.getAddress(), failedAck);
                } else {
                    //TODO other transport
                }
            }
        } else {
            RoutingEntity nextEntity = pendingMessage.routeQueue.poll();
            MeshLog.e("[p2p_process] onV2ReceivedFailedMessageAck nextEntity info " + nextEntity.toString());

            RoutingEntity nextShortest = RouteManager.getInstance().getShortestPath(nextEntity.getHopAddress());
            if (nextShortest != null) {
                if (nextShortest.getType() == RoutingEntity.Type.WiFi) {
                    sendAppMessage(pendingMessage.messageId, nextShortest.getIp(), pendingMessage.messageData);
                } else if (nextShortest.getType() == RoutingEntity.Type.BLE) {
                    bleTransport.addAppMessageInQueue(pendingMessage.messageId, nextShortest.getAddress(), pendingMessage.messageData);
                } else {
                    //TODO other transport
                }
            }
        }
    }


    /*************************** End v2 method *************************/

    /**
     * Received Direct User From Wifi Client
     *
     * @param senderInfo sender's details
     * @param btUser     sender's connected bt user details
     * @param btMeshUser sender's connected bt mesh user details
     */

    @Override
    public void onReceivedDirectUserFromWifiClient(String senderInfo, String btUser, String btMeshUser) {
    }

    @Override
    public void onReceivedFilePacket(byte[] data) {

    }

    /**
     * Received Direct User From Wifi-Master
     *
     * @param senderInfo   master node's details
     * @param btUser       mater's bt user
     * @param btMeshUser   mater's bt mesh user
     * @param wifiUser     mater's wifi user
     * @param wifiMeshUser mater's wifi mesh user
     */

    @Override
    public void onReceivedDirectUserFromWifiMaster(String senderInfo, String btUser,
                                                   String btMeshUser, String wifiUser,
                                                   String wifiMeshUser) {
    }


    /**
     * Mesh User found
     * 1. Update UI AND DB
     * 2. Pass This User To Ble User
     * 3. Pass This User To Wifi Users, if Master
     *
     * @param senderId     hopNodeId of discovered nodeId
     * @param userListJson connected user's json
     */

    @Override
    public void onMeshLinkFound(String senderId, String hopNodeId, String userListJson) {
    }

    /**
     * Mesh user disconnected
     * 1. Update UI AND DB
     * 2. Send to wifi users; if master without the forwarder
     * 3. Send to Ble User
     *
     * @param nodeIds     disconnected nodeIds
     * @param forwarderId message forwarderId
     */
    @Override
    public void onMeshLinkDisconnect(String nodeIds, String forwarderId) {
        //TODO: Direct and Mesh Disconnect will manage separately in future

        List<DisconnectionModel> disconnectedNodeList = GsonUtil.on().getDisconnectedNodeList(nodeIds);

        List<RoutingEntity> offlineRoutingEntities = new ArrayList<>();

        for (DisconnectionModel model : disconnectedNodeList) {
            List<RoutingEntity> updateNodeAsOffline;

            if (model.getUserType() == RoutingEntity.Type.WiFi) {
                updateNodeAsOffline = RouteManager.getInstance().updateNodeAsOffline("", model.getNodeId());
            } else {
                updateNodeAsOffline = RouteManager.getInstance().updateNodeAsOffline(forwarderId, model.getNodeId());
            }

            if (CollectionUtil.hasItem(updateNodeAsOffline)) {
                offlineRoutingEntities.addAll(updateNodeAsOffline);
            }
        }

        MeshLog.w("[Disconnect] on Wifi MeshLink Disconnect" + nodeIds + " FORWARDER:" +
                AddressUtil.makeShortAddress(forwarderId));

        if (!CollectionUtil.hasItem(offlineRoutingEntities)) return;

        for (RoutingEntity routingEntity : offlineRoutingEntities) {
            linkStateListener.onUserDisconnected(routingEntity.getAddress());
            linkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());

            RoutingEntity entity = RouteManager.getInstance().getEntityByAddress(routingEntity.getAddress());
            if (entity == null || !entity.isOnline()) {
                if (remoteTransport != null) {
                    remoteTransport.onBuyerDisconnected(routingEntity.getAddress());
                }
            }
//            connectionLinkCache.removeNodeInfo(routingEntity.getAddress());
        }

        List<RoutingEntity> buyersInternetOfflineUserList = new ArrayList<>();
        for (RoutingEntity entity : offlineRoutingEntities) {
            if (entity.getType() == RoutingEntity.Type.INTERNET) {
                buyersInternetOfflineUserList.add(entity);
            }
        }

        MeshLog.v("[p2p_process] offline calculation in wifi transport. before filter: " + offlineRoutingEntities.toString());
        offlineRoutingEntities.removeAll(buyersInternetOfflineUserList);
        MeshLog.v("[p2p_process] offline calculation in wifi transport. after filter: " + offlineRoutingEntities.toString());

        List<RoutingEntity> validOfflineList = new ArrayList<>();
        for (RoutingEntity entity : offlineRoutingEntities) {
            RoutingEntity validEntity = RouteManager.getInstance().getEntityByAddress(entity.getAddress());
            if (validEntity == null || !validEntity.isOnline()) {
                validOfflineList.add(entity);
            }
        }

        // send to ble users
        List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();

        for (RoutingEntity bleEntity : bleUserList) {
            List<RoutingEntity> allPossiblePath = RouteManager.getInstance().getAllPossibleOnlinePathById(bleEntity.getAddress());

            if (allPossiblePath.size() > 1) {

                String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);
                List<RoutingEntity> bleUser = new ArrayList<>();
                bleUser.add(bleEntity);
                passOfflineEntitiesToBLE(leaveNodeList, bleUser);

            } else {
                if (CollectionUtil.hasItem(validOfflineList)) {
                    String leaveNodeList = buildNodeIdListJson(validOfflineList);

                    List<RoutingEntity> bleUser = new ArrayList<>();
                    bleUser.add(bleEntity);
                    // Send to ble user
                    passOfflineEntitiesToBLE(leaveNodeList, bleUser);
                }
            }
        }


        //passOfflineEntitiesToAdhoc(leaveNodeList);


    }


    private void passOfflineEntitiesToBLE(String leaveNodeList, List<RoutingEntity> bleUserList) {

        if (!TextUtils.isEmpty(leaveNodeList)) {
            MeshLog.i("[BLE_PROCESS] send the disconnected node to BLE user from wifi transport");
            for (RoutingEntity routingEntity : bleUserList) {
                bleTransport.sendMessage(routingEntity.getAddress(), JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList));
            }
        }
    }

    private int sendP2pHelloResponseToClient(RoutingEntity senderNodeInfo) {
        MeshLog.e("[BLE_PROCESS] mater send hello packet to :" + senderNodeInfo.getIp());
        byte[] data = JsonDataBuilder.prepareP2pWifiHelloPacketAsMaster(myNodeId, MASTER_IP_ADDRESS);
        addDiscoveryTaskInQueue("WiFI hello Client", true, true, senderNodeInfo.getIp(),
                () -> data);
        return -1;
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


    @Override
    public void onReceivedMsgAck(String sender, String receiver, String messageId, int status, String ackBody, String immediateSender) {
        MeshLog.v("(-) MSG ACK RECEVED " + sender.substring(sender.length() - 3) + "-->  "
                + receiver.substring(receiver.length() - 3) + "--> " + messageId + "status : " + status);

        //Remove pending message
        connectionLinkCache.removePendingMessage(messageId);

        if (receiver.equals(myNodeId)) {
            try {
                JSONObject js = new JSONObject(ackBody);

                if (js.getInt(PurchaseConstants.JSON_KEYS.ACK_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND_ACK) {

                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
//                    internetTransport.processInternetOutgoingMessageACK(sender, originalReceiver, messageId, status);
                    remoteTransport.sendBuyerReceivedAck(sender, originalReceiver, messageId, status, ackBody);
                } else {

                    linkStateListener.onMessageDelivered(messageId, status);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            sendMessageAckToOtherMesh(sender, receiver, messageId, status, ackBody, immediateSender);
        }
    }

    @Override
    public void onReceiveRouteInfoUpdate(String routeInfoFor, String newHopAddress, long time) {
        if (Text.isNotEmpty(routeInfoFor) && Text.isNotEmpty(newHopAddress)) {
            MeshLog.i("[Cycle-WiFi] RouteInfo update request arrived for:" + routeInfoFor + " hop:" + newHopAddress);
            NodeInfo nodeInfo = connectionLinkCache.getNodeInfoById(routeInfoFor);
            if (nodeInfo != null) {
                nodeInfo.setHopId(newHopAddress);
                nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                nodeInfo.mGenerationTime = time;
                ConcurrentLinkedQueue<NodeInfo> nodeInfoList = new ConcurrentLinkedQueue<>();
                nodeInfoList.add(nodeInfo);
                updateRoutingTableAndUI(nodeInfoList);
            } else {
                MeshLog.e("[Cycle-WiFi] NODE info NULL in Route info update");
            }

        }
    }

    @Override
    public void onReceiveDisconnectRequest(String senderAddress) {
        MeshLog.w("[Cycle-WiFi] onReceiveDisconnectRequest:" +
                AddressUtil.makeShortAddress(senderAddress) + ". Less expected, a client would request " +
                "for BT connection iff it is not connected anymore or has not synced with node info " +
                "yet!!!");
        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(senderAddress);
        if (routingEntity != null) {
            if (routingEntity.getIp() != null && routingEntity.getIp().equals(P2P_MASTER_IP_ADDRESS)
                    && P2PUtil.isMeLC()) {
                //We are disabling to disconnect from GO and expected that our automated mechanism
                // automatically re initiate whole discovery process
                disableWifiAdapter();

            } else if (P2PUtil.isMeGO()) {
                MeshLog.w("[Cycle-WiFi] GO received disconnection request");
            }
        }
    }

    @Override
    public void onMessageReceived(String sender, String receiver, String messageId, byte[] frameData, String ipAddress, String immediateSender) {
        MeshLog.e("[BLE_PROCESS] Wifi message received :" + new String(frameData));
        MeshLog.e("[BLE_PROCESS] Wifi message sender :" + sender + "   Receiver: " + receiver);
        if (receiver.equals(myNodeId)) {

            try {
                JSONObject js = new JSONObject(new String(frameData));
                byte[] msg_data = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_DATA).getBytes();

                if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
                    remoteTransport.processInternetOutgoingMessage(sender, originalReceiver, messageId, msg_data, false, 0);
                } else if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE) {
                    String sellerId = js.getString(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS);
                    String ackBody = Util.buildInternetSendingAckBody(sender);
                    linkStateListener.onMessageReceived(sender, msg_data);

                    sendMessageAck(receiver, sellerId, messageId, Constant.MessageStatus.RECEIVED, ackBody, ipAddress, immediateSender);
                } else {
                    linkStateListener.onMessageReceived(sender, msg_data);

                    MeshLog.v("(-) MSG ACK SEND" + sender.substring(sender.length() - 3) + "-> "
                            + receiver.substring(receiver.length() - 3) + "-->" + messageId);

                    if (!TextUtils.isEmpty(messageId)) {
                        String ackBody = Util.buildLocalAckBody();

//                    RoutingEntity realEntity = RouteManager.getInstance().getRoutingEntityByAddress(sender);
                        sendMessageAck(receiver, sender, messageId, Constant.MessageStatus.RECEIVED, ackBody, ipAddress, immediateSender);
                   /* if (realEntity != null && realEntity.getType() != RoutingEntity.Type.WiFi) {
                        sendMessageAck(receiver, sender, messageId, Constant.MessageStatus.RECEIVED, ackBody);
                    }*/
                    }
                }
            } catch (JSONException e) {
                MeshLog.p("JSONException  " + e.getMessage());
            }
        } else {

            int transferId = sendMessageToOtherMesh(sender, receiver, messageId, frameData, ipAddress, immediateSender);

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
    public void onReceiveBuyerFileMessage(String sender, String receiver, String messageData, int fileMessageType, String immediateSender, String messageId) {
        MeshLog.v("FILE_SPEED_TEST_6 " + Calendar.getInstance().getTime());
        try {
            if (receiver.equals(myNodeId)) {
                MeshLog.v("Buyer_file received in wifi transport");
                JSONObject js = new JSONObject(messageData);
                String msg_data = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_DATA);

                if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);

                    if (fileMessageType == Constant.FileMessageType.FILE_ACK_MESSAGE || fileMessageType == Constant.FileMessageType.FILE_INFO_MESSAGE) {
                        byte[] message = JsonDataBuilder.buildBuyerFileMessage(sender, originalReceiver, msg_data.getBytes(), fileMessageType, messageId);

                        MeshLog.v("FILE ACK SIZE " + message.length);

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
                        remoteTransport.processInternetOutgoingMessage(sender, originalReceiver, messageId, msg_data.getBytes(), true, fileMessageType);
                    }
                } else if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE) {
                    String sellerId = js.getString(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS);
                    JSONObject msgObject = new JSONObject(msg_data);
                    msgObject.put(JsonDataBuilder.KEY_IMMEDIATE_SENDER, sellerId);
                    linkStateListener.onFileMessageReceived(sender, msgObject.toString());
                }
            } else {
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);

                if (routingEntity != null) {
                    MeshLog.v("Buyer_file received in wifi transport need to pass other user :" + AddressUtil.makeShortAddress(routingEntity.getAddress()));

                    byte[] data = JsonDataBuilder.buildBuyerFileMessage(sender, receiver, messageData.getBytes(), fileMessageType, messageId);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                        sendAppMessage(routingEntity.getIp(), data);
                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        bleTransport.addFileMessageInQueue(routingEntity.getAddress(), messageId, data);
                        /*boolean isSuccess = bluetoothTransport.sendMeshMessage(routingEntity.getAddress(), data);
                        if (!isSuccess) {
                            bleTransport.sendFileMessage(routingEntity.getAddress(), data);
                        }*/
                    }
                } else {
                    MeshLog.v("WiFi buyer file message route null");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPaymentDataReceived(String senderId, String receiver, String messageId, byte[] payMesg) {

        //Todo tariqul we have to manage immediate sender here

        MeshLog.p("onPaymentDataReceived");
        if (receiver.equals(myNodeId)) {
            MeshLog.v("my message");
            linkStateListener.onMessagePayReceived(senderId, payMesg);
            sendPayMessageAck(receiver, senderId, messageId);
        } else {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiver, RoutingEntity.Type.WiFi);
            if (routingEntity != null) {
                MeshLog.v("(P) RoutingEntity" + routingEntity.toString());
                byte[] msgBody = JsonDataBuilder.buildPayMessage(senderId, receiver, messageId, payMesg);

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("(P) sendMessage Wifi user");
                    sendMeshMessage(routingEntity.getIp(), msgBody);
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
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    MeshLog.v("(P) sendMessage ble user");
                    bleTransport.sendMessage(routingEntity.getAddress(), msgBody);
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
            linkStateListener.onPayMessageAckReceived(sender, receiver, messageId);
        } else {
            sendPayMessageAck(sender, receiver, messageId);
        }
    }

    private void sendPayMessageAck(String sender, String receiver, String messageId) {
        //Todo tariqul we have to manage immediate sender here

        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiver, RoutingEntity.Type.WiFi);
        if (routingEntity != null) {
            MeshLog.v("(P ack) RoutingEntity" + routingEntity.toString());
            byte[] msgBody = JsonDataBuilder.buildPayMessageAck(sender, receiver, messageId);
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                MeshLog.v("(P) sendMessage Wifi user");
                sendMeshMessage(routingEntity.getIp(), msgBody);
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
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                MeshLog.v("(P) send ack message  ble user");
                bleTransport.sendMessage(routingEntity.getAddress(), msgBody);
            }
        } else {
            MeshLog.v(" (P) sendMessage User does not exist in routing table");
        }
    }


    private int sendMessageToOtherMesh(String sender, String receiver, String messageId, byte[] data, String senderIp, String senderId) {
        int transferId = 0;

        PendingMessage oldPendingMsg = connectionLinkCache.getPendingMessage(messageId);
        if (oldPendingMsg != null) {
            MeshLog.v("WiFi duplicate message received :" + AddressUtil.makeShortAddress(sender));
            oldPendingMsg.previousAttemptEntity = null; //To protect direct connection offline, just retry through other path
            middleManMessageSendStatus(messageId, false);
            return -1;
        }

        String immediateSender = "";
        RoutingEntity immediateSenderEntity = RouteManager.getInstance().getWiFiEntityByIp(senderIp);
        byte[] message = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);

        if (immediateSenderEntity == null) {
            // Immediate routing entity null make it online and pass to other connected users
            immediateSenderEntity = updateMissingEntity(senderId, senderIp);
        }
        immediateSender = immediateSenderEntity.getAddress();

        RoutingEntity routingEntity = RouteManager.getInstance()
                .getNextNodeEntityByReceiverAddress(immediateSender, receiver, RoutingEntity.Type.WiFi);
        //Add message in pending queue
        PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver,
                immediateSenderEntity.getAddress(), message, routingEntity);
        List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
        pendingMessage.routeQueue.addAll(allPossiblePath);
        connectionLinkCache.addPendingMessage(messageId, pendingMessage);

        if (routingEntity != null) {

            MeshLog.e("Next hop ID to send message " + AddressUtil.makeShortAddress(routingEntity.getAddress()) + " Type: " + routingEntity.getType());

            HandlerUtil.postForeground(() -> Toast.makeText(context, "Wifi hop S :" + AddressUtil.makeShortAddress(sender)
                    + " R: " + AddressUtil.makeShortAddress(receiver), Toast.LENGTH_SHORT).show());

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendAppMessage(messageId, routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                transferId = adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    transferId = messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                } else {
                    MeshLog.w(" BLE LINK NOT FOUND FOR DENDING MSG IN MESH");
                }
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                MeshLog.e("[BLE_PROCESS] send message wifi to BLE: " + routingEntity.getAddress());
                bleTransport.addAppMessageInQueue(messageId, routingEntity.getAddress(), message);

                //bleTransport.sendMessage(routingEntity.getAddress(), message);
            } else {
                MeshLog.v("WT User not found to send the local message");

            }
        } else {
            MeshLog.e("Routing Entity NULL on sendMessageToOtherMesh");
            middleManMessageSendStatus(messageId, false);
        }

        return transferId;
    }

    private RoutingEntity updateMissingEntity(String senderId, String senderIp) {
        RoutingEntity immediateSenderEntity = new RoutingEntity(senderId);
        immediateSenderEntity.setIp(senderIp);
        immediateSenderEntity.setType(RoutingEntity.Type.WiFi);
        immediateSenderEntity.setOnline(true);
        immediateSenderEntity.setHopAddress(null);
        immediateSenderEntity.setHopCount(0);
        RouteManager.getInstance().updateEntity(immediateSenderEntity);

        String dataId = JsonDataBuilder.getDataGenerationId();
        ConcurrentLinkedQueue<RoutingEntity> updateEntityListForBle = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<RoutingEntity> updateEntityListForWiFi = new ConcurrentLinkedQueue<>();
        updateEntityListForBle.add(immediateSenderEntity);
        updateEntityListForWiFi.add(immediateSenderEntity);

        //Add current data id to exist list
        AddressUtil.addDataGenerationId(dataId);
        v2PassDiscoveredMeshNodesToBleUsers(updateEntityListForBle, null, dataId);
        v2SendDiscoveredUserToLcUsers(immediateSenderEntity.getAddress(), updateEntityListForWiFi, null, dataId);

        linkStateListener.onLocalUserConnected(immediateSenderEntity.getAddress(), immediateSenderEntity.getPublicKey());

        return immediateSenderEntity;

    }

    private RoutingEntity updateMissingMeshEntity(String hopAddress, String userAddress, int type) {

        RoutingEntity updateMeshEntity = new RoutingEntity(userAddress);
        updateMeshEntity.setIp(null);
        updateMeshEntity.setType(type);
        updateMeshEntity.setOnline(true);
        updateMeshEntity.setHopAddress(hopAddress);
        updateMeshEntity.setHopCount(1);
        RouteManager.getInstance().updateEntity(updateMeshEntity);

        String dataId = JsonDataBuilder.getDataGenerationId();
        ConcurrentLinkedQueue<RoutingEntity> updateEntityListForBle = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<RoutingEntity> updateEntityListForWiFi = new ConcurrentLinkedQueue<>();
        updateEntityListForBle.add(updateMeshEntity);
        updateEntityListForWiFi.add(updateMeshEntity);

        //Add current data id to exist list
        AddressUtil.addDataGenerationId(dataId);
        v2PassDiscoveredMeshNodesToBleUsers(updateEntityListForBle, null, dataId);
        //v2SendDiscoveredUserToLcUsers(immediateSenderEntity.getAddress(), updateEntityListForWiFi, null, dataId);

        linkStateListener.onLocalUserConnected(updateMeshEntity.getAddress(), updateMeshEntity.getPublicKey());

        return updateMeshEntity;

    }

    private void sendMessageAckToOtherMesh(String sender, String receiver, String messageId, int status, String ackBody, String immediateSender) {

        //RoutingEntity immediateSenderEntity = RouteManager.getInstance().getWiFiEntityByIp(ipAddress);
        byte[] data = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);

        /*String immediateSender = "";
        if (immediateSenderEntity != null) {
            immediateSender = immediateSenderEntity.getAddress();
        }*/

        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(immediateSender, receiver, RoutingEntity.Type.WiFi);

        //Add message in pending queue
        /*PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, immediateSenderEntity.getAddress(), data, routingEntity);
        List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
        pendingMessage.routeQueue.addAll(allPossiblePath);

        connectionLinkCache.addPendingMessage(messageId, pendingMessage);*/

        if (routingEntity != null) {

            MeshLog.i(" Send message =" + routingEntity.toString());
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendAppMessage(routingEntity.getIp(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendMeshMessage(routingEntity.getIp(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                bleTransport.addAppMessageInQueue(messageId, routingEntity.getAddress(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, data));
                } else {
                    MeshLog.w(" BLE LINK NOT FOUND");
                }
            } else {
                remoteTransport.sendBuyerReceivedAck(sender, receiver, messageId, status, ackBody);

            }
        } else {
            //middleManMessageSendStatus(messageId, false);
        }

    }

    public void sendMessageAck(String sender, String receiver, String messageId, int status, String ackBody, String ipAddress, String immediateSender) {
        byte[] ackMessage = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);


        RoutingEntity immediateSenderEntity = RouteManager.getInstance().getWiFiEntityByIp(ipAddress);
        if (immediateSenderEntity != null) {
            immediateSender = immediateSenderEntity.getAddress();
        } else {
            updateMissingEntity(immediateSender, ipAddress);
        }


        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiver, RoutingEntity.Type.WiFi);
        if (routingEntity != null) {
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                sendAppMessage(routingEntity.getIp(), ackMessage);
            } else {
                bleTransport.sendMessage(routingEntity.getAddress(), ackMessage);
            }
        } else {
            updateMissingMeshEntity(immediateSender, receiver, RoutingEntity.Type.WifiMesh);
            sendMessageAck(sender, receiver, messageId, status, ackBody, ipAddress, immediateSender);
            MeshLog.e("[BLE_PROCESS] send Message Ack failed Routing entity:" + routingEntity + ":Receiver:" + receiver);
        }
    }

    public int sendMeshMessage(String ip, byte[] data) {
        return addDiscoveryTaskInQueue("App message", false, false, ip, () -> data);
    }


    public void passUserInfo(String ip, byte[] data) {
        messageDispatcher.addSendMessage(MessageBuilder.buildWiFiDiscoveryMeshMessage(ip, () -> data));
    }

    public void disableWifiAdapter() {
        TransportManagerX transportManagerX = TransportManagerX.getInstance();
        if (transportManagerX.getExistingRole() == TransportManagerX.MODE_CLIENT) {
            HardwareStateManager hardwareStateManager = new HardwareStateManager();
            hardwareStateManager.init(context);
            hardwareStateManager.disableWifi(isEnable ->
                    MeshLog.v("[WIFI]Requested to turn off. Disabling done:" + !isEnable));
            MeshLog.w("[Cycle-WiFi]WiFi turning off upon direct connection cycle formation");
            //Counter steps are automatically taken from TransPortMangerX or WiFiDirectLegacyManger
        }
    }

    @Override
    public void onReceiverInternetUserLocally(String sender, String receiver, String sellerId, String userList) {
        MeshLog.v("Wifi onReceiverInternetUserLocally =" + receiver);
        if (receiver.equalsIgnoreCase(myNodeId)) {

//            linkStateListener.onCurrentSellerId(sellerId);
            /*if (remoteTransport.amIDirectUser()) {
                return;
            }*/

            MeshLog.i("[Internet_Process] local internet user received: " + userList);
            ConcurrentLinkedQueue<RoutingEntity> userNodeInfoList = GsonUtil.on().getEntityQueue(userList);

            for (RoutingEntity item : userNodeInfoList) {

                if (TextUtils.isEmpty(item.getAddress()) || item.getAddress().equals(myNodeId)) {
                    continue;
                }


                /*if (RouteManager.getInstance().isOnline(item.getAddress())) {
                    linkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());
                    continue;
                }*/

                RoutingEntity oldRoutingEntity = RouteManager.getInstance()
                        .getEntityByDestinationAndHop(item.getAddress(), item.getHopAddress());

                if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {


                    // we will update old routing entity by type only. Because it is internet section

                    if (oldRoutingEntity.getType() == RoutingEntity.Type.INTERNET) {

                        oldRoutingEntity.setType(RoutingEntity.Type.INTERNET);
                        oldRoutingEntity.setHopCount(item.getHopCount());
                        oldRoutingEntity.setHopAddress(item.getHopAddress());

                        boolean isReplaced = RouteManager.getInstance().replaceRoute(oldRoutingEntity);

                        if (isReplaced) {
                            linkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());
                        }

                        continue;
                    }
                }

                boolean updated = RouteManager.getInstance().insertRoute(item);
                if (updated) {
                    item.setType(RoutingEntity.Type.INTERNET);

                    MeshLog.v("Wifi transport interent user ids send success=" + item);

                    linkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());
                }
            }
        } else {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);
            if (routingEntity != null) {

                // Increased hop count
                ConcurrentLinkedQueue<RoutingEntity> userNodeInfoList = GsonUtil.on().getEntityQueue(userList);
                for (RoutingEntity r : userNodeInfoList) {
                    r.setHopCount(r.getHopCount() + 1);
                }

                userList = GsonUtil.on().toJsonFromEntityList(userNodeInfoList);

                byte[] internetIdList = JsonDataBuilder.buildInternetUserIds(myNodeId, receiver, sellerId, userList);

                MeshLog.v("(-) RoutingEntity" + routingEntity.toString());
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("senUserList Wifi user");
                    sendMeshMessage(routingEntity.getIp(), internetIdList);

                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), internetIdList);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    MeshLog.v("senUserList ble user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> internetIdList));
                    } else {
                        MeshLog.v("senUserList BLE LINK NOT FOUND");
                    }
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    bleTransport.sendMessage(routingEntity.getAddress(), internetIdList);
                }
            } else {
                MeshLog.v("senUserList User does not exist in routing table");
            }
        }
    }

    @Override
    public void onInternetDirectUserReceived(String nodeId, ConcurrentLinkedQueue<NodeInfo> userNodeInfoList, Link link) {

    }

    @Override
    public void onInternetUserReceived(String nodeId, ConcurrentLinkedQueue<RoutingEntity> userNodeInfoList) {

    }

    @Override
    public void onInternetUserLeave(String sender, String receiver, String userList) {
        if (receiver.equals(myNodeId)) {

            /*if (remoteTransport.amIDirectUser())
                return;*/

            MeshLog.v("[Internet] onInternetUserLeave" + userList);
            String[] userIdArray = TextUtils.split(userList, "@");

            for (String id : userIdArray) {

                RouteManager.getInstance().makeInternetUserOffline(sender, Arrays.asList(userIdArray));

                linkStateListener.onUserDisconnected(id);
                //RouteManager.getInstance().updateNodeAsOffline(id);
            }

        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);

            if (routingEntity != null) {
                byte[] userListMessage = JsonDataBuilder.prepareInternetLeaveMessage(sender, receiver, userList);

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.mm("[Internet] forwarding disconnected user to other via WiFi");
                    sendMeshMessage(routingEntity.getIp(), userListMessage);
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
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    MeshLog.mm("[Internet] forwarding disconnected user to other via BLE");
                    bleTransport.sendMessage(routingEntity.getAddress(), userListMessage);
                }
            }
        }
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
        String receiver = handshakeInfo.getReceiverId();
        String sender = handshakeInfo.getSenderId();

        if (myNodeId.equals(receiver)) {

            String targetId = handshakeInfo.getTargetReceiver();

            if (receiver.equalsIgnoreCase(targetId)) {
                linkStateListener.onHandshakeInfoReceived(handshakeInfo);
            } else {
                handshakeInfo.setReceiverId(targetId);
                processHopSendData(handshakeInfo);
            }

        } else {
            processHopSendData(handshakeInfo);
        }
    }

    private void processHopSendData(HandshakeInfo handshakeInfo) {
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(handshakeInfo.getReceiverId());
        if (routingEntity == null) {
            MeshLog.e("Routing Entity NULL on sendMessageToOtherMesh");
            return;
        }
        String handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);
        byte[] message = handshakeInfoText.getBytes();
        sendHopMessageToOthers(routingEntity, message);
    }

    private void sendHopMessageToOthers(RoutingEntity routingEntity, byte[] message) {
        if (routingEntity.getType() == RoutingEntity.Type.INTERNET) {
            remoteTransport.sendAppMessage(routingEntity.getAddress(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            sendMeshMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
            } else {
                MeshLog.w(" BLE LINK NOT FOUND FOR DENDING MSG IN MESH");
            }
        } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
            bleTransport.sendMessage(routingEntity.getAddress(), message);
        } else {
            MeshLog.v("WT User not found to send the local message");
        }
    }


    private void updateRoutingTableAndUI(ConcurrentLinkedQueue<NodeInfo> nodeInfoList) {
        if (CollectionUtil.hasItem(nodeInfoList)) {
            MeshLog.i("GET ::" + nodeInfoList.toString());

            for (NodeInfo nodeInfo : nodeInfoList) {

                boolean isUpdated = updateRouteWithCurrentTime(nodeInfo);
                if (isUpdated) {
                    addNodeInformation(nodeInfo.getUserId(), nodeInfo);
                }

                MeshLog.e("[BLE_PROCESS] Routing table update status :" + isUpdated);
                if (isUpdated && RouteManager.getInstance().isOnline(nodeInfo.getUserId())) {
                    MeshLog.e("[BLE_PROCESS] Pass to app level :" + isUpdated);
                    linkStateListener.onLocalUserConnected(nodeInfo.getUserId(), nodeInfo.getPublicKey());
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

    private String buildNodeIdListJson(List<RoutingEntity> routingEntities) {
        if (CollectionUtil.hasItem(routingEntities)) {

            List<DisconnectionModel> disconnectionModelList = new ArrayList<>();
            for (RoutingEntity entity : routingEntities) {
                DisconnectionModel model = new DisconnectionModel();
                model.setNodeId(entity.getAddress());

                model.setUserType(RoutingEntity.Type.BLE_MESH);


                disconnectionModelList.add(model);
            }


            return GsonUtil.on().toJsonFromDisconnectionList(disconnectionModelList);
        }
        return null;

    }

    @Override
    public void onReceiveNewRole(String sender, String receiver, int role) {
        if (myNodeId.equals(receiver)) {
            int previousRole = connectionLinkCache.setNewUserRole(sender, role);
            linkStateListener.onUserModeSwitch(sender, role, previousRole);
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.WiFi);
            if (routingEntity == null) return;

            byte[] message = JsonDataBuilder.buildUserRoleSwitchMessage(sender, receiver, role);

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                MeshLog.v("[p2p_process] forwarding role switch message via WIFI");
                sendMeshMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                MeshLog.v("[p2p_process] forwarding role switch message via BLE");
                bleTransport.sendMessage(routingEntity.getAddress(), message);
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
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(immediateSender, receiver, RoutingEntity.Type.WiFi);

            if (routingEntity != null) {
                MeshLog.v("Multihop received in WIFI transport need to pass other user :" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
                byte[] data = JsonDataBuilder.buildFiledMessage(sender, receiver, messageId, message.getBytes());

                PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver,
                        immediateSender, data, routingEntity);
                List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
                pendingMessage.routeQueue.addAll(allPossiblePath);
                connectionLinkCache.addPendingMessage(messageId, pendingMessage);

                if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                    sendAppMessage(routingEntity.getIp(), data);
                } else {
                    bleTransport.addFileMessageInQueue(receiver, messageId, data);
                    /*boolean isSuccess = bluetoothTransport.sendMeshMessage(routingEntity.getAddress(), data);
                    if (!isSuccess) {
                        bleTransport.sendFileMessage(routingEntity.getAddress(), data);
                    }*/
                }
            } else {
                MeshLog.v("WiFi buyer file message route null");
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


    /**
     * <h1>Discovery message queue add task</h1>
     *
     * @param addInFirst : boolean add current task first or last
     * @param ipAddr:    (required) String receiver id address
     * @param dataPuller : interface for instant data prepare
     */
    public synchronized int addDiscoveryTaskInQueue(String title, boolean isHelloPacket,
                                                    boolean addInFirst, String ipAddr,
                                                    DiscoveryTask.DataPuller dataPuller) {
        int internalId = UUID.randomUUID().toString().hashCode();

        if (addInFirst) {
            messageEventQueue.addDiscoveryTaskInFirst(new DiscoveryTask(ipAddr, dataPuller, isHelloPacket) {
                @Override
                public void run() {
                    try {
                        boolean isSuccess = false;
                        byte[] data = this.puller.getData();
                        while (this.retryCount < this.maxRetryCount) {
                            this.retryCount++;
                            int result = MeshHttpServer.on().sendMessage(this.receiverId, data, this.mTimeOut);
                            isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                            if (isSuccess) {
                                break;
                            }
                            MeshLog.i("Wifi message send retry count :" + this.retryCount + " status :" + isSuccess);
                        }

                        if (this.isHelloPacket) {
                            meshMessageListener.onWifiHelloMessageSend(this.receiverId, isSuccess);
                        } else {
                            meshMessageListener.onMessageSend(this.messageInternalId, this.receiverId, isSuccess);
                        }

                    } catch (ExecutionException | InterruptedException e) {
                        MeshLog.i("Wifi message send error : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } else {
            messageEventQueue.addDiscoveryTaskInLast(new DiscoveryTask(ipAddr, dataPuller, isHelloPacket) {
                @Override
                public void run() {
                    try {
                        boolean isSuccess = false;
                        while (this.retryCount < this.maxRetryCount) {
                            this.retryCount++;
                            int result = MeshHttpServer.on().sendMessage(this.receiverId, this.puller.getData(), this.mTimeOut);
                            isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                            if (isSuccess) {
                                break;
                            }
                            MeshLog.i("Wifi message send retry count :" + this.retryCount + " status :" + isSuccess);
                        }

                        if (this.isHelloPacket) {
                            meshMessageListener.onWifiHelloMessageSend(this.receiverId, isSuccess);
                        } else {
                            meshMessageListener.onMessageSend(this.messageInternalId, this.receiverId, isSuccess);
                        }

                    } catch (ExecutionException | InterruptedException e) {
                        MeshLog.i("Wifi message send error : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
        return internalId;

    }

    public int sendAppMessage(String messageId, String ip, byte[] messageData) {
        int internalId = UUID.randomUUID().toString().hashCode();
        messageEventQueue.addAppMessageInQueue(new DiscoveryTask(messageId, ip, messageData) {
            @Override
            public void run() {
                try {
                    boolean isSuccess = false;
                    while (this.retryCount < this.maxRetryCount) {
                        this.retryCount++;
                        int result = MeshHttpServer.on().sendMessage(this.ipOrAddress, this.messageData, this.mTimeOut);
                        isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        if (isSuccess) {
                            break;
                        }
                        MeshLog.i("Wifi message send retry count :" + this.retryCount + " status :" + isSuccess);
                    }
                    middleManMessageSendStatus(this.messagePublicId, isSuccess);
                } catch (ExecutionException | InterruptedException e) {
                    MeshLog.i("Wifi message send error : " + e.getMessage());
                    e.printStackTrace();
                    middleManMessageSendStatus(this.messagePublicId, false);
                }
            }
        });
        return internalId;
    }

    public int sendAppMessage(String ipAddress, byte[] data) {
        int internalId = UUID.randomUUID().toString().hashCode();
        messageEventQueue.addAppMessageInQueue(new DiscoveryTask(null, ipAddress, data) {
            @Override
            public void run() {
                try {
                    boolean isSuccess = false;
                    while (this.retryCount < this.maxRetryCount) {
                        this.retryCount++;
                        int result = MeshHttpServer.on().sendMessage(this.ipOrAddress, this.messageData, this.mTimeOut);
                        isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        if (isSuccess) {
                            break;
                        }
                        MeshLog.i("Wifi message send retry count :" + this.retryCount + " status :" + isSuccess);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    MeshLog.i("Wifi message send error : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        return internalId;
    }


    private void middleManMessageSendStatus(String messageId, boolean isSuccess) {

        if (TextUtils.isEmpty(messageId)) return;
        String[] messageIdToken = messageId.split("_");
        boolean isFileMessage = false;
        if (messageIdToken.length > 1) {
            if (TextUtils.isDigitsOnly(messageIdToken[1])) {
                isFileMessage = true;
            }
        }


        if (isSuccess) {
            if (isFileMessage) {
                MeshLog.v("[Ble_process] middle man wifi packet send state : " + isSuccess);
                connectionLinkCache.removePendingMessage(messageId);
            }
        } else {
            MeshLog.e("[Middleman] wifi attempt to send other path");

            PendingMessage pendingMessage = connectionLinkCache.getPendingMessage(messageId);
            if (pendingMessage != null) {

                List<RoutingEntity> offlineEntities = RouteManager.getInstance()
                        .makeUserOffline(pendingMessage.previousAttemptEntity);

                if (CollectionUtil.hasItem(offlineEntities)) {
                    for (RoutingEntity entity : offlineEntities) {
                        // Todo may be we have to manage buyer disconnection section here
                        linkStateListener.onUserDisconnected(entity.getAddress());
                    }
                }

                if (pendingMessage.routeQueue.isEmpty()) {
                    MeshLog.v("[Middleman] wifi  queue is empty previous id :" + AddressUtil.makeShortAddress(pendingMessage.previousSender));

                    if (!myNodeId.equals(pendingMessage.previousSender)) {
                        byte[] failedAck = JsonDataBuilder.buildFailedMessageAck(myNodeId, pendingMessage.actualSender,
                                pendingMessage.actualReceiver, pendingMessage.messageId);
                        RoutingEntity entity = RouteManager.getInstance().getShortestPath(pendingMessage.previousSender);

                        if (entity != null) {
                            MeshLog.v("[Middleman] wifi  error message send to :" + AddressUtil.makeShortAddress(entity.getAddress()) + " hop :" + entity.getHopAddress());
                            if (entity.getType() == RoutingEntity.Type.BLE) {
                                bleTransport.addAppMessageInQueue(pendingMessage.messageId, entity.getAddress(), failedAck);
                            } else {
                                sendAppMessage(pendingMessage.messageId, entity.getIp(), failedAck);
                            }
                        }
                    }
                    connectionLinkCache.removePendingMessage(messageId);

                } else {
                    RoutingEntity routingEntity = pendingMessage.routeQueue.poll();
                    MeshLog.v("[Middleman] wifi  Try to other path " + routingEntity.getAddress());

                    if (!TextUtils.isEmpty(routingEntity.getHopAddress())) {
                        routingEntity = RouteManager.getInstance().getShortestPath(routingEntity.getHopAddress());
                    }

                    if (routingEntity == null) {
                        MeshLog.v("[Middleman] wifi  Next shortest routing path null");
                        return;
                    }
                    pendingMessage.previousAttemptEntity = routingEntity;
                    MeshLog.v("[Middleman] wifi  User has alternative paths " + routingEntity.getAddress());

                    if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        if (isFileMessage) {
                            bleTransport.addFileMessageInQueue(routingEntity.getAddress(), pendingMessage.messageId, pendingMessage.messageData);
                        } else {
                            bleTransport.addAppMessageInQueue(pendingMessage.messageId, routingEntity.getAddress(), pendingMessage.messageData);
                        }
                    } else {
                        sendAppMessage(pendingMessage.messageId, routingEntity.getIp(), pendingMessage.messageData);
                    }
                }
            } else {
                MeshLog.v("[Middleman] wifi  pending message not found");
            }
        }
    }


    //Todo we have to send message one after another
    public void prepareAndSendCredentialMessage(String ssid, String password, String goNodeId) {
        List<RoutingEntity> directWifiUsers = RouteManager.getInstance().getWifiUser();
        MeshLog.v("[BLE_PROCESS] send credential to other wifi users: " + directWifiUsers.size());
        if (CollectionUtil.hasItem(directWifiUsers)) {
            RoutingEntity masterEntity = null;

            for (RoutingEntity item : directWifiUsers) {
                byte[] data = JsonDataBuilder.prepareAPCredentialMessage(myNodeId, item.getAddress(), ssid, password, goNodeId);

                if (item.getIp().equals(MASTER_IP_ADDRESS)) {
                    masterEntity = item;
                } else {
                    //We are not add this message in queue. Because it is not need to add in queue for waiting
                    //addDiscoveryTaskInQueue("prepareAndSendCredentialMessage", false, true, item.getIp(), () -> data);
                    sendMessageAndGetCallBack(item.getIp(), data, null);
                }
            }

            if (masterEntity != null) {
                byte[] data = JsonDataBuilder.prepareAPCredentialMessage(myNodeId, masterEntity.getAddress(), ssid, password, goNodeId);
                //We are not add this message in queue. Because it is not need to add in queue for waiting
                // addDiscoveryTaskInQueue("prepareAndSendCredentialMessage", false, true, masterEntity.getIp(), () -> data);
                sendMessageAndGetCallBack(masterEntity.getIp(), data, null);
            }
        } else {
            MeshLog.v("No wifi user connected to receive credential message");
        }
    }

    /**
     * For log tracking
     *
     * @param logType     : String(required) type of log
     * @param sender      : String(required)
     * @param entityList: received entity list
     */
    private void printLog(String logType, String sender, ConcurrentLinkedQueue<RoutingEntity> entityList) {
        String nodes = "";
        if (CollectionUtil.hasItem(entityList)) {
            for (RoutingEntity item : entityList) {
                nodes = nodes + "," + AddressUtil.makeShortAddress(item.getAddress());
            }
        }
        MeshLog.v("[wifi-" + logType + "] Sender: " + AddressUtil.makeShortAddress(sender) + " Nodes: [" + nodes + "]");
    }
}
