package com.letbyte.core.meshfilesharing.core;

import android.text.TextUtils;
import android.util.Base64;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.core.listeners.ReceiverFileStateHandler;
import com.letbyte.core.meshfilesharing.core.listeners.SenderFileStateHandler;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.data.model.MultihopFileAck;
import com.letbyte.core.meshfilesharing.data.model.MultihopFileMessage;
import com.letbyte.core.meshfilesharing.data.model.MultihopFilePacket;
import com.letbyte.core.meshfilesharing.data.model.MultihopResumeRequestFromSender;
import com.letbyte.core.meshfilesharing.data.model.PendingFile;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MultiHopFileTracker;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Azizul Islam on 6/9/21.
 */
public class MultihopFileManager {

    /**
     * Instance variable
     */
    private MeshFileManager meshFileManager;
    private DatabaseService mDatabaseService;
    private FileHelper mFileHelper;
    private TransportManagerX mTransportManagerX;
    private SenderFileStateHandler mSenderFileStateHandler;
    private ReceiverFileStateHandler mReceiverFileStateHandler;
    private final int FILE_INFO_NOT_EXIST = 0;
    public static final int FILE_INFO_EXIST = 1;
    public static final int FILE_BALANCE_EXCEED = 2;

    private final long TRACKER__SCHEDULING_TIME = 10 * 1000L;


    /**
     * Reachable time limit actually depends on the time between two message for sender and receiver
     * <p>
     * 1. Sender: a) Packet and b) corresponding packet ack
     * <p>
     * Note: If ack not come we will think receiver did not receive the packet
     * <p>
     * 1. Receiver: a) x packet b) x+1 packet (next packet)
     * <p>
     * Note: When receiver will receive last packet then we don't need this th time checking
     */
    private final long FILE_PACKET_REACHABLE_TIME_LIMIT = 20 * 1000L;


    private ConcurrentHashMap<String, MultiHopFileTracker> mFileTrackingMap;


    public MultihopFileManager(MeshFileManager manager, DatabaseService dbService, TransportManagerX mTransportManagerX,
                               SenderFileStateHandler mSenderFileStateHandler, ReceiverFileStateHandler mReceiverFileStateHandler) {
        this.meshFileManager = manager;
        this.mDatabaseService = dbService;
        this.mFileHelper = new FileHelper();
        this.mTransportManagerX = mTransportManagerX;
        this.mSenderFileStateHandler = mSenderFileStateHandler;
        this.mReceiverFileStateHandler = mReceiverFileStateHandler;
        this.mFileTrackingMap = new ConcurrentHashMap<>();

    }

    public int prepareAndSendMultihopFileMessage(PendingFile pendingFile) {
        final int[] fileStatus = {BaseMeshMessage.MESSAGE_STATUS_SUCCESS};
        new Thread(() -> {
            String myNodeId = mTransportManagerX.getMyNodeId();


            FilePacket oldPacket = mDatabaseService.getFilePackets(myNodeId, pendingFile.fileTransferId, Const.FileStatus.FAILED);
            if (oldPacket == null) {
                MultihopFileMessage multihopFileMessage = new MultihopFileMessage(myNodeId,
                        pendingFile.receiverId,
                        pendingFile.fileTransferId,
                        mFileHelper.getFileName(pendingFile.filePath),
                        mFileHelper.getFileSize(pendingFile.filePath),
                        pendingFile.appToken);

                if (pendingFile.fileMeta != null) {
                    multihopFileMessage.messageMetaData = Base64.encodeToString(pendingFile.fileMeta, Base64.DEFAULT);
                }
                FilePacket filePacket = prepareFilePacket(pendingFile.filePath, multihopFileMessage);
                mDatabaseService.insertFilePacket(filePacket);
                // Todo if the app message failed in hop layer need to notify main sender
                String fileMsgId = mFileHelper.getFileMessageId(myNodeId, pendingFile.fileTransferId);
                fileStatus[0] = mTransportManagerX.sendMultihopFileMessage(pendingFile.receiverId, fileMsgId, multihopFileMessage.toJson().getBytes());
                if (fileStatus[0] == BaseMeshMessage.MESSAGE_STATUS_FAILED) {
                    filePacket.fileStatus = Const.FileStatus.FAILED;
                    mDatabaseService.updateFilePacket(filePacket);
                } else {
                    // It is only sender side
                    addFilePacketHashMap(filePacket, true);
                }
            } else {
                // It is only sender side
                addFilePacketHashMap(oldPacket, true);

                MultihopResumeRequestFromSender multihopFileResumeRequest = new MultihopResumeRequestFromSender(pendingFile.fileTransferId);
                String fileMsgId = mFileHelper.getFileMessageId(myNodeId, pendingFile.fileTransferId);
                mTransportManagerX.sendMultihopFileMessage(pendingFile.receiverId, fileMsgId, multihopFileResumeRequest.toJson().getBytes());
            }
        }).start();
        return fileStatus[0];
    }

