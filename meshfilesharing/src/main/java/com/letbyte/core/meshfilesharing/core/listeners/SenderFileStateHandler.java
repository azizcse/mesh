package com.letbyte.core.meshfilesharing.core.listeners;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.core.BuyerFileManager;
import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FileAckMessage;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.MessageProcessor;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class SenderFileStateHandler extends AbstractFileStateHandler {

    private Set<Long> mFileServerSet;

    public SenderFileStateHandler(Set<Long> fileServerSet,
                                  DatabaseService databaseService, FileHelper fileHelper,
                                  MeshFileManager meshFileManager,
                                  MeshFileEventListener meshFileEventListener) {
        super(databaseService, fileHelper, meshFileManager, meshFileEventListener);
        mFileServerSet = fileServerSet;
    }

    @Override
    public void onFilePercentProgress(FilePacket filePacket, int percentProgress) {
        MeshLog.v("P2P_FILE_RESUME sender progress");
        if (filePacket != null) {
            Timber.d("File_progress  sender: %s", filePacket.toString());

            String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                    filePacket.mFileId);
            if (filePacket instanceof BroadcastFilePacket) {
                // sending progress for broadcastFile
            } else {
                mMeshFileEventListener.onFileProgress(fileId, percentProgress,
                        filePacket.appToken);
            }



            /*//If me is the source then propagate progress event otherwise only update db in above super
            //call
            RoutingEntity routingEntity = RouteManager.getInstance().
                    getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);
            //Next hop is the destination
            if (routingEntity != null && routingEntity.getAddress().equals(filePacket.mPeerAddress)) {
                //Me is the source
                if (mMyAddress.equals(filePacket.mSourceAddress) && mMeshFileEventListener != null) {

                    String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                            filePacket.mFileId);
                    if (filePacket instanceof BroadcastFilePacket) {
                        // sending progress for broadcastFile
                    } else {
                        mMeshFileEventListener.onFileProgress(fileId, percentProgress,
                                filePacket.appToken);
                    }

                } else {
                    //Me is not the source, So propagate the file progress info to earlier node

                    TransportManagerX transportManagerX = TransportManagerX.getInstance();
                    if (transportManagerX != null) {
                        FileAckMessage fileAckMessage = new FileAckMessage(filePacket.mFileId);
                        fileAckMessage.mTransferredBytes = filePacket.mTransferredBytes;
                        fileAckMessage.mPeerAddress = filePacket.mSourceAddress;

                        transportManagerX.sendFileMessage(filePacket.mSourceAddress,
                                fileAckMessage.toJson().getBytes());
                    }
                }
            }*/
        } else {
            MeshLog.v("P2P_FILE_RESUME packet null");
        }
    }

    @Override
    public void onFileTransferFinish(FilePacket filePacket) {
        MeshLog.v("P2P_FILE_RESUME sender finish");
        if (filePacket != null) {
            boolean hasRemoved = mFileServerSet.remove(filePacket.mFileId);
            Timber.d("File sent finish:%s, %s", hasRemoved, filePacket.toString());
            super.onFileTransferFinish(filePacket);

            if (hasRemoved) {

                RoutingEntity routingEntity = RouteManager.getInstance().
                        getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);

                //To process next file in BT Queue
                if (RoutingEntity.isBtNode(routingEntity) && mMeshFileManager != null) {

                    mMeshFileManager.onBTFileSentFinish(filePacket);
                }

                if (mMyAddress.equals(filePacket.mSourceAddress)) {

                    //Whether next node is the destination node or not
                    if (mMeshFileEventListener != null && routingEntity != null &&
                            filePacket.mPeerAddress.equals(routingEntity.getAddress())) {


                        mMeshFileManager.onProcessNextFileSend(false, filePacket.mPeerAddress, true);

                        String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                                filePacket.mFileId);
                        if (filePacket instanceof BroadcastFilePacket) {

                            mMeshFileEventListener.onSenderBroadcastFileTransferFinish(((BroadcastFilePacket) filePacket).mBroadcastId,
                                    filePacket.mPeerAddress, filePacket.mSelfFullFilePath);
                        } else {
                            mMeshFileEventListener.onFileTransferFinish(fileId, filePacket.appToken);
                        }

                    }

                } else {
                    //Delete the file as forwarding done
                    List<FilePacket> packetList = new ArrayList<>();
                    packetList.add(filePacket);

                    mFileHelper.deleteFile(packetList);
                }
                /**
                 * We delete the DB reference iff next node is the destination, otherwise it is
                 * deleted from {@link MessageProcessor#processMessage(String, FileAckMessage)}
                 */
                if (routingEntity != null && routingEntity.getAddress().equals(
                        filePacket.mPeerAddress)) {
                    mDatabaseService.deletePacket(filePacket);
                }
            }
        }
    }

    @Override
    public void onFileTransferError(FilePacket filePacket, int errorCode) {
        MeshLog.v("P2P_FILE_RESUME sender error");
        if (filePacket != null) {
            super.onFileTransferError(filePacket, errorCode);

            if (mFileServerSet.remove(filePacket.mFileId)) {
                RoutingEntity routingEntity = RouteManager.getInstance().
                        getNextNodeEntityByReceiverAddress(filePacket.mPeerAddress);

                //To process next file in BT Queue
                if (RoutingEntity.isBtNode(routingEntity) && mMeshFileManager != null) {
                    mMeshFileManager.onBTFileSentFinish(filePacket);
                }

                if (mMyAddress.equals(filePacket.mSourceAddress)) {

                    MeshLog.e("File send error: "+ AddressUtil.makeShortAddress(filePacket.mPeerAddress));
                    mDatabaseService.updateFilePacket(filePacket);

                    if (mMeshFileEventListener != null) {
                        String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress, filePacket.mFileId);
                        if (filePacket instanceof BroadcastFilePacket) {
                            mMeshFileEventListener.onBroadcastFileTransferError(((BroadcastFilePacket) filePacket).mBroadcastId,
                                    fileId, filePacket.appToken, filePacket.mPeerAddress);
                        } else {




                            String errorMessage = "File sending failed";
                            if (errorCode == MeshFile.FILE_BALANCE_EXCEED){
                                errorMessage = "Receiver does not have enough internet to receive the file";
                            } else if (errorCode == MeshFile.BUYER_FILE_SIZE_EXCEED) {
                                errorMessage = Constant.ErrorMessages.EXCEED_FILE_SIZE_1;
                            } else if (errorCode == MeshFile.MULTI_HOP_FILE_SIZE_EXCEED) {
                                errorMessage = Constant.ErrorMessages.EXCEED_FILE_SIZE_10;
                            }

                            mMeshFileEventListener.onFileTransferError(fileId, filePacket.appToken, errorMessage);
                        }
                    }

                    mMeshFileManager.onFileProcessError(false,filePacket.mPeerAddress);
                }
            }
        }
    }

    public void onFileReadError(FilePacket filePacket) {
        if (filePacket != null) {
            if (!isMeSource(filePacket)) {
                if (mMeshFileManager != null && mMeshFileManager.mForwardingBytesCountMap != null) {
                    String fileId = mFileHelper.getFileMessageId(filePacket.mSourceAddress,
                            filePacket.mFileId);
                    mMeshFileManager.mForwardingBytesCountMap.put(fileId,
                            filePacket.mTransferredBytes);
                }
            }
        }
    }

    private boolean isMeSource(FilePacket filePacket) {
        return filePacket != null && filePacket.mSourceAddress.equals(mMyAddress);
    }
}
