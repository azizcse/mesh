package com.w3engineers.meshrnd.test.filesharing;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-04 at 11:53 AM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-04 at 11:53 AM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-04 at 11:53 AM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/

@VisibleForTesting
public class FileReadWriteTest {

    public void testFileRW(Context context, String filePath) {
        //Test random file R/W operations
        new Thread(() -> {

            //It will be now done by HTTPFileClient

        }).start();
    }

}
