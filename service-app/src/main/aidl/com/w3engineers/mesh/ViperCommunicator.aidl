package com.w3engineers.mesh;
import com.w3engineers.models.UserInfo;
import com.w3engineers.models.PendingContentInfo;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.models.FileData;

/**
 This interface will be implementaed in client service,
 remote service will call this interface to send data to client
*/

interface ViperCommunicator {

    void onStartTeleMeshService(in boolean isSuccess,in String nodeId, in String mseeage);
    void onTeleServiceStarted(in boolean isSuccess, in String nodeId, in String pubKey, in String message);

    void onPeerAdd(in String peerId);
    void onPeerRemoved(in String nodeId);
    void onRemotePeerAdd (in String peerId);
    void onUserInfoReceive(in List<UserInfo> userInfoList);
    void destroyFullService();

    void onDataReceived(in String senderId, in byte[] frameData);
    void onAckReceived(in String messageId, in int status);

    void setServiceForeground(boolean isForeGround);
    void onServiceUpdateNeeded(in boolean isNeeded);

    void onInterruption(in int hardwareState, in List<String> permissions);

    void receiveOtherAppVersion(in FileData fileData);
    void onAppUpdateRequest(in String receiverId);

    void onFileProgress(in String fileTransferId, in int percentProgress);
    void onFileTransferFinish(in String fileTransferId);
    void onFileTransferError(in String fileTransferId, in String errorMessage);
    void onFileReceiveStarted(in FileData fileData);
    void onPendingFileReceive(in PendingContentInfo pendingContentInfo);

    void receiveBroadcast(in BroadcastData broadcastData);
}
