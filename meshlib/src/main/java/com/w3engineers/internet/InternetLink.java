/*
package com.w3engineers.internet;

import android.content.Context;
import android.net.Network;
import android.text.TextUtils;
import android.widget.Toast;


import com.w3engineers.internet.webrtc.PeerConnectionHelper;
import com.w3engineers.internet.webrtc.PeerConnectionHolder;
import com.w3engineers.internet.webrtc.SimpleSdpObserver;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.mesh.wifi.protocol.Link;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.OkHttpClient;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

public class InternetLink implements Link {
    private final String TAG = "InternetMsg ";
    private Context mContext;
    private String mNodeId;
    //  private byte[] mNodeInfo;
    private Socket socket;
    private boolean usingAdhocInternet;
    private long TIME_INTERVAL = 6000;
    private int RECONNECTION_ATTEMPTS = 3;

    private String EMIT_CONNECT = "connect";
    String EMIT_SUCCESS = "success";
    private String EMIT_FAIL = "failed";
    private String EMIT_DISCONNECT = "disconnect";
    String EMIT_USER_LIST = "user_list";
    private String EMIT_SINGLE_USER = "single_user";
    private String EMIT_BUYER_RCV = "buyer_received";

    private String HANDSHAKING_MESSAGE = "message";
    private String CREATE_OR_JOIN = "create or join";


    public static String APP_NAME = "telemesh";
    private boolean isCLoseCall;

    private static ConnectionStateListener connectionStateListener;


    private Network mNetwork;
    private static MessageDispatcher messageDispatcher;

    private static InternetLink sInstance;
    private ConnectionLinkCache mConnectionLinkCache;
    private boolean isWifiDataAvailable;
    private InternetTransport internetTransport;

//    private String myUserInfo;

    public InternetLink(Context context, String nodeId, ConnectionStateListener connectionStateListener,
                        MessageDispatcher messageDispatcher, ConnectionLinkCache connectionLinkCache, InternetTransport internetTransport, boolean isWifiNetwork) {
        this.mContext = context;
        this.mNodeId = nodeId;
        this.messageDispatcher = messageDispatcher;

        this.connectionStateListener = connectionStateListener;
        this.mConnectionLinkCache = connectionLinkCache;
        this.internetTransport = internetTransport;
        sInstance = this;

        //prepareNetworkInterface(isWifiNetwork);
        isWifiDataAvailable = isWifiNetwork;
        if (isWifiNetwork) {
            mNetwork = WiFiUtil.getConnectedWiFiNetwork(mContext);

            // Check already have connection or not
            if (!PeerConnectionHolder.getConnectionHolderList().isEmpty()) {
                PeerConnectionHolder.closeAllDataChannel();
                // PeerConnectionHolder.getConnectionHolderList().clear();
            }

            HandlerUtil.postForeground(new Runnable() {
                @Override
                public void run() {
                    initSocket();
                }
            }, TIME_INTERVAL);

        } else {
            */
