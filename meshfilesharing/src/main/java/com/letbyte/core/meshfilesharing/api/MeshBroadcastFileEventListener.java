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
public interface MeshBroadcastFileEventListener {

    void onBroadcastFileProgress(String fileTransferId, int percentProgress, String appToken);

    void onBroadcastFileTransferFinish(String fileTransferId, String appToken);

    void onBroadcastFileTransferError(String fileTransferId, String appToken);

    void onBroadcastFileReceiveStarted(String sourceAddress, String fileTransferId, String filePath, byte[] msgMetaData, String appToken);

}
