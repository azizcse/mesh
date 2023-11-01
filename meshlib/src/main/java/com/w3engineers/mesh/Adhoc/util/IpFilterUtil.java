package com.w3engineers.mesh.Adhoc.util;

import android.util.Log;

import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.MeshLog;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;

/**
 * <h1> IP mismatch resolves here for zeroconf</h1>
 * <p>
 * Purpose: Active Ip filter
 * Approach: 1. Generate all ip address by splitting local ip subnet
 * 2. Prepare IP queue
 * 3. Check each if by ping http server
 * 4. Remove all unreachable ip from queue
 * 5. Filter all active Ip with DB existence
 * </p>
 *
 * @see com.w3engineers.mesh.Adhoc.AdHocTransport
 */
public class IpFilterUtil {
    private ThreadPoolExecutor mExecutor;
    private Consumer adhocConsumer;
    private int portNumber;
    private boolean isRunning;
    private volatile List<String> activeIpAddress;
    private static int REACHABLE_COUNT = 0;
    private final int MAX_IP_TO_SCAN = 255;


    public IpFilterUtil() {
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
    }

    public IpFilterUtil(String myIpAddress, int portNumber, Consumer<List<String>> consumer) {
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        this.adhocConsumer = consumer;
        this.portNumber = portNumber;
        isRunning = true;
        activeIpAddress = new ArrayList<>();
        prepareAllIPAddress(myIpAddress);

    }

    // Generate all ip address
    private void prepareAllIPAddress(String myIp) {
        REACHABLE_COUNT = 0;
        activeIpAddress.clear();
        List<String> allLocalIpAddress = new ArrayList<>();
        String[] tokens = myIp.split("\\.");
        String firstPart = tokens[0];
        String secondPart = tokens[1];
        String thirdPart = tokens[2];
        for (int i = 1; i <= MAX_IP_TO_SCAN; i++) {
            String generatedIp = firstPart + "." + secondPart + "." + thirdPart + "." + i;
            allLocalIpAddress.add(generatedIp);
        }
        Log.e("Jmdnslog-log", "Generated ip count=" + allLocalIpAddress.size());

        for (String item : allLocalIpAddress) {
            if (!isRunning) return;
            try {
                mExecutor.submit(new ReachableTask(item, (ip, isReachable) -> {
                    if (isReachable) {
                        activeIpAddress.add(ip);
                    }
                    setReachableCount();
                }));

            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ensure 255 ip reachable check
     * and called db filtering from here
     */
    private synchronized void setReachableCount() {
        REACHABLE_COUNT++;
        if (REACHABLE_COUNT % 50 == 0) {
            MeshLog.v("Called DB filtering with =" + activeIpAddress.size());
            filterWithDb();
        }
        //Log.e("Jmdnslog-log", "Reachable count =" + REACHABLE_COUNT);
    }


    // DB filter
    private void filterWithDb() {
        try {
            List<String> tempList = new ArrayList<>();
            tempList.addAll(activeIpAddress);
            for (String item : tempList) {
                RoutingEntity routingEntity = RouteManager.getInstance().getAdhocEntityByIp(item);
                if (routingEntity != null) {
                    activeIpAddress.remove(item);
                }
            }
            stopFiltering();
            MeshLog.e("After DB filter =" + activeIpAddress.toString());

            adhocConsumer.accept(activeIpAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopFiltering() {
        isRunning = false;
        mExecutor.shutdown();
    }

    public void isUserAvailable(String ipAddress, int portNumber, Consumer consumer) {
        this.portNumber = portNumber;
        mExecutor.execute(new ReachableTask(ipAddress, (ip, isReachable) -> consumer.accept(isReachable)));
    }


    /**
     * <p>
     * Inner thread class to check connection
     * It attempt to create connection with our http server.
     * If it able to make connection that means there must have an effective user with this ip.
     * Takes a BiConsumer functional interface as parameter for callback
     * </p>
     */
    private class ReachableTask implements Runnable {
        private String ipAddress;
        private BiConsumer biConsumer;

        public ReachableTask(String ip, BiConsumer<String, Boolean> biConsumer) {
            this.ipAddress = ip;
            this.biConsumer = biConsumer;
        }

        @Override
        public void run() {
            try {
                SocketAddress socketAddress = new InetSocketAddress(ipAddress, portNumber);
                Socket socket = new Socket();
                socket.connect(socketAddress, 1000);
                socket.close();
                biConsumer.accept(ipAddress, true);
                return;
            } catch (Exception e) {
                // e.printStackTrace();
            }
            try {
                biConsumer.accept(ipAddress, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}



