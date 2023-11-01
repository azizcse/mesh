package com.letbyte.core.meshfilesharing.core;

import android.util.Base64;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.core.listeners.ReceiverFileStateHandler;
import com.letbyte.core.meshfilesharing.core.listeners.SenderFileStateHandler;
import com.letbyte.core.meshfilesharing.data.BuyerFileAck;
import com.letbyte.core.meshfilesharing.data.BuyerFileMessage;
import com.letbyte.core.meshfilesharing.data.BuyerFilePacket;
import com.letbyte.core.meshfilesharing.data.BuyerFileResumeRequestFromSender;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;

/**
 * <h1>
 * This class is responsible for
 * Buyer to buyer and seller to buyer file sharing through tunneling
 * Prepare file packet and send to target node
 *
 * </h1>
 */
public class BuyerFileManager {
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


    private LinkStateListener linkStateListener = null;


    public BuyerFileManager(MeshFileManager manager, DatabaseService dbService, TransportManagerX mTransportManagerX,
                            SenderFileStateHandler mSenderFileStateHandler, ReceiverFileStateHandler mReceiverFileStateHandler,
                            LinkStateListener linkStateListener) {
        this.meshFileManager = manager;
        this.mDatabaseService = dbService;
        this.mFileHelper = new FileHelper();
        this.mTransportManagerX = mTransportManagerX;
        this.mSenderFileStateHandler = mSenderFileStateHandler;
        this.mReceiverFileStateHandler = mReceiverFileStateHandler;
        this.linkStateListener = linkStateListener;

    }

    public void prepareAndSendBuyerFileMessage(String receiverId, String filePath, byte[] msgMetaData, String appToken, String fileMessageId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] arr = fileMessageId.split(FileHelper.FILE_ID_SEPARATOR);
                long transferId = Long.parseLong(arr[1]);


                FilePacket oldPacket = mDatabaseService.getFilePackets(mTransportManagerX.getMyNodeId(), transferId, Const.FileStatus.FAILED);

