package com.w3engineers.mesh.wifi.dispatch;

import android.bluetooth.BluetoothDevice;
import android.os.RemoteException;

import com.w3engineers.mesh.TransportState;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;

import java.util.List;

/**
 * <p>App layer public interface</p>
 * <p>
 * 1. Trigger mesh library start process from App
 * 2. Handle All discovery and messaging related call from library
 * 3. Pass node disconnect event to App
 * </p>
 */

public interface LinkStateListener {

    int USER_DISABLED_BT = 1, USER_DISABLED_WIFI = 2, LOCATION_PROVIDER_OFF = 3;
    int CONNECTED_WITH_GO_BEING_GO = 4;

    /**
     * <h1>After creating ethereum node ID library call this method</h1>
     *
     * @param nodeId         : String ethereum node id
     * @param transportState : enum represent the state of meshLib
     *                       SUCCESS if library able to create or read  ID FAILED otherwise
     * @param msg            : String message reason if failed to start the library
     */

    void onTransportInit(String nodeId, String publicKey, TransportState transportState, String msg);

    /**
     * <p>Local user connected call back</p>
     * After found a local user this method called from library
     *
     * @param nodeId : String ID of connected node
     */
    void onLocalUserConnected(String nodeId, String publicKey);

    /**
     * <p>Other node id from different mesh network</p>
     *
     * @param nodeId : String ID of connected users
     */
    void onRemoteUserConnected(String nodeId, String publicKey);

    /**
     * <p>Called when connection to device is closed explicitly from either side</p>
     * or because device is out of range.
     *
     * @param nodeId : String disconnected node ID
     */
    void onUserDisconnected(String nodeId);

    /**
     * <p>Called when new data frame is received from remote device.</p>
     *
     * @param senderId  : String message sender ID
     * @param frameData : byte array original message
     */
    void onMessageReceived(String senderId, byte[] frameData);

    default void onLogTextReceive(String text) throws RemoteException {
    }

    /**
     * @param senderId
     * @param frameData
     *//*
    void onLocalMessageReceived(String senderId, byte[] frameData);

    *//**
     *
     * @param senderId
     * @param frameData
     *//*
    void onInternetMessageReceived(String senderId, byte[] frameData);*/


    /**
     * <p>Message delivered ack</p>
     *
     * @param messageId : String message sent id
     * @param status    : Integer{
     *                  0 for sending
     *                  1 for sent
     *                  2 for delivered
     *                  3 for received
     *                  4 for failed }
     */
    void onMessageDelivered(String messageId, int status);

    default void onMessageDelivered(String messageId, int status, String appToken) {
    }

    default void onProbableSellerDisconnected(String sellerId) {
    }

    void onMessagePayReceived(String sender, byte[] paymentData);

    void onPayMessageAckReceived(String sender, String receiver, String messageId);

    void buyerInternetMessageReceived(java.lang.String sender, java.lang.String receiver, java.lang.String messageId, String messageData, long dataLength, boolean isIncoming, boolean isFile);

//    default void onCurrentSellerId(String sellerId) {
//    }


    /**
     * This callback is fired if library faces any interruption from system or user or some
     * different condition. Application may show a warning or any counter measurement action upon
     * this event.
     *
     * @param details for which reason interruption occurred, {@code LinkStateListener} has the
     *                constants
     */
    void onInterruption(int details);

    void onInterruption(List<String> missingPermissions);

    default void onServiceApkDownloadNeeded(boolean isNeeded) throws RemoteException {
    }


    void onHandshakeInfoReceived(HandshakeInfo handshakeInfo);

    /**
     * This method is responsible to pass client info json and also the current version
     */

    void onBroadcastMessageReceive(Broadcast broadcast);

    void onBroadcastACKMessageReceived(BroadcastAck broadcastAck);

    boolean onBroadcastSaveAndExist(Broadcast broadcast);

    void onReceivedAckSend(String broadcastID, String senderId);

    default void onUserModeSwitch(String sendId, int newRole, int previousRole) {
    }

    default void onFileMessageReceived(String sender, String message) {
    }

    default void onClientBtMsgSocketConnected(BluetoothDevice bluetoothDevice) {
    }

    default void onBTMessageSocketDisconnect(String userId) {
    }

    default void onBroadcastContentDetailsReceived(String sender, String message) {
    }

    default void onReceivedFilePacket(byte[] data){}

//    default String onBalanceVerify(long fileSize, List<String> hopList){
//        return "";
//    }

    //default void onReceiveBuyerFileMessage(String sender, String messageData){}
}