/*CellularDataNetworkUtil.on(mContext, new CellularDataNetworkUtil.CellularDataNetworkListenerForPurchase() {
                @Override
                public void onAvailable(Network network1) {
                    MeshLog.v(TAG + " Mobile Data onAvailable " + network1.toString());
                    //if (!usingAdhocInternet){
                    mNetwork = network1;
                    HandlerUtil.postForeground(new Runnable() {
                        @Override
                        public void run() {
                            initSocket();
                        }
                    }, TIME_INTERVAL);
                    // }
                }

                @Override
                public void onLost() {
                    if (mNetwork != null) {
                        MeshLog.v(TAG + "Mobile data onLost: " + mNetwork.toString());
                    }
                    //if (!usingAdhocInternet) {
                    mNetwork = null;
                    //}
                }
            }).initMobileDataNetworkRequest();*//*


        }
    }


    private void initSocket() {
        try {
            if (!isWifiDataAvailable && mNetwork == null) {
                return;
            }
            isCLoseCall = false;
            MeshLog.v(TAG + "Init socket io configuration isWifiDataAvailable: " + isWifiDataAvailable);

            String SOCKET_URL = SharedPref.read(Constant.KEY_MULTIVERSE_URL);

            MeshLog.v(TAG + "socket url :: " + SOCKET_URL);

            OkHttpClient okHttpClient;
            if (isWifiDataAvailable) {
                okHttpClient = new OkHttpClient.Builder()
                        .build();
            } else {
                okHttpClient = new OkHttpClient.Builder()
                        // .socketFactory(mNetwork.getSocketFactory())
                        .build();
            }

            // set as an option
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            //opts.reconnectionAttempts = RECONNECTION_ATTEMPTS;
            opts.reconnectionDelay = TIME_INTERVAL;
            opts.timeout = TIME_INTERVAL;
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            opts.transports = new String[]{WebSocket.NAME};
            socket = IO.socket(SOCKET_URL, opts);

            socket.on(EMIT_FAIL, onFail);
            socket.on(EMIT_CONNECT, onConnect);
            socket.on(EMIT_DISCONNECT, onDisconnect);
            // socket.on(EMIT_USER_LIST, onGetUserList);

            // Handshaking , Offer, Answer, SDP candidate
            socket.on(HANDSHAKING_MESSAGE, handShaking);
            socket.on(EMIT_SINGLE_USER, onGetSingleUser);

            socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    MeshLog.e(TAG + " Socket EVENT_ERROR: " + args[0].toString());
                }
            });

            socket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        MeshLog.e(TAG + " Socket EVENT_CONNECT_TIMEOUT: " + args[0].toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });

            socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        MeshLog.e(TAG + " Socket EVENT_CONNECTING: " + args.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });

            socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        int attempt = Integer.parseInt(args[0].toString());
                        if (attempt == RECONNECTION_ATTEMPTS) {
                            HandlerUtil.postForeground(() -> {
                                if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER
                                        || PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_SELLER) {
                                    Toast.makeText(mContext, "Please check your internet connection ", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        MeshLog.e(TAG + " Socket EVENT_RECONNECT_ATTEMPT: " + args[0].toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        MeshLog.e(TAG + " Socket EVENT_CONNECT_ERROR: " + args[0].toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            socket.connect();

        } catch (URISyntaxException e) {
            MeshLog.v(TAG + " socket init exception: " + e.getMessage());
            e.printStackTrace();
        }
        MeshLog.k(TAG + " socket register success :: " + mNodeId);

    }

    public void closeSocket() {
        if (socket != null) {

            isCLoseCall = true;
            MeshLog.e(TAG + " socket function closing");

            socket.disconnect();
            socket.close();

            socket.off(EMIT_FAIL, onFail);
            socket.off(EMIT_CONNECT, onConnect);
            socket.off(EMIT_DISCONNECT, onDisconnect);
            //socket.off(EMIT_USER_LIST, onGetUserList);

            socket.off(HANDSHAKING_MESSAGE, handShaking);
            socket.off(EMIT_SINGLE_USER, onGetSingleUser);

        }
    }


    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            MeshLog.e(TAG + " socket disconnected \n" + "Message:: " + args[0].toString());
            if (socket != null) {
                MeshLog.e(TAG + " socket id on Disconnect: " + socket.id() + " :: for user" + mNodeId + " :: cause::" + args[0]);
            }

            if (socket != null) {
                String socketId = socket.id();
                MeshLog.e(TAG + " Socket id on disconnect:: " + socketId);

                if (socketId == null) {
                    //socket.close();

                    // connectionStateListener.onMeshLinkDisconnect(mNodeId, "");
                    MeshLog.e(TAG + " connection call again from onFail for::  " + mNodeId);
                }
            }

            if (args[0].toString().contains("ping")) {
                MeshLog.v(TAG + " Ping error occurred");
                HandlerUtil.postBackground(new Runnable() {
                    @Override
                    public void run() {
                        MeshLog.v(TAG + " Reconnection attempt after ping time out");
                        socket.close();
                        initSocket();
                    }
                }, TIME_INTERVAL);
            } else {
                if (!isCLoseCall) {
                    HandlerUtil.postBackground(() -> {
                        MeshLog.v("[Internet] Reconnection attempt for disconnect");
                        socket.close();
                        initSocket();
                    }, TIME_INTERVAL);
                }
            }

        }
    };

    private Emitter.Listener onFail = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            //TODO provide forwarder id
            //  MeshLog.mm("Internet socket onFail");

            MeshLog.e(TAG + "Socket onFail call");
            if (socket != null) {
                MeshLog.e(TAG + " socket id on onFail: " + socket.id() + " :: for user" + mNodeId + " :: cause::" + args[0]);
            }

            if (socket != null) {
                String socketId = socket.id();
                MeshLog.e(TAG + " Socket id on disconnect:: " + socketId);

                if (socketId == null) {
                    //socket.close();

                    //connectionStateListener.onMeshLinkDisconnect(mNodeId, "");
                    MeshLog.e(TAG + " connection call again from onFail for::  " + mNodeId);
                }
            }
        }
    };


    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            MeshLog.i(TAG + " Internet socket Connected");
            //   isConnected = true;
            socket.emit(CREATE_OR_JOIN, prepareJoinEvent());

        }
    };


    private Emitter.Listener onGetUserList = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MeshLog.i("[Internet] **InternetTransportLink ");
            //processServerData(String.valueOf(args[0]), Constant.DataType.USER_LIST);
            String message = String.valueOf(args[0]);
        }
    };

    private Emitter.Listener onGetSingleUser = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String userMessage = String.valueOf(args[0]);

            // Here User will create offer. And received id user will accept answer
            MeshLog.v(TAG + "Single user: " + userMessage);
            prepareOffer(userMessage);
        }
    };

    */
