package com.w3engineers.mesh.tunnel;

import android.content.Context;
import android.net.Network;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.Adhoc.AdHocTransport;
import com.w3engineers.mesh.ble.BleTransport;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.model.PendingMessage;
import com.w3engineers.mesh.queue.DiscoveryTask;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.NetworkOperationHelper;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public class RemoteTransport implements ConnectionStateListener, MeshTransport,
        RemoteServerCallback, TelemeshTunnel.TunnelStatusListener, TelemeshTunnel.TelemeshLogListener,
        TelemeshTunnel.TunnelConnectionListener, TunnelAutoConnect.ReTryCompletedCallbackListener {
    private final Context mContext;
    private final String myNodeId;
    private final int APP_PORT;
    private LinkStateListener mLinkStateListener;
    private ConnectionLinkCache connectionLinkCache;

    private WifiTransPort wifiTransPort;
    private BluetoothTransport bluetoothTransport;
    private AdHocTransport adHocTransport;
    private BleTransport bleTransport;

    private MessageDispatcher messageDispatcher;
    private String TAG = "RemoteTransport";
    private DispatchQueue dispatchQueue;
    private ForwardListener forwardListener;
    private RemoteManager mRemoteManager;

    private static final int startIndex = 0;
    private static final int endIndex = 42;
    private HashSet<String> internetUser;
    private boolean isTunnelingOngoing = false;
    private boolean isMobileDataActivated = false;

    TelemeshTunnel telemeshTunnel = null;
    private TunnelAutoConnect tunnelAutoConnect = null;
    private int sshStartflag = 0; // 0 for none, 1 for start tunnel from other and 2 for stop tunnel from other
    private UserDetailsReceivedListener mUserDetailsReceivedListener;
    private MeshLibMessageEventQueue messageEventQueue;
    private boolean isRemoteTransportRunning;
    private final NetworkOperationHelper.NetworkInterfaceListener networkInterfaceListener = new NetworkOperationHelper.NetworkInterfaceListener() {
        @Override
        public void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi) {
            MeshLog.ssh("onNetWorkStateChanged:: IsConnected:" + isOnline + "  Is WIfi:" + isWiFi);
            if (isOnline) {
                if (isWiFi) {

                    if (NetworkOperationHelper.isOnline()) {
                        if (!isTunnelingOngoing) {
                            if (TunnelConnectionStatus.isConnectionStatus() == TunnelConstant.DISCONNECTED) {
                                MeshLog.ssh(" Connecting from onNetWorkStateChanged");
                                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                                HandlerUtil.postBackground(() -> telemeshTunnel.startTunnel("onNetWorkStateChanged 1"), 8000);
                                sshStartflag = 1;
                            }
                        }
                    } else {
                        MeshLog.ssh("Wifi data has no internet");
                        TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                        telemeshTunnel.stopTunnel();
                        sshStartflag = 0;
                        leaveAllInternetUserFromDbUi();
                    }
                } else {
                    if (NetworkOperationHelper.isOnline()) {
                        if (!isTunnelingOngoing) {
                            if (TunnelConnectionStatus.isConnectionStatus() == TunnelConstant.DISCONNECTED) {
                                MeshLog.ssh(" 3 Connecting from onNetWorkStateChanged 1");
                                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                                telemeshTunnel.startTunnel("onNetWorkStateChanged 2");
                                sshStartflag = 1;
                            }
                        }
                    } else {
                        MeshLog.ssh("Mobile data has no internet");
                        TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                        telemeshTunnel.stopTunnel();
                        sshStartflag = 0;
                        leaveAllInternetUserFromDbUi();
                    }
                }
            } else {
                if (TunnelConnectionStatus.isSshConnected()) {
                    MeshLog.ssh("Disconnected for WiFi Adapter off");
                    telemeshTunnel.stopTunnel();
                    sshStartflag = 0;
                    TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                    leaveAllInternetUserFromDbUi();
                }
            }
        }
    };

    public RemoteTransport(Context context, int appPort, String nodeId, ConnectionLinkCache connectionLinkCache,
                           LinkStateListener listener, MessageDispatcher messageDispatcher) {
        mContext = context;
        APP_PORT = appPort;
        myNodeId = nodeId;
        mLinkStateListener = listener;
        this.connectionLinkCache = connectionLinkCache;
        this.messageDispatcher = messageDispatcher;
        this.dispatchQueue = new DispatchQueue();


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
            } else if (item instanceof BleTransport) {
                this.bleTransport = (BleTransport) item;
            } else if (item instanceof MeshLibMessageEventQueue) {
                messageEventQueue = (MeshLibMessageEventQueue) item;
            }
        }
    }


    @Override
    public void start() {
        //Init tunnel related object at start time

        mRemoteManager = new RemoteManager(this, messageDispatcher, this.mContext, messageEventQueue, myNodeId);
        telemeshTunnel = new TelemeshTunnel(TunnelConstant.dotRemoteUrl,
                TunnelConstant.serverSSHPort, APP_PORT,
                TunnelConstant.serverHTTPPort);
        tunnelAutoConnect = new TunnelAutoConnect(telemeshTunnel);

        Log.e("Remote_transport", "Remote Transport started");
        TelemeshTunnel.setTunnelStatusListener(this::onTunnelStatusUpdated);
        TelemeshTunnel.setTelemeshLogListener(this::onLog);
        TelemeshTunnel.setTunnelConnectionListener(this::onTunnelConnectionUpdated);
        TunnelAutoConnect.setReTryCompletedCallbackListener(this::onReTryCompletedCallbackListener);
        NetworkOperationHelper.setNetworkInterfaceListeners(networkInterfaceListener, mContext);

//        NetworkOperationHelper.setNetWorkStateChangedListener(this::onNetWorkStateChanged);
//        NetworkOperationHelper.registerNetReceiver(mContext);


        internetUser = new HashSet();
        MeshLog.v("init internet transport :: done");
        initialRequestToSshServer();
        this.isRemoteTransportRunning = true;
    }

    @Override
    public void stop() {
        this.isRemoteTransportRunning = false;
        Log.e("Remote_transport", "Remote Transport stopped");
        tunnelAutoConnect.stopAutoConnect();
        isTunnelingOngoing = false;
        telemeshTunnel.stopTunnel();


        //Delay added so that tunnel server stopped properly
        //for proper tunneling server stopped
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        NetworkOperationHelper.unregisterWifiReceiver(mContext);
//        NetworkOperationHelper.setNetWorkStateChangedListener(null);
//        NetworkOperationHelper.registerNetReceiver(mContext);

        NetworkOperationHelper.removeNetworkInterfaceListeners(networkInterfaceListener);
        TelemeshTunnel.setTunnelStatusListener(null);
        TelemeshTunnel.setTelemeshLogListener(null);

        //Clear previous object
        telemeshTunnel = null;
        mRemoteManager = null;
    }

    @Override
    public void onTunnelConnectionUpdated(boolean isConnected) {
        if (!isRemoteTransportRunning) {
            MeshLog.ssh("Remote_transport return due to stopped ");
            return;
        }
        if (!isConnected && telemeshTunnel.isTunnelConnected()) {
            MeshLog.e("Remote_transport  tunnel connected but log Trigger disconnected");
            return;
        }
        MeshLog.ssh("Remote_transport Tunnel Connection status: " + isConnected + " session :" + telemeshTunnel.isTunnelConnected());

        if (isConnected) {
            MeshLog.e("Tunnel Connected");
            MeshLog.ssh("Connected from onTunnelConnectionUpdated");
            TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTED);
            TunnelConnectionStatus.setSshTunnelConnected(true);
            tunnelAutoConnect.stopAutoConnect();
            sshStartflag = 0;
            try {
                HandlerUtil.postForeground(() -> Toaster.showLong("SSH connected .."));
            } catch (Exception e) {
                MeshLog.e(e.getMessage());
            }
        } else {
            MeshLog.ssh("Tunnel Disconnected");
            if (NetworkOperationHelper.isOnline()) {
                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                tunnelAutoConnect.stopAutoConnect();
                sshStartflag = 0;
                MeshLog.ssh("Trying to Reconnect....");
                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                tunnelAutoConnect.startAutoConnect(sshStartflag);
                sshStartflag = 1;
                try {
                    HandlerUtil.postForeground(() -> Toaster.showLong("SSH Disconnected! Reconnecting...."));
                } catch (Exception e) {
                    MeshLog.e(e.getMessage());
                }
            } else {
                MeshLog.ssh("Failed To Reconnect As Internet isn't available");
                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                tunnelAutoConnect.stopAutoConnect();
                sshStartflag = 0;
            }
        }
    }


    /**
     * Connection status listener from tunnel service
     *
     *
     */

    /**
     * Starting tunnel service
     *
     * @param subdomain to
     */
    private void startTunnelService(String subdomain) {
        if (TextUtils.isEmpty(telemeshTunnel.getSubdomainName())) {
            telemeshTunnel.setSubdomainName(subdomain);
        }


        if (NetworkOperationHelper.isOnline()) {
            HandlerUtil.postForeground(() -> MeshLog.i("Internet connection is available"));
            telemeshTunnel.startTunnel("startTunnelService 2");
            sshStartflag = 1;
        } else {
            HandlerUtil.postForeground(() -> MeshLog.i("Internet connection is not available, trying to activate mobile data"));
        }

//            if (NetworkOperationHelper.isConnectedToInternet(mContext)) {
//                HandlerUtil.postForeground(() -> MeshLog.i("Internet connection is available"));
//                telemeshTunnel.startTunnel("startTunnelService 2");
//                sshStartflag = 1;
//            } else {
//                try {
//                    Thread.sleep(2000);
//                    if (NetworkOperationHelper.isConnectedToInternet(mContext)) {
//                        HandlerUtil.postForeground(() -> MeshLog.i("Internet connection is available"));
//                        telemeshTunnel.startTunnel("startTunnelService 3");
//                        sshStartflag = 1;
//                    } else {
//                        HandlerUtil.postForeground(() -> MeshLog.i("Internet connection is not available, trying to activate mobile data"));
//                        NetworkOperationHelper.setMobileDataActivationListener(this::onMobileDataChecked);
//
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            NetworkOperationHelper.forceNetWorkConnection(mContext, false);
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }

    }


    public void sendAppMessage(String messageId, String receiverId, byte[] data) {
        MeshLog.v("FILE_SPEED_TEST_8 " + Calendar.getInstance().getTime());
        messageEventQueue.addAppMessageInQueue(new DiscoveryTask(messageId, receiverId, data) {
            @Override
            public void run() {
                try {
                    boolean isSuccess = false;
                    while (this.retryCount < this.maxRetryCount) {
                        this.retryCount++;
                        int result = mRemoteManager.postData(this.ipOrAddress, new String(this.messageData));
                        isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        if (isSuccess) {
                            break;
                        }
                    }
                    middleManMessageSendStatus(this.messagePublicId, isSuccess);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    middleManMessageSendStatus(this.messagePublicId, false);
                }
            }
        });
    }

    private void middleManMessageSendStatus(String messageId, boolean isSuccess) {
        if (isSuccess) {
            connectionLinkCache.removePendingMessage(messageId);
        } else {
            MeshLog.e("[Middleman] wifi attempt to send other path");

            PendingMessage pendingMessage = connectionLinkCache.getPendingMessage(messageId);
            if (pendingMessage != null) {

                List<RoutingEntity> offlineEntities = RouteManager.getInstance()
                        .makeUserOffline(pendingMessage.previousAttemptEntity);

                if (CollectionUtil.hasItem(offlineEntities)) {
                    for (RoutingEntity entity : offlineEntities) {
                        mLinkStateListener.onUserDisconnected(entity.getAddress());
                    }
                }

                if (pendingMessage.routeQueue.isEmpty()) {
                    MeshLog.v("[Middleman] wifi  queue is empty previous id :" + AddressUtil.makeShortAddress(pendingMessage.previousSender));

                    /*if (!myNodeId.equals(pendingMessage.previousSender)) {
                        byte[] failedAck = JsonDataBuilder.buildFailedMessageAck(myNodeId, pendingMessage.actualSender,
                                pendingMessage.actualReceiver, pendingMessage.messageId);
                        RoutingEntity entity = RouteManager.getInstance().getShortestPath(pendingMessage.previousSender);

                        if (entity != null) {
                            MeshLog.v("[Middleman] wifi  error message send to :" + AddressUtil.makeShortAddress(entity.getAddress()) + " hop :" + entity.getHopAddress());
                            if (entity.getType() == RoutingEntity.Type.BLE) {
                                bleTransport.addAppMessageInQueue(pendingMessage.messageId, entity.getAddress(), failedAck);
                            } else {
                                wifiTransPort.sendAppMessage(pendingMessage.messageId, entity.getIp(), failedAck);
                            }
                        }
                    }*/
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

                    if (routingEntity.getType() == RoutingEntity.Type.INTERNET &&
                            RouteManager.getInstance().isDirectlyConnected(routingEntity.getAddress())) {

                        sendAppMessage(pendingMessage.messageId, routingEntity.getAddress(), pendingMessage.messageData);

                    }

                   /* if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        bleTransport.addAppMessageInQueue(pendingMessage.messageId, routingEntity.getAddress(), pendingMessage.messageData);
                    } else {
                        sendAppMessage(pendingMessage.messageId, routingEntity.getIp(), pendingMessage.messageData);
                    }*/
                }
            } else {
                MeshLog.v("[Middleman] wifi  pending message not found");
            }
        }
    }

    public void sendAppMessage(String receiverId, byte[] data) {
        messageEventQueue.addAppMessageInQueue(new DiscoveryTask(receiverId, () -> data) {
            @Override
            public void run() {
                try {
                    while (this.retryCount < this.maxRetryCount) {
                        this.retryCount++;
                        int result = mRemoteManager.postData(this.receiverId, new String(this.puller.getData()));
                        boolean isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        if (isSuccess) {
                            break;
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void postMessage(String receiverId, byte[] data) {
        messageEventQueue.addDiscoveryTaskInLast(new DiscoveryTask(receiverId, () -> data) {
            @Override
            public void run() {
                try {
                    while (this.retryCount < this.maxRetryCount) {
                        this.retryCount++;
                        int result = mRemoteManager.postData(this.receiverId, new String(this.puller.getData()));
                        boolean isSuccess = result == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                        if (isSuccess) {
                            break;
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onReceivedRemoteUsersId(List<String> usersIdList) {

        String data = JsonDataBuilder.prepareRemoteHelloPacket(myNodeId, GsonUtil.getUserInfo());
        for (String userId : usersIdList) {
            if (data.length() > 0) {
                if (userId.contains(myNodeId) || userId.length() < 38) continue;
                MeshLog.e("RemoteTransport  onReceivedRemoteUsersId:  " + userId);
                // just parse the meshID

                String userAddress = userId.substring(startIndex, endIndex);
                internetUser.add(userAddress);
                postMessage(userAddress, data.getBytes());
            }
        }
    }

    @Override
    public void onReceivedUserDetailsFromRemoteUser(String senderInfo, String connectedBuyers) {
        MeshLog.e("RemoteTransport  onReceivedUserDetailsFromRemoteUser: " + senderInfo);

        RoutingEntity sender = verifySender(senderInfo);
        if (sender == null) return;

        ConcurrentLinkedQueue<RoutingEntity> connectedBuyersDetails = GsonUtil.on().getEntityQueue(connectedBuyers);

        //RoutingEntity existingEntity = RouteManager.getInstance().getRoutingEntityByAddress(sender.getAddress());

        //if (existingEntity != null && existingEntity.isOnline()) return;

        // May be this user is online and connected by locally. So we cannot ignore it now
        // But there is a possibility that this user is connected in INTERNET


        ConcurrentLinkedQueue<RoutingEntity> allUsers = new ConcurrentLinkedQueue<>();
        allUsers.add(sender);

        if (CollectionUtil.hasItem(connectedBuyersDetails)) {
            allUsers.addAll(connectedBuyersDetails);
        }
        updateRoutingTableAndUI(allUsers);

        sendResponseToTarget(sender);

        passTheseUserToConnectedBuyers(allUsers);
    }


    @Override
    public void onReceivedUserDetailsResponseFromRemoteUser(String senderInfo, String localUsers) {
        MeshLog.e("RemoteTransport  onReceivedUserDetailsResponseFromRemoteUser: " + senderInfo);
        RoutingEntity sender = verifySender(senderInfo);
        if (sender == null) return;

        ConcurrentLinkedQueue<RoutingEntity> connectedBuyersDetails = GsonUtil.on().getEntityQueue(localUsers);

        //RoutingEntity existingEntity = RouteManager.getInstance().getRoutingEntityByAddress(sender.getAddress());
        //if (existingEntity != null && existingEntity.isOnline()) return;

        // May be this user is online and connected by locally. So we cannot ignore it now
        // But there is a possibility that this user is connected in INTERNET

        ConcurrentLinkedQueue<RoutingEntity> allUsers = new ConcurrentLinkedQueue<>();
        allUsers.add(sender);

        if (CollectionUtil.hasItem(connectedBuyersDetails)) {
            allUsers.addAll(connectedBuyersDetails);
        }
        updateRoutingTableAndUI(allUsers);

        passTheseUserToConnectedBuyers(allUsers);

    }

    @Override
    public void onReceivedRemoteUsersLeaveId(String disconnectedAddress) {
        if (disconnectedAddress != null && disconnectedAddress.length() > 38) {
            String disconnectedId = disconnectedAddress.substring(startIndex, endIndex);
            if (!TextUtils.isEmpty(disconnectedId)) {
                MeshLog.w("onReceivedRemoteUsersLeaveId >>" + AddressUtil.makeShortAddress(disconnectedId));
                RoutingEntity disconnectedEntity = RouteManager.getInstance().getSingleUserInfoByType(disconnectedId, RoutingEntity.Type.INTERNET);
                // Check disconnect node was a BT connection
                if (disconnectedEntity != null && disconnectedEntity.getType() == RoutingEntity.Type.INTERNET) {

                    List<RoutingEntity> offlineEntities = RouteManager.getInstance().updateNodeAsOfflineForInternet(disconnectedId);

                    // remove from own end
                    if (CollectionUtil.hasItem(offlineEntities)) {
                        removeOfflineEntitiesFromUiANDdb(offlineEntities);
                    }
                    List<String> leavNodeList = makeLeaveList(offlineEntities);
                    if (CollectionUtil.hasItem(leavNodeList)) {
                        forwardLeaveMessageToBuyers(leavNodeList);
                    }

                } else {
                    if (disconnectedEntity != null) {
                        MeshLog.e("REMOTE offline fails:" + disconnectedEntity.toString());
                    }
                }
            }
        }
    }

    @Override
    public void onDisconnectLink(Link link) {
    }

    @Override
    public void onMeshLinkFound(String sender, String hopNodeId, String jsonString) {

    }

    @Override
    public void onMeshLinkDisconnect(String nodeIds, String hopId) {

        String[] leaveNodeIds = nodeIds.split(",");


       /* RoutingEntity route = RouteManager.getInstance().getRoutByDestinationAndHopAndType(nodeId, hopId, RoutingEntity.Type.INTERNET);

        if (route == null) {
            return;
        }*/

        //RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(nodeId);
//        if (!TextUtils.isEmpty(hopId)) {
//            // It is buyer
//            // We have to check that its already hop ID match
//
//            if (route != null && route.getHopAddress() != null && !route.getHopAddress().equalsIgnoreCase(hopId)) {
//                MeshLog.i("[remoteTransport] hop id node valid for buyer disconnection");
//                return;
//            }
//        }
        //If not internet user no need to remove
//        if (route != null && route.getType() != RoutingEntity.Type.INTERNET) {
//            return;
//        }
//        List<String> idList = makeLeaveList(nodeId);


        RouteManager.getInstance().makeInternetUserOffline(hopId, Arrays.asList(leaveNodeIds));
        List<String> userLeft = new ArrayList<>();

        for (String nodeId : leaveNodeIds) {
            RoutingEntity entity = RouteManager.getInstance().getSingleUserInfoByType(nodeId, RoutingEntity.Type.INTERNET);

            if (entity == null) {
                // That means this buyer has only one connection
                // swo we can notify UI and forward to other

                if (mLinkStateListener != null) {
                    mLinkStateListener.onUserDisconnected(nodeId);
                }
                userLeft.add(nodeId);
            }
        }
        forwardLeaveMessageToBuyers(userLeft);
    }

    private List<String> makeLeaveList(String nodeId) {
        List<RoutingEntity> childAddressList = RouteManager.getInstance().getAllDisconnectedInternetUser(nodeId);
        List<String> idList = new ArrayList<>();
        idList.add(nodeId);

        if (childAddressList != null && !childAddressList.isEmpty()) {
            MeshLog.v(TAG + " Child list size: " + childAddressList.size());
            for (RoutingEntity entity : childAddressList) {
                if (!idList.contains(entity.getAddress())) {
                    idList.add(entity.getAddress());
                }

            }
        }
        return idList;
    }

    private List<String> makeLeaveList(List<RoutingEntity> childAddressList) {

        List<RoutingEntity> validChildData = new ArrayList<>();
        // now check that child user exists in internet or not
        for (RoutingEntity entity : childAddressList) {
            RoutingEntity oldRoute = RouteManager.getInstance().getSingleUserInfoByType(entity.getAddress(), RoutingEntity.Type.INTERNET);
            if (oldRoute != null && oldRoute.isOnline()) {
                validChildData.add(oldRoute);
            }
        }

        childAddressList.removeAll(validChildData);

        List<String> idList = new ArrayList<>();
        if (childAddressList != null && !childAddressList.isEmpty()) {
            MeshLog.v(TAG + " Child list size: " + childAddressList.size());
            for (RoutingEntity entity : childAddressList) {
                if (!idList.contains(entity.getAddress())) {
                    idList.add(entity.getAddress());
                    mLinkStateListener.onUserDisconnected(entity.getAddress());
                }
            }
        }
        return idList;
    }

    private void forwardLeaveMessageToBuyers(List<String> idList) {
        if (idList.isEmpty())
            return;

        String removeNodeIdList = TextUtils.join("@", idList);
        for (String address : ConnectionLinkCache.getInstance().getInternetBuyerList()) {
            if (!address.equals(myNodeId)) {
                Log.d(TAG, "Buyer node: id: " + address);
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(address);
                if (routingEntity != null) {
                    byte[] userListMessage = JsonDataBuilder.prepareInternetLeaveMessage(myNodeId, address, removeNodeIdList);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Internet] send disconnected user to wifi");

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
                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        MeshLog.mm("[Internet] send disconnected user to BLE");
                        bleTransport.sendMessage(routingEntity.getAddress(), userListMessage);
                    }
                } else {
                    MeshLog.mm("senUserList User does not exist in routing table");
                }
            }
        }
    }


    @Override
    public void onInternetUserReceived(String nodeId, ConcurrentLinkedQueue<RoutingEntity> userIdList) {
        //Buyer list by other network seller
        if (userIdList == null) {
            Log.e(TAG, "User List null");
            return;
        }
        MeshLog.i("[Internet] onInternetUserReceived " + userIdList.toString() + "\nreceiver: " + nodeId);

        if (!nodeId.equalsIgnoreCase(myNodeId)) {
            MeshLog.i("[Internet] Internet User list receive =" + userIdList.toString());
            for (RoutingEntity item : userIdList) {

                updateUiAndDB(nodeId, item);

            }
            // Here we have to send all info to all my connected buyer
            passTheseUserToConnectedBuyers(userIdList);
        }
    }

    private void updateUiAndDB(String nodeId, RoutingEntity item) {
        if (TextUtils.isEmpty(item.getAddress()) || item.getAddress().equals(myNodeId)) {
            return;
        }

        /*RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(item.getAddress());

        if (route != null && route.isOnline() && (route.getType() != RoutingEntity.Type.INTERNET)) {

            // We are not insert new row here. Because this user already connected by internet.
            // We will update this user with new data

            // Only one user has one row for Internet. So we are updating current roe

            route.setHopAddress(item.getHopAddress());
            route.setHopCount(item.getHopCount());

            boolean isReplaced = RouteManager.getInstance().replaceRoute(route);

            if (isReplaced) {
                mLinkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey());
            }

            return;
        }*/


        MeshLog.i("[Internet] User  Insert into routing table");


        boolean updated = updateRouteWithCurrentTime(item);

        if (updated) {
            MeshLog.i("[Internet] User Send to UI");
            HandlerUtil.postBackground(() -> mLinkStateListener.onRemoteUserConnected(item.getAddress(), item.getPublicKey()), 2000L);

        }
    }

    public void onBuyerConnected(String nodeId) {
        MeshLog.v("[Remote] buyer found in local " + nodeId);

        if (!nodeId.equalsIgnoreCase(myNodeId)) {
            // Here buyer added
            connectionLinkCache.addBuyerAddressToList(nodeId);
            Log.d(TAG, "Buyer connected");
            RoutingEntity buyerInfo = RouteManager.getInstance().getEntityByAddress(nodeId);

            sendBuyerToInternetUser(buyerInfo);

            passInernetUsersToThisBuyer(nodeId);
        }
    }

    private void passInernetUsersToThisBuyer(String nodeId) {
        List<RoutingEntity> internetUserList = RouteManager.getInstance().getInternetUsers();

        ConcurrentLinkedQueue<RoutingEntity> internetUserNodeInfoList = new ConcurrentLinkedQueue<>();
        if (CollectionUtil.hasItem(internetUserList)) {
            for (RoutingEntity routingEntity : internetUserList) {

                if (!routingEntity.getAddress().equalsIgnoreCase(nodeId)) {
                    routingEntity.setHopAddress(myNodeId);
                    routingEntity.setHopCount(routingEntity.getHopCount() + 1);
                    routingEntity.setType(RoutingEntity.Type.INTERNET);

                    internetUserNodeInfoList.add(routingEntity);
                }
            }

            String userNodeInfoList = GsonUtil.on().toJsonFromEntityList(internetUserNodeInfoList);
            // String idListStr = android.text.TextUtils.join("@", userIdList);
            MeshLog.e("[Internet] Send internet user to buyer =" + userNodeInfoList + " for user " + nodeId);
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(nodeId);
            if (routingEntity != null) {
                byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, nodeId, myNodeId, userNodeInfoList);
                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.mm("[Internet] senUserList Wifi user");

                    wifiTransPort.sendMeshMessage(routingEntity.getIp(), userListMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    BaseMeshMessage baseMeshMessage = MessageBuilder.buildWiFiDiscoveryMeshMessage(routingEntity.getIp(), () -> userListMessage);
                    messageDispatcher.addSendMessage(baseMeshMessage);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {

                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        MeshLog.mm("[Internet] senUserList bt user");
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtDiscoveryMessage((BleLink) bleLink, () -> userListMessage));
                        //sendEventQueue.execute(() -> bleLink.sendMeshMessage(userListMessage));
                    } else {
                        MeshLog.mm("[Internet] senUserList BLE LINK NOT FOUND");
                    }
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    MeshLog.mm("[Internet] senUserList ble user");
                    bleTransport.sendMessage(routingEntity.getAddress(), userListMessage);
                }
            } else {
                MeshLog.mm("senUserList User does not exist in routing table");
            }
        }
    }

    public void onBuyerDisconnected(String nodeIds) {
        MeshLog.v("onBuyerDisconnected " + nodeIds);

        if (!TextUtils.isEmpty(nodeIds) && !nodeIds.equals(myNodeId)) {
            for (String meshID : internetUser) {
                sendMessage(meshID, JsonDataBuilder.prepareBuyerLeaveMessage(nodeIds, myNodeId));
            }

            String[] nodeList = nodeIds.split(",");

            for (String nodeId : nodeList) {
                if (connectionLinkCache.getInternetConnectionLink(nodeId)) {
                    connectionLinkCache.removeInternetLink(nodeId);
                }
            }
        }
    }

    private void passTheseUserToConnectedBuyers(ConcurrentLinkedQueue<RoutingEntity> connectedNodes) {

        for (RoutingEntity entity : connectedNodes) {
            entity.setHopAddress(myNodeId);
            entity.setIp(null);
            entity.setType(RoutingEntity.Type.INTERNET);
            entity.setHopCount(entity.getHopCount() + 1);
        }
        String userIdList = GsonUtil.on().toJsonFromEntityList(connectedNodes);

        ConcurrentLinkedQueue<String> buyers = connectionLinkCache.getInternetBuyerList();
        MeshLog.v("buyer List " + buyers.toString());
        for (String buyerNodeId : buyers) {
            if (!buyerNodeId.equals(myNodeId)) {
                Log.d(TAG, "Buyer node: id: " + buyerNodeId);

                // String idListStr = android.text.TextUtils.join("@", userIdList);
                MeshLog.e("[Internet] Send internet user to buyer =" + buyerNodeId + " for user " + userIdList);
                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(buyerNodeId);
                if (routingEntity != null) {
                    byte[] userListMessage = JsonDataBuilder.buildInternetUserIds(myNodeId, buyerNodeId, myNodeId, userIdList);
                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Internet] senUserList Wifi user");
                        wifiTransPort.sendMeshMessage(routingEntity.getIp(), userListMessage);
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
                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        bleTransport.sendMessage(routingEntity.getAddress(), userListMessage);
                    }
                } else {
                    MeshLog.mm("senUserList User does not exist in routing table");
                }
            }

        }
    }


    @Override
    public void onMessageReceived(String sender, String receiver, String messageId, byte[] frameData, String ipAddress, String immediateSender) {

        if (receiver.equalsIgnoreCase(myNodeId)) {
            mLinkStateListener.onMessageReceived(sender, frameData);
            MeshLog.mm("(I)Receiver id =" + receiver);
            String ackBody = Util.buildRemoteAckBody();
            sendMessageAck(receiver, sender, messageId, Constant.MessageStatus.RECEIVED, ackBody, immediateSender);

        } else {
            MeshLog.v("Message Queuing Start for incoming");
//            long messageSize = Util.buildWebRtcMessage(sender, receiver, messageId, frameData).getBytes().length;
            long messageSize = JsonDataBuilder.buildMessage(sender, receiver, messageId, frameData).length;
            MeshLog.v("Message Byte Size incoming (dummy) " + messageSize);
            long byteSize = messageSize + Constant.MESSAGE_ACK_SIZE;

//            onPaymentGotForIncomingMessage(true, receiver, sender, messageId, new String(frameData));

            mLinkStateListener.buyerInternetMessageReceived(sender, receiver, messageId, new String(frameData), byteSize, true, false);
        }
    }


    @Override
    public void onReceivedMsgAck(String sender, String receiver, String messageId, int messageStatus, String ackBody, String ipAddress) {
        if (!TextUtils.isEmpty(receiver)) {
            MeshLog.mm("message receive status got");
            if (receiver.equals(myNodeId)) {
                MeshLog.mm("[Multiverse] message receive status send to my self to ui");
                mLinkStateListener.onMessageDelivered(messageId, messageStatus);
            } else {

                RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);

                String ackBody1 = Util.buildInternetReceivingAckBody(myNodeId);

                byte[] ackMessage = JsonDataBuilder.buildAckMessage(myNodeId, receiver, messageId, messageStatus, ackBody1);

                if (routingEntity != null) {

                    if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                        MeshLog.mm("[Multiverse] Internet to P2P onReceiveMessageStatus() status =" + messageStatus);
                        wifiTransPort.sendAppMessage(routingEntity.getIp(), ackMessage);

                    } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                        adHocTransport.sendAdhocMessage(routingEntity.getIp(), ackMessage);
                    } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                        MeshLog.mm("[Multiverse] Internet to Ble onReceiveMessageStatus() status =" + messageStatus);
                        Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                        if (bleLink != null) {
                            messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, ackMessage));
                            //sendEventQueue.execute(() -> bleLink.sendMeshMessage(ackMessage));
                        }
                    } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                        MeshLog.mm("[Multiverse] Internet to Ble onReceiveMessageStatus() status =" + messageStatus);
                        bleTransport.addAppMessageInQueue(null, routingEntity.getAddress(), ackMessage);
                    }
                }

            }
        }
    }


    @Override
    public void onReceiveBuyerFileMessage(String sender, String receiver, String messageData, int fileMessageType, String immediateSender, String messageId) {
        MeshLog.v("FILE_SPEED_TEST_10 " + Calendar.getInstance().getTime());
        try {
            RoutingEntity entity = RouteManager.getInstance().getSingleUserInfoByType(sender, RoutingEntity.Type.INTERNET);
            if (entity == null) {
                makeInternetUserOnline(sender, immediateSender);
            }

            if (receiver.equals(myNodeId)) {
                MeshLog.v("Buyer_file received in remote transport");

                JSONObject msgObject = new JSONObject(messageData);
                msgObject.put(JsonDataBuilder.KEY_IMMEDIATE_SENDER, immediateSender);
                mLinkStateListener.onFileMessageReceived(sender, msgObject.toString());
            } else {
                if (fileMessageType == Constant.FileMessageType.FILE_ACK_MESSAGE || fileMessageType == Constant.FileMessageType.FILE_INFO_MESSAGE) {
                    String messageDataLocal = Util.buildInternetReceivingMessage(messageData.getBytes(), myNodeId);
                    byte[] message = JsonDataBuilder.buildBuyerFileMessage(sender, receiver, messageDataLocal.getBytes(), fileMessageType, messageId);

                    RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);

                    if (routingEntity != null) {
                        MeshLog.v("Buyer_file file message received need to pass :" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
                        if (routingEntity.getType() == RoutingEntity.Type.WiFi) {
                            wifiTransPort.sendMeshMessage(routingEntity.getIp(), message);
                        } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                            bleTransport.addFileMessageInQueue(routingEntity.getAddress(), messageId, message);
                            /*boolean isSuccess = bluetoothTransport.sendMeshMessage(routingEntity.getAddress(), message);
                            if(!isSuccess) {
                                bleTransport.sendFileMessage(routingEntity.getAddress(), message);
                            }*/
                        }
                    }
                } else {
                    long messageSize = JsonDataBuilder.buildBuyerFileMessage(sender, receiver, messageData.getBytes(), fileMessageType, messageId).length;
                    long byteSize = messageSize + Constant.FILE_MESSAGE_ACK_SIZE;
                    mLinkStateListener.buyerInternetMessageReceived(sender, receiver, messageId, messageData, byteSize, true, true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveMessageStatus(String senderId, String receiverId, String messageId, int messageStatus) {

    }

//    @Override
//    public void onHighBandMessage(String senderAddress, String receiverAddress, byte[] message) {
//        //Nothing to DO in Internet for now
//    }
//
//    @Override
//    public void onDryHighBandMessage(String senderAddress, String receiverAddress, byte[] message) {
//        //Nothing to DO in Internet for now
//    }
//
//    @Override
//    public void onHighBandConfirmationMessageWithCredential(String senderAddress, String receiverAddress, String ssid, String key) {
//
//    }

    @Override
    public void onFileMessageReceived(String sender, String receiver, String messageId, String message, String immediateSender) {
        Log.d("ABCD ", message);
        mLinkStateListener.onFileMessageReceived(sender, message);
    }

    @Override
    public void onBroadcastReceived(Broadcast broadcast) {

    }

    @Override
    public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {

    }

    @Override
    public void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid, String password, int wifiNodeCount, boolean isFirstMessage) {

    }

    @Override
    public void onInternetUserLeave(String sender, String receiver, String userList) {
        //No use of this method 10/02/2020 major arif
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
        String receiverId = handshakeInfo.getReceiverId();

        if (receiverId.equalsIgnoreCase(myNodeId)) {
            mLinkStateListener.onHandshakeInfoReceived(handshakeInfo);

        } else {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiverId);
            String handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);
            byte[] message = handshakeInfoText.getBytes();

            if (routingEntity != null) {

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    wifiTransPort.sendAppMessage(routingEntity.getIp(), message);

                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);

                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());

                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                    }
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    bleTransport.sendAppMessage(routingEntity.getAddress(), message);

                }
            }
        }
    }

    /*@Override
    public void onVersionMessageReceived(String sender, String receiver, int versionCode, String appToken, int versionType) {
        // Todo Not need to implement any code here
    }*/

    private void sendBuyerToInternetUser(RoutingEntity buyerNodeInfo) {
        MeshLog.e("sendBuyerToInternetUser");
        List<RoutingEntity> buyerNodeList = new ArrayList<>();
        buyerNodeInfo.setHopAddress(myNodeId);
        buyerNodeInfo.setIp(null);
        buyerNodeInfo.setHopCount(buyerNodeInfo.getHopCount() + 1);
        buyerNodeInfo.setType(RoutingEntity.Type.INTERNET);
        buyerNodeList.add(buyerNodeInfo);


        for (String meshId : internetUser) {
            sendMessage(meshId, JsonDataBuilder.prepareBuyerUserList(buyerNodeList, myNodeId));
        }

    }


    public void onPaymentGotForIncomingMessage(boolean success, String _owner, String _sender, String _msg_id, String _msgData, boolean isFile) {
        processIncomingAfterPaymentComplete(success, _owner, _sender, _msg_id, _msgData, isFile);
    }

    private void processIncomingAfterPaymentComplete(boolean success, String _owner, String _sender, String _msg_id, String _msgData, boolean isFile) {
        if (success) {

            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(_owner);

            if (routingEntity != null) {
                MeshLog.mm("(-) RoutingEntity" + routingEntity.toString());

                String messageDataLocal = Util.buildInternetReceivingMessage(_msgData.getBytes(), myNodeId);

                byte[] message = null;

                if (isFile) {
                    message = JsonDataBuilder.buildBuyerFileMessage(_sender, _owner, messageDataLocal.getBytes(), Constant.FileMessageType.FILE_PACKET_MESSAGE, _msg_id);
                } else {
                    message = JsonDataBuilder.buildMessage(_sender, _owner, _msg_id, messageDataLocal.getBytes());
                }

                //Save pending message in cache for multi-path try
                PendingMessage pendingMessage = new PendingMessage(_msg_id, _sender, _owner, null, message, routingEntity);
                List<RoutingEntity> allPossiblePath = connectionLinkCache.filterShortestPathEntity(routingEntity, _owner);
                pendingMessage.routeQueue.addAll(allPossiblePath);
                connectionLinkCache.addPendingMessage(_msg_id, pendingMessage);

                if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                    MeshLog.i("sendMessage Wifi user");
                    wifiTransPort.sendAppMessage(_msg_id, routingEntity.getIp(), message);
                } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                    MeshLog.i("sendMessage ble user");
                    if (isFile) {
                        bleTransport.addFileMessageInQueue(routingEntity.getAddress(), _msg_id, message);
                        /*boolean isSuccess = bluetoothTransport.sendMeshMessage(routingEntity.getAddress(), message);
                        if (!isSuccess){
                            bleTransport.sendFileMessage(routingEntity.getAddress(), message);
                        }*/
                    } else {
                        bleTransport.addAppMessageInQueue(_msg_id, routingEntity.getAddress(), message);
                    }

                } else if (routingEntity.getType() == RoutingEntity.Type.HB && !TextUtils.isEmpty(routingEntity.getIp())) {
                    //adHocTransport.sendAdhocMessage(routingEntity.getIp(), message);
                } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                    /*MeshLog.i("sendMessage BT user");
                    Link bleLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                    if (bleLink != null) {
                        messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) bleLink, message));
                        //sendEventQueue.execute(() -> bleLink.sendMeshMessage(message));
                    } else {
                        MeshLog.i("(!) BLE LINK NOT FOUND");
                    }*/
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
        MeshLog.e("processOutGoingAfterPaymentComplete:" + owner + " " + sender);
        if (success) {
            byte[] messageData = null;
            if (isFile) {
                messageData = JsonDataBuilder.buildBuyerFileMessage(sender, owner, msgData.getBytes(), Constant.FileMessageType.FILE_PACKET_MESSAGE, msg_id);
            } else {
                messageData = JsonDataBuilder.buildMessage(sender, owner, msg_id, msgData.getBytes());
            }

            //Save pending message in cache for multi-path try
            RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(owner);
            List<RoutingEntity> otherReachablePath = connectionLinkCache.filterShortestPathEntity(routingEntity, owner);
            PendingMessage pendingMessage = new PendingMessage(msg_id, sender, owner, sender, messageData, routingEntity);
            pendingMessage.routeQueue.addAll(otherReachablePath);
            connectionLinkCache.addPendingMessage(msg_id, pendingMessage);

            if (!TextUtils.isEmpty(owner) && RouteManager.getInstance().isDirectlyConnected(owner)) {
                sendAppMessage(msg_id, owner, messageData);
            } else {
                RoutingEntity remoteUser = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(owner);
                if (remoteUser != null) {
                    sendAppMessage(msg_id, remoteUser.getAddress(), messageData);
                } else {
                    MeshLog.e("Routing Entity NULL");
                }
            }
        }
    }

    public void processInternetOutgoingMessage(String sender, String receiver, String messageId, byte[] data, boolean isFile, int fileMessageType) {
        MeshLog.v("Message Queuing Start for outgoing");
        long bytesize = 0;
        if (isFile) {
            long messageSize = JsonDataBuilder.buildBuyerFileMessage(sender, receiver, data, fileMessageType, messageId).length;
            bytesize = messageSize + Constant.FILE_MESSAGE_ACK_SIZE;
        } else {
            long messageSize = JsonDataBuilder.buildMessage(sender, receiver, messageId, data).length;
            MeshLog.v("Message Byte Size outgoing (dummy) " + messageSize);
            bytesize = messageSize + Constant.MESSAGE_ACK_SIZE;
        }
        mLinkStateListener.buyerInternetMessageReceived(sender, receiver, messageId, new String(data), bytesize, false, isFile);
    }

    public void sendBuyerReceivedAck(String sender, String receiver, String messageId, int status, String ackBody) {
        if (RouteManager.getInstance().isDirectlyConnected(receiver)) {
            sendMessage(receiver, JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody));
        } else {
            RoutingEntity remoteUser = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            if (remoteUser != null) {
                sendMessage(remoteUser.getAddress(), JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody));
            } else {
                MeshLog.v("MRemote transport remoteUser not found");
            }

        }

    }

    @Override
    public void onSuccess() {
        MeshLog.v("Tunnel Service Success");
    }

    @Override
    public void onFail() {
        MeshLog.v("Tunnel Service Fail");
    }

    private RoutingEntity verifySender(String senderInfo) {
        RoutingEntity sender = GsonUtil.on().getEntityFromJson(senderInfo);
        if (sender == null || sender.getAddress() == null) return null;
        if (sender.getAddress().equals(myNodeId)) return null;
        return sender;
    }

    private void removeOfflineEntitiesFromUiANDdb(List<RoutingEntity> offlineEntities) {
        for (RoutingEntity routingEntity : offlineEntities) {
            //UI callback
            MeshLog.w("Disconnected INTERNET USER:: ->>" + AddressUtil.makeShortAddress(
                    routingEntity.getAddress()));
            mLinkStateListener.onUserDisconnected(routingEntity.getAddress());
            mLinkStateListener.onProbableSellerDisconnected(routingEntity.getAddress());

            /*MeshLog.e("INTERNET user disconnect; Node info removed ");
            connectionLinkCache.removeNodeInfo(routingEntity.getAddress());

            connectionLinkCache.removeDirectLink(routingEntity.getAddress());
            RouteManager.getInstance().updateNodeAsOffline("", routingEntity.getAddress());*/

        }
    }

    /**
     * Update RoutingAndUI
     *
     * @param nodeInfo target details
     */
   /* private void updateRoutingAndUI(NodeInfo nodeInfo) {
        //Routing table update
        MeshLog.e("updateRoutingAndUI 1 " + nodeInfo.getUserId());
        RoutingEntity routingEntity = new RoutingEntity(nodeInfo.getUserId());
        routingEntity.setOnline(true);
        routingEntity.setHopAddress(null);
        routingEntity.setIp(null);
        routingEntity.setType(RoutingEntity.Type.INTERNET);
        routingEntity.setNetworkName(nodeInfo.getBleName());
        boolean updated = RouteManager.getInstance().updateRoute(routingEntity);

        if (updated && RouteManager.getInstance().isOnline(nodeInfo.getUserId())) {
            addNodeInformation(nodeInfo.getUserId(), nodeInfo);
            mLinkStateListener.onRemoteUserConnected(nodeInfo.getUserId(), nodeInfo.getPublicKey());
            MeshLog.i("Routing table updated for:: " + AddressUtil.makeShortAddress(routingEntity.getAddress()));
        } else {
            MeshLog.v("Routing table not updated ");
        }
    }*/

    /**
     * Update Routing TableAndUI
     *
     * @param nodeInfoList nodeInfo list
     */
    private void updateRoutingTableAndUI(ConcurrentLinkedQueue<RoutingEntity> nodeInfoList) {
        if (CollectionUtil.hasItem(nodeInfoList)) {
            for (RoutingEntity entity : nodeInfoList) {

                boolean isUpdated = updateRouteWithCurrentTime(entity);

                if (isUpdated && RouteManager.getInstance().isOnline(entity.getAddress())) {
//                    addNodeInformation(entity.getAddress(), entity);
                    HandlerUtil.postBackground(() -> mLinkStateListener.onRemoteUserConnected(entity.getAddress(), entity.getPublicKey()), 2000L);


                    MeshLog.i("Routing table updated for:" + AddressUtil.makeShortAddress(entity.getAddress()));
                } else {
                    MeshLog.i("Could not update routing for:" + AddressUtil.makeShortAddress(entity.getAddress()));
                }

            }
        }

    }

    private void sendResponseToTarget(RoutingEntity sender) {
        String data = JsonDataBuilder.prepareRemoteHelloPacketResponse(myNodeId, GsonUtil.getUserInfo());
//        BaseMeshMessage baseMeshMessage = MessageBuilder.buildMeshRemoteMessage(mRemoteManager, sender.getUserId(), data.getBytes());
//        baseMeshMessage.mMaxRetryCount = MAX_TRY;
//        messageDispatcher.addSendMessage(baseMeshMessage);
        postMessage(sender.getAddress(), data.getBytes());
    }

    public void sendMessageAck(String sender, String receiver, String messageId, int status, String ackBody, String immediateSender) {

        MeshLog.e(" remote transport sendMessageAck");
        byte[] ackMessage = JsonDataBuilder.buildAckMessage(sender, receiver, messageId, status, ackBody);

        RoutingEntity routingEntity = RouteManager.getInstance().getShortestPath(immediateSender);
        if (routingEntity == null) {
            MeshLog.v("RemoteTansport Rounting entity nul");
            routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(receiver);
            //MeshLog.v("RemoteTansport other end internet user: " + routingEntity.getAddress());
        }

        if (routingEntity != null) {
            MeshLog.v("RemoteTansport other end internet user: " + routingEntity.getAddress());
            if (routingEntity.getType() == RoutingEntity.Type.WiFi && !TextUtils.isEmpty(routingEntity.getIp())) {
                //sendMeshMessage(routingEntity.getIp(), ackMessage);
                wifiTransPort.sendMeshMessage(routingEntity.getIp(), ackMessage);
            } else if (routingEntity.getType() == RoutingEntity.Type.BLE) {
                MeshLog.e("[Wifi] BLE LINK NOT FOUND IN MSG ACK");
                bleTransport.sendMessage(routingEntity.getAddress(), ackMessage);
            } else if (routingEntity.getType() == RoutingEntity.Type.BT) {
                Link directLink = connectionLinkCache.getDirectConnectedBtLink(routingEntity.getAddress());
                if (directLink == null) {
                    MeshLog.e("[Wifi] BT LINK NOT FOUND IN MSG ACK");
                    return;
                }
                messageDispatcher.addSendMessage(MessageBuilder.buildMeshBtMessage((BleLink) directLink, ackMessage));
            } else if (routingEntity.getType() == RoutingEntity.Type.INTERNET) {
                MeshLog.e("sendMsgACK");
//                BaseMeshMessage baseMeshMessage = MessageBuilder.buildMeshRemoteMessage(mRemoteManager, routingEntity.getAddress(), ackMessage);
//                baseMeshMessage.mMaxRetryCount = MAX_TRY;
//                messageDispatcher.addSendMessage(baseMeshMessage);
                sendAppMessage(routingEntity.getAddress(), ackMessage);
            }
        } else {
            MeshLog.e("[Wifi] send Message Ack failed Routing entity null ");
            makeInternetUserOnline(receiver, immediateSender);
            sendMessageAck(sender, receiver, messageId, status, ackBody, immediateSender);
        }
    }

    public void makeInternetUserOnline(String userId, String immediateSender) {
        MeshLog.e("[Remote] makeInternetUserOnline userId " + userId + " immediateSender " + immediateSender);
        if (!TextUtils.isEmpty(immediateSender)) {
            RoutingEntity entity;
            if (immediateSender.equals(userId)) {
                entity = new RoutingEntity(immediateSender);
                entity.setHopCount(0);
            } else {
                entity = new RoutingEntity(userId);
                entity.setHopCount(1);
                entity.setHopAddress(immediateSender);
            }

            entity.setType(RoutingEntity.Type.INTERNET);
            entity.setUserMode(PreferencesHelper.INTERNET_USER);
            entity.setOnline(true);

            ConcurrentLinkedQueue<RoutingEntity> immediateList = new ConcurrentLinkedQueue<>();
            immediateList.add(entity);

            // notify UI
            updateRoutingTableAndUI(immediateList);

            // send to buyer
            passTheseUserToConnectedBuyers(immediateList);

        } else {
            MeshLog.e("No immediate sender found");
        }
    }

    public int sendMeshMessage(String ip, byte[] data) {
        return messageDispatcher.addSendMessage(MessageBuilder.buildMeshWiFiMessage(ip, data));
    }

    /**
     * Update Route With CurrentTime
     *
     * @param routingEntity source data
     * @return update status
     */
    private boolean updateRouteWithCurrentTime(RoutingEntity routingEntity) {

        // Here we will check that this user already connected in Internet or not.
        // If connected we don't need to insert again. Otherwise it will create
        // duplicate route with same path

        // For node who has hop address we have to update hop count.

        // If the hop address is different we will insert  duplicate route


        RoutingEntity oldRoute = RouteManager.getInstance().getEntityByDestinationAndHop(routingEntity.getAddress(), routingEntity.getHopAddress());

        if (oldRoute != null && oldRoute.isOnline()) {
            oldRoute.setHopCount(routingEntity.getHopCount());
            oldRoute.setHopAddress(routingEntity.getHopAddress());
            return RouteManager.getInstance().replaceRoute(oldRoute);
        }

        RoutingEntity oldRoutingEntity = RouteManager.getInstance().getSingleUserInfoByType(routingEntity.getAddress(), RoutingEntity.Type.INTERNET);
        if (oldRoutingEntity != null && oldRoutingEntity.isOnline()) {

            if (!TextUtils.isEmpty(routingEntity.getHopAddress())) {
                return RouteManager.getInstance().insertRoute(routingEntity);
            } else if (!TextUtils.isEmpty(oldRoutingEntity.getHopAddress())) {
                return RouteManager.getInstance().insertRoute(routingEntity);
            } else {
                return true;
            }
        } else {
            return RouteManager.getInstance().insertRoute(routingEntity);
        }
    }

    /**
     * Add Node Information
     *
     * @param userId   user ID
     * @param nodeInfo User Info
     */
    private void addNodeInformation(String userId, NodeInfo nodeInfo) {
        if (!TextUtils.isEmpty(userId)) {
            ConnectionLinkCache.getInstance().addNodeInfo(userId, nodeInfo);
        } else {
            MeshLog.e("UserId empty in add info");
        }

    }

    public void sendMessage(String receiverId, byte[] messageData) {
        MeshLog.e("sendMessage ");
//        BaseMeshMessage meshMessage = MessageBuilder.buildMeshRemoteMessage(mRemoteManager, receiverId, messageData);
//        meshMessage.mMaxRetryCount = MAX_TRY;
//        return messageDispatcher.addSendMessage(meshMessage);
        postMessage(receiverId, messageData);
    }

    private void initialRequestToSshServer() {
        HandlerUtil.postBackground(() -> startTunnelService(myNodeId), 3000);
    }

    /*@Override
    public void onMobileDataChecked(boolean isActivated) {
        if (isActivated) {
            MeshLog.e("Mobile data is  available, tunneling is starting");
            HandlerUtil.postForeground(() -> Toaster.showLong("Mobile data is  available, tunneling is starting"));
            if (TunnelConnectionStatus.isConnectionStatus() == TunnelConstant.DISCONNECTED) {
                MeshLog.e(" 1 start connecting from onMobileDataChecked ");
                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                telemeshTunnel.startTunnel("onMobileDataChecked ");
            }
        } else {
            MeshLog.e("Mobile data is not available, tunneling is not possible");
            HandlerUtil.postForeground(() -> Toaster.showLong("Mobile data is not available, Please enable mobile data for tunneling"));
        }
    }*/

    /*@Override
    public void onNetWorkStateChanged(boolean isConnected, boolean isWifi) {
        MeshLog.ssh("onNetWorkStateChanged:: IsConnected:" + isConnected + "  Is WIfi:" + isWifi);
        if (isConnected) {
            if (isWifi) {
                if (NetworkOperationHelper.isConnectedToInternet(mContext)) {
                    if (!isTunnelingOngoing) {
                        if (TunnelConnectionStatus.isConnectionStatus() == TunnelConstant.DISCONNECTED) {
                            MeshLog.ssh(" Connecting from onNetWorkStateChanged");
                            TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                            HandlerUtil.postBackground(() -> telemeshTunnel.startTunnel("onNetWorkStateChanged 1"), 8000);
                            sshStartflag = 1;
                        }
                    }
                } else {
                    MeshLog.ssh("Wifi data has no internet");
                    TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                    telemeshTunnel.stopTunnel();
                    sshStartflag = 0;
                    leaveAllInternetUserFromDbUi();
                }
            } else {
                if (NetworkOperationHelper.isConnectedToInternet(mContext)) {
                    if (!isTunnelingOngoing) {
                        if (TunnelConnectionStatus.isConnectionStatus() == TunnelConstant.DISCONNECTED) {
                            MeshLog.ssh(" 3 Connecting from onNetWorkStateChanged 1");
                            TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                            telemeshTunnel.startTunnel("onNetWorkStateChanged 2");
                            sshStartflag = 1;
                        }
                    }
                } else {
                    MeshLog.ssh("Mobile data has no internet");
                    TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                    telemeshTunnel.stopTunnel();
                    sshStartflag = 0;
                    leaveAllInternetUserFromDbUi();
                }
            }
        } else {
            if (TunnelConnectionStatus.isSshConnected()) {
                MeshLog.ssh("Disconnected for WiFi Adapter off");
                telemeshTunnel.stopTunnel();
                sshStartflag = 0;
                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
                leaveAllInternetUserFromDbUi();
            }
        }
    }*/

    @Override
    public void onTunnelStatusUpdated(boolean isTunnelingWorking) {
        isTunnelingOngoing = isTunnelingWorking;
        if (isTunnelingWorking) {
            MeshLog.ssh("SSH Tunnel has connected");
            TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTED);
            TunnelConnectionStatus.setSshTunnelConnected(true);

        } else {
            MeshLog.ssh("Disconnected from Tunneling");
            TunnelConnectionStatus.setConnectionStatus(TunnelConstant.DISCONNECTED);
        }
    }

    @Override
    public void onLog(String message) {
    }


    @Override
    public void onReTryCompletedCallbackListener(boolean status) {
        if (status) {
            MeshLog.ssh("Internet Connection Available!! But Failed to connect");
            leaveAllInternetUserFromDbUi();
        }
    }

    private void leaveAllInternetUserFromDbUi() {

        int userMode = PreferencesHelper.on().getDataShareMode();

        if (!TunnelConnectionStatus.isSshConnected())
            return;
        TunnelConnectionStatus.setSshTunnelConnected(false);

        List<RoutingEntity> offlineEntities = RouteManager.getInstance().getInternetUsers();

        // We first make the offline all user
        RouteManager.getInstance().makeUserOffline(offlineEntities);

        if (offlineEntities != null && offlineEntities.size() > 0) {
            if (CollectionUtil.hasItem(offlineEntities)) {
                removeOfflineEntitiesFromUiANDdb(offlineEntities);
            }
            if (userMode == PreferencesHelper.DATA_SELLER) {
                List<String> leftNodeList = makeLeaveList(offlineEntities);
                if (CollectionUtil.hasItem(leftNodeList)) {
                    forwardLeaveMessageToBuyers(leftNodeList);
                }
            }
        }
    }

    public void sendBuyersOffline() {
        ConcurrentLinkedQueue<String> buyerQueue = ConnectionLinkCache.getInstance().getInternetBuyerList();
        String idList = TextUtils.join(",", buyerQueue);
        MeshLog.v("sendBuyersOffline " + idList);

        onBuyerDisconnected(idList);

    }

//    @Override
//    public void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi) {
//
//    }
}