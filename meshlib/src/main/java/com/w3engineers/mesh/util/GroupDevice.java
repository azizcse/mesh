package com.w3engineers.mesh.util;


import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Provides Group related utility for P2p connection-formation
 */
public class GroupDevice implements Parcelable {

    private String groupName;
    private String deviceName;
    private String deviceMac;
    private String ipAddress;

    private int deviceServerSocketPort;

    private String customName;

    public GroupDevice() {

    }
    public GroupDevice(BluetoothDevice device) {
        this.deviceName = device.getName();
        this.deviceMac = device.getAddress();
    }


    public GroupDevice(WifiP2pDevice device) {
        this.deviceName = device.deviceName;
        this.deviceMac = device.deviceAddress;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getDeviceServerSocketPort() {
        return deviceServerSocketPort;
    }

    public void setDeviceServerSocketPort(int deviceServerSocketPort) {
        this.deviceServerSocketPort = deviceServerSocketPort;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("GroupDevice[deviceName=").append(deviceName).append("][deviceMac=").append(deviceMac).append("][ipAddress=").append(ipAddress).append("][deviceServerSocketPort=").append(deviceServerSocketPort).append("]").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupDevice that = (GroupDevice) o;

        if (deviceName != null ? !deviceName.equals(that.deviceName) : that.deviceName != null)
            return false;
        return deviceMac != null ? deviceMac.equals(that.deviceMac) : that.deviceMac == null;
    }

    @Override
    public int hashCode() {
        int result = deviceName != null ? deviceName.hashCode() : 0;
        result = 31 * result + (deviceMac != null ? deviceMac.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceName);
        dest.writeString(deviceMac);
        dest.writeString(deviceName);
    }

    public static Creator<GroupDevice> CREATOR = new Creator<GroupDevice>() {
        @Override
        public GroupDevice createFromParcel(Parcel source) {
            return null;
        }

        @Override
        public GroupDevice[] newArray(int size) {
            return new GroupDevice[0];
        }
    };
}
