package com.w3engineers.mesh.httpservices;

import android.content.Context;
import android.net.Network;
import android.util.Log;
import android.util.Pair;

import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.NanoHTTPD;
import com.w3engineers.mesh.queue.DiscoveryTask;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.WiFiUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * NANO http server initializer
 */
public class MeshHttpServer implements NanoHTTPServer.HttpDataListener {

    public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;
    private static MeshHttpServer server;
    private NanoHTTPServer nanoHTTPServer;
    private ExecutorService callableExecutor;
    private ConnectionStateListener linkStateListener;
    private ConnectionStateListener adHocListener;
    private int APP_PORT;
    private Context context;
    private MeshLibMessageEventQueue discoveryEventQueue;
    //private OkHttpClient okHttpClient;
    /**
     * To temporarily store missing discovery data which was paused during server pausing
     */
    private volatile boolean mIsDirectDiscoveryPause;
    private String myUserId;
    private INanoServerInitiator mINanoServerInitiator;

    /**
     * To temporarily store missing discovery data which was paused during server pausing
     */

    private MeshHttpServer() {
        callableExecutor = Executors.newFixedThreadPool(1);
        context = MeshApp.getContext();
        myUserId = SharedPref.read(Constant.KEY_USER_ID);
        //buildOkHttpClient();
    }

    public static MeshHttpServer on() {
        if (server == null) {
            server = new MeshHttpServer();
        }

        return server;
    }

    public void start(int appPort, INanoServerInitiator iNanoServerInitiator) {
        stop();
        this.APP_PORT = appPort;
        this.mINanoServerInitiator = iNanoServerInitiator;

        nanoHTTPServer = iNanoServerInitiator.generateNanoServer(appPort);//new NanoHTTPServer(appPort);
        nanoHTTPServer.setHttpDataListener(this::receivedData);

        //buildOkHttpClient();
    }

    public void setWifiDataListener(MeshLibMessageEventQueue discoveryEventQueue, ConnectionStateListener connectionStateListener) {
        this.discoveryEventQueue = discoveryEventQueue;
        this.linkStateListener = connectionStateListener;

    }

    /*private void buildOkHttpClient() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        //This logic is working fine for now. If any device show a behavior of being GO and a
        //Adhoc or HS Client at the same time then outgoing request of Adhoc or Client might
        //need separate handle
        if (!P2PUtil.isMeGO()) {
            if (!WiFiUtil.isHotSpotEnabled()) {
                //FIXME first decide me go or client while building the packet
                Network network = getNetwork();
                if (network != null) {
                    builder.socketFactory(network.getSocketFactory());
                }
            }
        }
        builder.connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.retryOnConnectionFailure(true);
        client = builder.build();
    }*/


    /**
     * <p>Purpose to build ok http client once time
     * and use it </p>
     */
    /*private void buildOkHttpClient() {
        try {
            KeyStore keyStore = KeyStore.getInstance("BKS");
            InputStream keyStoreStream = MeshApp.getContext().getAssets().open("bks_keystore.bks");
            keyStore.load(keyStoreStream, "password".toCharArray());
            keyStoreStream.close();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "password".toCharArray());
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                    .retryOnConnectionFailure(true)
                    .sslSocketFactory(sslContext.getSocketFactory())
                    .build();
        } catch (IOException e) {
            Log.e("Okhttoclient", "IOException");
        } catch (KeyStoreException e) {
            Log.e("Okhttoclient", "KeyStoreException");
        } catch (NoSuchAlgorithmException e) {
            Log.e("Okhttoclient", "NoSuchAlgorithmException");
        } catch (CertificateException e) {
            Log.e("Okhttoclient", "CertificateException");
        } catch (UnrecoverableKeyException e) {
            Log.e("Okhttoclient", "UnrecoverableKeyException");
        } catch (KeyManagementException e) {
            Log.e("Okhttoclient", "KeyManagementException");
        }

    }*/
    public void setListener(ConnectionStateListener connectionStateListener) {

        this.adHocListener = connectionStateListener;

    }

    public void stop() {
        if (nanoHTTPServer != null) {
            nanoHTTPServer.stop();

        }
        if (mINanoServerInitiator != null) {
            mINanoServerInitiator.stopFileServer();
        }

    }

    private Network getNetwork() {
        return WiFiUtil.getConnectedWiFiNetwork(context);
    }
    //.url("http://" + ip + ":8080/hellopacket?data=" + dataStr)

