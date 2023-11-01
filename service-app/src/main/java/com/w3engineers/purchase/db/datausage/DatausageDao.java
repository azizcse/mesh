package com.w3engineers.purchase.db.datausage;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DatausageDao {
    @Query("SELECT SUM(data_in_byte) FROM Datausage WHERE date >= :fromDate")
    public abstract LiveData<Long> getDataUsage(long fromDate);

    @Query("SELECT SUM(data_in_byte) FROM Datausage WHERE date >= :fromDate")
    public Long getUsedData(long fromDate);

    @Update
    void updatePurchase(Datausage datausage);

    @Insert
    void insertAll(Datausage... datausages);

    @Delete
    void delete(Datausage datausage);
}
