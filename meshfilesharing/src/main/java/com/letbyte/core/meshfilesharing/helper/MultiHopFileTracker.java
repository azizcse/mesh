package com.letbyte.core.meshfilesharing.helper;

public class MultiHopFileTracker {
    public long filePacketSendTime;
    public long lastProgress;
    public String sourceAddress;
    public long transferId;
    public long comparisonTime;

    public MultiHopFileTracker(long filePacketSendTime, long lastProgress,
                               String sourceAddress, long transferId, long comparisonTime) {
        this.filePacketSendTime = filePacketSendTime;
        this.lastProgress = lastProgress;
        this.sourceAddress = sourceAddress;
        this.transferId = transferId;
        this.comparisonTime = comparisonTime;
    }
}
