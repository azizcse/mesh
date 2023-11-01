package com.w3engineers.mesh.linkcash;

import android.text.TextUtils;

import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.datasharing.database.DatabaseService;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.db.users.UserEntity;
import com.w3engineers.mesh.model.PendingMessage;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.wifi.protocol.Link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides Bluetooth connection link
 */
public class ConnectionLinkCache {
    private volatile Map<String, Link> discoveredDirectLinkMap;
    private LinkStateListener linkStateListener;
    private String mNodeId;
    private static ConnectionLinkCache sConnectionLinkCache;
    private volatile ConcurrentLinkedQueue<String> internetBuyerList;
    private volatile long clientRequestTime = 0;

    private Map<String, PendingMessage> pendingMessageMap;

    private volatile Set<String> mConnectedBleNames;

    private ConnectionLinkCache() {
        pendingMessageMap = new ConcurrentHashMap<>();
    }

    //synchronized method to control simultaneous accessPe
    synchronized public static ConnectionLinkCache getInstance() {
        if (sConnectionLinkCache == null) {
            // if instance is null, initialize
            sConnectionLinkCache = new ConnectionLinkCache();
        }
        return sConnectionLinkCache;
    }

    public ConnectionLinkCache initConnectionLinkCache(LinkStateListener linkStateListener, String myNodeId) {
        discoveredDirectLinkMap = Collections.synchronizedMap(new HashMap<>());
        //meshIdAndNodeInfoMap = new ConcurrentHashMap<>();
        //mConnectedSsidNames = Collections.synchronizedSet(new HashSet<>());
        mConnectedBleNames = Collections.synchronizedSet(new HashSet<>());
        internetBuyerList = new ConcurrentLinkedQueue<>();
        this.linkStateListener = linkStateListener;
        this.mNodeId = myNodeId;
        return this;
    }


    public synchronized boolean isBleUserConnected() {
        for (Map.Entry<String, Link> item : discoveredDirectLinkMap.entrySet()) {
            Link link = item.getValue();
            if (link.getType() == Link.Type.BT) {
                return true;
            }
        }
        return false;
    }

    public synchronized BleLink getDirectConnectedBtLink(String nodeId) {
        return (BleLink) discoveredDirectLinkMap.get(nodeId);
    }

    public synchronized List<BleLink> getDirectBleLinks() {
        List<BleLink> directLinkList = new ArrayList<>();
        for (Map.Entry<String, Link> item : discoveredDirectLinkMap.entrySet()) {
            BleLink link = (BleLink) item.getValue();
            if (link.getType() == Link.Type.BT) {
                directLinkList.add(link);
            }
        }
        return directLinkList;
    }

    public synchronized List<String> getDirectWifiNodeIds() {
        List<String> directNodeIdList = new ArrayList<>();
        for (Map.Entry<String, Link> item : discoveredDirectLinkMap.entrySet()) {
            Link link = item.getValue();

            if (link.getType() == Link.Type.WIFI) {
                directNodeIdList.add(item.getKey());
            }
        }
        return directNodeIdList;
    }

    public synchronized List<String> getDirectBleNodeIds() {
        List<String> directNodeIdList = new ArrayList<>();
        for (Map.Entry<String, Link> item : discoveredDirectLinkMap.entrySet()) {
            Link link = item.getValue();

            if (link.getType() == Link.Type.BT) {
                directNodeIdList.add(item.getKey());
            }
        }
        return directNodeIdList;
    }

    public synchronized void clearAllLinks() {

        MeshLog.e("BT disconnect call from clearAllLinks ");

        for (Map.Entry<String, Link> item : discoveredDirectLinkMap.entrySet()) {
            String key = item.getKey();
            Link link = item.getValue();
            link.disconnect();
            if (key != null) {
                linkStateListener.onUserDisconnected(key);
            }
        }

        // TODO discuss needed

        discoveredDirectLinkMap.clear();
        //discoveredMeshLinkMap.clear();
        internetBuyerList.clear();
    }


    public NodeInfo getNodeInfoById(String nodeId) {
        return RouteManager.getInstance().getNodeInfoById(nodeId);
    }


    public List<String> getInternetSellers() {
        return RouteManager.getInstance().getSellerList();
    }

