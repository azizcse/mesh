package com.w3engineers.mesh.ble.message;

public interface BleMessageHelper {
    int CHUNK_SIZE = 450; // The size is bytes. Ideal byte limit is 509 if MTU 512. But 450 is working for all devices
    String MESSAGE_ID = "ble_msg_" + System.currentTimeMillis();
    int HEADER_BYTE_SIZE = 5;
    int MAX_DATA_SIZE_IN_KILO_BYTE = 999;
    //int MAX_DATA_SIZE_IN_KILO_BYTE = 1;
    int MAX_DATA_SIZE_IN_BYTE = MAX_DATA_SIZE_IN_KILO_BYTE * 1024; // bytes -> 999 KB

    int PASSWORD_LENGTH = 8;


    interface MessageType {
        byte APP_MESSAGE = 0x1;
        byte CREDENTIAL_MESSAGE = 0x2;
        byte CLIENT_IDENTITY = 0x3;
        byte FORCE_CONNECTION = 0x4;
        byte FORCE_CONNECTION_REPLY = 0x5;
        byte FILE_CONTENT_MESSAGE = 0x6;
        byte BLE_HEART_BIT = 0x7;
        byte APP_FILE_MESSAGE = 0x8;
    }

}
