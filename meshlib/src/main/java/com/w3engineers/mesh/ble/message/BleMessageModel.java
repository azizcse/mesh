package com.w3engineers.mesh.ble.message;

import android.bluetooth.BluetoothDevice;

public class BleMessageModel {

    public String receiverId;
    public String messageId;
    public byte[] data;
    public BluetoothDevice device;
    public byte messageType = 1;

    /**
     * This variable will be used for two purpose.
     * 1. For sender side. Current chunk number that already sent.
     * 2. For receiver side. Total current received data size
     */
    public int currentChunk = 0;

    /**
     * This variable will be use for two purpose.
     * <p>
     * 1. For sender side: Total chunk count. Chunk come from total_size/base_size
     * 2. For receiver side total data length.
     */
    public int totalChunk = 0;

    public long enterTime;
}