/**
     * To make internet response synchronous
     *
     * @param dataInfo
     * @param type
     * @param mSenderId
     * @param mReceiverId
     *//*


    public void processServerData(String dataInfo, int type, String mSenderId, String mReceiverId) {
        switch (type) {
            case Constant.DataType.USER_LIST:
                ConcurrentLinkedQueue<NodeInfo> userList = GsonUtil.on().queueFromJson(dataInfo);
//                connectionStateListener.onInternetUserReceived(mSenderId, userList, InternetLink.this);
                break;


            case Constant.DataType.DIRECT_USER:
                ConcurrentLinkedQueue<NodeInfo> directUser = GsonUtil.on().queueFromJson(dataInfo);

                connectionStateListener.onInternetDirectUserReceived(mNodeId, directUser, InternetLink.this);
                break;

            case Constant.DataType.USER_MESSAGE:
                try {
                    MeshLog.mm("[Internet] Internet User info receive ++");

                    JSONObject data = new JSONObject(dataInfo);
                    String transactionId = data.getString("txn");

                    connectionStateListener.onMessageReceived(mSenderId, mReceiverId, transactionId, dataInfo.getBytes(), "","");
                } catch (Exception t) {
                    MeshLog.mm(TAG + "Could not parse malformed JSON: " + t.getMessage());
                }

                break;
            case Constant.DataType.ACK_MESSAGE:
                try {

                    MeshLog.v(TAG + "ACK found: " + dataInfo);

                    JSONObject data = new JSONObject(dataInfo);
                    int status = data.optInt(Constant.JsonKeys.STATUS);
                    String messageId = data.optString(Constant.JsonKeys.MSG_ID);

                    connectionStateListener.onReceiveMessageStatus(mSenderId, mReceiverId, messageId, status);
                } catch (Exception t) {
                    MeshLog.mm(TAG + "Could not parse malformed JSON: " + t.getMessage());
                }
                break;
            case Constant.DataType.LEAVE_MESSAGE:
                // here we get actual leave id. And we will call listener
                try {
                    JSONObject jsonObject = new JSONObject(dataInfo);
                    String leaveUserId = jsonObject.optString(Constant.JsonKeys.USER_ID);
                    String hopId = jsonObject.optString(Constant.JsonKeys.HOP_ID);
                    connectionStateListener.onMeshLinkDisconnect(leaveUserId, hopId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

        }
    }


    private Emitter.Listener handShaking = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try {
                if (args[0] instanceof String) {
                    String message = (String) args[0];
                    if (message.equals("got user media")) {
                        MeshLog.v(TAG + "got user media so may be start called");
                        //maybeStart();
                    }
                } else {
                    JSONObject message = (JSONObject) args[0];
                    MeshLog.v(TAG + "connectToSignallingServer: got message " + message);

                    String userId = message.getString(Constant.JsonKeys.SELF_ETH_ID); // Actually it is sender User ID

                    if (TextUtils.isEmpty(userId)) {
                        MeshLog.v(TAG + "Send id empty");
                        return;
                    }

                    MeshLog.v(TAG + "User Id: " + userId);

                    switch (message.getString(Constant.JsonKeys.TYPE)) {
                        case Constant.SDPEvent.OFFER:

                            PeerConnectionHelper connectionHelperForOffer = PeerConnectionHolder.getPeerConnection(userId);
                            if (connectionHelperForOffer == null) {
                                MeshLog.e(TAG + " connection helper not found. Created one");
                                connectionHelperForOffer = new PeerConnectionHelper(userId, mContext);
                                PeerConnectionHolder.addPeerConnection(userId, connectionHelperForOffer);
                            } else {
                                // for answer end check the connection has already created but not established
                                if (!connectionHelperForOffer.isConnected()) {
                                    MeshLog.e(TAG + " Webrtc incomplete connection. Receiving new offer");

                                    connectionHelperForOffer.getPeerConnection().close();

                                    PeerConnectionHolder.removePeerConnection(userId);
                                    connectionHelperForOffer = new PeerConnectionHelper(userId, mContext);
                                    PeerConnectionHolder.addPeerConnection(userId, connectionHelperForOffer);
                                }
                            }

                            connectionHelperForOffer.getPeerConnection().setRemoteDescription(new SimpleSdpObserver(),
                                    new SessionDescription(OFFER, message.getString(Constant.JsonKeys.SDP)));
                            connectionHelperForOffer.doAnswer();
                            //connectionStateListener.onLinkConnected(mNodeId, InternetLink.this, LinkMode.INTERNET);
                            mConnectionLinkCache.addBuyerAddressToList(mNodeId);
                            break;
                        case Constant.SDPEvent.ANSWER:
                            MeshLog.v(TAG + "Answer received");
                            // Here offer creator will receive the answer
                            // So no need to create connection helper again if not exist

                            PeerConnectionHelper connectionHelperForAnswer = PeerConnectionHolder.getPeerConnection(userId);

                            if (connectionHelperForAnswer != null) {
                                connectionHelperForAnswer.getPeerConnection().setRemoteDescription(new SimpleSdpObserver(),
                                        new SessionDescription(ANSWER, message.getString(Constant.JsonKeys.SDP)));

                                mConnectionLinkCache.addBuyerAddressToList(mNodeId);
                            }
                            break;
                        case Constant.SDPEvent.CANDIDATE:
                            PeerConnectionHelper connectionHelperForCandidate = PeerConnectionHolder.getPeerConnection(userId);
                            if (connectionHelperForCandidate != null) {
                                MeshLog.v(TAG + "connectToSignallingServer: receiving candidates");
                                IceCandidate candidate = new IceCandidate(message.getString(Constant.JsonKeys.ID),
                                        message.getInt(Constant.JsonKeys.LABEL), message.getString(Constant.JsonKeys.CANDIDATE));
                                connectionHelperForCandidate.getPeerConnection().addIceCandidate(candidate);
                            }
                            break;
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
                MeshLog.e(TAG + "Hand shaking message parsing error: " + e.getMessage());

            }
        }
    };


    private JSONObject prepareJoinEvent() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Constant.JsonKeys.ROOM, "telemesh");
            jsonObject.put(Constant.JsonKeys.ETH_ID, SharedPref.read(Constant.KEY_USER_ID));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void prepareOffer(String json) {
        try {
            JSONArray jsonArray = new JSONArray(json);
            JSONObject userObject = jsonArray.getJSONObject(0);
            String userId = userObject.optString(Constant.JsonKeys.ETH_ID);

            if (userId.equals(mNodeId)) {
                MeshLog.e(TAG + " prepare offer but SELF user found");
                return;
            }

            PeerConnectionHelper connectionHelper = PeerConnectionHolder.getPeerConnection(userId);

            //Checking the user already local connected or not

            RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(userId);
            if (routingEntity != null && routingEntity.isOnline() && routingEntity.getType() != RoutingEntity.Type.INTERNET) {

                internetTransport.verifyUserExistence(routingEntity, (ip, isReachable) -> {
                    if (!isReachable) {
                        sendOffer(connectionHelper, userId);
                    } else {
                        // Todo we are not notify UI. We ignoring handshaking. May be need to notify UI
                        MeshLog.v(TAG + "already this user locally connected");
                    }
                });
            } else {
                sendOffer(connectionHelper, userId);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendOffer(PeerConnectionHelper connectionHelper, String userId) {
        if (connectionHelper == null) {
            connectionHelper = new PeerConnectionHelper(userId, mContext);
            PeerConnectionHolder.addPeerConnection(userId, connectionHelper);
        } else {

            // check exactly connection established or not
            if (connectionHelper.isConnected()) {
                MeshLog.v(TAG + " Already have connection in Web rtc");
                return;
            } else {
                MeshLog.v(TAG + " Already have connection in Web rtc But incomplete ");
                // remove previous object and creat another one
                //connectionHelper.getPeerConnection().dispose();
                //connectionHelper.getLocalDataChannel().close();

                connectionHelper.getPeerConnection().close();

                PeerConnectionHolder.removePeerConnection(userId);
                connectionHelper = new PeerConnectionHelper(userId, mContext);
                PeerConnectionHolder.addPeerConnection(userId, connectionHelper);
            }

        }

        if (!connectionHelper.isReceiveOffer() && !connectionHelper.isSendOffer()) {
            connectionHelper.doCall();
        }
    }

    public static InternetLink getInstance() {
        return sInstance;
    }

    public static MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }

    */
