package com.w3engineers.mesh.libmeshx.wifid;

/**
 * Constants for P2P
 */
public class Constants {

    public interface Service {
        /**
         * Identity of broadcasting service. Try to keep it short. It has cost with discovery
         * performance.
         */

        String TYPE = "plk.c";
        String BT_NAME = "asn";
        long DISCOVERY_DELAY = 1000;//In ms
    }

    public static int APP_PORT = 0;
}
