package com.letbyte.core.meshfilesharing.comm.bt;

import android.os.Handler;
import android.os.HandlerThread;

import com.letbyte.core.meshfilesharing.core.MeshFileManager;
import com.letbyte.core.meshfilesharing.data.BTFileRequestMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastMessage;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FileMessage;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.util.MeshLog;

import java.util.concurrent.ConcurrentLinkedQueue;

import timber.log.Timber;

public class BTFileManager {

    private BTFileLink mBTFileLink;
    private MeshFileHelper mMeshFileHelper;
    private final ConcurrentLinkedQueue<FilePacket> mBTFileSendQueue;
    private TransportManagerX mTransportManagerX;
    private volatile FilePacket mPendingSentPacket, mPendingReceivePacket;
    private Handler mSendingHandler;


    public BTFileManager(MeshFileManager meshFileManager) {
        this.mMeshFileHelper = meshFileManager.getMeshFileHelper();
        mBTFileSendQueue = new ConcurrentLinkedQueue<>();
        mTransportManagerX = TransportManagerX.getInstance();

        HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName() + "-send");
        handlerThread.start();
        mSendingHandler = new Handler(handlerThread.getLooper());
    }

    public void setBTFileLink(BTFileLink BTFileLink) {
        mBTFileLink = BTFileLink;
    }

    public BTFileLink getBTFileLink() {
        return mBTFileLink;
    }

    public synchronized boolean sendFile(FilePacket filePacket) {
        boolean isAccepted = false;

        synchronized (mBTFileSendQueue) {
            MeshLog.i("BT file sending started");
            if (filePacket != null) {
                Timber.d("Q size:%s, %s", mBTFileSendQueue.size(), filePacket.toString());
                isAccepted = mBTFileSendQueue.add(filePacket);
                processFileSendQueue();
            }
        }

        return isAccepted;
    }

    private void processFileSendQueue() {
        mSendingHandler.post(() -> {

            Timber.d("Sending message from sender:isSending:%s-%s-%s", isSending(),
                    mBTFileSendQueue.size(), mPendingSentPacket);
            if (!isSending() && CollectionUtil.hasItem(mBTFileSendQueue)) {
                mPendingSentPacket = mBTFileSendQueue.poll();
                FileMessage fileMessage = null;
                if (mPendingSentPacket instanceof BroadcastFilePacket) {
                    fileMessage = mMeshFileHelper.getBroadcastContentMessage((BroadcastFilePacket) mPendingSentPacket);
                } else {
                    fileMessage = mMeshFileHelper.get(mPendingSentPacket);
                }

                String json = fileMessage.toJson();
                if (fileMessage instanceof BroadcastMessage) {
                    mTransportManagerX.sendBroadcastMessage(mMeshFileHelper
                            .getBroadcastFromMessage((BroadcastMessage) fileMessage));
                } else {
                    mTransportManagerX.sendFileMessage(fileMessage.mPeerAddress, json.getBytes());
                }
            }
        });
    }

    public void onFileRequestFromSender(FileMessage fileMessage, FilePacket filePacket) {
        if (filePacket != null) {
            if (fileMessage instanceof BroadcastMessage) {
                receiveBroadcastFile(fileMessage, filePacket);
            } else {
                Timber.d("Receive request from sender:isReceiving-%s, %s", isReceiving(),
                        filePacket.toString());
                if (isReceiving()) {
                    mPendingReceivePacket = filePacket;
                } else {
                    receiveFile(filePacket);
                }
            }

        }
    }

    private void receiveFile(FilePacket filePacket) {

        if (filePacket != null) {

            boolean isReceiving = isReceiving();

            mBTFileLink.accept(filePacket);

            //Send receive confirmation message
            BTFileRequestMessage btFileRequestMessage = mMeshFileHelper.getBTFileRequest(filePacket);
            String json = btFileRequestMessage.toJson();
            mTransportManagerX.sendFileMessage(btFileRequestMessage.mSourceAddress, json.getBytes());
            Timber.d("isReceiving-%s--%s--%s", isReceiving, filePacket, btFileRequestMessage);
        }
    }

    private void receiveBroadcastFile(FileMessage fileMessage, FilePacket filePacket) {

        if (filePacket != null) {

            boolean isReceiving = isReceiving();

            mBTFileLink.accept(filePacket);

            //Send receive confirmation message

            BTFileRequestMessage btFileRequest = mMeshFileHelper.getBTFileRequest(filePacket);
            String json = btFileRequest.toJson();
            mTransportManagerX.sendFileMessage(btFileRequest.mSourceAddress, json.getBytes());
            Timber.d("isReceiving-%s--%s--%s", isReceiving, filePacket, btFileRequest);
        }

    }

    public void onFileRequestFromReceiver(BTFileRequestMessage btFileRequestMessage) {
        if (btFileRequestMessage != null) {
            Timber.d("Request from receiver:%s, %s, %s", isSending(),
                    btFileRequestMessage, mPendingSentPacket == null ? null :
                            mPendingSentPacket.toString());
            if (mPendingSentPacket != null && btFileRequestMessage.mFileTransferId ==
                    mPendingSentPacket.mFileId && mPendingSentPacket != null &&
                    !mBTFileLink.isBTFileSending()) {
                mPendingSentPacket.mTransferredBytes = btFileRequestMessage.mTransferredBytes;
                mBTFileLink.sendFile(mPendingSentPacket);
                mPendingSentPacket = null;
            }
        }
    }

    public void onBTFileSentFinish(FilePacket filePacket) {
        processFileSendQueue();
    }

    public void onBTFileReceiveFinish(FilePacket filePacket) {
        if (mPendingReceivePacket != null) {
            receiveFile(mPendingReceivePacket);
            mPendingReceivePacket = null;
        }
    }

    public void onPeerDisconnect() {
        if (mBTFileLink != null) {
            mBTFileLink.onPeerLeave();
            mBTFileLink = null;
        }
    }

    public boolean isSending() {
        Timber.d("Is Sending state:%s-%s-%s", mPendingSentPacket, mBTFileLink,
                mBTFileLink == null ? mBTFileLink : mBTFileLink.isBTFileSending());
        return ((mPendingSentPacket != null || mBTFileLink != null) && mBTFileLink.isBTFileSending());
    }

    public boolean isReceiving() {
        return mBTFileLink != null && mBTFileLink.isReceiving();
    }

    public void stop() {
        mPendingSentPacket = mPendingReceivePacket = null;
        if (mBTFileLink != null) {
            mBTFileLink.stopLink();
        }
    }
}
