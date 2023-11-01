package com.w3engineers.mesh.util;

import com.google.gson.Gson;
import com.w3engineers.models.MeshControlConfig;
import com.w3engineers.models.UserInfo;

public class ConfigJsonBuilder {
    private static ConfigJsonBuilder jsonBuilder;
    private Gson gson;

    private ConfigJsonBuilder() {
        gson = new Gson();
    }

    public static ConfigJsonBuilder on() {
        if (jsonBuilder == null) {
            jsonBuilder = new ConfigJsonBuilder();
        }
        return jsonBuilder;
    }

    public String toJson(MeshControlConfig meshControlConfig){
        return gson.toJson(meshControlConfig, MeshControlConfig.class);
    }

    public MeshControlConfig formJson(String jsonString){
        return gson.fromJson(jsonString, MeshControlConfig.class);
    }

}
