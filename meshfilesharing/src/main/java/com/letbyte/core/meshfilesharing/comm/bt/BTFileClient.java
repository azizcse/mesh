package com.letbyte.core.meshfilesharing.comm.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.letbyte.core.meshfilesharing.BuildConfig;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.mesh.util.MeshLog;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h1>The Bluetooth client connection manged here</h1>
 * The client is connected with another Bluetooth server.
 * Communication via socket managed here
 */
public class BTFileClient {

    private final int MAX_FILE_SOCKET_RETRY = 3;
    private BluetoothSocket mBluetoothSocket;
    private Executor executor;
    private BTFileLinkListener mBTFileLinkListener;
    private FileHelper mFileHelper;
    private BTFileServer mBTFileServer;
    private final String prefixTag = "[BT Classic] ";

    public BTFileClient(BTFileServer btFileServer, BTFileLinkListener btFileLinkListener, FileHelper fileHelper) {

        mBTFileServer = btFileServer;
        this.executor = Executors.newSingleThreadExecutor();
        mFileHelper = fileHelper;
        mBTFileLinkListener = btFileLinkListener;
    }

    public boolean createConnection(final BluetoothDevice bluetoothDevice) {
        // here we will try maximum 3 times to connect with server

        //If file socket connection fails then we should make a link with SDK so that connection can
        // be re-initiated
        int attempt = 0;
        String logText = "BT File socket connection - ";
        boolean isSuccess = false;
        MeshLog.e(prefixTag + " Bt-filesocket", "Bt file attempt to connect.....");
        while (attempt++ < MAX_FILE_SOCKET_RETRY) {
            try {
                UUID uuid = UUID.fromString(BuildConfig.BT_FILE_UUID);
                mBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord
                        (uuid);
                mBluetoothSocket.connect();

                if (mBTFileServer != null) {
                    mBTFileServer.stopListenThread();
                }

                BTFileLink link = new BTFileLink(mBluetoothSocket, mFileHelper);
                link.start();

                if (mBTFileLinkListener != null) {
                    mBTFileLinkListener.onBTLink(link);
                }
                logText += "success";
                isSuccess = true;
                MeshLog.e(prefixTag + " Bt file client connected");
                break;
            } catch (IOException | IllegalThreadStateException e) {
                e.printStackTrace();
                MeshLog.e(prefixTag + " Bt file client connect failed :" + attempt);
                MeshLog.e(prefixTag + " BT ERROR: " + e.getMessage());
                try {
                    if (mBluetoothSocket != null) {
                        mBluetoothSocket.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                logText += "failed";

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            logText += "-" + attempt;
            MeshLog.i(prefixTag + logText);
        }
        return isSuccess;
    }

    public void stop() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }

        stopConnectionProcess();
    }


    protected void stopConnectionProcess() {

        //Close socket if it is not in a complete connected state
        if (mBluetoothSocket != null && !mBluetoothSocket.isConnected()) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBTFileLinkListener(BTFileLinkListener BTFileLinkListener) {
        mBTFileLinkListener = BTFileLinkListener;
    }

}
