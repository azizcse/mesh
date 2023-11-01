package com.letbyte.core.meshfilesharing.comm.bt;

import android.bluetooth.BluetoothSocket;

import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.core.listeners.ReceiverFileStateHandler;
import com.letbyte.core.meshfilesharing.core.listeners.SenderFileStateHandler;
import com.letbyte.core.meshfilesharing.data.BroadcastFilePacket;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.letbyte.core.meshfilesharing.helper.MeshFileHelper;
import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.util.MeshLog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import timber.log.Timber;


public class BTFileLink extends Thread {

    private final long FILE_TRANSFER_TIMEOUT = 30 * 1000;
    private final int DEFAULT_BUFFER_SIZE = 4 * 1024;
    private BluetoothSocket mBluetoothSocket;
    private DataInputStream mDataInputStream;
    private DataOutputStream mDataOutputStream;
    private int mIncomingPacketLastPercentage, mOutgoingPacketLastPercentage;
    private volatile FilePacket mInComingFilePacket, mOutgoingPacket;
    private volatile boolean mIsFirstChunk;
    private FileHelper mFileHelper;
    private SenderFileStateHandler mSenderFileStateHandler;
    private ReceiverFileStateHandler mReceiverFileStateHandler;
    private Executor mFileSenderExecutor;
//    private Handler mHandler;

