package com.w3engineers.mesh.wifidirect;


import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;


import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifidirect.listener.ConnectionInfoListener;
import com.w3engineers.mesh.wifidirect.receiver.WiFiDirectBroadcastReceiver;
import com.w3engineers.mesh.wifidirect.listener.WiFiDirectStatusListener;

import java.util.HashMap;
import java.util.Map;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * Singleton class acting as a "server" device.
 * <p>
 * With Wroup Library you can register a service in the current local network to be discovered by
 * other devices. When a service is registered a WiFi P2P Group is created, we know it as Wroup ;)
 * <p>
 * <code>WiFiDirectService</code> is the group owner and it manages the group changes (connections and
 * disconnections). When a new client is connected/disconnected the service device notify to the
 * other devices connected.
 * <p>
 * To register a service you must do the following:
 * <pre>
 * {@code
 *
 * wiFiP2PService = WiFiDirectService.getInstance(getApplicationContext());
 * wiFiP2PService.registerService(groupName, new ServiceRegisteredListener() {
 *
 *  public void onSuccessServiceRegistered() {
 *      Log.i(TAG, "Wroup created. Waiting for client connections...");
 *  }
 *
 *  public void onErrorServiceRegistered(WiFiP2PError wiFiP2PError) {
 *      Log.e(TAG, "Error creating group");
 *  }
 *
 * });
 * }
 * </pre>
 */
public class WiFiDirectService implements ConnectionInfoListener {


    private static final String TAG = "[p2p_process]";

    private static final String SERVICE_TYPE = "_wroup._tcp";
    private final int GO_ADVERTISE_INTERVAL = 40 * 1000;

    private WiFiP2PInstance wiFiP2PInstance;
    private WiFiDirectBroadcastReceiver broadcastReceiver;
    private Boolean groupAlreadyCreated = false;
    private Context mContext;
    private String myNodeId;
    private WiFiDirectStatusListener wiFiDirectStatusListener;
    private String mySsid;
    private String password;
    private volatile boolean isGoBroadcastingEnable;

    public WiFiDirectService(Context context, String userId, WiFiDirectStatusListener statusListener) {
        this.mContext = context;
        this.myNodeId = userId;
        this.wiFiDirectStatusListener = statusListener;

        wiFiP2PInstance = WiFiP2PInstance.getInstance(context);
        wiFiP2PInstance.setPeerConnectedListener(this);
        broadcastReceiver = wiFiP2PInstance.getBroadcastReceiver();
        //registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mContext.registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unregisterBroadcastReceiver(){
        try{
            mContext.unregisterReceiver(broadcastReceiver);
        }catch (IllegalArgumentException e){
            //e.printStackTrace();
        }

    }


    public void registerService() {
        registerBroadcastReceiver();
        removeAndCreateGroup();
    }

