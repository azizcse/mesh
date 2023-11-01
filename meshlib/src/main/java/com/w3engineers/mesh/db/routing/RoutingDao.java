package com.w3engineers.mesh.db.routing;

import android.text.TextUtils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.w3engineers.ext.strom.application.data.helper.local.base.BaseDao;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.meta.TableMeta;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Flowable;

/**
 * Routing DAO
 */
@Dao
public abstract class RoutingDao extends BaseDao<RoutingEntity> {

    String mMyNodeId;

    /**
     * Seems Room was not calling default constructor, rather it's own constructor. So we cooked
     * an initiating method
     */
    public void init(String nodeId) {
        mMyNodeId = nodeId;
    }

    /**
     * Return all online nodes except exception node id
     *
     * @param exceptionNodeId
     * @return
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != " +
            ":exceptionNodeId")
    protected abstract List<RoutingEntity> getAllOnline(String exceptionNodeId);

    /**
     * Return all online nodes except exception node id
     *
     * @param exceptionNodeId
     * @return
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 0 AND " + TableMeta.ColumnNames.ADDRESS + " != " +
            ":exceptionNodeId")
    protected abstract List<RoutingEntity> getAllOffLine(String exceptionNodeId);


    /**
     * Return all online nodes except exception node id
     *
     * @param exceptionNodeId
     * @return
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.ADDRESS + " != :exceptionNodeId AND "
            + TableMeta.ColumnNames.TYPE + "!=" + RoutingEntity.Type.WiFi)
    protected abstract List<RoutingEntity> getOnlineEntityExceptWifi(String exceptionNodeId);

    /**
     * Return all online nodes except my node
     *
     * @return
     */
    public List<RoutingEntity> getAllOnline() {
        return getAllOnline(mMyNodeId);
    }

    /**
     * Return all online nodes except my node
     *
     * @return
     */
    public List<RoutingEntity> getAllOffline() {
        return getAllOffLine(mMyNodeId);
    }

    public List<RoutingEntity> onlineEntityExceptWifi() {
        return getOnlineEntityExceptWifi(mMyNodeId);
    }

    /**
     * Return entity based on address without any kind of validation
     *
     * @param address
     * @return
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :address AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1")
    public abstract RoutingEntity getUnValidatedEntity(String address);

    @Transaction
    public RoutingEntity getOnlineRoutingEntity(String address) {
        RoutingEntity routingEntity = getUnValidatedEntity(address);

//        if (routingEntity != null && routingEntity.isOnline()) {
//            return routingEntity;
//        }

        return routingEntity;
    }

    @Transaction
    public RoutingEntity getLocalOnlineRoutingEntity(String address) {
        RoutingEntity routingEntity = getUnValidatedEntity(address);

        if (routingEntity != null && routingEntity.isOnline() && routingEntity.getType() !=
                RoutingEntity.Type.INTERNET) {
            return routingEntity;
        }

        return null;
    }

    /**
     * Fetch all nodes of given type which are online, all nodes which use this nodes as hop node
     *
     * @param myNodeId
     * @return
     */
    // Can below subquery be brought under same umbrella?
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.NEXT_HOP + " IN " +
            "(SELECT " + TableMeta.ColumnNames.ADDRESS + " FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.TYPE
            + " = " + RoutingEntity.Type.WiFi + " AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId" +
            ") AND (" + TableMeta.ColumnNames.NEXT_HOP + " IS NULL  OR " + TableMeta.ColumnNames.NEXT_HOP + "='')" +//Hop addresses
            " UNION  " +
            " SELECT * FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.TYPE +
            " = " + RoutingEntity.Type.WiFi + " AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId " +
            " AND (" + TableMeta.ColumnNames.NEXT_HOP + " IS NULL OR " + TableMeta.ColumnNames.NEXT_HOP + "='')")