/**
     * This the handshaking message to establish peer to peer connection
     * in web RTC
     *
     * @param message  JSONObject
     * @param toNodeId receiver Id
     * @param myNodeId my Id
     *//*

    public void sendHandshakingMessage(JSONObject message, String toNodeId, String myNodeId) {
        MeshLog.v(TAG + "sending hand shaking message in socket");
        try {
            message.put(Constant.JsonKeys.TO_ETH_ID, toNodeId);
            message.put(Constant.JsonKeys.SELF_ETH_ID, myNodeId);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (socket != null) {
            socket.emit(HANDSHAKING_MESSAGE, message);
        }
    }

    public void sendAck(String sender, String receiverId, String messageId, int status) {
        try {
            JSONObject mainJsonObject = new JSONObject();

            JSONObject headerObject = new JSONObject();
            headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.ACK_MESSAGE);
            headerObject.put(Constant.JsonKeys.SENDER, sender);
            headerObject.put(Constant.JsonKeys.RECEIVER, receiverId);

            mainJsonObject.put(Constant.JsonKeys.HEADER, headerObject);

            JSONObject messageObject = new JSONObject();
            messageObject.put(Constant.JsonKeys.STATUS, status);
            messageObject.put(Constant.JsonKeys.MSG_ID, messageId);

            mainJsonObject.put(Constant.JsonKeys.MESSAGE, messageObject);

            RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);

            if (route == null) {
                return;
            }

            PeerConnectionHelper connectionHelper;
            if (TextUtils.isEmpty(route.getHopAddress())) {
                connectionHelper = PeerConnectionHolder.getPeerConnection(receiverId);
            } else {
                connectionHelper = PeerConnectionHolder.getPeerConnection(route.getHopAddress());
            }

            if (connectionHelper != null) {

                MeshLog.v(TAG + "Ack size: " + mainJsonObject.toString().getBytes().length);
                boolean isSend = connectionHelper.sendMessage(mainJsonObject.toString());

                MeshLog.i(TAG + "Message is Send: " + isSend);
            } else {
                MeshLog.e(TAG + "Error: connection helper null");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    */
