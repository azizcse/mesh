package com.letbyte.core.meshfilesharing.data.model;

import com.letbyte.core.meshfilesharing.data.FilePacket;

/**
 * Created by Azizul Islam on 12/28/20.
 */
public class PendingFile {
    public String receiverId;
    public String filePath;
    public byte[] fileMeta;
    public String appToken;
    public long fileTransferId;

    public FilePacket filePacket;
    public boolean isResume;
    public String sourceAddress;

    public PendingFile(String receiverId, String filePath, byte[] fileMeta, String appToken, long fileTransferId) {
        this.receiverId = receiverId;
        this.filePath = filePath;
        this.fileMeta = fileMeta;
        this.appToken = appToken;
        this.fileTransferId = fileTransferId;
    }

    public PendingFile(FilePacket filePacket,String sourceAddress,String appToken ,byte[] fileMeta, boolean isResume){
        this.filePacket = filePacket;
        this.sourceAddress = sourceAddress;
        this.appToken = appToken;
        this.fileMeta = fileMeta;
        this.isResume = isResume;
        this.fileTransferId = filePacket.mFileId;
        this.receiverId = filePacket.mPeerAddress;
    }
}
