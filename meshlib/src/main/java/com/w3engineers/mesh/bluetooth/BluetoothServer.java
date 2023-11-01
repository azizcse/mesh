package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.w3engineers.mesh.LinkMode;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BTDiscoveryMessage;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.JsonDataBuilder;

import java.io.IOException;

/**
 * <h1>Bluetooth Server connection managed here</h1>
 * <p>Communication is controlled with socket</p>
 */
public class BluetoothServer {

    private BluetoothAdapter bluetoothAdapter;
    private String nodeId;
    private ConnectionListenThread connectionListenThread;
    private ConnectionStateListener bleListener;
    private String publicKey;
    private MessageDispatcher messageDispatcher;
    private final Object mLock = new Object();
//    private String myUserInfo;

    public BluetoothServer(String nodeId, ConnectionStateListener bleListener/*, MessageDispatcher messageDispatcher*/) {
        this.nodeId = nodeId;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bleListener = bleListener;
        //this.messageDispatcher = messageDispatcher;
    }

    public void starListenThread() {
        if (connectionListenThread == null || !connectionListenThread.isRunning()) {
            synchronized (mLock) {
                connectionListenThread = new ConnectionListenThread();
                MeshLog.i(" [BT-Classic] BT server -> Opened");
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

    private void checkBtConnectionAndStartServer() {
        HandlerUtil.postBackground(() -> checkAndStartServer());
    }

    private void checkAndStartServer() {
     /*   int autonomousMode = SharedPref.readInt(Constant.RANDOM_STATE);
        int lcCount = RouteManager.getInstance().getWifiUser().size();*/

        if (BleLink.getBleLink() == null) {
            MeshLog.e("[BT-Classic] BT link not created start server again");
            starListenThread();
        }else {
            MeshLog.e("[BT-Classic] BT link created no need to start server");
        }
    }


    private class ConnectionListenThread implements Runnable {

        private Thread thread;
        private volatile boolean isRunning;
        private BluetoothServerSocket bluetoothServerSocket;


        public ConnectionListenThread() {
            isRunning = true;
        }

        @Override
        public void run() {
            //while (isRunning) {
            MeshLog.i("[BT-Classic] Server running ........");
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Constant.NAME_INSECURE,
                        Constant.MY_UUID_INSECURE);


                BluetoothSocket bluetoothSocket = bluetoothServerSocket.accept();
                bluetoothAdapter.cancelDiscovery();


                if (BleLink.getBleLink() != null) return;

                BTManager.getInstance().cancelDiscovery();

                BleLink link = BleLink.on(bluetoothSocket, bleListener, LinkMode.SERVER/*, messageDispatcher*/);
                link.start();
              /*  BTDiscoveryMessage btDiscoveryMessage = MessageBuilder.buildMeshBtDiscoveryMessage(link, () -> JsonDataBuilder.prepareBtHelloPacket(nodeId, GsonUtil.getUserInfo()));
                messageDispatcher.addSendMessage(btDiscoveryMessage);*/

                link.startHeartbeatSend();

                MeshLog.v("[BT-Classic] BT server accept connection");
            } catch (IOException e) {
//                e.printStackTrace();
                isRunning = false;

                MeshLog.i("[BT-Classic] BT Socket thread closed.....");
            } catch (IllegalThreadStateException e) {
                MeshLog.i(" Ble Socket thread IllegalThreadStateException");
            } finally {
                Log.e("Bluetooth-dis", "--BT server is in close state---");
                isRunning = false;
                if (bluetoothAdapter.isEnabled()){
                    checkBtConnectionAndStartServer();
                }
                if (bluetoothServerSocket != null) {
                    try {
                        bluetoothServerSocket.close();

                        MeshLog.i(" [BT-Classic] BT Server Closed");
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            }
//            connectionListenThread = null;

            //}
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
                    MeshLog.i(" [BT-Classic] BT server -> Closed");
                } else {
                    MeshLog.i("[BT-Classic] Bluetooth Socket NULL");
                }
                isRunning = false;

                if (thread != null) {
                    thread.interrupt();
                    MeshLog.i("[BT-Classic] Bluetooth Socket Thread interrupt");
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
