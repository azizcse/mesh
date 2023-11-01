package com.w3engineers.mesh;

import com.w3engineers.mesh.wifi.dispatch.DispatchQueue;
import com.w3engineers.mesh.wifi.protocol.MeshTransport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h1>Purpose to control all transport from here</h1>
 */
public class CombineTransport implements MeshTransport {
    private List<MeshTransport> transports = new ArrayList<>();
    public DispatchQueue queue;
    private boolean running;

    public CombineTransport() {
        queue = new DispatchQueue();
    }

    /**
     * <p>Add all transport to list</p>
     *
     * @param transport : variable argument so that we can add at a time
     */
    public void addTransport(MeshTransport... transport) {
        this.transports.addAll(Arrays.asList(transport));
    }

    public boolean isRunning(){
        return running;
    }

    @Override
    public void start() {
        if (running)
            return;

        running = true;

        queue.dispatch(() -> {
            for (MeshTransport transport : transports) {
                transport.start();
            }
        });
    }

    @Override
    public void stop() {
        if (!running)
            return;

        running = false;

        queue.dispatch(() -> {
            for (MeshTransport transport : transports) {
                transport.stop();
            }
        });
    }

    @Override
    public void forceStop() {
        if (!running)
            return;

        running = false;
        queue.dispatch(() -> {
            for (MeshTransport transport : transports) {
                transport.forceStop();
            }
        });
    }

    @Override
    public void restart() {
        if (running)
            return;

        running = true;
        queue.dispatch(() -> {
            for (MeshTransport transport : transports) {
                transport.restart();
            }
        });
    }
}
