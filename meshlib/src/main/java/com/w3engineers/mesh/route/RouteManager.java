package com.w3engineers.mesh.route;

import android.text.TextUtils;

import androidx.room.Room;

import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.datasharing.database.AppDatabase;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.meta.TableMeta;
import com.w3engineers.mesh.db.routing.NodeInfoDao;
import com.w3engineers.mesh.db.routing.RoutingDao;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.linkcash.NodeInfo;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Flowable;
import timber.log.Timber;

/**
 * Routing table manager
 */
public class RouteManager {

    private final RoutingDao mRoutingDao;
    private final NodeInfoDao mNodeInfoDao;
    private static final RouteManager ourInstance = new RouteManager();

    public static RouteManager getInstance() {
        return ourInstance;
    }

    String mMyNodeId;

    private RouteManager() {
        AppDatabase appDatabase = Room.
                databaseBuilder(App.getContext(), AppDatabase.class, TableMeta.DB_NAME).
                allowMainThreadQueries().fallbackToDestructiveMigration().build();
        mRoutingDao = appDatabase.getRoutingDao();
        mNodeInfoDao = appDatabase.getNodeInfoDao();
        mMyNodeId = SharedPref.read(Constant.KEY_USER_ID);
        mRoutingDao.init(mMyNodeId);
    }

    /**
     * Return entity if it is online
     *
     * @param address
     * @return
     */
    public RoutingEntity getRoutingEntityByAddress(String address) {
        if (Text.isNotEmpty(address)) {
            RoutingEntity routingEntity = mRoutingDao.getOnlineRoutingEntity(address);
            if (routingEntity != null) {
                Timber.d(routingEntity.toString());
            }
            return routingEntity;
        }
        return null;
    }

    public RoutingEntity getLocalOnlyRoutingEntityByAddress(String address) {
        if (Text.isNotEmpty(address)) {
            RoutingEntity routingEntity = mRoutingDao.getLocalOnlineRoutingEntity(address);
            if (routingEntity != null) {
                Timber.d(routingEntity.toString());
            }
            return routingEntity;
        }
        return null;
    }

    /* */

    /**
     * @param receiverAddress
     * @return next hop address if it is a mesh node or next node entity
     *//*
    public RoutingEntity getNextNodeEntityByReceiverAddress(String receiverAddress) {
        if (Text.isNotEmpty(receiverAddress)) {
            RoutingEntity routingEntity = mRoutingDao.getOnlineRoutingEntity(receiverAddress);
            if (routingEntity != null && !TextUtils.isEmpty(routingEntity.getHopAddress())) {
                return mRoutingDao.getOnlineRoutingEntity(routingEntity.getHopAddress());
            }
            return routingEntity;
        }
        return null;
    }*/
    public RoutingEntity getNextNodeEntityByReceiverAddress(String receiverAddress) {
        if (Text.isNotEmpty(receiverAddress)) {

            List<Integer> allTypeList = getAllPossibleTypeById(receiverAddress);

            if (allTypeList != null && !allTypeList.isEmpty()) {

                if (allTypeList.contains(RoutingEntity.Type.INTERNET)) {
                    // So receiver address is connected by internet
                    // Now we have to check receiver address has other local connection or not
                    // so the list size is greater than 1 then we can sure there are a local connection

                    if (hasAnyLocalConnection(allTypeList)) {
                        // Now we will give priority local connection
                        RoutingEntity routingEntity = mRoutingDao.getShortestPathExcludeByType(receiverAddress,
                                RoutingEntity.Type.INTERNET);

                        if (routingEntity != null && !TextUtils.isEmpty(routingEntity.getHopAddress())) {
                            // if the main receiver is local then hop must be local
                            routingEntity = mRoutingDao.getShortestPathExcludeByType(routingEntity.getHopAddress(),
                                    RoutingEntity.Type.INTERNET);
                        }

                        return routingEntity;
                    } else {

                        // Now it is internet user

                        RoutingEntity routingEntity = mRoutingDao.getShortestPath(receiverAddress);
                        if (routingEntity != null && !TextUtils.isEmpty(routingEntity.getHopAddress())) {

                            // If hop exists then the hop can be internet or local.
                            // When local that means I am buyer.

                            routingEntity = mRoutingDao.getShortestPath(routingEntity.getHopAddress());
                        }
                        return routingEntity;
                    }


                } else {
                    RoutingEntity routingEntity = mRoutingDao.getShortestPath(receiverAddress);
                    if (routingEntity != null && !TextUtils.isEmpty(routingEntity.getHopAddress())) {

                        // if the main receiver is local then hop must be local
                        routingEntity = mRoutingDao.getShortestPathExcludeByType(routingEntity.getHopAddress(),
                                RoutingEntity.Type.INTERNET);
                    }
                    return routingEntity;
                }
            }

        }
        return null;
    }

