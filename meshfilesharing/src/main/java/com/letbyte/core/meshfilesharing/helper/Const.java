package com.letbyte.core.meshfilesharing.helper;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-05 at 11:50 AM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-05 at 11:50 AM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-05 at 11:50 AM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class Const {

    public interface SharedPref {
        String DEFAULT_STORAGE = "DEFAULT_STORAGE";
    }

    public interface FileStatus {
        int INPROGRESS = 1;
        int FAILED = 2;
        int FINISH = 3;
    }

    public interface FileRequest {
        String RESUME_FROM = "resume_from";
    }

}
