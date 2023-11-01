package com.w3engineers.mesh.model

import com.google.gson.annotations.SerializedName
import com.w3engineers.mesh.util.JsonDataBuilder

data class HandshakeInfo(
        @SerializedName(JsonDataBuilder.KEY_SENDER_ID) var senderId : String,
        @SerializedName(JsonDataBuilder.KEY_RECEIVER_ID) var receiverId : String,
        @SerializedName(JsonDataBuilder.KEY_TARGET_ADDRESS) var targetReceiver : String,
        // Type can be ping, broadcast_sync, info_data, app_update_request
        @SerializedName(JsonDataBuilder.KEY_MESSAGE_TYPE) var type : Int,
        @SerializedName(JsonDataBuilder.KEY_APP_TOKEN) var appToken : String?,

        // For user profile data
        @SerializedName(JsonDataBuilder.KEY_USER_PROFILE_DATA) var profileData : String?,

        // For broadcast ids sync
        @SerializedName(JsonDataBuilder.KEY_BROADCAST_IDS) var broadcastIds : List<String>?,
        @SerializedName(JsonDataBuilder.KEY_SYNC_BROADCAST_IDS) var syncBroadcastIds : List<String>?,
        @SerializedName(JsonDataBuilder.KEY_REQUEST_BROADCAST_IDS) var requestBroadcastIds : List<String>?,

        // For syncing latitude and longitude
        @SerializedName(JsonDataBuilder.KEY_CONTENT_LATITUDE) var latitude : Double?,
        @SerializedName(JsonDataBuilder.KEY_CONTENT_LONGITUDE) var longitude : Double?,
        // For user apps version data and TeleService Data
        @SerializedName(JsonDataBuilder.KEY_APP_DATA) var appData : List<AppVersion>?,
        // For client app info data
        @SerializedName(JsonDataBuilder.KEY_APP_CONFIG_VERSION) var clientAppConfigVersion : Int?,
        @SerializedName(JsonDataBuilder.KEY_APP_CONFIG) var clientAppConfig : String?,
        // For credentials data
        @SerializedName(JsonDataBuilder.KEY_CREDENTIALS) var credentialsConfig : String?
) {
    constructor(senderId: String, receiverId: String, targetReceiver: String, type: Int) : this(senderId, receiverId, targetReceiver, type, null,
            null, null, null, null, null, null,
            null, null, null, null)
}