package com.letbyte.core.meshfilesharing.data.filepacket;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.TableMeta;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;

import java.util.List;

@Dao
public abstract class FilePacketDao {

    @Transaction
    public long insertFilePacket(FilePacket packet) {
        long row = -1;
        if(packet != null) {
            FilePacket filePacket = getFilePacketById(packet.mFileId, packet.mSourceAddress);

            if(filePacket == null) {

                row = insertFilePacketRaw(packet);

            } else {
                if(packet.mTransferredBytes > filePacket.mTransferredBytes) {

                    row = insertFilePacketRaw(packet);
                }
            }
        }

        return row;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertFilePacketRaw(FilePacket packet);

    @Query("SELECT * FROM " + TableMeta.TableNames.FILE_PACKET + " WHERE " +
            TableMeta.ColNames.FILE_TRANSFER_ID + " = :fileId AND  " +
            TableMeta.ColNames.SOURCE_ADDRESS + " =:sourceAddress")
    public abstract FilePacket getFilePacketById(long fileId, String sourceAddress);

    @Query("SELECT * FROM " + TableMeta.TableNames.FILE_PACKET + " WHERE " +
            TableMeta.ColNames.FILE_STATUS + " == :fileStatus")
    public abstract List<FilePacket> getFilePackets(int fileStatus);

    @Query("SELECT * FROM " + TableMeta.TableNames.FILE_PACKET + " WHERE ((" +
            TableMeta.ColNames.PEER_ADDRESS + " == :nodeId OR "+TableMeta.ColNames.SOURCE_ADDRESS +
            " == :nodeId) AND "+ TableMeta.ColNames.FILE_STATUS +" == :fileStatus)")
    public abstract List<FilePacket> getFilePackets(String nodeId, int fileStatus);

    @Query("SELECT * FROM " + TableMeta.TableNames.FILE_PACKET + " WHERE " +
            TableMeta.ColNames.FILE_TRANSFER_ID + " = :fileId AND  " +
            TableMeta.ColNames.SOURCE_ADDRESS + " =:sourceAddress AND " +
            TableMeta.ColNames.FILE_STATUS + " == :fileStatus")
    public abstract FilePacket getFilePacket(long fileId, String sourceAddress, int fileStatus);

    @Query("SELECT * FROM " + TableMeta.TableNames.FILE_PACKET + " WHERE " +
            TableMeta.ColNames.LAST_MODIFIED + " - :currentTime > :timeGap")
    abstract List<FilePacket> getAllBefore(long currentTime, long timeGap);


    public List<FilePacket> getAllBefore(long timeGap) {
        return getAllBefore(System.currentTimeMillis(), timeGap);
    }


    @Update
    public abstract int updateFilePacket(FilePacket packet);

    @Query("UPDATE "+ TableMeta.TableNames.FILE_PACKET+" SET "+ TableMeta.ColNames.FILE_STATUS +
            " = " + Const.FileStatus.FAILED + " WHERE " + "("+ TableMeta.ColNames.TRANSFERRED_BYTES +
            " < " + TableMeta.ColNames.FILE_SIZE + " OR "+TableMeta.ColNames.FILE_STATUS+" != "+
            Const.FileStatus.FINISH+")")
    public abstract int updateFailedPackets();

    @Query("UPDATE "+ TableMeta.TableNames.FILE_PACKET+" SET "+ TableMeta.ColNames.FILE_STATUS +
            " = " + Const.FileStatus.FAILED + " WHERE " + "("+ TableMeta.ColNames.TRANSFERRED_BYTES +
            " < " + TableMeta.ColNames.FILE_SIZE + " AND ("+
            TableMeta.ColNames.SOURCE_ADDRESS +" == :nodeId OR "+ TableMeta.ColNames.PEER_ADDRESS +
            " == :nodeId))")
    protected abstract int updateFilesFailed(String nodeId);

    @Query("SELECT * FROM "+ TableMeta.TableNames.FILE_PACKET +" WHERE " +
            "("+ TableMeta.ColNames.TRANSFERRED_BYTES +
            " < " + TableMeta.ColNames.FILE_SIZE + " AND ("+
            TableMeta.ColNames.SOURCE_ADDRESS +" == :nodeId OR "+ TableMeta.ColNames.PEER_ADDRESS +
            " == :nodeId))")
    protected abstract List<FilePacket> getFilePacketsOf(String nodeId);

    @Transaction
    public List<FilePacket> updateFailedFor(String nodeId) {

        if(updateFilesFailed(nodeId) > 0) {
            return getFilePacketsOf(nodeId);
        }
        return null;
    }

    @Query("DELETE FROM " + TableMeta.TableNames.FILE_PACKET)
    public abstract int deleteAll();

    @Query("DELETE FROM " + TableMeta.TableNames.FILE_PACKET + " WHERE ("+
            TableMeta.ColNames.SOURCE_ADDRESS +" == :sourceAddress AND "+
            TableMeta.ColNames.FILE_TRANSFER_ID +" == :fileTransferId AND NOT("+
            TableMeta.ColNames.FILE_STATUS +" != "+Const.FileStatus.FINISH+" AND "+
            TableMeta.ColNames.SOURCE_ADDRESS +" == :selfAddress))")
    public abstract int delete(String sourceAddress, long fileTransferId, String selfAddress);


    @Transaction
    public int deleteAll(List<FilePacket> filePackets, String selfAddress) {
        int deleteCount = 0;
        if(CollectionUtil.hasItem(filePackets)) {
            for(FilePacket filePacket : filePackets) {
                deleteCount += delete(filePacket.mSourceAddress, filePacket.mFileId, selfAddress);
            }
        }

        return deleteCount;
    }
}
