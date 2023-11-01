package com.w3engineers.mesh.libmeshx.discovery;

/**
 * p2p log listener
 */
public interface MeshXLogListener {


    /**
     * Log Message from underlying layers
     * @param logMessage
     */
    void onLog(String logMessage);
}
