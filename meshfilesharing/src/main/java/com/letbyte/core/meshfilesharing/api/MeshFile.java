package com.letbyte.core.meshfilesharing.api;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-27 at 1:37 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-27 at 1:37 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-27 at 1:37 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public interface MeshFile {

    int FAILED = 1;
    int FAILED_COULD_NOT_WRITE = 2;
    int FAILED_COULD_NOT_READ = 3;
    int FAILED_COULD_NOT_FOUND = 4;
    int FAILED_LINK_CLOSED = 5;
    int UNKNOWN_FILE_ID = -1;
    int FILED_BALANCE_EXCEED = 6; //error as receiver does not have enough balance
    int MULTI_HOP_FILE_SIZE_EXCEED = 7;
    int BUYER_FILE_SIZE_EXCEED = 8;
    int FILE_BALANCE_EXCEED = 9;

}