//direct nodes
    protected abstract List<RoutingEntity> getConnectedWiFiEntities(String myNodeId);

    @Query("SELECT " + TableMeta.ColumnNames.IP + " FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.TYPE + " = :type AND " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
//direct nodes
    protected abstract List<String> getConnectedIpOf(int type, String myNodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.TYPE + " = :type AND " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
//direct nodes
    protected abstract List<RoutingEntity> getConnectedEntitiesByType(int type, String myNodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.TYPE + "=" + RoutingEntity.Type.BT + " OR " +
            TableMeta.ColumnNames.TYPE + "=" + RoutingEntity.Type.BtMesh + " AND " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
//direct nodes
    protected abstract List<RoutingEntity> getAllBtUsers(String myNodeId);


    public List<RoutingEntity> getAllBtUsers() {
        return getAllBtUsers(mMyNodeId);
    }

    public List<RoutingEntity> getConnectedWiFiEntities() {

        String myNodeId = mMyNodeId;
        return getConnectedWiFiEntities(myNodeId);
    }

    public List<String> getConnectedIp(int type) {

        String myNodeId = mMyNodeId;
        return getConnectedIpOf(type, myNodeId);
    }

    public List<RoutingEntity> getConnectedEntitiesByType(int type) {

        String myNodeId = mMyNodeId;
        return getConnectedEntitiesByType(type, myNodeId);
    }


    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IP + " = :ip AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = :type")
    public abstract RoutingEntity getRoutingEntityByIp(String ip, int type);

    /*@Query("SELECT COUNT(" + TableMeta.ColumnNames.USER_HASH + ")" +
            " FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.USER_HASH + " = :hash")
    public abstract int checkUserHashExists(int hash);*/

    /**
     * Set all WiFi and WiFi via entities as offline.
     *
     * @return Affected entitiesrows
     */
    @Transaction
    public List<RoutingEntity> updateWiFiNodeAsOffline() {
        List<RoutingEntity> routingEntities = getAllWifiUsers();

        if (CollectionUtil.hasItem(routingEntities)) {

            for (RoutingEntity routingEntity : routingEntities) {
                routingEntity.resetMetaData();
            }

            int updateCount = update(routingEntities);

            return routingEntities;
        }

        return null;
    }

    /**
     * @return Return the given node with all the nodes of that nodes hopped node
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND (" + TableMeta.ColumnNames.ADDRESS + " = :targetedAddress OR " +
            TableMeta.ColumnNames.NEXT_HOP + " = :targetedAddress)")
    protected abstract List<RoutingEntity> selectNodesWithHopedNodes(String targetedAddress);

    /**
     * @return Return the given node with all the nodes of that nodes hopped node
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND  " +
            TableMeta.ColumnNames.NEXT_HOP + " = :targetedAddress")
    protected abstract List<RoutingEntity> selectOnlyHopedNodes(String targetedAddress);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND  " +
            TableMeta.ColumnNames.NEXT_HOP + " = :targetedAddress AND "
            + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.INTERNET)
    public abstract List<RoutingEntity> selectOnlyHopedNodesForInternetBySeller(String targetedAddress);


    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND  " +
            TableMeta.ColumnNames.NEXT_HOP + " IS NOT NULL AND "
            + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.INTERNET)
    public abstract List<RoutingEntity> selectOnlyHopedNodesForInternet();


    /**
     * This method normally used to validate any update request. We check whether the update request
     * is authorized. The node who is requesting the update is really the valid owner to request
     *
     * @param fromAddress
     * @param targetAddress
     * @return
     */
    //Kept this query separate this is reusable and logic configurable for future changes
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :targetAddress AND " +
            TableMeta.ColumnNames.NEXT_HOP + " = :fromAddress")
    protected abstract RoutingEntity getValidatedEntity(String fromAddress, String targetAddress);

    /**
     * If the hop address is null or a direct connected node
     * And it will work only for local user
     *
     * @param targetAddress
     * @return
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :targetAddress AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.TYPE + " != " + RoutingEntity.Type.INTERNET + " AND " +
            TableMeta.ColumnNames.NEXT_HOP + " IS NULL OR " + TableMeta.ColumnNames.NEXT_HOP + "=''")

    protected abstract RoutingEntity getValidatedEntity(String targetAddress);


    /**
     * Update {@code targetAddress} node and all nodes hoped by {@code targetAddress} node as
     * offline. It validate whether {@code fromAddress} has the authority to update
     * {@code targetAddress} as offline. If {@code fromAddress} is null then we validate that
     * {@code targetAddress} is directly connected or not.
     *
     * @param fromAddress
     * @param targetAddress
     * @return
     */
    @Transaction
    public List<RoutingEntity> updateAsOffline(String fromAddress, String targetAddress) {
        if (Text.isNotEmpty(targetAddress) && !mMyNodeId.equals(targetAddress)) {

            RoutingEntity r = TextUtils.isEmpty(fromAddress) ? getValidatedEntity(targetAddress) : getValidatedEntity(fromAddress, targetAddress);
            if (r != null) {
                List<RoutingEntity> routingEntities = new ArrayList<>();

                List<Integer> allPossiblePathOfTargetNode = getAllPossibleTypeById(targetAddress);

                if (TextUtils.isEmpty(fromAddress)) {
                    List<RoutingEntity> hopeNodeList = selectOnlyHopedNodes(targetAddress);

                    //Check has any internet user
                    for (RoutingEntity entity : hopeNodeList) {
                        // this node is seller or not and has any other connection or not
                        if (entity.getUserMode() == PreferencesHelper.DATA_SELLER) {
                            List<Integer> sellerMultiplePaths = getAllPossibleTypeById(entity.getAddress());
                            if (sellerMultiplePaths.size() == 1) {
                                routingEntities.addAll(selectOnlyHopedNodesForInternetBySeller(entity.getAddress()));
                            }

                            routingEntities.add(entity);
                        } else if (entity.getType() == RoutingEntity.Type.INTERNET) {
                            // If target address has only one path then we will disconnect internet user
                            if (allPossiblePathOfTargetNode.size() == 1) {
                                routingEntities.add(entity);
                            }

                        } else {
                            routingEntities.add(entity);
                        }
                    }

                } else {
                    if (r.getUserMode() == PreferencesHelper.DATA_SELLER
                            && allPossiblePathOfTargetNode.size() == 1) {

                        routingEntities.addAll(selectOnlyHopedNodesForInternetBySeller(r.getAddress()));
                    }
                    // Check targetAddress is Seller or not
                    // if multiple path not exists disconnect all chile under target address
                }

                routingEntities.add(r);

                if (CollectionUtil.hasItem(routingEntities)) {

                    for (RoutingEntity routingEntity : routingEntities) {
                        routingEntity.resetMetaData();
                    }

                    int updateCount = update(routingEntities);

                    return routingEntities;
                }
            }
        }

        return null;
    }


    public List<RoutingEntity> getAllRoutingEntityWithHopId(String targetAddress) {
        return selectNodesWithHopedNodes(targetAddress);
    }


    /**
     * get details of the hopNode for destination node
     *
     * @param destinationAddress
     * @return null if no hop available
     */
    @Query("SELECT " + TableMeta.ColumnNames.NEXT_HOP +
            " FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :destinationAddress AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 ")
    public abstract List<String> getHopIds(String destinationAddress);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract long insert(RoutingEntity routingEntity);

    /**
     * Insert entry if a new entry. If already exist then update. Always check time condition with
     * threshold value of {@value Constant.Cycle#MESH_USER_DELAY_THRESHOLD}
     *
     * @param routingEntity
     * @return
     */
    @Transaction
    public boolean upsert(RoutingEntity routingEntity) {
        MeshLog.e("[ID VALIDATION] myID::" + AddressUtil.makeShortAddress(mMyNodeId) + " updatedID::" + AddressUtil.makeShortAddress(routingEntity.getAddress()));
        if (mMyNodeId.equals(routingEntity.mAddress)) {
            return false;
        }
        RoutingEntity existingRoutingEntity = getUnValidatedEntity(routingEntity.mAddress);

        //No entry so insert
        if (existingRoutingEntity == null) {
            long row = insert(routingEntity);
            return row != -1;
        }

        //Already exist. So update
        //Put condition at Java layer rather SQLite, because if update is done from Query annotation
        // then it does not always contribute towards reactive upward stream

        //Time can be +/- of Threshold seconds
        long timeDiff = routingEntity.mTime - existingRoutingEntity.mTime;
        if (timeDiff < 0) {
            timeDiff = -timeDiff;
        }

        //We do not apply any condition if it is to make online while current db status is offline
        //No older routing entry is allowed than earlier entry
        //We block any older routing entry
        if ((routingEntity.isOnline() && !existingRoutingEntity.isOnline()) || routingEntity.mTime > existingRoutingEntity.mTime ||
                //or difference maximum can be fo threshold seconds if clock mismatch by two devices
                timeDiff < Constant.Cycle.MESH_USER_DELAY_THRESHOLD) {

            int updateCount = update(routingEntity);
            return updateCount > 0;

        } else if (!(routingEntity.mTime > existingRoutingEntity.mTime) || !(timeDiff < Constant.Cycle.MESH_USER_DELAY_THRESHOLD)) {

            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).
                    format(new Date(existingRoutingEntity.mTime));
            MeshLog.w("[Cycle-message] packet arrived with delay by:" + (timeDiff / 1000) + "seconds. " +
                    "Existing time:" + time +
                    " Packet:" + routingEntity.toString());
            return false;
        }
        return false;
    }

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " AS TYPE_USERS WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.WiFi + " AND " +
            TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
    protected abstract List<RoutingEntity> getWifiConnected(String myNodeId);

    public List<RoutingEntity> getWifiConnected() {
        String myNodeId = mMyNodeId;
        return getWifiConnected(myNodeId);
    }


    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " AS TYPE_USERS WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.BT)
    public abstract List<RoutingEntity> isBtConnected();

    @Query("DELETE FROM " + TableMeta.TableNames.ROUTING)
    public abstract void deleteAll();

    @Query("SELECT " + TableMeta.ColumnNames.TYPE + " FROM " +
            TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :nodeID AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1")
    public abstract int getLinkTypeById(String nodeID);

    @Query("SELECT " + TableMeta.ColumnNames.TYPE + " FROM " +
            TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :nodeID")
    public abstract List<Integer> getMultipleLinkTypeById(String nodeID);

    @Query("SELECT " + TableMeta.ColumnNames.TYPE + " FROM " +
            TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :nodeID AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1")
    public abstract List<Integer> getMultipleLinkTypeByIdDebug(String nodeID);

    @Query("SELECT " + TableMeta.ColumnNames.IS_ONLINE + " FROM " +
            TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :nodeId")
    public abstract int isThisUserIsInOnline(String nodeId);

    @Query("SELECT " + TableMeta.ColumnNames.IS_ONLINE + " FROM " +
            TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :nodeId" + " AND " +
            TableMeta.ColumnNames.TYPE + "=" + RoutingEntity.Type.WiFi)
    public abstract int isThisWifiUserIsInOnline(String nodeId);


    @Query("UPDATE " + TableMeta.TableNames.ROUTING + " SET " + TableMeta.ColumnNames.IS_ONLINE + " =0 , "
            + TableMeta.ColumnNames.NEXT_HOP + "='' , "
            + TableMeta.ColumnNames.IP + " = null "
            + " WHERE " + TableMeta.ColumnNames.TYPE + " = "
            + RoutingEntity.Type.INTERNET + " AND " + TableMeta.ColumnNames.ADDRESS + " IN (:list)")
    public abstract int makeInternetUserOffline(String[] list);

    @Query("UPDATE " + TableMeta.TableNames.ROUTING + " SET " + TableMeta.ColumnNames.IS_ONLINE + " =0 , "
            + TableMeta.ColumnNames.NEXT_HOP + "='' , "
            + TableMeta.ColumnNames.IP + " = null "
            + " WHERE " + TableMeta.ColumnNames.TYPE + " = "
            + RoutingEntity.Type.INTERNET + " AND " + TableMeta.ColumnNames.NEXT_HOP + " = :senderId AND "
            + TableMeta.ColumnNames.ADDRESS + " IN (:list)")
    public abstract int makeInternetUserOffline(String senderId, List<String> list);


    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.BT + " AND " +
            TableMeta.ColumnNames.ADDRESS + " = :hopNodeId")
    public abstract RoutingEntity isBtHopIdExistInOnline(String hopNodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.WiFi + " AND " +
            TableMeta.ColumnNames.ADDRESS + " = :hopNodeId")
    public abstract RoutingEntity isWifiHopIdExistInOnline(String hopNodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = :interfaceType AND " + TableMeta.ColumnNames.ADDRESS +
            " = :nodeId AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
    protected abstract RoutingEntity isNodeConnectedIn(String nodeId, int interfaceType, String myNodeId);

    public boolean isNodeConnectedIn(String nodeId, int interfaceType) {
        return isNodeConnectedIn(nodeId, interfaceType, mMyNodeId) != null;
    }

    @Query("SELECT " + TableMeta.ColumnNames.TYPE + " FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :address")
    public abstract int getReceiverAddressType(String address);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING)
    public abstract Flowable<List<RoutingEntity>> getAll();

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.TYPE + "=" + RoutingEntity.Type.WiFi + " OR " +
            TableMeta.ColumnNames.TYPE + "=" + RoutingEntity.Type.WifiMesh + " AND " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
    //direct nodes
    protected abstract List<RoutingEntity> getAllWifiUsers(String myNodeId);

    public List<RoutingEntity> getAllWifiUsers() {
        return getAllWifiUsers(mMyNodeId);
    }

    /**
     * @return Return the given node with all the nodes of that nodes hopped node
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND (" + TableMeta.ColumnNames.ADDRESS + " = :targetedAddress OR " +
            TableMeta.ColumnNames.NEXT_HOP + " = :targetedAddress)")
    public abstract List<RoutingEntity> getNodesWithHopedNodes(String targetedAddress);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND " + TableMeta.ColumnNames.NEXT_HOP + " = :hopAddress AND " +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.INTERNET)
    public abstract List<RoutingEntity> getAllDisconnectedInternetUser(String hopAddress);

    // fixme: 10/17/2019 should be with offline query in a single transaction
    public void makeUserOffline(RoutingEntity routingEntity) {
        routingEntity.resetMetaData();
        update(routingEntity);
    }

   /* @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING +
            " WHERE " + TableMeta.ColumnNames.TYPE + "=" + RoutingEntity.Type.BtMesh + " AND " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != :myNodeId AND "
            + TableMeta.ColumnNames.WRITE_TIME + " BETWEEN :startTime AND :endTime")
    //bt mesh nodes
    public abstract List<RoutingEntity> getBtMeshUserByTimeRange(String myNodeId, long startTime, long endTime);*/


    /**
     * @return Return the given node with all the nodes of that nodes hopped node
     */
    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE +
            "= 1 AND (" + TableMeta.ColumnNames.IP + " = :IpAddress)")
    public abstract RoutingEntity getNodeByIP(String IpAddress);


    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " AS TYPE_USERS WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.HB + " AND " +
            TableMeta.ColumnNames.ADDRESS + " != :myNodeId")
    protected abstract List<RoutingEntity> isAdhocConnected(String myNodeId);

    public List<RoutingEntity> getAdhocConnected() {
        return isAdhocConnected(mMyNodeId);
    }

    /**
     * Set all WiFi and WiFi via entities as offline.
     *
     * @return Affected entitiesrows
     */
    @Transaction
    public List<RoutingEntity> updateOnlineNodeAsOffline() {
        List<RoutingEntity> routingEntities = getAllOnline();

        if (CollectionUtil.hasItem(routingEntities)) {

            for (RoutingEntity routingEntity : routingEntities) {
                routingEntity.resetMetaData();
                routingEntity.setType(0);
            }
            int updateCount = update(routingEntities);
            return routingEntities;
        }
        return null;
    }

    @Transaction
    public List<RoutingEntity> updateOnlineNodeAsOfflineOnlyWifi() {
        List<RoutingEntity> routingEntities = getAllOnline();

        if (CollectionUtil.hasItem(routingEntities)) {

            for (RoutingEntity routingEntity : routingEntities) {
                if (routingEntity.getType() == RoutingEntity.Type.WiFi
                        || routingEntity.getType() == RoutingEntity.Type.WifiMesh) {
                    routingEntity.resetMetaData();
                }
            }
            int updateCount = update(routingEntities);
            return routingEntities;
        }
        return null;
    }

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE (" +
            TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.BT +
            " OR " + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.BLE +
            " OR " + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.WiFi +
            " OR " + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.HB +
            ") AND (" + TableMeta.ColumnNames.IS_ONLINE + " = 1 ) AND (" + TableMeta.ColumnNames.ADDRESS + " != :myNodeId)")
    //direct nodes
    protected abstract List<RoutingEntity> getConnectedDirectUsers(String myNodeId);

    public List<RoutingEntity> getConnectedDirectUsers() {
        return getConnectedDirectUsers(mMyNodeId);
    }

    //Mesh v2 implementation

    @Query("SELECT " + TableMeta.ColumnNames.ADDRESS + " FROM " + TableMeta.TableNames.ROUTING)
    public abstract List<String> getAllNodeIds();

    @Query("SELECT " + TableMeta.ColumnNames.ADDRESS + " FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " != " +
            ":exceptionNodeId")
    protected abstract List<String> getOnlineNodeIds(String exceptionNodeId);


    public List<String> getAllOnlineNodeIds() {
        return getOnlineNodeIds(mMyNodeId);
    }

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :nodeId AND " + TableMeta.ColumnNames.IS_ONLINE + "= 1 LIMIT 1")
    public abstract RoutingEntity getEntityByAddress(String nodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.IP + " = " + ":goIp LIMIT 1")
    public abstract RoutingEntity getGoEntity(String goIp);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING + " WHERE "
            + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " = :nodeId")
    public abstract RoutingEntity getShortestPath(String nodeId);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING + " WHERE "
            + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.ADDRESS + " = :nodeId AND "
            + TableMeta.ColumnNames.TYPE + " !=:excludeType")
    public abstract RoutingEntity getShortestPathExcludeByType(String nodeId, int excludeType);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING
            + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND " + TableMeta.ColumnNames.TYPE
            + " !=" + RoutingEntity.Type.INTERNET + " GROUP BY " + TableMeta.ColumnNames.ADDRESS)
    public abstract List<RoutingEntity> getAllOnlineUserWithMinimumHop();

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :nodeId AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC")
    public abstract List<RoutingEntity> getAllPossibleOnlinePathById(String nodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :nodeId ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC")
    public abstract List<RoutingEntity> getAllPossiblePathById(String nodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :nodeId AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.TYPE + " =:type ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC")
    public abstract List<RoutingEntity> getAllPossiblePathByIdAndType(String nodeId, int type);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE "
            + TableMeta.ColumnNames.ADDRESS + " = :destinationId AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.NEXT_HOP + " = :hopId LIMIT 1")
    public abstract RoutingEntity getRoutByDestinationAndHop(String destinationId, String hopId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE "
            + TableMeta.ColumnNames.ADDRESS + " = :destinationId AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.NEXT_HOP + " = :hopId AND "+TableMeta.ColumnNames.TYPE +" = :type LIMIT 1")
    public abstract RoutingEntity getRoutByDestinationAndHopAndType(String destinationId, String hopId, int type);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING
            + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.TYPE + "!=:type GROUP BY " + TableMeta.ColumnNames.ADDRESS)
    public abstract List<RoutingEntity> getAllUserExcludeByType(int type);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING
            + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.TYPE + "=:type GROUP BY " + TableMeta.ColumnNames.ADDRESS)
    public abstract List<RoutingEntity> getAllUserByType(int type);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING
            + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.BLE + " OR "
            + TableMeta.ColumnNames.TYPE + " = " + RoutingEntity.Type.BLE_MESH + "  GROUP BY " + TableMeta.ColumnNames.ADDRESS)
    public abstract List<RoutingEntity> getAllBleAndBleMeshUser();

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS + " = :nodeId LIMIT 1")
    public abstract RoutingEntity getArbitraryPath(String nodeId);

