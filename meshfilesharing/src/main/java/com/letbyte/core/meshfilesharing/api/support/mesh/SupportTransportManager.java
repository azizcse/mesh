package com.letbyte.core.meshfilesharing.api.support.mesh;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;

import com.letbyte.core.meshfilesharing.api.MeshFileCommunicator;
import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.data.BaseMessage;
import com.letbyte.core.meshfilesharing.data.MessageProcessor;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.TransportState;
import com.w3engineers.mesh.httpservices.MeshHttpServer;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.ForwardListener;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;

import java.util.List;

import timber.log.Timber;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 1:29 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 1:29 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 1:29 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class SupportTransportManager implements ForwardListener {
    private static final SupportTransportManager ourInstance = new SupportTransportManager();
    private FileHelper mFileHelper;
    public String mDefaultStorageDirectory;
    private MessageProcessor mMessageProcessor;

    public static SupportTransportManager getInstance() {
        return ourInstance;
    }

    private MeshFileManager mMeshFileManager;
    private ForwardListener forwardListener;
    private MeshFileHelper mMeshFileHelper;
    private Context mContext;

    private SupportTransportManager() {
    }

    public MeshFileCommunicator getMeshFileCommunicator() {
        return mMeshFileManager;
    }

    public void initForwardListener(ForwardListener forwardListener) {
        this.forwardListener = forwardListener;
    }

    public TransportManagerX getTransportManager(Context context, int appPort, String address,
                                                 String publicKey,
                                                 String networkPrefix,
                                                 String multiverseUrl,
                                                 LinkStateListener linkStateListener) {

        mFileHelper = new FileHelper();
        mMeshFileHelper = new MeshFileHelper();
        this.mContext = context;

        TransportManagerX transportManagerX = TransportManagerX.on(context, appPort,
                address, publicKey, networkPrefix,
                multiverseUrl, new LinkStateListener() {

                    @Override
                    public void onTransportInit(String nodeId, String publicKey, TransportState transportState, String msg) {
                        MeshHttpServer.on().start(appPort, mMeshFileManager);
                        linkStateListener.onTransportInit(nodeId, publicKey, transportState, msg);
                    }


                    @Override
                    public void onLocalUserConnected(String nodeId, String userInfo) {
                        linkStateListener.onLocalUserConnected(nodeId, userInfo);
                    }

                    @Override
                    public void onRemoteUserConnected(String nodeId, String userInfo) {
                        linkStateListener.onRemoteUserConnected(nodeId, userInfo);
                    }

                    @Override
                    public void onUserDisconnected(String nodeId) {
                        linkStateListener.onUserDisconnected(nodeId);
                        if (mMeshFileManager !=null){
                            mMeshFileManager.onNodeLeave(nodeId);
                        }
                    }

                    @Override
                    public void onMessageReceived(String senderId, byte[] frameData) {
                        MeshLog.e("FileMessageTest", "Message receive in support transport layer");
                        linkStateListener.onMessageReceived(senderId, frameData);
                        // check it is file message or not
                        /*BaseMessage message = BaseMessage.toBaseMessage(frameData);

                        //For file messages as we sent message hop by hop so this sender is always
                        // the immediate sender address rather the source address
                        if (!mMessageProcessor.processMessage(senderId, message)) {
                            linkStateListener.onMessageReceived(senderId, frameData);
                        }*/
                    }

                    @Override
                    public void onLogTextReceive(String text) throws RemoteException {
                        linkStateListener.onLogTextReceive(text);
                    }

                    @Override
                    public void onMessageDelivered(String messageId, int status) {
                        linkStateListener.onMessageDelivered(messageId, status);
                    }

                    @Override
                    public void onMessageDelivered(String messageId, int status, String appToken) {
                        linkStateListener.onMessageDelivered(messageId, status, appToken);
                    }

                    @Override
                    public void onProbableSellerDisconnected(String sellerId) {
                        linkStateListener.onProbableSellerDisconnected(sellerId);
                    }

                    @Override
                    public void onMessagePayReceived(String sender, byte[] paymentData) {
                        linkStateListener.onMessagePayReceived(sender, paymentData);
                    }

                    @Override
                    public void onPayMessageAckReceived(String sender, String receiver, String messageId) {
                        linkStateListener.onPayMessageAckReceived(sender, receiver, messageId);
                    }

                    @Override
                    public void buyerInternetMessageReceived(String sender, String receiver, String messageId, String messageData, long dataLength, boolean isIncoming, boolean isFile) {
                        linkStateListener.buyerInternetMessageReceived(sender, receiver, messageId, messageData, dataLength, isIncoming,  isFile);
                    }

//                    @Override
//                    public void onCurrentSellerId(String sellerId) {
//                        linkStateListener.onCurrentSellerId(sellerId);
//                    }

                    @Override
                    public void onInterruption(int details) {
                        linkStateListener.onInterruption(details);
                    }

                    @Override
                    public void onInterruption(List<String> missingPermissions) {
                        linkStateListener.onInterruption(missingPermissions);
                    }

                    @Override
                    public void onServiceApkDownloadNeeded(boolean isNeeded) throws RemoteException {
                        linkStateListener.onServiceApkDownloadNeeded(isNeeded);
                    }

                    @Override
                    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {
                        linkStateListener.onHandshakeInfoReceived(handshakeInfo);
                    }

                    @Override
                    public void onBroadcastMessageReceive(Broadcast broadcast) {
                        linkStateListener.onBroadcastMessageReceive(broadcast);
                    }

                    @Override
                    public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {
                        linkStateListener.onBroadcastACKMessageReceived(broadcastAck);
                    }

                    @Override
                    public boolean onBroadcastSaveAndExist(Broadcast broadcast) {
                        return linkStateListener.onBroadcastSaveAndExist(broadcast);
                    }

                    @Override
                    public void onReceivedAckSend(String broadcastID, String senderId) {
                        linkStateListener.onReceivedAckSend(broadcastID, senderId);
                    }

                    @Override
                    public void onUserModeSwitch(String sendId, int newRole, int previousRole) {
                        linkStateListener.onUserModeSwitch(sendId, newRole, previousRole);
                    }

                    @Override
                    public void onFileMessageReceived(String sender, String message) {
                        BaseMessage baseMessage = BaseMessage.toBaseMessage(message);
                        mMessageProcessor.processMessage(sender, baseMessage);
                    }

                    @Override
                    public void onBroadcastContentDetailsReceived(String sender, String message) {
                        BaseMessage baseMessage = BaseMessage.toBaseMessage(message);
                        mMessageProcessor.processMessage(sender, baseMessage);
                    }

                    @Override
                    public void onClientBtMsgSocketConnected(BluetoothDevice bluetoothDevice) {
                        mMeshFileManager.onBtUserConnected(bluetoothDevice);
                    }


                    @Override
                    public void onBTMessageSocketDisconnect(String userId) {
                        Timber.d("BT Socket disconnect. All BT File process should reset");
                        mMeshFileManager.onBtMessageSocketDisconnect(userId);
                    }

                    @Override
                    public void onReceivedFilePacket(byte[] data) {
                        BaseMessage baseMessage = BaseMessage.toBaseMessage(new String(data));
                        mMessageProcessor.processMessage("", baseMessage);
                    }
                });

        if (mMeshFileManager == null) {
            mMeshFileManager = new MeshFileManager(TransportManagerX.getInstance(), context, linkStateListener);
        }

        mMessageProcessor = new MessageProcessor(context,mFileHelper, DatabaseService.getInstance(context),
                mMeshFileHelper, mMeshFileManager, appPort);

        transportManagerX.initForwardListener(this);

        return transportManagerX;
    }

    @Override
    public void onMessageForwarded(String sourceAddress, String receiver, String messageId, int transferId, byte[] frameData) {
        MeshLog.e("FileMessageTest", "sourceAddress: " + sourceAddress + " receiver: " + receiver);

        // Send to app layer if the has need to data of forwarder
        if (forwardListener != null) {
            forwardListener.onMessageForwarded(sourceAddress, receiver, messageId, transferId, frameData);
        }
    }
}
