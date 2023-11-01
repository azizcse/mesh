package com.letbyte.core.meshfilesharing.comm.ble;

import android.util.Base64;

import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.core.listeners.ReceiverFileStateHandler;
import com.letbyte.core.meshfilesharing.core.listeners.SenderFileStateHandler;
import com.letbyte.core.meshfilesharing.data.BTFileRequestMessage;
import com.letbyte.core.meshfilesharing.data.BleFileAckMessage;
import com.letbyte.core.meshfilesharing.data.BleFileMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FileMessage;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.util.MeshLog;

import java.util.concurrent.ConcurrentHashMap;

public class BleFileManager {
    private MeshFileManager meshFileManager;
    private DatabaseService mDatabaseService;
    private FileHelper mFileHelper;
    private TransportManagerX mTransportManagerX;

    //private FilePacket mIncomingFilePacket;
    private ConcurrentHashMap<String, FilePacket> mIncomingFilePacketQueue;
    //private FilePacket mOutgoingFilePacket;
    private ConcurrentHashMap<String, FilePacket> mOutgoingFilePacketQueue;

    private SenderFileStateHandler mSenderFileStateHandler;
    private ReceiverFileStateHandler mReceiverFileStateHandler;


    public BleFileManager(MeshFileManager manager, DatabaseService dbService, TransportManagerX mTransportManagerX,
                          SenderFileStateHandler mSenderFileStateHandler, ReceiverFileStateHandler mReceiverFileStateHandler) {
        this.meshFileManager = manager;
        this.mDatabaseService = dbService;
        this.mFileHelper = new FileHelper();
        this.mTransportManagerX = mTransportManagerX;
        this.mSenderFileStateHandler = mSenderFileStateHandler;
        this.mReceiverFileStateHandler = mReceiverFileStateHandler;

    }

    public void onFileRequestFromReceiver(BTFileRequestMessage btFileRequestMessage) {
        MeshLog.v("Content broadcast preparing to send ");
        FilePacket filePacket = mDatabaseService.getFilePackets(btFileRequestMessage.mSourceAddress,
                btFileRequestMessage.mFileTransferId);

        sendNextPacket(filePacket, btFileRequestMessage.mFileTransferId);
    }


    public void onSendFilePacket(boolean isSuccess, String fileMessageId) {
        String[] fileIdArr = fileMessageId.split(FileHelper.FILE_ID_SEPARATOR);
        String source = fileIdArr[0];
        long fileTransferId = Long.parseLong(fileIdArr[1]);
        FilePacket filePacket = mDatabaseService.getFilePackets(source, fileTransferId);
        boolean isNeedToSend = false;
        if (isSuccess) {
            MeshLog.i("Content broadcast. File packet send successfully");
            long pendingSize = filePacket.mFileSize - filePacket.mTransferredBytes;
            if (pendingSize <= MeshFileHelper.FILE_PACKET_SIZE_BLE) {
                filePacket.mTransferredBytes = filePacket.mFileSize;
                filePacket.fileStatus = Const.FileStatus.FINISH;
                MeshLog.v("Content broadcast full packet send");
            } else {
                filePacket.mTransferredBytes += MeshFileHelper.FILE_PACKET_SIZE_BLE;
                isNeedToSend = true;
            }

        } else {
            filePacket.fileStatus = Const.FileStatus.FAILED;
        }

        mDatabaseService.updateFilePacket(filePacket);

        if (isNeedToSend) {
            MeshLog.v("Content broadcast send next packet");
            sendNextPacket(filePacket, fileTransferId);
        } else {
            MeshLog.i("Content broadcast. File send successfully done");
            if (mSenderFileStateHandler != null) {

                if (mOutgoingFilePacketQueue == null) {
                    mOutgoingFilePacketQueue = new ConcurrentHashMap<>();
                }
                FilePacket mOutgoingFilePacket = mOutgoingFilePacketQueue.get(fileMessageId);

                if (mOutgoingFilePacket != null) {
                    addLatestDataToPacket(mOutgoingFilePacket, filePacket);
                }

                FilePacket f;
                if (mOutgoingFilePacket instanceof BroadcastFilePacket) {
                    f = ((BroadcastFilePacket) mOutgoingFilePacket).copyBroadCastFilePacket();
                } else {
                    f = filePacket;
                }

                mSenderFileStateHandler.onFileTransferFinish(f);
            }
        }
    }

