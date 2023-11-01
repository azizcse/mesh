package com.letbyte.core.meshfilesharing.comm.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.letbyte.core.meshfilesharing.BuildConfig;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

import java.io.IOException;
import java.util.UUID;

/**
 * <h1>Bluetooth Server for file channel</h1>
 * <p>This class is only to initiate socket connection</p>
 */
public class BTFileServer {

    private BluetoothAdapter bluetoothAdapter;
    private ConnectionListenThread connectionListenThread;
    private final Object mLock = new Object();
    private FileHelper mFileHelper;
    private BTFileLinkListener mBTFileLinkListener;

    private final String prefixTag = "[BT Classic] ";

    public BTFileServer(BTFileLinkListener btFileLinkListener, FileHelper fileHelper) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mFileHelper = fileHelper;
        this.mBTFileLinkListener = btFileLinkListener;
    }

    public void starListenThread() {
        if (connectionListenThread == null || !connectionListenThread.isRunning()) {
            synchronized (mLock) {
                connectionListenThread = new ConnectionListenThread();
                MeshLog.i(prefixTag + "  -> Opened");
                if (connectionListenThread != null) {
                    connectionListenThread.start();
                }
            }
        }
    }

    public void stopListenThread() {
        if (connectionListenThread != null) {
            synchronized (mLock) {
                if (connectionListenThread != null) {
                    connectionListenThread.stop();
                    connectionListenThread = null;
                }
            }
        }
    }


    private class ConnectionListenThread implements Runnable {

        private Thread thread;
        private boolean isRunning;
        private BluetoothServerSocket bluetoothServerSocket;
        private volatile boolean isBtConnected = false;

        public ConnectionListenThread() {
            isRunning = true;
        }

        @Override
        public void run() {
            //while (isRunning) {
            MeshLog.i(prefixTag + " BT File Server running ........");
            BluetoothSocket bluetoothSocket = null;
            try {
                UUID uuid = UUID.fromString(BuildConfig.BT_FILE_UUID);
                bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Constant.NAME_INSECURE,
                        uuid);

                if (isBtConnected) return;
                bluetoothSocket = bluetoothServerSocket.accept();
                bluetoothAdapter.cancelDiscovery();
                isBtConnected = true;
                MeshLog.e(prefixTag + " BT server connected");

            } catch (IOException e) {
//                e.printStackTrace();
                isRunning = false;
                isBtConnected = false;
                MeshLog.i(prefixTag + " socket thread closed.....");
            } catch (IllegalThreadStateException e) {
                MeshLog.i(prefixTag + " socket Socket thread IllegalThreadStateException");
            } finally {
                if (bluetoothServerSocket != null) {
                    try {
                        bluetoothServerSocket.close();
                        isBtConnected = false;
                        MeshLog.i(prefixTag + " BT File Server Closed");
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            }
//            connectionListenThread = null;
            isRunning = false;

            if (bluetoothSocket != null) {

                if (mBTFileLinkListener != null) {

                    if (mBTFileLinkListener.getBTFileLink() == null) {
                        BTFileLink link = new BTFileLink(bluetoothSocket, mFileHelper);
                        link.start();

                        mBTFileLinkListener.onBTLink(link);
                    }
                }
            } else {
                //If bluetooth connection not able to create
                // then start server theread again
                starListenThread();
            }

        }

        synchronized void start() {
            thread = new Thread(this, "listen");
            thread.setPriority(Thread.MAX_PRIORITY);
            isRunning = true;
            thread.start();
        }

        synchronized void stop() {
            /*if (!isRunning) {
                return;
            }*/
            try {
                if (bluetoothServerSocket != null) {
                    bluetoothServerSocket.close();
                    MeshLog.i(prefixTag + " SERVER -> Closed");
                } else {
                    MeshLog.i(prefixTag + " Socket NULL");
                }
                isRunning = false;
                isBtConnected = false;
                if (thread != null) {
                    thread.interrupt();
                    MeshLog.i(prefixTag + " Socket Thread interrupt");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean isRunning() {
            return isRunning;
        }
    }

}
