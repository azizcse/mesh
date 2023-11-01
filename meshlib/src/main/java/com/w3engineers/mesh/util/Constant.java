package com.w3engineers.mesh.util;

import android.graphics.Color;
import android.os.Build;
import android.os.Environment;

import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.MeshApp;

import java.util.UUID;

/**
 * Maintains lib related constants
 */
public class Constant {

    public static String RANDOM_STATE = "mesh_random_state";
    public static String KEY_DEVICE_SSID_NAME = "mesh_ssid_name";
    public static String KEY_DEVICE_BLE_NAME = "mesh_device_ble";
    public static String KEY_USER_ID = "mesh_id";
    public static String KEY_USER_IP = "ip_address";
    public static String KEY_PUBLIC_KEY = "public_key";
    public static String KEY_APP_TOKEN = "ap_tkn";
    public static String KEY_CURRENT_LOG_FILE_NAME = "log_file";
    public static String KEY_RUNNING_ROLE = "current_role";
    public static String KEY_NEXT_ROLE = "next_role";
    public static String KEY_TEST_MODE = "test_mode";

    public static String KEY_USER_NAME = "mesh_name";
    public static String KEY_BLE_PREFIX = "mesh_ble_prefix";
    public static String RECENT_IMAGE_PATH = "mesh_recent_image_path";

    public static String CURRENT_LOG_FILE_NAME;


    public static String KEY_MULTIVERSE_URL = "mesh_multiverse_url";
    public static String KEY_APP_NAME = "app_name_multiverse";
    public static final String NAME_INSECURE = "mesh_BluetoothChatInsecure";

    // Unique UUID for this application
    public static final UUID MY_UUID_INSECURE = UUID
            .fromString(BuildConfig.BT_UUID);

    public static String MASTER_IP_ADDRESS = "192.168.49.1";

    public static String P2P_IP_ADDRESS_PREFIX = "192.168.49.";

    public static String KEY_DEVICE_AP_MODE = "mesh_device_ap_mode";

    public static String KEY_NETWORK_PREFIX = "mesh_network_prefix";

    public static String LATEST_UPDATE = "latest_update";
    public static long SELLER_MINIMUM_WARNING_DATA = 1024; // The format is byte
    public static String AUTO_IMAGE_CAPTURE = "auto_image_capture";
    public static String DIS_REQUEST = "disc_request";

    public static String VIPER_PACKAGE = "com.w3engineers.ext.viper";
    public static String TELEMESH_PACKAGE = "com.w3engineers.unicef.telemesh";

    public static String NETWORK_SSID = "NETWORK_SSID";
    public static String MY_INFO_PREF_KEY = "MY_INFO_PREF_KEY";

    public static String ID_PREF_ACK = "ack";
    public static String ID_SPLITTER = "_";
    public static String FILE_CONNECTION = "file_";

    public interface MessageStatus {
        int SENDING = 0;
        int RECEIVING = 5;
        int SEND = 1;
        int DELIVERED = 2;
        int RECEIVED = 3;
        int FAILED = 4;
    }

    public interface DataType {
        int USER_LIST = 1;
        int USER_MESSAGE = 2;
        int ACK_MESSAGE = 3;
        int DIRECT_USER = 4;
        int LEAVE_MESSAGE = 5;
        int USER_INFO_MESSAGE = 6;


        // For version message
        int VERSION_HANDSHAKING = 7;
        int APP_UPDATE_REQUEST = 8;

        // for webrtc reconnect
        int REQUEST_NODE_INFO = 9;

        // for hmac message
        int HMAC_REQUEST = 10;
        int HMAC_RESPONSE = 11;

        // client information sync
        int CLIENT_INFO_VERSION_HANDSHAKING = 12;
        int CLIENT_INFO_DATA = 13;
    }

    public interface JsonKeys {
        String TYPE = "type";
        String MESSAGE = "m";
        String HEADER = "h";
        String SENDER = "s";
        String RECEIVER = "r";

        String STATUS = "status";
        String MSG_ID = "msgId";
        String USER_ID = "user_id";
        String HOP_ID = "hop_id";

