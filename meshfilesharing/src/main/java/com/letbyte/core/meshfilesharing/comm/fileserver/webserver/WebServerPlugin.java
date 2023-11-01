package com.letbyte.core.meshfilesharing.comm.fileserver.webserver;

import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.IHTTPSession;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response;

import java.io.File;
import java.util.Map;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-19 at 6:59 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-19 at 6:59 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-19 at 6:59 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public interface WebServerPlugin {

    boolean canServeUri(String uri, File rootDir);

    void initialize(Map<String, String> commandLineOptions);

    Response serveFile(String uri, Map<String, String> headers, IHTTPSession session, File file, String mimeType);
}