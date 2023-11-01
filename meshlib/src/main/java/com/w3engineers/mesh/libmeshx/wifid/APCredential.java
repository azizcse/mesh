package com.w3engineers.mesh.libmeshx.wifid;

import com.w3engineers.mesh.util.MeshLog;

public class APCredential {

    public String mSSID, mPassPhrase;
    public long discoverTime;
    public int attemptCount;
    public String ethId = "";
    public boolean isSpecialSearch;
    public String macAddress;

    public APCredential(String SSID, String passPhrase, long discoverTime) {
        this.mSSID = SSID;
        this.mPassPhrase = passPhrase;
        this.discoverTime = discoverTime;
        MeshLog.v("[Subnetmerge]:" + this);
    }

    public APCredential(String ethId, String SSID, String passPhrase, long discoverTime) {
        this.ethId = ethId;
        this.mSSID = SSID;
        this.mPassPhrase = passPhrase;
        this.discoverTime = discoverTime;
        MeshLog.v("[Subnetmerge]:" + this);
    }

    public APCredential(String mSSID, String mPassPhrase, String ethId, String macAddress, boolean isSpecialSearch) {
        this.mSSID = mSSID;
        this.mPassPhrase = mPassPhrase;
        this.ethId = ethId;
        this.isSpecialSearch = isSpecialSearch;
        this.macAddress = macAddress;
    }

    @Override
    public String toString() {
        return "APCredentials{" +
                "mSSID='" + mSSID + '\'' +
                ", mPassPhrase='" + mPassPhrase + '\'' +
                '}';
    }
}
