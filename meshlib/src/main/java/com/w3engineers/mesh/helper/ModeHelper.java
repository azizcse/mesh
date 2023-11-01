package com.w3engineers.mesh.helper;

import com.w3engineers.ext.strom.App;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.P2PUtil;
import com.w3engineers.mesh.wifi.WiFiUtil;

/**
 * Helper utility for connection mode
 */
public class ModeHelper {

    //Mode details:
    //https://docs.google.com/spreadsheets/d/1oepOPWVRliPjTIC0zo6D2vxMidEEXGXqazMzOY4pPuc/edit#gid=0

    /**
     * @return next potential role {@link RoutingEntity} type
     */
    public int[] getNextHighBandMode() {

        boolean isBtUserConnected = RouteManager.getInstance().isBtUserConnected();
        boolean isWiFiUserConnected = RouteManager.getInstance().isWifiUserConnected();
        boolean isHybridUserConnected = RouteManager.getInstance().isAdhocUserConneted();
        boolean isHSConnected = RouteManager.getInstance().isConnectedIn(RoutingEntity.Type.HS);
        boolean isClientConnected = RouteManager.getInstance().isConnectedIn(
                RoutingEntity.Type.CLIENT);

        //For now checking only WiFi hotspot enabled or not, expecting to check only by DB
        boolean isHSEnabled = WiFiUtil.isHotSpotEnabled();
        //For now checking only WiFi connected or not, expecting to check only by DB
        boolean isClientEnabled = WiFiUtil.isWifiConnected(App.getContext());

        int[] potentialRoles = null;
        if(isBtUserConnected && !isWiFiUserConnected && !isHybridUserConnected && !isHSConnected &&
                !isClientConnected) {
            //Active mode: BT
            //Optional mode: HS, C, LC, GO,

            //As the node is connected though BT, so our regular role scheduler would role over P2P
            //interface and hence we are trying to switch towards extendable role early
            potentialRoles = new int[]{RoutingEntity.Type.HS, RoutingEntity.Type.CLIENT,
                    RoutingEntity.Type.GO, RoutingEntity.Type.WiFi};


        } else if(!isBtUserConnected && isWiFiUserConnected && !isHybridUserConnected && !isHSConnected &&
                !isClientConnected) {//Only GO or LC connected

            if(P2PUtil.isMeGO()) {
                //Active mode: GO
                //Optional mode: BT, C, HB

                //As it is GO, so next potential option might be a optimistic client or if connected
                // in Adhoc then as HB mode
                potentialRoles = new int[]{RoutingEntity.Type.CLIENT, RoutingEntity.Type.HB};

            } else {
                //Active mode: LC
                //Optional mode: BT

                //No counter measurement taken in terms of HighBandWidth

            }

        } else if(isBtUserConnected && isWiFiUserConnected && !isHybridUserConnected && !isHSConnected &&
                !isClientConnected) {

            if(P2PUtil.isMeGO()) {
                //Active mode: BT, GO
                //Optional mode: C, HB

                //As it is GO, so next potential option might be a optimistic client or if connected
                // in Adhoc then as HB mode
                potentialRoles = new int[]{RoutingEntity.Type.CLIENT, RoutingEntity.Type.HB};

            } else {
                //Active mode: BT, LC
                //Optional mode: NONE

                //No counter measurement required as no further option available

            }
        }

        return potentialRoles;
    }

}
