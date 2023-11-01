package com.w3engineers.mesh.db.routing;

import androidx.room.Dao;
import androidx.room.Query;

import com.w3engineers.ext.strom.application.data.helper.local.base.BaseDao;
import com.w3engineers.mesh.linkcash.NodeInfo;

import java.util.List;

@Dao
public abstract class NodeInfoDao extends BaseDao<NodeInfo> {

    /**
     * <h1>Select specific NodeInfo</h1>
     *
     * @param userId : (required) String
     * @return : NodeInfo obj
     */
    @Query("SELECT * FROM nodeinfo WHERE user_id =:userId LIMIT 1")
    public abstract NodeInfo getNodeInfoById(String userId);

    /**
     * <h1>Get all Seller</h1>
     * Here hardcoded user_mode 1 means SELLER
     * Hadrcoded user_tyep 5 means internet user
     *
     * @return
     */
    @Query("SELECT * FROM nodeinfo WHERE user_mode = 1 AND user_type != 5")
    public abstract List<NodeInfo> getSellerList();

    /**
     * <h1>Delete all item from table</h1>
     */
    @Query("DELETE FROM nodeinfo")
    public abstract void deleteAllItem();

    @Query("DELETE FROM nodeinfo WHERE user_id=:userId")
    public abstract int deleteNodeInfoById(String userId);

    /**
     * <h1>Select node info</h1>
     * Select node info with bt name
     *
     * @param btName : (String) required
     * @return : NodeInfo
     */
   /* @Query("SELECT * FROM nodeinfo WHERE bt_name LIKE :btName LIMIT 1")
    public abstract NodeInfo getNodeInfoByBtName(String btName);


    @Query("SELECT * FROM nodeinfo WHERE ssid_name LIKE :ssid LIMIT 1")
    public abstract NodeInfo getNodeInfoBySSid(String ssid);*/
}