    public RoutingEntity getNextNodeEntityByReceiverAddress(String immediateSenderId, String receiverAddress, int transport) {

        //Todo we have to manage Shortest path for internet later (For internet it is )
        // Todo basically it is query where only use for message forwarding section.
        //  So we can ignore Internet user from query

        List<RoutingEntity> allPathFound = mRoutingDao.getAllPossibleOnlinePathById(receiverAddress);

        // We have to filter out Internet user from this list. It is only for local
        // This query used other section
        List<RoutingEntity> allPossiblePath = new ArrayList<>();
        for (RoutingEntity r : allPathFound) {
            if (r.getType() != RoutingEntity.Type.INTERNET) {
                allPossiblePath.add(r);
            }
        }

//        for (RoutingEntity r : validInternetUser){
//            allPossiblePath.remove(r);
//        }


        RoutingEntity entity = null;


        // First sort the routing path by hop count

        // Second sort the node list descending order if transport is wifi mesh

        if (transport == RoutingEntity.Type.WiFi) {
            allPossiblePath = sortListForWifi(allPossiblePath);
        } else if (transport == RoutingEntity.Type.BLE) {
            allPossiblePath = sortListForBle(allPossiblePath);
        }

        for (RoutingEntity item : allPossiblePath) {
            if (item != null && !TextUtils.isEmpty(item.getHopAddress())) {

                if (transport == RoutingEntity.Type.BLE) {
                    if (item.getType() == RoutingEntity.Type.WifiMesh
                            || item.getType() == RoutingEntity.Type.BLE_MESH) {
                        entity = getShortestPath(item.getHopAddress());
                        if (entity != null) {
                            MeshLog.i("Route path select wifi for wifi mesh : " + AddressUtil.makeShortAddress(entity.getAddress()));
                        }
                        break;
                    }
                } else if (transport == RoutingEntity.Type.WiFi) {
                    if (item.getType() == RoutingEntity.Type.BLE_MESH) {
                        entity = getShortestPath(item.getHopAddress());
                        if (entity != null && !entity.getAddress().equals(immediateSenderId)) {
                            MeshLog.i("Route path select wifi for wifi mesh: " + AddressUtil.makeShortAddress(entity.getAddress()));
                            break;
                        } else {
                            //MeshLog.i("Route path select loop created: " + AddressUtil.makeShortAddress(entity.getAddress()));
                            entity = null;
                        }
                    } else if (item.getType() == RoutingEntity.Type.WifiMesh) {
                        entity = getShortestPath(item.getHopAddress());
                        if (entity != null && !entity.getAddress().equals(immediateSenderId)) {
                            MeshLog.i("Route path select wifi for wifi mesh: " + AddressUtil.makeShortAddress(entity.getAddress()));
                            break;
                        } else {
                            //MeshLog.i("Route path select loop created: " + AddressUtil.makeShortAddress(entity.getAddress()));
                            entity = null;
                        }
                    }
                } else {
                    entity = getShortestPath(item.getHopAddress());
                    break;
                }

            } else {
                entity = item;
                MeshLog.i("Route path select direct: " + AddressUtil.makeShortAddress(entity.getAddress()));
                break;
            }
        }

        return entity;
    }


