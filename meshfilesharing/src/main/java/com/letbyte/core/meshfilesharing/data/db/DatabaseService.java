package com.letbyte.core.meshfilesharing.data.db;

import android.content.Context;

import androidx.room.Room;

import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.TableMeta;
import com.letbyte.core.meshfilesharing.data.filepacket.FilePacketDao;
import com.w3engineers.mesh.TransportManagerX;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseService {
    private static DatabaseService sDbService;
    private AppDatabase mDatabase;
    private ExecutorService mExecutorService;
    private Context mContext;
    private FilePacketDao filePacketDao;

    private DatabaseService(Context context) {
        this.mContext = context;
        mExecutorService = Executors.newFixedThreadPool(1);
        mDatabase = Room.databaseBuilder(mContext, AppDatabase.class, TableMeta.DB_NAME).allowMainThreadQueries().build();
        filePacketDao = mDatabase.filePacketDao();
    }

    public static DatabaseService getInstance(Context context) {
        if (sDbService == null) {
            sDbService = new DatabaseService(context);
        }
        return sDbService;
    }

    public void insertFilePacket(FilePacket packet) {
        mExecutorService.execute(() -> filePacketDao.insertFilePacket(packet));
    }

    public void updateFilePacket(FilePacket packet) {
        mExecutorService.execute(() -> filePacketDao.updateFilePacket(packet));
    }

    public void updateFailedPackets() {
        mExecutorService.execute(() -> filePacketDao.updateFailedPackets());
    }

    /**
     * Update any to or from of this address files as failed
     * @param nodeId
     */
    public List<FilePacket> updateFilesFailed(String nodeId) {
        return filePacketDao.updateFailedFor(nodeId);
    }

    public FilePacket getOlderFilePackets(long fileId, String sourceAddress) {
        return filePacketDao.getFilePacketById(fileId, sourceAddress);
    }

    public List<FilePacket> getFilePackets(int status) {
        return filePacketDao.getFilePackets(status);
    }

    /**
     * Any file packets related with this nodeId
     * @param nodeId
     * @return
     */
    public List<FilePacket> getFilePackets(String nodeId, int status) {
        return filePacketDao.getFilePackets(nodeId, status);
    }

    public FilePacket getFilePackets(String sourceAddress, long fileId, int status) {
        return filePacketDao.getFilePacket(fileId, sourceAddress, status);
    }

    public FilePacket getFilePackets(String sourceAddress, long fileId) {
        return filePacketDao.getFilePacketById(fileId, sourceAddress);
    }

    public List<FilePacket> getOlderFilePackets(long minimumTimeGap) {
        return filePacketDao.getAllBefore(minimumTimeGap);
    }

    public int deleteAllPackets() {
        return filePacketDao.deleteAll();
    }

    /**
     * Delete all packets except if any packet source address is self and the status is failed
     * @param filePackets
     * @return
     */
    public int deleteAllPackets(List<FilePacket> filePackets) {
        TransportManagerX transportManagerX = TransportManagerX.getInstance();
        String selfId = transportManagerX == null ? null : transportManagerX.getMyNodeId();
        return filePacketDao.deleteAll(filePackets, selfId);
    }
    public int deletePacket(FilePacket filePacket) {
        List<FilePacket> filePackets = new ArrayList<>();
        filePackets.add(filePacket);
        return deleteAllPackets(filePackets);
    }

    public ExecutorService getExecutorService() {
        return mExecutorService;
    }
}
