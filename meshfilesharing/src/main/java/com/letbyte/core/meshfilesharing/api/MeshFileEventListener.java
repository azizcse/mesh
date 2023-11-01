package com.letbyte.core.meshfilesharing.api;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 1:13 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 1:13 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 1:13 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public interface MeshFileEventListener {

    void onFileProgress(String fileTransferId, int percentProgress, String appToken);

    void onFileTransferFinish(String fileTransferId, String appToken);

    void onFileTransferError(String fileTransferId, String appToken, String errorMessage);

    void onFileReceiveStarted(String sourceAddress, String fileTransferId, String filePath, byte[] msgMetaData, String appToken);

    default void onBroadcastFileTransferFinish(String broadcastId, String broadcastText, String contentPath, String senderId, String appToken) {
    }

    default void onSenderBroadcastFileTransferFinish(String broadcastId, String userId, String contentPath) {
    }

    default void onBroadcastFileTransferError(String broadcastId, String fileTransferId, String appToken, String userId) {
    }


}
