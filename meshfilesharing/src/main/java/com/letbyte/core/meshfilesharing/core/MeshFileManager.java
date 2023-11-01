package com.letbyte.core.meshfilesharing.core;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import androidx.collection.ArraySet;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.api.MeshFileCommunicator;
import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.api.support.mesh.SupportTransportManager;
import com.letbyte.core.meshfilesharing.comm.ble.BleFileManager;
import com.letbyte.core.meshfilesharing.comm.bt.BTFileClient;
import com.letbyte.core.meshfilesharing.comm.bt.BTFileLink;
import com.letbyte.core.meshfilesharing.comm.bt.BTFileLinkListener;
import com.letbyte.core.meshfilesharing.comm.bt.BTFileManager;
import com.letbyte.core.meshfilesharing.comm.bt.BTFileServer;
import com.letbyte.core.meshfilesharing.comm.fileserver.FileServer;
import com.letbyte.core.meshfilesharing.comm.fileserver.webserver.HttpFileClient;
import com.letbyte.core.meshfilesharing.core.listeners.ReceiverFileStateHandler;
import com.letbyte.core.meshfilesharing.core.listeners.SenderFileStateHandler;
import com.letbyte.core.meshfilesharing.data.BaseFileMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FileMessage;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.FileResendRequestMessage;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.data.model.PendingFile;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.httpservices.INanoServerInitiator;
import com.w3engineers.mesh.httpservices.NanoHTTPServer;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.queue.MessageListener;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import id.zelory.compressor.Compressor;
import timber.log.Timber;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 1:45 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 1:45 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 1:45 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class MeshFileManager implements MeshFileCommunicator, MessageListener, BTFileLinkListener, INanoServerInitiator {

    public static final long BT_FILE_SEND_CONNECTION_DELAY = 2000;
    public static final long OTHER_INTERFACE_FILE_DELAY = 2000;
    private static final long FILE_REQUEST_CHECKER_WAITING_TIME = 30 * 1000;
    private MeshFileEventListener mMeshFileEventListener;
    private final MeshFileHelper mMeshFileHelper;
    private final TransportManagerX mTransportManagerX;
    public Map<String, Long> mForwardingBytesCountMap;
    private final Context mContext;
    private final FileHelper mFileHelper;
    private final DatabaseService mDatabaseService;
    private LinkStateListener linkStateListener = null;

    private static final long FILE_SENDING_WAITING_QUEUE_TIME = 120 * 1000L;
    private long fileTransferredId;
    private long previousTransferredBytes;

    private String fileReceiverId;

    /**
     * We will maintain allowed file check upon security measurements taken through this map
     */
    private final Set<Long> mFileIdServerSet;
    private FileServer mFileServer;
    private final SenderFileStateHandler mSenderFileStateHandler;
    public ReceiverFileStateHandler mReceiverFileStateHandler;
    private final BTFileServer mBTFileServer;
    public final BTFileManager mBTFileManager;
    public final BleFileManager mBleFileManager;
    public final BuyerFileManager buyerFileManager;
    public final MultihopFileManager multihopFileManager;

    private final Map<String, Deque<PendingFile>> pendingFileQueueMap;

    private ConcurrentLinkedQueue<String> fileReceiverMap;

    private boolean isInternetFile = false;

    public MeshFileManager(TransportManagerX transportManagerX, Context context, LinkStateListener linkStateListener) {
        mMeshFileHelper = new MeshFileHelper();
        mTransportManagerX = transportManagerX;
        mForwardingBytesCountMap = new ConcurrentHashMap<>(3);

        mFileIdServerSet = new ArraySet<>(3);
        fileReceiverMap = new ConcurrentLinkedQueue<>();
        mTransportManagerX.setMessageListener(this);
        this.mContext = context;
        mFileHelper = new FileHelper();
        mDatabaseService = DatabaseService.getInstance(context);
        this.linkStateListener = linkStateListener;
        attemptCacheClean();
        updateDB();
        mSenderFileStateHandler = new SenderFileStateHandler(mFileIdServerSet, mDatabaseService,
                mFileHelper, this, mMeshFileEventListener);
        mReceiverFileStateHandler = new ReceiverFileStateHandler(mDatabaseService,
                mFileHelper, this, mMeshFileEventListener);

        mBTFileServer = new BTFileServer(this, mFileHelper);
        //mBTFileServer.starListenThread();
        mBTFileManager = new BTFileManager(this);
        mBleFileManager = new BleFileManager(this, mDatabaseService, mTransportManagerX,
                mSenderFileStateHandler, mReceiverFileStateHandler);
        buyerFileManager = new BuyerFileManager(this, mDatabaseService, mTransportManagerX,
                mSenderFileStateHandler, mReceiverFileStateHandler, this.linkStateListener);
        multihopFileManager = new MultihopFileManager(this, mDatabaseService, mTransportManagerX,
                mSenderFileStateHandler, mReceiverFileStateHandler);

        pendingFileQueueMap = new ConcurrentHashMap<>();
        //initFileServer();
    }

    private void updateDB() {
        mDatabaseService.updateFailedPackets();
    }

    public void markFilesFailed(String nodeId) {
        DatabaseService.getInstance(mContext).getExecutorService().execute(() -> {

            //files wer being transferred
            List<FilePacket> filePackets = mDatabaseService.updateFilesFailed(nodeId);
            if (CollectionUtil.hasItem(filePackets)) {
                String fileTransferId;
                for (FilePacket filePacket : filePackets) {
                    Timber.d("Item updated as failed:%s", filePacket);
                    fileTransferId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                            filePacket.mFileId);
                    mMeshFileEventListener.onFileTransferError(fileTransferId, filePacket.appToken, "User disconnected");
                }
            }
        });
    }

    @Override
    public String sendFile(String receiverId, String filePath, byte[] msgMetaData, String appToken) {

        try {
            JSONObject jsonObject = new JSONObject();
            /*if (msgMetaData != null && msgMetaData.length > Constant.FILE_METADATA_MAX_SIZE){
                jsonObject.put("success", false);
                jsonObject.put("msg", Constant.ErrorMessages.EXCEED_FILE_META_SIZE);
                return jsonObject.toString();
            }*/


            String fileMessageId = String.valueOf(MeshFile.UNKNOWN_FILE_ID);

            MeshFileHelper meshFileHelper = new MeshFileHelper();

            long fileTransferId = meshFileHelper.generateFileId(receiverId, filePath);
            fileMessageId = mFileHelper.getFileMessageId(mTransportManagerX.getMyNodeId(), fileTransferId);

            //make priority for local path for this query.
            RoutingEntity routerEntity = RouteManager.getInstance().getShortestPath(receiverId);


            //TODO need to adjust with local file sharing
            // Check is receiver internet user and it is not direct connected node.

            if (routerEntity.getType() == RoutingEntity.Type.INTERNET && routerEntity.getHopAddress() != null) {
                long fileSize = mFileHelper.getFileSize(filePath);
                String sellerId = null;
                if (fileSize > Constant.BUYER_MAX_FILE_SIZE) {
                    jsonObject.put("success", false);
                    jsonObject.put("msg", Constant.ErrorMessages.EXCEED_FILE_SIZE_1);
                    return jsonObject.toString();
                }

                /*else if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_BUYER ){

                    List<String> hopList = RouteManager.getInstance().getHopIds(routerEntity.getAddress());

                     sellerId = this.linkStateListener.onBalanceVerify(fileSize, hopList);
                     if(TextUtils.isEmpty(sellerId)) {
                         jsonObject.put("success", false);
                         jsonObject.put("msg", Constant.ErrorMessages.EXCEED_BALANCE);
                         return jsonObject.toString();
                     }
                }*/

                buyerFileManager.prepareAndSendBuyerFileMessage(receiverId, filePath, msgMetaData, appToken, fileMessageId);
                jsonObject.put("success", true);
                jsonObject.put("msg", fileMessageId);
                return jsonObject.toString();

            }

            PendingFile pendingFile = new PendingFile(receiverId, filePath, msgMetaData, appToken, fileTransferId);
            if (isForceConnectionNeeded(receiverId)) {
                //Start multihop fil send process
                long fileSize = mFileHelper.getFileSize(filePath);
                if (fileSize > Constant.MULTIHOP_MAX_FILE_SIZE) {
                    jsonObject.put("success", false);
                    jsonObject.put("msg", Constant.ErrorMessages.EXCEED_FILE_SIZE_10);
                    return jsonObject.toString();
                }

                int fileStatus = multihopFileManager.prepareAndSendMultihopFileMessage(pendingFile);
                if (fileStatus == BaseMeshMessage.MESSAGE_STATUS_FAILED) {
                    jsonObject.put("success", false);
                    jsonObject.put("msg", Constant.ErrorMessages.NO_USER);
                    return jsonObject.toString();
                }
            } else {
                //Add file in queue
                addFileInQueue(receiverId, pendingFile);

                storeQueueFileInDb(pendingFile);

                if (mTransportManagerX.isFileReceivingMode()) {

                    if (!isForceConnectionNeeded(receiverId)) {
                        mTransportManagerX.setFileSendingMode(true);
                        onProcessNextFileSend(false, receiverId, false);
                    } else {
                        MeshLog.v("[msg_process] file added in queue +++");
                    }
                } else if (!mTransportManagerX.isFileSendingMode()
                        && !mTransportManagerX.isFileReceivingMode()) {
                    mTransportManagerX.setFileSendingMode(true);
                    onProcessNextFileSend(false, receiverId, false);
                }
            }

            jsonObject.put("success", true);
            jsonObject.put("msg", fileMessageId);
            return jsonObject.toString();


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String sendFile(PendingFile pendingFile) {

        String fileTransferId = String.valueOf(MeshFile.UNKNOWN_FILE_ID);

        if (pendingFile == null) {
            MeshLog.e("[FILE_PROCESS] pending file null before send");
            return fileTransferId;
        }

        //RoutingEntity routerEntity = RouteManager.getInstance().getEntityByAddress(receiverId);

        MeshFileHelper meshFileHelper = new MeshFileHelper();

        fileTransferId = mFileHelper.getFileMessageId(mTransportManagerX.getMyNodeId(), pendingFile.fileTransferId);

        /*if (routerEntity == null) {
            MeshLog.e("[p2p_process] routing entity null-----");
            mTransportManagerX.processForceConnection(receiverId);
            PendingFile pendingFile = new PendingFile(receiverId, filePath, msgMetaData, appToken, fileTransferID);
            addFileInQueue(receiverId, pendingFile);
            return fileTransferId;
        }


        if (routerEntity.getType() != RoutingEntity.Type.WiFi) {
            MeshLog.v("[FILE_PROCESS] the receiver user not direct user");
            mTransportManagerX.processForceConnection(receiverId);
            PendingFile pendingFile = new PendingFile(receiverId, filePath, msgMetaData, appToken, fileTransferID);
            addFileInQueue(receiverId, pendingFile);
            return fileTransferId;
        }

        if (routerEntity.getType() == RoutingEntity.Type.WiFi) {
            if (!P2PUtil.isMeGO() && !routerEntity.getIp().equals(WifiTransPort.P2P_MASTER_IP_ADDRESS)) {
                mTransportManagerX.processForceConnection(receiverId);
                PendingFile pendingFile = new PendingFile(receiverId, filePath, msgMetaData, appToken, fileTransferID);
                addFileInQueue(receiverId, pendingFile);

                return fileTransferId;
            }
        }
*/


        MeshLog.e("FileMessageTest", "prepare sending file");


        //generate FileMessage if valid file
        FileMessage fileMessage = meshFileHelper.generateFileMessage(mTransportManagerX.getMyNodeId(),
                pendingFile.receiverId, pendingFile.filePath, pendingFile.fileMeta, pendingFile.appToken);

        if (pendingFile.filePacket != null) {
            fileMessage = mMeshFileHelper.get(pendingFile.filePacket);
            fileMessage.messageMetaData = pendingFile.filePacket.metaData;
        }

        if (fileMessage != null) {
            if (mTransportManagerX != null) {

                //Initiating FileServer, so that immediate recipent can start receiving file
                // immediately
                fileMessage.mFileTransferId = pendingFile.fileTransferId;

                fileTransferId = sendFile(fileMessage);
            } else {
                MeshLog.e("FileMessageTest", "mTransportManagerX null");
            }
        } else {
            MeshLog.e("FileMessageTest", "File message null");
        }

        return fileTransferId;
    }


    private boolean isForceConnectionNeeded(String receiverId) {
        RoutingEntity routerEntity = RouteManager.getInstance().getShortestPath(receiverId);
        if (routerEntity == null) {
            return true;
        }
        if (routerEntity.getType() != RoutingEntity.Type.WiFi) {
            return routerEntity.getType() != RoutingEntity.Type.INTERNET; //Buyer file message are going through different path, no need to check here.
        }
        return false;
     /*   return routerEntity == null
                || routerEntity.getType() != RoutingEntity.Type.WiFi
                || (routerEntity.getType() != RoutingEntity.Type.INTERNET && routerEntity.getHopAddress() != null)
                *//*|| (!P2PUtil.isMeGO() && !routerEntity.getIp().equals(WifiTransPort.P2P_MASTER_IP_ADDRESS))*//*;*/
    }


    public boolean isFileMessageQueueEmpty() {
        if (pendingFileQueueMap.isEmpty()) return true;
        boolean isEmpty = true;
        for (Map.Entry<String, Deque<PendingFile>> item : pendingFileQueueMap.entrySet()) {
            Deque<PendingFile> deque = item.getValue();
            if (!deque.isEmpty()) {
                isEmpty = false;
            }
        }
        return isEmpty;
    }

    /**
     * <p>Add pending file in queue for further </p>
     *
     * @param receiverId  (required)
     * @param pendingFile (required)
     */
    public void addFileInQueue(String receiverId, PendingFile pendingFile) {
        if (receiverId == null || pendingFile == null) return;
        Deque<PendingFile> pendingFileQueue = pendingFileQueueMap.get(receiverId);
        if (pendingFileQueue == null) {
            MeshLog.v("[msg_process] user :" + AddressUtil.makeShortAddress(receiverId) + " new queue created");
            pendingFileQueue = new ArrayDeque<>();
            pendingFileQueueMap.put(receiverId, pendingFileQueue);
        }
        pendingFileQueue.add(pendingFile);
    }

    public void onProcessNextFileSend(boolean isFromReceiverSide, String receiverId, boolean isFileProcessFinished) {

        if (isFromReceiverSide) {
            MeshLog.v("[msg_process] file receiver");
            if (fileReceiverMap.isEmpty()) {
                mTransportManagerX.setFileReceivingMode(false);
            } else {
                MeshLog.e("[File_Process] I am still receiving file from Other. onProcessNextFileSend");
            }

            if (!isFileMessageQueueEmpty()) {
                HandlerUtil.removeBackground(fileFreeModeChecker);
                Deque<PendingFile> pendingFileQueue = pendingFileQueueMap.get(receiverId);

                if (pendingFileQueue != null && !pendingFileQueue.isEmpty()) {
                    boolean isForceConnectionNeeded = isForceConnectionNeeded(receiverId);
                    MeshLog.v("[msg_process] Queue is not empty force connection : " + isForceConnectionNeeded);
                    if (isForceConnectionNeeded) {
                        MeshLog.v("[msg_process] called for force connection");
                        mTransportManagerX.processForceConnection(receiverId);
                    } else {
                        MeshLog.v("[msg_process] Send file to : " + AddressUtil.makeShortAddress(receiverId));
                        PendingFile pendingFile = pendingFileQueue.poll();
                        if (pendingFile != null) {

                            fileReceiverId = receiverId;
                            fileTransferredId = pendingFile.fileTransferId;

                            startFileWaitingTracker();

                            if (pendingFile.isResume) {
                                resumeFileFromQueue(pendingFile);
                            } else {
                                sendFile(pendingFile);
                            }
                        }
                    }
                } else {
                    if (!mTransportManagerX.isFileSendingMode()) {
                        pendingFileQueueMap.remove(receiverId);
                        MeshLog.v("[msg_process] User: " + AddressUtil.makeShortAddress(receiverId) + " empty queue process next");
                        Set<String> keySet = pendingFileQueueMap.keySet();

                        if (!keySet.isEmpty()) {
                            Iterator<String> iterator = keySet.iterator();
                            onProcessNextFileSend(true, iterator.next(), false);
                        }
                    }
                }

            } else {
                mTransportManagerX.setFileSendingMode(false);
                // Here receiver is fully free.
                if (isFileProcessFinished && mTransportManagerX.hasAnyFileRequest()) {
                    HandlerUtil.postBackground(fileFreeModeChecker, FILE_REQUEST_CHECKER_WAITING_TIME);
                }
            }
        } else {

            if (isFileMessageQueueEmpty()) {
                mTransportManagerX.setFileSendingMode(false);
                pendingFileQueueMap.clear();
                MeshLog.v("[msg_process] Send file queue is empty -----");

                // Here Sender is fully free.
                if (isFileProcessFinished && mTransportManagerX.hasAnyFileRequest()) {
                    HandlerUtil.postBackground(fileFreeModeChecker, FILE_REQUEST_CHECKER_WAITING_TIME);
                }

            } else {
                HandlerUtil.removeBackground(fileFreeModeChecker);
                Deque<PendingFile> pendingFileQueue = pendingFileQueueMap.get(receiverId);

                if (pendingFileQueue != null && !pendingFileQueue.isEmpty()) {
                    boolean isForceConnectionNeeded = isForceConnectionNeeded(receiverId);
                    MeshLog.v("[msg_process] Queue is not empty force connection : " + isForceConnectionNeeded);
                    if (isForceConnectionNeeded) {
                        MeshLog.v("[msg_process] called for force connection");
                        mTransportManagerX.processForceConnection(receiverId);
                    } else {
                        MeshLog.v("[msg_process] Send file to : " + AddressUtil.makeShortAddress(receiverId));
                        PendingFile pendingFile = pendingFileQueue.poll();
                        if (pendingFile != null) {

                            fileReceiverId = receiverId;
                            fileTransferredId = pendingFile.fileTransferId;
                            startFileWaitingTracker();

                            if (pendingFile.isResume) {
                                resumeFileFromQueue(pendingFile);
                            } else {
                                sendFile(pendingFile);
                            }
                        }
                    }
                } else {
                    if (!mTransportManagerX.isFileReceivingMode()) {
                        pendingFileQueueMap.remove(receiverId);
                        MeshLog.v("[msg_process] User: " + AddressUtil.makeShortAddress(receiverId) + " empty queue process next");
                        Set<String> keySet = pendingFileQueueMap.keySet();

                        if (!keySet.isEmpty()) {
                            Iterator<String> iterator = keySet.iterator();
                            onProcessNextFileSend(false, iterator.next(), false);
                        } else {
                            TransportManagerX.getInstance().setFileSendingMode(false);
                        }
                    }
                }
            }
        }
    }

    /**
     * When user file send or receive error
     * we have to execute next file from queue if available
     *
     * @param isFromReceiverSide tracking that user from receiver or not
     * @param userId             Source or receiver id
     */
    public void onFileProcessError(boolean isFromReceiverSide, String userId) {

        MeshLog.v("[FILE_PROCESS] File send/receive error occurred");
        pendingFileQueueMap.remove(userId);
        if (isFromReceiverSide) {
            if (fileReceiverMap.isEmpty()) {
                mTransportManagerX.setFileReceivingMode(false);
            } else {
                MeshLog.e("[File_Process] I am still receiving another packet from other");
            }
        }

        if (isFileMessageQueueEmpty()) {
            MeshLog.v("[FILE_PROCESS] there are no file to send");
            if (!isFromReceiverSide) {
                mTransportManagerX.setFileSendingMode(false);
            }
            if (mTransportManagerX.hasAnyFileRequest()) {
                HandlerUtil.postBackground(fileFreeModeChecker, FILE_REQUEST_CHECKER_WAITING_TIME);
            }
        } else {
            MeshLog.v("[FILE_PROCESS] there are other file to send");
            executeNextQueue(isFromReceiverSide, "");

        }
    }

    @Override
    public void sendBroadcast(Broadcast broadcast) {
        MeshLog.e("Broadcast FileMessageTest", "prepare sending file");
        MeshFileHelper meshFileHelper = new MeshFileHelper();

        RoutingEntity shortestEntity = RouteManager.getInstance().getShortestPath(broadcast.getReceiverId());
        String contentPath = broadcast.getContentPath();

        if (shortestEntity.getType() == RoutingEntity.Type.BLE && !TextUtils.isEmpty(contentPath)) {
            if ((new File(contentPath)).length() > MeshFileHelper.BLE_BROADCAST_FILE_SIZE) {
                contentPath = getCompressedContentPath(contentPath);
                broadcast.setContentPath(contentPath);
            }
        }

        BroadcastMessage broadcastMessage = meshFileHelper.getBroadcastMessage(broadcast);

        if (broadcastMessage != null) {
            if (mTransportManagerX != null) {
                sendFile(broadcastMessage);
            } else {
                MeshLog.e("Broadcast FileMessageTest", "mTransportManagerX null");
            }
        } else {
            MeshLog.e("Broadcast FileMessageTest", "File message null");
        }
    }

    private String getCompressedContentPath(String filePath) {
        try {
            File before = new File(filePath);
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);

            if (bitmap == null) {
                return filePath;
            }
            int width = (bitmap.getWidth() * 50) / 100;
            int height = (bitmap.getHeight() * 50) / 100;

            if (width == 0 || height == 0) {
                return filePath;
            }

            File compressedImageFile = new Compressor(mContext)
                    .setMaxWidth(width)
                    .setMaxHeight(height)
                    .setQuality(75)
                    .setDestinationDirectoryPath(FileHelper.getCompressedContentDir())
                    .compressToFile(before);

            return compressedImageFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }


    private void initFileServer(int port) {

        //If any server is open, for now we force close so that our next file process smoothly,
        //Right now leaving the control on users
        clearBroadcastCacheDir();
        if (mFileServer == null) {
            mFileServer = new FileServer(mFileHelper, mSenderFileStateHandler, port);
        }
        if (!mFileServer.isAlive()) {
            try {
                mFileServer.start();
            } catch (IOException e) {
                e.printStackTrace();
                MeshLog.e("[File-Server] :: " + e.toString());
            }
        }
    }

    /**
     * <h1>Clear cache broadcast dir</h1>
     */
    private void clearBroadcastCacheDir() {
        File cacheFileDir = new File(FileHelper.getCompressedContentDir());
        if (cacheFileDir.isDirectory()) {
            File[] fileList = cacheFileDir.listFiles();
            for (File item : fileList) {
                item.delete();
            }
        }
    }


    private String sendFile(FilePacket f) {
        FilePacket filePacket = null;
        String fileTransferId = String.valueOf(MeshFile.UNKNOWN_FILE_ID);

        filePacket = f;
        if (filePacket != null) {
            fileTransferId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                    filePacket.mFileId);

            RoutingEntity routingEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);

            //Inserting inprogress filepacket
            filePacket.fileStatus = Const.FileStatus.INPROGRESS;
            mDatabaseService.insertFilePacket(filePacket);

            FileMessage fileMessage = mMeshFileHelper.get(filePacket);
            String fileMessageJson = fileMessage.toJson();
            MeshLog.e("FileMessageTest", "file json: " + fileMessageJson);

            MeshLog.i("User info: " + routingEntity.toString());

            // Here receiver address will be send
            int internalMetaDataMessageId = mTransportManagerX.sendFileMessage(routingEntity.getAddress(),
                    fileMessageJson.getBytes());

            //If Mesh library accept data to send then we
            MeshLog.e("FileMessageTest", "internalMetaDataMessageId: " + internalMetaDataMessageId);
            if (internalMetaDataMessageId != BaseMeshMessage.MESSAGE_STATUS_FAILED) {

                MeshLog.e("FileMessageTest", "fileTransferId: " + fileTransferId);

            } else {
                // TODO: 2/28/2020 initial message sending failed
                MeshLog.e("Initial File message sending failed");
            }
        }

        return fileTransferId;
    }

    private String sendBroadcastFile(BroadcastFilePacket f) {
        BroadcastFilePacket filePacket = null;
        String fileTransferId = String.valueOf(MeshFile.UNKNOWN_FILE_ID);

        filePacket = f;
        if (filePacket != null) {
            fileTransferId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                    filePacket.mFileId);

            RoutingEntity routingEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);

            //Inserting inprogress filepacket
            filePacket.fileStatus = Const.FileStatus.INPROGRESS;

            // Db Update
            //mDatabaseService.insertFilePacket(filePacket);

            BroadcastMessage fileMessage = mMeshFileHelper.getBroadcastContentMessage(filePacket);
            String fileMessageJson = fileMessage.toJson();
            MeshLog.e("FileMessageTest", "file json: " + fileMessageJson);

            // Here receiver address will be send
            int internalMetaDataMessageId = mTransportManagerX.sendBroadcastMessage(mMeshFileHelper.getBroadcastFromFilePacket(filePacket));

            //If Mesh library accept data to send then we
            MeshLog.e("FileMessageTest", "internalMetaDataMessageId: " + internalMetaDataMessageId);
            if (internalMetaDataMessageId != BaseMeshMessage.MESSAGE_STATUS_FAILED) {

                MeshLog.e("FileMessageTest", "fileTransferId: " + fileTransferId);

            } else {
                // TODO: 2/28/2020 initial message sending failed
                MeshLog.e("Initial File message sending failed");
            }
        }

        return fileTransferId;
    }

    public String sendFile(FileMessage fileMessage) {
        //mTransportManagerX.initDryHighBandMode(fileMessage.mPeerAddress);
        String fileTransferId = null;
        if (fileMessage instanceof BroadcastMessage) {
            BroadcastFilePacket filePacket = mMeshFileHelper.getBroadcastFilePacket((BroadcastMessage) fileMessage);
            fileTransferId = forwardToTargetTransport(fileMessage, fileTransferId, filePacket);
        } else {
            FilePacket filePacket = mMeshFileHelper.getFilePacketFromFileMessage(fileMessage);
            fileTransferId = forwardToTargetTransport(fileMessage, fileTransferId, filePacket);
        }

        // check it is connected with bt or not then we will start server for wifi

        MeshLog.v(String.format("[File]Sending :%s", fileMessage.toString()));
        return fileTransferId;
    }

    public void resendMissingFile(FilePacket filePacket) {

        RoutingEntity routingEntity = RouteManager.getInstance().getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);
        if (routingEntity != null) {

            FileMessage fileMessage = mMeshFileHelper.get(filePacket);
            mFileIdServerSet.add(filePacket.mFileId);

            mFileServer.addRespondingPacket(filePacket);

            mTransportManagerX.setFileSendingMode(true);
            mTransportManagerX.sendFileMessage(routingEntity.getAddress(),
                    fileMessage.toJson().getBytes());

            MeshLog.v("[File-Resend]Forwarded a file packet to: " + fileMessage);
        } else {
            MeshLog.e("[File-Resend]No forwarder address for file");
        }
    }

    public void addReceiverFileMap(String fileId) {
        fileReceiverMap.add(fileId);
    }

    public void removeFileReceiverMap(String fileId) {
        fileReceiverMap.remove(fileId);
    }

    private String forwardToTargetTransport(FileMessage fileMessage, String fileTransferId, FilePacket filePacket) {
        previousTransferredBytes = filePacket.mTransferredBytes;

        List<RoutingEntity> routingEntityList = RouteManager.getInstance().
                getAllPossibleOnlinePathById(fileMessage.mPeerAddress);

        if (routingEntityList != null && !routingEntityList.isEmpty()) {

            List<RoutingEntity> directConnectionList = new ArrayList<>();
            for (RoutingEntity entity : routingEntityList) {
                if (TextUtils.isEmpty(entity.getHopAddress())) {
                    directConnectionList.add(entity);
                }
            }

            if (!directConnectionList.isEmpty()) {
                RoutingEntity routingEntity = null;
                for (RoutingEntity entity : directConnectionList) {
                    if (entity.getType() == RoutingEntity.Type.WiFi) {
                        routingEntity = entity;
                        break;
                    } else {
                        routingEntity = entity;
                    }
                }

                if (routingEntity != null) {
                    //FileServer open for
                    mFileIdServerSet.add(filePacket.mFileId);

                    if (RoutingEntity.isWiFiNode(routingEntity)) {
                        if (!TextUtils.isEmpty(fileMessage.mFilePath)) {
                            mFileServer.addRespondingPacket(filePacket);
                        }
                        if (fileMessage instanceof BroadcastMessage) {
                            fileTransferId = sendBroadcastFile((BroadcastFilePacket) filePacket);
                        } else {
                            fileTransferId = sendFile(filePacket);
                        }
                    } else {

                        // Normally broadcast meta message will pass to common message
                        // And the the next user will be BLE user
                        MeshLog.v("Content Broadcast file. User is BLE "
                                + (routingEntity.getType() == RoutingEntity.Type.BLE) + " eth: "
                                + AddressUtil.makeShortAddress(routingEntity.getAddress()));

                        if (fileMessage instanceof BroadcastMessage) {
                            mDatabaseService.insertFilePacket(filePacket);
                            mBleFileManager.sendFile(filePacket);
                            fileTransferId = sendBroadcastFile((BroadcastFilePacket) filePacket);
                        } else {
                            //Todo we have to manage other file packet here
                        }

              /*  if (mBTFileManager.sendFile(filePacket)) {
                    fileTransferId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                            filePacket.mFileId);
                }*/
                    }
                }
            }
        }
        return fileTransferId;
    }

    @Override
    public void setEventListener(MeshFileEventListener meshFileEventListener) {
        mMeshFileEventListener = meshFileEventListener;

        if (mReceiverFileStateHandler != null) {
            mReceiverFileStateHandler.setMeshFileEventListener(mMeshFileEventListener);
        }

        if (mSenderFileStateHandler != null) {
            mSenderFileStateHandler.setMeshFileEventListener(mMeshFileEventListener);
        }
    }

    @Override
    public void setDefaultStoragePath(String defaultStoragePath) {
        if (Text.isNotEmpty(defaultStoragePath)) {
            SupportTransportManager.getInstance().mDefaultStorageDirectory = defaultStoragePath;
        }
    }

    @Override
    public boolean sendFileResumeRequest(String messageId, String appToken, byte[] metaData) {

        MeshLog.v("P2P_FILE_RESUME Start");
        String[] arr = messageId.split(FileHelper.FILE_ID_SEPARATOR);
        String sourceAddress = arr[0];
        long fileTransferId = Long.parseLong(arr[1]);

        FilePacket filePacket = mDatabaseService.getFilePackets(sourceAddress, fileTransferId,
                Const.FileStatus.FAILED);

        boolean isFileAvailable = true;
        if (filePacket == null) {
            MeshLog.e("[file_process] file resume request not processed. File packet not exists");
            if (!sourceAddress.equals(mTransportManagerX.getMyNodeId())) {
                isFileAvailable = false;
            } else {
                //TODO need meaningful message
                return false;
            }
        }

        if (!isFileAvailable) {


            RoutingEntity routerEntity = RouteManager.getInstance().getShortestPath(sourceAddress);
            if (routerEntity == null) {
                MeshLog.v("file resume request: Routing entity null");
                return false;
            }

            if (!isForceConnectionNeeded(sourceAddress) && TextUtils.isEmpty(routerEntity.getHopAddress())) {
                MeshLog.v("sending new file request.");
                FileResendRequestMessage fileResendRequestMessage = mMeshFileHelper.getResendRequest(sourceAddress, mTransportManagerX.getMyNodeId(), fileTransferId, appToken);
                mTransportManagerX.sendFileMessage(sourceAddress, fileResendRequestMessage.toJson().getBytes());
                return true;
            } else {
                MeshLog.v("Buyer or multihop file sharing.");
                // Todo we need to handle byer file resuming request and with local multihop sharing here.
                return false;
            }
        }


        String destination = filePacket.mPeerAddress.equals(mTransportManagerX.getMyNodeId()) ? sourceAddress : filePacket.mPeerAddress;

        RoutingEntity routerEntity = RouteManager.getInstance().getShortestPath(destination);

        if (routerEntity == null) {
            MeshLog.v("file resume request: Routing entity null");
            return false;
        }


        // Internet file resume process
        if (routerEntity.getType() == RoutingEntity.Type.INTERNET && routerEntity.getHopAddress() != null) {

            long fileSize = mFileHelper.getFileSize(filePacket.mSelfFullFilePath);
            if (fileSize > Constant.BUYER_MAX_FILE_SIZE) {
                mSenderFileStateHandler.onFileTransferError(filePacket, MeshFile.BUYER_FILE_SIZE_EXCEED);
            }

            if (filePacket.mSourceAddress.equals(mTransportManagerX.getMyNodeId())) {
                buyerFileManager.prepareAndSendBuyerFileMessage(filePacket.mPeerAddress, filePacket.mSelfFullFilePath,
                        android.util.Base64.decode(filePacket.metaData, android.util.Base64.DEFAULT), appToken, messageId);
            } else {
                buyerFileManager.sendResumeAck(messageId, filePacket);
            }
            return true;
        }

        PendingFile pendingFile = new PendingFile(filePacket, sourceAddress, appToken, metaData, true);
        if (isForceConnectionNeeded(destination)) {
            long fileSize = mFileHelper.getFileSize(filePacket.mSelfFullFilePath);
            if (fileSize > Constant.MULTIHOP_MAX_FILE_SIZE) {
                mSenderFileStateHandler.onFileTransferError(filePacket, MeshFile.MULTI_HOP_FILE_SIZE_EXCEED);
            }

            if (filePacket.mSourceAddress.equals(mTransportManagerX.getMyNodeId())) {
                multihopFileManager.prepareAndSendMultihopFileMessage(pendingFile);
            } else {
                multihopFileManager.prepareMultihopFileAckPacketAndSend(filePacket);
            }

        } else {

            MeshLog.v("[msg_process] destination address: " + AddressUtil.makeShortAddress(destination));

            addFileInQueue(destination, pendingFile);

            if (mTransportManagerX.isFileReceivingMode()) {

                if (!isForceConnectionNeeded(destination)) {
                    if (!filePacket.mPeerAddress.equals(mTransportManagerX.getMyNodeId())) {
                        mTransportManagerX.setFileSendingMode(true);
                    }
                    onProcessNextFileSend(false, destination, false);
                } else {
                    MeshLog.v("[msg_process] file added in queue +++");
                }
            } else if (!mTransportManagerX.isFileSendingMode() && !mTransportManagerX.isFileReceivingMode()) {
                if (!filePacket.mPeerAddress.equals(mTransportManagerX.getMyNodeId())) {
                    mTransportManagerX.setFileSendingMode(true);
                }
                onProcessNextFileSend(false, destination, false);
            }

        }
        return true;
    }


    private void resumeFileFromQueue(PendingFile pendingFile) {

        FilePacket filePacket = pendingFile.filePacket;
        if (filePacket != null) {

            previousTransferredBytes = filePacket.mTransferredBytes;

            MeshLog.v("[msg_process] resume file from queue called");
            filePacket.appToken = pendingFile.appToken;
            BaseFileMessage baseFileMessage;
            String receiverAddress = null;
            if (pendingFile.sourceAddress.equals(mTransportManagerX.getMyNodeId())) {

                // Send a request to main destination for requesting resume process
                baseFileMessage = mMeshFileHelper.buildRequestDestinationForCallResumeMessage
                        (filePacket);
                receiverAddress = filePacket.mPeerAddress;

            } else {

                baseFileMessage = mMeshFileHelper.getResumeRequest(
                        filePacket);

                RoutingEntity routingEntity = RouteManager.getInstance().
                        getNextNodeEntityByReceiverAddress(pendingFile.sourceAddress);

                //We send this message one by one incrementally, this is because any intermediate
                //node can start resuming data without sending any particular info to source node
                if (routingEntity != null) {
                    receiverAddress = routingEntity.getAddress();
                }
            }
            Timber.d("[File-Resume]%s", baseFileMessage);
            if (AddressUtil.isValidEthAddress(receiverAddress) && baseFileMessage != null) {

                // Todo We have to check that the destination is direct user or not.
                //Todo we have to maintain queue management here also
                if (pendingFile.fileMeta != null) {
                    baseFileMessage.messageMetaData = Base64.encodeToString(pendingFile.fileMeta, Base64.DEFAULT);
                }

                int internalMetaDataMessageId = mTransportManagerX.sendFileMessage(receiverAddress,
                        baseFileMessage.toJson().getBytes());
                //return internalMetaDataMessageId != BaseMeshMessage.MESSAGE_STATUS_FAILED;
            }
        } else {
            MeshLog.v("[msg_process] file packet null");
        }

    }

    @Override
    public void onConnectedWithTargetNode(String targetNodeID) {
        MeshLog.i("[p2p_process] notify file module to send file");
        Deque<PendingFile> pendingFileDeque = pendingFileQueueMap.get(targetNodeID);
        if (pendingFileDeque != null && pendingFileDeque.size() > 0) {
            for (PendingFile item : pendingFileDeque) {
                PendingFile pendingFile = pendingFileDeque.poll();

                fileTransferredId = pendingFile.fileTransferId;

                fileReceiverId = pendingFile.receiverId;
                startFileWaitingTracker();

                if (pendingFile.isResume) {
                    resumeFileFromQueue(pendingFile);
                } else {
                    sendFile(pendingFile);
                }
            }
        }
    }

    @Override
    public void onMessageSend(int messageId, String ipaddress, boolean messageStatus) {
        if (messageStatus) {
            Timber.d("[File][Wrote]Message ack for %s, status %s", messageId, messageStatus);
        } else {
            Timber.e("[File][Wrote]Message ack for %s, status %s", messageId, messageStatus);
        }
    }

    @Override
    public void onGetFileFreeModeToSend(String targetNodeId) {
        MeshLog.i("[FILE_PROCESS] received file free message. We can send file from here");
        onProcessNextFileSend(false, targetNodeId, false);
    }

    @Override
    public void onGetFileFileUserNotFound(String targetNodeId, boolean isFromBusyState) {
        if (isFromBusyState) {
            // For busy state we don't need to remove users file from queue. And execute next queue
            // So first check we have enough item or not
            if (pendingFileQueueMap.size() > 1) {
                executeNextQueue(false, targetNodeId);
            } else {
                if (mTransportManagerX.hasAnyFileRequest()) {
                    HandlerUtil.postBackground(fileFreeModeChecker, FILE_REQUEST_CHECKER_WAITING_TIME);
                }
            }
        } else {
            // After searching when we wnt find targeted node the we will det file in fail status
            Deque<PendingFile> fileQueue = pendingFileQueueMap.get(targetNodeId);

            if (!fileQueue.isEmpty()) {
                PendingFile pendingFile = fileQueue.poll();

                if (pendingFile != null) {
                    MeshLog.e("[File-Process] Fail the packet. User not found or not able to connect");
                    FilePacket fp = mDatabaseService.getFilePackets(pendingFile.sourceAddress, pendingFile.fileTransferId);
                    fp.fileStatus = Const.FileStatus.FAILED;
                    mDatabaseService.updateFilePacket(fp);
                    String fileId = mFileHelper.getFileMessageId(fp.mSourceAddress, fp.mFileId);

                    // remove file waiting tracker
                    removeFileWaitingTracker();

                    mMeshFileEventListener.onFileTransferError(fileId, fp.appToken, "User not reachable");

                } else {
                    MeshLog.i("[File-Process] File user not found but queue empty");
                }
            } else {
                MeshLog.i("[File-Process] File user not found but queue empty");
            }

            onFileProcessError(false, targetNodeId);
        }
    }

    public void destroy() {
        if (mTransportManagerX != null) {
            mTransportManagerX.setMessageListener(null);
        }
    }

    private void attemptCacheClean() {

        //This clean up can be by
        //1. WorkManager once in a Week or on any other condition
        //2. Existing file size if cross a threshold value then also we can initiate cleanup
        //In production the approach would basically depends on how we are using these APIs

        //Whatever is the deletion protocol, expected behavior is failed file entry of sender
        // should not be deleted as it is required for file resume feature

        AndroidUtil.postBackground(() -> {

            DatabaseService databaseService = DatabaseService.getInstance(mContext);
            List<FilePacket> filePackets = databaseService.getOlderFilePackets(
                    MeshFileHelper.MAXIMUM_DURATION_TO_STORE_FILE_CACHE);

            int fileDeletedCount = mFileHelper.deleteFile(filePackets);
            if (fileDeletedCount > 0) {

                int numberOfRows = databaseService.deleteAllPackets(filePackets);
                MeshLog.v("[File]Cache cleanup. Deleted " + fileDeletedCount + " files and " +
                        numberOfRows + " rows in DB");
            }

        });

    }

    public MeshFileEventListener getMeshEventListener() {
        return mMeshFileEventListener;
    }

    public void resetFileServer() {
        if (mFileServer != null) {
            mFileServer.stop();
            mFileServer = null;
        }
    }

    @Override
    public BTFileLink getBTFileLink() {
        return mBTFileManager.getBTFileLink();
    }

    @Override
    public void onBTLink(BTFileLink btFileLink) {
        if (btFileLink != null) {
            Timber.d("BT Classic [BT-Socket]File link established-%s", btFileLink);
            if (mBTFileManager != null) {
                mBTFileManager.setBTFileLink(btFileLink);
            }
            btFileLink.setReceiverFileStateHandler(mReceiverFileStateHandler);
            btFileLink.setSenderFileStateHandler(mSenderFileStateHandler);
        }
    }

    public void onBtMessageSocketDisconnect(String nodeID) {
        //Any ongoing file transfer will be failed
        if (mBTFileManager != null) {
            mBTFileManager.onPeerDisconnect();
        }
        //Order is important here, do not rearrange code please unless discussed
        markFilesFailed(nodeID);//Might need to update as it would be automatically failed from links
        //mBTFileServer.starListenThread();

        /*//TODO to check BT node from Routing Table or any other way
        RoutingEntity routingEntity = RouteManager.getInstance().getRoutingEntityByAddress(nodeID);
        boolean isBtNode = RoutingEntity.isBtNode(routingEntity);

        if(isBtNode) {

            if(mBTFileLink != null) {
                mBTFileLink.onPeerLeave();
            }

            mBTFileServer.starListenThread();
        }*/
    }

    public void onByteReceived(FilePacket filePacket, int numberOfBytes) {
        //Whether received in WiFi/BT
        //If me is not the recipient, forward packet to WiFi immediately, prior during message
        // receive
        //If to proceed with BT then check the Queue and proceed accordingly

        if (filePacket != null && numberOfBytes > 0) {

            if (mTransportManagerX.getMyNodeId().equals(filePacket.mPeerAddress)) {
                //Me is the destination
            } else {
                //Need to forward data
                RoutingEntity toRoutingEntity = RouteManager.getInstance().
                        getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);
                if (toRoutingEntity != null) {
                    //Waiting for this packet
                    //Forward the packet
                    if (RoutingEntity.isBtNode(toRoutingEntity)) {

                        if (mBTFileManager != null) {
                            //FileServer open for
                            mFileIdServerSet.add(filePacket.mFileId);
                            if (filePacket instanceof BroadcastFilePacket) {
                                BroadcastFilePacket f = ((BroadcastFilePacket) filePacket).copyBroadCastFilePacket();
                                sendToHandler(f);
                            } else {
                                FilePacket f = filePacket.copy();
                                sendToHandler(f);
                            }
                        } else {
                            MeshLog.e("[BT-File-Socket]BT File sending, link null at " +
                                    "router side");
                        }

                    } else if (RoutingEntity.isWiFiNode(toRoutingEntity)) {
                        if (mFileServer != null) {

                            mFileServer.onBytesAvailable(filePacket);

                        } else {
                            MeshLog.e("[WiFi-File-Server]WiFi server not available");
                        }
                    }
                }
            }
        }
    }

    private void sendToHandler(FilePacket filePacket) {
        filePacket.mTransferredBytes = 0;
        HandlerUtil.postBackground(() -> mBTFileManager.sendFile(filePacket),
                BT_FILE_SEND_CONNECTION_DELAY);
    }

    public void onBtUserConnected(BluetoothDevice bluetoothDevice) {

        HandlerUtil.postBackground(() -> {
                            /*//Check BT user or not
                            //Try to connect with BT channel
                            BluetoothDevice bluetoothDevice = TransportManagerX.getInstance().
                                    getBlueToothTransport().bluetoothClient.mBluetoothDevice;
                          */
            BTFileClient btFileClient = new BTFileClient(mBTFileServer, this,
                    mFileHelper);
            boolean isConnected = btFileClient.createConnection(bluetoothDevice);

            // Connection fail or success we have to reset connecting mode
            mTransportManagerX.setBtConnecting(false);

            if (!isConnected) {

                //TransportManagerX.getInstance().getBlueToothTransport().disconnectBtMessageSocket();
            }
        }, MeshFileManager.BT_FILE_SEND_CONNECTION_DELAY);
    }

    public void onBTFileSentFinish(FilePacket filePacket) {

        mBTFileManager.onBTFileSentFinish(filePacket);
    }

    public void onBTFileReceiveFinish(FilePacket filePacket) {

        mBTFileManager.onBTFileReceiveFinish(filePacket);
    }

    public MeshFileHelper getMeshFileHelper() {
        return mMeshFileHelper;
    }

    public void onFileReceiveFinish(FilePacket filePacket) {

        RoutingEntity senderEntity = RouteManager.getInstance().
                getNextNodeEntityByReceiverAddress(filePacket.mSourceAddress);
        if (RoutingEntity.isBtNode(senderEntity)) {
            mBTFileManager.onBTFileReceiveFinish(filePacket);

        } else if (!mTransportManagerX.getMyNodeId().equals(filePacket.mPeerAddress)) {
            //Me is not the receiver
            RoutingEntity receiverEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);

            if (RoutingEntity.isWiFiNode(senderEntity) && RoutingEntity.isWiFiNode(receiverEntity)) {
                //In high organic WiFi mesh, highBand mode
                if (filePacket instanceof BroadcastFilePacket) {
                    BroadcastFilePacket f = ((BroadcastFilePacket) filePacket).copyBroadCastFilePacket();
                    f.mTransferredBytes = 0;
                    mFileServer.addRespondingPacket(f);
                    sendBroadcastFile((BroadcastFilePacket) filePacket);
                } else {
                    FilePacket f = filePacket.copy();
                    f.mTransferredBytes = 0;
                    mFileServer.addRespondingPacket(f);
                    sendFile(f);
                }

            }
        }
    }

    /**
     * For each pending queue file will also be inserted in database as a in progress file
     * and it will help when file need to be resend or retry
     *
     * @param pendingFile {@link PendingFile}
     */
    private void storeQueueFileInDb(PendingFile pendingFile) {
        FileMessage fileMessage = mMeshFileHelper.generateFileMessage(mTransportManagerX.getMyNodeId(),
                pendingFile.receiverId, pendingFile.filePath, pendingFile.fileMeta, pendingFile.appToken);
        fileMessage.mFileTransferId = pendingFile.fileTransferId;

        FilePacket filePacket = mMeshFileHelper.getFilePacketFromFileMessage(fileMessage);
        filePacket.fileStatus = Const.FileStatus.INPROGRESS;
        mDatabaseService.insertFilePacket(filePacket);
    }

    private void executeNextQueue(boolean isFromReceiverSide, String ignoreUserId) {
        Set<String> keySet = pendingFileQueueMap.keySet();
        if (!keySet.isEmpty()) {
            Iterator<String> iterator = keySet.iterator();
            String userId = iterator.next();
            // We checking that for ignoring busy receiver id. We will keep the file
            // in queue and execute next queue
            if (userId.equals(ignoreUserId)) {
                if (iterator.hasNext()) {
                    userId = iterator.next();
                } else {
                    userId = "";
                }
            }
            if (TextUtils.isEmpty(userId)) {
                MeshLog.e("[FILE_PROCESS] the file receiver id is empty.");
                return;
            }
            onProcessNextFileSend(isFromReceiverSide, userId, false);
        }
    }

    private final Runnable fileFreeModeChecker = new Runnable() {
        @Override
        public void run() {
            if (!mTransportManagerX.isFileReceivingMode()
                    && !mTransportManagerX.isFileSendingMode()
                    && mTransportManagerX.hasAnyFileRequest()) {
                MeshLog.v("[FILE_PROCESS] I'm free and I've file a request to receive.");
                mTransportManagerX.sendFileFreeModeMessage();
            }
        }
    };

    @Override
    public NanoHTTPServer generateNanoServer(int port) {
        initFileServer(port);
        return mFileServer;
    }

    @Override
    public void stopFileServer() {
        if (mFileServer != null) {
            mFileServer = null;
        }
    }

    private final Runnable fileSendWaitingTracker = new Runnable() {
        @Override
        public void run() {
            // First we have to check the file already send or failed
            FilePacket fp = mDatabaseService.getFilePackets(mTransportManagerX.getMyNodeId(), fileTransferredId);
            MeshLog.i("[FILE_PROCESS] File timeout call");
            if (fp != null) {
                MeshLog.e("[FILE_PROCESS] timeout file status: " + fp.toString());
            }

            if (fp != null
                    && ((fp.fileStatus == Const.FileStatus.INPROGRESS && fp.mTransferredBytes <= previousTransferredBytes)
                    || (fp.fileStatus == Const.FileStatus.FAILED))) {

                MeshLog.e("[FILE_PROCESS] the running file is failed");
                // After waiting 1.5 minute the file still not send.
                // Now we can send a fail response to app and execute next item from queue
                // we can use same method onFileProcessError()

                fp.fileStatus = Const.FileStatus.FAILED;
                mDatabaseService.updateFilePacket(fp);

                onFileProcessError(false, fileReceiverId);

                String fileId = mFileHelper.getFileMessageId(fp.mSourceAddress, fp.mFileId);
                mMeshFileEventListener.onFileTransferError(fileId, fp.appToken, "Request timeout");

                //RoutingEntity entity = RouteManager.getInstance().getShortestPath(fileReceiverId);
            } else {
                MeshLog.i("[FILE_PROCESS] file timeout no condition matched");
            }

        }
    };

    private void startFileWaitingTracker() {
        HandlerUtil.postBackground(fileSendWaitingTracker, FILE_SENDING_WAITING_QUEUE_TIME);
    }

    private void removeFileWaitingTracker() {
        HandlerUtil.removeBackground(fileSendWaitingTracker);
    }

    public void setInternetFileMode(boolean isInternetFile) {
        this.isInternetFile = isInternetFile;
    }

    public void onNodeLeave(String nodeId) {
        mDatabaseService.updateFilesFailed(nodeId);
        pendingFileQueueMap.remove(nodeId);
        MeshFileManager mMeshFileManager;
        onProcessNextFileSend(false, nodeId, true);

    }
}