/**
     * This process is simple. When a buyer complete his
     * purchase this seller will send buyer's information to
     * other internet user
     *
     * @param nodeId String buyers node id
     *//*

    public void sendBuyer(String nodeId) {
        HashMap<String, PeerConnectionHelper> connectionList = PeerConnectionHolder.getConnectionHolderList();
        if (!connectionList.isEmpty()) {
            NodeInfo buyerNodeInfo = mConnectionLinkCache.getNodeInfoById(nodeId);
            List<NodeInfo> buyerNode = new ArrayList<>();
            buyerNode.add(buyerNodeInfo);

            for (Map.Entry<String, PeerConnectionHelper> entry : connectionList.entrySet()) {
                String receiverId = entry.getKey();
                PeerConnectionHelper connectionHelper = entry.getValue();
                String userList = prepareBuyerUserList(buyerNode, receiverId);
                if (userList == null || connectionHelper == null) return;
                boolean isSend = connectionHelper.sendMessage(userList);
                MeshLog.i(TAG + " Single buyer send to seller: " + isSend);
            }
        }
    }

    public void sendBuyerDisconnectMessage(String nodeId) {
        HashMap<String, PeerConnectionHelper> connectionList = PeerConnectionHolder.getConnectionHolderList();
        if (!connectionList.isEmpty()) {
            String leaveEvent = prepareLeaveMessage(nodeId);
            for (Map.Entry<String, PeerConnectionHelper> entry : connectionList.entrySet()) {
                String receiverId = entry.getKey();
                PeerConnectionHelper connectionHelper = entry.getValue();
                if (leaveEvent == null || connectionHelper == null) return;
                boolean isSend = connectionHelper.sendMessage(leaveEvent);
                MeshLog.i(TAG + " Leave event send to seller: " + isSend);
            }
        }

    }

    */