    public Integer sendMessage(String ip, byte[] data) throws ExecutionException, InterruptedException {
        return sendMessage(ip, data, DEFAULT_CONNECTION_TIMEOUT);
    }

    public Integer sendMessage(String ip, byte[] data, int connectionTimeOutInMillis)
            throws ExecutionException, InterruptedException {
        MeshLog.v("FILE_SPEED_TEST_4 " + Calendar.getInstance().getTime());
        MeshLog.i("MeshHttpServer Send Data To :: " + ip);
        String dataStr = new String(data);//android.util.Base64.encodeToString(data, Base64.DEFAULT);
        RequestBody formBody = new FormBody.Builder()
                .add("data", dataStr)
                .build();
        Future<Integer> future = callableExecutor.submit(() -> {

            OkHttpClient client;
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            //This logic is working fine for now. If any device show a behavior of being GO and a
            //Adhoc or HS Client at the same time then outgoing request of Adhoc or Client might
            //need separate handle
            Network network = getNetwork();
            if (network != null) {
                builder.socketFactory(network.getSocketFactory());
            } else {
                MeshLog.e("Network null not able to set wifi socketfactory");
            }
            /*if (!P2PUtil.isMeGO()) {
                if (!WiFiUtil.isHotSpotEnabled()) {
                    //FIXME first decide me go or client while building the packet
                    builder.socketFactory(getNetwork().getSocketFactory());
                }
            }*/
            builder.connectTimeout(connectionTimeOutInMillis, TimeUnit.MILLISECONDS);
            builder.retryOnConnectionFailure(true);
            client = builder.build();

            int responseCode = 0;
            Request request = new Request.Builder()
                    .url("http://" + ip + ":" + APP_PORT + NanoHTTPD.URI_TEXT)
                    .post(formBody)
                    .addHeader("cache-control", "no-cache")
                    .addHeader("Connection", "close")
                    .addHeader("id", myUserId)
//                    .addHeader("connection", "keep-alive")
                    .build();

            try {

                Response response = client.newCall(request).execute();
                if (response != null) {
                    responseCode = response.code() == 200 ? 1 : 0;
                    MeshLog.i("http response: " + response.body().string());
                }
            } catch (IOException e) {
                MeshLog.e("[MeshHttpServer] data send error:: " + e.getMessage() + " ip:" + ip + " port:" + APP_PORT);
                //e.printStackTrace();
                responseCode = sendMessageInRawSocket(ip, data);

            }
            return responseCode;
        });

        return future.get();
    }


    private int sendMessageInRawSocket(String ip, byte[] data) {

        int newTcpPort = APP_PORT - 2;
        Socket socket = null;
        try {
            socket = new Socket();

            InetAddress addr = InetAddress.getByName(ip);
            SocketAddress sockaddr = new InetSocketAddress(addr, newTcpPort);
            socket.connect(sockaddr, 8000);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.write(data);
            dos.flush();
            dos.close();
            socket.close();
            MeshLog.e("[MeshHttpServer] data send success in Raw socket::");
            return 1;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            MeshLog.e("[MeshHttpServer] data send failed in Raw socket::");
        } finally {
            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }

    public void parseReceivedData(JSONObject jo, String ipAddress) {
        if (jo == null) return;
        boolean isWifiP2p = ipAddress.contains(".49.");
        int type = jo.optInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
        String buddy = jo.optString(JsonDataBuilder.KEY_BUDDY_ID);
        MeshLog.e("[BLE_PROCESS] WiFi received message :" + jo.toString());
        switch (type) {

            // Mesh v2 message
            case JsonDataBuilder.HELLO_MASTER:
                String senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                String onlineNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                String offLineNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                String dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                linkStateListener.onV2ReceivedHelloFromClient(senderInfo, onlineNodes, offLineNodes, dataId);
                break;
            case JsonDataBuilder.HELLO_CLIENT:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                String lcOnlineNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_DIRECT_NODES);
                String otherOnlineNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                String offlineNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                linkStateListener.onV2ReceivedHelloFromMaster(senderInfo, lcOnlineNodes, otherOnlineNodes, offlineNodes, dataId);
                break;

            case JsonDataBuilder.MESH_NODE:
                String senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String onlineMeshNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                String offlineMeshNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                linkStateListener.onV2ReceivedMeshUsers(senderId, onlineMeshNodes, offlineMeshNodes, dataId);
                break;
            case JsonDataBuilder.CREDENTIAL_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String ssId = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                String password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                String goNodeId = jo.optString(JsonDataBuilder.KEY_GO_ID);
                linkStateListener.onV2CredentialReceived(senderId, receiverId, ssId, password, goNodeId);
                break;

            case JsonDataBuilder.BLE_MESH_DECISION_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String ssid = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                int wifiCount = jo.optInt(JsonDataBuilder.MY_NODE_COUNT);
                boolean isFirst = jo.optBoolean(JsonDataBuilder.IS_SERVER);
                linkStateListener.onV2BleMeshDecisionMessageReceive(senderId, receiverId, ssid,
                        password, wifiCount, isFirst);
                break;

            case JsonDataBuilder.FORCE_CONNECTION_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                ssid = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                boolean isRequest = jo.optBoolean(JsonDataBuilder.IS_FORCE_REQUEST);
                boolean isAbleToReceive = jo.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);
                linkStateListener.onV2ForceConnectionMessage(senderId, receiverId, ssid, password, isRequest, isAbleToReceive);
                break;

            case JsonDataBuilder.FILE_RECEIVE_FREE_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                isAbleToReceive = jo.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);
                linkStateListener.onV2GetFileFreeModeMessage(senderId, receiverId, isAbleToReceive);
                break;

