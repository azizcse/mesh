package com.w3engineers.mesh.httpservices;

public interface INanoServerInitiator {
    NanoHTTPServer generateNanoServer(int port);
    void stopFileServer();
}
