package com.w3engineers.mesh.Adhoc.protocol;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * <h1> Zero conf network related broadcast resolved here </h1>
 * <p> All types of call back initialized from there </p>
 */
public class JmdResolver implements WifiResolver, ServiceListener {
    private final String serviceType;
    private final String serviceName;
    private Listener listener;
    private DispatchQueue queue;
    private Context context;
    private boolean running;
    private WifiManager manager;
    private WifiManager.MulticastLock lock;
    private JmDNS jmdns;

    public JmdResolver(String serviceTypeWithoutLocal, String serviceName, Listener listener,
                       DispatchQueue queue, Context context) {

        this.serviceType = serviceTypeWithoutLocal + "local.";
        this.serviceName = serviceName;
        this.listener = listener;
        this.queue = queue;
        this.context = context;

    }

    @Override
    public void start(InetAddress address, int port) {
        if (!startJmdns(address)) {
            return;
        }
        //Log.e("Jmdnslog", "onAdhocEnabled =" + address.getHostAddress());
        startResolveInternal();
        startPublishInternal(port);
    }

    @Override
    public void startPublishOnly(InetAddress address, int port) {
        if (!startJmdns(address))
            return;

        startPublishInternal(port);
    }

    @Override
    public void startResolveOnly(InetAddress address) {
        if (!startJmdns(address))
            return;

        startResolveInternal();
    }

    @Override
    public boolean isRunning(){
        return running;
    }

    @Override
    public void stopServiceListening() {
        jmdns.removeServiceListener(serviceType, this);
    }

    @Override
    public void stop() {
        if (!running)
            return;
        running = false;

        jmdns.removeServiceListener(serviceType, this);
        jmdns.unregisterAllServices();
        try {
            jmdns.close();
            MeshLog.i("JMDNS SERVICE STOP");
        } catch (Exception ex) {

        }

        jmdns = null;

        try {
            lock.release();
        } catch (Exception releaseLock) {
            //AppLog.v("jmdns failed lock.release()");
        }
    }


    private void startPublishInternal(int port) {
        JSONObject jo = new JSONObject();
        try {
            jo.put("hi", "value");
        } catch (JSONException e) {
        }

        ServiceInfo serviceInfo = ServiceInfo.create(serviceType, serviceName, port, jo.toString());
        try {
            jmdns.registerService(serviceInfo);
        } catch (IOException | NullPointerException ex) {
            //AppLog.v("jmd failed registerService()");
        }
    }

    private void startResolveInternal() {
        jmdns.addServiceListener(serviceType, this);
    }

    private boolean startJmdns(InetAddress address) {
        manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        lock = manager.createMulticastLock("BnjTransport");
        lock.setReferenceCounted(true);
        try {
            lock.acquire();
            jmdns = JmDNS.create(address);
            //AppLog.v("jmdns bind address {}" + address);
        } catch (IOException ex) {
            try {
                lock.release();
            } catch (Exception releaseLock) {
            }
            //AppLog.v("jmdns failed jmdns.create() {}");
            return false;
        }

        running = true;
        //AppLog.v("jmdns started");

        return true;
    }


    /****************** ServiceListener************/

    @Override
    public void serviceAdded(ServiceEvent event) {
        if (event.getName().equals(this.serviceName))
            return;
        jmdns.requestServiceInfo(event.getType(), event.getName());
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        listener.serviceLost(event);
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (event.getName().equals(this.serviceName))
            return;
        final String name = event.getName();
        final String address = event.getInfo().getHostAddress();
        final int port = event.getInfo().getPort();

        //MeshLog.e( "serviceResolved  addre=" + address + " port =" + port);

        queue.dispatch(new Runnable() {
            @Override
            public void run() {
                listener.onBonjourServiceResolved(name, address, port);
            }
        });
    }

}
