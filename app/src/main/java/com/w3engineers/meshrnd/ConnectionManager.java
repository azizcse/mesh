package com.w3engineers.meshrnd;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.letbyte.core.meshfilesharing.api.MeshFileCommunicator;
import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.api.support.mesh.SupportTransportManager;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.TransportState;
import com.w3engineers.mesh.UserState;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.premission.MeshSystemRequestActivity;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;
import com.w3engineers.meshrnd.model.MessageModel;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.ui.Nearby.NearbyCallBack;
import com.w3engineers.meshrnd.ui.chat.ChatActivity;
import com.w3engineers.meshrnd.ui.chat.ChatDataProvider;
import com.w3engineers.meshrnd.ui.chat.MessageListener;
import com.w3engineers.meshrnd.ui.nav.BottomNavActivity;
import com.w3engineers.meshrnd.ui.nav.MeshStateListener;
import com.w3engineers.meshrnd.util.HandlerUtil;
import com.w3engineers.meshrnd.util.JsonKeys;
import com.w3engineers.walleter.wallet.WalletService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

public class ConnectionManager implements LinkStateListener, ForwardListener {
    //(com,2077 - > 66,65)
    //(noa,2067 - > 67,61)
    private static ConnectionManager connectionManager;
    private Map<String, UserModel> discoverUserMap;
    private Map<String, Link> connectedLinkMap;
    private TransportManagerX mTransportManager;
    private NearbyCallBack nearbyCallBack;
    private final int APP_PORT = 7576;
    private Map<Long, String> messageStatusMap;
    private MeshFileCommunicator mMeshFileCommunicator;

/*    private final String WIFI_PREFIX = "kind.k";
    private final String BLE_PREFIX = "mane";*/

    // Network prefix must be more than three character with lowercase without any dot
    private final String SOCKET_URL = "https://dev-signal.telemesh.net/";//"https://multiverse.w3engineers.com/";
    private BottomNavActivity.StateListener mStateListener;
    private Map<String, String> requestUserInfoList;
    private WalletService.WalletLoadListener mWalletLoadListener = new WalletService.WalletLoadListener() {
        @Override
        public void onWalletLoaded(String walletAddress, String publicKey, String appToken) {
            mTransportManager = SupportTransportManager.getInstance().getTransportManager(
                    App.getContext(), APP_PORT, walletAddress, publicKey,
                    BuildConfig.NETWORK_PREFIX, SOCKET_URL, ConnectionManager.this);

            mTransportManager.startMesh();

            mMeshFileCommunicator = SupportTransportManager.getInstance().getMeshFileCommunicator();
//            mMeshFileCommunicator.setDefaultStoragePath(App.getContext().getCacheDir().getAbsolutePath());

            initFileTransferListener();
        }

        @Override
        public void onErrorOccurred(String message, String appToken) {

        }

        @Override
        public void onErrorOccurred(int code, String appToken) {
            switch (code) {
                case WalletService.ERROR_CODE_NO_PERMISSION:
                    mMeshStateListener.onInterruption(MeshSystemRequestActivity.REQUEST_SYSTEM_PERMISSIONS);
                    break;
                default:
                    break;
            }
        }
    };

    private ConnectionManager(BottomNavActivity.StateListener stateListener) {
        mStateListener = stateListener;
        discoverUserMap = Collections.synchronizedMap(new HashMap());
        connectedLinkMap = Collections.synchronizedMap(new HashMap<>());
        requestUserInfoList = Collections.synchronizedMap(new HashMap<>());
        Util.checkDeviceLogValidity();

    }

    public void init() {
        MeshLog.v("init() called from app");
        WalletService.getInstance(App.getContext()).createOrLoadWallet(
                WalletService.DEFAULT_PASSWORD,
                App.getContext().getPackageName(),
                mWalletLoadListener);
    }

    public void stopMesh() {
        if (mTransportManager != null) {
            mTransportManager.stopMesh();
        }
    }

    public void stopAdhoc() {
        if (mTransportManager != null) {
            mTransportManager.getAdHocTransport().stop();
        }
    }

