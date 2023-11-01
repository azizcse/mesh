package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.ble.message.BleMessageDriver;
import com.w3engineers.mesh.ble.message.BleMessageHelper;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothClient;
import com.w3engineers.mesh.bluetooth.BluetoothServer;
import com.w3engineers.mesh.bluetooth.ConnectionState;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.datasharing.util.PurchaseConstants;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.helper.MiddleManMessageStatusListener;
import com.w3engineers.mesh.libmeshx.wifid.WifiCredential;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.DisconnectionModel;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.model.PendingMessage;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.queue.DiscoveryTask;
import com.w3engineers.mesh.queue.MeshMessageListener;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.tunnel.RemoteTransport;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.MessageCallback;
import com.w3engineers.mesh.wifi.TransportStateListener;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
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

public class BleTransport implements MeshTransport, ConnectionStateListener, MiddleManMessageStatusListener {

    public interface BleConnectionListener {
        void onBleUserConnected(boolean isServer, String userId);

        void onBleUserDisconnect(String nodeId);
    }

    private Context mContext;
    private LinkStateListener linkStateListener;
    private String myNodeId;
    private WifiTransPort wifiTransPort;
    private BleMessageDriver mBleMessageDriver;
    private DispatchQueue dispatchQueue;
    //private InternetTransport internetTransport;
    private RemoteTransport remoteTransport;
    private AdHocTransport adHocTransport;
    private MeshLibMessageEventQueue discoveryEventQueue;
    private TransportStateListener transportStateListener;
    private MeshMessageListener meshMessageListener;
    private ConnectionLinkCache connectionLinkCache;
    public BluetoothClient bluetoothClient;
    private BluetoothServer bluetoothServer;

    public BleTransport(Context context, String nodeId, LinkStateListener linkStateListener,
                        TransportStateListener transportStateListener, ConnectionLinkCache linkCache) {
        this.mContext = context;
        this.linkStateListener = linkStateListener;
        this.myNodeId = nodeId;
        this.dispatchQueue = new DispatchQueue();
        this.transportStateListener = transportStateListener;
        this.connectionLinkCache = linkCache;
        bluetoothServer = new BluetoothServer(nodeId, this);
        bluetoothClient = new BluetoothClient(nodeId, this, bluetoothServer);
    }

    @Override
    public void start() {
        MeshLog.v("[BLE_PROCESS] Ble transport started");

        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            bluetoothServer.starListenThread();
        }

