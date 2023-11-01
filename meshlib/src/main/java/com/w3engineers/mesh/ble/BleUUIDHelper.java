package com.w3engineers.mesh.ble;

import android.util.Log;

import com.w3engineers.mesh.util.MeshLog;

import java.util.UUID;

public class BleUUIDHelper {
    private static final String POSITIVE_PREFIX = "A";
    private static final String NEGATIVE_PREFIX = "B";
    private static final String DELIMITER = "-";
    private static final String PARTIAL_UUID = "7D2EA28A-F7BD-485A-BD9D-92";

    private static final String PREFIX_TAG = "[BLE_PROCESS]";


    /**
     * The user ID is who actually broadcast
     *
     * @param userId Broadcast user ID
     * @return
     */
    private static String generateUUIDString(String userId) {
        // First create a hash
        String uuid = "";
        int hash = userId.hashCode();

        if (hash < 0) {
            uuid += NEGATIVE_PREFIX;
        } else {
            uuid += POSITIVE_PREFIX;
        }

        hash = Math.abs(hash);
        uuid += hash;

        uuid = PARTIAL_UUID + uuid;
        return uuid;
    }

    public static UUID generateUUID(String userId) {
        return UUID.fromString(userId);
    }

    public static boolean isDesireUUID(String serviceUUID, String userId) {
        String[] arr = serviceUUID.split(DELIMITER);
        int hash = userId.hashCode();
        if (arr.length == 5) {
            String value = arr[4];
            // Removing first letter not need it.
            value = value.substring(2);

            MeshLog.v(PREFIX_TAG + " First extract: " + value);

            String prefix = String.valueOf(value.charAt(0));

            MeshLog.v(PREFIX_TAG + " First extrac2: " + prefix);

            // now removing 2nd letter
            value = value.substring(1);

            MeshLog.v(PREFIX_TAG + " First extrac3: " + value);

            int receivedHash = Integer.parseInt(value);
            if (prefix.toUpperCase().equals(NEGATIVE_PREFIX)) {
                receivedHash = -1 * receivedHash;
            }

            return receivedHash == hash;
        }

        return false;
    }
}
