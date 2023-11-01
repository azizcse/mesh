package com.w3engineers.mesh.wifi.dispatch;

import android.net.wifi.p2p.WifiP2pDevice;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.libmeshx.wifid.WiFiDevicesList;
import com.w3engineers.mesh.util.AndroidUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ARP table reader class
 */
public class ArpReader {
    private final long ARP_SCHEDULER_DELAY = 20 * 1000;
    public interface Listener{
        void onListPrepared(List<String> ipList);
        default void onMapPrepared(Map<String, String> macIpMap) {}
    }
    private Map<String, String> mMacIpMap;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            readArpTable(null);
            AndroidUtil.postBackground(mRunnable, ARP_SCHEDULER_DELAY);
        }
    };

    public ArpReader(){
        mMacIpMap = new HashMap<>();
//        AndroidUtil.postBackground(mRunnable, 0);
    }

    public void readArpTable(Listener listener){
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            List<String> ipList = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        ipList.add(ip);
                        mMacIpMap.put(mac, ip);
                    }
                }

            }

            if(listener != null) {
                listener.onListPrepared(ipList);
                listener.onMapPrepared(mMacIpMap);
            }

        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    public String getMacFromIp(String ip) {

        if(Text.isNotEmpty(ip) && mMacIpMap != null && mMacIpMap.size() > 0) {

            return mMacIpMap.get(ip);
        }

        return null;
    }

    public String getIpFromMac(String mac) {

        if(Text.isNotEmpty(mac) && mMacIpMap != null && mMacIpMap.size() > 0) {

            for (Map.Entry<String, String> entry : mMacIpMap.entrySet()) {
                if(mac.equals(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public List<String> getIpFromMac(WiFiDevicesList wifiP2pDevices) {

        if(CollectionUtil.hasItem(wifiP2pDevices)) {


            List<String> ipList = new ArrayList<>();
            String ip;
            for (WifiP2pDevice wifiP2pDevice : wifiP2pDevices) {

                if(wifiP2pDevice != null && Text.isNotEmpty(wifiP2pDevice.deviceAddress)) {
                    ip = mMacIpMap.get(wifiP2pDevice.deviceAddress);
                    if(Text.isNotEmpty(ip)) {
                        ipList.add(ip);
                    }
                }
            }

            return ipList;
        }

        return null;
    }
}
