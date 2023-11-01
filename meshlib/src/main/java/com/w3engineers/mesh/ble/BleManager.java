package com.w3engineers.mesh.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.text.TextUtils;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.ble.message.BleDataListener;
import com.w3engineers.mesh.ble.message.BleInternalMessageCallback;
import com.w3engineers.mesh.ble.message.BleMessageHelper;
import com.w3engineers.mesh.ble.message.BleMessageManager;
import com.w3engineers.mesh.controller.ManagerStateListener;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.wifid.APCredential;
import com.w3engineers.mesh.libmeshx.wifid.WifiCredential;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WifiTransPort;
import com.w3engineers.mesh.wifidirect.WiFiDirectController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BleManager implements BLEDataListener, BleDataListener, BleInternalMessageCallback {

    private final String PREFIX_TAG = "[BLE_PROCESS]";
    private static BleManager sBleManager;
    private final Context mContext;
    private final static Object mLock = new Object();

    private final GattServer mGattServer;
    private final GattClient mGattClient;
    private final BluetoothManager mBluetoothManager;
    private final BluetoothAdapter mBluetoothAdapter;

    private final BLEDataListener bleDataListener;
    private ManagerStateListener managerStateListener;
    private final String myUserId;

    private BleDataListener messageCallback;

    private ConcurrentHashMap<String, BleUserModel> mBleUserMap;

    private final BleMessageManager mBleMessageManager;

    private BleTransport.BleConnectionListener bleConnectionListener;
    private WiFiDirectController wiFiDirectController;

    /**
     * The time limit 10 seconds for initial BLE connection establish time including MTU change
     * <p>
     * next 70 seconds is for wifi connection established time
     * It is only threshold time
     */
    private final long WIFI_CONNECTION_CHECKER_TIME = 70 * 1000L;

    private BleManager(Context context, String userId, BLEDataListener listener) {
        this.mContext = context;
        this.bleDataListener = listener;

        this.myUserId = userId;

        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mGattServer = new GattServer(mBluetoothAdapter, mBluetoothManager, mContext, myUserId, this, this);
        mGattClient = new GattClient(mBluetoothAdapter, mContext, this, this);

        mBleUserMap = new ConcurrentHashMap<>();

        mBleMessageManager = BleMessageManager.getInstance(mContext, this);
        mBleMessageManager.initBleInternalMessageListener(this);
    }

    public static BleManager getInstance(Context context, String userId, BLEDataListener listener) {
        if (sBleManager == null) {
            synchronized (mLock) {
                if (sBleManager == null) {
                    sBleManager = new BleManager(context, userId, listener);
                }
            }
        }
        return sBleManager;
    }

    public static BleManager getInstance() {
        return sBleManager;
    }

    public <T> void initializeObject(T type) {
        if (type instanceof ManagerStateListener) {
            managerStateListener = (ManagerStateListener) type;
        } else if (type instanceof BleTransport.BleConnectionListener) {
            bleConnectionListener = (BleTransport.BleConnectionListener) type;
        } else if (type instanceof WiFiDirectController) {
            wiFiDirectController = (WiFiDirectController) type;
        }
    }

    public void initAllProcess(boolean isForcefulConnection) {
        // For Internet only mode no need to start BLE search and connection
        if (isInternetOnlyModeEnable()) {
            stop();
            return;
        }

        int size = RouteManager.getInstance().getWifiUser().size();
        // For ignoring above line for now. We will remove above line later
        size = 0;
        if (size > 0) {
            stopScan();
            startServer();
        } else {

            MeshLog.v(PREFIX_TAG + " BLE started from beginning");
            // We are resting the BLE internal message queue after restart
            if (mBleMessageManager != null) {
                mBleMessageManager.resetQueue();
            }

            //When node has no force connection then we will start server only
            if (!isForcefulConnection) {
                startServer();
            }


            if (isForcefulConnection) {
                startScan(isForcefulConnection);
            } else {
                int bleUserList = RouteManager.getInstance().getBleUsers().size();
                if (bleUserList <= 0) {
                    startScan(isForcefulConnection);
                }
            }

        }
    }

    public boolean hasForceConnection() {
        return mGattClient.hasForceConnection();
    }

    public void startServer() {
        // For Internet only mode no need to start BLE search and connection
        if (isInternetOnlyModeEnable()) {
            stop();
            return;
        }
        if (BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            mGattServer.startServer();
        }
    }

    public void startScan(boolean isForcefulConnection) {
        // For Internet only mode no need to start BLE search and connection
        if (isInternetOnlyModeEnable()) {
            stop();
            return;
        }
        mGattClient.startScan(isForcefulConnection);
    }

    private boolean isInternetOnlyModeEnable() {
        return PreferencesHelper.on().getDataShareMode() == PreferencesHelper.INTERNET_USER;
    }


    public void setScanFilter(String... userIds) {
        mGattClient.setScanFilter(userIds);
    }

    public void stop() {
        stopServer();
        stopScan();
    }

    public void stopServer() {
        MeshLog.e(PREFIX_TAG + " Server stop call");
        mGattServer.stopServer();
        // Clear ble client device from map
        List<String> removedUserList = new ArrayList<>();
        for (String userEth : mBleUserMap.keySet()) {
            BleUserModel model = mBleUserMap.get(userEth);
            if (model != null && model.isServer) {
                removedUserList.add(userEth);
            }
        }

        for (String userId : removedUserList) {
            mBleUserMap.remove(userId);
            bleConnectionListener.onBleUserDisconnect(userId);
            MeshLog.i(PREFIX_TAG + " Remove device from map after server stop");
        }

    }

    public void stopScan() {
        MeshLog.v(PREFIX_TAG + " Ble scan stop from ble manager");
        mGattClient.stopScan();
    }

    public void restartAdvertise() {
        mGattServer.restartAdvertise();
    }

    public void stopAdvertise() {
        mGattServer.stopAdvertising();
    }

    public void setCredential(String ssid, String password) {
        mGattServer.setCredential(ssid, password);
    }

    public void disconnectGattServer() {
        mGattClient.disconnectGattServer(false);
    }


    public void initBleMessageCallback(BleDataListener callback) {
        this.messageCallback = callback;
    }


    /*
     * Message send method
     * */

    public void sendMessage(String receiverId, byte[] data) {
        if (receiverId == null) {
            // If receiver ID null then we set it empty.
            // So when it is empty the receiver model will be null and message will be failed
            receiverId = "";
        }

        BleUserModel receiverModel = mBleUserMap.get(receiverId);
        // from here we have to get ble device. And pass to server and client
        // In server side will directly use this device
        // In client side by this device we can find gatt for particular server

        if (receiverModel != null) {
            if (receiverModel.isServer) {
                sendDataFromServer(data, receiverModel.device);
            } else {
                sendDataFromClient(data, receiverModel.device);
            }
        } else {
            MeshLog.e(PREFIX_TAG + " receiver id not found: " + receiverId);
            if (messageCallback != null) {
                messageCallback.onGetMessageSendResponse(false);
            }

            if (bleConnectionListener != null) {
                bleConnectionListener.onBleUserDisconnect(receiverId);
            }

            List<RoutingEntity> bleNodeChecker = RouteManager.getInstance().getBleUsers();
            if (bleNodeChecker.size() == 0) {
                initAllProcess(false);
            }
        }
    }

    public void disconnect() {
        for (String userEth : mBleUserMap.keySet()) {
            //BleUserModel bleUserModel = mBleUserMap.get(userEth);
            disconnectNode(userEth);
        }
    }

    public void disconnectNode(String userId) {
        BleUserModel model = mBleUserMap.get(userId);
        if (model != null) {
            MeshLog.e(PREFIX_TAG + " Disconnection called from BLE");
            if (model.isServer) {
                mGattServer.disconnectDevice(model.device);
            } else {
                mGattClient.disconnectDevice(model.device);
            }

            mBleUserMap.remove(userId);

            if (bleConnectionListener != null) {
                bleConnectionListener.onBleUserDisconnect(userId);
            }

            // TODO: 20/5/21  restart BLE if no BLE left

        }
    }

    private void sendDataFromServer(byte[] data, BluetoothDevice device) {
//        MeshLog.v("FILE_SPEED_TEST_4.6 " + Calendar.getInstance().getTime());
        boolean isSuccess = mGattServer.sendDataFromServer(data, device);

        // message not send successfully we will send a error response to driver layer
        if (!isSuccess && messageCallback != null) {
            messageCallback.onGetMessageSendResponse(false);
        }

        if (!isSuccess) {
            // In this section User actually disconnected
            onNodeDisconnected(device);
        }
    }

    private void sendDataFromClient(byte[] data, BluetoothDevice device) {
//        MeshLog.v("FILE_SPEED_TEST_4.7 " + Calendar.getInstance().getTime());
        boolean isSuccess = mGattClient.sendData(data);

        // message not send successfully we will send a error response to driver layer
        if (!isSuccess && messageCallback != null) {
            messageCallback.onGetMessageSendResponse(false);
        }

        if (!isSuccess) {
            // In this section User actually disconnected
            onNodeDisconnected(device);
        }
    }

    /*
     * That receive GATT client and server
     * */
    @Override
    public void onGetCredential(String ssid, String password, String userId) {
       /* if (bleDataListener != null) {
            bleDataListener.onGetCredential(ssid, password);
        }*/
    }

    @Override
    public void onGetServerServerStartStatus(boolean isStarted) {
        if (bleDataListener != null) {
            bleDataListener.onGetServerServerStartStatus(isStarted);
        }
    }


    /**
     * This method is fired for initial discover for client and server. When MTU transferred
     * completed then it call
     *
     * @param isServer the flag who is server and who is client
     * @param device   The connected device object
     */
    @Override
    public void onGetDiscover(boolean isServer, BluetoothDevice device) {
        //we will stop one side server and client for prevent circle connection
        MeshLog.v(PREFIX_TAG + " IS ME SERVER:" + isServer);

        // It is initial connection of a ble and here we will start a timer for checking
        // that BLE need to rescan or not. Why?
        // BLE-BLE connection used multiple purpose
        // 1. decision, 2. communication interface. When make a decision but wifi connection
        // not established we have to rescan it

        startOffWifiConnectionChecker(true);

        if (isServer) {

            // We stopping client side if only it has not any connection
            if (RouteManager.getInstance().getBleUsers().isEmpty()) {
                disconnectGattServer();
            }


            stopScan();
            stopAdvertise();
        } else {
            //Todo check has any ble client exists in map or not

            // We have to improve it
            stopServer();
        }

    }

    @Override
    public void onGetClientSideDiscover(String userId, BluetoothDevice device, boolean isForceConnection) {

        BleUserModel model = new BleUserModel();
        model.device = device;
        model.isServer = false;
        if (mBleUserMap == null) {
            mBleUserMap = new ConcurrentHashMap<>();
        }
        mBleUserMap.put(userId, model);
        MeshLog.e(PREFIX_TAG + " client side BLE connection put : " + userId);

        if (isForceConnection) {

            String fileUserId = TransportManagerX.getInstance().fileUserId();

            if (TextUtils.isEmpty(fileUserId)) {
                mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.FORCE_CONNECTION,
                        JsonDataBuilder.prepareBleForceConnectionMessage(myUserId, true, false));
            } else {
                if (fileUserId.equals(userId)) {
                    MeshLog.v(PREFIX_TAG + " I found my receiver to send file");

                    mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.FORCE_CONNECTION,
                            JsonDataBuilder.prepareBleForceConnectionMessage(myUserId, true, false));

                } else {
                    MeshLog.v(PREFIX_TAG + " It is not my receiver id");
                }

            }


        } else {
            // Here initial discover completed for BLE. And it is the client side.
            // And now client will send first decision message from here

            if (RouteManager.getInstance().getBleUsers().size() > 0) {
                MeshLog.i(PREFIX_TAG + " this ble has already a client connection. So disconnecting");
                mGattClient.disconnectDevice(device);
                return;
            }

            // Decision message will contain etherium address and current node count
            byte[] data = JsonDataBuilder.prepareBleDecisionMessage(myUserId);

            mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.CLIENT_IDENTITY, data);
        }
    }

    @Override
    public void onGetNewNode(String userId, BluetoothDevice device, boolean isServer) {

       /* BleUserModel model = new BleUserModel();
        model.device = device;
        model.isServer = isServer;
        mBleUserMap.put(userId, model);

        if (!isServer) {
            // Send identity message
            mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.CLIENT_IDENTITY, myUserId.getBytes());
        }

        if (bleConnectionListener != null) {
            bleConnectionListener.onBleUserConnected(isServer, userId);
        }*/
    }

    @Override
    public void onNodeDisconnected(BluetoothDevice device) {
        String removeUserId = getUserIdByDevice(device);
        // Remove device from ble user map
        mBleUserMap.remove(removeUserId);
        MeshLog.e(PREFIX_TAG + " BLE user disconnected: " + removeUserId);

        if (Text.isNotEmpty(removeUserId)) {

            if (removeUserId.equals(internalDisconnectId)) {
                internalDisconnectId = null;
            } else {
                bleConnectionListener.onBleUserDisconnect(removeUserId);
            }

            // Restart BLE if force connection not enabled
            // If file mode enabled then not need scan after disconnect

            if (!hasForceConnection() && !isFileModeEnabled()) {
                if (RouteManager.getInstance().getBleUsers().size() <= 0) {
                    stop();
                    initAllProcess(hasForceConnection());
                }
            }
        }

    }

    /**
     * Update node device when this device object changed for BLE server side.
     * We will pass this object in BleManager layer and update the bleUserMap.
     * Means we use updated BluetoothDevice object
     *
     * @param device link{@BluetoothDevice} from server
     */

    @Override
    public void onNodeUpdate(BluetoothDevice device) {
        String userId = getUserIdByDevice(device);
        if (Text.isNotEmpty(userId)) {
            BleUserModel userModel = mBleUserMap.get(userId);

            if (userModel != null && userModel.isServer) {
                userModel.device = device;
                MeshLog.i(PREFIX_TAG + " Bluetooth device updated. " +
                        "" +
                        "BLE server updated their own device");
            }
        }
    }

    @Override
    public void onGetMyMode(boolean isServer, boolean isForceConnection) {
        // Now we are turing off server if client got an connection
        // This will prevent circle connection
        if (isServer) {
            //stopServer();
        }
        if (bleDataListener != null) {
            bleDataListener.onGetMyMode(isServer, isForceConnection);
        }
    }

    @Override
    public void onClientSideDisconnected() {
        List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();
        if (!bleUserList.isEmpty()) {
            RoutingEntity routingEntity = bleUserList.get(0);
            /*BleUserModel userModel = mBleUserMap.get(routingEntity.getAddress());
            if(userModel!=null && !userModel.isServer) {
                //bleConnectionListener.onBleUserDisconnect(routingEntity.getAddress());
            }*/

            //The client side has ping mechanism so if any disconnect happen we will got from that
        }
    }

    /*
     * Ble message callback
     * */

    @Override
    public void onGetMessageSendResponse(boolean isSuccess) {
        if (messageCallback != null) {
            messageCallback.onGetMessageSendResponse(isSuccess);
        }
    }

    @Override
    public void onGetMessage(String senderId, BluetoothDevice device, byte[] partialData) {
        // We ignoring it from here. It is not using here

    }

    @Override
    public void onGetRawMessage(BluetoothDevice device, byte[] rawData) {
        String userId = getUserIdByDevice(device);

//        MeshLog.v("[BLE_PROCESS] raw data received: " + (new String(rawData)));
        if (messageCallback != null) {
            messageCallback.onGetMessage(userId, device, rawData);
        }
    }

    /*
     * Ble internal message will be receive here.
     * */

    @Override
    public void onReceiveIdentityMessage(String data, BluetoothDevice device) {
       /* MeshLog.v(PREFIX_TAG + " User eth received: " + userId);
        onGetNewNode(userId, device, true);*/
        MeshLog.v(PREFIX_TAG + " decision message received: " + data + " Myhash: " + getMyGoAddressHash());
        try {
            JSONObject jsonObject = new JSONObject(data);
            String userId = jsonObject.optString(JsonDataBuilder.ETH_ID);
            int otherCount = jsonObject.optInt(JsonDataBuilder.MY_NODE_COUNT);
            int otherGoHash = jsonObject.optInt(JsonDataBuilder.GO_ADDRESS_HASH);
            int otherUserMode = jsonObject.optInt(JsonDataBuilder.MY_CURRENT_MODE);

            //Todo We have to compare other user mode to self mode. And take decision
            // that subnet merge required or not (This section is paused now)

            // we have to check already we have any device in map or not and it isServer true or not
            if (hasAnyClient()) {
                MeshLog.i(PREFIX_TAG + " We have already a connection to process.");

                // Disconnecting new request
                mGattServer.disconnectDevice(device);

                // Send internal disconnection message where gatt client will disconnect.
                // Gatt server disconnect is broken internally (System fault)
                // FIXME: 5/31/2021 send and internal message to second client for disconnection
                return;
            }


            if (!mBleUserMap.containsKey(userId)) {
                // Now send same data to client side.
                // This is server section

                //Todo we can check here as a client I have any force connection or not

                // here we are checking that any BLE connection active or not
                /*if (RouteManager.getInstance().getBleUsers().size() > 0) {

                    // Below line of codes are now. DON'T Remove it. May be we need this later

                    // It is server side
                   *//* startOffWifiConnectionChecker(false);
                    MeshLog.i(PREFIX_TAG + " This device has already a valid connection. So disconnecting current connection.");
                    mGattServer.disconnectDevice(device);
                    restartAdvertise();
                    return;*//*
                }*/

                BleUserModel model = new BleUserModel();
                model.device = device;
                model.isServer = true;
                MeshLog.e("[BLE_PROCESS] master side BLE connection put : " + userId);
                mBleUserMap.put(userId, model);

                mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.CLIENT_IDENTITY, JsonDataBuilder.prepareBleDecisionMessage(myUserId));
            }

            RoutingEntity existingEntity = RouteManager.getInstance().getEntityByAddress(userId);
            if (existingEntity != null && existingEntity.isOnline()) {
                // extra checking that that user already connected by wifi or not

                disconnectNode(userId);
                MeshLog.v(PREFIX_TAG + " This is user already connected " + AddressUtil.makeShortAddress(userId));
                return;
            }

            /*
             * Need to make decision
             * */

            int myCount = RouteManager.getInstance().getWifiUser().size();

            MeshLog.v(PREFIX_TAG + " my count: " + myCount + " other count = " + otherCount);
            if (myCount == 0 && otherCount == 0) {

                MeshLog.v(PREFIX_TAG + " My both node count 0: GO/LC decision will be start here");

                BleUserModel bleUserModel = mBleUserMap.get(userId);
                if (bleUserModel != null && bleDataListener != null) {
                    bleDataListener.onGetMyMode(bleUserModel.isServer, false);

                }

            } else if (myCount < otherCount && otherCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                MeshLog.v(PREFIX_TAG + " My node now is LC and start searching");
                if (bleDataListener != null) {
                    // bleDataListener.onGetMyMode(false, false);
                }
                if (bleDataListener != null) {
                    // bleDataListener.onGetCredential(String.valueOf(otherGoHash),"");
                }
            } else if (myCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER && myCount > otherCount
                    && (WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER - myCount) >= otherCount) { // If we want set the main limit is 1 then we can comment out the last condition

                if (wiFiDirectController.getMyApCredential() != null) {
                   /* if (isFileModeEnabled()) {
                        MeshLog.v("[BLE_PROCESS] I'm already file sending mode");
                        return;
                    }*/
                    // the credential sender not need to track ble
                    //startOffWifiConnectionChecker(false);
                    APCredential credential = wiFiDirectController.getMyApCredential();
                    MeshLog.v(PREFIX_TAG + " Sending credential message from here: " + credential.toString());


                    byte[] messageData = JsonDataBuilder.prepareCredentialMessage(credential.mSSID, credential.mPassPhrase, myUserId);
                    mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.CREDENTIAL_MESSAGE, messageData);

                    if (P2PUtil.isMeLC()) {
                        // If it is LC we can start ble advertisement here. This LC's main
                        // task is to pass SSID and password
                        // before restart we have to check have any valid ble connection
                        boolean hasBleUser = RouteManager.getInstance().getBleUsers().size() > 0;

                        // remove the ble tracker runnable
                        //startOffWifiConnectionChecker(false);

                        if (hasBleUser) {
                            restartAdvertise();
                        } else {
                            initAllProcess(hasForceConnection());
                        }
                    }
                } else {
                    MeshLog.e(PREFIX_TAG + " Credential null before sending");
                }


            } else if (myCount == otherCount && myCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER
                    && (WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER - myCount) >= otherCount) { // If we want set the main limit is 1 then we can comment out the last condition
                if (getMyGoAddressHash() > otherGoHash) {

                    if (wiFiDirectController.getMyApCredential() != null) {
                        // the credential sender will restart the tracker
                        // Because already some second counted. lets say 10 seconds lost
                        // within 60 seconds other device need to connect and and transfer node info
                        // So we will add 10 seconds again and restart tracker to prevent
                        // send credential to another node

                        startOffWifiConnectionChecker(true);

                        APCredential credential = wiFiDirectController.getMyApCredential();
                        MeshLog.v(PREFIX_TAG + " Sending credential message from here: " + credential.toString());

                        /*String message = credential.mPassPhrase + credential.mSSID;
                        mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.CREDENTIAL_MESSAGE, message.getBytes());*/

                        byte[] messageData = JsonDataBuilder.prepareCredentialMessage(credential.mSSID, credential.mPassPhrase, myUserId);
                        mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.CREDENTIAL_MESSAGE, messageData);

                        if (P2PUtil.isMeLC()) {
                            // If it is LC we can start ble advertisement here. This LC's main
                            // task is to pass SSID and password

                            // The blewo BLE restart will off. Cause if we restart again then it will
                            // connect another device and send credential which
                            // may create a problem to connect GO node
                            // The BLE will restart after tracker end

                           /* // before restart we have to check have any valid ble connection
                            boolean hasBleUser = RouteManager.getInstance().getBleUsers().size() > 0;

                            if (hasBleUser) {
                                restartAdvertise();
                            } else {
                                initAllProcess(hasForceConnection());
                            }*/
                        }
                    } else {
                        MeshLog.e(PREFIX_TAG + " Credential null before sending");
                    }

                } else {
                    MeshLog.e(PREFIX_TAG + " BLE user not server!!");
                    if (bleDataListener != null) {
                        //bleDataListener.onGetCredential(String.valueOf(otherGoHash),"");
                    }
                }
            } else {
                BleUserModel bleUserModel = mBleUserMap.get(userId);
                MeshLog.e(PREFIX_TAG + " Node info sending process start from BLE");
                if (bleUserModel != null) {
                    // For node sync moment not need to track ble
                    //startOffWifiConnectionChecker(false);

                    if (bleConnectionListener != null) {
                        bleConnectionListener.onBleUserConnected(bleUserModel.isServer, userId);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveCredentialMessage(String userId, String password, String ssid) {
        MeshLog.v(PREFIX_TAG + " Credential message received ssid: " + ssid + " password: " + password);
        if (isFileModeEnabled()) {
            MeshLog.v("[BLE_PROCESS] I'm already file sending mode");
            return;
        }
        if (bleDataListener != null) {
            bleDataListener.onGetCredential(ssid, password, userId);
        }
    }

    @Override
    public void onMessageSendingStatus(String messageId, boolean isSuccess) {
        MeshLog.v(PREFIX_TAG + " Ble internal message sending status: " + isSuccess);
    }

    @Override
    public void onGetForceConnectionRequest(String data, BluetoothDevice device) {
        try {
            MeshLog.v(PREFIX_TAG + " Received force connection message");
            JSONObject jsonObject = new JSONObject(data);
            String userId = jsonObject.optString(JsonDataBuilder.ETH_ID);
            boolean isForceConnection = jsonObject.optBoolean(JsonDataBuilder.FORCE_CONNECTION);
            boolean isServer = jsonObject.optBoolean(JsonDataBuilder.IS_SERVER);

            BleUserModel model = new BleUserModel();
            model.device = device;
            model.isServer = true;
            mBleUserMap.put(userId, model);

            // This side may be server or client. But Generally it is server side
            if (isForceConnection && bleDataListener != null) {
                // Note: We don't need to store the requester id because
                // this connection not effect in routing table
                mBleMessageManager.sendMessage(userId, BleMessageHelper.MessageType.FORCE_CONNECTION_REPLY,
                        JsonDataBuilder.prepareBleForceConnectionReply(myUserId, !isFileModeEnabled()));

                // trigger GO here because a force connection request come.
                // Before trigger just check that any file mode enabled or not.

                // Other sender will search.


                if (!isFileModeEnabled()) {
                    MeshLog.v(PREFIX_TAG + " Here a force connection request come. It should be GO");
                    bleDataListener.onGetMyMode(model.isServer, true);

                    TransportManagerX.getInstance().setSpecificNodeIdWhoWantsToConnect(userId);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onGetForceConnectionReply(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            String userId = jsonObject.optString(JsonDataBuilder.ETH_ID);
            boolean isAvailable = jsonObject.optBoolean(JsonDataBuilder.IS_ABLE_TO_RECEIVE);

            if (isAvailable && bleDataListener != null) {
                bleDataListener.onStatSpecialSearch(userId);
            }

            if (!isAvailable) {
                MeshLog.v(PREFIX_TAG + " The user is busy");
                // We have to execute next queue
                TransportManagerX.getInstance().executeNextFileItemInQueue(userId);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private String getUserIdByDevice(BluetoothDevice device) {
        String userId = "";
        // Maybe the user id not included in user map
        for (String userEth : mBleUserMap.keySet()) {
            BleUserModel bleUserModel = mBleUserMap.get(userEth);
            if (bleUserModel != null && bleUserModel.device.getAddress().equalsIgnoreCase(device.getAddress())) {
                userId = userEth;
                break;
            }
        }
        return userId;
    }

    private boolean hasAnyClient() {

        for (String userEth : mBleUserMap.keySet()) {
            BleUserModel bleUserModel = mBleUserMap.get(userEth);
            if (bleUserModel != null && bleUserModel.isServer) {
                return true;
            }
        }

        return false;
    }

    /*
     * GO connection checker runnable
     * */

    public void startOffWifiConnectionChecker(boolean isOn) {
        if (isOn) {
            HandlerUtil.postBackground(goLcConnectionChecker, WIFI_CONNECTION_CHECKER_TIME);
        } else {
            HandlerUtil.removeBackground(goLcConnectionChecker);
        }
    }

    /**
     * First check that any valid BLE connection established or not
     * Valid-> BLE-BLE connection maintaining in routing table
     * <p>
     * if no valid connection it restart
     */
    private final Runnable goLcConnectionChecker = () -> {
        boolean isBleUserConnected = RouteManager.getInstance().getBleUsers().size() > 0;
        //Todo tariqul we have to check that node sync running or not

        if (isBleUserConnected) {
            stopScan();
            restartAdvertise();
            MeshLog.v(PREFIX_TAG + " After tack BLE will now special advertise");
        } else {
            MeshLog.v(PREFIX_TAG + " Start BLE from beginning");
            initAllProcess(hasForceConnection());
        }


        //MeshLog.v(PREFIX_TAG + " GO-LC connection checker, has wifi connection " + isWifiUserConnected);
    };

    /**
     * Internal disconnect handle
     */
    private static String internalDisconnectId;

    public List<RoutingEntity> disconnectUserInternally(String senderAddress) {
        internalDisconnectId = senderAddress;


        List<RoutingEntity> bleUserList = RouteManager.getInstance().getBleUsers();

        for (RoutingEntity entity : bleUserList) {
            if (entity.getAddress().equals(senderAddress)) {
                disconnectNode(senderAddress);
                return RouteManager.getInstance().updateNodeAsOffline("", entity.getAddress());
            }
        }
        return null;
    }

    private int getMyGoAddressHash() {
        if (P2PUtil.isMeGO()) {
            return myUserId.hashCode();
        } else {
            RoutingEntity routingEntity = RouteManager.getInstance().getGoRoutingEntityByIp(WifiTransPort.P2P_MASTER_IP_ADDRESS);
            if (routingEntity != null) {
                return routingEntity.getAddress().hashCode();
            }
        }
        return 0;
    }

    private boolean isFileModeEnabled() {
        return TransportManagerX.getInstance().isFileSendingMode()
                || TransportManagerX.getInstance().isFileReceivingMode();
    }

    private void processForceConnectionMessage(String receiverId) {
        String ssid = "";
        String password = "";
        if (P2PUtil.isMeGO()) {
            int wifiUserCount = RouteManager.getInstance().getWifiUser().size();
            if (wifiUserCount > 1 && wifiUserCount < WifiTransPort.GO_MAXIMUM_CLIENT_NUMBER) {
                ssid = WifiCredential.ssid;
                password = WifiCredential.password;
            }
        }


    }

    private byte[] prepareBleForceConnectionMessage(String receiverId, String ssid, String password,
                                                    boolean isRequest, boolean isAbleToReceive) {
        return JsonDataBuilder.prepareForceConnectionMessage(myUserId, receiverId, ssid, password,
                isRequest, isAbleToReceive);
    }

    /**
     * Pinger is used to track BLE-BLE connection
     * As we know Gatt client don't bother GATT server is alive or not.
     * So if somehow GATT server disconnect then it is really hard to track in client side.
     * <p>
     * So pinger will ping every {@link BleManager#WIFI_CONNECTION_CHECKER_TIME} seconds
     * to check the server is alive or not.
     * <p>
     * It is only need to manage route path and also help to manage multiple path in routing
     */
    public void startPinger() {
        MeshLog.v(PREFIX_TAG + " Ble pinger started");
        HandlerUtil.postBackground(Pinger, WIFI_CONNECTION_CHECKER_TIME);
    }

    private final Runnable Pinger = new Runnable() {
        @Override
        public void run() {
            if (mBleUserMap.size() > 0) {
                for (String userEth : mBleUserMap.keySet()) {

                    List<RoutingEntity> userList = RouteManager.getInstance().getUsersByType(RoutingEntity.Type.BLE);
                    RoutingEntity routingEntity = null;
                    if (userList != null && !userList.isEmpty()) {
                        routingEntity = userList.get(0);
                    }

                    if (routingEntity != null && routingEntity.isOnline() && routingEntity.getType() == RoutingEntity.Type.BLE) {
                        BleUserModel bleUserModel = mBleUserMap.get(userEth);
                        if (bleUserModel != null && !bleUserModel.isServer) {
                            mBleMessageManager.sendMessage(userEth, BleMessageHelper.MessageType.BLE_HEART_BIT, "p".getBytes());
                            HandlerUtil.postBackground(Pinger, WIFI_CONNECTION_CHECKER_TIME);
                        }
                    } else {
                        MeshLog.e(PREFIX_TAG, "User not in table");
                    }

                }
            } else {
                MeshLog.v(PREFIX_TAG + " Ble pinger no data found");
                List<RoutingEntity> allBleUsers = RouteManager.getInstance().getBleUsers();
                if (!allBleUsers.isEmpty()) {
                    MeshLog.e(PREFIX_TAG + " ble data exists in routing table but map empty");
                    for (RoutingEntity entity : allBleUsers) {
                        bleConnectionListener.onBleUserDisconnect(entity.getAddress());
                    }
                }

                List<RoutingEntity> bleNodeChecker = RouteManager.getInstance().getBleUsers();
                if (bleNodeChecker.size() == 0) {
                    initAllProcess(hasForceConnection());
                }
            }
        }
    };
}
