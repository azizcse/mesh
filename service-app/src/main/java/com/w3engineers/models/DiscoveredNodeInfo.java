package com.w3engineers.models;

public class DiscoveredNodeInfo {
    private String pubKey;
    private boolean isLocal;

    public String getPubKey() {
        return pubKey;
    }

    public DiscoveredNodeInfo setPubKey(String pubKey) {
        this.pubKey = pubKey;
        return this;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public DiscoveredNodeInfo setLocal(boolean local) {
        isLocal = local;
        return this;
    }
}
