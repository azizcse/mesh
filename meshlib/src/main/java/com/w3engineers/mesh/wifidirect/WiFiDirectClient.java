package com.w3engineers.mesh.wifidirect;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.util.Log;


import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.libmeshx.wifid.APCredential;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifidirect.listener.ConnectionInfoListener;
import com.w3engineers.mesh.wifidirect.listener.ServiceDisconnectedListener;
import com.w3engineers.mesh.wifidirect.listener.WiFiDirectStatusListener;

import java.util.Map;
import java.util.Random;

/**
 * <h1>Wifi direct client LC (Legacy client)
 *     LC search GO(Group owner) to connect. its periodically switch
 *     and trigger GO search. GO advertise some group related info
 *
 * </h1>
 *
 *
 *
 */
public class WiFiDirectClient implements ConnectionInfoListener, ServiceDisconnectedListener {

    private static final String TAG = "[p2p_process]";
    private DnsSdTxtRecordListener dnsSdTxtRecordListener;
    private DnsSdServiceResponseListener dnsSdServiceResponseListener;
    public static final int SCAN_SLOT_INTERNAL_MIN = 10;
    public static final int SCAN_SLOT_INTERNAL_MAX = SCAN_SLOT_INTERNAL_MIN + 5;

    private WiFiP2PInstance wiFiP2PInstance;
    private WiFiDirectStatusListener wiFiDirectStatusListener;
    private volatile boolean isGoSearchRunning;

    private String specialSearchID;
    private volatile boolean isSpecialSearch;
    private String myNodeId;

    public WiFiDirectClient(Context context, WiFiDirectStatusListener statusListener, String myUserId) {
        this.wiFiDirectStatusListener = statusListener;
        wiFiP2PInstance = WiFiP2PInstance.getInstance(context);
        wiFiP2PInstance.setServerDisconnectedListener(this);
        this.myNodeId = myUserId;
    }

    public void initializeServicesDiscovery() {
        // We need to start discovering peers to activate the service search
        wiFiP2PInstance.startPeerDiscovering();
        long delay = getSearchDelay() * 1000;
        searchGO("initializeServicesDiscovery with time: " + delay);
        HandlerUtil.postBackground(scheduleGoSearch, delay);
    }

