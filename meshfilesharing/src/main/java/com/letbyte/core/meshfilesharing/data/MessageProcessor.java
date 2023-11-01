package com.letbyte.core.meshfilesharing.data;

import android.content.Context;
import android.util.Base64;

import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.comm.ble.BleFileManager;
import com.letbyte.core.meshfilesharing.comm.bt.BTFileManager;
import com.letbyte.core.meshfilesharing.comm.fileserver.webserver.HttpFileClient;
import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.data.model.MultihopFileAck;
import com.letbyte.core.meshfilesharing.data.model.MultihopFileMessage;
import com.letbyte.core.meshfilesharing.data.model.MultihopFilePacket;
import com.letbyte.core.meshfilesharing.data.model.PendingFile;
import com.letbyte.core.meshfilesharing.data.model.MultihopResumeRequestFromSender;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-05 at 3:16 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-05 at 3:16 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-05 at 3:16 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class MessageProcessor {

    private FileHelper mFileHelper;
    private DatabaseService mDatabaseService;
    private MeshFileHelper mMeshFileHelper;
    private MeshFileManager mMeshFileManager;
    private TransportManagerX mTransportManagerX;
    private BTFileManager mBTFileManager;
    private BleFileManager mBleFileManager;
    private final int APP_PORT;
    private Context context;

    public MessageProcessor(Context context, FileHelper fileHelper, DatabaseService databaseService, MeshFileHelper meshFileHelper,
                            MeshFileManager meshFileManager, int appPort) {
        this.context = context;
        mFileHelper = fileHelper;
        mDatabaseService = databaseService;
        mMeshFileHelper = meshFileHelper;
        mMeshFileManager = meshFileManager;
        mTransportManagerX = TransportManagerX.getInstance();
        mBTFileManager = meshFileManager.mBTFileManager;
        mBleFileManager = meshFileManager.mBleFileManager;
        this.APP_PORT = appPort;
    }

    public boolean processMessage(String senderAddress, BaseMessage baseMessage) {

        boolean isProcessed = false;
        if (baseMessage instanceof BroadcastMessage) {

            isProcessed = processMessage(senderAddress, (BroadcastMessage) baseMessage);
        } else if (baseMessage instanceof FileMessage) {

            isProcessed = processMessage(senderAddress, (FileMessage) baseMessage);
        } else if (baseMessage instanceof FileAckMessage) {

            isProcessed = processMessage(senderAddress, (FileAckMessage) baseMessage);
        } else if (baseMessage instanceof FileResumeRequestMessageFromReceiver) {

            isProcessed = processMessage((FileResumeRequestMessageFromReceiver) baseMessage);
        } else if (baseMessage instanceof FileRequestResumeMessageFromSender) {

            isProcessed = processMessage(senderAddress, (FileRequestResumeMessageFromSender) baseMessage);
        } else if (baseMessage instanceof FileResendRequestMessage) {
            isProcessed = processMessage(senderAddress, (FileResendRequestMessage) baseMessage);
        } else if (baseMessage instanceof BTFileRequestMessage) {

            isProcessed = processMessage(senderAddress, (BTFileRequestMessage) baseMessage);
        } else if (baseMessage instanceof BleFileMessage) {
            mBleFileManager.onReceiveFilePacket((BleFileMessage) baseMessage);

        } else if (baseMessage instanceof BleFileAckMessage) {
            BleFileAckMessage fileAckMessage = (BleFileAckMessage) baseMessage;
            mBleFileManager.onSendFilePacket(true, fileAckMessage.fileMessageId);

        } else if (baseMessage instanceof BuyerFileMessage) {
            mMeshFileManager.buyerFileManager.onReceiveBuyerFileMessage(senderAddress, (BuyerFileMessage) baseMessage);
        } else if (baseMessage instanceof BuyerFilePacket) {
            mMeshFileManager.buyerFileManager.onReceiveBuyerFilePacket(senderAddress, (BuyerFilePacket) baseMessage);
        } else if (baseMessage instanceof BuyerFileAck) {
            mMeshFileManager.buyerFileManager.onReceiveBuyerFilePacketSendAck(senderAddress, (BuyerFileAck) baseMessage);
        } else if (baseMessage instanceof BuyerFileResumeRequestFromSender) {
            mMeshFileManager.buyerFileManager.onReceivedBuyerFileResumeRequest(senderAddress, (BuyerFileResumeRequestFromSender) baseMessage);
        } else if (baseMessage instanceof MultihopFileMessage) {
            mMeshFileManager.multihopFileManager.onReceivedMultihopFileMessage(senderAddress, (MultihopFileMessage) baseMessage);
        } else if (baseMessage instanceof MultihopFilePacket) {
            mMeshFileManager.multihopFileManager.onReceivedMultihopFilePacket(senderAddress, (MultihopFilePacket) baseMessage);
        } else if (baseMessage instanceof MultihopFileAck) {
            mMeshFileManager.multihopFileManager.onReceivedMultihopFileAck(senderAddress, (MultihopFileAck) baseMessage);
        } else if (baseMessage instanceof MultihopResumeRequestFromSender) {
            mMeshFileManager.multihopFileManager.onReceivedMultihopFileResumeRequest(senderAddress, (MultihopResumeRequestFromSender) baseMessage);
        }


        return isProcessed;
    }

    public boolean processMessage(FileResumeRequestMessageFromReceiver fileResumeRequestMessageFromReceiver) {

        Timber.d("[File-Resume][FileResumeRequestMessage]%s", fileResumeRequestMessageFromReceiver);

        boolean isProcessed;

        if (fileResumeRequestMessageFromReceiver != null &&
                AddressUtil.isValidEthAddress(fileResumeRequestMessageFromReceiver.mPeerAddress) &&
                fileResumeRequestMessageFromReceiver.mRequestingBytesFrom > -1) {

            FilePacket filePacket = mDatabaseService.getFilePackets(
                    fileResumeRequestMessageFromReceiver.mPeerAddress, fileResumeRequestMessageFromReceiver.mFileTransferId);

            // TODO: 7/1/2020 Here best fit solution would be to retain any file for a node unless
            // the destination receive the file. Also do not allow to update selfFilePah from any
            // kind of remote request with DB. Also while FileACK for last byte transferred then
            // file could be deleted by intermediate nodes
            //Fix for not properly deleting or maintaining of file and file packet
            if (filePacket != null && !new File(filePacket.mSelfFullFilePath).exists()) {
                mDatabaseService.deletePacket(filePacket);
                filePacket = null;
                return false;
            }

            //TODO
            //We are being ensured that intermediate node has complete file or not
            //onFragmented File support is effectively paused due to this condition, will resume this
            //Facility upon fixing serialized file transfer of BT-WiFi path
            /*if (filePacket != null && filePacket.mTransferredBytes >
                    fileResumeRequestMessage.mRequestingBytesFrom ||*/
            //If I have the complete file or I am the receiver
            if ((filePacket != null && filePacket.mTransferredBytes >=
                    filePacket.mFileSize && filePacket.mFileSize > 0) ||
                    fileResumeRequestMessageFromReceiver.mPeerAddress.equals(mTransportManagerX.getMyNodeId())) {
                //This particular node has the data or is the destination So it can start sending
                // data on it's own

                if (filePacket == null) {

                    //Ideally this should not happen.
                    //As If me is source and any entry is not successful yet we do not delete that
                    //entry
                    MeshLog.e("[File-Resume]File entry not available.");

                } else {

                    Timber.e("[File-Resume]File resume start for failed file");

                    String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                            filePacket.mFileId);
//                    mMeshFileManager.mFileBytesCountMap.put(fileId, 0L);

                    long transferredBytes = filePacket.mTransferredBytes;
                    Timber.e("[File-Resume]T bytes: " + filePacket.mTransferredBytes + " request size: " + fileResumeRequestMessageFromReceiver.mRequestingBytesFrom);


                    filePacket.mTransferredBytes = fileResumeRequestMessageFromReceiver.mRequestingBytesFrom;
                    //send data back to this node without forwarding further
                   /* FileMessage fileMessage = mMeshFileHelper.get(filePacket);
                    fileMessage.messageMetaData = fileResumeRequestMessage.messageMetaData;*/

                    PendingFile pendingFile = new PendingFile(filePacket, filePacket.mSourceAddress, filePacket.appToken, filePacket.mData, false);

//                    mMeshFileManager.addFileInQueue(filePacket.mPeerAddress, pendingFile);
//                    mMeshFileManager.onProcessNextFileSend();

                    mMeshFileManager.addFileInQueue(filePacket.mPeerAddress, pendingFile);
                    if (mTransportManagerX.isFileSendingMode()) {
                        MeshLog.e("Sender already in file sending mode. It will serve only one file at a time");
                    } else {
                        mTransportManagerX.setFileSendingMode(true);
                        mMeshFileManager.onProcessNextFileSend(false, filePacket.mPeerAddress, false);
                    }

                   /* if (mMeshFileManager.isFileMessageQueueEmpty()) {


                    } else {

                        MeshLog.e("Sender is busy add resume message in queue");
                        mMeshFileManager.addFileInQueue(filePacket.mPeerAddress, pendingFile);
                    }*/

                    /*filePacket.mTransferredBytes = transferredBytes;
                    //This router node does not contain full file
                    if(filePacket.mTransferredBytes < filePacket.mFileSize) {
                        //For now this portion won't execute, will be enabled upon eventually
                        // removing the serialization of BT-WiFi path file transfer
                        boolean isSend = mMeshFileManager.sendFileResumeRequest(fileId, filePacket.appToken, null);
                        Timber.e("File resume start for failed file");
                    } else {
                        Timber.e("T bytes: " + filePacket.mTransferredBytes + " F size: " + filePacket.mFileSize);
                    }*/
                }

            } else {

                Timber.e("[File-Resume]File resume called for other source");
                FilePacket packet = mMeshFileHelper.get(fileResumeRequestMessageFromReceiver);
                mDatabaseService.insertFilePacket(packet);

                // TODO: 3/11/2020 Intermediate node should update relative offset field
                //This node has not the file so let the original destination be forwarded
                //incrementally
                RoutingEntity routingEntity = RouteManager.getInstance().
                        getNextNodeEntityByReceiverAddress(fileResumeRequestMessageFromReceiver.mPeerAddress);
                if (routingEntity != null) {

                    mTransportManagerX.sendFileMessage(routingEntity.getAddress(),
                            fileResumeRequestMessageFromReceiver.toJson().getBytes());
                    MeshLog.v("[File-Resume]Forwarded a file packet to: " + fileResumeRequestMessageFromReceiver);

                } else {
                    MeshLog.e("[File-Resume]No forwarder address for file");
                }

            }
        }
        isProcessed = true;

        return isProcessed;
    }

    public boolean processMessage(String senderAddress, FileResendRequestMessage fileResendRequestMessage) {
        FilePacket filePacket = mDatabaseService.getFilePackets(fileResendRequestMessage.mSourceAddress, fileResendRequestMessage.mFileTransferId);
        mMeshFileManager.resendMissingFile(filePacket);
        return true;
    }

    public boolean processMessage(String senderAddress, FileMessage fileMessage) {

        boolean isProcessed = false;

        if (fileMessage != null) {
            MeshLog.v(String.format("[File]Receiving:%s", fileMessage.toString()));
            // Here we will get the file message
            // inserting empty file packet

            //Forming packet
            if (fileMessage instanceof BroadcastMessage) {
                BroadcastFilePacket filePacket;
                filePacket = (BroadcastFilePacket) mDatabaseService.getFilePackets(fileMessage.mSourceAddress,
                        fileMessage.mFileTransferId);
                if (filePacket == null) {//Not a resume file request so generate a new entry
                    filePacket = mMeshFileHelper.getBroadcastFilePacket((BroadcastMessage) fileMessage);
                    filePacket.mSelfFullFilePath = mFileHelper.generateFilePath(null, filePacket.mFileName);
                }
                //Receive senders uri
                filePacket.mPeerFullFilePath = fileMessage.mFilePath;
                filePacket.fileStatus = Const.FileStatus.INPROGRESS;
                // Store in BroadcastDB
                mDatabaseService.insertFilePacket(filePacket);
                processNextEvent(senderAddress, fileMessage, filePacket);
            } else {
                FilePacket filePacket;
                filePacket = mDatabaseService.getFilePackets(fileMessage.mSourceAddress,
                        fileMessage.mFileTransferId);
                if (filePacket == null) {//Not a resume file request so generate a new entry
                    filePacket = mMeshFileHelper.getFilePacketFromFileMessage(fileMessage);
                    filePacket.mSelfFullFilePath = mFileHelper.generateFilePath(null, filePacket.mFileName);
                }
                //Receive senders uri
                filePacket.mPeerFullFilePath = fileMessage.mFilePath;
                filePacket.fileStatus = Const.FileStatus.INPROGRESS;
                mDatabaseService.insertFilePacket(filePacket);
                processNextEvent(senderAddress, fileMessage, filePacket);
            }

            isProcessed = true;
        }
        return isProcessed;
    }

    private void processNextEvent(String senderAddress, FileMessage fileMessage, FilePacket filePacket) {
        //Event process and prepare for next entry
        MeshFileEventListener meshFileEventListener = mMeshFileManager.getMeshEventListener();
        String myAddress = mTransportManagerX.getMyNodeId();
        // check this is the actual receiver
        if (meshFileEventListener != null) {
            if (myAddress.equals(fileMessage.mPeerAddress)) {
                String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                        filePacket.mFileId);
                if (fileMessage instanceof BroadcastMessage) {
                    // need adjustment
                    // Broadcast file received started
                } else {
                    mTransportManagerX.setFileReceivingMode(true);

                    meshFileEventListener.onFileReceiveStarted(filePacket.mSourceAddress, fileMessageId,
                            filePacket.mSelfFullFilePath, Base64.decode(fileMessage.messageMetaData, Base64.DEFAULT), filePacket.appToken);
                }
            }
        }

        RoutingEntity senderEntity = RouteManager.getInstance().
                getShortestPath(senderAddress);

        //Process FileMessage for router
            /*if (!fileMessage.mPeerAddress.equals(myAddress) && !RoutingEntity.
                    isBtNode(senderEntity)) {
                //Me is not the destination

                MeshLog.e("FileMessageTest", "Middle man file meta receive");

                String fileId = mFileHelper.getFileMessageId(fileMessage.mSourceAddress,
                        fileMessage.mFileTransferId);
                Long byteCount = mMeshFileManager.mForwardingBytesCountMap.get(fileId);
                if(byteCount == null) {
                    mMeshFileManager.mForwardingBytesCountMap.put(fileId, 0L);
                }

                FileMessage f = fileMessage.copy();
                f.mFilePath = filePacket.mSelfFullFilePath;
                mMeshFileManager.sendFile(f);
            }*/

        //Start receiving file from immediate sender node
        if (RoutingEntity.isWiFiNode(senderEntity)) {
            // We start wifi client only iff the source is a WiFi connection to me
            // TODO: 4/23/2020 Upon highBW we might need to change here so that we can track the
            // whole path has no BT

            HttpFileClient httpFileClient = new HttpFileClient(context, senderEntity, filePacket,
                    APP_PORT, mFileHelper,
                    mMeshFileManager.mReceiverFileStateHandler);
            /*if (BTManager.getInstance().isDiscovering()) {
                BTManager.getInstance().cancelDiscovery();
            }*/
            MeshLog.i("[file_process] http file process called");

            // Track file receiver state (Receive multiple file or not)
            mMeshFileManager.addReceiverFileMap(mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId));

            httpFileClient.start();
        } else {

            // here we will send  file request message in BLE

            if (fileMessage instanceof BroadcastMessage) {
                if (mBleFileManager != null) {
                    mBleFileManager.onFileRequestFromSender(fileMessage, filePacket);
                }
                MeshLog.v("Content broadcast. File request send to BLE");
            } else {
                //Todo add process for normal files
            }

           /* if (mBTFileManager != null) {

                mBTFileManager.onFileRequestFromSender(fileMessage, filePacket);

            } else {
                MeshLog.e("[BT-Socket]BT File sending but link null at client side");
            }*/
        }
    }

    /**
     * This message is only for Actual file sender.
     * He will get ACK message when Main destination will receive a file packet
     * And from here Main file sender will send percentage in UI layer
     * <p>
     * File Ack message is come from the forwarder (Who is the last of to reach destination)
     *
     * @param sender         Actual sender
     * @param fileAckMessage File ack message
     * @return
     */
    public boolean processMessage(String sender, FileAckMessage fileAckMessage) {
        boolean isProcessed = false;

        if (fileAckMessage != null) {
            if (mTransportManagerX.getMyNodeId().equals(fileAckMessage.mPeerAddress)) {

                // This section only for main file message sender. He got the ACK from main destination's previous hop
                // So source address will be self address
                String sourceAddress = mTransportManagerX.getMyNodeId();

                MeshLog.e("FileMessageTest", "File ack received from forwarder");
                FilePacket myFilePacket = mDatabaseService.getOlderFilePackets(fileAckMessage.mFileTransferId, sourceAddress);

                if (myFilePacket != null) {

                    boolean isTransFerDone = fileAckMessage.mTransferredBytes >=
                            myFilePacket.mFileSize;

                    MeshLog.e("FileMessageTest", "File ack received myFileBytes count: " + myFilePacket.mTransferredBytes);

                    MeshFileEventListener meshFileEventListener = mMeshFileManager.getMeshEventListener();

                    if (meshFileEventListener != null) {

                        String fileMessageId = mFileHelper.getFileMessageId(sourceAddress, myFilePacket.mFileId);

                        int percentage = mFileHelper.calculatePercentage(
                                myFilePacket.mFileSize, fileAckMessage.mTransferredBytes);

                        if (isTransFerDone) {

                            meshFileEventListener.onFileTransferFinish(fileMessageId, myFilePacket.appToken);
                        } else if (percentage > 0) {

                            meshFileEventListener.onFileProgress(fileMessageId, percentage, myFilePacket.appToken);
                        }
                    }

                    if (isTransFerDone) {
                        List<FilePacket> packetList = new ArrayList<>();
                        packetList.add(myFilePacket);
                        mDatabaseService.deleteAllPackets(packetList);
                    }
                }

            } else {
                mTransportManagerX.sendFileMessage(fileAckMessage.mPeerAddress, fileAckMessage.toJson().getBytes());
            }

            isProcessed = true;
        }

        return isProcessed;
    }

    private boolean processMessage(String sourceAddress, FileRequestResumeMessageFromSender message) {
        Timber.d("[File-Resume]%s", message);
        boolean isProcessed = false;
        if (message != null) {
            if (mTransportManagerX.getMyNodeId().equals(message.mPeerAddress)) {

                // This is the main destination and here it will request to source (or hop) for next
                // packet
                String messageId = mFileHelper.getFileMessageId(message.mSourceAddress,
                        message.fileTransferId);
                mMeshFileManager.sendFileResumeRequest(messageId, message.appToken,
                        message.messageMetaData == null ? null : Base64.decode(message.messageMetaData, Base64.DEFAULT));

            } else {
                //It had to forward manually as somehow file API is changed
                mTransportManagerX.sendFileMessage(message.mPeerAddress, message.toJson().getBytes());
            }

            isProcessed = true;
        }

        return isProcessed;
    }

    private boolean processMessage(String sourceAddress, BTFileRequestMessage btFileRequestMessage) {
        boolean isProcessed = false;

        if (mBleFileManager != null) {
            mBleFileManager.onFileRequestFromReceiver(btFileRequestMessage);
        }
       /* if (mBTFileManager != null) {
            mBTFileManager.onFileRequestFromReceiver(btFileRequestMessage);
        }*/

        return isProcessed;
    }
}