    public void addNodeInfo(String userNodeId, NodeInfo userNodeInfo) {
        MeshLog.e("NODE info added in list: " + userNodeInfo.toString());

        if (!TextUtils.isEmpty(userNodeInfo.getPublicKey())) {
            try {
                DatabaseService databaseService = DatabaseService.getInstance(MeshApp.getContext());
                UserEntity userEntity = databaseService.getUserById(userNodeId);
                if (userEntity == null) {
                    userEntity = new UserEntity();
                    userEntity.address = userNodeId;
                    userEntity.publicKey = userNodeInfo.getPublicKey();
                    databaseService.insertUserEntity(userEntity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        RouteManager.getInstance().addNodeInfo(userNodeInfo);

        //meshIdAndNodeInfoMap.put(userNodeId, userNodeInfo);
        //mConnectedBleNames.add(userNodeInfo.getBleName());
        //mConnectedSsidNames.add(userNodeInfo.getSsidName());
    }

    public void removeNodeInfo(String nodeId) {
        //NodeInfo nodeInfo = meshIdAndNodeInfoMap.get(nodeId);
        NodeInfo nodeInfo = RouteManager.getInstance().getNodeInfoById(nodeId);
        if (nodeInfo != null) {
            RouteManager.getInstance().removeModeInfo(nodeInfo);
        }
        /*if (nodeInfo != null) {
            mConnectedBleNames.remove(nodeInfo.getBleName());
            mConnectedSsidNames.remove(nodeInfo.getSsidName());
        }*/
        //meshIdAndNodeInfoMap.remove(nodeId);

    }


    public synchronized void addDirectLink(String nodeId, Link link, NodeInfo nodeInfo) {
        discoveredDirectLinkMap.clear();
        discoveredDirectLinkMap.put(nodeId, link);
        addNodeInfo(nodeId, nodeInfo);
    }


    public synchronized void removeDirectLink(String nodeId) {
        discoveredDirectLinkMap.remove(nodeId);
        //discoveredMeshLinkMap.clear();
    }


    public String getConnectedBtSet() {
        return "Need to show from db";
    }

    /*public String getConnectedSSIDSet() {
        return mConnectedSsidNames.toString();
    }*/

    //NEWLY ADDED
    public void addBuyerAddressToList(String nodeId) {
        if (!TextUtils.isEmpty(nodeId)) {

           /* Link oldLink = internetUserLinkMap.get(nodeId);
            if (oldLink != null) {
                ((InternetLink) oldLink).closeSocket();
                internetUserLinkMap.remove(nodeId); // to remove the old link cache
            }*/
            if (!internetBuyerList.contains(nodeId)) {
                internetBuyerList.add(nodeId);
            }
        }
    }

    public boolean getInternetConnectionLink(String nodeId) {
        if (!TextUtils.isEmpty(nodeId)) {
            return internetBuyerList.contains(nodeId);
        } else {
            MeshLog.e("Node id null for getInternetConnectionLink()");
            return false;
        }

    }

    public ConcurrentLinkedQueue<String> getInternetBuyerList() {
        return internetBuyerList;
    }

    public void removeInternetLink(String nodeID) {
        if (!TextUtils.isEmpty(nodeID)) {
            internetBuyerList.remove(nodeID);
        }
    }

    public void removeAll() {
        internetBuyerList.clear();
    }


    public long getClientRequestTime() {
        if (clientRequestTime > 0) {
            return clientRequestTime;
        }
        return 0;
    }


    public boolean isBtNameExistInConnectedSet(String name) {
        return mConnectedBleNames.contains(name);
    }

    public void addBtName(String name) {
        mConnectedBleNames.add(name);
    }

    public void removeBtName(String name) {
        mConnectedBleNames.remove(name);
    }

    public int setNewUserRole(String userId, int role) {

        List<RoutingEntity> allPossiblePathList = RouteManager.getInstance().getAllPossiblePathById(userId);

        int previousRole = -1;
        if (allPossiblePathList.size() > 0) {
            previousRole = allPossiblePathList.get(0).getUserMode();
        }
        for (RoutingEntity entity : allPossiblePathList) {
            entity.setUserMode(role);
            RouteManager.getInstance().replaceRoute(entity);
        }

        return previousRole;
    }

    //********* Mesh v2 multi-path messaging ******************/


    public void addPendingMessage(String key, PendingMessage value) {
        pendingMessageMap.put(key, value);
    }

    public PendingMessage removePendingMessage(String key) {
        return pendingMessageMap.remove(key);
    }

    public PendingMessage getPendingMessage(String key) {
        if (key == null) return null;
        return pendingMessageMap.get(key);
    }


    public List<RoutingEntity> filterShortestPathEntity(RoutingEntity shortestPathEntity, String destination) {

        List<RoutingEntity> allReachablePaths = RouteManager.getInstance().getAllPossibleOnlinePathById(destination);

        if (shortestPathEntity == null || allReachablePaths == null || allReachablePaths.isEmpty()) {
            return allReachablePaths;
        }

        for (RoutingEntity item : allReachablePaths) {
            if (item.getAddress().equals(shortestPathEntity.getAddress())
                    && TextUtils.equals(item.getHopAddress(), shortestPathEntity.getHopAddress())
                    && item.getHopCount() == shortestPathEntity.getHopCount()
                    && item.getType() == shortestPathEntity.getType()) {

                allReachablePaths.remove(item);
                break;

            }
        }

        return allReachablePaths;
    }

}
