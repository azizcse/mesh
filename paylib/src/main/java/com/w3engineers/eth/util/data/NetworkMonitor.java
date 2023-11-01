package com.w3engineers.eth.util.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Text;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class NetworkMonitor {
    private static volatile boolean isOnline = false;
//    private static volatile NetworkInterfaceListener networkInterfaceListener;
//    private static volatile String socketUrl;
    private static volatile Context context;
//    private static volatile Socket socket;
    private static volatile String TAG = "NetworkMonitor";
//    private static boolean usingWifiNetwork;

    private static volatile Network cellularNetwork;
//    private static volatile Network usableNetwork;
//    private static volatile boolean isConnecting;
//    private static volatile boolean usingCellularNetwork;
    private static volatile ConcurrentLinkedQueue<NetworkInterfaceListener> networkInterfaceListeners;
//    private static volatile ConnectivityManager connectivityManager;
//    private static Handler handler;
//    private static HandlerThread handlerThread;


    public interface NetworkInterfaceListener{
        void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi);
    }

    public static void start(Context context_, String socket_Url, NetworkInterfaceListener network_Interface_Listener){
        if (networkInterfaceListeners == null){
            networkInterfaceListeners = new ConcurrentLinkedQueue<>();
        }

        networkInterfaceListeners.add(network_Interface_Listener);

//        socketUrl = socket_Url;
        context = context_.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                setCellularObserver();
//                setWifiObserver();
            }
        }).start();
    }

    public static void setNetworkInterfaceListeners(NetworkInterfaceListener networkInterfaceListeners_){
        if (networkInterfaceListeners == null){
            networkInterfaceListeners = new ConcurrentLinkedQueue<>();
        }
        if (!networkInterfaceListeners.contains(networkInterfaceListeners_)) {
            networkInterfaceListeners.add(networkInterfaceListeners_);
        }
    }

    private static void setCellularObserver(){
        Log.v("cellular","observe");

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
//                usableNetwork = network;
//                usingCellularNetwork = true;
                for (NetworkInterfaceListener n : networkInterfaceListeners){
                    n.onNetworkAvailable(isOnline, network, false);
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.v("cellular","network not found");


                if (cellularNetwork != null){
                    cellularNetwork = null;
                    makeOffLine();
//                    closeSocket();
                }

            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.v("cellular","network lost");

                if (cellularNetwork != null){
                    cellularNetwork = null;
                    makeOffLine();
//                    closeSocket();
                }
//                usingCellularNetwork = false;
            }
        });
    }

    /*private static void setWifiObserver(){
        Log.v("wifi","observe");

        NetworkRequest.Builder request = new NetworkRequest.Builder();

        request.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(request.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.v("wifi","network connected " + network);
                initSocket(network);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.v("wifi","network not found");
                if (usingWifiNetwork){
                    makeOffLine();
                    closeSocket();
                }
                usingWifiNetwork = false;
                if (cellularNetwork != null && !usingCellularNetwork){
//                    initSocket(cellularNetwork);
                    setCellularObserver();
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.v("wifi","network lost");

                if (usingWifiNetwork){
                    makeOffLine();
                    closeSocket();
                }
                usingWifiNetwork = false;
                if (cellularNetwork != null && !usingCellularNetwork){
                   // initSocket(cellularNetwork);
                    setCellularObserver();
                }
            }

            @Override
            public void onLosing(Network network, int maxMsToLive) {
                super.onLosing(network, maxMsToLive);
//                Log.v(TAG, network.toString() + "--" + maxMsToLive);
            }


            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
//                Log.v(TAG, network.toString() + "--" + networkCapabilities.toString());
            }

            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
//                Log.v(TAG, network.toString());
            }
        });
    }*/

/*    private static void closeSocket(){
        if (socket != null){
            if (socket.connected()){
                socket.disconnect();
            }
            socket.close();
            socket = null;
        }
    }*/

    private static void makeOffLine(){
        if (isOnline){
            isOnline = false;
//            isConnecting = false;
//            usingCellularNetwork = false;
//            usingWifiNetwork = false;
            cellularNetwork = null;
            for (NetworkInterfaceListener n : networkInterfaceListeners){
                n.onNetworkAvailable(isOnline, null, false);
            }
        }
    }

    /*private synchronized static void initSocket(final Network network) {


        if (isConnecting) return;

        try {

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            boolean isWiFi = false;
            if (networkInfo != null) {
                String typeName = networkInfo.getTypeName();
                Log.v(TAG, typeName);

                isWiFi = typeName.equalsIgnoreCase("wifi");
            }

            OkHttpClient okHttpClient = new OkHttpClient.Builder().socketFactory(network.getSocketFactory()).build();

            // set as an option
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            opts.reconnectionAttempts = 3;
            opts.timeout = 5000;
            socket = IO.socket(socketUrl, opts);


            boolean finalIsWiFi = isWiFi;
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_CONNECT: ");
                    usingWifiNetwork = finalIsWiFi;
                    isOnline = true;
                    usableNetwork = network;
                    usingCellularNetwork = !finalIsWiFi;
                    closeSocket();
                    isConnecting = false;
                    for (NetworkInterfaceListener n : networkInterfaceListeners){
                        n.onNetworkAvailable(isOnline, network, finalIsWiFi);
                    }
                }
            });

            socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_CONNECTING: ");
                    isConnecting = true;
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_DISCONNECT: ");
                    isConnecting = false;
                }
            });

            socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_ERROR: ");
                    isConnecting = false;
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_CONNECT_ERROR: " );
                    isConnecting = false;
                }
            });

            socket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_CONNECT_TIMEOUT: " );
                    isConnecting = false;
                  if (socket != null){
                      socket.connect();
                  }
                }
            });

            boolean finalIsWiFi1 = isWiFi;
            socket.on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_RECONNECT: ");
                    usingWifiNetwork = finalIsWiFi1;
                    isOnline = true;
                    usableNetwork = network;
                    usingCellularNetwork = !finalIsWiFi1;
                    closeSocket();
                    isConnecting = false;
                    for (NetworkInterfaceListener n : networkInterfaceListeners){
                        n.onNetworkAvailable(isOnline, network, finalIsWiFi1);
                    }
                }
            });

            socket.on(Socket.EVENT_RECONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_RECONNECT_ERROR: ");
                    isConnecting = false;
                }
            });

            socket.on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_RECONNECT_FAILED: ");
                    isConnecting = false;
                }
            });
            socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_RECONNECT_ATTEMPT: ");
                    isConnecting = true;
                }
            });
            socket.on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, "Socket EVENT_RECONNECTING: ");
                    isConnecting = true;
                }
            });

            socket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }*/

    public static boolean isOnline(){
        return isOnline;
    }

    public static Network getNetwork(){
        return cellularNetwork;
    }

    public static Network getConnectedMobileNetwork(Context context) {
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
}
