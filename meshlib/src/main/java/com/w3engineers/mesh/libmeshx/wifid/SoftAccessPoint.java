package com.w3engineers.mesh.libmeshx.wifid;

import android.content.Context;
import android.content.IntentFilter;
import android.net.MacAddress;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLogListener;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AndroidUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.P2PUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import timber.log.Timber;

import static android.net.wifi.p2p.WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * SoftAccess point information provider
 */
public class SoftAccessPoint implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.ChannelListener {

    public static final String ANDROID_Q_STATIC_SSID = P2PUtil.GO_PREFIX + "xy";
    public static final String ANDROID_Q_STATIC_PASSPHRASE = "12345678";

    public static final int SERVICE_STATE_OFF = 0;
    public static final int SERVICE_STATE_IDLE = 1;
    public static final int SERVICE_STATE_BROADCASTING = 2;
    private final long SERVICE_STATE_IDLE_DURATION = 30 * 1000;
    private final long SERVICE_STATE_BROADCASTING_DURATION = 60 * 1000;

    private final int SERVICE_BROADCASTING_INTERVAL = 10 * 1000;
    private final long DEVICE_DISCONNECTOR_DECIDER_DELAY = 10 * 1000;
    public static final String OWN_AP_LOG_PREFIX = "[OWN]";

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private Context mContext;
    private PeerReceiver mPeerReceiver;
    protected String mNetworkName = "", mPassphrase = "";
    private MeshXLogListener mMeshXLogListener;
    private WiFiDevicesList mLastWifiP2pDeviceList;
    private SoftAPStateListener mSoftAPStateListener;
    private String mMyBTName;
    public volatile int mServiceState = SERVICE_STATE_OFF;
    private String mServiceString;
    //    private ArpReader mArpReader = new ArpReader();
    private ConcurrentLinkedQueue<String> mPingingIpList;
    public static final char SEPARATOR = ':';
    /**
     * Identity should be as small as possible
     */
    private final String mBroadcastInfo;

    public interface ServiceStateListener {
        void onServiceRemoved(boolean isRemoved);
    }

    private Runnable mServiceBroadcastingScheduler = new Runnable() {
        @Override
        public void run() {

            //If service broadcasting then keeping it off for certain duration
            if (mServiceState == SERVICE_STATE_BROADCASTING) {
                MeshLog.i("Scheduled service broadcast stopping...");
                stopServiceBroadcasting();
                AndroidUtil.postBackground(mServiceBroadcastingScheduler,
                        SERVICE_STATE_IDLE_DURATION);

            } else if (mServiceState == SERVICE_STATE_IDLE) {
                //If service idle then broadcasting for certain duration
                MeshLog.i("Scheduled service broadcast starting...");
                startServiceBroadcasting();
                AndroidUtil.postBackground(mServiceBroadcastingScheduler,
                        SERVICE_STATE_BROADCASTING_DURATION);
            }
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return true;
        }
    };

    public interface SoftAPStateListener {
        /**
         * Soft AP triggered on or off
         *
         * @param isEnabled
         */
        void onSoftAPChanged(boolean isEnabled, String Ssid, String password);

        /**
         * Connetced with given nodes. This is called only for the first time it connects with few devices
         *
         * @param wifiP2pDevices
         */
        void onSoftApConnectedWithNodes(Collection<WifiP2pDevice> wifiP2pDevices);

        /**
         * Calls every time when GO became empty
         */
        void onSoftApDisConnectedWithNodes(String ip);
    }

