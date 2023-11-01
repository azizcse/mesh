package com.w3engineers.mesh.db.peers;


/**
 * Peers DAO
 */

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.mesh.db.meta.TableMeta;

import java.util.List;

@Dao
public interface PeersDao {
    @Query("SELECT * FROM " + TableMeta.TableNames.PEERS)
    List<PeersEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(PeersEntity... userEntities);

    @Query("SELECT * FROM " + TableMeta.TableNames.PEERS + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :id AND " + TableMeta.ColumnNames.APP_TOKEN + " = :token")
    PeersEntity getPeersByIdAndToken(String id, String token);

    @Query("SELECT * FROM " + TableMeta.TableNames.PEERS + "  WHERE " + TableMeta.ColumnNames.ADDRESS
            + " != :id AND " + TableMeta.ColumnNames.IS_ONLINE + " = :status AND "
            + TableMeta.ColumnNames.APP_TOKEN + " = :appToken")
    List<PeersEntity> getAllOnlinePeers(String id, boolean status, String appToken);

    @Query("SELECT * FROM " + TableMeta.TableNames.PEERS + "  WHERE " + TableMeta.ColumnNames.IS_ME
            + " = :isMe")
    List<PeersEntity> getSelfPeers(boolean isMe);

    @Query("UPDATE " + TableMeta.TableNames.PEERS + " SET " + TableMeta.ColumnNames.IS_ONLINE
            + " = :status WHERE " + TableMeta.ColumnNames.ADDRESS + " = :id")
    void updateOnlineStatusById(String id, boolean status);

    @Query("UPDATE " + TableMeta.TableNames.PEERS + " SET " + TableMeta.ColumnNames.IS_ONLINE + " = :status")
    void updateAllPeersOnlineStatus(boolean status);

    @Query("SELECT " + TableMeta.ColumnNames.PUBLIC_KEY + " FROM " + TableMeta.TableNames.PEERS + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :id")
    String getPeersPublicKey(String id);

    @Query("SELECT COUNT(DISTINCT " + TableMeta.ColumnNames.ADDRESS + ") FROM " + TableMeta.TableNames.PEERS + " where " + TableMeta.ColumnNames.ADDRESS + " != :id")
    LiveData<Integer> getPeersCount(String id);

    @Query("UPDATE " + TableMeta.TableNames.PEERS + " SET " + TableMeta.ColumnNames.USER_APP_VERSION
            + " = :appVersion WHERE address = :id AND " + TableMeta.ColumnNames.APP_TOKEN + " = :appToken")
    void updatePeersAppVersion(String id, String appToken, int appVersion);

    @Query("UPDATE " + TableMeta.TableNames.PEERS + " SET " + TableMeta.ColumnNames.ADDRESS
            + " = :walletAddress AND " + TableMeta.ColumnNames.PUBLIC_KEY + " = :pubKey AND "
            + TableMeta.ColumnNames.APP_USER_INFO + " = :userInfo" + " WHERE " + TableMeta.ColumnNames.ADDRESS + " = :id AND "
            + TableMeta.ColumnNames.APP_TOKEN + " = :appToken")
    void updateSelfPeerInfo(String walletAddress, String pubKey, String userInfo, String id, String appToken);

    @Query("DELETE FROM " + TableMeta.TableNames.PEERS + " WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :walletAddress AND " + TableMeta.ColumnNames.APP_TOKEN + " = :appToken")
    void deleteOldSelfInfo(String walletAddress, String appToken);

    @Query("SELECT * FROM " + TableMeta.TableNames.PEERS + " WHERE " + TableMeta.ColumnNames.ADDRESS + " = :id")
    PeersEntity getFirstPeerById(String id);

    @Query("SELECT DISTINCT " + TableMeta.ColumnNames.ADDRESS + " FROM " + TableMeta.TableNames.PEERS
            + " where " + TableMeta.ColumnNames.ADDRESS + " != :id AND " + TableMeta.ColumnNames.IS_ONLINE + " = :status")
    List<String> getOnlineUserAddress(String id, boolean status);

    @Query("SELECT " + TableMeta.ColumnNames.ADDRESS + " FROM " + TableMeta.TableNames.PEERS + " WHERE "
            + TableMeta.ColumnNames.ADDRESS + " IN (:whereCl)")
    List<String> getValidUserAddress(List<String> whereCl);

    @Query("SELECT * FROM " + TableMeta.TableNames.PEERS + "  WHERE " + TableMeta.ColumnNames.ADDRESS
            + " = :peerId")
    List<PeersEntity> getAllPeerById(String peerId);
}