/**
     * This method is responsible for sending all self buyer list
     * to other internet user (Seller and Internet only user)
     * when they connect to this Seller
     *//*

    public void sendBuyerList() {
        HashMap<String, PeerConnectionHelper> connectionList = PeerConnectionHolder.getConnectionHolderList();
        if (!connectionList.isEmpty()) {

            List<NodeInfo> nodeInfoList = new ArrayList<>();
            for (String nodeId : mConnectionLinkCache.getInternetBuyerList()) {
                if (!nodeId.equals(mNodeId)) {
                    NodeInfo nodeInfo = mConnectionLinkCache.getNodeInfoById(nodeId);
                    nodeInfoList.add(nodeInfo);
                    MeshLog.i(TAG + " First discover:: Send node buyer info: " + nodeInfo.toString());
                }
            }

            if (!nodeInfoList.isEmpty()) {
                for (Map.Entry<String, PeerConnectionHelper> entry : connectionList.entrySet()) {
                    String receiverId = entry.getKey();
                    PeerConnectionHelper connectionHelper = entry.getValue();
                    String userMessage = prepareBuyerUserList(nodeInfoList, receiverId);
                    if (userMessage == null || connectionHelper == null) return;

                    boolean isSend = connectionHelper.sendMessage(userMessage);

                    MeshLog.i(TAG + " buyer list send for first discover: " + isSend);
                }
            }
        }
    }

    public String getMyNodeInfo() {
        NodeInfo myNodeInfo = JsonDataBuilder.myNodeInfoBuild(mNodeId, RoutingEntity.Type.INTERNET);
        List<NodeInfo> nodeInfoList = new ArrayList<>();
        nodeInfoList.add(myNodeInfo);
        String myNodeString = GsonUtil.on().toJsonFromList(nodeInfoList);

        try {
            JSONObject mainObject = new JSONObject();
            JSONObject headerObject = new JSONObject();
            headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.DIRECT_USER);

            mainObject.put(Constant.JsonKeys.HEADER, headerObject);
            mainObject.put(Constant.JsonKeys.MESSAGE, myNodeString);

            return mainObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String requestNodeInfo() {
        try {
            JSONObject mainObject = new JSONObject();
            JSONObject headerObject = new JSONObject();
            headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.REQUEST_NODE_INFO);
            mainObject.put(Constant.JsonKeys.HEADER, headerObject);
            return mainObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public String getNodeId() {
        return mNodeId;
    }

    @Override
    public void disconnect() {
        //   isConnected = false;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public int sendFrame(String senderId, String receiverId, String messageId, byte[] frameData) {
        if (!senderId.equals(receiverId)) {
            MeshLog.i(TAG + " Internet send frame call ");
            try {
                String message = new String(frameData);

                MeshLog.mm(TAG + "**sendFrame data** :" + frameData + "\nSender" + senderId + "\nreceiver" + receiverId);


                MeshLog.mm(TAG + " Send frame called at InternetTransport =" + message);
                if (!TextUtils.isEmpty(message)) {
                    JSONObject jsonObject = null;

                    jsonObject = new JSONObject();
                    jsonObject.put("text", message);
                    jsonObject.put("txn", messageId);

                    // Assume that connection established already

                    // Here we will check first receiver id has any hop address or not.

                    RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(receiverId);

                    if (route == null) {
                        return BaseMeshMessage.MESSAGE_STATUS_FAILED;
                    }

                    PeerConnectionHelper connectionHelper;
                    if (TextUtils.isEmpty(route.getHopAddress())) {
                        connectionHelper = PeerConnectionHolder.getPeerConnection(receiverId);
                    } else {
                        connectionHelper = PeerConnectionHolder.getPeerConnection(route.getHopAddress());
                    }

                    if (connectionHelper != null) {
                        JSONObject mainObject = new JSONObject();
                        JSONObject headerObject = new JSONObject();
                        headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.USER_MESSAGE);
                        headerObject.put(Constant.JsonKeys.SENDER, senderId);
                        headerObject.put(Constant.JsonKeys.RECEIVER, receiverId);
                        mainObject.put(Constant.JsonKeys.HEADER, headerObject);
                        mainObject.put(Constant.JsonKeys.MESSAGE, jsonObject);


                        MeshLog.v(TAG + " Message Byte Size outgoing (original) " + mainObject.toString().getBytes().length);

                        boolean isSend = connectionHelper.sendMessage(mainObject.toString());

                        MeshLog.i(TAG + " Message is Send: " + isSend);
                    } else {
                        MeshLog.e(TAG + " Error: connection helper null during sending data");
                    }


                    MeshLog.v(TAG + " My send message (sendFrame) ::\n" + "Sender id::" + senderId + "\nReceiver id::" + receiverId);
                    MeshLog.i(TAG + " JSON" + jsonObject.toString());

                }
            } catch (JSONException e) {
                e.printStackTrace();
                return BaseMeshMessage.MESSAGE_STATUS_FAILED;

            }
            return BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
        }
        return BaseMeshMessage.MESSAGE_STATUS_FAILED;
    }


    @Override
    public Type getType() {
        return Type.INTERNET;
    }

    @Override
    public int getUserMode() {
        return 0;
    }


    @Override
    public int sendMeshMessage(byte[] data) {
        return 0;
    }

    public int sendBuyerReceivedAck(String sender, String receiver, String messageId) {
        //this.socket.emit(EMIT_BUYER_RCV_ACK, selfAddress, appName, sender, transactionId);
        try {
            this.socket.emit(EMIT_BUYER_RCV, sender, APP_NAME, receiver, messageId);

            MeshLog.mm(TAG + " Buyer receive ack send :::" + "Buyer Id :" + sender);
            return BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
        } catch (Exception e) {
            return BaseMeshMessage.MESSAGE_STATUS_FAILED;
        }
    }

    public void sendUserInfoThroughInternet(String sender, String receiver, String messageId, byte[] data) {
        try {
            JSONObject mainJsonObject = new JSONObject();
            JSONObject headerObject = new JSONObject();
            headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.USER_INFO_MESSAGE);
            headerObject.put(Constant.JsonKeys.SENDER, sender);
            headerObject.put(Constant.JsonKeys.RECEIVER, receiver);

            mainJsonObject.put(Constant.JsonKeys.HEADER, headerObject);

            JSONObject messageObject = new JSONObject();
            messageObject.put(Constant.JsonKeys.USER_INFO, new String(data));
            messageObject.put(Constant.JsonKeys.MSG_ID, messageId);

            mainJsonObject.put(Constant.JsonKeys.MESSAGE, messageObject);

            RoutingEntity route = RouteManager.getInstance().getRoutingEntityByAddress(receiver);

            if (route == null) {
                return;
            }

            PeerConnectionHelper connectionHelper;
            if (TextUtils.isEmpty(route.getHopAddress())) {
                connectionHelper = PeerConnectionHolder.getPeerConnection(receiver);
            } else {
                connectionHelper = PeerConnectionHolder.getPeerConnection(route.getHopAddress());
            }

            if (connectionHelper != null) {

                boolean isSend = connectionHelper.sendMessage(mainJsonObject.toString());

                MeshLog.i(TAG + " User info send through internet: " + isSend);
            } else {
                MeshLog.e(TAG + " Error: connection helper null during sending user info");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface InternetMessageSubmitListener {
        void onMessageSubmitted(int submissiomId, boolean success);
    }

    public void clearSelfBuyerUserList() {
        mConnectionLinkCache.removeAll();
    }

    private String prepareBuyerUserList(List<NodeInfo> nodeInfoList, String receiverId) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONObject headerObject = new JSONObject();
            headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.USER_LIST);
            headerObject.put(Constant.JsonKeys.SENDER, mNodeId);
            headerObject.put(Constant.JsonKeys.RECEIVER, receiverId);

            String userList = nodeInfoList.isEmpty() ? "" : GsonUtil.on().toJsonFromList(nodeInfoList);

            jsonObject.put(Constant.JsonKeys.HEADER, headerObject);
            jsonObject.put(Constant.JsonKeys.MESSAGE, userList);

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String prepareLeaveMessage(String nodeId) {
        try {
            JSONObject headerObject = new JSONObject();
            headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.LEAVE_MESSAGE);

            JSONObject messageObject = new JSONObject();
            messageObject.put(Constant.JsonKeys.USER_ID, nodeId);
            messageObject.put(Constant.JsonKeys.HOP_ID, mNodeId);

            JSONObject mainObject = new JSONObject();
            mainObject.put(Constant.JsonKeys.HEADER, headerObject);
            mainObject.put(Constant.JsonKeys.MESSAGE, messageObject);

            return mainObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void destroyObject() {
        // sInstance = null;
    }

}
*/
