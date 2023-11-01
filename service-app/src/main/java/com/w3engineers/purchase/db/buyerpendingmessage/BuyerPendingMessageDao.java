package com.w3engineers.purchase.db.buyerpendingmessage;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface BuyerPendingMessageDao {

    @Query("SELECT * FROM buyerpendingmessage WHERE msg_id =:msgID")
    public BuyerPendingMessage getMsgById(String msgID);

    @Query("SELECT * FROM buyerpendingmessage WHERE status =:status ORDER BY create_time ASC LIMIT 1")
    public BuyerPendingMessage getMsgByStatus(int status);

    @Query("SELECT * FROM buyerpendingmessage WHERE status =:status AND ( (is_incomming=1 AND owner =:userAddress) OR ( is_incomming=0 AND sender=:userAddress ))  ORDER BY create_time ASC LIMIT 1")
    public BuyerPendingMessage getBuyerPendingMessageByUser(int status, String userAddress);

    @Update
    void update(BuyerPendingMessage buyerPendingMessage);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long[] insertAll(BuyerPendingMessage... buyerPendingMessages);

    @Delete
    void delete(BuyerPendingMessage buyerPendingMessage);
}