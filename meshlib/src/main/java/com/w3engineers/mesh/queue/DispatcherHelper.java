package com.w3engineers.mesh.queue;

import com.w3engineers.mesh.queue.messages.BTDiscoveryMessage;
import com.w3engineers.mesh.util.MeshLog;

/**
 * Queued event Dispatcher Helper
 */
public class DispatcherHelper {

    public final Object lock = new Object();
    public volatile int mCountBTDiscovering;
    private static DispatcherHelper sDispatcherHelper = new DispatcherHelper();

    private DispatcherHelper() {}

    public static DispatcherHelper getDispatcherHelper() {
        return sDispatcherHelper;
    }

    /**
     * @return whether any {@link BTDiscoveryMessage} available in the queue or not
     */
    public boolean isNotBTConnecting() {
        boolean isNotConnecting = mCountBTDiscovering < 1;
        MeshLog.i("[isNotBTConnecting]::"+isNotConnecting);

//        return isNotConnecting;
        // FIXME: 10/22/19 We do not want to allow other interface to achieve best performance now.
        //  Upon fixing cycle related thing will re enable and enhance performance
        return true;
    }

}