            case JsonDataBuilder.APP_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                String message = jo.optString(JsonDataBuilder.KEY_MESSAGE);

                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onMessageReceived(senderId, receiverId, messageId, message.getBytes(), ipAddress, buddy);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onMessageReceived(senderId, receiverId, messageId, message.getBytes(), ipAddress, buddy);
                    }
                }
                break;

            case JsonDataBuilder.P2P_HELLO_MASTER:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                String btUsers = jo.optString(JsonDataBuilder.KEY_BT_USERS);
                String btMeshUsers = jo.optString(JsonDataBuilder.KEY_BT_MESH_USER);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceivedDirectUserFromWifiClient(senderInfo, btUsers,
                                btMeshUsers);
                    }
                }
                break;
            case JsonDataBuilder.P2P_HELLO_CLIENT:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                btUsers = jo.optString(JsonDataBuilder.KEY_BT_USERS);
                btMeshUsers = jo.optString(JsonDataBuilder.KEY_BT_MESH_USER);
                String wifiUsers = jo.optString(JsonDataBuilder.KEY_WIFI_USERS);
                String wifiMeshUsers = jo.optString(JsonDataBuilder.KEY_WIFI_MESH_USER);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceivedDirectUserFromWifiMaster(senderInfo, btUsers,
                                btMeshUsers, wifiUsers, wifiMeshUsers);
                    }
                }
                break;
            case JsonDataBuilder.ADHOC_HELLO_MASTER:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                btUsers = jo.optString(JsonDataBuilder.KEY_BT_USERS);
                btMeshUsers = jo.optString(JsonDataBuilder.KEY_BT_MESH_USER);
                //wifiUsers = jo.optString(ProtoBuilder.KEY_WIFI_USERS);
                //wifiMeshUsers = jo.optString(ProtoBuilder.KEY_WIFI_MESH_USER);
                if (adHocListener != null) {
                    adHocListener.onReceivedDirectUserFromAdHocListener(senderInfo, btUsers,
                            btMeshUsers);
                }
                break;
            case JsonDataBuilder.ADHOC_HELLO_CLIENT:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                btUsers = jo.optString(JsonDataBuilder.KEY_BT_USERS);
                btMeshUsers = jo.optString(JsonDataBuilder.KEY_BT_MESH_USER);
                wifiUsers = jo.optString(JsonDataBuilder.KEY_WIFI_USERS);
                wifiMeshUsers = jo.optString(JsonDataBuilder.KEY_WIFI_MESH_USER);
                if (adHocListener != null) {
                    adHocListener.onReceivedDirectUserFromAdHocBroadcaster(senderInfo, btUsers,
                            btMeshUsers, wifiUsers, wifiMeshUsers);
                }
                break;
            case JsonDataBuilder.INFO_REQUEST:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                btUsers = jo.optString(JsonDataBuilder.KEY_BT_USERS);
                btMeshUsers = jo.optString(JsonDataBuilder.KEY_BT_MESH_USER);
                if (adHocListener != null) {
                    adHocListener.onGetInfoRequest(senderInfo, btUsers, btMeshUsers);
                }
                break;

            case JsonDataBuilder.ACK_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                String ackBody = jo.optString(JsonDataBuilder.KEY_ACK_BODY);
                int status = jo.optInt(JsonDataBuilder.KEY_ACK_STATUS);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceivedMsgAck(senderId, receiverId, messageId, status, ackBody, buddy);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onReceivedMsgAck(senderId, receiverId, messageId, status, ackBody, buddy);
                    }
                }
                break;
            case JsonDataBuilder.MESH_USER:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String hopNodeId = jo.optString(JsonDataBuilder.KEY_HOP_NODE_ID);
                String meshUser = jo.optString(JsonDataBuilder.KEY_MESH_USERS);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onMeshLinkFound(senderId, hopNodeId, meshUser);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onMeshLinkFound(senderId, hopNodeId, meshUser);
                    }
                }

                break;
            case JsonDataBuilder.NODE_LEAVE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String leaveNodeId = jo.optString(JsonDataBuilder.KEY_LEAVE_NODE_ID);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onMeshLinkDisconnect(leaveNodeId, senderId);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onMeshLinkDisconnect(leaveNodeId, senderId);
                    }
                }
                break;
            case JsonDataBuilder.FAILED_MESSAGE_ACK:
                String source = jo.optString(JsonDataBuilder.KEY_MESSAGE_SOURCE);
                String destination = jo.optString(JsonDataBuilder.KEY_MESSAGE_DESTINATION);
                String hop = jo.optString(JsonDataBuilder.KEY_ACK_SENDER);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                linkStateListener.onV2ReceivedFailedMessageAck(source, destination, hop, messageId);
                break;
            case JsonDataBuilder.INTERNET_USER:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String sellerId = jo.optString(JsonDataBuilder.KEY_SELLER_ID);
                String internetUsers = jo.optString(JsonDataBuilder.KEY_INTERNET_USERS);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceiverInternetUserLocally(senderId, receiverId, sellerId, internetUsers);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onReceiverInternetUserLocally(senderId, receiverId, sellerId, internetUsers);
                    }
                }

                break;
            case JsonDataBuilder.PAYMENT_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                MeshLog.v("Received pay message");
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onPaymentDataReceived(senderId, receiverId, messageId, message.getBytes());
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onPaymentDataReceived(senderId, receiverId, messageId, message.getBytes());
                    }
                }
                break;
            case JsonDataBuilder.PAYMENT_ACK:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onPaymentAckReceived(senderId, receiverId, messageId);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onPaymentAckReceived(senderId, receiverId, messageId);
                    }
                }
                break;
            case JsonDataBuilder.ROUTE_INFO:
                String nodeAddress = jo.optString(JsonDataBuilder.KEY_TARGET_ADDRESS);
                String nodeHopAddress = jo.optString(JsonDataBuilder.KEY_HOP_NODE_ID);
                long time = jo.optLong(JsonDataBuilder.KEY_TIME_STAMP);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceiveRouteInfoUpdate(nodeAddress, nodeHopAddress, time);
                    }
                }

                break;
            case JsonDataBuilder.DISCONNECT_REQUEST:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceiveDisconnectRequest(senderId);
                    }
                }
                break;
            case JsonDataBuilder.INTERNET_USER_LEAVE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String userList = jo.optString(JsonDataBuilder.KEY_USER_LIST);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onInternetUserLeave(senderId, receiverId, userList);
                    }
                }
                break;
            case JsonDataBuilder.HEART_BEAT:
                break;

            case JsonDataBuilder.USER_ROLE_SWITCH:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                int newRole = jo.optInt(JsonDataBuilder.KEY_NEW_ROLE);
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onReceiveNewRole(senderId, receiverId, newRole);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onReceiveNewRole(senderId, receiverId, newRole);
                    }
                }
                break;
            case JsonDataBuilder.APP_FILE_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                if (linkStateListener != null) {
                    linkStateListener.onFileMessageReceived(senderId, receiverId, messageId, message, buddy);
                }
                /*if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onFileMessageReceived(senderId, receiverId, messageId, message);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onFileMessageReceived(senderId, receiverId, messageId, message);
                    }
                }*/
                break;
            case JsonDataBuilder.BUYER_FILE_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                int fileMessageType = jo.optInt(JsonDataBuilder.KEY_BUYER_FILE_ACK);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                linkStateListener.onReceiveBuyerFileMessage(senderId, receiverId, message, fileMessageType, buddy, messageId);
                break;

            case JsonDataBuilder.APP_BROADCAST_MESSAGE:

                Broadcast broadcast = GsonUtil.on().broadcastFromString(jo.toString());

                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onBroadcastReceived(broadcast);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onBroadcastReceived(broadcast);
                    }
                }
                break;

            case JsonDataBuilder.APP_BROADCAST_ACK_MESSAGE:

                BroadcastAck broadcastAck = GsonUtil.on().broadcastAckFromString(jo.toString());

                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onBroadcastACKMessageReceived(broadcastAck);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onBroadcastACKMessageReceived(broadcastAck);
                    }
                }

                break;

            case JsonDataBuilder.GO_NETWORK_FULL:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                linkStateListener.onV2ReceivedGoNetworkFullResponse(senderId);
                break;

            case JsonDataBuilder.SPECIAL_DISCONNECT_MESSAGE:
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String duplicateId = jo.optString(JsonDataBuilder.KEY_DUPLICATE_ID);
                linkStateListener.onV2ReceiveSpecialDisconnectMessage(receiverId, duplicateId);
                break;

            case JsonDataBuilder.HANDSHAKE_PING:
            case JsonDataBuilder.HANDSHAKE_BROADCAST:
            case JsonDataBuilder.HANDSHAKE_INFO:
            case JsonDataBuilder.HANDSHAKE_CONFIG:
                HandshakeInfo handshakeInfo = GsonUtil.on().handshakeInfoFromString(jo.toString());
                if (isWifiP2p) {
                    if (linkStateListener != null) {
                        linkStateListener.onHandshakeInfoReceived(handshakeInfo);
                    }
                } else {
                    if (adHocListener != null) {
                        adHocListener.onHandshakeInfoReceived(handshakeInfo);
                    }
                }
                break;

            default:
                MeshLog.e("No json type match =" + type);
                break;
        }

    }


    @Override
    public void receivedData(String userId, String ipAddress, String data) {
        //byte[] decrypted = AesSaltEncryption.decrypt(data.getBytes());
        processMessage(userId, ipAddress, data);
    }

    private void processMessage(String userId, String ip, String data) {
        //MeshLog.i("MeshHttpServer received Data From :: " + data);

        //byte[] b = android.util.Base64.decode(data, Base64.DEFAULT);

        try {
            JSONObject jo = new JSONObject(data);
            Log.v("PING_PROCESS --", "Msg: " + jo);

            //Frames.Frame frame = Frames.Frame.parseFrom(b);
            int type = jo.getInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
            //Only to pass locally. Avoid increase parameter
            jo.put(JsonDataBuilder.KEY_BUDDY_ID, userId);

            if (JsonDataBuilder.isAppMessage(type)) {
                discoveryEventQueue.addAppMessageInQueue(new DiscoveryTask(ip, type, jo) {
                    @Override
                    public void run() {
                        parseReceivedData(this.jsonObject, this.senderId);
                    }
                });

            } else {
                discoveryEventQueue.addDiscoveryTaskInLast(new DiscoveryTask(ip, type, jo) {
                    @Override
                    public void run() {
                        parseReceivedData(this.jsonObject, this.senderId);
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
            MeshLog.e(" Parse Error for:" + data);
        }

    }


    /**
     * Stop accepting any discovery messages
     */
    public void pauseDirectDiscovery() {
        MeshLog.i("[SERVER] Pause DirectDiscovery");
        mIsDirectDiscoveryPause = true;
    }

    public void resumeDirectDiscovery() {
        mIsDirectDiscoveryPause = false;
        MeshLog.i("[SERVER] Resume DirectDiscovery");
        processMissingDiscoveryMessage();
    }

    /**
     * Stop accepting any discovery messages
     */
    public void pauseAdhocDiscovery() {
        // MeshLog.i("[SERVER] Pause AdHocDiscovery");
        // mIsAdhocDiscoveryPause = true;
    }

    /**
     * During pause of Server, we might receive sme discovery data. We process it upon resume of
     * server
     */
    private synchronized void processMissingDiscoveryMessage() {
        /*if (CollectionUtil.hasItem(mBufferedDiscoveryData)) {
            MeshLog.i("Process MissingDiscoveryMessage " + mBufferedDiscoveryData.toString());
            Pair<String, String> ipData;
            do {
                ipData = mBufferedDiscoveryData.remove();
                if (ipData != null) {
                    processMessage("", ipData.first, ipData.second);
                }
            } while (!mBufferedDiscoveryData.isEmpty());
        }*/
    }

}
