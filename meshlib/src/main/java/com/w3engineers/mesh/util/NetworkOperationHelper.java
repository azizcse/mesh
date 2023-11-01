package com.w3engineers.mesh.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class NetworkOperationHelper {

    private static volatile boolean isOnline = false;
    private static volatile boolean observerStarted = false;
    private static volatile Context context;
    private static volatile String TAG = "NetworkOperationHelper";
    private static volatile Network cellularNetwork;

    private static volatile ConcurrentLinkedQueue<NetworkInterfaceListener> networkInterfaceListeners;

    public interface NetworkInterfaceListener{
        void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi);
    }

    public static void setNetworkInterfaceListeners(NetworkInterfaceListener networkInterfaceListeners_, Context context_){

        if (networkInterfaceListeners == null){
            networkInterfaceListeners = new ConcurrentLinkedQueue<>();
        }
        MeshLog.v("networkInterfaceListeners size1 " + networkInterfaceListeners.size());
        if (!networkInterfaceListeners.contains(networkInterfaceListeners_)) {
            networkInterfaceListeners.add(networkInterfaceListeners_);
        }
        MeshLog.v("networkInterfaceListeners size2 " + networkInterfaceListeners.size());

        if (!observerStarted) {
            context = context_;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setCellularObserver();
                }
            }).start();
        }

    }

    public static void removeNetworkInterfaceListeners(NetworkInterfaceListener networkInterfaceListeners_){
        try {
            MeshLog.v("networkInterfaceListeners size3 " + networkInterfaceListeners.size());
            if (networkInterfaceListeners != null && networkInterfaceListeners.contains(networkInterfaceListeners_)){
                MeshLog.v("networkInterfaceListeners size4 " + networkInterfaceListeners.size());
                networkInterfaceListeners.remove(networkInterfaceListeners_);
            }
            MeshLog.v("networkInterfaceListeners size5 " + networkInterfaceListeners.size());
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    private static void setCellularObserver(){
        Log.v("cellular","observe");

        observerStarted = true;

        NetworkRequest.Builder request = new NetworkRequest.Builder();

        request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        request.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(request.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.v("cellular","network connected " + network.toString());
                cellularNetwork = network;
                isOnline = true;
                for (NetworkInterfaceListener n : networkInterfaceListeners){
                    n.onNetworkAvailable(isOnline, cellularNetwork, false);
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.v("cellular","network not found");
                if (cellularNetwork != null){
                    cellularNetwork = null;
                    makeOffLine();
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.v("cellular","network lost");
                if (cellularNetwork != null){
                    cellularNetwork = null;
                    makeOffLine();
                }
            }
        });
    }

    private static void makeOffLine(){
        if (isOnline){
            isOnline = false;
            cellularNetwork = null;
            for (NetworkInterfaceListener n : networkInterfaceListeners){
                n.onNetworkAvailable(isOnline, null, false);
            }
        }
    }

    public static boolean isOnline(){
        return isOnline;
    }

    public static Network getNetwork(){
        return cellularNetwork;
    }

    public static Network getConnectedMobileNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo;
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            networkInfo = connectivityManager.getNetworkInfo(network);
            NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
            String typeName = networkInfo.getTypeName();

            if (detailedState.equals(NetworkInfo.DetailedState.CONNECTED) && !TextUtils.isEmpty(typeName) && typeName.toLowerCase().contains("mobile")) {
                isOnline = true;
                return network;
            }
        }
        return null;
    }




    /*Context context;
    public static MobileDataActivateListener mobileDataActivateListener;
    public static NetWorkStateChangedListener mNetWorkStateChangedListener;

    *//**
     * Public constractor
     *
     * @param context
     *//*
    public NetworkOperationHelper(Context context) {
        this.context = context;
    }

    *//**
     * To check whether it is connected to any network and at the same time is it  connected to Internet
     *
     * @param context
     * @return whether it is connected or not
     *//*
    public static boolean isConnectedToInternet(Context context) {
        if (isConnected(context)) {
            final String command = "ping -c 1 google.com";
            try {
                return Runtime.getRuntime().exec(command).waitFor() == 0;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
//                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    *//**
     * Listener when cellular data is activated
     *
     * @param mMobileDataActivateListener
     *//*
    public static void setMobileDataActivationListener(MobileDataActivateListener mMobileDataActivateListener) {
        mobileDataActivateListener = mMobileDataActivateListener;
    }

    public static void setNetWorkStateChangedListener(NetWorkStateChangedListener netWorkStateChangedListener) {
        mNetWorkStateChangedListener = netWorkStateChangedListener;
    }

    *//**
     * Whether it is connect to any network, it isn't checking whether the connection has internet
     *
     * @param context
     * @return
     *//*
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    *//**
     * Forcing specific network connection
     *
     * @param context
     * @param isWifi
     *//*
    public static void forceNetWorkConnection(Context context, boolean isWifi) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Settings.Global.getInt(context.getContentResolver(), "mobile_data", 1) != 1) {
            mobileDataActivateListener.onMobileDataChecked(false);
        }
        NetworkRequest.Builder req = new NetworkRequest.Builder();
        req.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if (isWifi) {
            req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            req.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }

        connectivityManager.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (connectivityManager.getNetworkInfo(network).getType() == ConnectivityManager.TYPE_MOBILE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        //connectivityManager.bindProcessToNetwork(network);
                        mobileDataActivateListener.onMobileDataChecked(true);
                    }
                } else {
                    mobileDataActivateListener.onMobileDataChecked(false);
                }
            }
        });
    }

    public interface MobileDataActivateListener {
        void onMobileDataChecked(boolean isActivated);
    }

    *//**
     * Broadcast listener when there is a change in connectivity
     *//*
    public static BroadcastReceiver mInternetConnectivityListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isWifi = false;
            boolean isConnected = false;
            Log.d("NetOperation", "Net changed");
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED) {
                Log.d("NetOperation", "Net changed  mobile");
                isWifi = false;
                isConnected = true;
            } else if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                Log.d("NetOperation", "Net changed  wifi");
                isWifi = true;
                isConnected = true;
            }

            //Background thread added for processing broadcast event
            //so that app ui not get stacked.

            final boolean connected = isConnected;
            final boolean wifi = isWifi;
            HandlerUtil.postBackground(() -> {
                if (mNetWorkStateChangedListener != null) {
                    mNetWorkStateChangedListener.onNetWorkStateChanged(connected, wifi);
                }
            });

        }
    };

    public interface NetWorkStateChangedListener {
        void onNetWorkStateChanged(boolean isConnected, boolean isWifi);
    }

    public static void unregisterWifiReceiver(Context context) {
        context.unregisterReceiver(mInternetConnectivityListener);
    }


    public static void registerNetReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mInternetConnectivityListener, filter);
    }*/
}