    private void removeAndCreateGroup() {
        wiFiP2PInstance.getWifiP2pManager().requestGroupInfo(wiFiP2PInstance.getChannel(), group -> {
            if (group != null) {
                wiFiP2PInstance.getWifiP2pManager().removeGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        groupAlreadyCreated = false;
                        // Now we can create the group
                        createGroup();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG, "Error deleting group");
                    }
                });
            } else {
                createGroup();
            }
        });
    }

    private void createGroup() {
        if (!groupAlreadyCreated) {
            wiFiP2PInstance.getWifiP2pManager().createGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    //Log.i(TAG, "Group created!");
                    groupAlreadyCreated = true;
                    //requestQroupInfoForAdvertising();
                }

                @Override
                public void onFailure(int reason) {
                    //Log.e(TAG, "Error creating group. Reason: " + WiFiP2PError.fromReason(reason));
                }
            });
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        //Log.i(TAG, "Im Go OnPeerConnected...");

        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            //Log.i(TAG, "I am the group owner");
            //Log.i(TAG, "My addess is: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
            requestGroupInfoForAdvertising();
        }
    }


    private void requestGroupInfoForAdvertising() {
        wiFiP2PInstance.getWifiP2pManager().requestGroupInfo(wiFiP2PInstance.getChannel(), group -> {
            if (group != null) {
                MeshLog.v("[p2p_process] Go created ssid : " + group.getNetworkName() + " pass: " + group.getPassphrase());
                mySsid = group.getNetworkName();
                password = group.getPassphrase();
                if(!isGoBroadcastingEnable) {
                    startServiceBroadcasting(group.getNetworkName(), group.getPassphrase(), "onGroupInfoAvailable");
                    wiFiDirectStatusListener.onGoCreated(group.getNetworkName(), group.getPassphrase());
                    HandlerUtil.postBackground(goScheduleBroadcast, GO_ADVERTISE_INTERVAL);
                }
                isGoBroadcastingEnable  = true;
            } else {
                int currentRole = SharedPref.readInt(Constant.KEY_RUNNING_ROLE);
                if(currentRole == CurrentRole.SPECIAL_GO){
                    requestGroupInfoForAdvertising();
                }
                //Log.e(TAG, "No ssid name SSID null.......... : Role: "+currentRole);
            }
        });
    }


    private Runnable goScheduleBroadcast = new Runnable() {
        @Override
        public void run() {
            if (isGoBroadcastingEnable && mySsid != null) {
                startServiceBroadcasting(mySsid, password, "goScheduleBroadcast");
                HandlerUtil.postBackground(goScheduleBroadcast, GO_ADVERTISE_INTERVAL);
            } else {
                MeshLog.e("[p2p_process] broadcast off or ssid null");
            }
        }
    };


    public void requestGOScheduledBroadCast() {
        MeshLog.e("[p2p_process] start Go schedule broadcast");
        isGoBroadcastingEnable = true;
        HandlerUtil.postBackground(goScheduleBroadcast, 0);
    }


    private boolean startServiceBroadcasting(String ssId, String password, String from) {
        Map<String, String> record = new HashMap<>();
        record.put(CurrentRole.Keys.SSID, ssId);
        record.put(CurrentRole.Keys.PASSWORD, password);
        record.put(CurrentRole.Keys.ETH_ID, this.myNodeId);
        record.put(CurrentRole.Keys.PREFIX, SharedPref.read(Constant.KEY_NETWORK_PREFIX));


        MeshLog.i(TAG + " Advertise local service triggered........"+from);

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("mesh", SERVICE_TYPE, record);
        wiFiP2PInstance.getWifiP2pManager().addLocalService(wiFiP2PInstance.getChannel(), serviceInfo, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service registered success  +++++");

            }

            @Override
            public void onFailure(int reason) {
                MeshLog.v(TAG + " ");
            }

        });

        return false;
    }

    public void stopGoAllEvent() {
        mySsid = null;
        password = null;
        isGoBroadcastingEnable = false;
        unregisterBroadcastReceiver();
        clearLocalServices();
        removeGroup();
        //stopPeerDiscovering();
        HandlerUtil.removeBackground(goScheduleBroadcast);
    }


    public void clearLocalServices() {
        HandlerUtil.removeBackground(goScheduleBroadcast);
        isGoBroadcastingEnable = false;
        unregisterBroadcastReceiver();
        wiFiP2PInstance.getWifiP2pManager().clearLocalServices(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //MeshLog.v("[p2p_process]  clear service success");
            }

            @Override
            public void onFailure(int reason) {
                //MeshLog.v("[p2p_process]  clear service failed: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

    private void removeGroup() {
        wiFiP2PInstance.getWifiP2pManager().requestGroupInfo(wiFiP2PInstance.getChannel(), new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                wiFiP2PInstance.getWifiP2pManager().removeGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        groupAlreadyCreated = false;
                        //MeshLog.v("[p2p_process]  remove group success");
                    }

                    @Override
                    public void onFailure(int reason) {
                        //MeshLog.v("[p2p_process]  remove group failed: " + WiFiP2PError.fromReason(reason));
                    }
                });
            }
        });
    }

    private void stopPeerDiscovering() {
        wiFiP2PInstance.getWifiP2pManager().stopPeerDiscovery(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //Log.d(TAG, "Peer discovering stopped");
            }

            @Override
            public void onFailure(int reason) {
                //Log.e(TAG, "Error stopping peer discovering: " + WiFiP2PError.fromReason(reason));
            }
        });
    }

}
