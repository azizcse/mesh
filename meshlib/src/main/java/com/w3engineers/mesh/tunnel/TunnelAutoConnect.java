package com.w3engineers.mesh.tunnel;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;

public class TunnelAutoConnect {
    private boolean mAdapterStatus = true;
    private Handler mHandler = new Handler();
    private TelemeshTunnel mTelemeshTunnel;
    private int reconnectCallCounter=0;
    private static ReTryCompletedCallbackListener mReTryCompletedCallbackListener;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(reconnectCallCounter<TunnelConstant.maxCounterReconnect)
            {
                MeshLog.ssh("Connecting from TunnelAutoConnect");
                TunnelConnectionStatus.setConnectionStatus(TunnelConstant.CONNECTING);
                mTelemeshTunnel.startTunnel(" runnable");
                mHandler.postDelayed(runnable, TunnelConstant.reconnectDelay);
            }
            if(reconnectCallCounter==TunnelConstant.maxCounterReconnect)
            {
                mReTryCompletedCallbackListener.onReTryCompletedCallbackListener(true);
            }
            reconnectCallCounter++;
        }
    };
    public TunnelAutoConnect(TelemeshTunnel telemeshTunnel) {
        this.mTelemeshTunnel = telemeshTunnel;
    }
    public static void setReTryCompletedCallbackListener(ReTryCompletedCallbackListener reTryCompletedCallbackListener)
    {
        mReTryCompletedCallbackListener = reTryCompletedCallbackListener;
    }
    public void startAutoConnect(int flag) {

        if (flag == 0) {
            this.mHandler.postDelayed(runnable, TunnelConstant.reconnectDelay);
        }

    }
    public void stopAutoConnect() {
        this.mHandler.removeCallbacks(runnable);
        this.reconnectCallCounter=0;
        mReTryCompletedCallbackListener.onReTryCompletedCallbackListener(false);
    }
    public interface ReTryCompletedCallbackListener{
        void onReTryCompletedCallbackListener(boolean status);
    }
}