    /**
     * Sort list for WiFi transport. When a message will be forwarded from wifitransport
     * first we will give priority for direct connected user then bleMesh user
     *
     * @param routList (sorted) direct,bleMesh,wifiMesh
     * @return
     */
    private List<RoutingEntity> sortListForWifi(List<RoutingEntity> routList) {
        List<RoutingEntity> sortedList = new ArrayList<>();
        List<RoutingEntity> directList = new ArrayList<>();
        for (RoutingEntity entity : routList) {
            if (TextUtils.isEmpty(entity.getHopAddress())) {
                directList.add(entity);
            }
        }

        if (!directList.isEmpty()) {
            Collections.sort(directList, new Comparator<RoutingEntity>() {
                @Override
                public int compare(RoutingEntity routingEntity, RoutingEntity t1) {
                    return Integer.compare(routingEntity.getType(), t1.getType());
                }
            });
        }

        routList.removeAll(directList);


        // We just file ble mesh user first in wifi
        List<RoutingEntity> bleMeshUserList = new ArrayList<>();
        for (RoutingEntity entity : routList) {
            if (entity.getType() == RoutingEntity.Type.BLE_MESH) {
                bleMeshUserList.add(entity);
            }
        }

        routList.removeAll(bleMeshUserList);

        // This basically sort for ble mesh user when the message
        // receive in wifi transport and forward to other

        sortedList.addAll(directList);
        sortedList.addAll(bleMeshUserList);
        sortedList.addAll(routList);

        return sortedList;
    }

    /**
     * Sort list for BLE transport. When a message will be forwarded from BleTransport
     * first we will give priority for direct connected user then wifiMesh user
     *
     * @param routList (sorted) direct,wifiMesh,bleMesh
     * @return
     */
    private List<RoutingEntity> sortListForBle(List<com.w3engineers.mesh.db.routing.RoutingEntity> routList) {
        List<com.w3engineers.mesh.db.routing.RoutingEntity> sortedList = new ArrayList<>();
        List<com.w3engineers.mesh.db.routing.RoutingEntity> directList = new ArrayList<>();
        for (com.w3engineers.mesh.db.routing.RoutingEntity entity : routList) {
            if (TextUtils.isEmpty(entity.getHopAddress())) {
                directList.add(entity);
            }
        }

        routList.removeAll(directList);

        // We just file ble mesh user first in wifi
        List<RoutingEntity> wifiMeshUserList = new ArrayList<>();
        for (RoutingEntity entity : routList) {
            if (entity.getType() == RoutingEntity.Type.WifiMesh) {
                wifiMeshUserList.add(entity);
            }
        }

        routList.removeAll(wifiMeshUserList);

        sortedList.addAll(directList);
        sortedList.addAll(wifiMeshUserList);
        sortedList.addAll(routList);

        return sortedList;
    }

    public RoutingEntity getWiFiEntityByIp(String ip) {
        if (Text.isNotEmpty(ip)) {
            RoutingEntity routingEntity = mRoutingDao.getRoutingEntityByIp(ip, RoutingEntity.Type.WiFi);
            if (routingEntity != null) {
                Timber.d(routingEntity.toString());
            }
            return routingEntity;
        }
        return null;
    }

    public RoutingEntity getAdhocEntityByIp(String ip) {
        if (Text.isNotEmpty(ip)) {
            RoutingEntity routingEntity = mRoutingDao.getRoutingEntityByIp(ip, RoutingEntity.Type.HB);
            if (routingEntity != null) {
                Timber.d(routingEntity.toString());
            }
            return routingEntity;
        }
        return null;
    }

    public boolean isUserHashExists(int hash) {
        return false;//mRoutingDao.checkUserHashExists(hash) > 0;
    }


    public List<String> getConnectedIpAddress(int type) {
        List<String> routingEntities = mRoutingDao.getConnectedIp(type);
        if (routingEntities != null) {
            MeshLog.i(" CONNECTED IP: " + routingEntities.toString());
        }
        return routingEntities;
    }

    public boolean insertRoute(RoutingEntity entity) {
        entity.setId(0);

        // Discard self entity
        if (entity.getAddress().equals(mMyNodeId)) {
            return false;
        }

        if (entity.getType() == RoutingEntity.Type.WiFi) {
            entity.setHopCount(0);
            entity.setHopAddress(null);
            RoutingEntity oldRoutingEntity = RouteManager.getInstance().getSingleUserInfoByType(entity.getAddress(), RoutingEntity.Type.WiFi);
            if (oldRoutingEntity != null) {
                MeshLog.i("[p2p_process] Same Wifi user found again: " + AddressUtil.makeShortAddress(entity.getAddress()));
                return false;
            }
        }

        MeshLog.v("Before insert the data is: " + entity.toString());

        return mRoutingDao.insert(entity) > 0;
    }

