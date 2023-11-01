package com.w3engineers.mesh.util;

import com.google.gson.Gson;
import com.w3engineers.models.UserInfo;

public class JsonBuilder {
    private static JsonBuilder jsonBuilder;
    private Gson gson;

    private JsonBuilder() {
        gson = new Gson();
    }

    public static JsonBuilder on() {
        if (jsonBuilder == null) {
            jsonBuilder = new JsonBuilder();
        }
        return jsonBuilder;
    }

    public String toJson(UserInfo userInfo){

        return gson.toJson(userInfo, UserInfo.class);
    }

    public UserInfo formJson(String jsonString){

        return gson.fromJson(jsonString, UserInfo.class);
    }

}