    /**
     * <h1>Prepare only initial file packet with all information</h1>
     *
     * @param filePath    : String (required)
     * @param fileMessage : MultihopFileMessage (required)
     * @return FilePacket obj
     */
    private FilePacket prepareFilePacket(String filePath, MultihopFileMessage fileMessage) {
        FilePacket filePacket = null;
        if (fileMessage != null) {
            filePacket = new FilePacket(fileMessage.fileTransferId);
            filePacket.mFileName = fileMessage.fileName;
            filePacket.mSelfFullFilePath = filePath;
            filePacket.mFileSize = fileMessage.fileSize;
            filePacket.mPeerAddress = fileMessage.destinationId;
            filePacket.appToken = fileMessage.appToken;
            filePacket.mSourceAddress = fileMessage.sourceAddress;
            filePacket.mTransferredBytes = 0;
            filePacket.metaData = fileMessage.messageMetaData;

            filePacket.fileStatus = Const.FileStatus.INPROGRESS;
        }
        return filePacket;
    }


    private MultihopFileMessage prepareMultihopFileMessage(FilePacket filePacket) {
        MultihopFileMessage multihopFileMessage = new MultihopFileMessage(
                filePacket.mSourceAddress,
                filePacket.mPeerAddress,
                filePacket.mFileId,
                filePacket.mFileName,
                filePacket.mFileSize,
                filePacket.appToken);
        multihopFileMessage.messageMetaData = filePacket.metaData;
        return multihopFileMessage;
    }


