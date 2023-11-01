package com.w3engineers.purchase.db;

public interface TableInfo {

    String TABLE_NAME = "AppUpdateInfo";
    String CLIENT_INFO = "ClientInfo";
    String TABLE_BROADCAST = "Broadcast";
    String TABLE_BROADCAST_TRACK = "Broadcast_Track";
    String TABLE_HANDSHAKE_TRACK = "Handshake_Track";
    String DB_NAME = "appupdateapp.db";

    interface Column {
        String ID = "id";
        String PACKAGE_NAME = "package_name";
        String APP_SIZE = "app_size";
        String MY_USER_ID = "my_user_id";
        String RECEIVER_ID = "receiver_id";
        String APP_NAME = "app_name";
        String SELF_APP_VERSION_CODE = "self_app_version_code";
        String SELF_APP_VERSION_NAME = "self_app_version_name";
        String RECEIVER_APP_VERSION_CODE = "receiver_app_version_code";
        String RECEIVER_APP_VERSION_NAME = "receiver_app_version_name";
        String IS_CHECKING = "is_checking";
        String IS_SYNC = "is_sync";
        String TIME = "time";

        String BROADCAST_ID = "broadcast_id";
        String BROADCAST_USER_ID = "broadcast_user_id";
        String BROADCAST_NOTIFICATION_MODE = "broadcast_notification_mode";
        String BROADCAST_META_DATA = "broadcast_meta_data";
        String BROADCAST_CONTENT_PATH = "broadcast_content_paths";
        String BROADCAST_CONTENT_META_DATA = "content_meta_data";
        String APP_TOKEN = "app_token";
        String BROADCAST_IS_MINE = "is_mine";
        String BROADCAST_EXPIRE_TIME="broadcast_expire_time";

        String BROADCAST_LATITUDE="latitude";
        String BROADCAST_LONGITUDE="longitude";
        String BROADCAST_RANGE="broadcast_range";

        String BROADCAST_MESSAGE_ID = "broadcast_message_id";
        String BROADCAST_TRACK_USER_ID = "broadcast_track_user_id";
        String BROADCAST_RECEIVE_STATUS = "broadcast_message_status";
        String BROADCAST_SEND_STATUS = "broadcast_send_status";

        String HANDSHAKE_USER_ID = "handshake_user_id";
        String HANDSHAKING_TIME = "handshaking_time";
        String HANDSHAKING_DATA = "handshaking_data";

        // Client information
        String KEY_HASH = "key_hash";
        String SHA_KEY = "sha_key";
        String WALLET_CREATION_ENABLE = "wallet_creation_enable";
        String DISCOVERY_ENABLE = "discovery_enable";
        String MESSAGE_ENABLE = "message_enable";
        String BLOCK_CHAIN_ENABLE = "blokchain_enable";
        String APP_DOWNLOAD_ENABLE = "app_download_enable";
    }
}
