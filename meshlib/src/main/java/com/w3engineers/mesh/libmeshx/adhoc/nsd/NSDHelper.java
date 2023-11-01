package com.w3engineers.mesh.libmeshx.adhoc.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for NSD
 */
public class NSDHelper {

    private static Object lock = new Object();
    private static NSDHelper sNSDHelper;
    private NSDListener mNSDListener;

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private volatile String myIP;

    private final String SERVICE_TYPE = "_www._tcp.";
    private final String SERVICE_NAME = "cko";
    /**
     * this name can be changed if same name already exist in the network
     */
    private String mServiceName = SERVICE_NAME;

    private List<NsdServiceInfo> mNsdServiceInfoList;

    public static synchronized NSDHelper getInstance(Context context) {
        if (sNSDHelper == null) {
            synchronized (lock) {
                if (sNSDHelper == null) {
                    sNSDHelper = new NSDHelper(context);
                }
            }
        }

        return sNSDHelper;
    }

    private NSDHelper(Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdServiceInfoList = new ArrayList<>(3);
    }

    public void setNSDListener(NSDListener nsdListener) {
        mNSDListener = nsdListener;
    }

    public void initializeNsd() {
        initializeDiscoveryListener();
        initializeRegistrationListener();

    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                MeshLog.v("[NSD]Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                MeshLog.v("[NSD]Service discovery success:" + service);

                //Not our service
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    MeshLog.v("[NSD]Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    //Same machine, same service
                    MeshLog.v("[NSD]Same machine:" + mServiceName);
                } else if (service.getServiceName().startsWith(SERVICE_NAME)) {
                    //If same service name in same network the NSD rename that automatically
                    //https://developer.android.com/training/connect-devices-wirelessly/nsd#register
                    mNsdManager.resolveService(service, buildResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                MeshLog.v("[NSD]onServiceLost:" + service.toString());
                if (CollectionUtil.hasItem(mNsdServiceInfoList)) {
                    if (mNsdServiceInfoList.remove(service)) {

                        if (mNSDListener != null) {
                            InetAddress inetAddress = service.getHost();
                            String ip = AddressUtil.getIpAddress(inetAddress);
                            if (AddressUtil.isValidIPAddress(ip)) {
                                mNSDListener.onNodeGone(ip);
                            }
                        }
                    }
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                MeshLog.v("[NSD]Discovery stopped:" + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                MeshLog.v("[NSD]Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                MeshLog.v("[NSD]Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public NsdManager.ResolveListener buildResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                MeshLog.v("[NSD]Resolve failed:" + errorCode);
            }

            @Override
            public synchronized void onServiceResolved(NsdServiceInfo serviceInfo) {
                MeshLog.e("[NSD]onServiceResolved:" + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    MeshLog.v("[NSD]Own Service !!!");
                    return;
                }

                if (!TextUtils.isEmpty(myIP) && myIP.equals(serviceInfo.getHost().getHostAddress())) {
                    MeshLog.e("[NSD] IP Conflicting in service Info !!!");
                    return;
                }

                mNsdServiceInfoList.add(serviceInfo);

                if (mNSDListener != null) {
                    InetAddress inetAddress = serviceInfo.getHost();
                    String ip = AddressUtil.getIpAddress(inetAddress);
                    MeshLog.v("[NSD] Host IP::" + serviceInfo.getHost().getHostAddress());
                    if (AddressUtil.isValidIPAddress(ip)) {
                        //tearDown();
                        //stopDiscovery();
                        mNSDListener.onNodeAvailable(ip, serviceInfo.getPort(), serviceInfo.getServiceName());
                    }
                }
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                MeshLog.v("[NSD]onRegistrationFailed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                MeshLog.v("[NSD]onServiceUnregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                MeshLog.v("[NSD]onUnregistrationFailed");
            }

        };
    }

    /**
     * Registers self to be discovered by others.
     *
     * @param name Should be as minimum as possible. Appends with {@link #SERVICE_NAME}.
     * @param port dynamic port association
     */
    public void registerService(String name, int port, InetAddress inetAddress) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(TextUtils.isEmpty(name) ? SERVICE_NAME : SERVICE_NAME + name);
        serviceInfo.setServiceType(SERVICE_TYPE);

        if (!TextUtils.isEmpty(inetAddress.getHostAddress())) {
            MeshLog.v("[NSD] OWN IP address" + inetAddress.getHostAddress());
            serviceInfo.setHost(inetAddress);
            myIP = inetAddress.getHostAddress();
        }
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        MeshLog.v("[NSD]stopDiscovery");
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public void tearDown() {
        if (mRegistrationListener != null) {
            MeshLog.v("[NSD]Stop Broadcasting");
            mNsdManager.unregisterService(mRegistrationListener);
        }
    }
}
