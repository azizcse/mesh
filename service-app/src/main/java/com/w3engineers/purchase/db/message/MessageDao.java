package com.w3engineers.purchase.db.message;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM message")
    List<Message> getAll();

    @Query("SELECT * FROM message WHERE receiverId= :receiverId")
    List<Message> getPendingMessage(String receiverId);

    @Query("SELECT * FROM message WHERE messageId= :id")
    Message getMessageById(String id);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Message... messages);

    @Delete
    void delete(Message... delete);

    @Query("DELETE FROM message WHERE messageId = :messageId")
    void deleteByMessageById(String messageId);

}