    /**
     * To sense receive file failure
     */
//    private Runnable mFailIncomingFile = this::failIncomingFile;
//    private Runnable mFailOutgoingFile = this::failOutgoingFile;
    public BTFileLink(BluetoothSocket bluetoothSocket, FileHelper fileHelper) {
//        mHandler = HandlerUtil.resolveNewHandler(getClass().getSimpleName());
        try {
            this.mBluetoothSocket = bluetoothSocket;
            this.mDataInputStream = new DataInputStream(bluetoothSocket.getInputStream());
            this.mDataOutputStream = new DataOutputStream(bluetoothSocket.getOutputStream());
            this.mFileHelper = fileHelper;
        } catch (IOException e) {
            e.printStackTrace();
        }

        mFileSenderExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Request accept iff {@link #mInComingFilePacket} is null or better say not waiting for any
     * file
     *
     * @param filePacket
     * @return
     */
    public boolean accept(FilePacket filePacket) {
        boolean isAccepting = false;
        if (mInComingFilePacket == null) {
            //Make sure not receiving any file
            mInComingFilePacket = filePacket;
            mIsFirstChunk = true;
            MeshLog.v("[File]Accepting file:" + mInComingFilePacket.toString());
            isAccepting = true;
            mIncomingPacketLastPercentage = 0;
            if (mReceiverFileStateHandler != null) {
                mReceiverFileStateHandler.onFileReceiveStarted(mInComingFilePacket);
            }
        }
        resetFileReadTimer(true);
        return isAccepting;
    }

    //For multihop file sharing we will use this write method
    public boolean writeData(byte[] data) {
        ByteBuffer header = ByteBuffer.allocate(4);
        header.order(ByteOrder.BIG_ENDIAN);
        header.putInt(data.length);

        try {
            mDataOutputStream.write(header.array());
            mDataOutputStream.write(data);
            mDataOutputStream.flush();
        } catch (IOException ex) {
            //CrashReporter.logException(ex);
            MeshLog.e("[Bt-Classic] File send error: " + ex.getMessage());
            try {
                mDataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }
        return true;
    }

    private boolean write(byte meta, byte data) {
        byte[] bytes = {data};
        return write(meta, bytes);
    }

    public boolean write(byte meta, byte[] data) {
        return write(meta, data, 0, data.length);
    }

    public boolean write(byte meta, byte[] data, int start, int end) {
        boolean isSend = false;
        try {
            this.mDataOutputStream.write(meta);
            this.mDataOutputStream.write(data, start, end);
            isSend = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isSend;
    }

    //Merge with above overriden version
    public boolean write(byte[] data, int start, int end) {
        boolean isSend = false;
        try {
            this.mDataOutputStream.write(data, start, end);
            isSend = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isSend;
    }


    @Override
    public void run() {

        int bufferSize = (int) Math.max(MeshFileHelper.FILE_PACKET_SIZE, DEFAULT_BUFFER_SIZE);
        byte[] bytes = new byte[bufferSize];

        try {
            int length;
            while (true) {

                bytes = new byte[bufferSize];
                length = mDataInputStream.read(bytes);
                if (length < 0) {
                    MeshLog.e("BT rear 0 byte.....");
                    continue;

                } else if (length < bufferSize) {
                    bytes = Arrays.copyOf(bytes, length);
                    String st = new String(bytes);
                    int l = st.length();
                }

                //TODO Separate the Thread between direct receive and process or write to disk, as in
                // same thread so it causes some time gap and in terms file transfer issue between
                //sender and receiver, It could have a separate copy if incomingPacket rather only a
                // global version only. Maybe in a separate class
                processFileResponse(bytes);

            }
        } catch (IOException e) {
            MeshLog.e("[BT-File-Socket]BT IOException");
            try {
                mDataInputStream.close();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }

            failIncomingFile();
        }
    }

    private void failIncomingFile() {
        if (mInComingFilePacket != null) {
            mIsFirstChunk = false;
            if (mReceiverFileStateHandler != null) {
                mReceiverFileStateHandler.onFileTransferError(mInComingFilePacket,
                        MeshFile.FAILED_COULD_NOT_READ);
            }
            MeshLog.v(String.format("[File]Failing-Incoming %s", mInComingFilePacket.toString()));
            mIncomingPacketLastPercentage = 0;
            mInComingFilePacket = null;
//        mHandler.removeCallbacks(mFailIncomingFile);
        }
    }

    /**
     * Process data from sender side.
     *
     * @param data
     * @return whether the written packet last packet or not
     */
    private boolean processFileResponse(byte[] data) {
        boolean isLastPacket = false;
        if (mInComingFilePacket != null && mFileHelper != null) {
            mInComingFilePacket.mData = data;
            long writtenBytes = mFileHelper.writePacketData(mInComingFilePacket);
            mInComingFilePacket.mTransferredBytes += writtenBytes;

            if (mReceiverFileStateHandler != null) {
                if (mIsFirstChunk) {
                    mIsFirstChunk = false;
                    mReceiverFileStateHandler.onFirstChunk(mInComingFilePacket, (int) writtenBytes);
                }

                mReceiverFileStateHandler.onFileProgress(mInComingFilePacket, (int) writtenBytes);
            }

            MeshLog.v(String.format("[File]Received %s of %s of %s",
                    Util.humanReadableByteCount(mInComingFilePacket.mTransferredBytes),
                    mInComingFilePacket.mFileId,
                    Util.humanReadableByteCount(mInComingFilePacket.mFileSize)));

            if (mInComingFilePacket.mFileSize <= mInComingFilePacket.mTransferredBytes) {
                mIncomingPacketLastPercentage = 0;

                if (mReceiverFileStateHandler != null) {
                    mReceiverFileStateHandler.onFileTransferFinish(mInComingFilePacket);
                    mInComingFilePacket = null;
                }
                isLastPacket = true;
//                mHandler.removeCallbacks(mFailIncomingFile);
                resetFileReadTimer(false);
            } else {
                //Percentage
                int percentage = mFileHelper.calculatePercentage(mInComingFilePacket.mFileSize,
                        mInComingFilePacket.mTransferredBytes);
                if (mIncomingPacketLastPercentage < percentage) {
                    mIncomingPacketLastPercentage = percentage;
                    if (mReceiverFileStateHandler != null) {
                        mReceiverFileStateHandler.onFilePercentProgress(mInComingFilePacket,
                                percentage);
                    }
                }
                resetFileReadTimer(true);
//                mHandler.removeCallbacks(mFailIncomingFile);
//                mHandler.postDelayed(mFailIncomingFile, FILE_TRANSFER_TIMEOUT);
            }
        } else {
            Timber.e("[BT-File]A packet loss for BT. %s", mInComingFilePacket);
        }
        return isLastPacket;
    }

    public boolean sendFile(FilePacket filePacket) {

        boolean isSending = false;
        Timber.d("mOutgoingPacket-%s--filePacket-%s", mOutgoingPacket, filePacket);
        if (mOutgoingPacket == null && filePacket != null &&
                Text.isNotEmpty(filePacket.mSelfFullFilePath)) {

            File file = new File(new File("/"), filePacket.mSelfFullFilePath);
            boolean doesFileExist = file.exists();
            Timber.d("doesFileExist-%s", doesFileExist);
            if (doesFileExist) {

                MeshLog.v(String.format("[File]Sending %s", filePacket.mTransferredBytes));
                isSending = true;
                mOutgoingPacket = filePacket;
                mFileSenderExecutor.execute(() -> {
                    InputStream fileInputStream = null;
                    try {

                        if (filePacket.mTransferredBytes > 0) {
                            if (filePacket.mTransferredBytes < file.length()) {
                                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                                FileChannel fileChannel = randomAccessFile.getChannel();

                                //Stream automatically closes the channel
                                fileInputStream = Channels.newInputStream(
                                        fileChannel.position(filePacket.mTransferredBytes));
                            } else {
                                MeshLog.e("Error condition!!! File resume request from" +
                                        "overflowed byte. Size:" + file.length() + "-requested from:" +
                                        filePacket.mTransferredBytes);
                            }
                        } else {

                            fileInputStream = new FileInputStream(file);
                        }

                        byte[] bytes = new byte[(int) MeshFileHelper.FILE_PACKET_SIZE];
                        int length;
                        resetFileWriteTimer(true);
                        while (fileInputStream.available() > 0) {//handle underflow

//                            mHandler.removeCallbacks(mFailOutgoingFile);
//                            mHandler.postDelayed(mFailOutgoingFile, FILE_TRANSFER_TIMEOUT);

                            length = fileInputStream.read(bytes);
                            if (write(bytes, 0, length)) {

                                if (mOutgoingPacket == null) {
                                    break;
                                }

                                mOutgoingPacket.mTransferredBytes += length;

                                if (mSenderFileStateHandler != null) {
                                    mSenderFileStateHandler.onFileProgress(mOutgoingPacket, length);
                                }
                                resetFileWriteTimer(true);
                                //Percent handle
                                int percentage = mFileHelper.calculatePercentage(
                                        mOutgoingPacket.mFileSize, mOutgoingPacket.mTransferredBytes);
                                if (mOutgoingPacketLastPercentage < percentage) {
                                    mOutgoingPacketLastPercentage = percentage;

                                    if (mSenderFileStateHandler != null) {
                                        mSenderFileStateHandler.onFilePercentProgress(
                                                mOutgoingPacket, mOutgoingPacketLastPercentage);
                                    }
                                }
                                if (percentage >= 100) {
                                    resetFileWriteTimer(false);
                                }

                                MeshLog.v(String.format("[File]Sent %s of %s of %s",
                                        Util.humanReadableByteCount(
                                                mOutgoingPacket.mTransferredBytes),
                                        mOutgoingPacket.mFileId,
                                        Util.humanReadableByteCount(mOutgoingPacket.mFileSize)));
                            } else {
                                //Could not write so fail file
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                        failOutgoingFile();
                    } finally {
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (mOutgoingPacket != null) {
                        //Order of below code is important
                        if (mOutgoingPacket instanceof BroadcastFilePacket) {
                            BroadcastFilePacket broadcastFilePacket = ((BroadcastFilePacket) mOutgoingPacket).copyBroadCastFilePacket();
                            prepareFileEvent(broadcastFilePacket);
                        } else {
                            FilePacket f = mOutgoingPacket.copy();
                            prepareFileEvent(f);
                        }
                    }
                });
            } else {
                MeshLog.e("[File]File does not exist. User should not delete file. Error state.");
                //If the file does not exist then no need of the entry in DB
                mOutgoingPacket = null;
                DatabaseService.getInstance(App.getContext()).deletePacket(filePacket);
            }
        }
        return isSending;
    }

    private void prepareFileEvent(FilePacket f) {
        mOutgoingPacketLastPercentage = 0;
        mOutgoingPacket = null;
        if (mSenderFileStateHandler != null) {
            if (f.mTransferredBytes >= f.mFileSize) {
                //We do not want to take any wrong direction
                mSenderFileStateHandler.onFileTransferFinish(f);
            } else {
                //Incomplete file, update file id wise byte
                mSenderFileStateHandler.onFileReadError(f);
            }
        }
    }

    private void failOutgoingFile() {
        if (mOutgoingPacket != null) {
            MeshLog.v(String.format("[File]Failing-Outgoing %s", mOutgoingPacket.toString()));
            if (mOutgoingPacket instanceof BroadcastFilePacket) {
                BroadcastFilePacket broadcastFilePacket = ((BroadcastFilePacket) mOutgoingPacket).copyBroadCastFilePacket();
                forwardErrorResponse(broadcastFilePacket);
            } else {
                FilePacket filePacket = mOutgoingPacket.copy();
                forwardErrorResponse(filePacket);
            }
        }
    }

    private void forwardErrorResponse(FilePacket filePacket) {
        mOutgoingPacketLastPercentage = 0;
        mOutgoingPacket = null;

        if (mSenderFileStateHandler != null) {
            mSenderFileStateHandler.onFileTransferError(filePacket, MeshFile.FAILED_COULD_NOT_WRITE);
        }
    }

    public void setSenderFileStateHandler(SenderFileStateHandler senderFileStateHandler) {
        mSenderFileStateHandler = senderFileStateHandler;
    }

    public void setReceiverFileStateHandler(ReceiverFileStateHandler receiverFileStateHandler) {
        mReceiverFileStateHandler = receiverFileStateHandler;
    }

    public void onPeerLeave() {

        if (mInComingFilePacket != null) {

            failIncomingFile();
        } else if (mOutgoingPacket != null) {

            failOutgoingFile();
        }
        stopLink();
    }

    public void stopLink() {
        if (mDataInputStream != null) {
            try {
                mDataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mDataOutputStream != null) {
            try {
                mDataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mSenderFileStateHandler != null && mOutgoingPacket != null) {
            if (mOutgoingPacket instanceof BroadcastFilePacket) {
                BroadcastFilePacket broadcastFilePacket = ((BroadcastFilePacket) mOutgoingPacket).copyBroadCastFilePacket();
                mOutgoingPacket = null;
                mSenderFileStateHandler.onFileTransferError(broadcastFilePacket, MeshFile.FAILED_LINK_CLOSED);
            } else {
                FilePacket filePacket = mOutgoingPacket.copy();
                mOutgoingPacket = null;
                mSenderFileStateHandler.onFileTransferError(filePacket, MeshFile.FAILED_LINK_CLOSED);
            }
        }

        if (mReceiverFileStateHandler != null && mInComingFilePacket != null) {
            mReceiverFileStateHandler.onFileTransferError(mInComingFilePacket,
                    MeshFile.FAILED_LINK_CLOSED);
        }
    }

    public boolean isBTFileSending() {
        boolean isSending = mOutgoingPacket != null && mOutgoingPacket.mTransferredBytes <
                mOutgoingPacket.mFileSize;
        MeshLog.v(String.format("[File]Is sending %s", isSending));
        return isSending;
    }

    public boolean isReceiving() {
        return mInComingFilePacket != null;
    }


    private void resetFileReadTimer(boolean isReset) {
        /*Log.e("BtsocketTime", "File read timer reset :" + isReset);
        if (isReset) {
            HandlerUtil.postBackground(filePacketReadRunnable, FILE_TRANSFER_TIMEOUT);
        } else {
            HandlerUtil.removeBackground(filePacketReadRunnable);
        }*/
    }

    private void resetFileWriteTimer(boolean isReset) {
        /*Log.e("BtsocketTime", "File write timer reset :" + isReset);
        if (isReset) {
            HandlerUtil.postBackground(filePacketWriteRunnable, FILE_TRANSFER_TIMEOUT);
        } else {
            HandlerUtil.removeBackground(filePacketWriteRunnable);
        }*/
    }

    private Runnable filePacketReadRunnable = new Runnable() {
        @Override
        public void run() {
            failIncomingFile();
        }
    };

    private Runnable filePacketWriteRunnable = new Runnable() {
        @Override
        public void run() {
            failOutgoingFile();
        }
    };
}