    public boolean replaceRoute(RoutingEntity entity) {
        return mRoutingDao.insertOrUpdate(entity) > 0;
    }

    /**
     * Update route whether a node is available or not
     *
     * @param routingEntity
     * @return
     */
    public boolean updateRoute(RoutingEntity routingEntity) {
        boolean isUpdated = mRoutingDao.upsert(routingEntity);
        if (isUpdated) {
            MeshLog.v("Entry updated into db   Route: " + isUpdated + "  " + routingEntity.toString());
        } else {
            MeshLog.i("Routing table update failed!! for::" + routingEntity.toString());
        }

        return isUpdated;
    }


    public int updateEntity(RoutingEntity entity) {
        return mRoutingDao.update(entity);
    }

    /**
     * Update {@code targetAddress} node and all nodes hoped by {@code targetAddress} node as
     * offline. It validate whether {@code fromAddress} has the authority to update
     * {@code targetAddress} as offline. If {@code fromAddress} is null then we validate that
     * {@code targetAddress} is directly connected or not.
     * <p>
     * It is only for Local connected user. For Internet offline please use {@link #updateNodeAsOfflineForInternet}
     *
     * @param fromAddress
     * @param targetAddress
     * @return
     */
    public List<RoutingEntity> updateNodeAsOffline(String fromAddress, String targetAddress) {

        List<RoutingEntity> routingEntities = mRoutingDao.updateAsOffline(fromAddress, targetAddress);
        MeshLog.i("updateNodeAsOffline list ::" + routingEntities);

        //
        if (CollectionUtil.hasItem(routingEntities)) {
            MeshLog.i("Entry updated as offline ::" + routingEntities.size());
            return routingEntities;
        } else {
            MeshLog.i("updateNodeAsOffline list ::" + routingEntities);
        }

        return null;
    }

    public List<RoutingEntity> updateNodeAsOfflineForInternet(String targetAddress) {

        List<RoutingEntity> routingEntities = selectOnlyHopedNodesForInternetBySeller(targetAddress);

        RoutingEntity directUser = getSingleUserInfoByType(targetAddress, RoutingEntity.Type.INTERNET);

        routingEntities.add(directUser);

        for (RoutingEntity entity : routingEntities) {
            entity.resetMetaData();
        }

        int updateCount = mRoutingDao.update(routingEntities);

        MeshLog.i("updateNodeAsOffline list ::" + routingEntities);

        //
        if (CollectionUtil.hasItem(routingEntities)) {
            MeshLog.i("Entry updated as offline ::" + routingEntities.size());
            return routingEntities;
        } else {
            MeshLog.i("updateNodeAsOffline list ::" + routingEntities);
        }

        return null;
    }


    public List<RoutingEntity> getAllRoutingEntityWithHopId(String directConnectedAddress) {
        return mRoutingDao.getAllRoutingEntityWithHopId(directConnectedAddress);
    }


    /**
     * Update all WiFi route as unavailable
     *
     * @return {@link RoutingEntity} of affected rows
     */
    public List<RoutingEntity> updateWiFiNodeAsOffline() {

        List<RoutingEntity> routingEntities = mRoutingDao.updateWiFiNodeAsOffline();
        if (routingEntities != null) {
            Timber.d("%s Entry updated as offline.", routingEntities.toString());
        }

        return routingEntities;
    }

    /**
     * Find this nodes hop address
     *
     * @param address
     * @return null if not available, empty string if directly connected, hop address if connected
     * via a node
     */
    public RoutingEntity getHoppedNode(String address) {

        ArrayList<RoutingEntity> routingEntities = (ArrayList<RoutingEntity>) mRoutingDao.getAllOnline();

        if (CollectionUtil.hasItem(routingEntities)) {

            List<RoutingEntity> matchedEntities =
                    CollectionUtil.getMatchedList(routingEntities, address);

            if (CollectionUtil.hasItem(matchedEntities)) {

                return matchedEntities.get(0);

            }
        }

        //Not available in routing table so return null
        return null;
    }