    public void onReceivedMultihopFileMessage(String senderId, MultihopFileMessage fileMessage) {
        FilePacket oldFilePacket = mDatabaseService.getFilePackets(senderId, fileMessage.fileTransferId);
        if (oldFilePacket == null) {
            //New file
            String filePath = mFileHelper.generateFilePath(null, fileMessage.fileName);
            FilePacket filePacket = prepareFilePacket(filePath, fileMessage);
            filePacket.mSelfFullFilePath = filePath;
            filePacket.fileStatus = Const.FileStatus.INPROGRESS;
            String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);

            MeshLog.v("Multihop_file new file  message received from internet");

            // Store in BroadcastDB
            mDatabaseService.insertFilePacket(filePacket);

            // receiver will start tracking.
            addFilePacketHashMap(filePacket, false);

            MeshFileEventListener meshFileEventListener = meshFileManager.getMeshEventListener();
            if (meshFileEventListener != null) {
                meshFileEventListener.onFileReceiveStarted(senderId, fileMessageId, filePath,
                        fileMessage.messageMetaData == null ? null :
                                Base64.decode(fileMessage.messageMetaData, Base64.DEFAULT), fileMessage.appToken);
            } else {
                MeshLog.v("File ui callback is null");
            }            //Send ACK for packet data
            prepareAckPacketAndSend(senderId, fileMessageId, 0);
        } else {
            String fileMessageId = mFileHelper.getFileMessageId(oldFilePacket.mSourceAddress, oldFilePacket.mFileId);
            prepareAckPacketAndSend(oldFilePacket.mSourceAddress, fileMessageId, oldFilePacket.mTransferredBytes);
        }
    }

    public void onReceivedMultihopFilePacket(String senderId, MultihopFilePacket multihopFilePacket) {
        MeshLog.i("multihop_file: onReceivedMultihopFilePacket source address: "
                + multihopFilePacket.sourceAddress + " id: " + multihopFilePacket.fileTransferId + " senderId: " + senderId);

        FilePacket filePacket = mDatabaseService.getFilePackets(multihopFilePacket.sourceAddress,
                multihopFilePacket.fileTransferId);
        if (filePacket == null) {
            MeshLog.i("multihop_file: packet received but file packet null");
        } else {
            byte[] data = Base64.decode(multihopFilePacket.data, Base64.DEFAULT);
            filePacket.mData = data;
            mFileHelper.writePacketData(filePacket);
            filePacket.mTransferredBytes += data.length;

            MeshLog.i("multihop_file packet received. packet size: " + data.length);
            int currentPercentage = FileHelper.getPercentage(filePacket);
            mReceiverFileStateHandler.onFilePercentProgress(filePacket, currentPercentage);

            boolean isFileReceiveDone = false;
            if (filePacket.mTransferredBytes >= filePacket.mFileSize) {
                filePacket.fileStatus = Const.FileStatus.FINISH;
                MeshLog.v("multihop_file  File receive ");
                isFileReceiveDone = true;
            }

            mDatabaseService.updateFilePacket(filePacket);


            String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);

            // Update tracker time because we successfully receive a packet
            updateFileTrackerTime(fileMessageId);

            prepareAckPacketAndSend(senderId, fileMessageId, filePacket.mTransferredBytes);

            if (isFileReceiveDone) {
                // The packet receive done. So we can remove the tracker from map
                removeFilePacketHashMap(fileMessageId);

                mReceiverFileStateHandler.onFileTransferFinish(filePacket);
            }
        }
    }

    public void onReceivedMultihopFileAck(String senderId, MultihopFileAck multihopFileAck) {
        String[] arr = multihopFileAck.fileMessageId.split(FileHelper.FILE_ID_SEPARATOR);
        long transferId = Long.parseLong(arr[1]);
        FilePacket filePacket = mDatabaseService.getFilePackets(arr[0], transferId);
        filePacket.fileStatus = Const.FileStatus.INPROGRESS;

        // At receiver side file info not exist
        // This is work when resume request send from sender and at receiver side file info not exist
        if (multihopFileAck.fileInfoStatus == FILE_INFO_NOT_EXIST) {
            filePacket.mTransferredBytes = 0;
            MultihopFileMessage multihopFileMessage = prepareMultihopFileMessage(filePacket);
            mDatabaseService.updateFilePacket(filePacket);
            mTransportManagerX.sendMultihopFileMessage(senderId, multihopFileAck.fileMessageId, multihopFileMessage.toJson().getBytes());
            return;
        }


        if (multihopFileAck.receivedBytes < filePacket.mFileSize) {

            // Update the file tracker time because we successfully send a file packet
            updateFileTrackerTime(multihopFileAck.fileMessageId);

            MultihopFilePacket multihopFilePacket = new MultihopFilePacket(filePacket.mFileId);
            multihopFilePacket.sourceAddress = filePacket.mSourceAddress;
            multihopFilePacket.destinationId = filePacket.mPeerAddress;

            //Byte count that receiver already ready received
            filePacket.mTransferredBytes = multihopFileAck.receivedBytes;
            MeshLog.v("multihop  already send :" + multihopFileAck.receivedBytes + " Total :" + filePacket.mFileSize);

            multihopFilePacket.data = Base64.encodeToString(mFileHelper.readPacketDataForBle(filePacket, false), Base64.DEFAULT);

            mDatabaseService.updateFilePacket(filePacket);

            int currentPercentage = FileHelper.getPercentage(filePacket);

            mSenderFileStateHandler.onFilePercentProgress(filePacket, currentPercentage);
            mTransportManagerX.sendMultihopFileMessage(senderId, multihopFileAck.fileMessageId, multihopFilePacket.toJson().getBytes());

        } else {

            // Total file sending completed so we can remove tracker from map
            removeFilePacketHashMap(multihopFileAck.fileMessageId);

            filePacket.mTransferredBytes = filePacket.mFileSize;
            filePacket.fileStatus = Const.FileStatus.FINISH;
            mDatabaseService.updateFilePacket(filePacket);
            mSenderFileStateHandler.onFilePercentProgress(filePacket, 100);
            mSenderFileStateHandler.onFileTransferFinish(filePacket);
        }
    }

    private void prepareAckPacketAndSend(String receiver, String fileId, long byteCount) {
        MultihopFileAck multihopFileAck = new MultihopFileAck(fileId, byteCount);
        mTransportManagerX.sendMultihopFileMessage(receiver, fileId, multihopFileAck.toJson().getBytes());
    }

    /**
     * <h1>
     * Receive file resume trigger action from send
     * Peek file packet from DB
     * if file packet exist then send id and transfer byte count
     * if not then set file info not exist status
     * </h1>
     *
     * @param senderAddress             (required) : String sender address
     * @param multihopFileResumeRequest (required) : Obj
     */
    public void onReceivedMultihopFileResumeRequest(String senderAddress, MultihopResumeRequestFromSender multihopFileResumeRequest) {

        FilePacket oldFilePacket = mDatabaseService.getFilePackets(senderAddress, multihopFileResumeRequest.fileTransferId);
        MeshLog.v("buyer_file received file resume request++++");
        if (oldFilePacket != null) {
            String fileMessageId = mFileHelper.getFileMessageId(oldFilePacket.mSourceAddress, oldFilePacket.mFileId);
            prepareAckPacketAndSend(oldFilePacket.mSourceAddress, fileMessageId, oldFilePacket.mTransferredBytes);
        } else {
            String fileMessageId = mFileHelper.getFileMessageId(senderAddress, multihopFileResumeRequest.fileTransferId);
            MultihopFileAck multihopFileAck = new MultihopFileAck(fileMessageId, 0);
            multihopFileAck.fileInfoStatus = FILE_INFO_NOT_EXIST;
            mTransportManagerX.sendMultihopFileMessage(senderAddress, fileMessageId, multihopFileAck.toJson().getBytes());
        }
    }

    private void updateFileTrackerTime(String fileId) {
        if (!mFileTrackingMap.isEmpty()) {
            MultiHopFileTracker tracker = mFileTrackingMap.get(fileId);
            if (tracker != null) {
                tracker.filePacketSendTime = System.currentTimeMillis();
                MeshLog.e("[File-Time-Tracker] tracker time updated");
            } else {
                MeshLog.e("[File-Time-Tracker] tracker null during time update");
            }
        }
    }


    private void startOrStopTracker(boolean isStart) {
        if (isStart) {
            HandlerUtil.postBackground(fileProgressTracker, TRACKER__SCHEDULING_TIME);
        } else {
            MeshLog.i("[File-Time-Tracker] runnable removed");
            HandlerUtil.removeBackground(fileProgressTracker);
        }
    }

    private void addFilePacketHashMap(FilePacket filePacket, boolean isSender) {

        // start tracker if map is empty. If not that means already tracker running
        if (mFileTrackingMap.isEmpty()) {
            startOrStopTracker(true);
        }

        String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);

        String userId;
        if (isSender) {
            userId = filePacket.mPeerAddress;
        } else {
            userId = filePacket.mSourceAddress;
        }

        MultiHopFileTracker tracker = new MultiHopFileTracker(System.currentTimeMillis(),
                filePacket.mTransferredBytes, filePacket.mSourceAddress,
                filePacket.mFileId, getEstimationPacketSendingTime(userId));

        mFileTrackingMap.put(fileId, tracker);
    }

    private void removeFilePacketHashMap(String fileId) {
        mFileTrackingMap.remove(fileId);

        if (mFileTrackingMap.isEmpty()) {
            startOrStopTracker(false);
        }
    }


    /**
     * There are multiple file will be in progress
     * and multiple file can be send/receiving state.
     * we have to check a map and manage start and waiting time
     * for each packet
     */
    private void checkFileCurrentProgress() {

        if (!mFileTrackingMap.isEmpty()) {
            // check current state for all packet in map
            for (String fileId : mFileTrackingMap.keySet()) {

                MultiHopFileTracker tracker = mFileTrackingMap.get(fileId);
                if (tracker != null) {

                    // first check each file cross its time limit or not
                    long currentTime = System.currentTimeMillis();

                    long timeDifference = currentTime - tracker.filePacketSendTime;

                    MeshLog.i("[File-Time-Tracker] time difference: " + timeDifference + " fileId: " + fileId);

                    if (timeDifference > tracker.comparisonTime) {
                        MeshLog.i("[File-Time-Tracker] time exceed need to check file state");

                        // check the file packet progress increased or not

                        FilePacket filePacket = mDatabaseService.getFilePackets(tracker.sourceAddress, tracker.transferId);

                        if (filePacket != null) {

                            if (filePacket.fileStatus == Const.FileStatus.INPROGRESS) {

                                // Now check progress changed or not
                                if (tracker.lastProgress >= filePacket.mTransferredBytes) {

                                    MeshLog.e("[File-Time-Tracker] file error");
                                    // That means no data changed

                                    String myId = mTransportManagerX.getMyNodeId();

                                    // Notify UI for file error
                                    if (tracker.sourceAddress.equals(myId)) {
                                        mSenderFileStateHandler.onFileTransferError(filePacket, MeshFile.FAILED);
                                    } else {
                                        mReceiverFileStateHandler.onFileTransferError(filePacket, MeshFile.FAILED);
                                    }

                                    // Update file packet as failed status
                                    filePacket.fileStatus = Const.FileStatus.FAILED;
                                    mDatabaseService.updateFilePacket(filePacket);

                                    // Removed the current file from map
                                    removeFilePacketHashMap(fileId);
                                } else {
                                    MeshLog.i("[File-Time-Tracker] but file progressing work");
                                }
                            } else {
                                String status;
                                if (filePacket.fileStatus == Const.FileStatus.FAILED) {
                                    status = "Failed";
                                } else if (filePacket.fileStatus == Const.FileStatus.FINISH) {
                                    status = "Finish";
                                } else {
                                    status = "No-Status";
                                }

                                MeshLog.i("[File-Time-Tracker] file already in " + status);

                                // Removed the current file from map
                                removeFilePacketHashMap(fileId);
                            }

                        } else {
                            MeshLog.e("[File-Time-Tracker] file packet null");
                        }
                    }

                } else {
                    MeshLog.e("[File-Time-Tracker] MultiHopFileTracker null: " + fileId);
                }
            }
        }

        // reschedule the map
        startOrStopTracker(!mFileTrackingMap.isEmpty());

    }

    /**
     * This tracker will be rescheduled every 10 seconds
     * and check file status, progress.
     */
    private final Runnable fileProgressTracker = this::checkFileCurrentProgress;

    /**
     * This method will calculate the estimation time to reach the destination.
     * The time calculation depend on
     * <p>
     * for sender -> Time between packet send and ack received
     * <p>
     * for receiver -> Waiting for next packet receive
     *
     * @param peerAddress Can be sender/ receiver
     * @return Time
     */
    private long getEstimationPacketSendingTime(String peerAddress) {

        RoutingEntity routingEntity = RouteManager.getInstance().getShortestPath(peerAddress);
        if (routingEntity != null) {
            int hopCount = routingEntity.getHopCount();

            int btConnection = 0;
            int wifiConnection = 0;

            if (hopCount % 2 == 0) {

                // if hop count is even then interface will be odd
                // And if first interface is wifi or ble that means
                // there are an extra interface (It is just assumption. And this possibility is high )

                int halfPath = hopCount / 2;

                if (routingEntity.getType() == RoutingEntity.Type.BLE_MESH) {
                    btConnection = 1 + halfPath;
                    wifiConnection = halfPath;
                } else if (routingEntity.getType() == RoutingEntity.Type.WifiMesh) {
                    wifiConnection = 1 + halfPath;
                    btConnection = halfPath;
                } else {
                    // There will be no else. If yes there will be Bug.
                    // We have send that file via P2P
                }


            } else {

                // If hop count odd like 1 A-B-C
                // So interface is two. A to B and B to C

                btConnection = (hopCount + 1) / 2;
                wifiConnection = btConnection;
            }

            long wifiTime = wifiConnection * 5000L * 2; // two way message. FilePacket and Ack
            long btTime = btConnection * 8000L * 2; // two way message File packet and ack


            long totalTime = wifiTime + btTime;

            MeshLog.w("[File-Time-Tracker] estimation time: " + totalTime);

            return totalTime;
        }

        return FILE_PACKET_REACHABLE_TIME_LIMIT;
    }

    public void prepareMultihopFileAckPacketAndSend(FilePacket filePacket) {
        String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
        prepareAckPacketAndSend(filePacket.mSourceAddress, fileMessageId, filePacket.mTransferredBytes);
    }
}
