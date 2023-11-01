package com.letbyte.core.meshfilesharing.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.filepacket.FilePacketDao;

@Database(entities = {FilePacket.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FilePacketDao filePacketDao();
}
