package com.letbyte.core.meshfilesharing.comm;

import android.text.TextUtils;

import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.helper.FileHelper;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-18 at 12:49 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-18 at 12:49 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-18 at 12:49 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public abstract class FileComm extends Thread {

    private final int RESERVE_PORT_TILL = 1024;
    protected FilePacket mFilePacket;
    protected int mPort;
    protected FileHelper mFileHelper;

    protected FileComm(FilePacket filePacket, int port, FileHelper fileHelper) {

        if(TextUtils.isEmpty(filePacket.mSelfFullFilePath) || port <= RESERVE_PORT_TILL) {
            throw new IllegalArgumentException("File path should be valid and ports can not be less " +
                    "than "+RESERVE_PORT_TILL);
        }

        mFilePacket = filePacket;
        mPort = port;
        mFileHelper = fileHelper;

        setDaemon(true);
        setName(getClass().getSimpleName()+":"+mPort);
    }
}
