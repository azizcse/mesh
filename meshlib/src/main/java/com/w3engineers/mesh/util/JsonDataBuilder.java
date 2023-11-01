package com.w3engineers.mesh.util;

import android.text.TextUtils;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.linkcash.ConnectionLinkCache;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.wifi.WifiTransPort;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides JSON data builder Utility
 */
public class JsonDataBuilder {

    public static final String KEY_MESSAGE_TYPE = "mlk_t";
    public static final String KEY_SENDER_INFO = "mlk_s";
    public static final String KEY_BT_USERS = "bu";
    public static final String KEY_BT_MESH_USER = "bmu";
    public static final String KEY_WIFI_USERS = "wu";
    public static final String KEY_WIFI_MESH_USER = "wmu";
    public static final String KEY_TIME_STAMP = "tm";
    public static final String KEY_HOP_NODE_ID = "hn";
    public static final String KEY_MESH_USERS = "mu";
    public static final String KEY_MESH_USERS_ONLINE = "muo";
    public static final String KEY_ACK_BODY = "ab";
    public static final String KEY_ACK_STATUS = "as";
    public static final String KEY_SELLER_ID = "sid";
    public static final String KEY_INTERNET_USERS = "iu";
    public static final String KEY_CONNECTED_BUYERS = "cb";

    public static final String KEY_SENDER_ID = "sid";
    public static final String KEY_SENDER_IP = "sip";
    public static final String KEY_RECEIVER_ID = "rid";
    public static final String KEY_MESSAGE_ID = "mid";
    public static final String KEY_MESSAGE = "m";
    public static final String KEY_USER_LIST = "ul";
    public static final String KEY_LEAVE_NODE_ID = "lni";
    public static final String KEY_TARGET_ADDRESS = "tid";
    public static final String KEY_VERSION_CODE = "vc";
    public static final String KEY_VERSION_NAME = "vn";
    public static final String KEY_APP_SIZE = "asize";
    public static final String KEY_APP_TOKEN = "at";
    public static final String KEY_VERSION_TYPE = "vt";
    public static final String KEY_HS_SSID = "hsssid";
    public static final String KEY_HS_PWD = "hpwd";
    public static final String KEY_NEW_ROLE = "_role";
    public static final String KEY_HMAC_MSG_TYPE = "ht";
    public static final String KEY_HMAC_REQUEST = "hrq";
    public static final String KEY_HMAC_RESPONSE = "hrs";
    public static final String KEY_BROADCAST_ID = "_bi";
    public static final String KEY_META_DATA = "__md";
    public static final String KEY_CONTENT_PATH = "_cp";
    public static final String KEY_CONTENT_EXPIRE_TIME = "_cet";
    public static final String KEY_CONTENT_LATITUDE = "_la";
    public static final String KEY_CONTENT_LONGITUDE = "_lo";
    public static final String KEY_BROADCAST_RANGE = "_br";
    public static final String KEY_BROADCAST_ADDRESS = "bla";
    public static final String KEY_BROADCAST_TEXT = "brt";
    public static final String KEY_ACK_SENDER = "acks";
    public static final String KEY_MESSAGE_DESTINATION = "md";
    public static final String KEY_MESSAGE_SOURCE = "ms";

    //Mesh v2
    public static final String KEY_NODE_IDS = "nis";
    public static final String KEY_NODE_INFOS = "nif";
    public static final String KEY_ONLINE_NODE_IDS = "oni";
    public static final String KEY_REQUESTED_IDS = "rid";

    public static final String KEY_ONLINE_DIRECT_NODES = "on";
    public static final String KEY_ONLINE_MESH_NODES = "omn";
    public static final String KEY_OFFLINE_NODES = "of";
    public static final String KEY_SOFTAP_SSID = "ssid";
    public static final String KEY_SOFTAP_PASSWORD = "spas";
    public static final String KEY_DATA_PREPARE_ID = "dpi";


    public static final String ETH_ID = "eth";
    public static final String MY_NODE_COUNT = "mn";
    public static final String MY_CURRENT_MODE = "mcm";
    public static final String FORCE_CONNECTION = "fc";
    public static final String IS_SERVER = "is";
    public static final String GO_ADDRESS_HASH = "gh";
    public static final String IS_FORCE_REQUEST = "ifr";
    public static final String IS_ABLE_TO_RECEIVE = "iar";
    public static final String KEY_GO_ID = "kgi";
    public static final String KEY_DUPLICATE_ID = "di";

    // To puss locally
    public static final String KEY_BUDDY_ID = "buddy_id";
    public static final String KEY_IMMEDIATE_SENDER = "imdtsndr";

    public static final String KEY_BUYERS_DETAILS_INFO = "bdi";

    public static final String KEY_BUYER_FILE_ACK = "ack";

    public static final String KEY_USER_PROFILE_DATA = "pd";
    public static final String KEY_APP_DATA = "ad";
    public static final String KEY_APP_CONFIG_VERSION = "acv";
    public static final String KEY_APP_CONFIG = "ac";
    public static final String KEY_CREDENTIALS = "cr";
    public static final String KEY_BROADCAST_IDS = "bi";
    public static final String KEY_SYNC_BROADCAST_IDS = "sb";
    public static final String KEY_REQUEST_BROADCAST_IDS = "rb";

    public static final int BT_HELLO = 1;
    public static final int P2P_HELLO_CLIENT = 2;
    public static final int P2P_HELLO_MASTER = 3;
    public static final int ADHOC_HELLO_CLIENT = 4;
    public static final int ADHOC_HELLO_MASTER = 5;
    public static final int MESH_USER = 6;
    public static final int INTERNET_USER = 7;
    public static final int APP_MESSAGE = 8;
    public static final int ACK_MESSAGE = 9;
    public static final int PAYMENT_MESSAGE = 10;
    public static final int PAYMENT_ACK = 11;
    public static final int INFO_REQUEST = 12;
    public static final int INTERNET_USER_LEAVE = 13;
    public static final int NODE_LEAVE = 14;
    public static final int DISCONNECT_REQUEST = 15;
    public static final int ROUTE_INFO = 16;
    public static final int HEART_BEAT = 17;
    public static final int VERSION_MESSAGE = 18;
    public static final int MESSAGE_TYPE_HIGH_BAND = 19;
    public static final int MESSAGE_TYPE_DRY_HIGH_BAND = 20;
    public static final int MESSAGE_TYPE_HIGH_BAND_CONFIRMATION = 21;
    public static final int USER_ROLE_SWITCH = 22;
    public static final int APP_FILE_MESSAGE = 23;
    public static final int APP_BROADCAST_MESSAGE = 24;
    public static final int APP_BROADCAST_ACK_MESSAGE = 25;
    public static final int HMAC_MESSAGE = 26;
    public static final int BUYER_FILE_MESSAGE = 27;

    //Mesh v2 key
    public static final int HELLO_MASTER = 30;
    public static final int HELLO_CLIENT = 31;
    public static final int NODE_RESPONSE = 32;
    public static final int MESH_NODE = 33;
    public static final int CREDENTIAL_MESSAGE = 34;
    public static final int BLE_MESH_DECISION_MESSAGE = 35;
    public static final int FORCE_CONNECTION_MESSAGE = 36;
    public static final int FILE_RECEIVE_FREE_MESSAGE = 37;
    public static final int GO_NETWORK_FULL = 38;
    public static final int SPECIAL_DISCONNECT_MESSAGE = 39;
    public static final int FAILED_MESSAGE_ACK = 40;
    //Internet
    public static final int REMOTE_USER_CONNECTED = 50;
    public static final int REMOTE_USER_DISCONNECTED = 51;
    public static final int REMOTE_USERS_CONNECTED = 52;
    public static final int REMOTE_USERS_DETAILS_REQUEST = 53;
    public static final int REMOTE_USERS_DETAILS_RESPONSE = 54;
    public static final int BUYER_LIST = 55;
    public static final int BUYER_LEAVE = 56;

    public static final int INFO_HANDSHAKE = 41;
    public static final int HANDSHAKE_PING = 42;
    public static final int HANDSHAKE_BROADCAST = 43;
    public static final int HANDSHAKE_INFO = 44;
    public static final int HANDSHAKE_CONFIG = 45;
    public static final int BROADCAST_FILE_MESSAGE = 46;

