package com.w3engineers.purchase.db.content;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.purchase.db.message.Message;

import java.util.List;

@Dao
public interface ContentDao {

    @Query("SELECT * FROM content WHERE app_token= :appToken ")
    List<Content> getAllPendingContent(String appToken);

    @Query("SELECT * FROM content WHERE contentId= :contentId")
    Content getContentMessageByContentId(String contentId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Content content);

    @Query("DELETE FROM content WHERE app_token = :appToken AND is_incoming = :isIncoming")
    void deleteContentsByAppToken(String appToken, boolean isIncoming);

    @Query("DELETE FROM content WHERE app_token = :appToken AND contentId = :contentId")
    void deleteContentsByContentId(String appToken, String contentId);

    @Query("DELETE FROM content WHERE app_token = :appToken AND contentId = :contentId AND is_incoming = :isIncoming")
    void deleteIncomingContentsByContentId(String appToken, String contentId, boolean isIncoming);

    @Query("UPDATE content SET state = :toState WHERE senderId = :userId AND state = :fromState")
    void updateContentStatusByUserId(String userId, int fromState, int toState);

    @Query("UPDATE content SET state = :toState WHERE state = :fromState")
    void updateAllContentStatus(int fromState, int toState);

}
