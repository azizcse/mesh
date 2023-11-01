package com.w3engineers.mesh.datasharing.database.message;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Message DAO
 */
@Dao
public interface MessageDao {

    @Query("SELECT * FROM message WHERE receiverId= :receiverId AND is_incoming= :isInComing")
    List<Message> getPendingMessage(String receiverId, boolean isInComing);

    @Query("SELECT * FROM message WHERE messageId= :id")
    Message getMessageById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Message... messages);

    @Query("DELETE FROM message WHERE messageId = :messageId")
    void deleteByMessageById(String messageId);

    @Query("SELECT * FROM message WHERE is_incoming= :isIncoming AND app_token = :appToken")
    List<Message> getAllIncomingPendingMessage(boolean isIncoming, String appToken);
}
