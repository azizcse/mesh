package com.w3engineers.purchase.db.clientinfo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.purchase.db.TableInfo;

import java.util.List;

@Dao
public interface ClientInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertClintInformation(ClientInfoEntity entity);

    @Query("SELECT * FROM " + TableInfo.CLIENT_INFO)
    List<ClientInfoEntity> getAllClientInformation();

    @Query("SELECT * FROM " + TableInfo.CLIENT_INFO + " WHERE " + TableInfo.Column.APP_TOKEN
            + " LIKE :appToken")
    ClientInfoEntity getClientInfoByToken(String appToken);
}
