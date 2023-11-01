package com.w3engineers.mesh.wifidirect;

/**
 * Created by Azizul Islam on 12/9/20.
 *
 * <h1>
 * <p>
 * Autonomous connectivity role
 * GO(Group Owner), LC(Legacy Client)
 * SPECIAL_LC      : kind of LC , Search time is larger then general LC
 * SPECIAL_GO      : kind of GO , Advertise time is longer then general GO
 * WIFI_CONNECTING : special state of autonomous algorithm. Try to connect with GO
 * IDEAL           : Autonomous algorithm ideal state. when a node connect any node via wifi , Autonomous algorithm entire
 * ideal state.
 *
 * </h1>
 */
public interface CurrentRole {
    int GO = 1;
    int LC = 2;
    int SPECIAL_GO = 3;
    int SPECIAL_LC = 4;
    int WIFI_CONNECTING = 5;
    int BLE_CONNECTING = 6;
    int ADHOC_CONNECTION = 7;
    int INTERNET = 8;
    int LOCAL_HOTSPOT = 9;
    int IDEAL = 10;

    /**
     * <p>
     *     Key for advertise GO(Group Owner) info
     *     
     * </p>
     */
    interface Keys {
        String PASSWORD = "pass";
        String SSID = "ssid";
        String ETH_ID = "eth";
        String PREFIX = "prefix";
    }
}
