package com.w3engineers.purchase.db.purchaserequests;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PurchaseRequestsDao {

    @Query("SELECT * FROM purchaserequests")
    List<PurchaseRequests> getAll();


    @Query("SELECT * FROM purchaserequests WHERE requester_address= :address AND state=:state ORDER BY nonce ASC LIMIT 1")
    PurchaseRequests getNext(String address, int state);

    @Query("SELECT * FROM purchaserequests WHERE rid= :id")
    PurchaseRequests getById(int id);

    @Update
    void update(PurchaseRequests purchaseRequests);

    @Insert
    long[] insertAll(PurchaseRequests... purchaseRequests);

    @Delete
    void delete(PurchaseRequests purchaseRequests);

    @Query("SELECT * FROM purchaserequests WHERE buyer_address= :buyerAddress AND request_value = :value AND request_type = :type AND state<=:state ORDER BY nonce ASC LIMIT 1")
    PurchaseRequests getPendingRequest(String buyerAddress, double value, int type, int state);

    @Query("SELECT * FROM purchaserequests WHERE buyer_address= :buyerAddress AND state<=:state")
    List<PurchaseRequests> getPendingRequest(String buyerAddress, int state);

    @Query("SELECT DISTINCT buyer_address FROM purchaserequests WHERE state =:state")
    List<String> getFailedRequestByUser(int state);

    @Query("SELECT * FROM purchaserequests WHERE state =:state")
    List<PurchaseRequests> getBuyerPendingRequest(int state);

    @Query("SELECT * FROM purchaserequests WHERE trx_hash= :hash")
    PurchaseRequests getRequestByHash(String hash);

    @Query("SELECT * FROM purchaserequests WHERE buyer_address= :buyerAddress AND state=:state ORDER BY nonce ASC")
    List<PurchaseRequests> getCompletedRequest(String buyerAddress, int state);

    @Query("SELECT * FROM purchaserequests WHERE requester_address= :requesterAddress AND state<=:state AND block_chain_endpoint= :endPoint")
    List<PurchaseRequests> getIncompleteRequestsByRequesterAddress(String requesterAddress, int state, int endPoint);

    @Query("SELECT * FROM purchaserequests WHERE message_id= :messageId")
    PurchaseRequests getRequestByMessageId(String messageId);
    //TODO create another method for seller pending request

}
