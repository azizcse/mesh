package com.w3engineers.mesh.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.w3engineers.mesh.LinkMode;
import com.w3engineers.mesh.queue.MessageBuilder;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.queue.messages.BTDiscoveryMessage;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.JsonDataBuilder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * <h1>The Bluetooth client connection manged here</h1>
 * The client is connected with another Bluetooth server.
 * Communication via socket managed here
 */
public class BluetoothClient {
    private BluetoothSocket bluetoothSocket;
    private Executor executor;
    private String myId;
    private ConnectionStateListener bleListener;
    private BluetoothServer server;
    //private MessageDispatcher messageDispatcher;
    public BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;

    private final int MAX_FILE_SOCKET_RETRY = 3;

    public BluetoothClient(String nodeId, ConnectionStateListener bleListener, BluetoothServer server/*, MessageDispatcher messageDispatcher*/) {

        this.executor = Executors.newSingleThreadExecutor();
        this.myId = nodeId;
        this.bleListener = bleListener;
        this.server = server;
        //this.messageDispatcher = messageDispatcher;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void createConnection(final BluetoothDevice bluetoothDevice, final ConnectionState listener) {
        try {
            executor.execute(() -> {
                bluetoothAdapter.cancelDiscovery();
                int messageId = BaseMeshMessage.DEFAULT_MESSAGE_ID;
                int attempt = 0;
                while (attempt++ < MAX_FILE_SOCKET_RETRY) {
                    try {
                        MeshLog.v("[BT-Classic] attempt to connect :" + bluetoothDevice.getName());
                        bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord
                                (Constant.MY_UUID_INSECURE);
                        bluetoothSocket.connect();
                        if (BleLink.getBleLink() != null) return;

                        BleLink link = BleLink.on(bluetoothSocket, bleListener, LinkMode.CLIENT/*, messageDispatcher*/);
                        link.start();

                        MeshLog.i("[BT-Classic] Client connection created");

                        listener.onConnectionState(UUID.randomUUID().toString().hashCode(), bluetoothDevice.getName());
                        bleListener.onClientBtMsgSocketConnected(bluetoothDevice);

                        break;
                    } catch (IOException | IllegalThreadStateException e) {
                        e.printStackTrace();
                        MeshLog.e(" [BT-Classic] ERROR: " + e.getMessage());
                        listener.onConnectionState(messageId, null);
                        try {
                            if (bluetoothSocket != null) {
                                bluetoothSocket.close();
                                bluetoothSocket = null;
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                mBluetoothDevice = bluetoothDevice;
            });
        } catch (RejectedExecutionException executionException) {
            MeshLog.e("Reject bluetooth connection from executor:" + executionException.getMessage());
            listener.onConnectionState(BaseMeshMessage.DEFAULT_MESSAGE_ID, null);
        }

    }

    public void stop() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }

        stopConnectionProcess();
    }


    protected void stopConnectionProcess() {

        //Close socket if it is not in a complete connected state
        if (bluetoothSocket != null && !bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
