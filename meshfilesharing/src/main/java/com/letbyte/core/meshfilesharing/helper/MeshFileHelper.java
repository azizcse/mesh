package com.letbyte.core.meshfilesharing.helper;

import android.util.Base64;

import com.letbyte.core.meshfilesharing.data.BTFileRequestMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FileRequestResumeMessageFromSender;
import com.letbyte.core.meshfilesharing.data.FileMessage;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.FileResendRequestMessage;
import com.letbyte.core.meshfilesharing.data.FileResumeRequestMessageFromReceiver;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;

import java.io.File;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 1:49 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 1:49 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 1:49 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class MeshFileHelper {

    /**
     * Beware: changing this value would impact on performance and compatibility
     */
    public static final long FILE_PACKET_SIZE = 50 * 1024;
    public static final long FILE_PACKET_SIZE_BLE = 10 * 1024;
    public static final long FILE_PACKET_SIZE_MULTIHOP = 20 * 1024;
    public static final long BLE_BROADCAST_FILE_SIZE = 200 * 1024;
    //    public static final long FILE_PACKET_SIZE = 3;
    public static final long MAXIMUM_DURATION_TO_STORE_FILE_CACHE = 24 * 60 * 60 * 1000;

    private FileHelper mFileHelper;

    public MeshFileHelper() {
        mFileHelper = new FileHelper();
    }

    /**
     * Generate a unique value for other peer's id and provided file path. This combination should
     * be unique for a system or node. Uses file path, peer id and current time to generate the id     *
     *
     * @param peerId
     * @param filePath
     * @return
     */
    public long generateFileId(String peerId, String filePath) {
        return (peerId + filePath).hashCode() + System.currentTimeMillis();
    }

    public FileMessage generateFileMessage(String senderAddress, String peerId, String filePath, byte[] msgMetaData, String appToken) {
        FileMessage fileMessage = null;
        if (Text.isNotEmpty(filePath) && AddressUtil.isValidEthAddress(peerId)) {

            File file = new File(filePath);
            if (file.exists()) {

                fileMessage = new FileMessage(generateFileId(peerId, filePath));
                fileMessage.mPeerAddress = peerId;
                fileMessage.mFilePath = filePath;
                fileMessage.appToken = appToken;
                fileMessage.mFileName = mFileHelper.getFileName(filePath);
                fileMessage.mFileSize = mFileHelper.getFileSize(filePath);
                fileMessage.mSourceAddress = senderAddress;

                fileMessage.messageMetaData = Base64.encodeToString(msgMetaData, Base64.DEFAULT);

//                fileMessage.mFileName = mFileHelper.getExtension(filePath);

            } else {
                MeshLog.e("FileMessageTest", "File not exist");
            }
        } else {
            MeshLog.e("FileMessageTest", "File path empty");
        }

        return fileMessage;
    }

    public FilePacket getFilePacketFromFileMessage(FileMessage fileMessage) {
        FilePacket filePacket = null;
        if (fileMessage != null) {
            filePacket = new FilePacket(fileMessage.mFileTransferId);
            filePacket.mFileName = fileMessage.mFileName;
            filePacket.mSelfFullFilePath = filePacket.mPeerFullFilePath = fileMessage.mFilePath;
            filePacket.mFileSize = fileMessage.mFileSize;
            filePacket.mPeerAddress = fileMessage.mPeerAddress;
            filePacket.appToken = fileMessage.appToken;
            filePacket.mSourceAddress = fileMessage.mSourceAddress;
            filePacket.mTransferredBytes = fileMessage.mTransferredBytes;
            filePacket.metaData = fileMessage.messageMetaData;
        }
        return filePacket;
    }

    public BroadcastFilePacket getBroadcastFilePacket(BroadcastMessage broadcastMessage) {
        BroadcastFilePacket filePacket = null;
        if (broadcastMessage != null) {
            filePacket = new BroadcastFilePacket(broadcastMessage.mFileTransferId);
            filePacket.mFileName = broadcastMessage.mFileName;
            filePacket.mSelfFullFilePath = filePacket.mPeerFullFilePath = broadcastMessage.mFilePath;
            filePacket.mFileSize = broadcastMessage.mFileSize;
            filePacket.mPeerAddress = broadcastMessage.mPeerAddress;
            filePacket.appToken = broadcastMessage.appToken;
            filePacket.mSourceAddress = broadcastMessage.mSourceAddress;
            filePacket.mTransferredBytes = broadcastMessage.mTransferredBytes;
            filePacket.mBroadcastId = broadcastMessage.mBroadcastContentId;
            filePacket.mBroadcastText = broadcastMessage.mBroadcastTextData;
            filePacket.mBroadcastExpireTime = broadcastMessage.mBroadcastExpireTime;
            filePacket.broadcastLatitude = broadcastMessage.broadcastLatitude;
            filePacket.broadcastLongitude = broadcastMessage.broadcastLongitude;
            filePacket.broadcastRange = broadcastMessage.broadcastRange;
        }
        return filePacket;
    }

    public Broadcast getBroadcastFromFilePacket(BroadcastFilePacket broadcastFilePacket) {

        BroadcastMessage fileMessage = getBroadcastContentMessage(broadcastFilePacket);
        String fileMessageJson = fileMessage.toJson();

        return new Broadcast().setReceiverId(broadcastFilePacket.mPeerAddress)
                .setSenderId(broadcastFilePacket.mSourceAddress)
                .setBroadcastId(broadcastFilePacket.mBroadcastId)
                .setBroadcastMeta(broadcastFilePacket.mBroadcastText)
                .setContentPath(broadcastFilePacket.mSelfFullFilePath)
                .setAppToken(broadcastFilePacket.appToken)
                .setExpiryTime(broadcastFilePacket.mBroadcastExpireTime)
                .setLatitude(broadcastFilePacket.broadcastLatitude)
                .setLongitude(broadcastFilePacket.broadcastLongitude)
                .setRange(broadcastFilePacket.broadcastRange)
                .setType(JsonDataBuilder.APP_BROADCAST_MESSAGE)
                .setBaseMessage(fileMessageJson);
    }

    public Broadcast getBroadcastFromMessage(BroadcastMessage broadcastMessage) {

        String json = broadcastMessage.toJson();

        return new Broadcast().setReceiverId(broadcastMessage.mPeerAddress)
                .setSenderId(broadcastMessage.mSourceAddress)
                .setBroadcastId(broadcastMessage.mBroadcastContentId)
                .setBroadcastMeta(broadcastMessage.mBroadcastTextData)
                .setContentPath(broadcastMessage.mFilePath)
                .setAppToken(broadcastMessage.appToken)
                .setExpiryTime(broadcastMessage.mBroadcastExpireTime)
                .setLatitude(broadcastMessage.broadcastLatitude)
                .setLongitude(broadcastMessage.broadcastLongitude)
                .setRange(broadcastMessage.broadcastRange)
                .setType(JsonDataBuilder.APP_BROADCAST_MESSAGE)
                .setBaseMessage(json);
    }

    public BroadcastMessage getBroadcastMessage(Broadcast broadcast) {

        BroadcastMessage broadcastMessage = new BroadcastMessage(generateFileId(broadcast.getReceiverId(), broadcast.getContentPath()));
        broadcastMessage.mBroadcastContentId = broadcast.getBroadcastId();
        broadcastMessage.mBroadcastTextData = broadcast.getBroadcastMeta();
        broadcastMessage.mPeerAddress = broadcast.getReceiverId();
        broadcastMessage.mFilePath = broadcast.getContentPath();
        broadcastMessage.appToken = broadcast.getAppToken();
        broadcastMessage.mFileName = mFileHelper.getFileName(broadcast.getContentPath());
        broadcastMessage.mFileSize = mFileHelper.getFileSize(broadcast.getContentPath());
        broadcastMessage.mSourceAddress = broadcast.getSenderId();
        broadcastMessage.mBroadcastExpireTime = broadcast.getExpiryTime();
        broadcastMessage.broadcastLatitude = broadcast.getLatitude();
        broadcastMessage.broadcastLongitude = broadcast.getLongitude();
        broadcastMessage.broadcastRange = broadcast.getRange();

        return broadcastMessage;
    }

    public FileMessage get(FilePacket filePacket) {
        FileMessage fileMessage = null;
        if (filePacket != null) {
            fileMessage = new FileMessage(filePacket.mFileId);
            fileMessage.mFileName = filePacket.mFileName;
            fileMessage.mFilePath = filePacket.mSelfFullFilePath;
            fileMessage.mFileSize = filePacket.mFileSize;
            fileMessage.mPeerAddress = filePacket.mPeerAddress;
            fileMessage.appToken = filePacket.appToken;
            fileMessage.mSourceAddress = filePacket.mSourceAddress;
            fileMessage.mTransferredBytes = filePacket.mTransferredBytes;
            fileMessage.messageMetaData = filePacket.metaData;
        }
        return fileMessage;
    }

    public BroadcastMessage getBroadcastContentMessage(BroadcastFilePacket broadcastFilePacket) {
        BroadcastMessage fileMessage = null;
        if (broadcastFilePacket != null) {
            fileMessage = new BroadcastMessage(broadcastFilePacket.mFileId);
            fileMessage.mFileName = broadcastFilePacket.mFileName;
            fileMessage.mFilePath = broadcastFilePacket.mSelfFullFilePath;
            fileMessage.mFileSize = broadcastFilePacket.mFileSize;
            fileMessage.mPeerAddress = broadcastFilePacket.mPeerAddress;
            fileMessage.appToken = broadcastFilePacket.appToken;
            fileMessage.mSourceAddress = broadcastFilePacket.mSourceAddress;
            fileMessage.mTransferredBytes = broadcastFilePacket.mTransferredBytes;
            fileMessage.messageMetaData = broadcastFilePacket.metaData;
            fileMessage.mBroadcastContentId = broadcastFilePacket.mBroadcastId;
            fileMessage.mBroadcastTextData = broadcastFilePacket.mBroadcastText;
            fileMessage.mBroadcastExpireTime = broadcastFilePacket.mBroadcastExpireTime;
            fileMessage.broadcastLatitude = broadcastFilePacket.broadcastLatitude;
            fileMessage.broadcastLongitude = broadcastFilePacket.broadcastLongitude;
            fileMessage.broadcastRange = broadcastFilePacket.broadcastRange;
        }
        return fileMessage;
    }

    public FilePacket get(FileResumeRequestMessageFromReceiver fileResumeRequestMessageFromReceiver) {
        FilePacket filePacket = null;

        if (fileResumeRequestMessageFromReceiver != null) {

            filePacket = new FilePacket(fileResumeRequestMessageFromReceiver.mFileTransferId);

            //Originals source address is the actual target address
            filePacket.mSourceAddress = fileResumeRequestMessageFromReceiver.mPeerAddress;
            filePacket.mPeerAddress = fileResumeRequestMessageFromReceiver.mRequesterAddress;
            filePacket.mFileSize = fileResumeRequestMessageFromReceiver.mFileSize;

            filePacket.mTransferredBytes = fileResumeRequestMessageFromReceiver.mRequestingBytesFrom;

            //Maintaining this relative offset is very important as an intermediate node or file
            //forwarder maintains only fragmented files in this sort of cases. So reading local disk
            //files with relative index and sending them to other node is with the absolute
            //index
            filePacket.mRelativeOffset = fileResumeRequestMessageFromReceiver.mRequestingBytesFrom;

            filePacket.mSelfFullFilePath = mFileHelper.generateFilePath(null,
                    "resume_" + fileResumeRequestMessageFromReceiver.mFileTransferId + ".tmp");
        }
        return filePacket;
    }


    public FileResendRequestMessage getResendRequest(String sourceAddress, String destinationAddress, long fileTransferId, String appToken) {
        return new FileResendRequestMessage(sourceAddress, destinationAddress, fileTransferId, appToken);
    }

    public FileResumeRequestMessageFromReceiver getResumeRequest(FilePacket filePacket) {
        FileResumeRequestMessageFromReceiver fileResumeRequestMessageFromReceiver = null;

        if (filePacket != null) {

            fileResumeRequestMessageFromReceiver = new FileResumeRequestMessageFromReceiver();

            //Originals source address is the actual target address
            fileResumeRequestMessageFromReceiver.mPeerAddress = filePacket.mSourceAddress;

            fileResumeRequestMessageFromReceiver.mRequesterAddress = filePacket.mPeerAddress;
            fileResumeRequestMessageFromReceiver.mFileTransferId = filePacket.mFileId;
            fileResumeRequestMessageFromReceiver.mRequestingBytesFrom = filePacket.mTransferredBytes;
            fileResumeRequestMessageFromReceiver.mFileSize = filePacket.mFileSize;

            fileResumeRequestMessageFromReceiver.appToken = filePacket.appToken;

        }
        return fileResumeRequestMessageFromReceiver;
    }

    public FileRequestResumeMessageFromSender buildRequestDestinationForCallResumeMessage(FilePacket filePacket) {
        FileRequestResumeMessageFromSender message = new FileRequestResumeMessageFromSender();
        message.appToken = filePacket.appToken;
        message.fileTransferId = filePacket.mFileId;
        message.mPeerAddress = filePacket.mPeerAddress;
        message.mSourceAddress = filePacket.mSourceAddress;
        return message;
    }

    public BTFileRequestMessage getBTFileRequest(FilePacket filePacket) {
        BTFileRequestMessage btFileRequestMessage = null;
        if (filePacket != null) {
            btFileRequestMessage = new BTFileRequestMessage(filePacket.mFileId);
            btFileRequestMessage.mPeerAddress = filePacket.mPeerAddress;
            btFileRequestMessage.mSourceAddress = filePacket.mSourceAddress;
            btFileRequestMessage.mTransferredBytes = filePacket.mTransferredBytes;
        }

        return btFileRequestMessage;
    }


}
