package com.w3engineers.mesh.tunnel;


import android.annotation.SuppressLint;
import android.net.Network;
import android.os.AsyncTask;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.NoSecurityRepo;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.NetworkOperationHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class is responsible for all tunnel related method
 * Tunnel start , session creation and other things
 */
public class TelemeshTunnel extends JSch {
    Session session = null;
    ChannelShell cc = null;
    String remoteUrl;
    String subdomainName;
    int sshPort;
    int localPort;
    int remotePort;
    public static TunnelStatusListener mTunnelStatusListener;
    public static TelemeshLogListener mTelemeshLogListener;
    public static boolean isTunnelOngoing = false;
    public static TunnelConnectionListener mTunnelConnectionListener;
    private ExecutorService executorService;

    public TelemeshTunnel(String remoteUrl, int sshPort, int localPort, int remotePort) {
        this.remoteUrl = remoteUrl;
        this.sshPort = sshPort;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.executorService = Executors.newSingleThreadExecutor();
        setLogger(telemeshLogger);
    }

    /**
     * To print all tunnel related log
     */
    Logger telemeshLogger = new com.jcraft.jsch.Logger() {
        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        public void log(int level, String message) {
            //MeshLog.e("Remote_transport  " + message);
            if (message.contains("Disconnecting")) {
                //mTelemeshLogListener.onLog(level + "  " + message);
                mTunnelConnectionListener.onTunnelConnectionUpdated(false);
            }
            if (message.contains("Connection established")) {
                mTunnelConnectionListener.onTunnelConnectionUpdated(true);
            }
            if (message.contains("Channel Closed")) {
                try {
                    HandlerUtil.postForeground(() -> Toaster.showLong("SSH Channel Closed...."));
                } catch (Exception e) {
                    MeshLog.e(e.getMessage());
                }
                //mTelemeshLogListener.onLog(level + "  " + message);
                //mTunnelConnectionListener.onTunnelConnectionUpdated(false);
            }
        }
    };

    /**
     * Setting subdomain name for tunneling
     *
     * @param subdomainName
     */
    public void setSubdomainName(String subdomainName) {
        this.subdomainName = subdomainName;
    }


    public interface TelemeshLogListener {
        void onLog(String message);
    }

    public String getSubdomainName() {
        return this.subdomainName;
    }

    public static void setTunnelStatusListener(TunnelStatusListener tunnelStatusListener) {
        mTunnelStatusListener = tunnelStatusListener;
    }

    public static void setTelemeshLogListener(TelemeshLogListener telemeshLogListener) {
        mTelemeshLogListener = telemeshLogListener;
    }

    public static void setTunnelConnectionListener(TunnelConnectionListener tunnelConnectionListener) {
        mTunnelConnectionListener = tunnelConnectionListener;
    }

    /**
     * Start tunnel AKA port forwarding
     */
    public void startTunnel(String from) {

        //stopTunnel();
        MeshLog.i("Tunnel_Start called from  " + from);

        Future<Boolean> futureTask = executorService.submit(() -> {
            try {


                Network mobileNetwork = NetworkOperationHelper.getConnectedMobileNetwork();

                if (mobileNetwork != null) {
                    MeshLog.i("Tunnel Start Sub-domain Name: " + subdomainName);
                    session = TelemeshTunnel.this.getSession(BuildConfig.LIBRARY_PACKAGE_NAME, remoteUrl);
                    session.setPort(sshPort);
                    session.setHostKeyRepository(new NoSecurityRepo());
                    session.setPassword(BuildConfig.TUNNEL_PASSWORD);
                    //session.setDaemonThread(true);
//                    session.setSocketFactory((SocketFactory) mobileNetwork.getSocketFactory());
                    session.connect();
                    session.sendKeepAliveMsg();
                    cc = (ChannelShell) session.openChannel("shell");
                    cc.setPty(false);
                    cc.setOutputStream(System.out);
                    cc.connect();
                    session.setPortForwardingR(subdomainName, remotePort, "localhost", localPort);
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                MeshLog.e("JSchException" + e.toString());
                e.printStackTrace();
                return false;
            }
        });

        if (futureTask.isDone()) {
            try {
                isTunnelOngoing = futureTask.get();
                mTunnelStatusListener.onTunnelStatusUpdated(isTunnelOngoing);

                MeshLog.i("From StartTunnel OnTunnelStatusUpdated : " + isTunnelOngoing);

            } catch (ExecutionException e) {
                MeshLog.e("ExecutionException" + e.toString());
                e.printStackTrace();
            } catch (InterruptedException e) {
                MeshLog.e("InterruptedException" + e.toString());
                e.printStackTrace();
            }

        }

       /* new AsyncTask<Void, Void, Void>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    MeshLog.i("Tunnel Start Subdomain Name: " + subdomainName);
                    session = TelemeshTunnel.this.getSession(BuildConfig.LIBRARY_PACKAGE_NAME, remoteUrl);
                    session.setPort(sshPort);
                    session.setHostKeyRepository(new NoSecurityRepo());
                    session.setPassword(BuildConfig.TUNNEL_PASSWORD);
                    //session.setDaemonThread(true);
                    session.connect();
                    session.sendKeepAliveMsg();
                    ChannelShell cc = (ChannelShell) session.openChannel("shell");
                    cc.setPty(false);
                    cc.setOutputStream(System.out);
                    cc.connect();
                    session.setPortForwardingR(subdomainName, remotePort, "localhost", localPort);
                } catch (JSchException e) {
                    MeshLog.e("JSchException" + e.toString());
                    tunnelRunningStats[0] = false;
                    // mTunnelStatusListener.onTunnelStatusUpdated(false);
                    e.printStackTrace();

                } catch (Exception e) {
                    MeshLog.e("JSchException" + e.toString());
                    tunnelRunningStats[0] = false;
                    e.printStackTrace();

                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                isTunnelOngoing = tunnelRunningStats[0];
                mTunnelStatusListener.onTunnelStatusUpdated(tunnelRunningStats[0]);
                super.onPostExecute(aVoid);
            }
        }.execute();*/
    }


    public boolean isTunnelConnected(){
        if(session != null){
            return session.isConnected();
        }
        return false;
    }

    /**
     * Stop tunneling , it also disconnect app from SSH server
     */
    public void stopTunnel() {
        isTunnelOngoing = false;
        if (session != null) {
            session.disconnect();
            session = null;
            MeshLog.e("Remote_transport Session Disconnected from stopTunnel");
        }
        if (cc != null) {
            cc.disconnect();
            cc = null;
            MeshLog.e("Remote_transport Channel Disconnected from stopTunnel");
        }
        if (mTunnelStatusListener != null) {
            mTunnelStatusListener.onTunnelStatusUpdated(false);
        }
    }

    public interface TunnelStatusListener {
        void onTunnelStatusUpdated(boolean isTunnelingWorking);
    }

    public interface TunnelConnectionListener {
        void onTunnelConnectionUpdated(boolean result);
    }
}
