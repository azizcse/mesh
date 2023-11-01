package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.w3engineers.mesh.LinkMode;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.wifi.protocol.Link;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <h1>All types of connection disconnection related message are managed here</h1>
 * <p>All transport related updated are notified through this interface</p>
 */
public interface ConnectionStateListener {

    default void onReceivedDirectUserFromWifiClient(String senderInfo, String btUser, String btMeshUser) {
    }

    default void onReceivedDirectUserFromAdHocListener(String senderInfo, String btUser, String btMeshUser) {
    }

    default void onReceivedDirectUserFromWifiMaster(String senderInfo, String btUser, String btMeshUser, String wifiUser, String wifiMeshUser) {
    }

    default void onReceivedDirectUserFromAdHocBroadcaster(String senderInfo, String btUser, String btMeshUser, String wifiUser, String wifiMeshUser) {
    }

    default void onGetInfoRequest(String senderInfo, String btUser, String btMeshUser) {
    }

    /**
     * @param link
     */
    default void onDisconnectLink(Link link) {
    }

    /**
     * @param senderId
     * @param jsonString
     * @param hopId
     */
    void onMeshLinkFound(String senderId, String hopId, String jsonString);

    /**
     * @param nodeId
     */
    void onMeshLinkDisconnect(String nodeId, String forwarderId);


    /**
     * @param sender
     * @param receiver
     * @param messageId
     * @param data
     * @param ipAddress
     */
    void onMessageReceived(String sender, String receiver, String messageId, byte[] data, String ipAddress, String immediateSender);


    default void onPaymentDataReceived(String sender, String receiver, String messageId, byte[] payData) {
    }


    default void onPaymentAckReceived(String sender, String receiver, String messageId) {
    }


    /**
     * <h1>Message ACK receiver</h1>
     *
     * @param sender    : String
     * @param receiver  : String
     * @param messageId :ling
     * @param ipAddress
     */
    void onReceivedMsgAck(String sender, String receiver, String messageId, int status, String ackBody, String ipAddress);

    default void onReceiveRouteInfoUpdate(String routeInfoFor, String newHopAddress, long time) {
    }

    default void onReceiveDisconnectRequest(String senderAddress) {
    }

    /**
     * @param userNodeInfoList
     */
    default void onInternetUserReceived(String nodeId, ConcurrentLinkedQueue<RoutingEntity> userNodeInfoList) {
    }

    default void onInternetDirectUserReceived(String nodeId, ConcurrentLinkedQueue<NodeInfo> userNodeInfoList, Link link) {
    }

    default void onReceiverInternetUserLocally(String sender, String receiver, String sellerId, String userIdList) {
    }

    default void onReceiveMessageStatus(String senderId, String receiverId, String messageId, int status) {
    }


    default void onReceivedDirectUserListInBt(String senderInfo, String wifiUser, String wifiMeshUser,
                                              BleLink bleLink, LinkMode linkMode) {
    }

    void onInternetUserLeave(String sender, String receiver, String userList);

    void onHandshakeInfoReceived(HandshakeInfo handshakeInfo);

    default void onReceiveNewRole(String sender, String receiver, int role) {
    }

    void onFileMessageReceived(String sender, String receiver, String messageId, String message, String immediateSender);

    void onBroadcastReceived(Broadcast broadcast);

    void onBroadcastACKMessageReceived(BroadcastAck broadcastAck);

    default void onClientBtMsgSocketConnected(BluetoothDevice bluetoothDevice) {
    }


    //Mesh v2 node sync

    default void onV2ReceivedHelloFromClient(String sendInfo, String nodeIds, String onLineIds, String dataId) {
    }

    default void onV2ReceivedHelloFromMaster(String senderInfo, String lcOnlineNodes, String otherOnlineNodes,
                                             String offlineNodes, String dataId) {
    }

    default void onV2ReceivedHelloFromMaster(String senderInfo, String onlineMeshNodes, String offlineNodes, String dataId) {
    }

    default void onV2ReceivedMeshUsers(String sender, String onlineNodes, String offlineNodes, String dataID) {
    }

    default void onV2CredentialReceived(String sender, String receiver, String ssid, String password, String goNodeId) {
    }

    void onV2BleMeshDecisionMessageReceive(String sender, String receiver, String ssid,
                                           String password, int wifiNodeCount, boolean isFirstMessage);

    default void onV2ForceConnectionMessage(String sender, String receiver, String ssid, String password,
                                            boolean isRequest, boolean isAbleToReceive) {
    }

    default void onV2GetFileFreeModeMessage(String sender, String receiver, boolean isAbleToReceive) {
    }

    default void onV2ReceivedGoNetworkFullResponse(String senderId) {
    }

    default void onV2ReceiveSpecialDisconnectMessage(String receiverId, String duplicateId) {
    }

    default void onV2ReceivedFailedMessageAck(String source, String destination, String hop, String messageId) {
    }

    default void onReceivedFilePacket(byte[] data){}

    //internet
    default void onReceivedRemoteUsersId(List<String> usersId) {
    }

    default void onReceivedUserDetailsFromRemoteUser(String senderInfo, String localUsers) {
    }
    default void onReceivedUserDetailsResponseFromRemoteUser(String senderInfo, String localUsers) {
    }

    default void onReceivedRemoteUsersLeaveId(String userId) {
    }

    default void onReceiveBuyerFileMessage(String sender, String receiver, String messageData, int fileMessageType, String immediateSender, String messageId){}
}

