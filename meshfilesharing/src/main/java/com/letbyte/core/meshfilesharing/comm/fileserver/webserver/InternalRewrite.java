package com.letbyte.core.meshfilesharing.comm.fileserver.webserver;

import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.NanoHTTPD;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-03-19 at 6:58 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-03-19 at 6:58 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-03-19 at 6:58 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class InternalRewrite extends Response {

    private final String uri;

    private final Map<String, String> headers;

    public InternalRewrite(Map<String, String> headers, String uri) {
        super(Status.OK, NanoHTTPD.MIME_HTML, new ByteArrayInputStream(new byte[0]), 0);
        this.headers = headers;
        this.uri = uri;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public String getUri() {
        return this.uri;
    }

    @Override
    protected void sendBody(OutputStream outputStream, long pending) throws IOException {
        super.sendBody(outputStream, pending);
    }
}