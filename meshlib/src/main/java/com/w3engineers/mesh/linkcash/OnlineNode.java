package com.w3engineers.mesh.linkcash;

/**
 * Created by Azizul Islam on 11/5/20.
 */
public class OnlineNode {
    public String nodeId;
    public int type;
    public String ip;

    public OnlineNode(String nodeId, int type, String ip) {
        this.nodeId = nodeId;
        this.type = type;
        this.ip = ip;
    }
}
