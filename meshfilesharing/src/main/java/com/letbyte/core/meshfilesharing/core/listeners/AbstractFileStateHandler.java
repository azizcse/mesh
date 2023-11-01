package com.letbyte.core.meshfilesharing.core.listeners;

import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.mesh.TransportManagerX;

public abstract class AbstractFileStateHandler implements FileStateListener {

    protected DatabaseService mDatabaseService;
    protected FileHelper mFileHelper;
    protected String mMyAddress;
    public MeshFileEventListener mMeshFileEventListener;
    protected MeshFileManager mMeshFileManager;

    protected AbstractFileStateHandler(DatabaseService databaseService, FileHelper fileHelper,
                                       MeshFileManager meshFileManager,
                                       MeshFileEventListener meshFileEventListener) {
        mFileHelper = fileHelper;
        mDatabaseService = databaseService;
        mMeshFileEventListener = meshFileEventListener;
        mMeshFileManager = meshFileManager;
        mMyAddress = TransportManagerX.getInstance().getMyNodeId();
    }

    @Override
    public void onFilePercentProgress(FilePacket filePacket, int percentProgress) {
        if (filePacket instanceof BroadcastFilePacket) {
            // update BroadcastFile
        } else {
            mDatabaseService.updateFilePacket(filePacket);
        }

    }

    @Override
    public void onFileProgress(FilePacket filePacket, int numberOfBytes) {
        if (filePacket instanceof BroadcastFilePacket) {
            // insert BroadcastFile
        } else {
            mDatabaseService.insertFilePacket(filePacket);
        }

        //Refreshing to ensure highBand flag retain as true
        TransportManagerX transportManagerX = TransportManagerX.getInstance();
        if (transportManagerX != null) {
            //transportManagerX.setHighBand();
        }
    }

    @Override
    public void onFileTransferFinish(FilePacket filePacket) {

        TransportManagerX transportManagerX = TransportManagerX.getInstance();
        if (transportManagerX != null) {
            transportManagerX.releaseHighBandMode();
        }
    }

    @Override
    public void onFileTransferError(FilePacket filePacket, int errorCode) {

        TransportManagerX transportManagerX = TransportManagerX.getInstance();
        if (transportManagerX != null) {
            transportManagerX.releaseHighBandMode();
        }
    }

    public void setMeshFileEventListener(MeshFileEventListener meshFileEventListener) {
        mMeshFileEventListener = meshFileEventListener;
    }
}
