package com.w3engineers.mesh.bluetooth;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * <h1>Bluetooth connection activeness is monitored here</h1>
 * <p>Sending ping with random time interval</p>
 */
public class BtHBSender {
    private static Handler backGroundHandler;
    private static HandlerThread handlerThread;

    private BtHBSender() {
    }

    public static void postBackground(Runnable runnable, long delay) {
        resolveHandler();
        backGroundHandler.removeCallbacksAndMessages(null);
        boolean status = backGroundHandler.postDelayed(runnable, delay);
       // MeshLog.i("BtHBSender Status ::" + status);
    }

    private static void resolveHandler() {
        if (handlerThread == null) {
            handlerThread = new HandlerThread("BT_Thread", Thread.MAX_PRIORITY);
            handlerThread.start();
        }
        if (backGroundHandler == null) {
            backGroundHandler = new Handler(handlerThread.getLooper());
        }

    }
}