    public SoftAccessPoint(Context context, SoftAPStateListener softAPStateListener, String broadCastInfo) {

        mBroadcastInfo = broadCastInfo;
        //Read my BT name. Hardcoded string
        mMyBTName = SharedPref.read(Constant.KEY_DEVICE_BLE_NAME);

        mLastWifiP2pDeviceList = new WiFiDevicesList();
        mSoftAPStateListener = softAPStateListener;
        mContext = context;
        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), this);

        mPingingIpList = new ConcurrentLinkedQueue<>();
        //setDeviceName(mWifiP2pManager, mChannel, "meshRnd");
    }

    public void setMeshXLogListener(MeshXLogListener meshXLogListener) {
        mMeshXLogListener = meshXLogListener;
    }

    private WifiP2pManager.GroupInfoListener mGroupInfoListener = new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            try {
//                MeshLog.i("[WDC] onGroupInfoAvailable()");

                if (group != null && (!mNetworkName.equals(group.getNetworkName()) || !mPassphrase.equals(group.getPassphrase()))) {

                    mServiceState = SERVICE_STATE_IDLE;

                    mNetworkName = group.getNetworkName();
                    mPassphrase = group.getPassphrase();
                    if (mMeshXLogListener != null) {
                        mMeshXLogListener.onLog(OWN_AP_LOG_PREFIX + " SSID - " + mNetworkName + "::Passphrase - " + mPassphrase);
                    }

                    SharedPref.write(Constant.KEY_DEVICE_SSID_NAME, mNetworkName);

//                    mMyBTName = "sample string to represent my BT name";
                    MeshLog.e("[p2p_process] Go created ssid: " + mMyBTName+" pass: "+mPassphrase);

                    mServiceString = group.getPassphrase() + group.getNetworkName().replace(
                            P2PUtil.GO_PREFIX, "") + (TextUtils.isEmpty(mBroadcastInfo) ? "" :
                            (SEPARATOR + mBroadcastInfo));

                    startContinousServiceBroadcasting();

                    if (mSoftAPStateListener != null) {
                        mSoftAPStateListener.onSoftAPChanged(true, mNetworkName, mPassphrase);
                    }
                } else {

                    // MeshLog.v("Already have local service for " + mNetworkName + " ," + mPassphrase);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    protected void requestScheduledServiceBroadcast() {
        MeshLog.v("[Mesh]requestScheduledServiceBroadcast");
        AndroidUtil.postBackground(mServiceBroadcastingScheduler, 0);
    }

    private WifiP2pManager.GroupInfoListener mGroupInfoListenerToClearIfExist = new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {

            if (group != null) {

                mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        createSoftAp();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Timber.d("%d", reason);
                        MeshLog.e("removeGroup Failed. Reason " + P2PUtil.parseReasonCode(reason));
                    }
                });
            } else {
                createSoftAp();
            }

        }
    };

    private boolean start() {

        MeshLog.i(" Triggered P2P device search-sap-start");
        if (mWifiP2pManager == null) {
            return false;
        }

        mPeerReceiver = new PeerReceiver(mP2PStateListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);
        mContext.registerReceiver(mPeerReceiver, filter);

        mWifiP2pManager.requestGroupInfo(mChannel, mGroupInfoListenerToClearIfExist);
        return true;
    }

    public void restart() {
        //Does remove GO and then start it
        start();
    }

    /**
     * Before calling make sure no group is present. Update receive on {@link P2PStateListener#onP2PConnectionChanged()}
     */
    private void createSoftAp() {
        WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                MeshLog.i(" Creating Soft AP");
            }

            @Override
            public void onFailure(int reason) {
                MeshLog.e("Soft AP Failed. Reason " + P2PUtil.parseReasonCode(reason));
            }
        };
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {

            MacAddress macAddress = MacAddress.fromString("0:a:d:3:9:a");

            WifiP2pConfig.Builder builder = new WifiP2pConfig.Builder();
            builder.setGroupOperatingBand(GROUP_OWNER_BAND_2GHZ);
            builder.setNetworkName(ANDROID_Q_STATIC_SSID);
            builder.setPassphrase(ANDROID_Q_STATIC_PASSPHRASE);
            builder.enablePersistentMode(true);
            builder.setDeviceAddress(macAddress);
            WifiP2pConfig wifiP2pConfig = builder.build();

            mWifiP2pManager.createGroup(mChannel, wifiP2pConfig, actionListener);
        } else {
            mWifiP2pManager.createGroup(mChannel, actionListener);
        }
    }

    // TODO: 8/21/2019
    // 1. Is there any way to check group existence?
    // 2. Does it fail if we try to remove if group does not exist?
    // - Jukka implemntation what?
    public void removeGroup() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                mServiceState = SERVICE_STATE_OFF;
