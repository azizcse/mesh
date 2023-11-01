package com.w3engineers.mesh.libmeshx.wifid;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.text.TextUtils;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import timber.log.Timber;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * Discovers peers and provided service type. If found then call {@link #onDesiredServiceFound(String, String, String, WifiP2pDevice)}
 */
public abstract class P2PServiceSearcher implements WifiP2pManager.ChannelListener {

    public static final int SCAN_SLOT_INTERNAL_MIN = 25;
    public static final int SCAN_SLOT_INTERNAL_MAX = SCAN_SLOT_INTERNAL_MIN + 15;
    public static final int SEARCHER_RESTART_INTERVAL = 1;

    // FIXME: 10/2/2019 Sync approach decrease the possibility of searching service for long run,
    //  will add dynamic time based logic with {@link P2PStateListener#onP2PPeersStateChange}
    private enum ServiceState {
        NONE,
        RequestingDiscoverPeer,
        DiscoverPeer,
        RequestingDiscoverService,
        DiscoverService
    }

    protected volatile boolean mIsPreparingSearching;
    protected volatile boolean mIsAlive;
    private Context mContext;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private PeerReceiver mPeerReceiver;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.DnsSdServiceResponseListener mDnsSdServiceResponseListener;
    private volatile ServiceState mServiceState = ServiceState.NONE;
    protected volatile boolean mIsPauseConnectivity;
    /**
     * set type of service we are looking for
     */
    String mServiceType = SharedPref.read(Constant.KEY_NETWORK_PREFIX);
    private volatile Set<String> mBlockedList;
    private Queue<String> mSearchingQueue;
    protected Runnable mSearcherScheduler = this::restart;

    private WifiP2pManager.ActionListener mActionListenerStart = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            updateServiceStateWithoutCondition(ServiceState.NONE);
            MeshLog.i("[WDC]stopPeerDiscovery call from start");
            startPeerDiscovery();
        }

        @Override
        public void onFailure(int reason) {
            MeshLog.e("[WDC]stopPeerDiscovery call from start. failed: "+P2PUtil.parseReasonCode(reason));
        }
    };

    private WifiP2pManager.ActionListener mActionListenerStop = new WifiP2pManager.ActionListener() {
        public void onSuccess() {
            updateServiceStateWithoutCondition(ServiceState.NONE);
            Timber.d("Stopped peer discovery");
            MeshLog.i("[WDC]stopPeerDiscovery-success");
        }

        public void onFailure(int reason) {
            MeshLog.e(" Stopping peer discovery failed, error code -> " + P2PUtil.parseReasonCode(reason));
            MeshLog.e("[WDC]stopPeerDiscovery-failed:"+P2PUtil.parseReasonCode(reason));
        }
    };

    public P2PServiceSearcher(Context context) {
        mContext = context;
        mBlockedList = new TreeSet<>();
        mSearchingQueue = new PriorityQueue<>();
    }

    protected abstract void onP2pAlreadyConnected(String ssid);

    /**
     *
     * @param ssid
     * @param passPhrase
     * @param searchMeta is null while open search. If any priority searched subnet found then this
     * value reflect the found subnet metadata(generally ethereum id of GO)
     * @param wifiP2pDevice
     */
    protected abstract void onDesiredServiceFound(String ssid, String passPhrase, String searchMeta, WifiP2pDevice wifiP2pDevice);

    public void startSearching() {
        mIsAlive = true;
        restart();
    }

    private boolean start() {

        MeshLog.v("[p2p_process] service discovery started. Search list:"+mSearchingQueue);

        int randValue = new Random().nextInt((SCAN_SLOT_INTERNAL_MAX + 1 ) -
                SCAN_SLOT_INTERNAL_MIN) + SCAN_SLOT_INTERNAL_MIN;
        AndroidUtil.postBackground(mSearcherScheduler, randValue * 1000);

        mServiceState = ServiceState.NONE;

        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), this);

        mPeerReceiver = new PeerReceiver(mP2PStateListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);
        mContext.registerReceiver(mPeerReceiver, filter);

        mPeerListListener = peers -> {

            final WifiP2pDeviceList pers = peers;
            int numm = 0;
            for (WifiP2pDevice peer : pers.getDeviceList()) {
                numm++;
                Timber.d("\t" + numm + ": " + peer.deviceName + " " + peer.deviceAddress);
            }

            if (numm > 0) {
                startServiceDiscovery();
            } else {
                startPeerDiscovery();
            }
        };

        mDnsSdServiceResponseListener = (instanceName, serviceType, device) -> {

            Timber.d("p2p-debug - initial");
            if (serviceType.startsWith(mServiceType)) {

                if(instanceName.length() > (P2PUtil.GO_PASSWORD_LENGTH +
                        SoftAccessPoint.ANDROID_Q_STATIC_SSID.replaceAll(P2PUtil.GO_PREFIX, "").length() -1 )) {

                    String password = instanceName.substring(0, P2PUtil.GO_PASSWORD_LENGTH);
                    String[] credentials = instanceName.replaceAll(password, "").
                            split(""+SoftAccessPoint.SEPARATOR);
                    if(credentials.length > 1) {
                        String ssid = P2PUtil.GO_PREFIX + credentials[0];
                        String searchMetaData = credentials[1];

                        Timber.d("p2p-debug - desired service found");
                        MeshLog.i("P2P SSID:" + ssid +"-P2P Password:" + password+
                                "-Hash:" + searchMetaData);
                        MeshLog.i("Blocked SSID list: " + mBlockedList);
                        MeshLog.i("Searching list: " + mSearchingQueue);

                        if(!mBlockedList.contains(ssid)) {

                            if (mIsPauseConnectivity) {
                                MeshLog.w(ssid + "network available while Connectivity " +
                                        "paused.");
                            } else {
                                if (mSearchingQueue.isEmpty() ||
                                        mSearchingQueue.contains(searchMetaData)) {

                                    String foundMeta = null;
                                    if(!mSearchingQueue.isEmpty()) {
                                        mSearchingQueue.remove(searchMetaData);
                                        foundMeta = searchMetaData;
                                    }

                                    MeshLog.i("[WDC]onDnsSdServiceAvailable - onDesiredServiceFound");
                                    onDesiredServiceFound(ssid, password, foundMeta, device);

                                } else {
                                    MeshLog.w("Not desired service.");
                                }
                            }

                        }
                    }
                }

            } else {
                MeshLog.i("  Not our Service, :" + SharedPref.read(Constant.KEY_NETWORK_PREFIX) + "!=" + serviceType + ":");
            }

            MeshLog.i("[WDC]startPeerDiscovery call from onDnsSdServiceAvailable");
            startPeerDiscovery();
        };

        if (mWifiP2pManager == null) {
            return false;
        }

        mWifiP2pManager.setDnsSdResponseListeners(mChannel, mDnsSdServiceResponseListener, null);

        mWifiP2pManager.stopPeerDiscovery(mChannel, mActionListenerStart);

        return true;
    }

    private void restart() {
        mIsPreparingSearching = true;
        stopInternal();
        new Thread(() -> {
            AndroidUtil.sleep(SEARCHER_RESTART_INTERVAL * 1000);
            if(mIsAlive) {
                start();
            }
            mIsPreparingSearching = false;
        }).start();
    }

    private synchronized void startServiceDiscovery() {

        if(mServiceState == ServiceState.DiscoverPeer) {
            updateServiceState(ServiceState.RequestingDiscoverService);
            MeshLog.i("[WDC]startServiceDiscovery");
            mWifiP2pManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    MeshLog.i("[WDC]startServiceDiscovery-clear success");
                    WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(SharedPref.read(Constant.KEY_NETWORK_PREFIX));
                    final Handler handler = new Handler();
                    mWifiP2pManager.addServiceRequest(mChannel, request, new WifiP2pManager.ActionListener() {

                        public void onSuccess() {
                            MeshLog.i("[WDC]startServiceDiscovery-add success");
                            handler.postDelayed(new Runnable() {
                                //There are supposedly a possible race-condition bug with the service discovery
                                // thus to avoid it, we are delaying the service discovery start here
                                public void run() {
                                    mWifiP2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                                        public void onSuccess() {
                                            updateServiceState(ServiceState.DiscoverService);
                                            MeshLog.i("[WDC]startServiceDiscovery-success");
                                            mServiceState = ServiceState.DiscoverService;
                                        }

                                        public void onFailure(int reason) {
                                            updateServiceStateWithoutCondition(ServiceState.DiscoverPeer);
                                            MeshLog.e("[WDC]startServiceDiscovery-failed:" + P2PUtil.parseReasonCode(reason));
                                        }
                                    });
                                }
                            }, Constants.Service.DISCOVERY_DELAY);
                        }

                        public void onFailure(int reason) {
                            updateServiceState(ServiceState.DiscoverPeer);
                            MeshLog.e("[WDC]startServiceDiscovery-add failed:" + P2PUtil.parseReasonCode(reason));
                            // No point starting service discovery
                        }
                    });
                }

                @Override
                public void onFailure(int reason) {
                    updateServiceState(ServiceState.DiscoverPeer);
                    MeshLog.e("[WDC]startServiceDiscovery-clear failed:" + P2PUtil.parseReasonCode(reason));

                }
            });
        }
    }

    private synchronized void startPeerDiscovery() {
        if(mServiceState == ServiceState.NONE) {
            updateServiceState(ServiceState.RequestingDiscoverPeer);
            MeshLog.i("[WDC]Started peer discovery");
            mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    updateServiceState(ServiceState.DiscoverPeer);
                    MeshLog.i(" Started peer discovery");
                    MeshLog.i("[WDC]Started peer discovery-success");
                }

                public void onFailure(int reason) {
                    updateServiceStateWithoutCondition(ServiceState.NONE);
                    MeshLog.e(" Starting peer discovery failed, error code " + P2PUtil.parseReasonCode(reason));
                    MeshLog.e("[WDC]Started peer discovery-failed:" + P2PUtil.parseReasonCode(reason));
                }
            });
        }
    }

    private void stopPeerDiscovery() {
//                public static final int ERROR               = 0;
//                public static final int P2P_UNSUPPORTED     = 1;
//                public static final int BUSY                = 2;
//                public static final int NO_SERVICE_REQUESTS = 3;
        // TODO: 8/21/2019
        //Check:
        // 1. whether it fails for blank peerDiscovery stop.
        // 2. Is there any way to detect peer disoovery running or not?
        MeshLog.i("[WDC]stopPeerDiscovery");
        mWifiP2pManager.stopPeerDiscovery(mChannel, mActionListenerStop);
    }

    // TODO: 8/21/2019
    // 1. Does it fail if no service request present?
    // 2. Is there any way to check service discovery running or not?
    private void stopServiceDiscovery() {
        MeshLog.i("[WDC]stopServiceDiscovery");
        mWifiP2pManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                updateServiceStateWithoutCondition(ServiceState.DiscoverPeer);
                MeshLog.i("[WDC]stopServiceDiscovery-success");
            }

            public void onFailure(int reason) {
                MeshLog.e(" Clearing service requests failed, error code -> " + P2PUtil.parseReasonCode(reason));
                MeshLog.e("[WDC]stopServiceDiscovery-failed:"+P2PUtil.parseReasonCode(reason));
            }
        });
    }


    private void stopInternal() {
        MeshLog.i("[WDC]stop p2p service searcher");
        if(mWifiP2pManager != null) {
            mWifiP2pManager.setDnsSdResponseListeners(mChannel, null, null);
            stopServiceDiscovery();
            stopPeerDiscovery();
        }
        try {
            mContext.unregisterReceiver(mPeerReceiver);
        } catch (IllegalArgumentException ex) {
//            ex.printStackTrace();
        }
    }

    public void stop() {
        mIsAlive = false;
        if(mSearchingQueue != null) {
            mSearchingQueue.clear();
        }

        if(mBlockedList != null) {
            mBlockedList.clear();
        }
        AndroidUtil.removeBackground(mSearcherScheduler);
        stopInternal();
    }


    private P2PStateListener mP2PStateListener = new P2PStateListener() {
        @Override
        public void onP2PStateChange(int state) {
            MeshLog.i("[WDC]onP2PStateChange:"+state);

        }

        @Override
        public void onP2PPeersStateChange() {
//            MeshLog.i("[WDC]onP2PPeersStateChange");
            //We do not want any new peer request while service discovery alive
            // TODO: 10/2/2019 except if it is too long that we have not discovered any GO
            if (mServiceState.ordinal() < ServiceState.RequestingDiscoverService.ordinal()) {
                mWifiP2pManager.requestPeers(mChannel, mPeerListListener);
            }
        }

        @Override
        public void onP2PConnectionChanged() {
            MeshLog.i("[WDC]startPeerDiscovery call from onP2PConnectionChanged");
            startPeerDiscovery();
        }

        @Override
        public void onP2PDisconnected() {
            MeshLog.i("[WDC]startPeerDiscovery call from onP2PDisconnected");
            startPeerDiscovery();
        }

        @Override
        public void onP2PPeersDiscoveryStarted() {
            MeshLog.i("[WDC]onP2PPeersDiscoveryStarted");

        }

        @Override
        public void onP2PPeersDiscoveryStopped() {
            MeshLog.i("[WDC]startPeerDiscovery call from onP2PPeersDiscoveryStopped");
            startPeerDiscovery();
        }
    };

    @Override
    public void onChannelDisconnected() {
        Timber.d("onChannelDisconnected");
    }

    /**
     * Update {@link #mServiceState} with serviceState iff new state is greater than earlier state
     * @param serviceState
     */
    private void updateServiceState(ServiceState serviceState) {
        if(ServiceState.valueOf(serviceState.name()).ordinal() >
                ServiceState.valueOf(mServiceState.name()).ordinal()) {
            mServiceState = serviceState;
        }
    }

    private void updateServiceStateWithoutCondition(ServiceState serviceState) {
        mServiceState = serviceState;
    }

    protected void pauseConnectivity() {
        mIsPauseConnectivity = true;
    }

    protected void resumeConnectivity() {
        mIsPauseConnectivity = false;
    }

    public boolean addBlockedList(String ssid) {
        return mBlockedList != null && !TextUtils.isEmpty(ssid) && !mBlockedList.contains(ssid) && mBlockedList.add(ssid);
    }

    public boolean removeFromBlockedList(String ssid) {
        return mBlockedList != null && Text.isNotEmpty(ssid) && mBlockedList.remove(ssid);
    }

    public boolean isBlocked(String ssid) {
        return Text.isNotEmpty(ssid) && CollectionUtil.hasItem(mBlockedList) &&
                mBlockedList.contains(ssid);
    }

    public boolean addSearchQueue(String searchFor) {
        if(mSearchingQueue != null) {
            return mSearchingQueue.add(searchFor);
        }
        return false;
    }

    public boolean addSearchQueue(List<String> searchFor) {
        if(CollectionUtil.hasItem(searchFor)) {
            if (mSearchingQueue == null) {
                mSearchingQueue = new PriorityQueue<>();
            }
            return mSearchingQueue.addAll(searchFor);
        }
        return false;
    }

    public boolean isSearching() {
        return CollectionUtil.hasItem(mSearchingQueue);
    }

    public Queue<String> getSearchingQueue() {
        return mSearchingQueue;
    }
}