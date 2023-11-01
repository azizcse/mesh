package com.w3engineers.mesh.queue.messages;

import com.w3engineers.mesh.util.JsonDataBuilder;

/**
 * Provides Message utility
 */
public class MessageUtil {

    public static boolean isDiscoveryMessage(int type) {
        return type == JsonDataBuilder.BT_HELLO ||
                isDirectDiscoveryMessage(type) || isAdhocDiscoveryMessage(type);
    }


    /**
     * To check a message type is WiFi direct discovery or not
     *
     * @param type
     * @return
     */
    public static boolean isDirectDiscoveryMessage(int type) {
        return type == JsonDataBuilder.P2P_HELLO_CLIENT
                || type == JsonDataBuilder.P2P_HELLO_MASTER
                || type == JsonDataBuilder.MESH_USER
                || type == JsonDataBuilder.HELLO_MASTER
                || type == JsonDataBuilder.HELLO_CLIENT
                || type == JsonDataBuilder.NODE_RESPONSE
                || type == JsonDataBuilder.MESH_NODE
                || type == JsonDataBuilder.NODE_LEAVE
                || type == JsonDataBuilder.DISCONNECT_REQUEST
                || type == JsonDataBuilder.CREDENTIAL_MESSAGE;
    }

    // FIXME: 1/16/2020 we should separate MESH_USER for adhoc and WiFi direct. Otherwise interface
    //postponing might impact performance. It will ease decisions

    /**
     * To check a message type is Adhoc discovery or not
     *
     * @param type
     * @return
     */
    public static boolean isAdhocDiscoveryMessage(int type) {
        return type == JsonDataBuilder.ADHOC_HELLO_CLIENT
                || type == JsonDataBuilder.ADHOC_HELLO_MASTER
                || type == JsonDataBuilder.MESH_USER;
    }

}
