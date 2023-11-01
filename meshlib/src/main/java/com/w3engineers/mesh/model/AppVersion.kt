package com.w3engineers.mesh.model

import com.google.gson.annotations.SerializedName
import com.w3engineers.mesh.util.JsonDataBuilder

data class AppVersion(
        @SerializedName(JsonDataBuilder.KEY_VERSION_CODE) var versionCode : Int,
        @SerializedName(JsonDataBuilder.KEY_VERSION_NAME) var versionName : String,
        @SerializedName(JsonDataBuilder.KEY_APP_SIZE) var appSize : String,
        @SerializedName(JsonDataBuilder.KEY_APP_TOKEN) var appToken : String
)