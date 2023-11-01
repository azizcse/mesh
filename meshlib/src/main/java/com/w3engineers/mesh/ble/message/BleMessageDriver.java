package com.w3engineers.mesh.ble.message;

import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.helper.MiddleManMessageStatusListener;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.queue.DiscoveryTask;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.MessageCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BleMessageDriver implements BleAppMessageListener {
    private static BleMessageDriver sInstance;
    private static final Object lock = new Object();

    private BleMessageManager mMessageManager;
    private ConnectionStateListener connectionStateListener;
    private Executor executor;
    private MeshLibMessageEventQueue discoveryEventQueue;
    private MessageCallback messageCallback;
    private MiddleManMessageStatusListener middleManMessageStatusListener;

    public static BleMessageDriver getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new BleMessageDriver();
                }
            }
        }

        return sInstance;
    }

    public <T> void setGenericObject(T... types) {
        for (T item : types) {
            if (item instanceof ConnectionStateListener) {
                this.connectionStateListener = (ConnectionStateListener) item;
            } else if (item instanceof MeshLibMessageEventQueue) {
                discoveryEventQueue = (MeshLibMessageEventQueue) item;
            }
        }
    }
   /* public void initListener(ConnectionStateListener connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
    }*/

    private BleMessageDriver() {
        mMessageManager = BleMessageManager.getInstance();
        mMessageManager.initBleAppMessageListener(this);
        executor = Executors.newSingleThreadExecutor();
    }

    public String sendMessage(String receiverId, byte[] messageData) {
        //Log.e("Compressed", "Main len : "+messageData.length);
        //byte[] compressedByte = TextCompressor.compressByte(messageData);
        //Log.e("Compressed", "Compressed len : "+compressedByte.length);
        MeshLog.i("[BLE_PROCESS] message receiver id: " + receiverId);
        return mMessageManager.sendMessage(receiverId, BleMessageHelper.MessageType.APP_MESSAGE, messageData);
    }

    public String sendFileMessage(String receiverId, byte[] messageData) {
        return mMessageManager.sendMessage(receiverId, BleMessageHelper.MessageType.APP_FILE_MESSAGE, messageData);
    }

    public String sendFileMessage(String receiverId, String messageId, byte[] data) {
        return mMessageManager.sendMessage(receiverId, messageId, BleMessageHelper.MessageType.APP_FILE_MESSAGE, data);
    }

    public String sendMessage(String receiverId, String messageId, byte[] msgData) {
        return mMessageManager.sendMessage(receiverId, messageId, BleMessageHelper.MessageType.APP_MESSAGE, msgData);
    }

    /**
     * For file sending specifically content broadcast we will use normal file sending process
     *
     * @param receiverId  the next hop receiver id
     * @param messageData File packet data
     * @return MessageId
     */
    public String sendFileContentMessage(String receiverId, byte[] messageData) {
        return mMessageManager.sendMessage(receiverId, BleMessageHelper.MessageType.FILE_CONTENT_MESSAGE, messageData);
    }

    @Override
    public void onReceiveMessage(String sender, byte[] message) {
        try {
            JSONObject jo = new JSONObject(new String(message));
            int type = jo.optInt(JsonDataBuilder.KEY_MESSAGE_TYPE);

            if (JsonDataBuilder.isAppMessage(type)) {
                discoveryEventQueue.addAppMessageInQueue(new DiscoveryTask(sender, type, jo) {
                    @Override
                    public void run() {
                        processDiscoveryMessage(this.senderId, this.type, this.jsonObject);
                    }
                });
            } else {
                discoveryEventQueue.addDiscoveryTaskInLast(new DiscoveryTask(sender, type, jo) {
                    @Override
                    public void run() {
                        processDiscoveryMessage(this.senderId, this.type, this.jsonObject);
                    }
                });
            }

        } catch (JSONException e) {
            MeshLog.e("[BLE_PROCESS] JsonException parsing BLE data");
        }
    }

    @Override
    public void onMessageSendingStatus(String messageId, boolean isSuccess) {
        if (middleManMessageStatusListener != null) {
            middleManMessageStatusListener.onMiddleManMessageSendStatusReceived(messageId, isSuccess);
        }
        if (messageCallback != null) {
            messageCallback.onBleMessageSend(messageId, isSuccess);
            messageCallback = null;
        } else {
            MeshLog.v("[BLE_PROCESS] message callback null----");
        }
    }

    @Override
    public void onReceivedFilePacket(String senderId, byte[] data) {
        discoveryEventQueue.addAppMessageInQueue(() -> {
            try {
                JSONObject jo = new JSONObject(new String(data));
                int type = jo.optInt(JsonDataBuilder.KEY_MESSAGE_TYPE);

                if (type == JsonDataBuilder.BROADCAST_FILE_MESSAGE) {
                    String message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                    connectionStateListener.onReceivedFilePacket(message.getBytes());
                } else {
                    //Todo we have to reduce the brelow complexity

                    if (type > 0) {
                        processDiscoveryMessage(senderId, type, jo);
                    } else {
                        connectionStateListener.onReceivedFilePacket(data);
                    }
                }


            } catch (JSONException e) {
                connectionStateListener.onReceivedFilePacket(data);
            }
        });
    }


    private void processDiscoveryMessage(String sender, int messageType, JSONObject jo) {
        MeshLog.e("[BLE_PROCESS] Ble message received type: " + messageType);
        switch (messageType) {
            case JsonDataBuilder.HELLO_MASTER:
                String senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                String onlineNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                String offLineNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                String dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                connectionStateListener.onV2ReceivedHelloFromClient(senderInfo, onlineNodes, offLineNodes, dataId);
                break;

            case JsonDataBuilder.HELLO_CLIENT:
                senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                String onlineMeshNode = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                String offlineNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                connectionStateListener.onV2ReceivedHelloFromMaster(senderInfo, onlineMeshNode, offlineNodes, dataId);
                break;

            case JsonDataBuilder.MESH_NODE:
                String senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String onlineMeshNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                String offlineMeshNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                connectionStateListener.onV2ReceivedMeshUsers(senderId, onlineMeshNodes, offlineMeshNodes, dataId);
                break;
            case JsonDataBuilder.NODE_LEAVE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String leaveNodeList = jo.optString(JsonDataBuilder.KEY_LEAVE_NODE_ID);
                connectionStateListener.onMeshLinkDisconnect(leaveNodeList, senderId);
                break;

            case JsonDataBuilder.DISCONNECT_REQUEST:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                connectionStateListener.onReceiveDisconnectRequest(senderId);
                break;

            case JsonDataBuilder.APP_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                String receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                String message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                connectionStateListener.onMessageReceived(senderId, receiverId, messageId, message.getBytes(), sender, sender);
                break;

            case JsonDataBuilder.ACK_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                String ackBody = jo.optString(JsonDataBuilder.KEY_ACK_BODY);
                int status = jo.optInt(JsonDataBuilder.KEY_ACK_STATUS);
                connectionStateListener.onReceivedMsgAck(senderId, receiverId, messageId, status, ackBody, sender);
                break;

            case JsonDataBuilder.BLE_MESH_DECISION_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String ssid = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                String password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                int wifiCount = jo.optInt(JsonDataBuilder.MY_NODE_COUNT);
                boolean isFirst = jo.optBoolean(JsonDataBuilder.IS_SERVER);
                connectionStateListener.onV2BleMeshDecisionMessageReceive(senderId, receiverId, ssid,
                        password, wifiCount, isFirst);
                break;

            case JsonDataBuilder.FORCE_CONNECTION_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                ssid = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                boolean isRequest = jo.optBoolean(JsonDataBuilder.IS_FORCE_REQUEST);
                boolean isAbleToReceive = jo.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);
                connectionStateListener.onV2ForceConnectionMessage(senderId, receiverId, ssid, password, isRequest, isAbleToReceive);
                break;

            case JsonDataBuilder.FILE_RECEIVE_FREE_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                isAbleToReceive = jo.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);
                connectionStateListener.onV2GetFileFreeModeMessage(senderId, receiverId, isAbleToReceive);
                break;

            case JsonDataBuilder.FAILED_MESSAGE_ACK:
                String source = jo.optString(JsonDataBuilder.KEY_MESSAGE_SOURCE);
                String destination = jo.optString(JsonDataBuilder.KEY_MESSAGE_DESTINATION);
                String hop = jo.optString(JsonDataBuilder.KEY_ACK_SENDER);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                connectionStateListener.onV2ReceivedFailedMessageAck(source, destination, hop, messageId);
                break;

            case JsonDataBuilder.APP_BROADCAST_MESSAGE:

                Broadcast broadcast = GsonUtil.on().broadcastFromString(jo.toString());
                connectionStateListener.onBroadcastReceived(broadcast);
                break;

            case JsonDataBuilder.APP_FILE_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                connectionStateListener.onFileMessageReceived(senderId, receiverId, messageId, message, sender);
                break;

            case JsonDataBuilder.APP_BROADCAST_ACK_MESSAGE:

                BroadcastAck broadcastAck = GsonUtil.on().broadcastAckFromString(jo.toString());
                connectionStateListener.onBroadcastACKMessageReceived(broadcastAck);

                break;

            case JsonDataBuilder.PAYMENT_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                connectionStateListener.onPaymentDataReceived(senderId, receiverId, messageId, message.getBytes());
                break;
            case JsonDataBuilder.PAYMENT_ACK:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                connectionStateListener.onPaymentAckReceived(senderId, receiverId, messageId);
                break;

            case JsonDataBuilder.INTERNET_USER_LEAVE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String userList = jo.optString(JsonDataBuilder.KEY_USER_LIST);
                connectionStateListener.onInternetUserLeave(senderId, receiverId, userList);
                break;

            case JsonDataBuilder.INTERNET_USER:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String sellerId = jo.optString(JsonDataBuilder.KEY_SELLER_ID);
                String internetUsers = jo.optString(JsonDataBuilder.KEY_INTERNET_USERS);
                connectionStateListener.onReceiverInternetUserLocally(senderId, receiverId, sellerId, internetUsers);
                break;

            case JsonDataBuilder.USER_ROLE_SWITCH:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                int newRole = jo.optInt(JsonDataBuilder.KEY_NEW_ROLE);
                connectionStateListener.onReceiveNewRole(senderId, receiverId, newRole);
                break;

            case JsonDataBuilder.BUYER_FILE_MESSAGE:
                senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                int fileMessageType = jo.optInt(JsonDataBuilder.KEY_BUYER_FILE_ACK);
                messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                connectionStateListener.onReceiveBuyerFileMessage(senderId, receiverId, message, fileMessageType, sender, messageId);
                break;

            case JsonDataBuilder.HANDSHAKE_PING:
            case JsonDataBuilder.HANDSHAKE_BROADCAST:
            case JsonDataBuilder.HANDSHAKE_INFO:
            case JsonDataBuilder.HANDSHAKE_CONFIG:
                HandshakeInfo handshakeInfo = GsonUtil.on().handshakeInfoFromString(jo.toString());
                connectionStateListener.onHandshakeInfoReceived(handshakeInfo);
                break;
        }

    }

    /**
     * <p>Shutdown executor service to protect memory leak</p>
     */
    public void shutdown() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }

    public void setMessageCallBack(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public void setMiddleManMessageListener(MiddleManMessageStatusListener listener) {
        this.middleManMessageStatusListener = listener;
    }
}
