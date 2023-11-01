package com.w3engineers.helper;

import com.google.gson.Gson;
import com.w3engineers.models.ContentMetaInfo;
import com.w3engineers.purchase.db.content.Content;

public class ContentMetaGsonBuilder {

    private static ContentMetaGsonBuilder contentMetaGsonBuilder;
    private static Gson gson = new Gson();

    static {
        contentMetaGsonBuilder = new ContentMetaGsonBuilder();
    }

    public static ContentMetaGsonBuilder getInstance() {
        return contentMetaGsonBuilder;
    }

    public String prepareContentMetaJson(ContentMetaInfo contentMetaInfo) {
        return gson.toJson(contentMetaInfo);
    }

    public ContentMetaInfo prepareContentMetaObj(String contentMetaText) {
        return gson.fromJson(contentMetaText, ContentMetaInfo.class);
    }
}