    public void stopServiceDiscovery() {
        HandlerUtil.removeBackground(scheduleGoSearch);
        isGoSearchRunning = false;
        dnsSdTxtRecordListener = null;
        dnsSdServiceResponseListener = null;
        wiFiP2PInstance.getWifiP2pManager().setDnsSdResponseListeners(wiFiP2PInstance.getChannel(),null, null);

        wiFiP2PInstance.getWifiP2pManager().clearServiceRequests(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                //MeshLog.v("[p2p_process] clearServiceRequests success");
            }

            public void onFailure(int reason) {
                //MeshLog.v("[p2p_process] clearServiceRequests failed: " + WiFiP2PError.fromReason(reason));
            }
        });
        wiFiP2PInstance.stopPeerDiscovery();
    }

    public void searchGO(String from) {
        MeshLog.e("[p2p_process] Go search start from : " + from);
        isGoSearchRunning = true;
        wiFiP2PInstance.startPeerDiscovering();
        setupDnsListeners();
        triggeredGoDiscovery();
    }


    private void triggeredGoDiscovery() {
        wiFiP2PInstance.getWifiP2pManager().clearServiceRequests(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //MeshLog.i(TAG + " clearServiceRequests success");
                WifiP2pServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
                wiFiP2PInstance.getWifiP2pManager().addServiceRequest(wiFiP2PInstance.getChannel(), serviceRequest, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        //MeshLog.v("[p2p_process] addServiceRequest success");
                        wiFiP2PInstance.getWifiP2pManager().discoverServices(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                //MeshLog.v("[p2p_process] discoverServices success");
                            }

                            @Override
                            public void onFailure(int reason) {
                                WiFiP2PError wiFiP2PError = WiFiP2PError.fromReason(reason);
                                if (wiFiP2PError != null) {
                                    MeshLog.e(TAG + " + discoverServices failed: " + WiFiP2PError.fromReason(reason));

                                }
                            }

                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        MeshLog.v("[p2p_process] addServiceRequest failed: " + WiFiP2PError.fromReason(reason));
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                MeshLog.v("[p2p_process] clearServiceRequests failed : " + WiFiP2PError.fromReason(reason));
            }
        });
    }

    private void setupDnsListeners() {
        if (dnsSdTxtRecordListener == null || dnsSdServiceResponseListener == null) {
            //MeshLog.i(TAG + " Service discovery listener initialized");
            dnsSdTxtRecordListener = getTxtRecordListener();
            dnsSdServiceResponseListener = getServiceResponseListener();

            wiFiP2PInstance.getWifiP2pManager().setDnsSdResponseListeners(wiFiP2PInstance.getChannel(),
                    dnsSdServiceResponseListener, dnsSdTxtRecordListener);
        }
    }

    private DnsSdTxtRecordListener getTxtRecordListener() {
        return new DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice device) {
                String myPrefix = SharedPref.read(Constant.KEY_NETWORK_PREFIX);

                if (txtRecordMap.containsKey(CurrentRole.Keys.PREFIX) && !myPrefix.equals(txtRecordMap.get(CurrentRole.Keys.PREFIX))) {
                    MeshLog.e("****** Not my service *********");
                    return;
                }
                if (txtRecordMap.containsKey(CurrentRole.Keys.PASSWORD)
                        && txtRecordMap.containsKey(CurrentRole.Keys.SSID)
                        && txtRecordMap.containsKey(CurrentRole.Keys.ETH_ID)) {


                    String ssid = txtRecordMap.get(CurrentRole.Keys.SSID);
                    String password = txtRecordMap.get(CurrentRole.Keys.PASSWORD);
                    String ethId = txtRecordMap.get(CurrentRole.Keys.ETH_ID);

                    if (isSpecialSearch && !specialSearchID.equals(ethId)) {
                        MeshLog.e("**** Waiting for special GO **** ");
                        return;
                    }

                    if(ethId.equals(myNodeId)){
                        MeshLog.e("**** Self GO discovered **** ");
                        wiFiDirectStatusListener.onConnectedWithSelfGo();
                        return;
                    }
                    MeshLog.v("[p2p_process] GO found ssid :" + ssid + " pass: " + password);
                    APCredential credential = new APCredential(ssid,password,ethId, device.deviceAddress,isSpecialSearch);
                    wiFiDirectStatusListener.onGoFound(credential);
                }
            }
        };
    }

    private DnsSdServiceResponseListener getServiceResponseListener() {
        return new DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                //MeshLog.i(TAG + " Discovered service...... success Type: " + registrationType);
            }
        };
    }


    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

    }

    @Override
    public void onServerDisconnectedListener() {

    }


    private Runnable scheduleGoSearch = new Runnable() {
        @Override
        public void run() {
            if (isGoSearchRunning) {
                long delay = getSearchDelay();
                searchGO("scheduleGoSearch time: " + delay);
                HandlerUtil.postBackground(this::run, delay * 1000);
            }
        }
    };


    private long getSearchDelay() {
        int randValue = new Random().nextInt((SCAN_SLOT_INTERNAL_MAX + 1) -
                SCAN_SLOT_INTERNAL_MIN) + SCAN_SLOT_INTERNAL_MIN;
        return randValue;
    }


    public void startSpecialSearch(String ethId) {
        if (ethId == null) {
            isSpecialSearch = false;
        } else {
            this.specialSearchID = ethId;
            isSpecialSearch = true;
        }
    }

    public String getSpecialSearchNodeId() {
        return this.specialSearchID;
    }
}
