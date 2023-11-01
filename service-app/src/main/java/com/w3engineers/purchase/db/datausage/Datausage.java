package com.w3engineers.purchase.db.datausage;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Datausage {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    public int id;

//    @ColumnInfo(name = "buyer_address")
//    public String buyerAddress;
//
//    @ColumnInfo(name = "seller_address")
//    public String sellerAddress;

    @ColumnInfo(name = "data_in_byte")
    public long dataByte;

    @ColumnInfo(name = "date")
    public long date;

    @ColumnInfo(name = "purpose")
    public int purpose;

    @ColumnInfo(name = "purchase_id")
    public int purchaseId;

}