    public static byte[] buildMeshMessage(String senderId, String hopNodeId, String userJson) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_HOP_NODE_ID, hopNodeId);
            jo.put(KEY_SENDER_ID, senderId);
            jo.put(KEY_MESH_USERS, userJson);

            jo.put(KEY_MESSAGE_TYPE, MESH_USER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }


    public static byte[] builFiledMessage(String sender, String receiver, byte[] data) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE, new String(data));

            jo.put(KEY_MESSAGE_TYPE, APP_FILE_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] buildFiledMessage(String sender, String receiver, String msgId, byte[] data) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE_ID, msgId);
            jo.put(KEY_MESSAGE, new String(data));

            jo.put(KEY_MESSAGE_TYPE, APP_FILE_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] buildBroadcastFileMessage(String senderId, String receiverId, byte[] data) {
        JSONObject jo = new JSONObject();

        try {
            jo.put(KEY_SENDER_ID, senderId);
            jo.put(KEY_RECEIVER_ID, receiverId);
            jo.put(KEY_MESSAGE, new String(data));

            jo.put(KEY_MESSAGE_TYPE, BROADCAST_FILE_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] buildBuyerFileMessage(String sender, String receiver, byte[] data, int fileMessageType, String messageId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE, new String(data));
            jo.put(KEY_BUYER_FILE_ACK, fileMessageType);
            jo.put(KEY_MESSAGE_ID, messageId);
            jo.put(KEY_MESSAGE_TYPE, BUYER_FILE_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] buildBroadcastData(String sender, String receiver, String actualReceiver, String broadcastId,
                                            String textData, String contentPath, long expireTime, String appToken, byte[] data,
                                            double latitude, double longitude, double range) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_BROADCAST_ID, broadcastId);
            jsonObject.put(KEY_SENDER_ID, sender);
            jsonObject.put(KEY_RECEIVER_ID, receiver);
            jsonObject.put(KEY_TARGET_ADDRESS, actualReceiver);
            jsonObject.put(KEY_BROADCAST_TEXT, textData);

            jsonObject.put(KEY_CONTENT_PATH, contentPath);
            jsonObject.put(KEY_CONTENT_EXPIRE_TIME, expireTime);

            jsonObject.put(KEY_CONTENT_LATITUDE, latitude);
            jsonObject.put(KEY_CONTENT_LONGITUDE, longitude);
            jsonObject.put(KEY_BROADCAST_RANGE, range);

            jsonObject.put(KEY_APP_TOKEN, appToken);

            jsonObject.put(KEY_MESSAGE, new String(data));

            jsonObject.put(KEY_MESSAGE_TYPE, APP_BROADCAST_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString().getBytes();
    }

    public static byte[] buildMessage(String sender, String receiver, String messageId, byte[] data) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE_ID, messageId);
            jo.put(KEY_MESSAGE, new String(data));

            jo.put(KEY_MESSAGE_TYPE, APP_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] buildVersionMessage(String sender, String receiver, int versionCode, String appToken,
                                             String versionName, String appSize, int versionType) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_VERSION_CODE, versionCode);
            jo.put(KEY_VERSION_NAME, versionName);
            jo.put(KEY_APP_SIZE, appSize);
            jo.put(KEY_APP_TOKEN, appToken);
            jo.put(KEY_VERSION_TYPE, versionType); // Version has two type 1. For version handshaking 2. For app update request
            jo.put(KEY_MESSAGE_TYPE, VERSION_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] buildHMACMessage(String sender, String receiver, int type,
                                          boolean isHmacRequest, String hmacValue) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            if (type == Constant.DataType.HMAC_REQUEST) {
                jo.put(KEY_HMAC_REQUEST, isHmacRequest);
            } else {
                jo.put(KEY_HMAC_RESPONSE, hmacValue);
            }
            jo.put(KEY_HMAC_MSG_TYPE, type); // HMAC has two type 1. For HMAC request 2. For HMAC response
            jo.put(KEY_MESSAGE_TYPE, HMAC_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }


    public static byte[] buildAckMessage(String sender, String receiver,
                                         String messageId, int status, String ackBody) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE_ID, messageId);
            jo.put(KEY_ACK_BODY, ackBody);
            jo.put(KEY_ACK_STATUS, status);

            jo.put(KEY_MESSAGE_TYPE, ACK_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] buildRouteInfoUpdateMessage(String targetAddress, String hopeId, long time) {
        MeshLog.i("(-) Send RouteInfo Update-Message target:: " + AddressUtil.makeShortAddress(targetAddress)
                + "updated hop:: " + AddressUtil.makeShortAddress(hopeId));

        if (AddressUtil.isValidEthAddress(targetAddress) && AddressUtil.isValidEthAddress(hopeId)) {

            JSONObject jo = new JSONObject();
            try {
                jo.put(KEY_SENDER_ID, SharedPref.read(Constant.KEY_USER_ID));
                jo.put(KEY_TARGET_ADDRESS, targetAddress);
                jo.put(KEY_HOP_NODE_ID, hopeId);
                jo.put(KEY_TIME_STAMP, time);

                jo.put(KEY_MESSAGE_TYPE, ROUTE_INFO);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jo.toString().getBytes();
        }
        return null;
    }

    public static byte[] buildDisconnectMessage() {

        String myAddress = SharedPref.read(Constant.KEY_USER_ID);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, myAddress);
            jo.put(KEY_MESSAGE_TYPE, DISCONNECT_REQUEST);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("(-) BuildDisconnectMessage: " + myAddress);
        return jo.toString().getBytes();
    }

    public static byte[] buildNodeLeaveEvent(String nodeId) {
        String myNodeId = SharedPref.read(Constant.KEY_USER_ID);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_LEAVE_NODE_ID, nodeId);
            jo.put(KEY_SENDER_ID, myNodeId);

            jo.put(KEY_MESSAGE_TYPE, NODE_LEAVE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] buildNodeLeaveEvent(String nodeId, String hopId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_LEAVE_NODE_ID, nodeId);
            jo.put(KEY_SENDER_ID, hopId);

            jo.put(KEY_MESSAGE_TYPE, NODE_LEAVE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] buildInternetUserIds(String sender, String receiver, String sellerId, String idList) {
        MeshLog.e("Wifi build internet user ids message :" + idList);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_SELLER_ID, sellerId);
            jo.put(KEY_INTERNET_USERS, idList);

            jo.put(KEY_MESSAGE_TYPE, INTERNET_USER);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo.toString().getBytes();
    }


    public static byte[] buildPayMessage(String sender, String receiver, String messageId, byte[] message) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE_ID, messageId);
            jo.put(KEY_MESSAGE, new String(message));

            jo.put(KEY_MESSAGE_TYPE, PAYMENT_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }


    public static byte[] buildPayMessageAck(String sender, String receiver, String messageId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_MESSAGE_ID, messageId);

            jo.put(KEY_MESSAGE_TYPE, PAYMENT_ACK);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }


    /**
     * Prepare Bt hello Packet
     *
     * @param myNodeId my node id
     * @return byte array of connectedUserList
     */
    public static byte[] prepareBtHelloPacket(String myNodeId, String userInfo) {
        NodeInfo myNodeInfo = myNodeInfoBuild(myNodeId, RoutingEntity.Type.BT);
        String myInfoJson = GsonUtil.on().toJsonFromItem(myNodeInfo);
        List<NodeInfo> connectedWifiUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiMeshUserNodeInfoList = new ArrayList<>();

        // wifi users
        List<RoutingEntity> connectedWifiUserList = RouteManager.getInstance().getWifiUser();
        if (CollectionUtil.hasItem(connectedWifiUserList)) {
            for (RoutingEntity routingEntity : connectedWifiUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().
                        getNodeInfoById(routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.BtMesh);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi mesh users
        List<RoutingEntity> connectedWifiMeshUserList = RouteManager.getInstance().getWifiMeshUser();
        if (CollectionUtil.hasItem(connectedWifiUserList)) {
            for (RoutingEntity routingEntity : connectedWifiMeshUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById
                        (routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.BtMesh);
                    connectedWifiMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }


        // adhoc users
        List<RoutingEntity> connectedAdhocUserList = RouteManager.getInstance().getAdhocUser();
        if (CollectionUtil.hasItem(connectedAdhocUserList)) {
            for (RoutingEntity routingEntity : connectedAdhocUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().
                        getNodeInfoById(routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.BtMesh);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }

        // adhocMesh users
        List<RoutingEntity> connectedAdhocMeshUserList = RouteManager.getInstance().getAdhocMeshUser();
        if (CollectionUtil.hasItem(connectedAdhocMeshUserList)) {
            for (RoutingEntity routingEntity : connectedAdhocMeshUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().
                        getNodeInfoById(routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.BtMesh);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }


        String connectedWifiUserNodeInfoListJson = GsonUtil.on().toJsonFromList(connectedWifiUserNodeInfoList);
        String connectedWifiMeshUserNodeInfoListJson = GsonUtil.on().toJsonFromList(connectedWifiMeshUserNodeInfoList);
        String logText = "";

        JSONObject jo = new JSONObject();
        //Frames.UserList.Builder usersProto = Frames.UserList.newBuilder();
        try {
            if (!TextUtils.isEmpty(connectedWifiUserNodeInfoListJson)) {
                logText = logText + "connectedWifiUserNodeInfoListJson: " + connectedWifiUserNodeInfoListJson;
                //usersProto.setWifiUser(connectedWifiUserNodeInfoListJson);
                jo.put(KEY_WIFI_USERS, connectedWifiUserNodeInfoListJson);
            }
            if (!TextUtils.isEmpty(connectedWifiMeshUserNodeInfoListJson)) {
                logText = logText + "connectedWifiMeshUserNodeInfoListJson: " + connectedWifiMeshUserNodeInfoListJson;
                //usersProto.setWifiMeshUser(connectedWifiMeshUserNodeInfoListJson);
                jo.put(KEY_WIFI_MESH_USER, connectedWifiMeshUserNodeInfoListJson);
            }
            MeshLog.i("prepareBtHelloPacket :: " + logText);
            jo.put(KEY_SENDER_INFO, myInfoJson);

            jo.put(KEY_MESSAGE_TYPE, BT_HELLO);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo.toString().getBytes();
    }

    /**
     * Prepare wifi hello Packet; this will use Adhoc Service Listener;
     *
     * @param myNodeId my node id
     * @param myIp     my ip address
     * @return byte array of connectedUserList
     */

    public static byte[] prepareWifiHelloPacketAsServiceListener(String myNodeId, String userInfo, String myIp, int type) {
        String myInfoJson = GsonUtil.on().toJsonFromItem(myNodeInfoBuild(myNodeId, myIp, RoutingEntity.Type.HB));
        List<NodeInfo> connectedBtUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedBtMeshUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiMeshUserNodeInfoList = new ArrayList<>();
        // Bt users; direct bt
        List<RoutingEntity> connectedBtUserList = RouteManager.getInstance().getBtUsers();
        if (CollectionUtil.hasItem(connectedBtUserList)) {
            for (RoutingEntity routingEntity : connectedBtUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedBtUserNodeInfoList.add(nodeInfo);
                }
            }
        }

        // Bt users; bt mesh
        List<RoutingEntity> connectedBtMeshUserList = RouteManager.getInstance().getBleMeshUsers();
        if (CollectionUtil.hasItem(connectedBtMeshUserList)) {
            for (RoutingEntity routingEntity : connectedBtMeshUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedBtMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi direct users
        List<RoutingEntity> connectedWifiUserList = RouteManager.getInstance().getWifiUser();
        if (CollectionUtil.hasItem(connectedWifiUserList)) {
            for (RoutingEntity routingEntity : connectedWifiUserList) {
                // exclude sender; if present in the list
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setIpAddress(routingEntity.getIp());
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi mesh users
        List<RoutingEntity> connectedWifiMeshUserList = RouteManager.getInstance().getWifiMeshUser();
        if (CollectionUtil.hasItem(connectedWifiMeshUserList)) {
            for (RoutingEntity routingEntity : connectedWifiMeshUserList) {
                // exclude sender; if present in the list

                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedWifiMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }

        connectedBtUserNodeInfoList.addAll(connectedWifiUserNodeInfoList);
        connectedBtMeshUserNodeInfoList.addAll(connectedWifiMeshUserNodeInfoList);

        String connectedBtUserListJson = GsonUtil.on().toJsonFromList(connectedBtUserNodeInfoList);
        String connectedBtMeshUserListJson = GsonUtil.on().toJsonFromList(connectedBtMeshUserNodeInfoList);

        //String connectedWifiUserListJson = GsonUtil.on().toJsonFromList(connectedWifiUserNodeInfoList);
        //String connectedWifiMeshUserListJson = GsonUtil.on().toJsonFromList(connectedWifiMeshUserNodeInfoList);

        String logText = "";
        //Frames.UserList.Builder userProto = Frames.UserList.newBuilder();
        JSONObject jo = new JSONObject();
        try {


            if (!TextUtils.isEmpty(connectedBtUserListJson)) {
                logText = logText + "connectedBtUserListJson: " + connectedBtUserListJson;
                jo.put(KEY_BT_USERS, connectedBtUserListJson);
                //userProto.setBtUser(connectedBtUserListJson);
            }
            if (!TextUtils.isEmpty(connectedBtMeshUserListJson)) {
                logText = logText + "connectedBtMeshUserListJson: " + connectedBtMeshUserListJson;
                jo.put(KEY_BT_MESH_USER, connectedBtMeshUserListJson);
                //userProto.setBtMeshUser(connectedBtMeshUserListJson);
            }
            /*if (!TextUtils.isEmpty(connectedWifiUserListJson)) {
                logText = logText + "connectedWifiUserListJson: " + connectedWifiUserListJson;
                jo.put(KEY_WIFI_USERS, connectedWifiUserListJson);
                //userProto.setWifiUser(connectedWifiUserListJson);
            }
            if (!TextUtils.isEmpty(connectedWifiMeshUserListJson)) {
                logText = logText + "connectedWifiMeshUserListJson: " + connectedWifiMeshUserListJson;
                jo.put(KEY_WIFI_MESH_USER, connectedWifiMeshUserListJson);
                //userProto.setWifiMeshUser(connectedWifiMeshUserListJson);

            }*/

            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_MESSAGE_TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("prepareWifiHelloPacketAsServiceListener :: " + logText);

        return jo.toString().getBytes();
    }


    /**
     * Prepare wifi hello Packet; this will use AdHoc Service Broadcaster;
     *
     * @param myNodeId my node id
     * @param myIp     my ip address
     * @return byte array of connectedUserList
     */
    public static byte[] prepareWifiHelloPacketAsAdHocBroadcaster(String myNodeId, String userInfo, String myIp, String senderId, boolean shouldAvoidBtlist) {
        String myInfoJson = GsonUtil.on().toJsonFromItem(myNodeInfoBuild(myNodeId, myIp, RoutingEntity.Type.HB));

        List<NodeInfo> connectedBtUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedBtMeshUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiMeshUserNodeInfoList = new ArrayList<>();


        if (!shouldAvoidBtlist) {
            // Bt users; direct bt
            List<RoutingEntity> connectedBtUserList = RouteManager.getInstance().getBtUsers();
            if (CollectionUtil.hasItem(connectedBtUserList)) {
                for (RoutingEntity routingEntity : connectedBtUserList) {
                    // exclude sender; if present in the list
                    if (routingEntity.getAddress().equals(senderId)) continue;
                    NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                            routingEntity.getAddress());
                    if (nodeInfo != null) {
                        nodeInfo = (NodeInfo) nodeInfo.clone();

                        nodeInfo.mGenerationTime = System.currentTimeMillis();
                        nodeInfo.setHopId(myNodeId);
                        nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                        connectedBtUserNodeInfoList.add(nodeInfo);
                    }
                }
            }


            // Bt users; bt mesh
            List<RoutingEntity> connectedBtMeshUserList = RouteManager.getInstance().getBleMeshUsers();
            if (CollectionUtil.hasItem(connectedBtMeshUserList)) {
                for (RoutingEntity routingEntity : connectedBtMeshUserList) {
                    // exclude sender; if present in the list
                    if (routingEntity.getAddress().equals(senderId)) continue;
                    if (routingEntity.getHopAddress() != null && routingEntity.getHopAddress().equals(senderId)) {
                        continue;
                    }
                    NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                            routingEntity.getAddress());
                    if (nodeInfo != null) {
                        nodeInfo = (NodeInfo) nodeInfo.clone();

                        nodeInfo.mGenerationTime = System.currentTimeMillis();
                        nodeInfo.setHopId(myNodeId);
                        nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                        connectedBtMeshUserNodeInfoList.add(nodeInfo);
                    }
                }
            }
        }

// wifi direct users
        List<RoutingEntity> connectedWifiUserList = RouteManager.getInstance().getWifiUser();
        if (CollectionUtil.hasItem(connectedWifiUserList)) {
            for (RoutingEntity routingEntity : connectedWifiUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setIpAddress(routingEntity.getIp());
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi mesh users
        List<RoutingEntity> connectedWifiMeshUserList = RouteManager.getInstance().getWifiMeshUser();
        if (CollectionUtil.hasItem(connectedWifiMeshUserList)) {
            for (RoutingEntity routingEntity : connectedWifiMeshUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                if (routingEntity.getHopAddress() != null) {
                    if (routingEntity.getHopAddress().equals(senderId)) continue;
                }
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedWifiMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }


        // Adhoc direct users
        List<RoutingEntity> connectedAdhocUserList = RouteManager.getInstance().getAdhocUser();
        if (CollectionUtil.hasItem(connectedAdhocUserList)) {
            for (RoutingEntity routingEntity : connectedAdhocUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();
                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setIpAddress(routingEntity.getIp());
                    nodeInfo.setUserType(RoutingEntity.Type.HB);
                    nodeInfo.setHopId("");
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi mesh users
        List<RoutingEntity> connectedAdhocMeshUserList = RouteManager.getInstance().getAdhocMeshUser();
        if (CollectionUtil.hasItem(connectedAdhocMeshUserList)) {
            for (RoutingEntity routingEntity : connectedAdhocMeshUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                if (routingEntity.getHopAddress() != null) {
                    if (routingEntity.getHopAddress().equals(senderId)) continue;
                }
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.HB_MESH);
                    connectedWifiMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        String connectedBtUserListJson = GsonUtil.on().toJsonFromList(connectedBtUserNodeInfoList);
        String connectedBtMeshUserListJson = GsonUtil.on().toJsonFromList(connectedBtMeshUserNodeInfoList);
        String connectedWifiUserListJson = GsonUtil.on().toJsonFromList(connectedWifiUserNodeInfoList);
        String connectedWifiMeshUserListJson = GsonUtil.on().toJsonFromList(connectedWifiMeshUserNodeInfoList);

        //Frames.UserList.Builder userProto = Frames.UserList.newBuilder();
        JSONObject jo = new JSONObject();
        String logText = "";
        try {


            if (!TextUtils.isEmpty(connectedBtUserListJson)) {
                logText = logText + "connectedBtUserListJson: " + connectedBtUserListJson;
                jo.put(KEY_BT_USERS, connectedBtUserListJson);
                //userProto.setBtUser(connectedBtUserListJson);
            }
            if (!TextUtils.isEmpty(connectedBtMeshUserListJson)) {
                logText = logText + "connectedBtMeshUserListJson: " + connectedBtMeshUserListJson;
                jo.put(KEY_BT_MESH_USER, connectedBtMeshUserListJson);
                //userProto.setBtMeshUser(connectedBtMeshUserListJson);
            }
            if (!TextUtils.isEmpty(connectedWifiUserListJson)) {
                logText = logText + "connectedWifiUserListJson: " + connectedWifiUserListJson;
                jo.put(KEY_WIFI_USERS, connectedWifiUserListJson);
                //userProto.setWifiUser(connectedWifiUserListJson);
            }
            if (!TextUtils.isEmpty(connectedWifiMeshUserListJson)) {
                logText = logText + "connectedWifiMeshUserListJson: " + connectedWifiMeshUserListJson;
                jo.put(KEY_WIFI_MESH_USER, connectedWifiMeshUserListJson);
                //userProto.setWifiMeshUser(connectedWifiMeshUserListJson);

            }
            jo.put(KEY_SENDER_INFO, myInfoJson);

            jo.put(KEY_MESSAGE_TYPE, ADHOC_HELLO_CLIENT);
        } catch (JSONException e) {

        }
        MeshLog.i("prepareWifiHelloPacketAsAdHocBroadcaster :: " + logText);

        return jo.toString().getBytes();

    }

    public static byte[] prepareP2pHelloPacketAsClient(String myNodeId, String myIp, int type, boolean isFileConnection) {

        String myInfoJson = GsonUtil.on().toJsonFromItem(getSelfEntity(myNodeId, myIp, type));
        List<RoutingEntity> allOnlineEntity = RouteManager.getInstance().getAllOnlineUserWithMinimumHop();
        List<RoutingEntity> filteredOnlineList = new ArrayList<>();
        for (RoutingEntity item : allOnlineEntity) {
            if (item.getType() != RoutingEntity.Type.INTERNET) {
                if (type == RoutingEntity.Type.WiFi) {
                    item.setType(RoutingEntity.Type.WifiMesh);
                } else {
                    item.setType(RoutingEntity.Type.BLE_MESH);
                }
                item.setOnline(true);
                item.setIp(null);
                item.setHopAddress(myNodeId);
                item.setHopCount(item.getHopCount() + 1);

                filteredOnlineList.add(item);
            }
        }
        /*
        List<RoutingEntity> allOffLineEntity = RouteManager.getInstance().getAllOffLineEntity();
        List<NodeInfo> offLineList = new ArrayList<>();
        for (RoutingEntity routingEntity : allOffLineEntity) {
            NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                    routingEntity.getAddress());
            if (nodeInfo != null) {
                nodeInfo = (NodeInfo) nodeInfo.clone();
                nodeInfo.isOnline = false;
                nodeInfo.mGenerationTime = System.currentTimeMillis();
                nodeInfo.setHopId(myNodeId);

                if (type == RoutingEntity.Type.WiFi) {
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                } else {
                    nodeInfo.setUserType(RoutingEntity.Type.BLE_MESH);
                }

                offLineList.add(nodeInfo);
            }
        }*/

        String onlineNodesJson = GsonUtil.on().toJsonFromEntityList(filteredOnlineList);
        //String offlineNodes = GsonUtil.on().toJsonFromList(offLineList);

        String dataId = isFileConnection ? getDataGenerationIdForFileConnection() : getDataGenerationId();
        AddressUtil.addDataGenerationId(dataId);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_ONLINE_MESH_NODES, onlineNodesJson);
            jo.put(KEY_OFFLINE_NODES, null);
            jo.put(KEY_DATA_PREPARE_ID, dataId);
            jo.put(KEY_MESSAGE_TYPE, HELLO_MASTER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.e("[BLE_PROCESS] prepare hello packet as client :" + jo.toString());
        return jo.toString().getBytes();
    }


    public static byte[] prepareP2pBleHelloPacketAsMaster(String myNodeId) {
        String myInfoJson = GsonUtil.on().toJsonFromItem(getSelfEntity(myNodeId, null, RoutingEntity.Type.BLE));

        List<RoutingEntity> allOnlineEntity = RouteManager.getInstance().getAllOnlineUserWithMinimumHop();
        List<RoutingEntity> allOnlineFilteredList = new ArrayList<>();
        for (RoutingEntity item : allOnlineEntity) {
            if (item.getType() != RoutingEntity.Type.INTERNET) {
                item.setType(RoutingEntity.Type.BLE_MESH);
                item.setHopCount(item.getHopCount() + 1);
                item.setHopAddress(myNodeId);
                item.setIp(null);

                allOnlineFilteredList.add(item);
            }
        }
        /*List<NodeInfo> onlineList = new ArrayList<>();

        for (RoutingEntity routingEntity : allOnlineEntity) {
            NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                    routingEntity.getAddress());
            if (nodeInfo != null) {
                nodeInfo = (NodeInfo) nodeInfo.clone();
                nodeInfo.isOnline = true;
                nodeInfo.mGenerationTime = System.currentTimeMillis();
                nodeInfo.setHopId(myNodeId);
                nodeInfo.setUserType(RoutingEntity.Type.BLE_MESH);
                onlineList.add(nodeInfo);
            }
        }*/

        /*List<RoutingEntity> allOffLineEntity = RouteManager.getInstance().getAllOffLineEntity();
        List<NodeInfo> offLineList = new ArrayList<>();
        for (RoutingEntity routingEntity : allOffLineEntity) {
            NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                    routingEntity.getAddress());
            if (nodeInfo != null) {
                nodeInfo = (NodeInfo) nodeInfo.clone();
                nodeInfo.isOnline = false;
                nodeInfo.mGenerationTime = System.currentTimeMillis();
                nodeInfo.setHopId(myNodeId);
                nodeInfo.setUserType(RoutingEntity.Type.BLE_MESH);
                offLineList.add(nodeInfo);
            }
        }*/

        String onlineNodesJson = GsonUtil.on().toJsonFromEntityList(allOnlineFilteredList);
        //String offlineNodes = GsonUtil.on().toJsonFromList(offLineList);

        String dataId = getDataGenerationId();
        AddressUtil.addDataGenerationId(dataId);

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_ONLINE_MESH_NODES, onlineNodesJson);
            jo.put(KEY_OFFLINE_NODES, null);
            jo.put(KEY_DATA_PREPARE_ID, dataId);
            jo.put(KEY_MESSAGE_TYPE, HELLO_CLIENT);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MeshLog.e("[BLE_PROCESS] Prepare hello ble master: " + jo.toString());
        return jo.toString().getBytes();
    }


    public static byte[] prepareP2pWifiHelloPacketAsMaster(String myNodeId, String myIp) {

        String myInfoJson = GsonUtil.on().toJsonFromItem(getSelfEntity(myNodeId, myIp, RoutingEntity.Type.WiFi));

        //Online LC
        List<RoutingEntity> lcOnlineEntityList = RouteManager.getInstance().getWifiUser();
        /*List<NodeInfo> lcNodeInfoList = new ArrayList<>();
        for (RoutingEntity item : lcOnlineEntityList) {
            NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(item.getAddress());
            if (nodeInfo != null) {
                nodeInfo = (NodeInfo) nodeInfo.clone();
                nodeInfo.setIpAddress(item.getIp());
                nodeInfo.setHopId(null);
                nodeInfo.isOnline = true;
                nodeInfo.setUserType(RoutingEntity.Type.WiFi);
                lcNodeInfoList.add(nodeInfo);
            }
        }*/

        //Mesh user List


        //Online other
        List<RoutingEntity> myWifiMeshUser = RouteManager.getInstance().getAllUserByType(RoutingEntity.Type.WifiMesh);
        for (RoutingEntity item : myWifiMeshUser) {
            item.setIp(null);
            item.setOnline(true);
        }

        List<RoutingEntity> uniqueMeshEntityList = new ArrayList<>();

        for (RoutingEntity item : myWifiMeshUser) {
            boolean isMatch = false;
            for (RoutingEntity meshEntity : lcOnlineEntityList) {
                if (item.getAddress().equals(meshEntity.getAddress())) {
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                uniqueMeshEntityList.add(item);
            }
        }

        // ble and ble mesh user

        List<RoutingEntity> allBleUser = RouteManager.getInstance().getAllBleAndBleMeshUser();

        for (RoutingEntity item : allBleUser) {
            item.setHopCount(item.getHopCount() + 1);
            item.setType(RoutingEntity.Type.WifiMesh);
            item.setHopAddress(myNodeId);
            item.setOnline(true);
            item.setIp(null);
        }

        List<RoutingEntity> tempEntityList = new ArrayList<>();
        tempEntityList.addAll(lcOnlineEntityList);
        tempEntityList.addAll(uniqueMeshEntityList);

        List<RoutingEntity> uniqueBleUsers = new ArrayList<>();

        for (RoutingEntity item : allBleUser) {
            boolean isMatch = false;
            for (RoutingEntity meshEntity : tempEntityList) {
                if (item.getAddress().equals(meshEntity.getAddress())) {
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                uniqueBleUsers.add(item);
            }
        }

        List<RoutingEntity> allMeshUserList = new ArrayList<>();
        allMeshUserList.addAll(uniqueMeshEntityList);
        allMeshUserList.addAll(uniqueBleUsers);



        /*List<RoutingEntity> uniqueOtherOnlineEntity = new ArrayList<>();

        if (uniqueNodeList != null && !uniqueNodeList.isEmpty()) {
            for (RoutingEntity dbEntity : otherOnlineEntities) {
                boolean isExistInCycleList = false;
                for (RoutingEntity cyEntity : uniqueNodeList) {
                    if (dbEntity.getAddress().equals(cyEntity.getAddress())) {
                        isExistInCycleList = true;
                        break;
                    }
                }

                if (!isExistInCycleList) {
                    uniqueOtherOnlineEntity.add(dbEntity);
                }
            }
        } else {
            uniqueOtherOnlineEntity.addAll(otherOnlineEntities);
        }*/

        /*List<NodeInfo> otherOnline = new ArrayList<>();
        for (RoutingEntity item : uniqueOtherOnlineEntity) {
            NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(item.getAddress());
            if (nodeInfo != null) {
                nodeInfo = (NodeInfo) nodeInfo.clone();
                nodeInfo.setIpAddress(null);
                nodeInfo.setHopId(myNodeId);
                nodeInfo.isOnline = true;
                nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                otherOnline.add(nodeInfo);
            }
        }
      */
        //Offline nodes
        /*List<RoutingEntity> allOffLineEntity = RouteManager.getInstance().getAllOffLineEntity();
        List<NodeInfo> offLineList = new ArrayList<>();
        for (RoutingEntity routingEntity : allOffLineEntity) {
            NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                    routingEntity.getAddress());
            if (nodeInfo != null) {
                nodeInfo = (NodeInfo) nodeInfo.clone();
                nodeInfo.isOnline = false;
                nodeInfo.mGenerationTime = System.currentTimeMillis();
                nodeInfo.setHopId(myNodeId);
                nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                offLineList.add(nodeInfo);
            }
        }*/

        String lcJsonString = GsonUtil.on().toJsonFromEntityList(lcOnlineEntityList);
        String onlineMeshString = GsonUtil.on().toJsonFromEntityList(allMeshUserList);
        //String offlineMeshString = GsonUtil.on().toJsonFromList(offLineList);


        JSONObject jo = new JSONObject();
        String dataId = getDataGenerationId();
        AddressUtil.addDataGenerationId(dataId);

        try {
            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_ONLINE_DIRECT_NODES, lcJsonString);
            jo.put(KEY_ONLINE_MESH_NODES, onlineMeshString);
            jo.put(KEY_OFFLINE_NODES, null);
            jo.put(KEY_DATA_PREPARE_ID, dataId);
            jo.put(KEY_MESSAGE_TYPE, HELLO_CLIENT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.e("[BLE_PROCESS] Prepare hello wifi master: " + jo.toString());

        return jo.toString().getBytes();
    }


    /**
     * Prepare wifi hello Packet; this will use wifi direct Group Owner;
     *
     * @param myNodeId my node id
     * @param myIp     my ip address
     * @return byte array of connectedUserList
     */
    public static byte[] prepareWifiHelloPacketAsMaster(String myNodeId, String userInfo, String myIp, String senderId) {
        String myInfoJson = GsonUtil.on().toJsonFromItem(myNodeInfoBuild(myNodeId, myIp, RoutingEntity.Type.WiFi));

        List<NodeInfo> connectedBtUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedBtMeshUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiUserNodeInfoList = new ArrayList<>();
        List<NodeInfo> connectedWifiMeshUserNodeInfoList = new ArrayList<>();
        // Bt users; direct bt
        List<RoutingEntity> connectedBtUserList = RouteManager.getInstance().getBtUsers();
        if (CollectionUtil.hasItem(connectedBtUserList)) {
            for (RoutingEntity routingEntity : connectedBtUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                    connectedBtUserNodeInfoList.add(nodeInfo);
                }
            }
        }


        // Bt users; bt mesh
        List<RoutingEntity> connectedBtMeshUserList = RouteManager.getInstance().getBleMeshUsers();
        if (CollectionUtil.hasItem(connectedBtMeshUserList)) {
            for (RoutingEntity routingEntity : connectedBtMeshUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                if (routingEntity.getHopAddress() != null && routingEntity.getHopAddress().equals(senderId)) {
                    continue;
                }
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                    connectedBtMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi direct users
        List<RoutingEntity> connectedWifiUserList = RouteManager.getInstance().getWifiUser();
        if (CollectionUtil.hasItem(connectedWifiUserList)) {
            for (RoutingEntity routingEntity : connectedWifiUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(null);
                    nodeInfo.setIpAddress(routingEntity.getIp());
                    nodeInfo.setUserType(RoutingEntity.Type.WiFi);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // wifi mesh users
        List<RoutingEntity> connectedWifiMeshUserList = RouteManager.getInstance().getWifiMeshUser();
        if (CollectionUtil.hasItem(connectedWifiMeshUserList)) {
            for (RoutingEntity routingEntity : connectedWifiMeshUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                if (routingEntity.getHopAddress() != null) {
                    if (routingEntity.getHopAddress().equals(senderId)) continue;
                }
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(routingEntity.getHopAddress());
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                    connectedWifiMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }

        // Adhoc users
        List<RoutingEntity> connectedAdhocUserList = RouteManager.getInstance().getAdhocUser();
        if (CollectionUtil.hasItem(connectedAdhocUserList)) {
            for (RoutingEntity routingEntity : connectedAdhocUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setIpAddress(routingEntity.getIp());
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                    connectedWifiUserNodeInfoList.add(nodeInfo);
                }
            }
        }
        // Adhoc mesh users
        List<RoutingEntity> connectedAdhocMeshUserList = RouteManager.getInstance().getAdhocMeshUser();
        if (CollectionUtil.hasItem(connectedAdhocMeshUserList)) {
            for (RoutingEntity routingEntity : connectedAdhocMeshUserList) {
                // exclude sender; if present in the list
                if (routingEntity.getAddress().equals(senderId)) continue;
                if (routingEntity.getHopAddress() != null) {
                    if (routingEntity.getHopAddress().equals(senderId)) continue;
                }
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                    connectedWifiMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        }

        String connectedBtUserListJson = GsonUtil.on().toJsonFromList(connectedBtUserNodeInfoList);
        String connectedBtMeshUserListJson = GsonUtil.on().toJsonFromList(connectedBtMeshUserNodeInfoList);
        String connectedWifiUserListJson = GsonUtil.on().toJsonFromList(connectedWifiUserNodeInfoList);
        String connectedWifiMeshUserListJson = GsonUtil.on().toJsonFromList(connectedWifiMeshUserNodeInfoList);

        //Frames.UserList.Builder userProto = Frames.UserList.newBuilder();
        String logText = "";
        JSONObject jo = new JSONObject();
        try {
            if (!TextUtils.isEmpty(connectedBtUserListJson)) {
                logText = logText + "connectedBtUserListJson: " + connectedBtUserListJson;
                jo.put(KEY_BT_USERS, connectedBtUserListJson);
                //userProto.setBtUser(connectedBtUserListJson);
            }
            if (!TextUtils.isEmpty(connectedBtMeshUserListJson)) {
                logText = logText + "connectedBtMeshUserListJson: " + connectedBtMeshUserListJson;
                jo.put(KEY_BT_MESH_USER, connectedBtMeshUserListJson);
                //userProto.setBtMeshUser(connectedBtMeshUserListJson);
            }
            if (!TextUtils.isEmpty(connectedWifiUserListJson)) {
                logText = logText + "connectedWifiUserListJson: " + connectedWifiUserListJson;
                jo.put(KEY_WIFI_USERS, connectedWifiUserListJson);
                //userProto.setWifiUser(connectedWifiUserListJson);
            }
            if (!TextUtils.isEmpty(connectedWifiMeshUserListJson)) {
                logText = logText + "connectedWifiMeshUserListJson: " + connectedWifiMeshUserListJson;
                jo.put(KEY_WIFI_MESH_USER, connectedWifiMeshUserListJson);
                //userProto.setWifiMeshUser(connectedWifiMeshUserListJson);
            }

            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_MESSAGE_TYPE, P2P_HELLO_CLIENT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("prepareWifiHelloPacketAsMaster :: " + logText);
        return jo.toString().getBytes();


    }

    public static NodeInfo myNodeInfoBuild(String myNodeId, int type) {
        String myBleName = SharedPref.read(Constant.KEY_DEVICE_BLE_NAME);

        MeshLog.w("MyNodeInfoBuild with BT name::" + myBleName);
        //String mySsidName = SharedPref.read(Constant.KEY_DEVICE_SSID_NAME);
        String myPublicKey = SharedPref.read("public_key");
        int deviceMode = PreferencesHelper.on().getDataShareMode();
        return new NodeInfo.Builder()
                .setUserId(myNodeId)
                //.setUserInfo(userInfo)
                //.setBleName(myBleName)
                .setHopId(null)
                .setIpAddress("")
                //.setSsidName(mySsidName)
                .setUserMode(deviceMode)
                .setPublicKey(myPublicKey)
                .setUserType(type)
                .setTime(System.currentTimeMillis())
                .build();
    }

    private static RoutingEntity getSelfEntity(String myNodeId, int type) {
        String myPublicKey = SharedPref.read("public_key");
        int deviceMode = PreferencesHelper.on().getDataShareMode();
        RoutingEntity selfEntity = new RoutingEntity(myNodeId);
        selfEntity.setType(type);
        selfEntity.setPublicKey(myPublicKey);
        selfEntity.setUserMode(deviceMode);
        selfEntity.setHopCount(0);
        return selfEntity;
    }

    private static RoutingEntity getSelfEntity(String myNodeId, String myIp, int type) {
        String myPublicKey = SharedPref.read("public_key");
        int deviceMode = PreferencesHelper.on().getDataShareMode();
        RoutingEntity myEntity = new RoutingEntity(myNodeId);
        myEntity.setType(type);
        myEntity.setIp(myIp);
        myEntity.setPublicKey(myPublicKey);
        myEntity.setUserMode(deviceMode);
        myEntity.setOnline(true);
        myEntity.setHopCount(0);
        return myEntity;
    }


    private static NodeInfo myNodeInfoBuild(String myNodeId, String myIp, int type) {
        String myBleName = SharedPref.read(Constant.KEY_DEVICE_BLE_NAME);

        MeshLog.i("myNodeInfoBuild with BT name::" + myBleName);
        //String mySsidName = SharedPref.read(Constant.KEY_DEVICE_SSID_NAME);
        String myPublicKey = SharedPref.read("public_key");
        int deviceMode = PreferencesHelper.on().getDataShareMode();
        return new NodeInfo.Builder()
                .setUserId(myNodeId)
                //.setUserInfo(userInfo)
                //.setBleName(myBleName)
                .setHopId(null)
                .setIpAddress(myIp)
                //.setSsidName(mySsidName)
                .setUserMode(deviceMode)
                .setPublicKey(myPublicKey)
                .setUserType(type)
                .setTime(System.currentTimeMillis())
                .setBle(type == RoutingEntity.Type.BLE)
                .build();
    }


    public static byte[] prepareBtUserToSendWifi(String myNodeId, ConcurrentLinkedQueue<NodeInfo> allNodeInfoList) {
        for (NodeInfo item : allNodeInfoList) {
            item.setHopId(myNodeId);
            item.setUserType(RoutingEntity.Type.WifiMesh);
        }

        String nodeInfoListJson = GsonUtil.on().toJsonFromQueue(allNodeInfoList);


        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_MESH_USERS, nodeInfoListJson);
            jo.put(KEY_HOP_NODE_ID, myNodeId);
            jo.put(KEY_SENDER_ID, myNodeId);

            jo.put(KEY_MESSAGE_TYPE, MESH_USER);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo.toString().getBytes();

    }

    public static byte[] prepareBtUserToSendAdhoc(String myNodeId, ConcurrentLinkedQueue<NodeInfo> allNodeInfoList) {
        for (NodeInfo item : allNodeInfoList) {
            item.setHopId(myNodeId);
            item.setUserType(RoutingEntity.Type.HB_MESH);
        }

        String nodeInfoListJson = GsonUtil.on().toJsonFromQueue(allNodeInfoList);


        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_MESH_USERS, nodeInfoListJson);
            jo.put(KEY_HOP_NODE_ID, myNodeId);

            jo.put(KEY_MESSAGE_TYPE, MESH_USER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();


    }


    public static byte[] prepareMissingBtMeshUserToSendWifiMaster(String myNodeId) {
        List<NodeInfo> connectedBtMeshUserNodeInfoList = new ArrayList<>();
        long startTime = ConnectionLinkCache.getInstance().getClientRequestTime();
        long endTime = System.currentTimeMillis();
        if (startTime == 0 && endTime == 0) {
            return null;
        }
        // Bt users; bt mesh
        List<RoutingEntity> connectedBtMeshUserList = RouteManager.getInstance().getBleMeshUsersByTimeDifference(myNodeId, startTime, endTime);
        if (CollectionUtil.hasItem(connectedBtMeshUserList)) {
            MeshLog.i("prepareMissingBtMeshUserToSendWifiMaster:: " + connectedBtMeshUserList.size());
            for (RoutingEntity routingEntity : connectedBtMeshUserList) {
                NodeInfo nodeInfo = ConnectionLinkCache.getInstance().getNodeInfoById(
                        routingEntity.getAddress());
                if (nodeInfo != null) {
                    nodeInfo = (NodeInfo) nodeInfo.clone();

                    nodeInfo.mGenerationTime = System.currentTimeMillis();
                    nodeInfo.setHopId(myNodeId);
                    nodeInfo.setUserType(RoutingEntity.Type.WifiMesh);
                    connectedBtMeshUserNodeInfoList.add(nodeInfo);
                }
            }
        } else {
            MeshLog.i("prepareMissingBtMeshUserToSendWifiMaster:: is empty");
            return null;
        }

        MeshLog.i("prepareMissingBtMeshUserToSendWifiMaster:: " + connectedBtMeshUserNodeInfoList.toString());

        String nodeInfoListJson = GsonUtil.on().toJsonFromList(connectedBtMeshUserNodeInfoList);

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_MESH_USERS, nodeInfoListJson);
            jo.put(KEY_HOP_NODE_ID, myNodeId);

            jo.put(KEY_MESSAGE_TYPE, MESH_USER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] prepareInternetLeaveMessage(String sender, String receiver, String nodeIdList) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_USER_LIST, nodeIdList);

            jo.put(KEY_MESSAGE_TYPE, INTERNET_USER_LEAVE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();

    }

    public static byte[] prepareHighBandRequestMessage(String sender, String receiver) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);


            jo.put(KEY_MESSAGE_TYPE, MESSAGE_TYPE_HIGH_BAND);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] prepareDryHighBandRequestMessage(String sender, String receiver) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);


            jo.put(KEY_MESSAGE_TYPE, MESSAGE_TYPE_DRY_HIGH_BAND);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    /**
     * @param sender   the originator of this message
     * @param receiver
     * @param ssid
     * @param key
     * @return
     */
    // Confirmation message for the HighBandwidth message availability
    public static byte[] confirmationHighBandRequestMessage(String sender, String receiver, String ssid, String key) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_HS_SSID, ssid);
            jo.put(KEY_HS_PWD, key);

            jo.put(KEY_MESSAGE_TYPE, MESSAGE_TYPE_HIGH_BAND_CONFIRMATION);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] buildUserRoleSwitchMessage(String sender, String receiver, int newRole) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_NEW_ROLE, newRole);
            jo.put(KEY_MESSAGE_TYPE, USER_ROLE_SWITCH);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }


    public static boolean isBroadCastMessage(String message) {
        int messageType = 0;
        try {
            JSONObject jsonObject = new JSONObject(message);
            messageType = jsonObject.optInt(KEY_MESSAGE_TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return messageType == APP_BROADCAST_MESSAGE;
    }

    public static byte[] prepareBroadcastACKMessage(String sender, String receiver, String broadcastId) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_BROADCAST_ID, broadcastId);

            jo.put(KEY_MESSAGE_TYPE, APP_BROADCAST_ACK_MESSAGE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }


    public static byte[] prepareBleDecisionMessage(String userId) {
        JSONObject jsonObject = new JSONObject();
        try {
            int count = RouteManager.getInstance().getWifiUser().size();

            int goAddressHash = 0;
            if (P2PUtil.isMeGO()) {
                goAddressHash = userId.hashCode();
            } else {
                RoutingEntity goEntity = RouteManager.getInstance()
                        .getGoRoutingEntityByIp(WifiTransPort.P2P_MASTER_IP_ADDRESS);
                if (goEntity != null) {
                    goAddressHash = goEntity.getAddress().hashCode();
                }
            }

            jsonObject.put(GO_ADDRESS_HASH, goAddressHash);
            jsonObject.put(ETH_ID, userId);
            jsonObject.put(MY_NODE_COUNT, count);
            jsonObject.put(MY_CURRENT_MODE, PreferencesHelper.on().getDataShareMode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString().getBytes();
    }

    public static byte[] prepareBleForceConnectionMessage(String userId, boolean isForce, boolean isServer) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ETH_ID, userId);
            jsonObject.put(FORCE_CONNECTION, isForce);
            jsonObject.put(IS_SERVER, isServer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString().getBytes();
    }

    public static byte[] prepareBleForceConnectionReply(String userId, boolean isAbleToReceive) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ETH_ID, userId);
            jsonObject.put(IS_ABLE_TO_RECEIVE, isAbleToReceive);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString().getBytes();
    }


    public static byte[] v2BuildMeshNodePacketToSendWifiLc(String myUserId, ConcurrentLinkedQueue<RoutingEntity> onlineNodes,
                                                           ConcurrentLinkedQueue<RoutingEntity> offLineNodes, String dataId) {

        String onlineNodeString = GsonUtil.on().toJsonFromEntityList(onlineNodes);
        String offlineNodeString = GsonUtil.on().toJsonFromEntityList(offLineNodes);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, myUserId);
            jo.put(KEY_ONLINE_MESH_NODES, onlineNodeString);
            jo.put(KEY_OFFLINE_NODES, offlineNodeString);
            jo.put(KEY_DATA_PREPARE_ID, dataId);
            jo.put(KEY_MESSAGE_TYPE, MESH_NODE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] prepare packet: " + jo.toString());
        return jo.toString().getBytes();
    }


    public static byte[] v2BuildMeshNodePacketToSendBle(String myUserId, ConcurrentLinkedQueue<RoutingEntity> onlineNodes,
                                                        ConcurrentLinkedQueue<RoutingEntity> offLineNodes, String dataId) {

        if (CollectionUtil.hasItem(onlineNodes)) {
            for (RoutingEntity item : onlineNodes) {
                item.setHopAddress(myUserId);
                item.setOnline(true);
                item.setType(RoutingEntity.Type.BLE_MESH);
                item.setHopCount(item.getHopCount() + 1);
                item.setIp(null);
            }
        }

        if (CollectionUtil.hasItem(offLineNodes)) {
            for (RoutingEntity item : offLineNodes) {
                item.setHopAddress(myUserId);
                item.setOnline(false);
                item.setType(RoutingEntity.Type.BLE_MESH);
                item.setHopCount(item.getHopCount() + 1);
                item.setIp(null);
            }
        }

        String onlineNodeString = GsonUtil.on().toJsonFromEntityList(onlineNodes);
        String offlineNodeString = GsonUtil.on().toJsonFromEntityList(offLineNodes);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, myUserId);
            jo.put(KEY_ONLINE_MESH_NODES, onlineNodeString);
            jo.put(KEY_OFFLINE_NODES, offlineNodeString);
            jo.put(KEY_DATA_PREPARE_ID, dataId);
            jo.put(KEY_MESSAGE_TYPE, MESH_NODE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] packet to send ble: " + jo.toString());
        return jo.toString().getBytes();
    }

    public static byte[] v2BuildMeshNodePacketToSendLcUsers(String myAddress, ConcurrentLinkedQueue<RoutingEntity> onlineMeshNodes,
                                                            ConcurrentLinkedQueue<RoutingEntity> offlineMeshNode, String dataId) {

        if (CollectionUtil.hasItem(onlineMeshNodes)) {
            for (RoutingEntity item : onlineMeshNodes) {
                item.setHopAddress(myAddress);
                item.setOnline(true);
                item.setType(RoutingEntity.Type.WifiMesh);
                item.setHopCount(item.getHopCount() + 1);
                item.setIp(null);
            }
        }

        if (CollectionUtil.hasItem(offlineMeshNode)) {
            for (RoutingEntity item : offlineMeshNode) {
                item.setHopAddress(myAddress);
                item.setOnline(false);
                item.setType(RoutingEntity.Type.UNDEFINED);
                item.setHopCount(item.getHopCount() + 1);
                item.setIp(null);
            }
        }

        String onlineNodeString = GsonUtil.on().toJsonFromEntityList(onlineMeshNodes);
        String offlineNodeString = GsonUtil.on().toJsonFromEntityList(offlineMeshNode);
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, myAddress);
            jo.put(KEY_ONLINE_MESH_NODES, onlineNodeString);
            jo.put(KEY_OFFLINE_NODES, offlineNodeString);
            jo.put(KEY_DATA_PREPARE_ID, dataId);
            jo.put(KEY_MESSAGE_TYPE, MESH_NODE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] packet to send Lc: " + jo.toString());
        return jo.toString().getBytes();

    }

    /**
     * @param sender     the originator of this message
     * @param receiver
     * @param ssid
     * @param passPhrase
     * @param goNodeId
     * @return
     */
    public static byte[] prepareAPCredentialMessage(String sender, String receiver, String ssid, String passPhrase, String goNodeId) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_SOFTAP_SSID, ssid);
            jo.put(KEY_SOFTAP_PASSWORD, passPhrase);
            jo.put(KEY_GO_ID, goNodeId);
            jo.put(KEY_MESSAGE_TYPE, CREDENTIAL_MESSAGE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] credential msg: " + jo.toString());

        return jo.toString().getBytes();
    }

    public static byte[] prepareBleMeshDecisionMessage(String sender, String receiver, String ssid,
                                                       String password, int wifiUserCount, boolean isFirst) {

        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, sender);
            jo.put(KEY_RECEIVER_ID, receiver);
            jo.put(KEY_SOFTAP_SSID, ssid);
            jo.put(KEY_SOFTAP_PASSWORD, password);
            jo.put(MY_NODE_COUNT, wifiUserCount);
            jo.put(IS_SERVER, isFirst);
            jo.put(KEY_MESSAGE_TYPE, BLE_MESH_DECISION_MESSAGE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] credential msg: " + jo.toString());

        return jo.toString().getBytes();
    }

    public static byte[] prepareForceConnectionMessage(String senderId, String receiverId,
                                                       String ssid, String password, boolean isRequest, boolean isAbleToReceive) {
        JSONObject jo = new JSONObject();

        try {
            jo.put(KEY_SENDER_ID, senderId);
            jo.put(KEY_RECEIVER_ID, receiverId);
            jo.put(KEY_SOFTAP_SSID, ssid);
            jo.put(KEY_SOFTAP_PASSWORD, password);
            jo.put(IS_FORCE_REQUEST, isRequest);
            jo.put(IS_ABLE_TO_RECEIVE, isAbleToReceive);
            jo.put(KEY_MESSAGE_TYPE, FORCE_CONNECTION_MESSAGE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] force connection message prepare: " + jo.toString());

        return jo.toString().getBytes();
    }

    public static byte[] prepareFreeMessageForRequestNode(String senderId, String receiverId, boolean isAbleToReceive) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, senderId);
            jo.put(KEY_RECEIVER_ID, receiverId);
            jo.put(IS_ABLE_TO_RECEIVE, isAbleToReceive);
            jo.put(KEY_MESSAGE_TYPE, FILE_RECEIVE_FREE_MESSAGE);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("[BLE_PROCESS] free message prepare: " + jo.toString());

        return jo.toString().getBytes();
    }


    public static byte[] buildNetworkFullResponseMessage(String myNodeId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SENDER_ID, myNodeId);
            jo.put(KEY_MESSAGE_TYPE, GO_NETWORK_FULL);

        } catch (JSONException e) {
        }

        return jo.toString().getBytes();
    }

    public static byte[] prepareCredentialMessage(String ssid, String password, String myId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_SOFTAP_SSID, ssid);
            jo.put(KEY_SOFTAP_PASSWORD, password);


            String goAddressHash = "";
            if (P2PUtil.isMeGO()) {
                goAddressHash = myId;
            } else {
                RoutingEntity goEntity = RouteManager.getInstance()
                        .getGoRoutingEntityByIp(WifiTransPort.P2P_MASTER_IP_ADDRESS);
                if (goEntity != null) {
                    goAddressHash = goEntity.getAddress();
                }
            }

            jo.put(KEY_GO_ID, goAddressHash);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] prepareSpecialDisconnectMessage(String receiverId, String duplicateNodeId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_RECEIVER_ID, receiverId);
            jo.put(KEY_DUPLICATE_ID, duplicateNodeId);
            jo.put(KEY_MESSAGE_TYPE, SPECIAL_DISCONNECT_MESSAGE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static byte[] buildFailedMessageAck(String ackSender, String from, String to, String messageId) {
        JSONObject jo = new JSONObject();
        try {
            jo.put(KEY_ACK_SENDER, ackSender);
            jo.put(KEY_MESSAGE_ID, messageId);
            jo.put(KEY_MESSAGE_SOURCE, from);
            jo.put(KEY_MESSAGE_DESTINATION, to);
            jo.put(KEY_MESSAGE_TYPE, FAILED_MESSAGE_ACK);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString().getBytes();
    }

    public static String getDataGenerationId() {
        return UUID.randomUUID().toString();
    }

    public static String getDataGenerationIdForFileConnection() {
        return Constant.FILE_CONNECTION + UUID.randomUUID().toString();
    }

    /**
     * prepare Remote Hello Packet
     *
     * @param myNodeId my node id
     * @return byte array of connectedUserList
     */
    public static String prepareRemoteHelloPacket(String myNodeId, String userInfo) {
        String myInfoJson = GsonUtil.on().toJsonFromItem(getSelfEntity(myNodeId, userInfo, RoutingEntity.Type.INTERNET));

        List<RoutingEntity> connectedUserNodeInfoList = new ArrayList<>();

        for (String address : ConnectionLinkCache.getInstance().getInternetBuyerList()) {
            RoutingEntity nodeInfo = RouteManager.getInstance().getEntityByAddress(address);
            if (nodeInfo != null) {
                nodeInfo.setIp(null);
                nodeInfo.setHopAddress(myNodeId);
                nodeInfo.setHopCount(nodeInfo.getHopCount() + 1);
                nodeInfo.setType(RoutingEntity.Type.INTERNET);
                connectedUserNodeInfoList.add(nodeInfo);
            }
        }


        String connectedUserListJson = GsonUtil.on().toJsonFromEntityList(connectedUserNodeInfoList);

        String logText = "";
        JSONObject jo = new JSONObject();
        try {
            if (!TextUtils.isEmpty(connectedUserListJson)) {
                logText = logText + "connectedBtUserListJson: " + connectedUserListJson;
                jo.put(KEY_CONNECTED_BUYERS, connectedUserListJson);
            }

            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_MESSAGE_TYPE, REMOTE_USERS_DETAILS_REQUEST);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("prepareUserListForRemote :: " + logText);
        return jo.toString();
    }

    /**
     * prepare Remote Hello Packet
     *
     * @param myNodeId my node id
     * @return byte array of connectedUserList
     */
    public static String prepareRemoteHelloPacketResponse(String myNodeId, String userInfo) {
        String myInfoJson = GsonUtil.on().toJsonFromItem(myNodeInfoBuild(myNodeId, userInfo, RoutingEntity.Type.INTERNET));

        List<RoutingEntity> connectedUserNodeInfoList = new ArrayList<>();

        for (String address : ConnectionLinkCache.getInstance().getInternetBuyerList()) {
            RoutingEntity nodeInfo = RouteManager.getInstance().getEntityByAddress(address);
            if (nodeInfo != null) {
                nodeInfo.setIp(null);
                nodeInfo.setHopAddress(myNodeId);
                nodeInfo.setHopCount(nodeInfo.getHopCount() + 1);
                nodeInfo.setType(RoutingEntity.Type.INTERNET);
                connectedUserNodeInfoList.add(nodeInfo);
            }
        }


        String connectedUserListJson = GsonUtil.on().toJsonFromEntityList(connectedUserNodeInfoList);

        String logText = "";
        JSONObject jo = new JSONObject();
        try {
            if (!TextUtils.isEmpty(connectedUserListJson)) {
                logText = logText + "connectedBtUserListJson: " + connectedUserListJson;
                jo.put(KEY_CONNECTED_BUYERS, connectedUserListJson);
            }

            jo.put(KEY_SENDER_INFO, myInfoJson);
            jo.put(KEY_MESSAGE_TYPE, REMOTE_USERS_DETAILS_RESPONSE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MeshLog.i("prepareRemoteHelloPacketResponse :: " + logText);
        return jo.toString();
    }

    public static byte[] prepareBuyerLeaveMessage(String nodeId, String mNodeId) {
        try {
            JSONObject messageObject = new JSONObject();
            messageObject.put(KEY_LEAVE_NODE_ID, nodeId);
            messageObject.put(KEY_HOP_NODE_ID, mNodeId);
            messageObject.put(KEY_MESSAGE_TYPE, BUYER_LEAVE);
            return messageObject.toString().getBytes();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] prepareBuyerUserList(List<RoutingEntity> nodeInfoList, String myNodeId) {
        try {
            JSONObject jsonObject = new JSONObject();
            String userList = nodeInfoList.isEmpty() ? "" : GsonUtil.on().toJsonFromEntityList(nodeInfoList);

            jsonObject.put(KEY_SENDER_ID, myNodeId);
            jsonObject.put(KEY_BUYERS_DETAILS_INFO, userList);
            jsonObject.put(KEY_MESSAGE_TYPE, BUYER_LIST);

            return jsonObject.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isAppMessage(int type) {
        if (type == APP_MESSAGE
                || type == ACK_MESSAGE
                || type == APP_FILE_MESSAGE
                || type == PAYMENT_ACK
                || type == PAYMENT_MESSAGE
                || type == APP_BROADCAST_MESSAGE
                || type == APP_BROADCAST_ACK_MESSAGE
                || type == BUYER_FILE_MESSAGE
                || type == VERSION_MESSAGE
                || type == HMAC_MESSAGE
                || type == INFO_HANDSHAKE) {
            return true;
        }
        return false;
    }


}
