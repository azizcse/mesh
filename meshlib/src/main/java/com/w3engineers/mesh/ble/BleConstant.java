package com.w3engineers.mesh.ble;

import com.w3engineers.mesh.BuildConfig;

import java.util.UUID;

public class BleConstant {

    //public static String SERVICE_STRING = BuildConfig.BLE_SERVICE_UUID;
    public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

    public static String CHARACTERISTIC_ECHO_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID CHARACTERISTIC_ECHO_UUID = UUID.fromString(CHARACTERISTIC_ECHO_STRING);

    public static String CHARACTERISTIC_TIME_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID CHARACTERISTIC_TIME_UUID = UUID.fromString(CHARACTERISTIC_TIME_STRING);

    public static String CLIENT_CONFIGURATION_DESCRIPTOR_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID CLIENT_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString(CLIENT_CONFIGURATION_DESCRIPTOR_STRING);

    public static final String CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID = "2902";


    public static final int MANUFACTURE_ID = 63534;
    public static final int MANUFACTURE_ID_SPECIAL = 63533;
    /**
     * Please do not change the length of this data. It would impact negatively on discovery
     */
    public static final String MANUFACTURE_DATA = "O"; // Only one Character
    public static final String MANUFACTURE_DATA_SPECIAL = "N"; // Only one Character

    public static final String FORCE = "D";
    public static final String NORMAL = "I";
}