    public int getLinkCount(int type) {
        return 0;
    }


    public static ConnectionManager on(BottomNavActivity.StateListener stateListener, Context context) {
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(stateListener);
        }
        return connectionManager;
    }

    public static ConnectionManager on() {
        return on(null, App.getContext());
    }

    public List<UserModel> getUserList() {
        return new ArrayList<>(discoverUserMap.values());
    }


    public String getConnectionType(String nodeId) {

        if (mTransportManager == null || TextUtils.isEmpty(nodeId)) {
            return "";
        }

        int type = mTransportManager.getLinkTypeById(nodeId);
        if (type == RoutingEntity.Type.WiFi) {
            return "WiFi";
        } else if (type == RoutingEntity.Type.BT) {
            return "BT";
        } else if (type == RoutingEntity.Type.WifiMesh) {
            return "WIFI MESH";
        } else if (type == RoutingEntity.Type.BtMesh) {
            return "BT MESH";
        } else if (type == RoutingEntity.Type.INTERNET) {
            return "Internet";
        } else if (type == RoutingEntity.Type.HB) {
            return "HB";
        } else if (type == RoutingEntity.Type.HB_MESH) {
            return "HB MESH";
        } else if (type == RoutingEntity.Type.BLE) {
            return "BLE";
        } else if (type == RoutingEntity.Type.BLE_MESH) {
            return "BLE_MESH";
        } else {
            onUserDisconnected(nodeId);
            return "NA";
        }
    }

    public String getMultipleConnectionType(String nodeId) {

        if (mTransportManager == null || TextUtils.isEmpty(nodeId)) {
            return "";
        }

        List<Integer> types = mTransportManager.getMultipleLinkTypeByIdDebug(nodeId);
        List<String> connectionTypeList = new ArrayList<>();

        for (int type : types) {
            switch (type) {
                case RoutingEntity.Type.WiFi:
                    connectionTypeList.add("W");
                    break;
                case RoutingEntity.Type.BT:
                    connectionTypeList.add("BT");
                    break;
                case RoutingEntity.Type.WifiMesh:
                    connectionTypeList.add("WM");
                    break;
                case RoutingEntity.Type.BtMesh:
                    connectionTypeList.add("BTM");
                    break;
                case RoutingEntity.Type.INTERNET:
                    connectionTypeList.add("I");
                    break;
                case RoutingEntity.Type.HB:
                    connectionTypeList.add("H");
                    break;
                case RoutingEntity.Type.HB_MESH:
                    connectionTypeList.add("HM");
                    break;
                case RoutingEntity.Type.BLE:
                    connectionTypeList.add("B");
                    break;
                case RoutingEntity.Type.BLE_MESH:
                    connectionTypeList.add("BM");
                    break;
                default:
                    onUserDisconnected(nodeId);
                    return "NA";
            }
        }
        return TextUtils.join(",", connectionTypeList);
    }


    @Override
    public void onTransportInit(String nodeId, String publicKey, TransportState transportState, String msg) {
        boolean isSuccess = transportState == TransportState.SUCCESS;
        if (isSuccess) {
            SharedPref.write(Constant.KEY_USER_ID, nodeId);

            //mTransportManager.configTransport(nodeId, publicKey, APP_PORT);

            //After several time res
            MeshLog.v("Mesh lib started+++++++");
            //mTransportManager.initMeshProcess();

        } else {
            HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(), msg, Toast.LENGTH_SHORT).show());
        }
        if (mStateListener != null) {
            mStateListener.onInit(isSuccess);
        }
    }


    @Override
    public void onLocalUserConnected(String userId, String publicKey) {
        UserModel userModel = ChatDataProvider.On().getUserInfoById(userId);
        MeshLog.v("[BLE_PROCESS] App received local user id :" + AddressUtil.makeShortAddress(userId));
        //reqUserInfo(userId);
        if (userModel == null) {
            reqUserInfo(userId);
        } else {
            userModel.mIsLocallyAlive = true;
            ChatDataProvider.On().upSertUser(userModel);
            discoverUserMap.put(userId, userModel);
            nearbyCallBack.onUserFound(userModel);
        }
    }

    @Override
    public void onRemoteUserConnected(String nodeId, String publicKey) {

        UserModel userModel = ChatDataProvider.On().getUserInfoById(nodeId);

        if (userModel == null) {
            reqUserInfo(nodeId);
        } else {
            userModel.mIsLocallyAlive = true;
            ChatDataProvider.On().upSertUser(userModel);
            discoverUserMap.put(nodeId, userModel);
            nearbyCallBack.onUserFound(userModel);
        }

    }

    @Override
    public void onUserDisconnected(String nodeId) {
        UserModel userModel = discoverUserMap.get(nodeId);
        if (userModel != null) {
            userModel.mIsLocallyAlive = false;
            ChatDataProvider.On().upSertUser(userModel);
        }
        if (nearbyCallBack != null) {
            nearbyCallBack.onDisconnectUser(nodeId);
        }
    }


    @Override
    public void onMessageDelivered(String messageId, int status, String appToken) {
        HandlerUtil.postForeground(() -> {
            if (status == Constant.MessageStatus.RECEIVED) {
                ChatDataProvider.On().updateMessageAck(messageId, status);
                if (messageListener != null) {
                    messageListener.onMessageDelivered();
                }
                if (mMeshStateListener != null) {
                    mMeshStateListener.onMessageReceived(messageId);
                }
            } else if (status == Constant.MessageStatus.DELIVERED) {
                int messageStatus = ChatDataProvider.On().getMessageStatus(messageId);
                MeshLog.k("message status from app:: " + messageStatus);
                if (messageStatus != Constant.MessageStatus.RECEIVED) {
                    ChatDataProvider.On().updateMessageAck(messageId, status);
                }
                if (messageListener != null) {
                    messageListener.onMessageDelivered();
                }
            } else if (status == Constant.MessageStatus.SEND) {
                if (requestUserInfoList.containsKey(messageId)) {
                    String nodeId = requestUserInfoList.get(messageId);
                    UserModel userModel = ChatDataProvider.On().getUserInfoById(nodeId);
                    if (userModel == null) {
                        UserModel userModel1 = new UserModel();
                        userModel1.setUserId(nodeId);
                        userModel1.setUserName("Anonymous");

                        ChatDataProvider.On().insertUser(userModel1);
                        requestUserInfoList.remove(messageId);
                    } else {
                        // userModel = UserModel.buildUserTempData(nodeId);
                        requestUserInfoList.remove(messageId);
                    }
                }

                int messageStatus = ChatDataProvider.On().getMessageStatus(messageId);
                if (messageStatus != Constant.MessageStatus.DELIVERED && messageStatus != Constant.MessageStatus.RECEIVED) {
                    ChatDataProvider.On().updateMessageAck(messageId, status);
                }
                if (messageListener != null) {
                    messageListener.onMessageDelivered();
                }
            }
        });
    }

    @Override
    public void onMessageDelivered(String messageId, int status) {
        HandlerUtil.postForeground(() -> {
            if (status == Constant.MessageStatus.RECEIVED) {
                ChatDataProvider.On().updateMessageAck(messageId, status);
                if (messageListener != null) {
                    messageListener.onMessageDelivered();
                }
                if (mMeshStateListener != null) {
                    mMeshStateListener.onMessageReceived(messageId);
                }
            } else if (status == Constant.MessageStatus.DELIVERED) {
                int messageStatus = ChatDataProvider.On().getMessageStatus(messageId);
                MeshLog.k("message status from app:: " + messageStatus);
                if (messageStatus != Constant.MessageStatus.RECEIVED) {
                    ChatDataProvider.On().updateMessageAck(messageId, status);
                }
                if (messageListener != null) {
                    messageListener.onMessageDelivered();
                }
            } else if (status == Constant.MessageStatus.SEND) {
                if (requestUserInfoList.containsKey(messageId)) {
                    String nodeId = requestUserInfoList.get(messageId);
                    UserModel userModel = ChatDataProvider.On().getUserInfoById(nodeId);
                    if (userModel == null) {
                        UserModel userModel1 = new UserModel();
                        userModel1.setUserId(nodeId);
                        userModel1.setUserName("Anonymous");

                        ChatDataProvider.On().insertUser(userModel1);
                        requestUserInfoList.remove(messageId);
                    } else {
                        // userModel = UserModel.buildUserTempData(nodeId);
                        requestUserInfoList.remove(messageId);
                    }
                }

                int messageStatus = ChatDataProvider.On().getMessageStatus(messageId);
                if (messageStatus != Constant.MessageStatus.DELIVERED && messageStatus != Constant.MessageStatus.RECEIVED) {
                    ChatDataProvider.On().updateMessageAck(messageId, status);
                }
                if (messageListener != null) {
                    messageListener.onMessageDelivered();
                }
            }
        });
    }

    @Override
    public void onMessagePayReceived(String sender, byte[] paymentData) {

    }

    @Override
    public void onPayMessageAckReceived(String sender, String receiver, String messageId) {

    }

    @Override
    public void buyerInternetMessageReceived(String sender, String receiver, String messageId,
                                             String messageData, long dataLength, boolean isIncoming, boolean isFile) {

    }

    @Override
    public void onInterruption(int details) {

        switch (details) {
            case LinkStateListener.LOCATION_PROVIDER_OFF:
                mTransportManager.requestPermission();
                break;

            case LinkStateListener.CONNECTED_WITH_GO_BEING_GO:
                Context context = App.getContext();
                Toaster.showLong(context.getString(R.string.warning_text_connected_with_go_being_go));
                break;

            default:
                if (mMeshStateListener != null) {
                    mMeshStateListener.onInterruption(details);
                }
                break;
        }
    }

    @Override
    public void onInterruption(List<String> missingPermissions) {
        mTransportManager.requestPermission(missingPermissions);
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {

    }

    @Override
    public void onBroadcastMessageReceive(Broadcast broadcast) {

    }

    @Override
    public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {

    }

    @Override
    public boolean onBroadcastSaveAndExist(Broadcast broadcast) {
        return false;
    }

    @Override
    public void onReceivedAckSend(String broadcastID, String senderId) {

    }

    @Override
    public void onMessageReceived(String sender, byte[] frameData) {
        try {
            String jsonString = new String(frameData).trim();
            JSONObject jo = new JSONObject(jsonString);
            int dataType = getDataType(jo);
            //  MeshLog.v("****link did recieved frame: ****" + dataType);
            MeshLog.e("[BLE_PROCESS] App received msg type: " + dataType + " Sender: " + AddressUtil.makeShortAddress(sender));
            switch (dataType) {
                case JsonKeys.TYPE_USER_INFO:
                    UserModel userModel = UserModel.fromJSON(jsonString);
                    if (userModel == null) return;
                    userModel.setUserId(sender);
                    MeshLog.mm(" RECEIVED USER INFO => " + userModel.toString());

                    discoverUserMap.put(userModel.getUserId(), userModel);
                    ChatDataProvider.On().upSertUser(userModel);
                    if (nearbyCallBack != null) {
                        userModel.mIsLocallyAlive = true;
                        ChatDataProvider.On().upSertUser(userModel);
                        MeshLog.e("[+] User Added");
                        nearbyCallBack.onUserFound(userModel);
                    } else {
                        MeshLog.mm("Nearby call back object is null ");
                        HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(), "Discovered ::  " +
                                "" + userModel.getUserName(), Toast.LENGTH_SHORT).show());
                    }
                    break;
                case JsonKeys.TYPE_TEXT_MESSAGE:
                    MessageModel messageModel = MessageModel.getMessage(jo);
                    // insert the message into db
                    if (messageModel != null) {
                        UserModel userModel1 = discoverUserMap.get(messageModel.friendsId);
                        MeshLog.k("[Message saved in DB]");
                        messageModel.receiveTime = System.currentTimeMillis();
                        ChatDataProvider.On().insertMessage(messageModel, userModel1);
                        if (messageListener != null) {
                            messageListener.onMessageReceived(messageModel);
                        } else {
                            if (userModel1 != null) {
                                HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(), "From:  " +
                                        "" + userModel1.getUserName() + "\n" + "Text:   " + messageModel.message, Toast.LENGTH_SHORT).show());
                            } else {
                                reqUserInfo(sender);
                            }

                            MeshLog.k("MessageListener call back object is null ");
                        }
                    } else {
                        HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(), "Empty Message model", Toast.LENGTH_SHORT).show());
                    }

                    break;

                case JsonKeys.TYPE_REQ_USR_INFO:
                    MeshLog.v("****Recieve type TYPE_REQ_USR_INFO ****" + sender);
                    HandlerUtil.postBackground(() -> sendMyInfo(sender));
                    break;
            }
        } catch (JSONException e) {
            MeshLog.e("JSONException occurred at connection manager on linkDidReceiveFrame " + e.getMessage());
        }
    }


    public void sendMessage(String receiverId, MessageModel message) {
        String msgJson = MessageModel.buildMessage(message);
        String userId = SharedPref.read(Constant.KEY_USER_ID);
        mTransportManager.sendTextMessage(userId, receiverId, message.messageId, msgJson.getBytes());
    }

    public String sendFile(String receiverId, String filePath, int fileType) {
        if (mMeshFileCommunicator != null) {
            MeshLog.e("FileMessageTest", "File message sending from connection manager");
            String type = "File";
            if (fileType == ChatActivity.IMAGE_MESSAGE) {
                type = "Image";
            } else if (fileType == ChatActivity.VIDEO_MESSAGE) {
                type = "Video";
            }
            return mMeshFileCommunicator.sendFile(receiverId, filePath, type.getBytes(), App.getContext().getPackageName());
        } else {
            MeshLog.e("mMeshFileCommunicator is null for file:" + filePath);
            return null;
        }
    }

    public boolean resumeFile(String fileTransferId, String appToken) {
        return mMeshFileCommunicator.sendFileResumeRequest(fileTransferId, appToken, null);
    }


    public String sendBroadcast(String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String broadcastId = UUID.randomUUID().toString();
                String type = "Image";
                if (filePath == null) {
                    Log.e("File_path", "Path null :");
                }
//                mMeshFileCommunicator.sendBroadcastContent(broadcastId, "Broadcast", filePath,
//                        type.getBytes(), System.currentTimeMillis(), "demo", 0, 0);
            }
        }).start();
        return "skdjg";
    }


    private int getDataType(JSONObject jo) {
        try {
            return jo.getInt(JsonKeys.KEY_DATA_TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void sendMyInfo(String nodeId) {

        if (Text.isNotEmpty(nodeId)) {
            MeshLog.v(" Send info to => " + nodeId.substring(nodeId.length() - 3));
            String userJson = UserModel.getUserJson();
            String userId = SharedPref.read(Constant.KEY_USER_ID);
            UUID uniqueId = UUID.randomUUID();
            mTransportManager.sendTextMessage(userId, nodeId, uniqueId.toString(), userJson.getBytes());
            MeshLog.i("****Send info: my uid: ****" + userId);
            MeshLog.i("****Send info: node id: ****" + nodeId);
        }

    }

    private void reqUserInfo(String nodeId) {

        String userJson = UserModel.buildUserInfoReqJson();
        if (nodeId == null || nodeId.length() < 3) {
            MeshLog.e(" Send info request  to.. =" + nodeId);
        } else {
            MeshLog.e("[BLE_PROCESS] prepare and send user info request =" + nodeId.substring(nodeId.length() - 3));

            String userId = SharedPref.read(Constant.KEY_USER_ID);
            UUID uniqueId = UUID.randomUUID();
            String messageId = uniqueId.toString();


            requestUserInfoList.put(messageId, nodeId);

            mTransportManager.sendTextMessage(userId, nodeId, messageId, userJson.getBytes());
        }
    }

    public <T> void initListener(T... type) {
        if (type == null) return;
        for (T item : type) {
            if (item instanceof NearbyCallBack) {
                nearbyCallBack = (NearbyCallBack) item;
                MeshLog.k("NearBy callback is init");
            }
        }
    }

    private MessageListener messageListener;

    public void initMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    private NearbyCallBack mNearbyCallBack;

    public void initNearByCallBackForChatActivity(NearbyCallBack nearbyCallBack) {
        this.mNearbyCallBack = nearbyCallBack;
    }

    public void networkReformTest() {
    }

    private MeshStateListener mMeshStateListener;

    public void setMeshStateListener(MeshStateListener meshStateListener) {
        this.mMeshStateListener = meshStateListener;
    }


    public void isUserAvailable(String userId, UserState userState) {
        if (mTransportManager != null) {
            mTransportManager.isUserAvailable(userId, userState);
        } else {
            userState.onUserConnected(userId, false);
        }
    }

    private void initFileTransferListener() {
        if (mMeshFileCommunicator != null) {
            mMeshFileCommunicator.setEventListener(new MeshFileEventListener() {
                @Override
                public void onFileProgress(String fileTransferId, int percentProgress, String appToken) {

                    boolean isAppDBUpdated = ChatDataProvider.On().updateMessageProgress(fileTransferId, percentProgress);
                    /*Timber.d("[FileListener]File progress for %s, percentage %s, " +
                                    "isAppDBUpdated:%s", fileTransferId, percentProgress,
                            isAppDBUpdated);*/
                    if (messageListener != null) {
                        messageListener.onFileProgressReceived(fileTransferId, percentProgress);
                    }
                }

                @Override
                public void onFileTransferFinish(String fileTransferId, String appToke) {
                    boolean isAppDBUpdated = ChatDataProvider.On().updateMessageProgress(fileTransferId, 100);
                    Timber.d("[FileListener]File transfer success for %s, " +
                            "isAppDBUpdated:%s", fileTransferId, isAppDBUpdated);
                    if (messageListener != null) {
                        messageListener.onFileTransferEvent(fileTransferId, true);
                    }
                }

                @Override
                public void onFileTransferError(String fileTransferId, String appToke, String errorMessage) {
                    Timber.d("[FileListener]File transfer failed for %s  %s", fileTransferId, errorMessage);
                    if (messageListener != null) {
                        messageListener.onFileTransferEvent(fileTransferId, false);
                    }
                }

                @Override
                public void onFileReceiveStarted(String sourceAddress, String fileTransferId, String filePath, byte[] msgMetaData,
                                                 String appToke) {
                    Timber.d("[FileListener]File path: %s", filePath);
                    MessageModel messageModel = new MessageModel();
                    if (msgMetaData == null) return;

                    String fileType = new String(msgMetaData);
                    if (fileType.equals("Video")) {
                        messageModel.messageType = ChatActivity.VIDEO_MESSAGE;
                    } else {
                        messageModel.messageType = ChatActivity.IMAGE_MESSAGE;
                    }
                    messageModel.messageId = fileTransferId;
                    messageModel.incoming = true;
                    messageModel.message = filePath;
                    messageModel.friendsId = sourceAddress;
                    messageModel.receiveTime = System.currentTimeMillis();

                    UserModel userModel1 = discoverUserMap.get(messageModel.friendsId);
                    boolean isInserted = ChatDataProvider.On().insertMessage(messageModel, userModel1);
                    Timber.d("[FileListener]isInserted:%s", isInserted);

                    if (messageListener != null) {
                        messageListener.onMessageReceived(messageModel);
                    }

                }
            });
        }
    }

    @Override
    public void onMessageForwarded(String sender, String receiver, String messageId, int transferId, byte[] frameData) {

    }

    public void deleteAllUsers() {
        ChatDataProvider.On().deleteAllUsers();
    }

    public void restartMesh(int role) {
        mTransportManager.restart(role);
    }

    public void sendBtClassicMessage(String testMessage) {
        mTransportManager.setBtClassicMessage(testMessage);
    }
}
