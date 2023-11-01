package com.w3engineers.mesh.datasharing.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.w3engineers.mesh.datasharing.database.message.Message;
import com.w3engineers.mesh.datasharing.database.message.MessageDao;
import com.w3engineers.mesh.db.peers.PeersDao;
import com.w3engineers.mesh.db.peers.PeersEntity;
import com.w3engineers.mesh.db.routing.NodeInfoDao;
import com.w3engineers.mesh.db.routing.RoutingDao;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.db.users.UserDao;
import com.w3engineers.mesh.db.users.UserEntity;
import com.w3engineers.mesh.linkcash.NodeInfo;

/**
 * Database manager
 */
@Database(entities = { Message.class,
         RoutingEntity.class, UserEntity.class, PeersEntity.class, NodeInfo.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract RoutingDao getRoutingDao();
    public abstract UserDao getUserDao();
    public abstract PeersDao getPeersDao();
    public abstract NodeInfoDao getNodeInfoDao();
}

