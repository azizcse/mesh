package com.w3engineers.mesh.meshlog.data.model;

/**
 * POJO for MeshLog
 */

public class MeshLogModel {
    private int type;
    private String log;

    public MeshLogModel(int type, String log) {
        this.type = type;
        this.log = log;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