    public boolean isWifiUserConnected() {
        List<RoutingEntity> routingEntities = mRoutingDao.getWifiConnected();
        return routingEntities != null && !routingEntities.isEmpty();

    }


    public boolean isBtUserConnected() {
        List<RoutingEntity> routingEntities = mRoutingDao.isBtConnected();
        return CollectionUtil.hasItem(routingEntities);

    }

    public List<RoutingEntity> getAllBtUsers() {
        return mRoutingDao.getAllBtUsers();
    }

    public List<RoutingEntity> getAllWifiUsers() {
        return mRoutingDao.getAllWifiUsers();
    }

    public List<RoutingEntity> getConnectedDirectWifiUsers() {
        return mRoutingDao.getWifiConnected();
    }

    public List<RoutingEntity> getBtUsers() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.BT);
    }

    public List<RoutingEntity> getBleUsers() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.BLE);
    }

    public List<RoutingEntity> getUsersByType(int type) {
        return mRoutingDao.getConnectedEntitiesByType(type);
    }


    public RoutingEntity getDirectBtUser() {
        List<RoutingEntity> routingEntities = mRoutingDao.getConnectedEntitiesByType
                (RoutingEntity.Type.BT);

        if (CollectionUtil.hasItem(routingEntities)) {
            return routingEntities.get(0);
        }
        return null;
    }

    public List<RoutingEntity> getBleMeshUsers() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.BtMesh);
    }

    public List<RoutingEntity> getBleMeshUsersByTimeDifference(String myNodeId, long startTime, long endTime) {
        return null;//mRoutingDao.getBtMeshUserByTimeRange(myNodeId, startTime, endTime);
    }

    public List<RoutingEntity> getWifiUser() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.WiFi);
    }

    public List<RoutingEntity> getAdhocUser() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.HB);
    }

    public List<RoutingEntity> getAdhocMeshUser() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.HB_MESH);
    }

    public List<RoutingEntity> getWifiMeshUser() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.WifiMesh);
    }

    public void deleteRoutingTableEntity() {
        mRoutingDao.deleteAll();
    }

    public int getLinkTypeById(String nodeID) {
        List<Integer> allTypeList = getAllPossibleTypeById(nodeID);
        if (allTypeList != null && !allTypeList.isEmpty()) {
            RoutingEntity entity;

            if (allTypeList.contains(RoutingEntity.Type.INTERNET)) {
                if (hasAnyLocalConnection(allTypeList)) {
                    // Now we will give priority local connection
                    entity = mRoutingDao.getShortestPathExcludeByType(nodeID,
                            RoutingEntity.Type.INTERNET);
                } else {
                    entity = mRoutingDao.getShortestPath(nodeID);
                }
            } else {
                entity = mRoutingDao.getShortestPath(nodeID);
            }

            if (entity != null) {
                return entity.getType();
            }
        }

        return 0;
    }

    private boolean hasAnyLocalConnection(List<Integer> connectionList) {

        return connectionList.contains(RoutingEntity.Type.WiFi)
                || connectionList.contains(RoutingEntity.Type.WifiMesh)
                || connectionList.contains(RoutingEntity.Type.BLE)
                || connectionList.contains(RoutingEntity.Type.BLE_MESH);
    }

    public List<Integer> getMultipleLinkTypeById(String nodeID) {
        return mRoutingDao.getMultipleLinkTypeById(nodeID);
    }

    public List<Integer> getMultipleLinkTypeByIdDebug(String nodeID) {
        return mRoutingDao.getMultipleLinkTypeByIdDebug(nodeID);
    }

    public boolean isLocallyOnline(String userId) {
        if (TextUtils.isEmpty(userId)) return false;

        int status = 0;
        RoutingEntity entity = mRoutingDao.getShortestPathExcludeByType(userId,
                RoutingEntity.Type.INTERNET);
        if (entity != null && entity.isOnline()) {
            status = 1;
        }
        return status == 1;
    }

    public boolean isOnline(String userId) {
        if (TextUtils.isEmpty(userId)) return false;

        int status = 0;
        RoutingEntity entity = mRoutingDao.getEntityByAddress(userId);
        if (entity != null && entity.isOnline()) {
            status = 1;
        }
        //MeshLog.e("Online Status Of :: " + userId.substring(userId.length() - 3) + " = " + status);
        return status == 1;
    }

    public boolean isWifiUserOnline(String userId) {
        if (TextUtils.isEmpty(userId)) return false;
        int status = mRoutingDao.isThisWifiUserIsInOnline(userId);
        MeshLog.e("[Wifi] Online Status Of :: " + AddressUtil.makeShortAddress(userId) + " = " + status);
        return status == 1;
    }

    public void makeUserOfline(List<String> list) {
        MeshLog.v("makeUserOfline  " + list.toString());
        mRoutingDao.makeInternetUserOffline(list.toArray(new String[list.size()]));
    }

    public void makeInternetUserOffline(String senderId, List<String> list) {
        mRoutingDao.makeInternetUserOffline(senderId, list);
    }


    public boolean isBtHopIdExist(String hopNodeId) {
        if (TextUtils.isEmpty(hopNodeId)) return false;
        RoutingEntity routingEntity =
                mRoutingDao.isBtHopIdExistInOnline(hopNodeId);
        return routingEntity != null;
    }

    public boolean isWifiHopIdExist(String hopNodeId) {
        if (TextUtils.isEmpty(hopNodeId)) return false;
        RoutingEntity routingEntity =
                mRoutingDao.isWifiHopIdExistInOnline(hopNodeId);
        return routingEntity != null;
    }

    public boolean isAdhocUserOnline(String hopNodeId) {
        if (TextUtils.isEmpty(hopNodeId)) return false;
        return mRoutingDao.isNodeConnectedIn(hopNodeId, RoutingEntity.Type.HB);
    }

    public int getReceiverAddressType(String receiverAddress) {
        if (Text.isNotEmpty(receiverAddress)) {
            return mRoutingDao.getReceiverAddressType(receiverAddress);
        }
        return -1;
    }

    public Flowable<List<RoutingEntity>> getAll() {
        return mRoutingDao.getAll();
    }


    public List<RoutingEntity> getConnectedNodesByAddress(String address) {
        if (TextUtils.isEmpty(address)) return null;
        return mRoutingDao.getNodesWithHopedNodes(address);
    }

    public List<RoutingEntity> getInternetUsers() {
        return mRoutingDao.getConnectedEntitiesByType(RoutingEntity.Type.INTERNET);
    }

    public List<RoutingEntity> makeUserOffline(RoutingEntity routingEntity) {
        List<RoutingEntity> offlineList = new ArrayList<>();

        if (routingEntity == null) return offlineList;

        if (routingEntity.getType() == RoutingEntity.Type.WiFi
                || routingEntity.getType() == RoutingEntity.Type.BLE) {

            MeshLog.e("making offline id: " + AddressUtil.makeShortAddress(routingEntity.getAddress()));
            List<RoutingEntity> users = updateNodeAsOffline("", routingEntity.getAddress());
            if (users != null) {
                offlineList.addAll(users);
            }
        } else if (routingEntity.getType() == RoutingEntity.Type.INTERNET) {
            MeshLog.e("making internet offline id: " + AddressUtil.makeShortAddress(routingEntity.getAddress()));
            offlineList.addAll(updateNodeAsOfflineForInternet(routingEntity.getAddress()));
        } else {
            mRoutingDao.makeUserOffline(routingEntity);

            offlineList.add(routingEntity);
        }

        return offlineList;
    }

    public void makeUserOffline(List<RoutingEntity> entities) {
        for (RoutingEntity entity : entities) {
            entity.resetMetaData();
        }
        mRoutingDao.update(entities);
    }

    public List<RoutingEntity> getAllDisconnectedInternetUser(String hopAddress) {
        return mRoutingDao.getAllDisconnectedInternetUser(hopAddress);
    }

    public RoutingEntity getNodeDetailsByIP(String ipAddress) {
        return mRoutingDao.getNodeByIP(ipAddress);
    }

    public boolean isAdhocUserConneted() {
        List<RoutingEntity> routingEntities = mRoutingDao.getAdhocConnected();
        return routingEntities != null && !routingEntities.isEmpty();
    }

    public boolean isConnectedIn(int interfaceType) {
        return CollectionUtil.hasItem(mRoutingDao.getConnectedEntitiesByType(interfaceType));
    }

    /**
     * Get all online node
     *
     * @return connected list
     */
    public List<RoutingEntity> getAllConnectedNodes() {
        return mRoutingDao.getAllOnlineUserWithMinimumHop();
    }

    public List<RoutingEntity> getAllOffLineEntity() {
        return mRoutingDao.getAllOffline();
    }


    public List<RoutingEntity> getOnlineEntityExceptWifi() {
        return mRoutingDao.onlineEntityExceptWifi();
    }

    /*******************************************************************************/
    /***************************** NodeInfo db access portion **********************/
    /*******************************************************************************/

    public long addNodeInfo(NodeInfo nodeInfo) {
        return mNodeInfoDao.insertOrUpdate(nodeInfo);
    }

    public NodeInfo getNodeInfoById(String userId) {
        return mNodeInfoDao.getNodeInfoById(userId);
    }

    public List<String> getSellerList() {
        List<RoutingEntity> sellerList = mRoutingDao.getAllSellerList();

        List<String> sellers = new ArrayList<>();
        if (sellerList != null && !sellerList.isEmpty()) {
            for (RoutingEntity entity : sellerList) {
                sellers.add(entity.getAddress());
            }
        }
        return sellers;
    }

    public int setNewUserRole(String userId, int newRole) {
        NodeInfo nodeInfo = mNodeInfoDao.getNodeInfoById(userId);
        if (nodeInfo != null) {
            nodeInfo.setUserMode(newRole);
            return mNodeInfoDao.update(nodeInfo);
        }
        return -1;
    }

    public int deleteNodeInfoById(String userId) {
        return mNodeInfoDao.deleteNodeInfoById(userId);
    }

    public void deleteNodeInfoEntity() {
        mNodeInfoDao.deleteAllItem();
    }

    public void removeModeInfo(NodeInfo nodeInfo) {
        if (nodeInfo != null) {
            mNodeInfoDao.delete(nodeInfo);
        }
    }

    /*public boolean isSSidExist(String ssidName) {
        NodeInfo nodeInfo = mNodeInfoDao.getNodeInfoBySSid(ssidName);
        if (nodeInfo == null) return false;
        return true;
    }

    public boolean isBtNameExist(String btName) {
        NodeInfo nodeInfo = mNodeInfoDao.getNodeInfoByBtName(btName);
        if (nodeInfo == null) return false;
        return true;
    }*/

    public List<RoutingEntity> resetDb() {
        List<RoutingEntity> list = mRoutingDao.updateOnlineNodeAsOffline();
        if (CollectionUtil.hasItem(list)) {
            MeshLog.i("Offline nodes::" + list.toString());
        }

        return list;
    }

    public List<RoutingEntity> resetDbForWifiNode() {
        List<RoutingEntity> list = mRoutingDao.updateOnlineNodeAsOfflineOnlyWifi();
        if (CollectionUtil.hasItem(list)) {
            MeshLog.i("Offline nodes for only wifi::" + list.toString());
        }

        return list;
    }


    public List<RoutingEntity> getConnectedDirectUsers() {
        return mRoutingDao.getConnectedDirectUsers();
    }

    //Mesh v2 implementation

    public List<String> getAllNodeIds() {
        return mRoutingDao.getAllNodeIds();
    }

    public List<String> getAllOnlineNodeIds() {
        return mRoutingDao.getAllOnlineNodeIds();
    }

    public RoutingEntity getEntityByAddress(String nodeId) {
        return mRoutingDao.getEntityByAddress(nodeId);
    }

    public RoutingEntity getGoRoutingEntityByIp(String ipAddress) {
        return mRoutingDao.getGoEntity(ipAddress);
    }

    public boolean isHopExists(String destinationId, String hopId) {
        if (hopId == null) return false;
        RoutingEntity entity = mRoutingDao.getRoutByDestinationAndHop(destinationId, hopId);
        return entity != null && entity.isOnline();
    }


    public RoutingEntity getEntityByDestinationAndHop(String destinationId, String hopId) {
        if (destinationId == null || hopId == null) return null;
        return mRoutingDao.getRoutByDestinationAndHop(destinationId, hopId);
    }


    public RoutingEntity getRoutByDestinationAndHopAndType(String destinationId, String hopId, int type) {
        if (destinationId == null || hopId == null) return null;
        return mRoutingDao.getRoutByDestinationAndHopAndType(destinationId, hopId, type);
    }


    /**
     * This method will return shortest path of a node.
     * If the node has both internet and local connection then local connection will be priorities
     *
     * @param nodeID
     * @return RoutingEntity
     */
    public RoutingEntity getShortestPath(String nodeID) {
        if (nodeID == null) return null;
        List<Integer> allTypeList = getAllPossibleTypeById(nodeID);
        if (allTypeList != null && !allTypeList.isEmpty()) {
            RoutingEntity entity;

            if (allTypeList.contains(RoutingEntity.Type.INTERNET)) {
                if (hasAnyLocalConnection(allTypeList)) {
                    // Now we will give priority local connection
                    entity = mRoutingDao.getShortestPathExcludeByType(nodeID,
                            RoutingEntity.Type.INTERNET);
                } else {
                    entity = mRoutingDao.getShortestPath(nodeID);
                }
            } else {
                entity = mRoutingDao.getShortestPath(nodeID);
            }

            return entity;
        }

        return null;


        // return mRoutingDao.getShortestPath(nodeId);
    }

    public List<RoutingEntity> getAllOnlineUserWithMinimumHop() {
        return mRoutingDao.getAllOnlineUserWithMinimumHop();
    }

    public List<RoutingEntity> getAllUserExcludeByType(int type) {
        return mRoutingDao.getAllUserExcludeByType(type);
    }

    public List<RoutingEntity> getAllBleAndBleMeshUser() {
        return mRoutingDao.getAllBleAndBleMeshUser();
    }

    public List<RoutingEntity> getAllUserByType(int type) {
        return mRoutingDao.getAllUserByType(type);
    }

    public List<RoutingEntity> getAllPossibleOnlinePathById(String nodeId) {
        return mRoutingDao.getAllPossibleOnlinePathById(nodeId);
    }

    public List<RoutingEntity> getAllPossiblePathById(String nodeId) {
        return mRoutingDao.getAllPossiblePathById(nodeId);
    }

    public List<RoutingEntity> getAllPossiblePathByIdAndType(String nodeId, int type) {
        return mRoutingDao.getAllPossiblePathByIdAndType(nodeId, type);
    }


    public RoutingEntity getArbitraryEntityById(String nodeId) {
        return mRoutingDao.getArbitraryPath(nodeId);
    }

    public Flowable<List<RoutingEntity>> getAllLinkPath() {
        return mRoutingDao.getAllLinkPath();
    }

    public boolean isDirectlyConnected(String nodeId) {
        if (TextUtils.isEmpty(nodeId)) return false;
        RoutingEntity routingEntity = mRoutingDao.getSingleUserInfoByType(nodeId, RoutingEntity.Type.INTERNET);
        return routingEntity != null &&
                (routingEntity.getHopAddress() == null || routingEntity.getHopAddress().isEmpty());
    }

    public RoutingEntity getSingleUserInfoByType(String userId, int type) {
        return mRoutingDao.getSingleUserInfoByType(userId, type);
    }

    public List<Integer> getAllPossibleTypeById(String nodeId) {
        return mRoutingDao.getAllPossibleTypeById(nodeId);
    }

    public RoutingEntity getSellerById(String id) {
        return mRoutingDao.getSellerById(id);
    }

    public List<RoutingEntity> selectOnlyHopedNodesForInternetBySeller(String targetedAddress) {
        return mRoutingDao.selectOnlyHopedNodesForInternetBySeller(targetedAddress);
    }

    public List<RoutingEntity> selectOnlyHopedNodesForInternet() {
        return mRoutingDao.selectOnlyHopedNodesForInternet();
    }

    public List<String> getHopIds(String destinationAddress) {
        return mRoutingDao.getHopIds(destinationAddress);
    }
}
