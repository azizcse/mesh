package com.w3engineers.mesh.libmeshx.wifid;

import android.net.wifi.p2p.WifiP2pDevice;
import android.text.TextUtils;

import com.w3engineers.mesh.util.P2PUtil;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides Connected WifiDevices details
 */
public class WiFiDevicesList extends ConcurrentLinkedQueue<WifiP2pDevice> {

    private static Object lock = new Object();

    public WiFiDevicesList() {
        super();
    }

    public WiFiDevicesList(Collection<? extends WifiP2pDevice> c) {
        super(c);
    }

    @Override
    public boolean addAll(Collection<? extends WifiP2pDevice> wifiP2pDevices) {

        boolean isAdded = false;
        for( WifiP2pDevice wifiP2pDevice : wifiP2pDevices) {
            if(contains(wifiP2pDevice)) {
               continue;
            }

            isAdded = add(wifiP2pDevice);
        }

        return isAdded;
    }

    public synchronized boolean substract(WiFiDevicesList wifiP2pDevices) {
        if(!P2PUtil.hasItem(wifiP2pDevices))
            return false;

        synchronized (lock) {
        for(WifiP2pDevice wifiP2pDevice : this) {
            if(wifiP2pDevices.contains(wifiP2pDevice)) {
                    remove(wifiP2pDevice);
                }
            }
        }

        return true;
    }

    @Override
    public boolean contains(Object o) {
        if(o == null)
            return false;

        WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) o;
        String address = wifiP2pDevice.deviceAddress;
        if(TextUtils.isEmpty(address)) {
            return false;
        }

        for(WifiP2pDevice wifiP2pDevice1 : this) {
            if(address.equals(wifiP2pDevice1.deviceAddress)) {
                return true;
            }
        }

        return false;
    }

    public WiFiDevicesList copy() {
        return new WiFiDevicesList(this);
    }
}
