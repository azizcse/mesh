package com.w3engineers.mesh.libmeshx.wifid;

import androidx.annotation.NonNull;

import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;

import java.util.List;

/**
 * Provides P2P configuration information
 */
public class WiFiMeshConfig {

    public boolean mIsGroupOwner, mIsClient;
    public List<String> mSearchList;

    /**
     * Stores the service name to broadcast during Adhoc service discovery
     */
    public String mServiceName = SharedPref.read(Constant.KEY_USER_NAME);
    public String mDeviceBtName = SharedPref.read(Constant.KEY_DEVICE_BLE_NAME);


    /**
     * Configure whether our system should be greedy to connect to desired network.
     */
    public boolean mIsForceFulReconnectionAllowed = true;

    @NonNull
    @Override
    public String toString() {
        return "GO:"+mIsGroupOwner+"-LC:"+mIsClient;
    }
}