    public void onReceiveFilePacket(BleFileMessage bleFileMessage) {
        FilePacket filePacket = mDatabaseService.getFilePackets(bleFileMessage.mSourceAddress,
                bleFileMessage.mFileTransferId);

        byte[] data = Base64.decode(bleFileMessage.data, Base64.DEFAULT);

        filePacket.mData = data;

        mFileHelper.writePacketData(filePacket);
        filePacket.mTransferredBytes += data.length;

        MeshLog.i("Content broadcast file packet received. packet size: " + data.length);

        boolean isFileReceiveDone = false;
        if (filePacket.mTransferredBytes >= filePacket.mFileSize) {
            filePacket.fileStatus = Const.FileStatus.FINISH;
            MeshLog.v("Content broadcast. File receive ");
            isFileReceiveDone = true;
        }

        mDatabaseService.updateFilePacket(filePacket);

        String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
        MeshLog.v("Content broadcast ACK send after file receive");
        // Send ACK to sender
        prepareAndSendAck(filePacket.mSourceAddress, fileMessageId);

        if (isFileReceiveDone) {
            if (mIncomingFilePacketQueue == null) {
                mIncomingFilePacketQueue = new ConcurrentHashMap<>();
            }
            FilePacket mIncomingFilePacket = mIncomingFilePacketQueue.get(fileMessageId);

            if (mIncomingFilePacket != null && mReceiverFileStateHandler != null) {

                addLatestDataToPacket(mIncomingFilePacket, filePacket);

                FilePacket f;
                if (mIncomingFilePacket instanceof BroadcastFilePacket) {
                    f = ((BroadcastFilePacket) mIncomingFilePacket).copyBroadCastFilePacket();
                } else {
                    f = filePacket;
                }


                mReceiverFileStateHandler.onFileTransferFinish(f);
            }
        }
    }

    public void sendFile(FilePacket filePacket) {
        if (mOutgoingFilePacketQueue == null) {
            mOutgoingFilePacketQueue = new ConcurrentHashMap<>();
        }
        String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
        mOutgoingFilePacketQueue.put(fileId, filePacket);
    }

    public void onFileRequestFromSender(FileMessage fileMessage, FilePacket filePacket) {
        // Basically we are thinking now broadcast file
        if (fileMessage instanceof BroadcastMessage) {
            BTFileRequestMessage btFileRequest = meshFileManager.getMeshFileHelper().getBTFileRequest(filePacket);
            String json = btFileRequest.toJson();
            mTransportManagerX.sendFileMessage(btFileRequest.mSourceAddress, json.getBytes());
        }

        if (mIncomingFilePacketQueue == null) {
            mIncomingFilePacketQueue = new ConcurrentHashMap<>();
        }

        String fileMessageId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
        mIncomingFilePacketQueue.put(fileMessageId, filePacket);
        //mIncomingFilePacket = filePacket;
    }

    private void addLatestDataToPacket(FilePacket old, FilePacket latest) {
        // Adding latest data to assigned object
        old.mFileId = latest.mFileId;
        old.mPeerAddress = latest.mPeerAddress;
        old.mSourceAddress = latest.mSourceAddress;
        old.mSelfFullFilePath = latest.mSelfFullFilePath;
        old.mPeerFullFilePath = latest.mPeerFullFilePath;
        old.mFileName = latest.mFileName;
        old.mFileSize = latest.mFileSize;
        old.mData = latest.mData;
        old.mLastModified = latest.mLastModified;
        old.fileStatus = latest.fileStatus;
        old.appToken = latest.appToken;
        old.mTransferredBytes = latest.mTransferredBytes;
    }

    private void sendNextPacket(FilePacket filePacket, long mFileTransferId) {
        if (filePacket != null) {
            BleFileMessage fileMessage = new BleFileMessage();
            fileMessage.mFileTransferId = mFileTransferId;
            fileMessage.mSourceAddress = filePacket.mSourceAddress;
            fileMessage.mPeerAddress = filePacket.mPeerAddress;
            //fileMessage.data = mFileHelper.readPacketDataForBle(filePacket);
            fileMessage.data = Base64.encodeToString(mFileHelper.readPacketDataForBle(filePacket, true), Base64.DEFAULT);

            byte[] data = fileMessage.toJson().getBytes();

            MeshLog.i("Content broadcast packet size after json " + data.length);

            String messageId = mTransportManagerX.sendFileToBle(filePacket.mPeerAddress, data);
            if (messageId != null) {
                //Message added to sdk
                MeshLog.v("Content broadcast file send to BLE");
            } else {
                MeshLog.e("Content broadcast file send to BLE error");
            }
        } else {
            MeshLog.e("Content broadcast file packet null");
        }
    }

    private void prepareAndSendAck(String receiver, String fileMessageId) {
        BleFileAckMessage fileAckMessage = new BleFileAckMessage(fileMessageId);
        mTransportManagerX.sendFileToBle(receiver, fileAckMessage.toJson().getBytes());
    }

}