//                MeshLog.i("[WDC] Cleared Local Group ");
                if (mSoftAPStateListener != null) {
                    mSoftAPStateListener.onSoftAPChanged(false, null, null);
                }
            }

            public void onFailure(int reason) {
                MeshLog.e("[WDC] Clearing Local Group failed, error code " + P2PUtil.parseReasonCode(reason));
            }
        });
    }

    /**
     * Start broadcasting service if Group already in effect.
     *
     * @return
     */
    public boolean startServiceBroadcasting() {
        if (Text.isNotEmpty(mServiceString)) {
            startServiceBroadcasting(mServiceString);
            return true;
        }

        return false;
    }

    private void startServiceBroadcasting(String instance) {

        Map<String, String> record = new HashMap<>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(instance,
                SharedPref.read(Constant.KEY_NETWORK_PREFIX), record);

        MeshLog.i(" Add local service" + instance);
        mWifiP2pManager.addLocalService(mChannel, service, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                mServiceState = SERVICE_STATE_BROADCASTING;
                MeshLog.i("[WDC] Added local service");
            }

            public void onFailure(int reason) {
                MeshLog.e("Adding local service failed, error code" + P2PUtil.parseReasonCode(reason));
            }
        });
    }

    /**
     * Remove scheduled broadcasting if any and re enable continous broadcasting.
     */
    protected void startContinousServiceBroadcasting() {
        AndroidUtil.removeBackground(mServiceBroadcastingScheduler);
        startServiceBroadcasting();
    }

    /**
     * Shutdown service broadcasting completely. It won't resume unless broadcasting started by
     * calling {@link #startServiceBroadcasting()} or restart the GO
     */
    protected void shutDownServiceBroadcasting() {
        MeshLog.v("[Mesh]shutDownServiceBroadcasting");
        AndroidUtil.removeBackground(mServiceBroadcastingScheduler);
        stopServiceBroadcasting();
    }

    private void stopServiceBroadcasting() {

        stopServiceBroadcasting(null);
    }

    public void stopServiceBroadcasting(ServiceStateListener serviceStateListener) {
        mNetworkName = mPassphrase = "";

        mWifiP2pManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                if (mServiceState != SERVICE_STATE_OFF) {
                    mServiceState = SERVICE_STATE_IDLE;
                }
                MeshLog.i("[WDC]Cleared local services");
                if (serviceStateListener != null) {
                    serviceStateListener.onServiceRemoved(true);
                }
            }

            public void onFailure(int reason) {
                MeshLog.v("[WDC]Clearing local services failed, error code " + P2PUtil.parseReasonCode(reason));
                if (serviceStateListener != null) {
                    serviceStateListener.onServiceRemoved(false);
                }
            }
        });
    }

    public void stop() {
        try {
            mContext.unregisterReceiver(mPeerReceiver);
        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
        }
        AndroidUtil.removeBackground(mServiceBroadcastingScheduler);
        stopServiceBroadcasting();
        removeGroup();
        mLastWifiP2pDeviceList = null;
    }

    /**
     * @return how many peers are connected at this moment with this service???
     */
    public int getConnectedPeersCount() {
        return mLastWifiP2pDeviceList == null ? 0 : mLastWifiP2pDeviceList.size();
    }

    /**
     * @return how many peers are connected at this moment with this service???
     */
    public Collection<WifiP2pDevice> getConnectedPeers() {
        return mLastWifiP2pDeviceList;
    }

    private P2PStateListener mP2PStateListener = new P2PStateListener() {
        @Override
        public void onP2PStateChange(int state) {
//            MeshLog.i("[WDC]onP2PStateChange:"+state);
        }

        @Override
        public void onP2PPeersStateChange() {
//            MeshLog.i("[WDC]onP2PPeersStateChange");
        }

        /**
         * group formed from above {@link SoftAccessPoint#createSoftAp()} method call
         */
        @Override
        public void onP2PConnectionChanged() {
//            MeshLog.i("[WDC]onP2PConnectionChanged");
            mWifiP2pManager.requestConnectionInfo(mChannel, SoftAccessPoint.this);
        }

        @Override
        public void onP2PConnectionChanged(Collection<WifiP2pDevice> wifiP2pDevices) {
//            MeshLog.i("[WDC]onP2PConnectionChanged");
            String st = P2PUtil.getLogString(wifiP2pDevices);
            MeshLog.v("[MeshX][Watching_p2p] Broadcast with:" + st);

            WiFiDevicesList devices = P2PUtil.getList(wifiP2pDevices);
            if (P2PUtil.hasItem(devices)) {
                WiFiDevicesList addedList = devices.copy();
                addedList.substract(mLastWifiP2pDeviceList);
                //ensure new devices arrived
                if (P2PUtil.hasItem(devices) && CollectionUtil.hasItem(mLastWifiP2pDeviceList)) {
                    //adding new unique devices
                    mLastWifiP2pDeviceList.addAll(addedList);
//                    if(mSoftAPStateListener != null && addedList.size() > 0) {
//                        mSoftAPStateListener.onSoftApConnectedWithNodes(addedList);
//                    }
                }


                if (P2PUtil.hasItem(mLastWifiP2pDeviceList)) {

                    WiFiDevicesList possibleDisconnectedList = mLastWifiP2pDeviceList.copy();
                    possibleDisconnectedList.substract(devices);

                    // TODO: 7/24/2019 optimize ping to stop pinging same device within certain time
                    //Received mac missing earlier entry so possible disconnection occurred
                    if (possibleDisconnectedList.size() > 0) {
                        MeshLog.v("[Meshx]Possible remove event for GO");
//                        mArpReader.readArpTable(null);
//                        List<String> possibleDisconnectedIpList = mArpReader.getIpFromMac(possibleDisconnectedList);
//                        prepareDisconnectIpQueue(possibleDisconnectedIpList);
//                        new DeviceDisconnector(possibleDisconnectedIpList).startWatching();
                    }
                }
            } else if (P2PUtil.hasItem(mLastWifiP2pDeviceList)) {
//                mArpReader.readArpTable(null);
//                List<String> possibleDisconnectedIpList = mArpReader.getIpFromMac(mLastWifiP2pDeviceList);
//                new DeviceDisconnector(possibleDisconnectedIpList).startWatching();
            }
        }

        @Override
        public void onP2PDisconnected() {
            MeshLog.i("[WDC]onP2PDisconnected");

        }

        @Override
        public void onP2PPeersDiscoveryStarted() {
            MeshLog.i("[WDC]onP2PPeersDiscoveryStarted");

        }

        @Override
        public void onP2PPeersDiscoveryStopped() {
            MeshLog.i("[WDC]onP2PPeersDiscoveryStopped");

        }
    };

    public boolean isGoAlive() {
        return mServiceState == SERVICE_STATE_IDLE || mServiceState == SERVICE_STATE_BROADCASTING;
    }


    @Override
    public void onChannelDisconnected() {
        MeshLog.i("[WDC]onChannelDisconnected");

    }

    /**
     * Connection info available on {@link WifiP2pManager#requestConnectionInfo(WifiP2pManager.Channel, WifiP2pManager.ConnectionInfoListener)}
     * call from {@link P2PStateListener#onP2PConnectionChanged()}
     *
     * @param info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        MeshLog.i("[p2p_process] onConnectionInfoAvailable-isGO" + info.isGroupOwner);
        if (info.isGroupOwner) {

            mWifiP2pManager.requestGroupInfo(mChannel, mGroupInfoListener);
        }
    }

   /* private class DeviceDisconnector {

        private List<String> mPossibleIpList;
        private Executor mExecutor;
        private Pinger.PingListener mPingListener = (ip, isReachable) -> {

            mPingingIpList.remove(ip);
            MeshLog.i("[MeshX][watching] ping result: " + ip + "-" + isReachable);
            if (!isReachable && Text.isNotEmpty(ip)) {
                mSoftAPStateListener.onSoftApDisConnectedWithNodes(ip);
            }
        };

        public DeviceDisconnector(List<String> possibleIpList) {
            mPossibleIpList = possibleIpList;
        }

        void startWatching() {

            new Thread(() -> {
                List<String> mIpList;
                mIpList = RouteManager.getInstance().getConnectedIpAddress(RoutingEntity.Type.WiFi);
                if (CollectionUtil.hasItem(mIpList)) {

                    List<String> commonPossibleDisconnectedIpList = new ArrayList<>();
                    for (String ip : mIpList) {
                        if (mPossibleIpList.contains(ip) && !mPingingIpList.contains(ip)) {
                            mPingingIpList.add(ip);
                            commonPossibleDisconnectedIpList.add(ip);
                        }
                    }
                    if (CollectionUtil.hasItem(commonPossibleDisconnectedIpList)) {

                        mExecutor = Executors.newFixedThreadPool(commonPossibleDisconnectedIpList.size());

                        MeshLog.v("[watching] pinging:" + commonPossibleDisconnectedIpList.toString());

                        for (String ip : commonPossibleDisconnectedIpList) {

                            mExecutor.execute(new Pinger(ip, mPingListener, 2));
                        }
                    }
                }
            }).start();
        }
    }*/

    /*
      Set WifiP2p Device Name
 */
    /*public void setDeviceName(WifiP2pManager manager, WifiP2pManager.Channel channel, String deviceName) {
        //set device name programatically
        try {
            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = manager.getClass().getMethod(
                    "setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);

            Object arglist[] = new Object[3];
            arglist[0] = channel;
            arglist[1] = deviceName;
            arglist[2] = new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    MeshLog.i("[p2p] Device name set Success");
                }

                @Override
                public void onFailure(int reason) {
                    MeshLog.e("[p2p] Device name set failed" + P2PUtil.parseReasonCode(reason));
                }
            };

            setDeviceName.invoke(manager, arglist);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }*/
}
