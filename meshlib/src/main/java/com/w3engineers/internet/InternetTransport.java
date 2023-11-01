/*
package com.w3engineers.internet;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.internet.webrtc.PeerConnectionHolder;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.Adhoc.protocol.WifiDetector;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.datasharing.util.Util;
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
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InternetTransport implements ConnectionStateListener, MeshTransport,
        InternetLink.InternetMessageSubmitListener, WifiDetector.Listener {
    private final Context mContext;
    private final String myNodeId;
    // private final byte[] mNodeInfo;
    private LinkStateListener mLinkStateListener;
    boolean isConnected = false;
    private ConnectionLinkCache connectionLinkCache;

    private WifiTransPort wifiTransPort;
    private BluetoothTransport bluetoothTransport;
    private AdHocTransport adHocTransport;

    private MessageDispatcher messageDispatcher;
    //private String APP_NAME;
    private String TAG = "WebRtcTest";
//    private InternetLink internetLink;

    private WifiDetector wifiDetector;
    private DispatchQueue dispatchQueue;
    private boolean usingAdhocInternet;
    private InternetLink internetLink;
    private ForwardListener forwardListener;

    public InternetTransport(Context context, String nodeId, ConnectionLinkCache connectionLinkCache,
                             LinkStateListener listener, MessageDispatcher messageDispatcher) {
        mContext = context;
        myNodeId = nodeId;
        // mNodeInfo = nodeInfo;
        mLinkStateListener = listener;
        this.connectionLinkCache = connectionLinkCache;
        this.messageDispatcher = messageDispatcher;
        this.dispatchQueue = new DispatchQueue();

        wifiDetector = new WifiDetector(this, dispatchQueue, context);

        MeshLog.v("init internet transport :: done");
    }


    public <T> void setInterTransport(T... transPorts) {
        for (T item : transPorts) {
            if (item instanceof WifiTransPort) {
                this.wifiTransPort = (WifiTransPort) item;
            } else if (item instanceof BluetoothTransport) {
                this.bluetoothTransport = (BluetoothTransport) item;
            } else if (item instanceof AdHocTransport) {
                this.adHocTransport = (AdHocTransport) item;
            } else if (item instanceof ForwardListener) {
                this.forwardListener = (ForwardListener) item;
            }
        }
    }

    @Override
    public void onAdhocEnabled(InetAddress address) {
        Log.e("Internettransport", "Adhoc connected detect in internet transport");
        if (!P2PUtil.isConnectedWithPotentialGO(mContext)) {
            HandlerUtil.postBackground(new Runnable() {
                @Override
                public void run() {
                    WiFiUtil.isInternetAvailable((message, isConnected) -> {

                        MeshLog.v("Adhoc connected 2 =" + message + " " + isConnected);

                        if (isConnected) {
                            //dispatchQueue.dispatch(()->startInternetConnectionForMobileData(true));
                            if (internetLink == null) return;

                            if (usingAdhocInternet) return;

                            usingAdhocInternet = true;
                            dispatchQueue.dispatch(() -> startInternetConnectionForMobileData(true));

                        }
                    });
                }
            }, 2000);
        }
    }

    @Override
    public void onAdhocDisabled() {
        MeshLog.v("Adhoc disconnected detect in internet transport");
        if (usingAdhocInternet) {

            //startInternetConnectionForMobileData(false);
            // dispatchQueue.dispatch(()->startInternetConnectionForMobileData(false));
            if (internetLink == null) return;

            usingAdhocInternet = false;
            dispatchQueue.dispatch(() -> startInternetConnectionForMobileData(false));
        }
    }

    @Override
    public void start() {
        makeSocketConnection();
        //sampleConnection();
        HandlerUtil.postBackground(() -> wifiDetector.start(), 4000);
    }

    @Override
    public void stop() {
        wifiDetector.stop();
        destroyInternetConnection();
    }

    private void destroyInternetConnection() {
        PeerConnectionHolder.removeAll();
        if (InternetLink.getInstance() != null) {
            InternetLink.getInstance().closeSocket();
            InternetLink.getInstance().destroyObject();
        }
    }

    private void makeSocketConnection() {
        if (WiFiUtil.isWifiConnected(mContext)) {
            if (!P2PUtil.isConnectedWithPotentialGO(mContext)) {
                WiFiUtil.isInternetAvailable((message, isConnected) -> {
                    MeshLog.v("Adhoc connected 1 =" + message + " " + isConnected);
                    if (isConnected) {

                        startInternetConnectionForMobileData(isConnected);
                    } else {
                        startInternetConnectionForMobileData(false);
                    }
                });
            } else {
                startInternetConnectionForMobileData(false);
            }
        } else {
            startInternetConnectionForMobileData(false);
        }

    }

    private void startInternetConnectionForMobileData(boolean isWifiNetwork) {


        usingAdhocInternet = isWifiNetwork;

//        int userMode = PreferencesHelper.on().getDataShareMode();
//        MeshLog.v("userMode it " + userMode);

//        if ((userMode != PreferencesHelper.DATA_BUYER && userMode != PreferencesHelper.MESH_USER) || isAdhocNetwork) {

//            Log.e(TAG, "First socket attempt start");
//            IS_INIT_NETWORK_SUCCESS = true;
        //destroyInternetConnection();
        if (internetLink != null) {
            internetLink.closeSocket();
        }
        internetLink = new InternetLink(mContext, myNodeId, InternetTransport.this, messageDispatcher, connectionLinkCache, this, isWifiNetwork);

//        } else {
//            MeshLog.i("Internet socket else option");
//        }
    }


    @Override
    public void onDisconnectLink(Link link) {
    }

    @Override
    public void onMeshLinkFound(String sender, String hopNodeId, String jsonString) {

    }

    @Override
    public void onMeshLinkDisconnect(String nodeId, String hopId) {

        RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(nodeId);
        if (!TextUtils.isEmpty(hopId)) {
            // It is buyer
            // We have to check that its already hop ID match

            if (route != null && route.getHopAddress() != null && !route.getHopAddress().equalsIgnoreCase(hopId)) {
                return;
            }
        }

        //If not internet user no need to remove
        if (route != null && route.getType() != RoutingEntity.Type.INTERNET) {
            return;
        }

        mLinkStateListener.onUserDisconnected(nodeId);

        // if hop remove all its buyer
        // if buyer remove only buyer

        connectionLinkCache.removeNodeInfo(nodeId);

        List<RoutingEntity> childAddressList = RouteManager.getInstance().getAllDisconnectedInternetUser(nodeId);
        List<String> idList = new ArrayList<>();
        idList.add(nodeId);

        if (childAddressList != null && !childAddressList.isEmpty()) {
            Log.d(TAG, "Child list size: " + childAddressList.size());
            for (RoutingEntity entity : childAddressList) {
                if (!idList.contains(entity.getAddress())) {
                    idList.add(entity.getAddress());
                    mLinkStateListener.onUserDisconnected(entity.getAddress());

                    connectionLinkCache.removeNodeInfo(entity.getAddress());
                }

            }
        }

        RouteManager.getInstance().makeUserOfline(idList);

        String removeNodeIdList = TextUtils.join("@", idList);
        for (String buyerNodeId : connectionLinkCache.getInternetBuyerList()) {
            if (!buyerNodeId.equals(myNodeId)) {
                Log.d(TAG, "Buyer node: id: " + buyerNodeId);
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(buyerNodeId);
                if (routingEntity != null) {
                    byte[] userListMessage = JsonDataBuilder.prepareInternetLeaveMessage(myNodeId, buyerNodeId, removeNodeIdList);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Internet] senUserList Wifi user");
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                        messageDispatcher.addSendMessage(baseMeshMessage);
                        // wifiTransPort.sendMeshMessage(routingEntity.getIp(),userListMessage);
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
                } else {
                    MeshLog.mm("senUserList User does not exist in routing table");
                }
            }
        }
    }


    @Override
    public void onReceivedMsgAck(String receiver, String sender, String messageId, int messageStatus, String ackBody, String ipAddress) {
    }


    @Override
    public void onInternetDirectUserReceived(String nodeId, ConcurrentLinkedQueue<NodeInfo> userNodeInfoList, Link link) { //Direct Users
        for (NodeInfo item : userNodeInfoList) {
            if (TextUtils.isEmpty(item.getUserId()) || item.getUserId().equals(myNodeId)) {
                continue;
            }

            RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(item.getUserId());

            if (routingEntity != null && routingEntity.isOnline() && routingEntity.getType() != RoutingEntity.Type.INTERNET) {

                verifyUserExistence(routingEntity, (ip, isReachable) -> {
                    if (isReachable) {
                        mLinkStateListener.onRemoteUserConnected(item.getUserId(), item.getPublicKey());
                        MeshLog.v("InternetMsg Removing peer connection from direct internet user ");
                        PeerConnectionHolder.closeSingleConnection(item.getUserId());
                    } else {
                        serveDirectInternetUserToUI(routingEntity, item);
                    }
                });
            } else {
                serveDirectInternetUserToUI(routingEntity, item);
            }

        }

        for (String buyerNodeId : connectionLinkCache.getInternetBuyerList()) {
            if (!buyerNodeId.equals(myNodeId)) {
                Log.d(TAG, "Buyer node: id: " + buyerNodeId);
                String userIdList = GsonUtil.on().toJsonFromQueue(userNodeInfoList);
                // String idListStr = android.text.TextUtils.join("@", userIdList);
                MeshLog.e("[Internet] Send internet user to buyer =" + userNodeInfoList + " for user " + nodeId);
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(buyerNodeId);
                if (routingEntity != null) {
                    byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, buyerNodeId, myNodeId, userIdList);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Internet] senUserList Wifi user");
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                        messageDispatcher.addSendMessage(baseMeshMessage);
                        // wifiTransPort.sendMeshMessage(routingEntity.getIp(),userListMessage);
                    } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                        messageDispatcher.addSendMessage(baseMeshMessage);
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
                } else {
                    MeshLog.mm("senUserList User does not exist in routing table");
                }
            }

        }
    }

    private void serveDirectInternetUserToUI(RoutingEntity routingEntity, NodeInfo item) {
        MeshLog.i("[Internet] User  Insert into routing table");

        if (routingEntity == null)
            routingEntity = new RoutingEntity(item.getUserId());

        routingEntity.setOnline(true);
        routingEntity.setType(RoutingEntity.Type.INTERNET);
        routingEntity.setHopAddress("");
        boolean updated = RouteManager.getInstance().updateRoute(routingEntity);
        if (updated) {
            //NodeInfo nodeInfo = new NodeInfo(item, "internetSSID", "internetLink", "public key", PreferencesHelper.MESH_USER, Constant.UserTpe.INTERNET);
            item.setUserType(RoutingEntity.Type.INTERNET);
            //item.setBleName("");
            //item.setSsidName("");

            connectionLinkCache.addNodeInfo(item.getUserId(), item);
            MeshLog.i("[Internet] User Send to UI");
            Log.d("Direct_user", "User id =" + item.getUserId() + " ");
            mLinkStateListener.onRemoteUserConnected(item.getUserId(), item.getPublicKey());
        }
    }

    @Override
    public void onInternetUserReceived(String nodeId, ConcurrentLinkedQueue<RoutingEntity> userIdList) { //Buyer list by other network seller
        if (userIdList == null) {
            Log.e(TAG, "User List null");
            return;
        }
        MeshLog.i("[Internet] onInternetUserReceived " + userIdList.toString() + "\nreceiver: " + nodeId);

        if (!nodeId.equalsIgnoreCase(myNodeId)) {
            MeshLog.i("[Internet] Internet User list receive =" + userIdList.toString());
            for (RoutingEntity item : userIdList) {

                if (TextUtils.isEmpty(item.getAddress()) || item.getAddress().equals(myNodeId)) {
                    continue;
                }

                RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(item.getAddress());

                if (route != null && route.isOnline() && (route.getType() != RoutingEntity.Type.INTERNET || TextUtils.isEmpty(route.getHopAddress()))) {

                    mLinkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());
                    continue;
                }

                MeshLog.i("[Internet] User  Insert into routing table");
                RoutingEntity routingEntity = new RoutingEntity(item.getAddress());
                routingEntity.setOnline(true);
                routingEntity.setType(RoutingEntity.Type.INTERNET);
                routingEntity.setHopAddress(nodeId); // this node id is sender of his buyer list
                boolean updated = RouteManager.getInstance().updateRoute(routingEntity);
                if (updated) {
                    //NodeInfo nodeInfo = new NodeInfo(item, "internetSSID", "internetLink", "public key", PreferencesHelper.MESH_USER, Constant.UserTpe.INTERNET);
//                    item.setUserType(RoutingEntity.Type.INTERNET);
                    //item.setBleName("");
                    //item.setSsidName("");
//                    connectionLinkCache.addNodeInfo(item.getAddress(), item);
                    MeshLog.i("[Internet] User Send to UI");

                    mLinkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());

                }
                // MeshLog.mm("Node info added");
                //connectionLinkCache.addInternetLink(item, internetLink);
            }
            //  RouteManager.getInstance().makeUserOfline(userIdList);

            // Here we have to send all info to all my connected buyer

            for (String buyerNodeId : connectionLinkCache.getInternetBuyerList()) {
                if (!buyerNodeId.equals(myNodeId)) {
                    String userNodeInfoList = GsonUtil.on().toJsonFromEntityList(userIdList);
                    // String idListStr = android.text.TextUtils.join("@", userIdList);
                    MeshLog.e("[Internet] Send internet user to buyer =" + userNodeInfoList + " for user " + nodeId);
                    RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(buyerNodeId);
                    if (routingEntity != null) {
                        byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, buyerNodeId, myNodeId, userNodeInfoList);
                        if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                            MeshLog.mm("[Internet] senUserList Wifi user");
                            BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                            messageDispatcher.addSendMessage(baseMeshMessage);
                        } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                            BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                            messageDispatcher.addSendMessage(baseMeshMessage);
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
                    } else {
                        MeshLog.mm("senUserList User does not exist in routing table");
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(String senderId, String receiver, String transactionId, byte[] data, String ipAddress, String immediateSender) {
        try {
            JSONObject jsonData = new JSONObject(new String(data));
            MeshLog.o("****** ServerInfoDebug: " + jsonData.toString());
            String message = jsonData.getString("text");
            Log.d(TAG, "Ack send");

            if (receiver.equalsIgnoreCase(myNodeId)) {
                mLinkStateListener.onMessageReceived(senderId, message.getBytes());
                MeshLog.mm("(I)Receiver id =" + receiver);
                InternetLink.getInstance().sendAck(receiver, senderId, transactionId, Constant.MessageStatus.RECEIVED);

            } else {
                MeshLog.v("Message Queuing Start for incoming");
                long messageSize = Util.buildWebRtcMessage(senderId, receiver, transactionId, message.getBytes()).getBytes().length;
                MeshLog.v("Message Byte Size incoming (dummy) " + messageSize);
                long byteSize = messageSize + Constant.MESSAGE_ACK_SIZE;
                mLinkStateListener.buyerInternetMessageReceived(senderId, receiver, transactionId,
                        message, byteSize, true, false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveMessageStatus(String senderId, String receiverId, String messageId, int messageStatus) {
        if (!TextUtils.isEmpty(receiverId)) {
            MeshLog.mm("message receive status got");
            if (receiverId.equals(myNodeId)) {
                MeshLog.mm("[Multiverse] message receive status send to my self to ui");
                mLinkStateListener.onMessageDelivered(messageId, messageStatus);
            } else {

                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);

                String ackBody = Util.buildInternetReceivingAckBody(myNodeId);

                byte[] ackMessage = JsonDataBuilder.buildAckMessage(myNodeId, receiverId, messageId, messageStatus, ackBody);

                if (routingEntity != null) {

                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Multiverse] Internet to P2P onReceiveMessageStatus() status =" + messageStatus);
                        wifiTransPort.sendMeshMessage(routingEntity.getIp(), ackMessage);

                    } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                        adHocTransport.sendAdhocMessage(routingEntity.getIp(), ackMessage);
                    } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                        MeshLog.mm("[Multiverse] Internet to Ble onReceiveMessageStatus() status =" + messageStatus);
                        Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                        if (bleLink != null) {
                            messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, ackMessage));
                            //sendEventQueue.execute(() -> bleLink.sendMeshMessage(ackMessage));
                        }
                    }
                }

            }
        }

    }

    @Override
    public void onFileMessageReceived(String sender, String receiver, String messageId, String message, String immediateSender) {

    }

    @Override
    public void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid, String password, int wifiNodeCount, boolean isFirstMessage) {

    }

    @Override
    public void onBroadcastReceived(Broadcast broadcast) {

    }

    @Override
    public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {

    }

    @Override
    public void onInternetUserLeave(String sender, String receiver, String userList) {
        //No use of this method 10/02/2020 major arif
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
        String receiverId = handshakeInfo.getReceiverId();

        if (!TextUtils.isEmpty(receiverId)) {

            if (receiverId.equalsIgnoreCase(myNodeId)) {
                mLinkStateListener.onHandshakeInfoReceived(handshakeInfo);

            } else {
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);

                String handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);
                byte[] message = handshakeInfoText.getBytes();

                if (routingEntity != null) {

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
        }
    }

    public void onBuyerConnected(String nodeId) {

        MeshLog.v("[Internet] buyer found in local " + nodeId);

        if (!nodeId.equalsIgnoreCase(myNodeId)) {
            // Here buyer added
            connectionLinkCache.addBuyerAddressToList(nodeId);
            Log.d(TAG, "Buyer connected");
            // Here new buyer info send to server
            InternetLink.getInstance().sendBuyer(nodeId);


            List<RoutingEntity> internetUserList = RouteManager.getInstance().getInternetUsers();

            ConcurrentLinkedQueue<NodeInfo> internetUserNodeInfoList = new ConcurrentLinkedQueue<>();
            if (CollectionUtil.hasItem(internetUserList)) {
                for (RoutingEntity routingEntity : internetUserList) {
                    NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(routingEntity.getAddress());
                    if (nodeInfo != null) {
                        nodeInfo = (NodeInfo) nodeInfo.clone();

                        nodeInfo.mGenerationTime = System.currentTimeMillis();
                        nodeInfo.setHopId(myNodeId);
                        internetUserNodeInfoList.add(nodeInfo);
                    }
                }

                String userNodeInfoList = GsonUtil.on().toJsonFromQueue(internetUserNodeInfoList);
                // String idListStr = android.text.TextUtils.join("@", userIdList);
                MeshLog.e("[Internet] Send internet user to buyer =" + userNodeInfoList + " for user " + nodeId);
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(nodeId);
                if (routingEntity != null) {
                    byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, nodeId, myNodeId, userNodeInfoList);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Internet] senUserList Wifi user");
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                        messageDispatcher.addSendMessage(baseMeshMessage);
                    } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                        BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                        messageDispatcher.addSendMessage(baseMeshMessage);
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
                } else {
                    MeshLog.mm("senUserList User does not exist in routing table");
                }
            }
        }
    }

    public void onBuyerDisconnected(String nodeId) {

        Log.d(TAG, "onBuyerDisconnected " + nodeId);

        if (!TextUtils.isEmpty(nodeId) && !nodeId.equals(myNodeId)) {

            if (connectionLinkCache.getInternetConnectionLink(nodeId)) {

                Log.d(TAG, "Internet buyer disconnected");
                connectionLinkCache.removeInternetLink(nodeId);
                if (InternetLink.getInstance() != null) {
                    InternetLink.getInstance().sendBuyerDisconnectMessage(nodeId);
                }
            }
        }
    }


    public void onPaymentGotForIncomingMessage(boolean success, String _owner, String _sender, String _msg_id, String _msgData, boolean isFile) {
        MeshLog.v("Message Queuing 16 " + success);
        processIncomingAfterPaymentComplete(success, _owner, _sender, _msg_id, _msgData, isFile);
    }

    private void processIncomingAfterPaymentComplete(boolean success, String _owner, String _sender, String _msg_id, String _msgData, boolean isFile) {

        if (success) {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(_owner);

            if (routingEntity != null) {
                MeshLog.mm("(-) RoutingEntity" + routingEntity.toString());

                String messageDataLocal = Util.buildInternetReceivingMessage(_msgData.getBytes(), myNodeId);

                byte[] message = JsonDataBuilder.buildMessage(_sender, _owner, _msg_id, messageDataLocal.getBytes());

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.i("sendMessage Wifi user");
                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);
                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    MeshLog.i("sendMessage ble user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                        //sendEventQueue.execute(() -> bleLink.sendMeshMessage(message));
                    } else {
                        MeshLog.i("(!) BLE LINK NOT FOUND");
                    }
                }
            } else {
                MeshLog.i("sendMessage User does not exist in routing table");
            }
        }
    }


    public void onPaymentGotForOutgoingMessage(boolean success, String owner, String sender, String msg_id, String msgData, boolean isFile) {
        MeshLog.v("Message Queuing 17 " + success);
        processOutGoingAfterPaymentComplete(success, owner, sender, msg_id, msgData, isFile);
    }

    private void processOutGoingAfterPaymentComplete(boolean success, String owner, String sender, String msg_id, String msgData, boolean isFile) {

        if (success) {
            messageDispatcher.addSendMessage(MessageBuilder.buildMeshInternetMessage((InternetLink) null, sender, owner, msg_id, msgData.getBytes()));
        }
    }

    public void processInternetOutgoingMessage(String sender, String receiver, String messageId, byte[] data) {
        MeshLog.v("Message Queuing Start for outgoing");
        long messageSize = Util.buildWebRtcMessage(sender, receiver, messageId, data).getBytes().length;
        MeshLog.v("Message Byte Size outgoing (dummy) " + messageSize);
        long bytesize = messageSize + Constant.MESSAGE_ACK_SIZE;

        mLinkStateListener.buyerInternetMessageReceived(sender, receiver, messageId, new String(data), bytesize, false, false);
    }

    public void sendUserInfoThroughInternet(String sender, String receiver, String messageId, byte[] data) {
        InternetLink.getInstance().sendUserInfoThroughInternet(sender, receiver, messageId, data);
    }


    public void sendBuyerReceivedAck(String sender, String receiver, String messageId, int status) {
        InternetLink internetLink = InternetLink.getInstance();
        if (internetLink != null) {
            internetLink.sendAck(sender, receiver, messageId, status);
        }

    }

    @Override
    public void onMessageSubmitted(int submissiomId, boolean success) {

    }

    public boolean isUserDirectlyConnected(String userId) {
        return PeerConnectionHolder.getPeerConnection(userId) != null;
    }

    public boolean amIDirectUser() {
        return !PeerConnectionHolder.getConnectionHolderList().isEmpty();
    }

    private void sampleConnection() {
        try {
            URL url = new URL("https://raw.githubusercontent.com/mobilesiri/JSON-Parsing-in-Android/master/index.html");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());

            Log.e(TAG, "Data found: " + convertStreamToString(in));
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (ProtocolException e) {
            Log.e(TAG, "ProtocolException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }


    void verifyUserExistence(RoutingEntity routingEntity, Pinger.PingListener listener) {
        if (routingEntity.getType() == RoutingEntity.Type.WiFi || routingEntity.getType() == RoutingEntity.Type.HB) {
            new Thread(new Pinger(routingEntity.getIp(), listener, 2)).start();
        } else {
            listener.onPingResponse(routingEntity.getIp(), true);
        }
    }

}*/
