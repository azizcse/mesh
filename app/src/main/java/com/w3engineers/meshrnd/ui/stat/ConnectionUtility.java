package com.w3engineers.meshrnd.ui.stat;

import com.w3engineers.mesh.db.routing.RoutingEntity;

public class ConnectionUtility {
    public static String getConnectionType(int type){
        if (type == RoutingEntity.Type.WiFi) {
            return "WiFi";
        } else if (type == RoutingEntity.Type.BT) {
            return "BT";
        } else if (type == RoutingEntity.Type.WifiMesh) {
            return "WM";
        } else if (type == RoutingEntity.Type.BtMesh) {
            return "BTM";
        } else if (type == RoutingEntity.Type.INTERNET) {
            return "Internet";
        } else if (type == RoutingEntity.Type.HB) {
            return "HB";
        } else if (type == RoutingEntity.Type.HB_MESH) {
            return "HBM";
        } else if (type == RoutingEntity.Type.BLE) {
            return "BLE";
        } else if (type == RoutingEntity.Type.BLE_MESH) {
            return "BM";
        } else {
            return "NA";
        }
    }
}
