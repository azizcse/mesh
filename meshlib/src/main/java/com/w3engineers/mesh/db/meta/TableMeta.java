package com.w3engineers.mesh.db.meta;

/**
 * Meta data for Table
 */
public class TableMeta {

    public static final String DB_NAME = "mesh.db";

    public interface TableNames {
        String ROUTING = "routing";
        String USERS = "users";
        String PEERS = "peers";
        String NODE_INFO = "nodeinfo";
        String ROUTING_PATH = "routingpath";
    }

    public interface ColumnNames {
        String ROW_ID = "id";
        String ADDRESS = "address";
        String MAC = "mac";
        String TYPE = "type";
        String NEXT_HOP = "hop";
        String HOP_COUNT = "hop_count";
        String IP = "ip";
        String IS_ONLINE = "is_online";
        String TIME = "time";
        String NETWORK_NAME = "net_name";
        String CONFIG_VERSION = "config_version";
        String NETWORK_GO_ID = "go_id";

        String AVATAR = "avatar";
        String USER_NAME = "user_name";
        String REG_TIME = "reg_time";
        String IS_SYNCED = "is_sync";
        String PUBLIC_KEY = "public_key";
        String PACKAGE_NAME = "package_name";
        String IS_ME = "is_me";
        String IS_NOTIFIED = "is_notified";
        String TIME_CREATED = "time_created";
        String TIME_MODIFIED = "time_modified";

        String WRITE_TIME = "write_time";
        String USER_HASH = "user_hash";

        String APP_TOKEN = "app_token";
        String APP_USER_INFO = "app_user_info";

        String USER_APP_VERSION = "user_app_version";
        String USER_LATITUDE = "latitude";
        String USER_LONGITUDE = "longitude";
//        String MESH_CONTROL_CONFIG = "mesh_control_config";

        //Node Info table column
        String COL_USER_ID = "user_id";
        String COL_HOP_ID = "hop_id";
        String COL_IP_ADDRESS= "ip_address";
        String COL_PUBLIC_KEY = "public_key";
        String COL_USER_MODE = "user_mode";
        String COL_USER_TYPE = "user_type";
        String COL_SSID_NAME = "ssid_name";
        String COL_BT_NAME = "bt_name";
        String COL_CREATED_TIME = "created_time";
    }

}