        String ROOM = "room";
        String ETH_ID = "user_eth_id";
        String USER_NAME = "user_name";
        String TO_ETH_ID = "to_eth_id";
        String SELF_ETH_ID = "self_eth_id";
        String NODE_INFO = "node_info";
        String SDP = "sdp";
        String CANDIDATE = "candidate";
        String LABEL = "label";
        String ID = "id";
        String USER_INFO = "user_info";
    }

    public interface SDPEvent {
        String ANSWER = "answer";
        String OFFER = "offer";
        String CANDIDATE = "candidate";
    }


    public interface PreferenceKeys {
        String TOTAL_MESSAGE_SENT = "total_msg_sent";
        String TOTAL_MESSAGE_RCV = "total_msg_rcv";
        String IS_SETTINGS_PERMISSION_DONE = "is_xiaomi_permission_done";
        String SERVICE_UPDATE_CHECK_TIME = "service_update_check_time";
        String STORED_LATITUDE = "stored_latitude";
        String STORED_LONGITUDE = "stored_longitude";
    }

    public interface DiagramColor {
        int[] colors = new int[]{Color.DKGRAY,
                Color.rgb(0, 102, 0),
                Color.rgb(0, 0, 204),
                Color.rgb(0, 204, 0),
                Color.rgb(100, 100, 255),
                Color.rgb(77, 0, 64),
                Color.rgb(179, 0, 149),
                Color.rgb(230, 0, 0),
                Color.LTGRAY};
        String[] colorDeffs = new String[]{"Disconnected",
                "WiFi Direct",
                "BLE Direct",
                "WiFi Mesh",
                "BLE Mesh",
                "Internet Direct",
                "Internet Mesh",
                "Connected Link",
                "Disconnected Link"
        };
    }

    public static class Directory {

        private String PARENT_DIRECTORY = Environment.getExternalStorageDirectory().toString() + "/Telemesh/";
        String NETWORK_IMAGES = "/NetworkImages/";
        public static String MESH_LOG = "/MeshLog/";
        public static String MESH_SDK_CRUSH = "/MeshSDKCrash/";
        String MESH_ID = "/MeshId/";
        String FILE_NO_MEDIA = "/.nomedia/";

        int MAXIMUM_IMAGE = 50;

        public String getParentDirectory() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return MeshApp.getContext().getExternalFilesDir("") + "/Telemesh/";
            }
            return PARENT_DIRECTORY;
        }



    }

    public interface DiagramId {
        int MAIN_LAYOUT_ID = 123450;
        int CENTER_LAYOUT_ID = 123451;
        int MY_NODE_ID = 123452;
        int SHOW_SCREENSHOT_ID = 123453;
        int DOT_BUTTON_ID = 123454;
        int DELETE_BUTTON_ID = 123455;
        int LINE_DRAW_ID = 123456;
        int INNER_LAYOUT_ID = 123457;

        int BOTTOM_NAVIGATION_HEIGHT = 280;
    }

    public interface Cycle {
        /**
         * To remove from temporary exception list and remove node from database if not updated with
         * new path
         */
        int DISCONNECTION_MESSAGE_TIMEOUT = 10 * 1000;

        int MESH_USER_DELAY_THRESHOLD = 3 * 1000;
    }


    public static long MESSAGE_ACK_SIZE = 180;
    public static long FILE_MESSAGE_ACK_SIZE = 360;
    public static long BUYER_MAX_FILE_SIZE = 1024 * 1024;
    public static long MULTIHOP_MAX_FILE_SIZE = 1024 * 1024 * 10;
    public static long HANDSHAKE_TIME_DIFFERENCE = 2 * 60 * 60 * 1000;
    public static long FILE_METADATA_MAX_SIZE = 10 * 1024;

    public interface ErrorMessages {
        String EXCEED_FILE_SIZE_1 = "File size can't be greater than 1 MB";
        String EXCEED_FILE_SIZE_10 = "File size can't be greater than 10 MB";
        String EXCEED_FILE_META_SIZE = "Metadata size can't be greater than 10 KB";
        String EXCEED_BALANCE = "You do not have enough balance to send this file";
        String NO_USER = "Destination user is not reachable.";
    }

    public interface FileMessageType {
        int FILE_INFO_MESSAGE = 1;
        int FILE_PACKET_MESSAGE = 2;
        int FILE_ACK_MESSAGE = 3;
    }


}