                if (oldPacket == null) {
                    BuyerFileMessage buyerFileMessage = new BuyerFileMessage(
                            mTransportManagerX.getMyNodeId(),
                            receiverId,
                            transferId,
                            mFileHelper.getFileName(filePath),
                            mFileHelper.getFileSize(filePath),
                            appToken);
                    buyerFileMessage.messageMetaData = Base64.encodeToString(msgMetaData, Base64.DEFAULT);

                    FilePacket filePacket = prepareFilePacket(filePath, buyerFileMessage);
                    //Insert very first packet of buyer file message
                    mDatabaseService.insertFilePacket(filePacket);
                    MeshLog.v("Buyer_file send metadata message");
                    //Send file meta info message
                    mTransportManagerX.sendBuyerFileMessage(receiverId, buyerFileMessage.toJson().getBytes(), Constant.FileMessageType.FILE_INFO_MESSAGE, null, fileMessageId);

                } else {
                    BuyerFileResumeRequestFromSender buyerFileResumeRequest = new BuyerFileResumeRequestFromSender(transferId);

                    mTransportManagerX.sendBuyerFileMessage(receiverId, buyerFileResumeRequest.toJson().getBytes(), Constant.FileMessageType.FILE_INFO_MESSAGE, null, fileMessageId);
                }
            }
        }).start();
    }

    /**
     * <h1>Prepare only initial file packet with all information</h1>
     *
     * @param filePath    : String (required)
     * @param fileMessage : BuyerFileMessage (required)
     * @return FilePacket obj
     */
    private FilePacket prepareFilePacket(String filePath, BuyerFileMessage fileMessage) {
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

    private BuyerFileMessage prepareBuyerFileMessage(FilePacket filePacket) {
        BuyerFileMessage buyerFileMessage = new BuyerFileMessage(
                filePacket.mSourceAddress,
                filePacket.mPeerAddress,
                filePacket.mFileId,
                filePacket.mFileName,
                filePacket.mFileSize,
                filePacket.appToken);
        buyerFileMessage.messageMetaData = filePacket.metaData;
        return buyerFileMessage;
    }

    /**
     * <h1>
     * Received initial packet for file
     * All file related info and type
     * </h2>
     *
     * @param senderAddress
     * @param buyerFileMessage
     */
    public void onReceiveBuyerFileMessage(String senderAddress, BuyerFileMessage buyerFileMessage) {
        FilePacket oldFilePacket = mDatabaseService.getFilePackets(senderAddress, buyerFileMessage.fileTransferId);
        if (oldFilePacket == null) {

            String filePath = mFileHelper.generateFilePath(null, buyerFileMessage.fileName);
            FilePacket filePacket = prepareFilePacket(filePath, buyerFileMessage);
            filePacket.mSelfFullFilePath = filePath;
            filePacket.fileStatus = Const.FileStatus.INPROGRESS;
            String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);


//            if (PreferencesHelper.on().getDataShareMode() == PreferencesHelper.DATA_BUYER && !this.linkStateListener.onBalanceVerify(buyerFileMessage.fileSize, hopList)) {
//                BuyerFileAck buyerFileAck = new BuyerFileAck(fileMessageId, 0);
//                buyerFileAck.fileInfoStatus = FILE_BALANCE_EXCEED;
//
//                mTransportManagerX.sendBuyerFileMessage(senderAddress, buyerFileAck.toJson().getBytes(), true);
//
//            }
//            else {
            //New file message received
            MeshLog.v("Buyer_file new file  message received from internet");

            // Store in BroadcastDB
            mDatabaseService.insertFilePacket(filePacket);


            MeshFileEventListener meshFileEventListener = meshFileManager.getMeshEventListener();
            if (meshFileEventListener != null) {
                meshFileEventListener.onFileReceiveStarted(senderAddress, fileMessageId, filePath, Base64.decode(buyerFileMessage.messageMetaData, Base64.DEFAULT), buyerFileMessage.appToken);
            } else {
                MeshLog.v("File ui callback is null");
            }

            //Send ACK for packet data
            prepareAckPacketAndSend(senderAddress, fileMessageId, 0, buyerFileMessage.immediateSender);

//            }
        } else {
            //File packet already exist in DB
            MeshLog.v("Buyer_file file  message received but old file exist");
            //TODO file resume related work
        }
    }

    /**
     * <h1>
     * Receive file packet and save received data
     * Make a request for next packet
     * </h1>
     *
     * @param senderAddress   : String (Required) Source address
     * @param buyerFilePacket : Buyer packet address
     */
    public void onReceiveBuyerFilePacket(String senderAddress, BuyerFilePacket buyerFilePacket) {
        FilePacket filePacket = mDatabaseService.getFilePackets(buyerFilePacket.sourceAddress,
                buyerFilePacket.fileTransferId);
        byte[] data = Base64.decode(buyerFilePacket.data, Base64.DEFAULT);
        filePacket.mData = data;
        mFileHelper.writePacketData(filePacket);
        filePacket.mTransferredBytes += data.length;

        MeshLog.i("Buyer_file packet received. packet size: " + data.length);
        int currentPercentage = FileHelper.getPercentage(filePacket);
        mReceiverFileStateHandler.onFilePercentProgress(filePacket, currentPercentage);

        boolean isFileReceiveDone = false;
        if (filePacket.mTransferredBytes >= filePacket.mFileSize) {
            filePacket.fileStatus = Const.FileStatus.FINISH;
            MeshLog.v("Buyer_file  File receive ");
            isFileReceiveDone = true;
        }

        mDatabaseService.updateFilePacket(filePacket);

        String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
        prepareAckPacketAndSend(senderAddress, fileMessageId, filePacket.mTransferredBytes, buyerFilePacket.immediateSender);

        if (isFileReceiveDone) {
            mReceiverFileStateHandler.onFileTransferFinish(filePacket);
        }
    }

    public void onReceiveBuyerFilePacketSendAck(String senderAddress, BuyerFileAck buyerFileAck) {

        String[] arr = buyerFileAck.fileMessageId.split(FileHelper.FILE_ID_SEPARATOR);
        long transferId = Long.parseLong(arr[1]);
        FilePacket filePacket = mDatabaseService.getFilePackets(arr[0], transferId);
        filePacket.fileStatus = Const.FileStatus.INPROGRESS;

        // At receiver side file info not exist
        // This is work when resume request send from sender and at receiver side file info not exist
        if (buyerFileAck.fileInfoStatus == FILE_INFO_NOT_EXIST) {
            filePacket.mTransferredBytes = 0;
            BuyerFileMessage buyerFileMessage = prepareBuyerFileMessage(filePacket);
            mDatabaseService.updateFilePacket(filePacket);
            mTransportManagerX.sendBuyerFileMessage(senderAddress, buyerFileMessage.toJson().getBytes(), Constant.FileMessageType.FILE_INFO_MESSAGE, buyerFileAck.immediateSender, buyerFileAck.fileMessageId);
            return;
        } else if (buyerFileAck.fileInfoStatus == MeshFile.FILE_BALANCE_EXCEED) {
            filePacket.fileStatus = Const.FileStatus.FAILED;
            mDatabaseService.updateFilePacket(filePacket);

            mSenderFileStateHandler.onFileTransferError(filePacket, MeshFile.FILED_BALANCE_EXCEED);
            return;
        }

        if (buyerFileAck.receivedBytes < filePacket.mFileSize) {

            BuyerFilePacket buyerFilePacket = new BuyerFilePacket(filePacket.mFileId);
            buyerFilePacket.sourceAddress = filePacket.mSourceAddress;
            buyerFilePacket.destinationId = filePacket.mPeerAddress;

            //Byte count that receiver already ready received
            filePacket.mTransferredBytes = buyerFileAck.receivedBytes;
            MeshLog.v("Buyer_file  alredy send :" + buyerFileAck.receivedBytes + " Total :" + filePacket.mFileSize);

            buyerFilePacket.data = Base64.encodeToString(mFileHelper.readPacketDataForBle(filePacket, false), Base64.DEFAULT);

            mDatabaseService.updateFilePacket(filePacket);

            int currentPercentage = FileHelper.getPercentage(filePacket);

            mSenderFileStateHandler.onFilePercentProgress(filePacket, currentPercentage);

            mTransportManagerX.sendBuyerFileMessage(senderAddress, buyerFilePacket.toJson().getBytes(),
                    Constant.FileMessageType.FILE_PACKET_MESSAGE, buyerFileAck.immediateSender, buyerFileAck.fileMessageId);

        } else {
            filePacket.mTransferredBytes = filePacket.mFileSize;
            filePacket.fileStatus = Const.FileStatus.FINISH;
            mDatabaseService.updateFilePacket(filePacket);
            mSenderFileStateHandler.onFilePercentProgress(filePacket, 100);
            mSenderFileStateHandler.onFileTransferFinish(filePacket);
        }
    }


    public void sendResumeAck(String fileMessageId, FilePacket filePacket) {
        FilePacket oldFilePacket = mDatabaseService.getFilePackets(filePacket.mSourceAddress, filePacket.mFileId);
        if (oldFilePacket != null) {
            prepareAckPacketAndSend(oldFilePacket.mSourceAddress, fileMessageId, oldFilePacket.mTransferredBytes, null);
        }
    }


    /**
     * ACK packet prepare and send
     *
     * @param receiver
     * @param fileId
     * @param byteCount
     */
    private void prepareAckPacketAndSend(String receiver, String fileId, long byteCount, String immediateSender) {
        BuyerFileAck buyerFileAck = new BuyerFileAck(fileId, byteCount);
        mTransportManagerX.sendBuyerFileMessage(receiver, buyerFileAck.toJson().getBytes(), Constant.FileMessageType.FILE_ACK_MESSAGE, immediateSender, fileId);
    }

    /**
     * <h1>
     * Receive file resume trigger action from send
     * Peek file packet from DB
     * if file packet exist then send id and transfer byte count
     * if not then set file info not exist status
     * </h1>
     *
     * @param senderAddress          (required) : String sender address
     * @param buyerFileResumeRequest (required) : Obj
     */
    public void onReceivedBuyerFileResumeRequest(String senderAddress, BuyerFileResumeRequestFromSender buyerFileResumeRequest) {

        FilePacket oldFilePacket = mDatabaseService.getFilePackets(senderAddress, buyerFileResumeRequest.fileTransferId);
        MeshLog.v("Buyer_file received file resume request++++");
        if (oldFilePacket != null) {
            String fileMessageId = mFileHelper.getFileMessageId(oldFilePacket.mSourceAddress, oldFilePacket.mFileId);
            prepareAckPacketAndSend(oldFilePacket.mSourceAddress, fileMessageId, oldFilePacket.mTransferredBytes, buyerFileResumeRequest.immediateSender);
        } else {
            String fileMessageId = mFileHelper.getFileMessageId(senderAddress, buyerFileResumeRequest.fileTransferId);
            BuyerFileAck buyerFileAck = new BuyerFileAck(fileMessageId, 0);
            buyerFileAck.fileInfoStatus = FILE_INFO_NOT_EXIST;
            mTransportManagerX.sendBuyerFileMessage(senderAddress, buyerFileAck.toJson().getBytes(),
                    Constant.FileMessageType.FILE_ACK_MESSAGE, buyerFileResumeRequest.immediateSender,fileMessageId);
        }
    }
}
