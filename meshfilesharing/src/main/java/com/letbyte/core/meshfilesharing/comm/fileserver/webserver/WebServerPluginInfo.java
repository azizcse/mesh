package com.letbyte.core.meshfilesharing.comm.fileserver.webserver;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-19 at 7:00 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-19 at 7:00 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-19 at 7:00 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public interface WebServerPluginInfo {

    String[] getIndexFilesForMimeType(String mime);

    String[] getMimeTypes();

    WebServerPlugin getWebServerPlugin(String mimeType);
}