/*    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :nodeId AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC")
    public abstract List<RoutingEntity> getAllPossiblePathById(String nodeId);*/

/*    @Query("SELECT " + TableMeta.ColumnNames.ROW_ID + " , " + TableMeta.ColumnNames.ADDRESS + " , " +TableMeta.ColumnNames.TYPE + " , " + TableMeta.ColumnNames.NEXT_HOP + " , " + TableMeta.ColumnNames.HOP_COUNT + " FROM " +
            TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1")
    public abstract Flowable<List<RoutingEntity>> getAllLinkPath();*/

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1")
    public abstract Flowable<List<RoutingEntity>> getAllLinkPath();

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE " +
            TableMeta.ColumnNames.ADDRESS + " = :userId AND " +
            TableMeta.ColumnNames.TYPE + " = :userConnectionTYpe AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC LIMIT 1")
    public abstract RoutingEntity getSingleUserInfoByType(String userId, int userConnectionTYpe);

    @Query("SELECT *,MIN(" + TableMeta.ColumnNames.HOP_COUNT + ") as "
            + TableMeta.ColumnNames.HOP_COUNT + " FROM " + TableMeta.TableNames.ROUTING
            + " WHERE " + TableMeta.ColumnNames.IS_ONLINE + " = 1 AND "
            + TableMeta.ColumnNames.TYPE + "!= " + RoutingEntity.Type.INTERNET + " AND " +
            TableMeta.ColumnNames.COL_USER_MODE + " = " + PreferencesHelper.DATA_SELLER
            + " GROUP BY " + TableMeta.ColumnNames.ADDRESS)
    public abstract List<RoutingEntity> getAllSellerList();


    @Query("SELECT " + TableMeta.ColumnNames.TYPE + " FROM " + TableMeta.TableNames.ROUTING + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :nodeId AND " + TableMeta.ColumnNames.IS_ONLINE + " = 1 ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC")
    public abstract List<Integer> getAllPossibleTypeById(String nodeId);

    @Query("SELECT * FROM " + TableMeta.TableNames.ROUTING + " WHERE "
            + TableMeta.ColumnNames.ADDRESS + " = :nodeId AND " + TableMeta.ColumnNames.IS_ONLINE
            + " = 1 AND " + TableMeta.ColumnNames.COL_USER_MODE + " = " + PreferencesHelper.DATA_SELLER + " AND "
            + TableMeta.ColumnNames.TYPE + " !=" + RoutingEntity.Type.INTERNET
            + " ORDER BY " + TableMeta.ColumnNames.HOP_COUNT + " ASC LIMIT 1")
    public abstract RoutingEntity getSellerById(String nodeId);
}


