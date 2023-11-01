package com.w3engineers.purchase.db.networkinfo;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.w3engineers.purchase.db.message.Message;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface NetworkInfoDao {

    @Query("SELECT * FROM NetworkInfo")
    List<NetworkInfo> getAll();

    @Query("SELECT * FROM NetworkInfo")
    Flowable<List<NetworkInfo>> getAllNetworkInfo();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(NetworkInfo... networkInfos);

    @Delete
    void delete(Message... delete);

    @Query("UPDATE NetworkInfo SET currency_amount = :currency, token_amount = :token WHERE network_type = :networkType")
    void updateCurrencyAndToken(int networkType, double currency, double token);

    @Query("UPDATE NetworkInfo SET currency_amount = :currency WHERE network_type = :networkType")
    void updateCurrency(int networkType, double currency);

    @Query("UPDATE NetworkInfo SET token_amount = :token WHERE network_type = :networkType")
    void updateToken(int networkType, double token);

    @Query("SELECT token_amount FROM NetworkInfo WHERE network_type = :networkType")
    Double getTokenByType(int networkType);

    @Query("SELECT currency_amount FROM NetworkInfo WHERE network_type = :networkType")
    Double getCurrencyByType(int networkType);
}
