package com.w3engineers.mesh.libmeshx.wifid;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;

import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

/**
 * Soft Access point searcher
 */
public class SoftAccessPointSearcher extends P2PServiceSearcher {

    public interface ServiceFound {
        void onServiceFoundSuccess(String ssid, String passPhrase, String searchMeta, WifiP2pDevice wifiP2pDevice);
        void onP2pAlreadyConnected(String ssid);
    }

    private ServiceFound mServiceFound;

    public void setServiceFound(ServiceFound serviceFound) {
        mServiceFound = serviceFound;
    }

    public SoftAccessPointSearcher(Context context) {
        super(context);
        mServiceType = SharedPref.read(Constant.KEY_NETWORK_PREFIX);
    }

    @Override
    protected void onDesiredServiceFound(String ssid, String passPhrase, String searchMeta, WifiP2pDevice wifiP2pDevice) {
        MeshLog.i("[WDC]onDesiredServiceFound:"+ssid+":"+mIsAlive+":"+mServiceFound);
        if(mServiceFound != null && mIsAlive) {
            mServiceFound.onServiceFoundSuccess(ssid, passPhrase, searchMeta, wifiP2pDevice);
        }
    }

    @Override
    protected void onP2pAlreadyConnected(String ssid){
        if(mServiceFound != null ) {
            mServiceFound.onP2pAlreadyConnected(ssid);
        }
    }
}
