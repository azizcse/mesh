package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothSocket;

import androidx.annotation.Nullable;

import com.w3engineers.mesh.LinkMode;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.queue.messages.MessageUtil;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AesSaltEncryption;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifi.dispatch.SerialEventQueue;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.mesh.wifi.util.Config;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


/**
 * This Class Contains the Bluetooth communication link
 * The link is managed by socket communication
 * One active link after a successful connection establishment
 */
public class BleLink extends Thread implements Link {


    public enum State {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    private int RANDOM_DELAY_MAX = 20;
    private int RANDOM_DELAY_MIN = 16;

    private volatile State state = State.CONNECTING;
    private BluetoothSocket bSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private ConnectionStateListener connectionListener;
    private DispatchQueue outputThread = new DispatchQueue();
    private Queue<byte[]> outputQueue = new LinkedList<>();

    private volatile boolean shouldCloseWhenOutputIsEmpty = false;
    private String nodeId;
    private ScheduledThreadPoolExecutor pool;
    private ExecutorService outputExecutor;
    private LinkMode linkMode;
    public int mUserMode;
    public String publicKey;
    public static final long BT_TIMEOUT = 40 * 1000;
    private static BleLink mBleLink;

    private static Object object = new Object();

    private BleLink(BluetoothSocket bluetoothSocket, ConnectionStateListener connectionListener, LinkMode state/*,
                    MessageDispatcher messageDispatcher*/) {
        try {
            this.bSocket = bluetoothSocket;
            this.in = new DataInputStream(bluetoothSocket.getInputStream());
            this.out = new DataOutputStream(bluetoothSocket.getOutputStream());
            this.connectionListener = connectionListener;
            this.linkMode = state;
            //this.messageDispatcher = messageDispatcher;
            this.state = State.CONNECTED;
            configureOutput();
            if(state == LinkMode.CLIENT){
                BtHBSender.postBackground(timeOutRunnable, BT_TIMEOUT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BleLink on(BluetoothSocket bluetoothSocket, ConnectionStateListener connectionListener, LinkMode state/*,
                             MessageDispatcher messageDispatcher*/) {
        BleLink bleLink = mBleLink;
        if (bleLink == null) {
            synchronized (object) {
                bleLink = mBleLink;
                if (bleLink == null) {
                    bleLink = mBleLink = new BleLink(bluetoothSocket, connectionListener, state/*, messageDispatcher*/);
                }
            }
        }
        return bleLink;
    }

    public static BleLink getBleLink() {
        return mBleLink;
    }

    /*public void write(byte[] data) {
        try {
            byte[] encrypted = AesSaltEncryption.encrypt(data);
            this.out.write(encrypted);
            //MeshLog.v("Ble write in socket success");
        } catch (IOException e) {
            e.printStackTrace();
            //MeshLog.v("Ble write in socket failed");
        }
    }*/


    @Override
    public void run() {

        int bufferSize = 4096;
        ByteBuf inputData = Unpooled.buffer(bufferSize);
        inputData.order(ByteOrder.BIG_ENDIAN);


        try {
            int len;
            while (true) {

                inputData.ensureWritable(bufferSize, true);

                len = in.read(
                        inputData.array(),
                        inputData.writerIndex(),
                        bufferSize);
                if (len < 0) {
                    MeshLog.e("BT rear 0 byte.....");
                    continue;
                }

                inputData.writerIndex(inputData.writerIndex() + len);

                if (!formFrames(inputData)) {
                    MeshLog.e("BT from frame failed ");
                    break;
                   /* inputData.discardReadBytes();
                    inputData.capacity(inputData.writerIndex() + bufferSize);
                    continue;*/
                }
                inputData.discardReadBytes();
                inputData.capacity(inputData.writerIndex() + bufferSize);
            } // while
        } catch (InterruptedIOException ex) {
            MeshLog.i("BT InterruptedIOException");
            try {
                in.close();
            } catch (IOException ioex) {

            }

            notifyDisconnect("InterruptedIOException -> " + ex.getMessage());
            return;
        } catch (IOException e) {
            MeshLog.i("BT IOException");
            try {
                in.close();
            } catch (IOException ioex) {

            }
            notifyDisconnect("IOException -> " + e.getMessage());
            return;
        }

        notifyDisconnect("Break loop");

    }


    /*private void parseReceivedData(byte[] data) {
        try {
            //frame = Frames.Frame.parseFrom(frameBody);
            JSONObject jo = new JSONObject(new String(data));
            BtHBSender.postBackground(timeOutRunnable, BT_TIMEOUT);
            int type = jo.getInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
            new Thread(() ->
                    messageDispatcher.addReceiveMessage(MessageUtil.isDiscoveryMessage(type) ?
                            MessageBuilder.buildMeshBtDiscoveryMessage(BleLink.this, jo) :
                            MessageBuilder.buildMeshBtMessage(BleLink.this, jo))
            ).start();
        } catch (JSONException e) {

        } catch (Exception ex) {

        }
    }*/


    private boolean formFrames(ByteBuf inputData) {
        final int headerSize = 4;

        while (true) {
            if (inputData.readableBytes() < headerSize)
                break;

            inputData.markReaderIndex();
            int frameSize = inputData.readInt();

            if (frameSize > Config.frameSizeMax) {
                return false;
            }

            if (inputData.readableBytes() < frameSize) {
                inputData.resetReaderIndex();
                break;
            }

            //final Frames.Frame frame;

            final byte[] frameBody = new byte[frameSize];
            inputData.readBytes(frameBody, 0, frameSize);

            try {
                //byte[] decryptedData = AesSaltEncryption.decrypt(frameBody);
                //frame = Frames.Frame.parseFrom(frameBody);

                String fullData = new String(frameBody);

                MeshLog.i("[BT-Classic] data received in BT classic");

                JSONObject jo = new JSONObject(fullData);
                BtHBSender.postBackground(timeOutRunnable, BT_TIMEOUT);
                int type = jo.getInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
                /*messageDispatcher.addReceiveMessage(MessageUtil.isDiscoveryMessage(type) ?
                        MessageBuilder.buildMeshBtDiscoveryMessage(BleLink.this, jo) :
                        MessageBuilder.buildMeshBtMessage(BleLink.this, jo));*/

                /*new Thread(() ->
                        messageDispatcher.addReceiveMessage(MessageUtil.isDiscoveryMessage(type) ?
                                MessageBuilder.buildMeshBtDiscoveryMessage(BleLink.this, jo) :
                                MessageBuilder.buildMeshBtMessage(BleLink.this, jo))
                ).start();*/

                new Thread(() -> processReceiveMessage(jo)).start();

            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }

        }

        return true;
    }


    public void processReceiveMessage(JSONObject jo) {
        try {
            MeshLog.e("FileMessageTest", "Message receive: " + jo);
            int type = jo.getInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
            switch (type) {
                case JsonDataBuilder.HELLO_MASTER:
                    String senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                    String onlineNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                    String offLineNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                    String dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                    connectionListener.onV2ReceivedHelloFromClient(senderInfo, onlineNodes, offLineNodes, dataId);
                    break;

                case JsonDataBuilder.HELLO_CLIENT:
                    senderInfo = jo.optString(JsonDataBuilder.KEY_SENDER_INFO);
                    String onlineMeshNode = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                    String offlineNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                    dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                    connectionListener.onV2ReceivedHelloFromMaster(senderInfo, onlineMeshNode, offlineNodes, dataId);
                    break;

                case JsonDataBuilder.MESH_NODE:
                    String senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    String onlineMeshNodes = jo.optString(JsonDataBuilder.KEY_ONLINE_MESH_NODES);
                    String offlineMeshNodes = jo.optString(JsonDataBuilder.KEY_OFFLINE_NODES);
                    dataId = jo.optString(JsonDataBuilder.KEY_DATA_PREPARE_ID);
                    connectionListener.onV2ReceivedMeshUsers(senderId, onlineMeshNodes, offlineMeshNodes, dataId);
                    break;
                case JsonDataBuilder.NODE_LEAVE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    String leaveNodeList = jo.optString(JsonDataBuilder.KEY_LEAVE_NODE_ID);
                    connectionListener.onMeshLinkDisconnect(leaveNodeList, senderId);
                    break;

                case JsonDataBuilder.DISCONNECT_REQUEST:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    connectionListener.onReceiveDisconnectRequest(senderId);
                    break;

                case JsonDataBuilder.APP_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    String receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    String messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    String message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                    connectionListener.onMessageReceived(senderId, receiverId, messageId, message.getBytes(), "", "");
                    break;

                case JsonDataBuilder.ACK_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    String ackBody = jo.optString(JsonDataBuilder.KEY_ACK_BODY);
                    int status = jo.optInt(JsonDataBuilder.KEY_ACK_STATUS);
                    connectionListener.onReceivedMsgAck(senderId, receiverId, messageId, status, ackBody, "");
                    break;

                case JsonDataBuilder.BLE_MESH_DECISION_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    String ssid = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                    String password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                    int wifiCount = jo.optInt(JsonDataBuilder.MY_NODE_COUNT);
                    boolean isFirst = jo.optBoolean(JsonDataBuilder.IS_SERVER);
                    connectionListener.onV2BleMeshDecisionMessageReceive(senderId, receiverId, ssid,
                            password, wifiCount, isFirst);
                    break;

                case JsonDataBuilder.FORCE_CONNECTION_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    ssid = jo.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                    password = jo.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                    boolean isRequest = jo.optBoolean(JsonDataBuilder.IS_FORCE_REQUEST);
                    boolean isAbleToReceive = jo.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);
                    connectionListener.onV2ForceConnectionMessage(senderId, receiverId, ssid, password, isRequest, isAbleToReceive);
                    break;

                case JsonDataBuilder.FILE_RECEIVE_FREE_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    isAbleToReceive = jo.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);
                    connectionListener.onV2GetFileFreeModeMessage(senderId, receiverId, isAbleToReceive);
                    break;

                case JsonDataBuilder.FAILED_MESSAGE_ACK:
                    String source = jo.optString(JsonDataBuilder.KEY_MESSAGE_SOURCE);
                    String destination = jo.optString(JsonDataBuilder.KEY_MESSAGE_DESTINATION);
                    String hop = jo.optString(JsonDataBuilder.KEY_ACK_SENDER);
                    messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    connectionListener.onV2ReceivedFailedMessageAck(source, destination, hop, messageId);
                    break;

                case JsonDataBuilder.APP_BROADCAST_MESSAGE:

                    Broadcast broadcast = GsonUtil.on().broadcastFromString(jo.toString());
                    connectionListener.onBroadcastReceived(broadcast);
                    break;

                case JsonDataBuilder.APP_FILE_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                    messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    connectionListener.onFileMessageReceived(senderId, receiverId, messageId, message, null);
                    break;

                case JsonDataBuilder.APP_BROADCAST_ACK_MESSAGE:

                    BroadcastAck broadcastAck = GsonUtil.on().broadcastAckFromString(jo.toString());
                    connectionListener.onBroadcastACKMessageReceived(broadcastAck);

                    break;

                case JsonDataBuilder.PAYMENT_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                    connectionListener.onPaymentDataReceived(senderId, receiverId, messageId, message.getBytes());
                    break;
                case JsonDataBuilder.PAYMENT_ACK:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    connectionListener.onPaymentAckReceived(senderId, receiverId, messageId);
                    break;

                case JsonDataBuilder.INTERNET_USER_LEAVE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    String userList = jo.optString(JsonDataBuilder.KEY_USER_LIST);
                    connectionListener.onInternetUserLeave(senderId, receiverId, userList);
                    break;

                case JsonDataBuilder.INTERNET_USER:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    String sellerId = jo.optString(JsonDataBuilder.KEY_SELLER_ID);
                    String internetUsers = jo.optString(JsonDataBuilder.KEY_INTERNET_USERS);
                    connectionListener.onReceiverInternetUserLocally(senderId, receiverId, sellerId, internetUsers);
                    break;

                case JsonDataBuilder.USER_ROLE_SWITCH:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    int newRole = jo.optInt(JsonDataBuilder.KEY_NEW_ROLE);
                    connectionListener.onReceiveNewRole(senderId, receiverId, newRole);
                    break;

                case JsonDataBuilder.BUYER_FILE_MESSAGE:
                    senderId = jo.optString(JsonDataBuilder.KEY_SENDER_ID);
                    receiverId = jo.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                    message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                    int fileMessageType = jo.optInt(JsonDataBuilder.KEY_BUYER_FILE_ACK);
                    messageId = jo.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                    connectionListener.onReceiveBuyerFileMessage(senderId, receiverId, message, fileMessageType, "", messageId);
                    break;

                case JsonDataBuilder.HANDSHAKE_PING:
                case JsonDataBuilder.HANDSHAKE_BROADCAST:
                case JsonDataBuilder.HANDSHAKE_INFO:
                case JsonDataBuilder.HANDSHAKE_CONFIG:
                    HandshakeInfo handshakeInfo = GsonUtil.on().handshakeInfoFromString(jo.toString());
                    connectionListener.onHandshakeInfoReceived(handshakeInfo);
                    break;
                case JsonDataBuilder.HEART_BEAT:
                    MeshLog.v("BT heart beat received+++");
                    break;

                case JsonDataBuilder.BROADCAST_FILE_MESSAGE:
                    message = jo.optString(JsonDataBuilder.KEY_MESSAGE);
                    connectionListener.onReceivedFilePacket(message.getBytes());
                    break;
                default:

                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            MeshLog.e("JSONException on BT parser = " + jo.toString());
        }

    }


    public void startHeartbeatSend() {

        try {
            pool.scheduleAtFixedRate(() -> {
                if (state != State.CONNECTED)
                    return;
                sendHeartbeat();
                /*if (RouteManager.getInstance().isBtUserConnected()) {
                    //MeshLog.e("[HB] BT heart Beat send");
                    sendHeartbeat();
                }*/
            }, 0, generateRandomDelayTime(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException rejectedExecutionException) {
            rejectedExecutionException.printStackTrace();
        }


    }


    private void configureOutput() {
        pool = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("NsdLink " + this.hashCode() + " Output");
                thread.setDaemon(true);
                return thread;
            }
        });

        outputExecutor = new SerialEventQueue(pool);
    }


    private void sendHeartbeat() {
        JSONObject jo = new JSONObject();
        try {
            jo.put(JsonDataBuilder.KEY_MESSAGE_TYPE, JsonDataBuilder.HEART_BEAT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /*Frames.HeartBit.Builder heartBit = Frames.HeartBit.newBuilder();
        Frames.Frame.Builder frame = Frames.Frame.newBuilder();
        frame.setKind(Frames.Kind.HEARTBEAT);
        frame.setBitMsg(heartBit);
        Frames.Frame message = frame.build();*/
        enqueueFrame(jo.toString().getBytes(), "");
    }

    /*private Runnable timeOutRunnable = new Runnable() {
        @Override
        public void run() {
            state = State.DISCONNECTED;
            MeshLog.e("Time out!! NO data received in Bt layer ");
            notifyDisconnect("timeout ");
        }
    };*/
    private Runnable timeOutRunnable = new BtTimeOutRunnable();

    private class BtTimeOutRunnable implements Runnable {
        private String id = "sjkdfhsjkdfsfgjdsd";

        @Override
        public void run() {
            state = State.DISCONNECTED;
            MeshLog.e("Time out!! NO data received in Bt layer ");
            notifyDisconnect("timeout ");
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof BtTimeOutRunnable)
                return id.equals(((BtTimeOutRunnable) obj).id);

            return false;
        }
    }


    private void notifyDisconnect(String from) {
        this.state = State.DISCONNECTED;
        MeshLog.e(" Ble link disconnected due to =" + from);
        try {
            out.close();
            in.close();
            if (bSocket != null)
                MeshLog.e("BT socket closed ");
            bSocket.close();
        } catch (IOException e) {
            //CrashReporter.logException(e);
        }
        pool.shutdown();
        outputExecutor.shutdown();

        HandlerUtil.postBackground(() -> connectionListener.onDisconnectLink(BleLink.this));

        mBleLink = null;

    }


    private int enqueueFrame(final byte[] data, String messageId) {
        return writeFrame(data);
    }

    private void writeNextFrame() {
        // Output thread.
        if (state == State.DISCONNECTED) {
            outputQueue.clear();
            //messageQueue.clear();
            return;
        }


        byte[] frame = outputQueue.poll();
        //Frames.Frame frame = messageQueue.pollFirst(100);
        if (frame == null) {
            if (shouldCloseWhenOutputIsEmpty) {
                try {
                    out.close();
                    MeshLog.e("BT socket closed ");
                    bSocket.close();
                } catch (IOException e) {
                    //CrashReporter.logException(e);
                    e.printStackTrace();
                }
            }

            //Logger.debug("bt link outputQueue empty");
            return;
        }

        if (writeFrame(frame) == -1) {
            outputQueue.clear();
            return;
        }

        outputThread.dispatch(this::writeNextFrame);
    }

    private synchronized int writeFrame(byte[] frame) {
        // Output thread.
        //byte[] encrypted = AesSaltEncryption.encrypt(frame);
        byte[] buffer = frame;

        ByteBuffer header = ByteBuffer.allocate(4);
        header.order(ByteOrder.BIG_ENDIAN);
        header.putInt(buffer.length);
        try {
            //out.writeInt(buffer.length);
            //out.write(buffer);
            out.write(header.array());
            out.write(buffer);
            out.flush();
        } catch (IOException ex) {
            //CrashReporter.logException(ex);
            MeshLog.e(" Message Write fails in BlueTooth Link" + ex.getMessage());
            try {
                out.close();
                MeshLog.e("BT socket closed ");
                bSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return BaseMeshMessage.MESSAGE_STATUS_FAILED;
        }

        return BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
    } // writeFrame


    @Override
    public String getNodeId() {
        return this.nodeId;
    }

    @Override
    public void disconnect() {
        outputThread.dispatch(() -> {
            shouldCloseWhenOutputIsEmpty = true;
            writeNextFrame();
            notifyDisconnect("disconnect()");
        });
    }

    @Override
    public boolean isConnected() {
        return state == State.CONNECTED;
    }


    @Override
    public Type getType() {
        return Type.BT;
    }

    @Override
    public int getUserMode() {
        return mUserMode;
    }

    private int generateRandomDelayTime() {
        Random random = new Random();
        int randValue = random.nextInt((RANDOM_DELAY_MAX - RANDOM_DELAY_MIN) + 1) + RANDOM_DELAY_MIN;
        MeshLog.e("Generate H.B. delay value = " + randValue);
        return randValue * 1000;
    }


    @Override
    public int sendMeshMessage(byte[] data) {
        return enqueueFrame(data, "");
    }


}
