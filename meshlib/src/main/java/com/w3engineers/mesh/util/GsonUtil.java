package com.w3engineers.mesh.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.linkcash.OnlineNode;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.DisconnectionModel;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.util.gson.HiddenAnnotationExclusionStrategy;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides GSON utility
 */
public class GsonUtil {
    private static GsonUtil gsonUtil;
    private Gson gson;

    private GsonUtil() {
        gson = new Gson();
    }

    public static GsonUtil on() {
        if (gsonUtil == null) {
            gsonUtil = new GsonUtil();
        }
        return gsonUtil;
    }

/*
    public <T> String toJsonFromList(List<T> itemList) {
        Type type = new TypeToken<List<T>>() {
        }.getType();
        try {
            return gson.toJsonFromList(itemList, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> List<T> listFromJson(String jsonStr, Type desiredType) {
        try {
            return gson.listFromJson(jsonStr, desiredType);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }*/

    public String toJsonFromMeshOnlineQueue(ConcurrentLinkedQueue<String> itemQueue) {
        if (CollectionUtil.hasItem(itemQueue)) {
            Type type = new TypeToken<ConcurrentLinkedQueue<String>>() {
            }.getType();
            try {
                return gson.toJson(itemQueue, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public ConcurrentLinkedQueue<String> meshOnlineQueueFromJson(String jsonStr) {
        Type type = new TypeToken<ConcurrentLinkedQueue<String>>() {
        }.getType();
        try {
            return gson.fromJson(jsonStr, type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String toFromList(List<OnlineNode> itemList) {
        if (itemList == null || itemList.isEmpty()) return null;
        Type type = new TypeToken<List<OnlineNode>>() {
        }.getType();
        try {
            return gson.toJson(itemList, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<OnlineNode> toListFromJson(String jsonStr) {
        Type type = new TypeToken<List<OnlineNode>>() {
        }.getType();
        try {
            return gson.fromJson(jsonStr, type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public String toJsonFromList(List<NodeInfo> itemList) {
        if (itemList == null || itemList.isEmpty()) return null;
        Type type = new TypeToken<List<NodeInfo>>() {
        }.getType();
        try {
            return gson.toJson(itemList, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toJsonFromQueue(ConcurrentLinkedQueue<NodeInfo> itemQueue) {
        if (CollectionUtil.hasItem(itemQueue)) {
            Type type = new TypeToken<ConcurrentLinkedQueue<NodeInfo>>() {
            }.getType();
            try {
                return gson.toJson(itemQueue, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public ConcurrentLinkedQueue<NodeInfo> queueFromJson(String jsonStr) {
        Type type = new TypeToken<ConcurrentLinkedQueue<NodeInfo>>() {
        }.getType();
        try {
            return gson.fromJson(jsonStr, type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String toJsonFromItem(NodeInfo nodeInfo) {

        try {
            return gson.toJson(nodeInfo, NodeInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public NodeInfo itemFromJson(String jsonStr) {
        try {
            return gson.fromJson(jsonStr, NodeInfo.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getUserInfo() {
        return SharedPref.read(Constant.MY_INFO_PREF_KEY);
    }

    public static void setUserInfo(String userInfo) {
        SharedPref.write(Constant.MY_INFO_PREF_KEY, userInfo);
    }


    public RoutingEntity getEntityFromJson(String jsonString) {
        Type type = new TypeToken<RoutingEntity>() {
        }.getType();

        try {
            return gson.fromJson(jsonString, type);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public ConcurrentLinkedQueue<RoutingEntity> getEntityQueue(String jsonStr) {
        Type type = new TypeToken<ConcurrentLinkedQueue<RoutingEntity>>() {
        }.getType();
        try {
            return gson.fromJson(jsonStr, type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String toJsonFromItem(RoutingEntity nodeInfo) {

        try {
            return gson.toJson(nodeInfo, RoutingEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toJsonFromEntityList(List<RoutingEntity> itemList) {
        if (itemList == null || itemList.isEmpty()) return null;
        Type type = new TypeToken<List<RoutingEntity>>() {
        }.getType();
        try {
            return gson.toJson(itemList, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toJsonFromEntityList(ConcurrentLinkedQueue<RoutingEntity> itemList) {
        if (itemList == null || itemList.isEmpty()) return null;
        Type type = new TypeToken<List<RoutingEntity>>() {
        }.getType();
        try {
            return gson.toJson(itemList, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toJsonFromDisconnectionList(List<DisconnectionModel> itemList) {
        if (itemList == null || itemList.isEmpty()) return null;
        Type type = new TypeToken<List<DisconnectionModel>>() {
        }.getType();
        try {
            return gson.toJson(itemList, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<DisconnectionModel> getDisconnectedNodeList(String jsonStr) {
        Type type = new TypeToken<List<DisconnectionModel>>() {
        }.getType();
        try {
            return gson.fromJson(jsonStr, type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String broadcastToString(Broadcast broadcast) {
        try {
            return gson.toJson(broadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Broadcast broadcastFromString(String userInfoData) {
        Type type = new TypeToken<Broadcast>() {}.getType();
        try {
            return gson.fromJson(userInfoData, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String broadcastAckToString(BroadcastAck broadcastAck) {
        try {
            return gson.toJson(broadcastAck);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BroadcastAck broadcastAckFromString(String userInfoData) {
        Type type = new TypeToken<BroadcastAck>() {}.getType();
        try {
            return gson.fromJson(userInfoData, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String handshakeInfoToString(HandshakeInfo handshakeInfo) {
        try {
            return gson.toJson(handshakeInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public HandshakeInfo handshakeInfoFromString(String userInfoData) {
        Type type = new TypeToken<HandshakeInfo>() {}.getType();

        try {
            return gson.fromJson(userInfoData, type);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}

