package com.w3engineers.mesh.util;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.libmeshx.wifid.WiFiDevicesList;
import com.w3engineers.mesh.wifi.WiFiUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;

import static com.w3engineers.mesh.util.Constant.MASTER_IP_ADDRESS;

/**
 * Provides Wifi direct utility services
 */
public class P2PUtil {

    public static final int GO_PASSWORD_LENGTH = 8;
    public static final String GO_PREFIX = "DIRECT-";
    public static final String ENCRYPT_PASSWORD = "2cu}+yD)H]%A;AgQLY+@9T]_,Z";
    public static boolean hasItem(Collection<WifiP2pDevice> wifiP2pDevices) {
        return wifiP2pDevices != null && wifiP2pDevices.size() > 0;
    }

    public static boolean hasNoItem(Collection<WifiP2pDevice> wifiP2pDevices) {
        return !hasItem(wifiP2pDevices);
    }

    public static WiFiDevicesList getList(Collection<WifiP2pDevice> wifiP2pDevices) {
        if (hasItem(wifiP2pDevices)) {
            WiFiDevicesList p2pDevices = new WiFiDevicesList();
            p2pDevices.addAll(wifiP2pDevices);
            return p2pDevices;
        }

        return null;
    }

    public static String getLogString(Collection<WifiP2pDevice> wifiP2pDevices) {
        if (hasItem(wifiP2pDevices)) {
            StringBuilder log = new StringBuilder();
            for (WifiP2pDevice wifiP2pDevice : wifiP2pDevices) {
                log.append("-").append(wifiP2pDevice.deviceAddress);
            }
            return log.toString();
        }

        return null;
    }

    public static String getLocalP2PIpAddress() {
        // TODO: 8/9/2019 Use {@link NetworkInterface.getByName()} method for fastness and accuracy
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); (en != null && en.hasMoreElements()); ) {

                NetworkInterface intf = en.nextElement();
                if (intf != null && intf.getName() != null && intf.getName().startsWith("p2p")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); (enumIpAddr != null && enumIpAddr.hasMoreElements()); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean isMeLC() {
        String localp2pIpAddress = getLocalP2PIpAddress();
        return Text.isNotEmpty(localp2pIpAddress) && localp2pIpAddress.startsWith(
                Constant.P2P_IP_ADDRESS_PREFIX);
    }

    public static boolean isMeGO() {
        String localp2pIpAddress = getLocalP2PIpAddress();
        return MASTER_IP_ADDRESS.equals(localp2pIpAddress);
    } // getLocalIpAddress()

    /**
     * Is the connected SSID is WiFi direct or P2P SSID
     *
     * @param connectedSSID
     * @return
     */
    public static boolean isPotentialGO(String connectedSSID) {
        if (Text.isNotEmpty(connectedSSID)) {
            connectedSSID = connectedSSID.replaceAll("\"", "");
            return Text.isNotEmpty(connectedSSID) && connectedSSID.startsWith(GO_PREFIX);
        }

        return false;
    }


    /**
     * Is the connected SSID is WiFi direct or P2P SSID
     *
     * @param context
     * @return
     */
    public static boolean isConnectedWithPotentialGO(Context context) {
        if (context != null) {
            String connectedSSID = WiFiUtil.getConnectedSSID(context);
            if (Text.isNotEmpty(connectedSSID)) {
                connectedSSID = connectedSSID.replaceAll("\"", "");
                return Text.isNotEmpty(connectedSSID) && connectedSSID.startsWith(GO_PREFIX);
            }
        }


        return false;
    }

    public static String parseReasonCode(int reason) {
        if (reason < 3 && reason > -1) {
            switch (reason) {
                case WifiP2pManager.ERROR:
                    return "SYSTEM ERROR";
                case WifiP2pManager.P2P_UNSUPPORTED:
                    return "P2P UNSUPPORTED";
                case WifiP2pManager.BUSY:
                    return "SYSTEM BUSY";
            }

        }
        return "ERROR";

    }
}
