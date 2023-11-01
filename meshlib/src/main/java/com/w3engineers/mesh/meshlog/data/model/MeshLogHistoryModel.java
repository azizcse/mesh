package com.w3engineers.mesh.meshlog.data.model;

/**
 * POJO for MeshLog history
 */

public class MeshLogHistoryModel {
    private String name;
    private String path;

    public MeshLogHistoryModel(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
