package com.letbyte.core.meshfilesharing.core.listeners;

import com.letbyte.core.meshfilesharing.data.FilePacket;

public interface IClientFileStateListener extends FileStateListener {
    void onFileReceiveStarted(FilePacket filePacket);
    void onFirstChunk(FilePacket filePacket, int bytes);
}
