package com.w3engineers.mesh.wifiap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.bluetooth.APListener;
import com.w3engineers.mesh.bluetooth.BTManager;
import com.w3engineers.mesh.bluetooth.BluetoothTransport;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bluetooth name related update manages here
 */
public class BluetoothDiscoveryReceiver extends BroadcastReceiver {

    //    private final int MAXIMUM_NODE_ACCUMULATION_CEILING = 30;
    private APListener mAPListener;
    private final int MAXIMUM_NODE_ACCUMULATION_CEILING = 1;
    private final long DISCOVERY_NODE_CACHING_TIME = 0;
    //    private final long DISCOVERY_NODE_CACHING_TIME = 2 * 1000;
    private Map<String, BluetoothDevice> mBluetoothDeviceMap;
    private BTDiscoveryListener mBTDiscoveryListener;
    private Runnable postDeviceList = () -> {
        if (mBTDiscoveryListener != null) {
            if (mBluetoothDeviceMap.size() > 0) {
                mBTDiscoveryListener.onBluetoothFound(new ArrayList<>(mBluetoothDeviceMap.values()));
                mBluetoothDeviceMap.clear();
            }
        }
    };

    public interface BTDiscoveryListener {
        void onBluetoothFound(List<BluetoothDevice> bluetoothDevices);

        void onScanFinished();
    }

    public BluetoothDiscoveryReceiver(BTDiscoveryListener BTDiscoveryListener) {
        this.mBTDiscoveryListener = BTDiscoveryListener;
        mBluetoothDeviceMap = new HashMap<>();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null || action.isEmpty()) return;

        switch (action) {
            case BluetoothDevice.ACTION_FOUND:

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                boolean isAccepted = false;
//                boolean isToAcceptBySmartPhoneType = mBTDiscoveryListener != null && //Listener enabled
//                        (device.getBluetoothClass() != null &&//BT is phone
//                                device.getBluetoothClass().getDeviceClass() ==
//                                        BluetoothClass.Device.PHONE_SMART) &&
//                        //BT not in secondary name  or connected with any mesh BT
//                        !BluetoothTransport.SECONDARY_BT_NAME.equals(name);

                String btName = device.getName();
                MeshLog.e("BT-Classic. Search name found: " + btName);
                String NETWORK_SSID = SharedPref.read(Constant.NETWORK_SSID);
                boolean isToAcceptByName = mBTDiscoveryListener != null && //Listener enabled
                        Text.isNotEmpty(btName) && btName.startsWith(NETWORK_SSID);

                String searchingId = BTManager.getInstance().getSearchingId();

                //MeshLog.e("BT-Classic. Special search id: " + searchingId + " isToAcceptByName " + isToAcceptByName);

                if (Text.isNotEmpty(btName) && !TextUtils.isEmpty(searchingId)) {
                    isToAcceptByName = btName.contains(AddressUtil.makeShortAddress(searchingId));
                    MeshLog.i("BT-classic we got the desire BT node: " + isToAcceptByName);
                }


                if (isToAcceptByName) {

                    //Turn off if we search by name, for type based search we will turn off only
                    // before posting list to upwards
                    BTManager.getInstance(context).cancelDiscovery();
                    mBluetoothDeviceMap.put(device.getAddress(), device);

                    //Continue caching the list for DISCOVERY_NODE_CACHING_TIME seconds. If no
                    // device received within this time then we push this list to upwards.
//                    if(mBluetoothDeviceMap.size() < MAXIMUM_NODE_ACCUMULATION_CEILING) {
//                        isAccepted = true;
//                        AndroidUtil.postBackground(postDeviceList, DISCOVERY_NODE_CACHING_TIME);
//                    }
                    AndroidUtil.postBackground(postDeviceList, DISCOVERY_NODE_CACHING_TIME);
                    //We do cache because before connection we cancel discovery which in turns
                    // deprive us from getting all the devices.

                    processCredentials(btName);
                } else {
                    isToAcceptByName = mBTDiscoveryListener != null && //Listener enabled
                            Text.isNotEmpty(btName) && btName.startsWith(BluetoothTransport.
                            SECONDARY_BT_NAME);
                    if (isToAcceptByName) {
                        processCredentials(btName);
                    }
                }
//                MeshLog.v("[BT]Found:"+name+"-accepted:"+isToAcceptByName);

                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                if (mBTDiscoveryListener != null) {
                    if (mBluetoothDeviceMap != null && mBluetoothDeviceMap.size() > 0) {
                        AndroidUtil.postBackground(postDeviceList, 0);
                    } else {
                        mBTDiscoveryListener.onScanFinished();
                    }
                }
                break;
        }
    }

    private void processCredentials(String btName) {

        // we don't need this credential now

       /* if (Text.isNotEmpty(btName)) {
            String[] credentials = btName.split(BTManager.SEPARATOR);
            if (CollectionUtil.hasItem(credentials) && credentials.length > 2) {
                String ssid = credentials[1], preSharedKey = credentials[2];
                MeshLog.v("[highband] found AP credentials over BT:" + ssid + ":" + preSharedKey);
                if (mAPListener != null) {
                    mAPListener.onAPAvailable(ssid, preSharedKey);
                }
            }
        }*/
    }

    public void setAPListener(APListener APListener) {
        mAPListener = APListener;
    }
}
