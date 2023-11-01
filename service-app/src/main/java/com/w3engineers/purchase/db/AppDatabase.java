package com.w3engineers.purchase.db;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.w3engineers.purchase.db.appupdateappinfo.AppUpdateInfoDao;
import com.w3engineers.purchase.db.appupdateappinfo.AppUpdateInfoEntity;
import com.w3engineers.purchase.db.broadcast.BroadcastDao;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;
import com.w3engineers.purchase.db.broadcast_track.BroadcastTrackDao;
import com.w3engineers.purchase.db.broadcast_track.BroadcastTrackEntity;
import com.w3engineers.purchase.db.buyerpendingmessage.BuyerPendingMessage;
import com.w3engineers.purchase.db.buyerpendingmessage.BuyerPendingMessageDao;
import com.w3engineers.purchase.db.clientinfo.ClientInfoDao;
import com.w3engineers.purchase.db.clientinfo.ClientInfoEntity;
import com.w3engineers.purchase.db.content.Content;
import com.w3engineers.purchase.db.content.ContentDao;
import com.w3engineers.purchase.db.datausage.Datausage;
import com.w3engineers.purchase.db.datausage.DatausageDao;
import com.w3engineers.purchase.db.handshaking_track.HandshakeTrackDao;
import com.w3engineers.purchase.db.handshaking_track.HandshakeTrackEntity;
import com.w3engineers.purchase.db.message.Message;
import com.w3engineers.purchase.db.message.MessageDao;
import com.w3engineers.purchase.db.networkinfo.NetworkInfo;
import com.w3engineers.purchase.db.networkinfo.NetworkInfoDao;
import com.w3engineers.purchase.db.purchase.Purchase;
import com.w3engineers.purchase.db.purchase.PurchaseDao;
import com.w3engineers.purchase.db.purchaserequests.PurchaseRequests;
import com.w3engineers.purchase.db.purchaserequests.PurchaseRequestsDao;


@Database(entities = {Purchase.class, PurchaseRequests.class, Datausage.class, Message.class,
        BuyerPendingMessage.class, NetworkInfo.class, Content.class, AppUpdateInfoEntity.class,
        BroadcastEntity.class, BroadcastTrackEntity.class, ClientInfoEntity.class, HandshakeTrackEntity.class},
        version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PurchaseDao purchaseDao();

    public abstract PurchaseRequestsDao purchaseRequestsDao();

    public abstract DatausageDao datausageDao();

    public abstract MessageDao messageDao();

    public abstract BuyerPendingMessageDao buyerPendingMessageDao();

    public abstract NetworkInfoDao getNetworkInfoDao();

    public abstract ContentDao getContentDao();

    public abstract AppUpdateInfoDao appUpdateInfoDao();

    public abstract ClientInfoDao clientInfoDao();

    public abstract BroadcastDao broadcastDao();

    public abstract BroadcastTrackDao broadcastTrackDao();

    public abstract HandshakeTrackDao handshakeTrackDao();
}

