package com.letbyte.core.meshfilesharing.core.listeners;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.data.FilePacket;

public interface FileStateListener {
    /**
     * Report iff any percent increases
     * @param filePacket
     * @param percentProgress
     */
    void onFilePercentProgress(FilePacket filePacket, int percentProgress);

    /**
     * Report any amount of progress
     * @param filePacket
     * @param numberOfBytes
     */
    void onFileProgress(FilePacket filePacket, int numberOfBytes);
    void onFileTransferFinish(FilePacket filePacket);

    //We must make sure unavailability of any node successfully generate the event of FileTransfer
    //error

    /**
     * This event called when any error condition faced
     * @param filePacket
     * @param errorCode from {@link MeshFile}
     */
    void onFileTransferError(FilePacket filePacket, int errorCode);
}
