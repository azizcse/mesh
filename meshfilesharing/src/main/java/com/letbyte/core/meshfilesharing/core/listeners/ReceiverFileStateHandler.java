package com.letbyte.core.meshfilesharing.core.listeners;

import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.comm.fileserver.webserver.HttpFileClient;
import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FileMessage;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;

import java.util.Map;

import timber.log.Timber;

public class ReceiverFileStateHandler extends AbstractFileStateHandler
        implements IClientFileStateListener {

    public ReceiverFileStateHandler(DatabaseService databaseService, FileHelper fileHelper,
                                    MeshFileManager meshFileManager,
                                    MeshFileEventListener meshFileEventListener) {
        super(databaseService, fileHelper, meshFileManager, meshFileEventListener);
    }

    @Override
    public void onFilePercentProgress(FilePacket filePacket, int percentProgress) {
        if (filePacket != null) {
            super.onFilePercentProgress(filePacket, percentProgress);
            Timber.d("File_progress  receiver :%s", filePacket.toString());
            if (mMyAddress.equals(filePacket.mPeerAddress)) {//Me is the recipient
                if (mMeshFileEventListener != null) {
                    String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
                    mMeshFileEventListener.onFileProgress(fileId, percentProgress, filePacket.appToken);
                }
            }
        }
    }

    @Override
    public void onFirstChunk(FilePacket filePacket, int bytes) {

        if (mMeshFileManager != null) {
            mMeshFileManager.onByteReceived(filePacket, bytes);
        }
    }

    @Override
    public void onFileTransferFinish(FilePacket filePacket) {
        if (filePacket != null) {
            super.onFileTransferFinish(filePacket);
            RoutingEntity senderAddress = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(filePacket.mSourceAddress);
            boolean isNextABTNode = RoutingEntity.isBtNode(senderAddress);

            if (mMyAddress.equals(filePacket.mPeerAddress)) {//Me is the recipient
                if (mMeshFileEventListener != null) {

                    // File receive success so we can remove the current file receiving id from map
                    mMeshFileManager.removeFileReceiverMap(mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId));

                    mMeshFileManager.onProcessNextFileSend(true, filePacket.mSourceAddress, true);
                    String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);

                    if (filePacket instanceof BroadcastFilePacket) {
                        mMeshFileEventListener.onBroadcastFileTransferFinish(((BroadcastFilePacket) filePacket).mBroadcastId,
                                ((BroadcastFilePacket) filePacket).mBroadcastText,
                                filePacket.mSelfFullFilePath,
                                filePacket.mSourceAddress,
                                filePacket.appToken);
                    } else {
                        mMeshFileEventListener.onFileTransferFinish(fileId, filePacket.appToken);
                    }
                }
                mDatabaseService.deletePacket(filePacket);
            } else {

                //Serialized file transfer for BT-WiFi configuration
                if (isNextABTNode) {

                    FilePacket f = filePacket.copy();
                    f.mTransferredBytes = 0;
                    FileMessage fileMessage = new MeshFileHelper().get(f);
                    if (fileMessage != null) {
                        mMeshFileManager.sendFile(fileMessage);
                    }

                }
            }

            if (isNextABTNode) {
                //Receive finished, so try for next receive
                mMeshFileManager.onBTFileReceiveFinish(filePacket);
            }
        }
    }

    @Override
    public void onFileTransferError(FilePacket filePacket, int errorCode) {
        if (filePacket != null) {
            super.onFileTransferError(filePacket, errorCode);
            mDatabaseService.updateFilePacket(filePacket);

            if (mMyAddress.equals(filePacket.mPeerAddress)) {//Me is the recipient
                if (mMeshFileEventListener != null) {
                    String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
                    mMeshFileEventListener.onFileTransferError(fileId, filePacket.appToken, "");
                }

                // File receive success so we can remove the current file receiving id from map
                mMeshFileManager.removeFileReceiverMap(mFileHelper.getFileMessageId(filePacket.mSourceAddress,filePacket.mFileId));

                mMeshFileManager.onFileProcessError(true, filePacket.mSourceAddress);
            }
        }
    }

    @Override
    public void onFileReceiveStarted(FilePacket filePacket) {
        if (filePacket != null) {
            mDatabaseService.updateFilePacket(filePacket);
            //No listeners is forwarded from here as upon receiving FileMessage MessageProcessor manages
            //this callback
        }
    }
}
