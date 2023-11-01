package com.letbyte.core.meshfilesharing.core.listeners;

import com.letbyte.core.meshfilesharing.data.FilePacket;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-08-25 at 5:45 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: MESH.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-08-25 at 5:45 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-08-25 at 5:45 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public interface BroadcastFileStateListener {
    void onBroadcastFileTransferFinish(FilePacket filePacket);
}