        mBleMessageDriver = BleMessageDriver.getInstance();
        mBleMessageDriver.setGenericObject(this, discoveryEventQueue);
        mBleMessageDriver.setMiddleManMessageListener(this);
        BleManager.getInstance().initializeObject(bleConnectionListener);
    }

    @Override
    public void stop() {
        MeshLog.v("[BLE_PROCESS] Ble transport stopped");
        if (bluetoothServer != null) {
            bluetoothServer.stopListenThread();
        }
        if (bluetoothClient != null) {
            bluetoothClient.stop();
        }

        mBleMessageDriver.shutdown();
    }

    public <T> void setInterTransport(T... types) {
        for (T item : types) {
            if (item instanceof WifiTransPort) {
                wifiTransPort = (WifiTransPort) item;
            } else if (item instanceof RemoteTransport) {
                remoteTransport = (RemoteTransport) item;
            } else if (item instanceof AdHocTransport) {
                adHocTransport = (AdHocTransport) item;
            } else if (item instanceof MeshLibMessageEventQueue) {
                discoveryEventQueue = (MeshLibMessageEventQueue) item;
            } else if (item instanceof MeshMessageListener) {
                meshMessageListener = (MeshMessageListener) item;
            }
        }
    }

    public void connectBleDevice(BluetoothDevice device, ConnectionState state) {
        bluetoothClient.createConnection(device, state);
    }

    public boolean isBtConnected() {
        return BleLink.getBleLink() != null;
    }

    /**
     * <h1>Send confirmation message</h1>
     *
     * @param receiverId      (required) String
     * @param data            (required) byte array
     * @param messageCallback (required) Listener
     */
    public void sendConfirmationMessage(String receiverId, byte[] data, final MessageCallback messageCallback) {
        final String messageIdToCheck = mBleMessageDriver.sendMessage(receiverId, data);
        mBleMessageDriver.setMessageCallBack(new MessageCallback() {
            @Override
            public void onBleMessageSend(String messageId, boolean isSuccess) {
                if (messageIdToCheck.equals(messageId)) {
                    messageCallback.onMessageSend(isSuccess);
                }
            }
        });
    }


    public void sendMessage(String receiverId, byte[] data) {
        addDiscoveryTaskInQueue(false, receiverId, () -> data);
    }


    public String sendFileMessage(String receiverId, byte[] data) {
        return mBleMessageDriver.sendFileMessage(receiverId, data);
    }


    private BleConnectionListener bleConnectionListener = new BleConnectionListener() {
        @Override
        public void onBleUserConnected(boolean isServer, String userId) {
            MeshLog.e("[BLE_PROCESS] My ble role is server: " + isServer);
            if (!isServer) {
                addDiscoveryTaskInQueue(false, userId,
                        () -> JsonDataBuilder.prepareP2pHelloPacketAsClient(myNodeId, null, RoutingEntity.Type.BLE, false));
            }
        }

        @Override
        public void onBleUserDisconnect(String nodeId) {
            List<RoutingEntity> disconnectedEntity = RouteManager.getInstance().getBleUsers();
            MeshLog.e("[BLE_PROCESS] BLE node disconnect: " + nodeId);
            MeshLog.i("[BLE_PROCESS] Ble connected user list: " + disconnectedEntity.size());
            if (disconnectedEntity != null && disconnectedEntity.size() > 0) {

                for (RoutingEntity entity : disconnectedEntity) {
                    if (entity.getAddress().equals(nodeId) && entity.isOnline()) {

                        linkStateListener.onBTMessageSocketDisconnect(entity.getAddress());
                        
                        bluetoothServer.starListenThread();

                        // Before calling this updateNodeAsOffline method we have to ensure that
                        // I've any wifi connection with that node or not
                        RoutingEntity existsWifiRoute = RouteManager.getInstance()
                                .getSingleUserInfoByType(entity.getAddress(), RoutingEntity.Type.WiFi);

                        List<RoutingEntity> offlineEntities;
                        if (existsWifiRoute == null || !existsWifiRoute.isOnline()) {
                            offlineEntities = RouteManager.getInstance().updateNodeAsOffline(null, nodeId);

                        } else {
                            // So I've duplicate direct local connection with this node.
                            // we have remove BLE connection only

                            entity.resetMetaData();
                            RouteManager.getInstance().updateEntity(entity);
                            offlineEntities = new ArrayList<>();
                            offlineEntities.add(entity);

                            // remove child node which are BLE mesh

                            List<RoutingEntity> bleMeshUserList = RouteManager.getInstance()
                                    .getUsersByType(RoutingEntity.Type.BLE_MESH);

                            // Generally these bleMeshUserList are belong to that node.
                            // But we will check again
                            if (bleMeshUserList != null && !bleMeshUserList.isEmpty()) {
                                for (RoutingEntity r : bleMeshUserList) {
                                    if (r.getHopAddress().equals(entity.getAddress())) {
                                        r.resetMetaData();
                                        RouteManager.getInstance().updateEntity(r);
                                        offlineEntities.add(r);
                                    }
                                }
                            }

                        }

                        removeOfflineEntitiesFromUI(offlineEntities);

                        List<RoutingEntity> buyersInternetOfflineUserList = new ArrayList<>();
                        for (RoutingEntity entity1 : offlineEntities) {
                            if (entity1.getType() == RoutingEntity.Type.INTERNET) {
                                buyersInternetOfflineUserList.add(entity1);
                            }
                        }


                        offlineEntities.removeAll(buyersInternetOfflineUserList);

                        if (CollectionUtil.hasItem(offlineEntities)) {
                            // pass to the wifi user
                            passOfflinedNodesToWifi(offlineEntities);
                        }
                    }
                }

            } else {
                MeshLog.e("BLE offline fails. No connection exists");
            }
        }

    };

    /**
     * <h1><Filter out my node info which send through discovery message/h1>
     *
     * @param itemList (required) Node info list
     */

    private void filterMyNodeEntity(ConcurrentLinkedQueue<RoutingEntity> itemList) {
        if (CollectionUtil.hasItem(itemList)) {
            for (RoutingEntity item : itemList) {
                if (item.getAddress().equals(myNodeId)) {
                    itemList.remove(item);
                    break;
                }
            }
        }
    }

    @Override
    public void onDisconnectLink(Link link) {
        //BT classic link disconnected
        List<RoutingEntity> entityList = RouteManager.getInstance().getBleUsers();
        if (!entityList.isEmpty()) {
            bluetoothServer.starListenThread();
            if (transportStateListener != null) {
                transportStateListener.onBleUserConnect(true, entityList.get(0).getAddress());
            }
        }
    }

    @Override
    public void onReceivedFilePacket(byte[] data) {
        linkStateListener.onReceivedFilePacket(data);
    }

    @Override
    public void onV2ReceivedHelloFromClient(String senderInfo, String onlineMeshNode, String offlineMeshNodes, String dataId) {
        RoutingEntity senderEntity = GsonUtil.on().getEntityFromJson(senderInfo);
        if (senderEntity == null) return;

        //Add data generation id in list
        AddressUtil.addDataGenerationId(dataId);

        processDataReceivedFromHelloClient(senderEntity, onlineMeshNode, offlineMeshNodes, dataId);
        /*//Fixme protect cycle connection
        if (RouteManager.getInstance().isOnline(senderEntity.getUserId())) {
            MeshLog.e("[BLE_PROCESS] cycle connection detect in ble");
            RoutingEntity routingEntity = RouteManager.getInstance()
                    .getRoutingEntityByAddress(senderEntity.getUserId());
            if (routingEntity != null
                    && (routingEntity.getType() == RoutingEntity.Type.HB
                    || routingEntity.getType() == RoutingEntity.Type.WiFi)) {

                new Thread(new Pinger(routingEntity.getIp(), (ip, isReachable) -> {
                    if (isReachable) {
                        MeshLog.e("[BLE_PROCESS] for cycle we will ignore this connection");
                        transportStateListener.onRemoveRedundantBleConnection(senderEntity.getUserId());
                    } else {
                        processDataReceivedFromHelloClient(senderEntity, onlineMeshNode, offlineMeshNodes);
                    }
                }, 2)).start();
            } else {
                MeshLog.e("[BLE_PROCESS] ble cycle this user already in my connection");
                processDataReceivedFromHelloClient(senderEntity, onlineMeshNode, offlineMeshNodes);
                //transportStateListener.onRemoveRedundantBleConnection(senderNodeInfo.getUserId());
            }

        } else {
            processDataReceivedFromHelloClient(senderEntity, onlineMeshNode, offlineMeshNodes);
        }

*/
    }

    private void processDataReceivedFromHelloClient(RoutingEntity senderNodeInfo, String onlineMeshNode,
                                                    String offlineMeshNodes, String dataId) {
        MeshLog.v("[BLE_PROCESS] received hello from client");
        //Send response hello to client user
        v2SendHelloResponseToClient(senderNodeInfo);
        //Update UI and  routing table
        ConcurrentLinkedQueue<RoutingEntity> onlineNodes = GsonUtil.on().getEntityQueue(onlineMeshNode);
        filterMyNodeEntity(onlineNodes);
        ConcurrentLinkedQueue<RoutingEntity> offlineNodes = GsonUtil.on().getEntityQueue(offlineMeshNodes);
        filterMyNodeEntity(offlineNodes);

        ConcurrentLinkedQueue<RoutingEntity> allOnLines = new ConcurrentLinkedQueue<>();
        allOnLines.add(senderNodeInfo);
        if (CollectionUtil.hasItem(onlineNodes)) {
            allOnLines.addAll(onlineNodes);
        }
        printLog("hello master", senderNodeInfo.getAddress(), onlineNodes);
        //Routing table and ui update
        v2UpdateUiAndRoutingTableForOnlineUsers(allOnLines);

        v2UpdateUiAndRoutingTableForOfflineUsers(offlineNodes);


        //Create new list to send
        ConcurrentLinkedQueue<RoutingEntity> onlineUserList = new ConcurrentLinkedQueue<>();
        onlineUserList.add(senderNodeInfo);
        if (CollectionUtil.hasItem(onlineNodes)) {
            onlineUserList.addAll(onlineNodes);
        }

        ConcurrentLinkedQueue<RoutingEntity> offlineUserList = new ConcurrentLinkedQueue<>();
        if (CollectionUtil.hasItem(offlineNodes)) {
            offlineUserList.addAll(offlineNodes);
        }
        //Send discovered user to wifi users
        v2SendDiscoveredUserToWifiUsers(senderNodeInfo.getAddress(), onlineUserList, offlineUserList, dataId);

        if (transportStateListener != null) {
            transportStateListener.onBleUserConnect(true, senderNodeInfo.getAddress());
        }
    }

    private void v2SendHelloResponseToClient(RoutingEntity senderNodeInfo) {
        byte[] data = JsonDataBuilder.prepareP2pBleHelloPacketAsMaster(myNodeId);
        addDiscoveryTaskInQueue(true, senderNodeInfo.getAddress(),
                () -> data);
    }

    private void v2UpdateUiAndRoutingTableForOnlineUsers(ConcurrentLinkedQueue<RoutingEntity> onlineNodes) {
        if (CollectionUtil.hasItem(onlineNodes)) {
            for (RoutingEntity item : onlineNodes) {
                //TODO check node existence
                MeshLog.v("[BLE_PROCESS] data info " + item.toString());
                boolean updated = RouteManager.getInstance().insertRoute(item);
                MeshLog.i("[BLE_PROCESS] ble update status: " + updated + " for: " + AddressUtil.makeShortAddress(item.getAddress()));
                if (updated) {
                    HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), 200);
                }

            }
        }
    }

    private void v2UpdateUiAndRoutingTableForOnlineMesUsers(ConcurrentLinkedQueue<RoutingEntity> onlineNodes) {
        if (CollectionUtil.hasItem(onlineNodes)) {
            for (RoutingEntity item : onlineNodes) {

                RoutingEntity oldRoutingEntity = RouteManager.getInstance()
                        .getEntityByDestinationAndHop(item.getAddress(), item.getHopAddress());

                if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {
                    if (oldRoutingEntity.getType() != RoutingEntity.Type.INTERNET) {
                        // So that mean this user already exists in locally.
                        // So we don't need to insert again

                        // HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), delay);
                        continue;
                    }

                }


                boolean updated = RouteManager.getInstance().insertRoute(item);
                MeshLog.i("[BLE_PROCESS] ble update status: " + updated + " for: " + AddressUtil.makeShortAddress(item.getAddress()));
                HandlerUtil.postBackground(() -> linkStateListener.onLocalUserConnected(item.getAddress(), item.getPublicKey()), 200);

            }
        }
    }

    private void v2UpdateUiAndRoutingTableForOfflineUsers(ConcurrentLinkedQueue<RoutingEntity> offlineNodes) {
        if (CollectionUtil.hasItem(offlineNodes)) {
            for (RoutingEntity item : offlineNodes) {
                //If node is already online in DB then do nothing
                boolean updated = RouteManager.getInstance().insertRoute(item);

            }
        }
    }

    private void v2SendDiscoveredUserToWifiUsers(String senderId, ConcurrentLinkedQueue<RoutingEntity> onlineNodes,
                                                 ConcurrentLinkedQueue<RoutingEntity> offlineNodes, String dataId) {
        MeshLog.i("[BLE_PROCESS] passTheseUsersToWifiUser available. called:");
        List<RoutingEntity> routingEntities = RouteManager.getInstance().getWifiUser();
        MeshLog.i("[BLE_PROCESS] passTheseUsersToWifiUser available. Size:" + (routingEntities == null ? 0 : routingEntities.size()));
        if (CollectionUtil.hasItem(routingEntities)) {
            byte[] nodeInfoData = JsonDataBuilder.v2BuildMeshNodePacketToSendLcUsers(myNodeId, onlineNodes, offlineNodes, dataId);
            for (RoutingEntity routingEntity : routingEntities) {
                //Except sender
                if (routingEntity.getAddress().equals(senderId)) continue;

                wifiTransPort.addDiscoveryTaskInQueue("Ble user send to wifi client",
                        false, false, routingEntity.getIp(), () -> nodeInfoData);
                MeshLog.i("User passed to wifi:" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
            }
        }
    }

    @Override
    public void onV2ReceivedHelloFromMaster(String senderInfo, String onlineMeshNodes, String offlinNodes, String dataId) {
        RoutingEntity senderEntity = GsonUtil.on().getEntityFromJson(senderInfo);
        if (senderEntity == null) return;
        //Add data generation is in list
        AddressUtil.addDataGenerationId(dataId);

        //Update UI and  routing table
        ConcurrentLinkedQueue<RoutingEntity> onlineNodes = GsonUtil.on().getEntityQueue(onlineMeshNodes);
        filterMyNodeEntity(onlineNodes);
        ConcurrentLinkedQueue<RoutingEntity> offlineNodes = GsonUtil.on().getEntityQueue(offlinNodes);
        filterMyNodeEntity(offlineNodes);
        //Create new list for
        ConcurrentLinkedQueue<RoutingEntity> allOnLines = new ConcurrentLinkedQueue<>();
        allOnLines.add(senderEntity);
        if (CollectionUtil.hasItem(onlineNodes)) {
            allOnLines.addAll(onlineNodes);
        }
        printLog("hello client", senderEntity.getAddress(), onlineNodes);
        //Routing table and ui update
        v2UpdateUiAndRoutingTableForOnlineUsers(allOnLines);
        v2UpdateUiAndRoutingTableForOfflineUsers(offlineNodes);

        //Create new list to send
        ConcurrentLinkedQueue<RoutingEntity> onlineUserList = new ConcurrentLinkedQueue<>();
        onlineUserList.add(senderEntity);
        if (CollectionUtil.hasItem(onlineNodes)) {
            onlineUserList.addAll(onlineNodes);
        }

        ConcurrentLinkedQueue<RoutingEntity> offlineUserList = new ConcurrentLinkedQueue<>();
        if (CollectionUtil.hasItem(offlineNodes)) {
            offlineUserList.addAll(offlineNodes);
        }
        //Send discovered user to wifi users
        v2SendDiscoveredUserToWifiUsers(senderEntity.getAddress(), onlineUserList, offlineUserList, dataId);

        if (transportStateListener != null) {
            transportStateListener.onBleUserConnect(false, senderEntity.getAddress());
        }

        if (BleManager.getInstance() != null) {
            BleManager.getInstance().startPinger();
        }

    }

    @Override
    public void onV2ReceivedMeshUsers(String sender, String onlineNodes, String offlineNodes, String dataId) {

        //Discard duplicate date.
        if (AddressUtil.isDataGenerationIdExist(dataId)) {
            MeshLog.v("[ble-" + " duplicate data id :" + dataId);
            return;
        } else {
            MeshLog.v("[ble-" + " received data id :" + dataId);
            AddressUtil.addDataGenerationId(dataId);
        }

        ConcurrentLinkedQueue<RoutingEntity> onlineUsers = GsonUtil.on().getEntityQueue(onlineNodes);
        filterMyNodeEntity(onlineUsers);
        ConcurrentLinkedQueue<RoutingEntity> offlineUsers = GsonUtil.on().getEntityQueue(offlineNodes);
        filterMyNodeEntity(offlineUsers);

        //TODO check if sender is already exist
        printLog("mesh user", sender, onlineUsers);
        v2UpdateUiAndRoutingTableForOnlineMesUsers(onlineUsers);

        v2UpdateUiAndRoutingTableForOfflineUsers(offlineUsers);

        ConcurrentLinkedQueue<RoutingEntity> allOnline = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<RoutingEntity> allOffline = new ConcurrentLinkedQueue<>();
        if (CollectionUtil.hasItem(onlineUsers)) {
            allOnline.addAll(onlineUsers);
        }

        if (CollectionUtil.hasItem(offlineUsers)) {
            allOffline.addAll(offlineUsers);
        }

        if (CollectionUtil.hasItem(allOnline)) {
            v2SendDiscoveredUserToWifiUsers(sender, allOnline, allOffline, dataId);
        }
        //v2ProcessBleDecisionMessage(allOnline);
    }

    @Override
    public void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid,
                                                  String password, int wifiNodeCount, boolean isFirstMessage) {

        if (receiver.equals(myNodeId)) {

            if (isFileModeEnabled()) {
                MeshLog.v("[BLE_PROCESS] I'm already file sending mode onV2BleMeshDecisionMessageReceive");
                return;
            }

            MeshLog.v("BLE_PROCESS received ble mesh decision message in BLE transport");
            // Need to send the reply

            // First check I am full network or not.
            int myWifiUserCount = RouteManager.getInstance().getWifiUser().size();

            if (myWifiUserCount == WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                MeshLog.v("BLE_PROCESS My network is full not need to any further process");
                return;
            }

            if ((myWifiUserCount < wifiNodeCount)
                    && Text.isNotEmpty(ssid)
                    && Text.isNotEmpty(password)) {

                if (BleManager.getInstance() != null) {

                    RoutingEntity myEntity = new RoutingEntity(myNodeId);
                    List<RoutingEntity> disconnectedNodeList = new ArrayList<>();
                    disconnectedNodeList.add(myEntity);
                    passOfflineNodeToBle(disconnectedNodeList);

                    BleManager.getInstance().onReceiveCredentialMessage("", ssid, password);
                }

                return;
            }


            String mySSID = "";
            String myPassword = "";

            if (myWifiUserCount == 0 && wifiNodeCount == 0) {
                mySSID = "";
                myPassword = "";

                if (BleManager.getInstance() != null) {
                    BleManager.getInstance().onGetMyMode(isFirstMessage, false);
                }

            } else if (myWifiUserCount > wifiNodeCount) {
                mySSID = WifiCredential.ssid;
                myPassword = WifiCredential.password;
            } else if (myWifiUserCount == wifiNodeCount) {
                // Here one user remain GO other will receive credential
                if (isFirstMessage) {
                    mySSID = WifiCredential.ssid;
                    myPassword = WifiCredential.password;
                } else {
                    if (BleManager.getInstance() != null) {

                        RoutingEntity myEntity = new RoutingEntity(myNodeId);
                        List<RoutingEntity> disconnectedNodeList = new ArrayList<>();
                        disconnectedNodeList.add(myEntity);
                        passOfflineNodeToBle(disconnectedNodeList);

                        BleManager.getInstance().onReceiveCredentialMessage("", ssid, password);
                    }
                }
            }

            if (isFirstMessage) {
                sendBleMeshDecisionMessage(myNodeId, sender, mySSID, myPassword, myWifiUserCount, false);
            }


        } else {
            MeshLog.v("[BLE_PROCESS] Send ble mesh decision message in wifi");
            sendBleMeshDecisionMessage(sender, receiver, ssid, password, wifiNodeCount, isFirstMessage);
        }
    }

    @Override
    public void onV2ForceConnectionMessage(String sender, String receiver, String ssid,
                                           String password, boolean isRequest, boolean isAbleToReceive) {

        MeshLog.i("[p2p_process] ble received ssid: " + ssid + " pass: " + password + " sender: " + AddressUtil.makeShortAddress(sender));

        if (receiver.equals(myNodeId)) {
            transportStateListener.onGetForceConnectionRequest(sender, ssid, password, isRequest, true, isAbleToReceive);
        } else {
            // RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiver, RoutingEntity.Type.BLE);
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (routingEntity != null) {
                byte[] data = JsonDataBuilder.prepareForceConnectionMessage(sender, receiver, ssid, password, isRequest, isAbleToReceive);
                sendMessageByType(data, routingEntity);
            } else {
                MeshLog.e("[BLE_PROCESS] the user not found when sending message " + AddressUtil.makeShortAddress(sender));
            }
        }
    }

    @Override
    public void onV2GetFileFreeModeMessage(String sender, String receiver, boolean isAbleToReceive) {
        transportStateListener.onGetFileFreeModeMessage(sender, receiver, isAbleToReceive);
    }

    private boolean isFileModeEnabled() {
        return TransportManagerX.getInstance().isFileSendingMode()
                || TransportManagerX.getInstance().isFileReceivingMode();
    }

    private void sendBleMeshDecisionMessage(String sender, String receiver, String ssid,
                                            String password, int wifiNodeCount, boolean isFirstMessage) {
        byte[] data = JsonDataBuilder.prepareBleMeshDecisionMessage(sender, receiver, ssid, password, wifiNodeCount, isFirstMessage);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.BLE);
        if (routingEntity != null) {
            MeshLog.v("[BLE_PROCESS] sending special ble decision message to WIFI:" + routingEntity.toString());
            sendMessageByType(data, routingEntity);
        } else {
            MeshLog.e("[BLE] send Message BLE mesh decision failed Routing entity null");
        }
    }

    private void sendMessageByType(byte[] data, RoutingEntity routingEntity) {
        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
            wifiTransPort.sendMeshMessage(routingEntity.getIp(), data);
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            adHocTransport.sendAdhocMessage(routingEntity.getIp(), data);
        } else {
            sendMessage(routingEntity.getAddress(), data);
        }
    }

    private void removeOfflineEntitiesFromUI(List<RoutingEntity> offlineEntities) {
        if (CollectionUtil.hasItem(offlineEntities)) {
            for (RoutingEntity routingEntity : offlineEntities) {
                //UI callback
                MeshLog.w("Disconnected BLE:: ->>" + AddressUtil.makeShortAddress(
                        routingEntity.getAddress()));
                linkStateListener.onUserDisconnected(routingEntity.getAddress());
                linkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());


                RoutingEntity entity = RouteManager.getInstance().getEntityByAddress(routingEntity.getAddress());
                if (entity != null && entity.isOnline()) continue;

                if (remoteTransport != null) {
                    remoteTransport.onBuyerDisconnected(routingEntity.getAddress());
                }

            }
        }
    }

    /*private void v2ProcessBleDecisionMessage(ConcurrentLinkedQueue<NodeInfo> allOnline) {
        if (CollectionUtil.hasItem(allOnline)) {
            for (NodeInfo nodeInfo : allOnline) {
                if (nodeInfo.isBleNode) {

                    if (isFileModeEnabled()) {
                        MeshLog.v("[BLE_PROCESS] I'm already file sending mode v2ProcessBleDecisionMessage");
                        return;
                    }

                    int wifiUserCount = RouteManager.getInstance().getWifiUser().size();
                    if (wifiUserCount == WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                        // We will not send any message to other
                        break;
                    }

                    String ssid = "";
                    String password = "";

                    if (wifiUserCount > 0) {
                        ssid = WifiCredential.ssid;
                        password = WifiCredential.password;
                    }

                    sendBleMeshDecisionMessage(myNodeId, nodeInfo.getUserId(), ssid, password,
                            wifiUserCount, true);

                    break;
                }
            }
        }
    }*/


    @Override
    public void onMeshLinkFound(String senderId, String hopId, String jsonString) {
    }

    @Override
    public void onMeshLinkDisconnect(String nodeIds, String forwarderId) {
        List<RoutingEntity> offlineRoutingEntities = new ArrayList<>();

        List<DisconnectionModel> disconnectedNodeList = GsonUtil.on().getDisconnectedNodeList(nodeIds);
        MeshLog.v("[Ble_process] onMeshLinkDisconnect in BLE transport: " + disconnectedNodeList.toString());
        for (DisconnectionModel model : disconnectedNodeList) {

            if (model.getUserType() == RoutingEntity.Type.BLE_MESH) {

                MeshLog.w("Disconnected BLE Mesh ->>" + AddressUtil.makeShortAddress(model.getNodeId()));

                List<RoutingEntity> updateNodeAsOffline = RouteManager.getInstance()
                        .updateNodeAsOffline(forwarderId, model.getNodeId());

                if (CollectionUtil.hasItem(updateNodeAsOffline)) {
                    offlineRoutingEntities.addAll(updateNodeAsOffline);
                }
                if (!offlineRoutingEntities.isEmpty()) {
                    MeshLog.i("[BT] Offline list ::" + "" + offlineRoutingEntities.size());
                }
            }
        }


        List<RoutingEntity> buyersInternetOfflineUserList = new ArrayList<>();
        for (RoutingEntity entity1 : offlineRoutingEntities) {
            if (entity1.getType() == RoutingEntity.Type.INTERNET) {
                buyersInternetOfflineUserList.add(entity1);
            }
        }

        MeshLog.v("[p2p_process] offline calculation in BLE transport. before filter: " + offlineRoutingEntities.toString());
        offlineRoutingEntities.removeAll(buyersInternetOfflineUserList);
        MeshLog.v("[p2p_process] offline calculation in BLE transport. after filter: " + offlineRoutingEntities.toString());

        for (RoutingEntity routingEntity : offlineRoutingEntities) {
            linkStateListener.onUserDisconnected(routingEntity.getAddress());
            linkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());

            //connectionLinkCache.removeNodeInfo(routingEntity.getAddress());

            RoutingEntity entity = RouteManager.getInstance().getEntityByAddress(routingEntity.getAddress());
            if (entity != null && entity.isOnline()) continue;

            if (remoteTransport != null) {
                remoteTransport.onBuyerDisconnected(routingEntity.getAddress());
            }
        }


        passOfflinedNodesToWifi(offlineRoutingEntities);

        //passOfflineEntitiesToAdhoc(offlineRoutingEntities);
    }

    @Override
    public void onMessageReceived(String sender, String receiver, String messageId, byte[] data, String senderIp, String immediateSender) {
        if (receiver.equals(myNodeId)) {
            try {
                JSONObject js = new JSONObject(new String(data));
                byte[] msg_data = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_DATA).getBytes();

                if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND) {
                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
                    remoteTransport.processInternetOutgoingMessage(sender, originalReceiver, messageId, msg_data, false, 0);
                } else if (js.getInt(PurchaseConstants.JSON_KEYS.MESSAGE_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE) {
                    String sellerId = js.getString(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS);
                    String ackBody = Util.buildInternetSendingAckBody(sender);
                    linkStateListener.onMessageReceived(sender, msg_data);

                    sendMessageAck(receiver, sellerId, messageId, Constant.MessageStatus.RECEIVED, ackBody, immediateSender);
                } else {

                    linkStateListener.onMessageReceived(sender, msg_data);
                    String ackBody = Util.buildLocalAckBody();
                    sendMessageAck(receiver, sender, messageId, Constant.MessageStatus.RECEIVED, ackBody, immediateSender);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            PendingMessage oldPendingMessage = connectionLinkCache.getPendingMessage(messageId);
            if (oldPendingMessage != null) {
                MeshLog.v("BLE duplicate message received :" + AddressUtil.makeShortAddress(sender));
                oldPendingMessage.previousAttemptEntity = null; //To protect direct connection offline, just retry through other path
                onMiddleManMessageSendStatusReceived(messageId, false);
                return;
            }

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(immediateSender, receiver, RoutingEntity.Type.BLE);
            byte[] message = JsonDataBuilder.buildMessage(sender, receiver, messageId, data);

            HandlerUtil.postForeground(() -> Toast.makeText(mContext, "Ble hop S :" + AddressUtil.makeShortAddress(sender)
                    + " R: " + AddressUtil.makeShortAddress(receiver), Toast.LENGTH_SHORT).show());

            PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, immediateSender, message, routingEntity);
            List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
            pendingMessage.routeQueue.addAll(allPossiblePath);
            connectionLinkCache.addPendingMessage(messageId, pendingMessage);

            if (routingEntity == null) {
                RoutingEntity wrongEntity = RouteManager.getInstance().getEntityByAddress(receiver);
                if (wrongEntity != null) {
                    MeshLog.e("Wrong entity offline " + AddressUtil.makeShortAddress(wrongEntity.getAddress()));
                    List<RoutingEntity> offlineEntities = RouteManager.getInstance().makeUserOffline(wrongEntity);
                    //Todo if it is ble then we have to rescan ble

                    if (CollectionUtil.hasItem(offlineEntities)) {
                        for (RoutingEntity entity : offlineEntities) {
                            // Todo may be we have to manage buyer disconnection section here
                            linkStateListener.onUserDisconnected(entity.getAddress());
                        }
                    }
                }
            }

            if (routingEntity != null && routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {

                wifiTransPort.sendAppMessage(messageId, routingEntity.getIp(), message);
            } else {
                MeshLog.e("Send failed entity not match");
                onMiddleManMessageSendStatusReceived(messageId, false);
            }
        }
    }


    @Override
    public void onReceiveBuyerFileMessage(String sender, String receiver, String messageData, int fileMessageType, String immediateSender, String messageId) {
//        MeshLog.v("FILE_SPEED_TEST_6.5 " + Calendar.getInstance().getTime());
        try {
            if (receiver.equals(myNodeId)) {
                MeshLog.v("Buyer_file received in BLE transport ");

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
                    byte[] data = JsonDataBuilder.buildBuyerFileMessage(sender, receiver, messageData.getBytes(), fileMessageType, messageId);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                        wifiTransPort.sendAppMessage(routingEntity.getIp(), data);
                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        addAppMessageInQueue(null, routingEntity.getAddress(), data);
                    }
                } else {
                    MeshLog.v("BLE buyer file message route null");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
                   /* MeshLog.v("(P) sendMessage ble user");
                    BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, msgBody));
                    } else {
                        MeshLog.v("(P) BLE LINK NOT FOUND");
                    }*/
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
               /* Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, msgBody));
                } else {
                    MeshLog.v("(P) BLE LINK NOT FOUND");
                }*/
            }
        } else {
            MeshLog.v(" (P) sendMessage User does not exist in routing table");
        }
    }

    @Override
    public void onReceivedMsgAck(String sender, String receiver, String messageId, int status, String ackBody, String immediateSender) {

        connectionLinkCache.removePendingMessage(messageId);

        if (receiver.equals(myNodeId)) {
            try {
                JSONObject js = new JSONObject(ackBody);

                if (js.getInt(PurchaseConstants.JSON_KEYS.ACK_MODE) == PurchaseConstants.MESSAGE_MODE.INTERNET_SEND_ACK) {

                    String originalReceiver = js.getString(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER);
                    // internetTransport.processInternetOutgoingMessageACK(sender, originalReceiver, messageId, status);
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

    private void sendMessageAck(String sender, String receiver, String messageId, int status, String ackBody, String immediateSender) {
        byte[] ackMessage = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress("", receiver, RoutingEntity.Type.BLE);
        if (routingEntity != null) {

            PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, myNodeId, ackMessage, routingEntity);
            List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
            pendingMessage.routeQueue.addAll(allPossiblePath);
            connectionLinkCache.addPendingMessage(messageId, pendingMessage);

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                wifiTransPort.sendAppMessage(messageId, routingEntity.getIp(), ackMessage);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                adHocTransport.sendAdhocMessage(routingEntity.getIp(), ackMessage);
            } else {
                addAppMessageInQueue(messageId, routingEntity.getAddress(), ackMessage);
            }
        } else {
            MeshLog.e("[BLE] send Message Ack failed Routing entity null");
        }
    }


    private void sendMessageAckToOtherMesh(String sender, String receiver, String messageId,
                                           int status, String ackBody, String immediateSender) {
        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.BLE);
        byte[] data = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);

        /*PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver, immediateSender, data, routingEntity);
        List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
        pendingMessage.routeQueue.addAll(allPossiblePath);

        connectionLinkCache.addPendingMessage(messageId, pendingMessage);*/

        if (routingEntity != null) {
            MeshLog.i(" Send message ack to other mesh  =>" + routingEntity.toString());

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                wifiTransPort.sendAppMessage(messageId, routingEntity.getIp(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                adHocTransport.sendAdhocMessage(routingEntity.getIp(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                addAppMessageInQueue(null, routingEntity.getAddress(), data);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                /*Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, data));
                } else {
                    MeshLog.e(" BT LINK NOT FOUND TO MSG ACK OTR MESH");
                }*/
            } else {
                MeshLog.v("Internet processMultihopInternetMessage ");
                remoteTransport.sendBuyerReceivedAck(sender, receiver, messageId, status, ackBody);
            }
        } else {
            MeshLog.e("RoutingEntity Null in sendMessageAckToOtherMesh in BTTransPort");
            //onMiddleManMessageSendStatusReceived(messageId, false);
        }
    }

    @Override
    public void onReceiveDisconnectRequest(String senderAddress) {
        MeshLog.e("[BLE-PROCESS] disconnect request received from: " + AddressUtil.makeShortAddress(senderAddress));
        //mBleMessageDriver.disconnectUserInternally(senderAddress);
        List<RoutingEntity> offlineEntity = BleManager.getInstance().disconnectUserInternally(senderAddress);
        passOfflinedNodesToWifi(offlineEntity);
    }


    @Override
    public void onV2ReceivedFailedMessageAck(String source, String destination, String hop, String messageId) {
        PendingMessage pendingMessage = connectionLinkCache.getPendingMessage(messageId);
        if (pendingMessage == null) return;

        RoutingEntity routingEntity = RouteManager.getInstance().getEntityByDestinationAndHop(destination, hop);

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
                if (previousEntity == null) return;

                if (previousEntity.getType() == RoutingEntity.Type.BLE) {
                    addAppMessageInQueue(pendingMessage.messageId, previousEntity.getAddress(), failedAck);
                } else if (previousEntity.getType() == RoutingEntity.Type.WiFi) {
                    wifiTransPort.sendAppMessage(pendingMessage.messageId, previousEntity.getIp(), failedAck);
                } else {

                }
            }
            connectionLinkCache.removePendingMessage(messageId);
        } else {
            RoutingEntity nextShortest = pendingMessage.routeQueue.poll();

            if (!TextUtils.isEmpty(nextShortest.getHopAddress())) {
                nextShortest = RouteManager.getInstance().getShortestPath(nextShortest.getHopAddress());
            }
            if (nextShortest.getType() == RoutingEntity.Type.BLE) {
                addAppMessageInQueue(pendingMessage.messageId, nextShortest.getAddress(), pendingMessage.messageData);

            } else if (nextShortest.getType() == RoutingEntity.Type.WiFi) {
                wifiTransPort.sendAppMessage(pendingMessage.messageId, nextShortest.getIp(), pendingMessage.messageData);

            } else {

            }
        }
    }

    @Override
    public void onReceiverInternetUserLocally(String sender, String receiver, String
            sellerId, String userList) {
        MeshLog.v("BT onReceiverInternetUserLocally =" + receiver);
        if (receiver.equalsIgnoreCase(myNodeId)) {
//            linkStateListener.onCurrentSellerId(sellerId);
            /*if (remoteTransport.amIDirectUser()) {
                MeshLog.v("direct user");
                return;
            }*/

            ConcurrentLinkedQueue<RoutingEntity> userNodeInfoList = GsonUtil.on().getEntityQueue(userList);

            for (RoutingEntity item : userNodeInfoList) {
                if (TextUtils.isEmpty(item.getAddress()) || item.getAddress().equals(myNodeId)) {
                    continue;
                }

               /* if (RouteManager.getInstance().isOnline(item.getAddress())) {
                    linkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());
                    continue;
                }*/

                RoutingEntity oldRoutingEntity = RouteManager.getInstance()
                        .getEntityByDestinationAndHop(item.getAddress(), item.getHopAddress());

                if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {


                    // we will update old routing entity if it is only online.
                    // We will add latest data

                    // Other wise we will insert new column.

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
                    MeshLog.v("BT transport interent user ids send success=" + item);
                    linkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());

                }
            }

        } else {


            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.BLE);
            if (routingEntity != null) {

                // Increased hop count
                ConcurrentLinkedQueue<RoutingEntity> userNodeInfoList = GsonUtil.on().getEntityQueue(userList);
                for (RoutingEntity r : userNodeInfoList) {
                    r.setHopCount(r.getHopCount() + 1);
                }

                userList = GsonUtil.on().toJsonFromEntityList(userNodeInfoList);

                byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, receiver, sellerId, userList);
                MeshLog.v("(-) RoutingEntity" + routingEntity.toString());
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.v("senUserList Wifi user");
                    // BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), userListMessage);

                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    //BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), userListMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    /*MeshLog.v("senUserList ble user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> userListMessage));
                    } else {
                        MeshLog.v("senUserList BLE LINK NOT FOUND");
                    }*/
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    MeshLog.v("send internet UserList ble user");
                    sendMessage(routingEntity.getAddress(), userListMessage);
                }
            } else {
                MeshLog.v("senUserList User does not exist in routing table");
            }
        }
    }

    @Override
    public void onInternetUserLeave(String sender, String receiver, String userList) {
        if (receiver.equals(myNodeId)) {

           /* if (remoteTransport.amIDirectUser())
                return;*/

            MeshLog.v("[Internet] onInternetUserLeave" + userList);

            String[] userIdArray = TextUtils.split(userList, "@");

            for (String id : userIdArray) {
                RouteManager.getInstance().makeInternetUserOffline(sender, Arrays.asList(userIdArray));

                linkStateListener.onUserDisconnected(id);
                //RouteManager.getInstance().updateNodeAsOffline(id);
            }


        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.BLE);

            if (routingEntity != null) {
                byte[] userListMessage = JsonDataBuilder.prepareInternetLeaveMessage(sender, receiver, userList);
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.mm("[Internet] senUserList Wifi user");
                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), userListMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), userListMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {

                  /*  Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        MeshLog.mm("[Internet] senUserList ble user");
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> userListMessage));
                        //sendEventQueue.execute(() -> bleLink.sendMeshMessage(userListMessage));
                    } else {
                        MeshLog.mm("[Internet] senUserList BLE LINK NOT FOUND");
                    }*/
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    sendMessage(routingEntity.getAddress(), userListMessage);
                }
            }
        }
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
        String receiver = handshakeInfo.getReceiverId();

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
            MeshLog.e("Router entity not found");
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

            wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);

            MeshLog.v("(-) Send message to => " + routingEntity.toString());
        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
            adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
        } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
            BleLink bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
            if (bleLink != null) {
                //   messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage(bleLink, message));
            } else {
                MeshLog.v("(*) BT LINK NOT FOUND MSG TO OTR MESH");
            }
        } else {
            MeshLog.v("BT User not found to send the local message");
        }
    }

    @Override
    public void onReceiveNewRole(String sender, String receiver, int role) {
        if (myNodeId.equals(receiver)) {
            int previousRole = connectionLinkCache.setNewUserRole(sender, role);
            linkStateListener.onUserModeSwitch(sender, role, previousRole);
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(sender, receiver, RoutingEntity.Type.BLE);
            if (routingEntity == null) return;

            byte[] message = JsonDataBuilder.buildUserRoleSwitchMessage(sender, receiver, role);

            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                MeshLog.v("[ble_process] forwarding role switch message via WIFI");
                wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (bleLink != null) {
                    //messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                }
            }
        }
    }

    @Override
    public void onFileMessageReceived(String sender, String receiver, String messageId, String message, String immediateSender) {
        MeshLog.v("BLE file packet received receiver : " + AddressUtil.makeShortAddress(receiver));
        if (receiver.equals(myNodeId)) {
            linkStateListener.onFileMessageReceived(sender, message);
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(immediateSender, receiver, RoutingEntity.Type.BLE);

            if (routingEntity != null) {
                MeshLog.v("Multihop received in Ble transport need to pass other user :" + AddressUtil.makeShortAddress(routingEntity.getAddress()));

                byte[] data = JsonDataBuilder.buildFiledMessage(sender, receiver, messageId, message.getBytes());
                List<RoutingEntity> bleEntityList = RouteManager.getInstance().getBleUsers();
                if (!bleEntityList.isEmpty()) {
                    PendingMessage pendingMessage = new PendingMessage(messageId, sender, receiver,
                            bleEntityList.get(0).getAddress(), data, routingEntity);
                    List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, receiver);
                    pendingMessage.routeQueue.addAll(allPossiblePath);
                    connectionLinkCache.addPendingMessage(messageId, pendingMessage);
                }

                if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                    wifiTransPort.sendAppMessage(messageId, routingEntity.getIp(), data);
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

    /**
     * <h1>Add discovery task in queue</h1>
     *
     * @param addFirst   : boolean add task in first or last
     * @param receiverId
     * @param dataPuller
     */
    public synchronized void addDiscoveryTaskInQueue(boolean addFirst, String receiverId, DiscoveryTask.DataPuller dataPuller) {

        if (addFirst) {
            discoveryEventQueue.addDiscoveryTaskInFirst(new DiscoveryTask(receiverId, dataPuller) {
                @Override
                public void run() {
                    byte[] data = this.puller.getData();
                    boolean isSuccess = sendMessageViaBt(data);
                    if (!isSuccess) {
                        String messageId = mBleMessageDriver.sendMessage(this.receiverId, data);
                        int delay = data.length / BleMessageHelper.CHUNK_SIZE;
                        if (delay == 0) {
                            delay = 1;
                        }
                        try {
                            Thread.sleep(delay * 1500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            discoveryEventQueue.addDiscoveryTaskInLast(new DiscoveryTask(receiverId, dataPuller) {
                @Override
                public void run() {
                    byte[] data = this.puller.getData();
                    boolean isSuccess = sendMessageViaBt(data);
                    if (!isSuccess) {
                        String messageId = mBleMessageDriver.sendMessage(this.receiverId, data);
                        int delay = data.length / BleMessageHelper.CHUNK_SIZE;

                        if (delay == 0) {
                            delay = 1;
                        }
                        try {
                            Thread.sleep(delay * 1500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    public void addFileMessageInQueue(String receiver, String messageId, byte[] data) {
        discoveryEventQueue.addAppMessageInQueue(new DiscoveryTask(messageId, receiver, data) {
            @Override
            public void run() {
                boolean isSuccess = sendMessageViaBt(this.messageData);
                if (!isSuccess) {
                    mBleMessageDriver.sendFileMessage(this.ipOrAddress, this.messagePublicId, this.messageData);
                } else {
                    onMiddleManMessageSendStatusReceived(this.messagePublicId, isSuccess);
                }
            }
        });
    }


    public void sendAppMessage(final String receiverId, final byte[] messageData) {
        addAppMessageInQueue(null, receiverId, messageData);
    }

    public void addAppMessageInQueue(final String msgId, final String receiverId, final byte[] messageData) {

        if (TextUtils.isEmpty(msgId)) {
            discoveryEventQueue.addAppMessageInQueue(new DiscoveryTask(receiverId, () -> messageData) {
                @Override
                public void run() {
                    byte[] data = this.puller.getData();

                    boolean isSuccess = sendMessageViaBt(data);
                    if (!isSuccess) {
                        mBleMessageDriver.sendMessage(this.receiverId, data);
                        int delay = data.length / BleMessageHelper.CHUNK_SIZE;

                        if (delay == 0) {
                            delay = 1;
                        }
                        try {
                            Thread.sleep(delay * 1500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            discoveryEventQueue.addAppMessageInQueue(new DiscoveryTask(msgId, receiverId, messageData) {
                @Override
                public void run() {
                    boolean isSuccess = sendMessageViaBt(this.messageData);
                    if (!isSuccess) {
                        mBleMessageDriver.sendMessage(this.ipOrAddress, this.messagePublicId, this.messageData);
                        int delay = this.messageData.length / BleMessageHelper.CHUNK_SIZE;

                        if (delay == 0) {
                            delay = 1;
                        }
                        try {
                            Thread.sleep(delay * 1500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onMiddleManMessageSendStatusReceived(String messageId, boolean isSuccess) {
        if (TextUtils.isEmpty(messageId)) return;
        MeshLog.v("[Ble_process] middle man ble packet send Id : " + messageId + " success : " + isSuccess);
        String[] messageIdToken = messageId.split("_");
        boolean isFileMessage = false;
        if (messageIdToken.length > 1) {
            if (TextUtils.isDigitsOnly(messageIdToken[1])) {
                isFileMessage = true;
            }
        }

        if (isSuccess) {
            if (isFileMessage) {
                connectionLinkCache.removePendingMessage(messageId);
            }
        } else {

            MeshLog.v("[Middleman] ble attempt to send other path");
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
                    MeshLog.v("[Middleman] ble queue is empty " + pendingMessage.previousSender);
                    if (!myNodeId.equals(pendingMessage.previousSender)) {
                        byte[] failedAck = JsonDataBuilder.buildFailedMessageAck(myNodeId, pendingMessage.actualSender,
                                pendingMessage.actualReceiver, pendingMessage.messageId);

                        MeshLog.i("[Middleman] ble onMiddleManMessageSendStatusReceived previous sender: " + pendingMessage.previousSender);
                        RoutingEntity entity = RouteManager.getInstance().getShortestPath(pendingMessage.previousSender);
                        if (entity != null) {
                            if (entity.getType() == RoutingEntity.Type.BLE) {
                                addAppMessageInQueue(pendingMessage.messageId, entity.getAddress(), failedAck);
                            } else {
                                wifiTransPort.sendAppMessage(pendingMessage.messageId, entity.getIp(), failedAck);
                            }
                        }

                    }
                    connectionLinkCache.removePendingMessage(messageId);

                } else {
                    RoutingEntity routingEntity = pendingMessage.routeQueue.poll();

                    MeshLog.v("[Middleman] ble User has alternative paths " + routingEntity.getAddress());
                    if (!TextUtils.isEmpty(routingEntity.getHopAddress())) {
                        routingEntity = RouteManager.getInstance().getShortestPath(routingEntity.getHopAddress());
                    }

                    if (routingEntity == null) {
                        MeshLog.v("[Middleman] ble Next shortest routing path null");
                        return;
                    }

                    pendingMessage.previousAttemptEntity = routingEntity;

                    if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        if (isFileMessage) {
                            addFileMessageInQueue(routingEntity.getAddress(), pendingMessage.messageId, pendingMessage.messageData);
                        } else {
                            addAppMessageInQueue(pendingMessage.messageId, routingEntity.getAddress(), pendingMessage.messageData);
                        }
                    } else {
                        wifiTransPort.sendAppMessage(pendingMessage.messageId, routingEntity.getIp(), pendingMessage.messageData);
                    }
                }
            } else {
                MeshLog.v("[Middleman] ble Pending message not found");
            }

        }
    }

    private void passOfflineNodeToBle(List<RoutingEntity> offlineRoutingEntities) {
        if (CollectionUtil.hasItem(offlineRoutingEntities)) {
            String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);
            byte[] data = JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList, myNodeId);
            List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();
            for (RoutingEntity entity : bleUserList) {
                sendMessage(entity.getAddress(), data);
            }
        }
    }

    private void passOfflinedNodesToWifi(List<RoutingEntity> offlineRoutingEntities) {

        // Don't remove below comment ou code. May be we need later

        /*List<RoutingEntity> validOfflineList = new ArrayList<>();
        for (RoutingEntity entity : offlineRoutingEntities) {
            RoutingEntity validEntity = RouteManager.getInstance().getEntityByAddress(entity.getAddress());
            if (validEntity == null || !validEntity.isOnline()) {
                validOfflineList.add(entity);
            }
        }*/

        // First remove buyers internet user if available
        List<RoutingEntity> buyersInternetOfflineUserList = new ArrayList<>();
        for (RoutingEntity entity1 : offlineRoutingEntities) {
            if (entity1.getType() == RoutingEntity.Type.INTERNET) {
                buyersInternetOfflineUserList.add(entity1);
            }
        }

        offlineRoutingEntities.removeAll(buyersInternetOfflineUserList);

        if (CollectionUtil.hasItem(offlineRoutingEntities)) {
            String leaveNodeList = buildNodeIdListJson(offlineRoutingEntities);
            if (!RouteManager.getInstance().isWifiUserConnected()) return;

            List<RoutingEntity> liveWifiConnectionList = RouteManager.getInstance().getWifiUser();
            MeshLog.i(" LiveWifiConnectionList for transferring disonnected node  -->" + liveWifiConnectionList.toString());
            if (CollectionUtil.hasItem(liveWifiConnectionList)) {
                byte[] data = JsonDataBuilder.buildNodeLeaveEvent(leaveNodeList, myNodeId);
                for (RoutingEntity rEntity : liveWifiConnectionList) {
                    wifiTransPort.addDiscoveryTaskInQueue("Node Leave", false, false, rEntity.getIp(), () -> data);
                }
            }
        }
    }

    private String buildNodeIdListJson(List<RoutingEntity> routingEntities) {
        if (CollectionUtil.hasItem(routingEntities)) {

            List<DisconnectionModel> disconnectionModelList = new ArrayList<>();
            for (RoutingEntity entity : routingEntities) {
                DisconnectionModel model = new DisconnectionModel();
                model.setNodeId(entity.getAddress());

                if (entity.getType() == RoutingEntity.Type.WiFi) {
                    model.setUserType(RoutingEntity.Type.WiFi);
                } else {
                    model.setUserType(RoutingEntity.Type.WifiMesh);
                }
                disconnectionModelList.add(model);
            }


            return GsonUtil.on().toJsonFromDisconnectionList(disconnectionModelList);
        }
        return null;
    }

    private void printLog(String logType, String sender, ConcurrentLinkedQueue<RoutingEntity> entityList) {
        String nodes = "";
        if (CollectionUtil.hasItem(entityList)) {
            for (RoutingEntity item : entityList) {
                nodes = nodes + "," + AddressUtil.makeShortAddress(item.getAddress());
            }
        }
        MeshLog.v("[ble-" + logType + "] Sender: " + AddressUtil.makeShortAddress(sender) + " Nodes: [" + nodes + "]");
    }


    public boolean sendMessageViaBt(byte[] message) {

        BleLink bleLink = BleLink.getBleLink();
        if (bleLink != null) {
            bleLink.sendMeshMessage(message);
        } else {
            return false;
        }
        return true;
    }

}
