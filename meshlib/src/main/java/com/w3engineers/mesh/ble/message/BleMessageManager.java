package com.w3engineers.mesh.ble.message;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.w3engineers.mesh.ble.BleManager;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Here we will manage BLE message
 * 1. Queue management
 * 2. Message ID Management
 * 3. Message for per user management
 * 4. Message Chunk management for serialize
 * 5. Message chunk receive for deserialize
 * 6. Ble message server client management
 */
public class BleMessageManager implements BleDataListener {
    private static BleMessageManager sInstance;
    private static final Object lock = new Object();
    private final String PREFIX_TAG = "[BLE_PROCESS]";

    //private BleMessageQueue<BleMessageModel> messageQueue;
    //private BleMessageModel runningModel;
    private final BleManager mBleManager;
    private final Context mContext;

    private final long MESSAGE_TIME_THRESHOLD = 15 * 1000;

    private BleInternalMessageCallback bleInternalMessageCallback;
    private BleAppMessageListener mAppMessageListener;


    private final ConcurrentHashMap<String, BleMessageModel> receivedMessageMap;
    private ConcurrentLinkedDeque<BleMessageModel> messageQueue;

    private volatile boolean isMessageRunning;

    private volatile boolean isBleChannelBusy = false;


    public static BleMessageManager getInstance(Context context, BleManager manager) {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new BleMessageManager(context, manager);
                }
            }
        }
        return sInstance;
    }

    public static BleMessageManager getInstance() {
        return sInstance;
    }

    private BleMessageManager(Context context, BleManager mBleManager) {
        this.mContext = context;
        messageQueue = new ConcurrentLinkedDeque<>();
        // We do not need any hardware related callback
        this.mBleManager = mBleManager;
        this.mBleManager.initBleMessageCallback(this);

        receivedMessageMap = new ConcurrentHashMap<>();
    }

    public void initBleInternalMessageListener(BleInternalMessageCallback callback) {
        this.bleInternalMessageCallback = callback;
    }

    public void initBleAppMessageListener(BleAppMessageListener listener) {
        this.mAppMessageListener = listener;
    }

    public void resetQueue() {
        messageQueue.clear();
        receivedMessageMap.clear();
        isMessageRunning = false;
    }

    private BleMessageModel prepareMessageMode(String receiverId, String messageId, byte messageType, byte[] data) {
        BleMessageModel model = new BleMessageModel();
        model.receiverId = receiverId;
        if (messageId == null)
            model.messageId = BleMessageHelper.MESSAGE_ID;
        model.messageType = messageType;
        model.data = data;
        return model;
    }

    public String sendMessage(String receiverId, byte messageType, byte[] data) {
        String messageId = BleMessageHelper.MESSAGE_ID;
        return sendMessage(receiverId, messageId, messageType, data);
    }


    public String sendMessage(String receiverId, String messageId, byte messageType, byte[] data) {

//        MeshLog.v("FILE_SPEED_TEST_4.5 " + Calendar.getInstance().getTime());
        if (data == null) {
            MeshLog.e(PREFIX_TAG + " Ble message data null. message type: " + messageType);
            return messageId;
        }

        BleMessageModel model = new BleMessageModel();
        model.receiverId = receiverId;
        model.messageId = messageId;
        model.messageType = messageType;
        model.data = data;


        // We give priority for file message. In the mean time other message will be added in queue
        // Each part of file message will get 5 second threshold for waiting any file message will
        // come or not. after 5 second other message will start to pass if no file message remaining.

        // Caution: If within 5 second next file message not prepared then it will high chance to
        // send other message. And in the receiver side the message parsing section will be corrupted.

        if (messageType == BleMessageHelper.MessageType.APP_FILE_MESSAGE) {

            // before executing we have to check any file already processing or not.
            // If any file processing we will add that message in queue and wait for execution

            // If any message not running and a

            MeshLog.i(PREFIX_TAG + " is any ble message running: " + isMessageRunning);

            if (!isMessageRunning) {
                messageQueue.addFirst(model);
                prepareAndSendFileAck(model);
                return messageId;
            }
        }

        if (messageQueue.isEmpty()) {
            // This is the first message. So not need any queue
            messageQueue.add(model);
            prepareAndSend(messageQueue.peek());
        } else {
            BleMessageModel oldMessageModel = messageQueue.peek();

            if (oldMessageModel != null && oldMessageModel.enterTime > 0) {
                if (System.currentTimeMillis() - oldMessageModel.enterTime > MESSAGE_TIME_THRESHOLD) {
                    isMessageRunning = false;
                }
            }


            if (messageType == BleMessageHelper.MessageType.BLE_HEART_BIT) {
                // We will check any ping exists or not
                if (!haveAnyPingMessage()) {
                    MeshLog.i(PREFIX_TAG + " Message added in queue. And it is ping");
                    messageQueue.add(model);
                }
            } else {
                MeshLog.i(PREFIX_TAG + " Message added in queue");
                messageQueue.add(model);
            }


            // we can check here again if queue size only one or not and call prepare and send
            if (!isMessageRunning) {
                MeshLog.i(PREFIX_TAG + " Message added in queue but not running");
                prepareAndSend(messageQueue.peek());
            }
        }
        return messageId;
    }


    private void prepareAndSend(BleMessageModel model) {
        if (isBleChannelBusy) {
            MeshLog.i(PREFIX_TAG + " Ble channel busy");
            return;
        }

        if (model == null) {
            MeshLog.v(PREFIX_TAG + " BleMessageModel null");
            if (!messageQueue.isEmpty()) {
                MeshLog.v(PREFIX_TAG + " Message queue not empty");
                prepareAndSend(messageQueue.peek());
            } else {
                isMessageRunning = false;
            }
        } else {
            isMessageRunning = true;
            model.enterTime = System.currentTimeMillis();
            // First prepare chunk
            if (model.data == null) {
                isMessageRunning = false;
                MeshLog.e(PREFIX_TAG + " BLE message model existing data null. Message type: " + model.messageType);
                messageQueue.poll();
                prepareAndSend(messageQueue.peek());

            } else {
                int actualSize = model.data.length;


                // Byte management section

                MeshLog.v(PREFIX_TAG + " Preparing a ble message model and send " + model.messageType);

                // Prepare first chunk of header
                ByteBuffer sizeBuffer = ByteBuffer.allocate(4);// Four bytes for actual data size
                sizeBuffer.putInt(actualSize);
                byte[] sizeHeader = sizeBuffer.array();

                ByteBuffer typeBuffer = ByteBuffer.allocate(1); // We are taking 1 byte for message type
                typeBuffer.put(model.messageType);
                byte[] typeHeader = typeBuffer.array();

                byte[] header = new byte[sizeHeader.length + typeHeader.length];

                System.arraycopy(typeHeader, 0, header, 0, typeHeader.length);

                System.arraycopy(sizeHeader, 0, header, typeHeader.length, sizeHeader.length);

                byte[] finalData = new byte[header.length + actualSize];

                System.arraycopy(header, 0, finalData, 0, header.length);
                System.arraycopy(model.data, 0, finalData, header.length, actualSize);

                actualSize = finalData.length;

                model.totalChunk = calculateChunkCount(actualSize);
                model.data = finalData;

                //runningModel = model;

                send(model);
            }
        }

    }

    private void prepareAndSendFileAck(BleMessageModel model) {
        isMessageRunning = true;
        model.enterTime = System.currentTimeMillis();
        // First prepare chunk
        int actualSize = model.data.length;


        // Byte management section

        MeshLog.v(PREFIX_TAG + " Preparing a ble file message model and send " + model.messageType);

        // Prepare first chunk of header
        ByteBuffer sizeBuffer = ByteBuffer.allocate(4);// Four bytes for actual data size
        sizeBuffer.putInt(actualSize);
        byte[] sizeHeader = sizeBuffer.array();

        ByteBuffer typeBuffer = ByteBuffer.allocate(1); // We are taking 1 byte for message type
        typeBuffer.put(model.messageType);
        byte[] typeHeader = typeBuffer.array();

        byte[] header = new byte[sizeHeader.length + typeHeader.length];

        System.arraycopy(typeHeader, 0, header, 0, typeHeader.length);

        System.arraycopy(sizeHeader, 0, header, typeHeader.length, sizeHeader.length);

        byte[] finalData = new byte[header.length + actualSize];

        System.arraycopy(header, 0, finalData, 0, header.length);
        System.arraycopy(model.data, 0, finalData, header.length, actualSize);

        actualSize = finalData.length;

        model.totalChunk = calculateChunkCount(actualSize);
        model.data = finalData;

        //runningModel = model;

        send(model);
    }

    private void send(BleMessageModel model) {
        // get chunk amount. It is the first
        int form = model.currentChunk * BleMessageHelper.CHUNK_SIZE;

        int remaining = model.data.length - form;
        int to;
        if (remaining > BleMessageHelper.CHUNK_SIZE) {
            to = form + BleMessageHelper.CHUNK_SIZE;
        } else {
            to = form + remaining;
        }
        byte[] data = getChunkData(model.data, form, to);

        mBleManager.sendMessage(model.receiverId, data);
    }

    private int calculateChunkCount(int length) {
        int chunkSize = 1;
        if (length < BleMessageHelper.CHUNK_SIZE) {
            return chunkSize;
        } else {
            chunkSize = length / BleMessageHelper.CHUNK_SIZE;

            int remaining = length - (chunkSize * BleMessageHelper.CHUNK_SIZE);
            if (remaining > 0) {
                chunkSize++;
            }
        }

        return chunkSize;
    }

    private byte[] getChunkData(byte[] data, int form, int to) {
        return Arrays.copyOfRange(data, form, to);
    }


    /**
     * Message callback
     */
    @Override
    public void onGetMessageSendResponse(boolean isSuccess) {
        MeshLog.v(PREFIX_TAG + " Byte transfer success: " + isSuccess);
        if (isSuccess) {

            // Actually the model will not be null.
            BleMessageModel runningModel = messageQueue.peek();
            if (runningModel != null) {
                // Increase chunk count for success;
                runningModel.currentChunk++;

                if (runningModel.currentChunk >= runningModel.totalChunk) {
                    //Todo send message send success response
                    MeshLog.v(PREFIX_TAG + " All data send successful. Try to send next one");

                    if (bleInternalMessageCallback != null) {
                        bleInternalMessageCallback.onMessageSendingStatus(runningModel.messageId, true);
                    }

                    if (mAppMessageListener != null) {
                        mAppMessageListener.onMessageSendingStatus(runningModel.messageId, true);
                    }

                    //runningModel = null;

                    messageQueue.poll();
                    isMessageRunning = false;

                    // before taking other message we have to check the recent message is file
                    // message pr not. if file message then we have to start tracker

                    if (runningModel.messageType == BleMessageHelper.MessageType.APP_FILE_MESSAGE) {
                        isBleChannelBusy = true;
                        registerBleChannelBusyState();
                    }

                    //Select next message and send
                    prepareAndSend(messageQueue.peek());
                } else {
                    send(runningModel);
                }
            }
        } else {
            BleMessageModel runningModel = messageQueue.peek();

            if (runningModel != null) {
                if (bleInternalMessageCallback != null) {
                    bleInternalMessageCallback.onMessageSendingStatus(runningModel.messageId, false);
                }

                if (mAppMessageListener != null) {
                    mAppMessageListener.onMessageSendingStatus(runningModel.messageId, false);
                }

                isMessageRunning = false;

                messageQueue.poll();
                prepareAndSend(messageQueue.peek());
            }

        }
    }

    @Override
    public void onGetMessage(String senderId, BluetoothDevice device, byte[] partialData) {

//        MeshLog.v("FILE_SPEED_TEST_5.5 " + Calendar.getInstance().getTime());
        if (receivedMessageMap.containsKey(senderId)) {
            // It is an exists message. Need to updated or concatenate message

            // Here is the possible case. Suppose all message received before clearing map a
            // new message come from same user. And it will will try to concatenate again.
            // Before concatenating message first check that the previous message completed or not.

            BleMessageModel model = receivedMessageMap.get(senderId);
            // Basically it will not be null
            if (model != null) {
                // First check that the existing model still remaining or not

                if (model.messageType == BleMessageHelper.MessageType.APP_FILE_MESSAGE) {
                    isBleChannelBusy = true;
                    registerBleChannelBusyState();
                }

                if (model.currentChunk == model.totalChunk) {
                    // Todo we have to create new model here.
                    // Todo Pass the current model to upper layer
                    MeshLog.e(PREFIX_TAG + " a new message received before clearing the previous message of same user");
                } else {
                    // Concatenate the byte data to model
                    try {
                        System.arraycopy(partialData, 0, model.data, model.currentChunk, partialData.length);
                    } catch (Exception e) {
                        e.printStackTrace();
                        receivedMessageMap.remove(senderId);
                        return;
                    }


                    model.currentChunk += partialData.length;

                    if (model.currentChunk >= model.totalChunk) {
                        MeshLog.v(PREFIX_TAG + " Full data received: data is: " + (new String(model.data)));

                        parsingMessageData(model);

                        //remove the map.
                        receivedMessageMap.remove(senderId);
                    }
                }
            }
        } else {
            // Fully new message

            BleMessageModel model = new BleMessageModel();
            byte typeData = partialData[0];

            byte[] sizeByte = Arrays.copyOfRange(partialData, 1, BleMessageHelper.HEADER_BYTE_SIZE);
            ByteBuffer sizeWrap = ByteBuffer.wrap(sizeByte);
            int dataSize = sizeWrap.getInt();

            MeshLog.v(PREFIX_TAG + "Sender ID: " + senderId + " data size: " + dataSize);

            // Checking the actual data size
            if (dataSize > BleMessageHelper.MAX_DATA_SIZE_IN_BYTE) {
                MeshLog.e(PREFIX_TAG + " Ble message found wrong data or wrong packet");
                return;
            }

            // check the message type is valid;
            if (!isValidMessageType(typeData)) {
                MeshLog.e(PREFIX_TAG + " Ble receive wrong type message. This type not supported");
                return;
            }

            model.messageType = typeData;
            model.totalChunk = dataSize;
            model.receiverId = senderId;
            model.device = device;

            // prepare a data byte for main data
            byte[] mainData = new byte[dataSize];

            byte[] partData = Arrays.copyOfRange(partialData, BleMessageHelper.HEADER_BYTE_SIZE, partialData.length);

            System.arraycopy(partData, 0, mainData, model.currentChunk, partData.length);
            model.currentChunk = partData.length;

            model.data = mainData;

            if (model.messageType == BleMessageHelper.MessageType.APP_FILE_MESSAGE) {
                isBleChannelBusy = true;
                registerBleChannelBusyState();
            }

            MeshLog.v(PREFIX_TAG + " Main data: " + (new String(mainData)));

            // check it is a only one packet or not.
            if (model.currentChunk >= model.totalChunk) {

                parsingMessageData(model);

                MeshLog.v(PREFIX_TAG + " Full message received. Send to upper layer");
            } else {
                // Waiting for next chunk
                receivedMessageMap.put(senderId, model);
            }

        }
    }

    private void parsingMessageData(BleMessageModel model) {
        switch (model.messageType) {
            case BleMessageHelper.MessageType.APP_MESSAGE:
            case BleMessageHelper.MessageType.FILE_CONTENT_MESSAGE:
                if (mAppMessageListener != null) {
                    mAppMessageListener.onReceiveMessage(model.receiverId, model.data);
                }
                break;
            case BleMessageHelper.MessageType.CLIENT_IDENTITY:
                if (bleInternalMessageCallback != null) {
                    bleInternalMessageCallback.onReceiveIdentityMessage(new String(model.data), model.device);
                }
                break;
            case BleMessageHelper.MessageType.CREDENTIAL_MESSAGE:
                try {
                    JSONObject jsonObject = new JSONObject(new String(model.data));
                    String ssid = jsonObject.optString(JsonDataBuilder.KEY_SOFTAP_SSID);
                    String password = jsonObject.optString(JsonDataBuilder.KEY_SOFTAP_PASSWORD);
                    String goID = jsonObject.optString(JsonDataBuilder.KEY_GO_ID);
                    if (bleInternalMessageCallback != null) {
                        bleInternalMessageCallback.onReceiveCredentialMessage(goID, password, ssid);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

              /*  String message = new String(model.data);
                String ssid;
                String password;
                if (message.length() > BleMessageHelper.PASSWORD_LENGTH) {
                    password = message.substring(0, BleMessageHelper.PASSWORD_LENGTH);
                    ssid = message.substring(BleMessageHelper.PASSWORD_LENGTH);

                    if (bleInternalMessageCallback != null) {
                        bleInternalMessageCallback.onReceiveCredentialMessage(model.receiverId, password, ssid);
                    }
                }*/
                break;
            case BleMessageHelper.MessageType.FORCE_CONNECTION:
                if (bleInternalMessageCallback != null) {
                    bleInternalMessageCallback.onGetForceConnectionRequest(new String(model.data), model.device);
                }
                break;
            case BleMessageHelper.MessageType.FORCE_CONNECTION_REPLY:
                if (bleInternalMessageCallback != null) {
                    bleInternalMessageCallback.onGetForceConnectionReply(new String(model.data));
                }
                break;
            case BleMessageHelper.MessageType.BLE_HEART_BIT:
                MeshLog.v(PREFIX_TAG + " We got the heart bit");
                break;
            case BleMessageHelper.MessageType.APP_FILE_MESSAGE:
                MeshLog.v(PREFIX_TAG + " We got the file here");
                if (mAppMessageListener != null) {
                    mAppMessageListener.onReceivedFilePacket(model.receiverId, model.data);
                }
                break;
        }
    }

    private boolean isValidMessageType(byte type) {
        return type == BleMessageHelper.MessageType.APP_MESSAGE
                || type == BleMessageHelper.MessageType.CLIENT_IDENTITY
                || type == BleMessageHelper.MessageType.CREDENTIAL_MESSAGE
                || type == BleMessageHelper.MessageType.FORCE_CONNECTION
                || type == BleMessageHelper.MessageType.FORCE_CONNECTION_REPLY
                || type == BleMessageHelper.MessageType.FILE_CONTENT_MESSAGE
                || type == BleMessageHelper.MessageType.BLE_HEART_BIT
                || type == BleMessageHelper.MessageType.APP_FILE_MESSAGE;
    }


    private void registerBleChannelBusyState() {
        HandlerUtil.postBackground(bleChannelBusyTracker, 7 * 1000L);
    }

    private boolean haveAnyPingMessage() {
        if (!messageQueue.isEmpty()) {
            for (BleMessageModel model : messageQueue) {
                if (model.messageType == BleMessageHelper.MessageType.BLE_HEART_BIT) {
                    return true;
                }
            }
        }
        return false;
    }

    private Runnable bleChannelBusyTracker = () -> {
        isBleChannelBusy = false;
        if (messageQueue != null && !isMessageRunning) {
            prepareAndSend(messageQueue.peek());
        }
    };

}
