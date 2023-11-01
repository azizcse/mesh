package com.w3engineers.mesh.db.routing;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.ext.strom.application.data.helper.local.base.BaseEntity;
import com.w3engineers.ext.strom.util.collections.Matchable;
import com.w3engineers.mesh.db.meta.TableMeta;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.AddressUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Entity of Routing
 */

//mMessageId is forced from super class, will change that in future version release
@Entity(tableName = TableMeta.TableNames.ROUTING)
public class RoutingEntity extends BaseEntity implements Matchable<String>, Comparable<RoutingEntity> {

    @Override
    public String getMatcher() {
        return mAddress;
    }

    @Override
    public int compareTo(RoutingEntity o) {
        if (getAddress() == null || o.getAddress() == null) {
            return 0;
        }
        return getAddress().compareTo(o.getAddress());
    }


    public interface Type {
        int UNDEFINED = 0;
        int WiFi = 1;
        int BT = 2;
        int WifiMesh = 3;
        int BtMesh = 4;
        int INTERNET = 5;
        int HB = 6;
        int HB_MESH = 7;
        int HS = 8;
        int CLIENT = 9;
        int BLE = 10;
        int BLE_MESH = 11;

        /**
         * Currently this type is not used in DB. We would make similarities on overall mode
         * decisions in future.
         */
        int GO = 10;
        int LC = 11;
        int NONE = -1;
    }

    public RoutingEntity(String address) {
        this.mAddress = address;
    }


    /**
     * Ethereum mAddress of node
     */
    @SerializedName("id")
    @NonNull
    @ColumnInfo(name = TableMeta.ColumnNames.ADDRESS)
    String mAddress;

    /**
     * User public key
     */
    @SerializedName("pk")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_PUBLIC_KEY)
    String publicKey;

    /**
     * The node is BT/P2P/Internet
     */
    @SerializedName("ut")
    @ColumnInfo(name = TableMeta.ColumnNames.TYPE)
    int mType;

    /**
     * mIp mAddress if applicable
     */
    @SerializedName("ip")
    @ColumnInfo(name = TableMeta.ColumnNames.IP)
    String mIp;

    /**
     * Ethereum mAddress of next hop node
     */
    @SerializedName("hid")
    @ColumnInfo(name = TableMeta.ColumnNames.NEXT_HOP)
    String mHopAddress;

    /**
     * Whether node is reachable right now or not.
     * Public for Room's internal usage.
     */
    @SerializedName("on")
    @ColumnInfo(name = TableMeta.ColumnNames.IS_ONLINE)
    boolean mIsOnline;

    /**
     * Represent latest update mTime
     */
    @SerializedName("t")
    @ColumnInfo(name = TableMeta.ColumnNames.TIME)
    long mTime = System.currentTimeMillis();

    @SerializedName("hc")
    @ColumnInfo(name = TableMeta.ColumnNames.HOP_COUNT)
    int mHopCount;

    @SerializedName("um")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_USER_MODE)
    int userMode;


    @NonNull
    public String getAddress() {
        return mAddress;
    }

    public void setAddress(@NonNull String address) {
        mAddress = address;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public String getIp() {
        return mIp;
    }

    public void setIp(String ip) {
        mIp = ip;
    }

    public String getHopAddress() {
        return mHopAddress;
    }

    public void setHopAddress(String hopAddress) {
        mHopAddress = hopAddress;
    }

    public boolean isOnline() {
        return mIsOnline;
    }

    public void setOnline(boolean online) {
        mIsOnline = online;
    }

    public void setHopCount(int count) {
        this.mHopCount = count;
    }

    public int getHopCount() {
        return mHopCount;
    }

    public int getUserMode() {
        return userMode;
    }

    public void setUserMode(int userMode) {
        this.userMode = userMode;
    }

    public void setTime(long time) {
        this.mTime = time;
    }

    public long getTime() {
        return mTime;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public static final Creator<RoutingEntity> CREATOR = new Creator<RoutingEntity>() {
        @Override
        public RoutingEntity createFromParcel(Parcel source) {
            return new RoutingEntity(source);
        }

        @Override
        public RoutingEntity[] newArray(int size) {
            return new RoutingEntity[size];
        }
    };

    protected RoutingEntity(Parcel in) {
        super(in);
        this.mAddress = in.readString();
        this.publicKey = in.readString();
        this.mHopAddress = in.readString();
        this.mIp = in.readString();
        this.mType = in.readInt();
        this.mIsOnline = in.readInt() == 1;
        this.mHopCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mAddress);
        dest.writeString(publicKey);
        dest.writeString(mHopAddress);
        dest.writeString(mIp);
        dest.writeInt(mType);
        dest.writeInt(this.mIsOnline ? 1 : 0);
        dest.writeInt(this.mHopCount);
    }

    /**
     * Reset meta data except it's ID entry
     */
    public void resetMetaData() {
        setIp(null);
        setHopAddress(null);
        setOnline(false);
    }

    @Override
    public String toString() {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).
                format(new Date(mTime));
        return "RoutingEntity{" +
                "mAddress='" + AddressUtil.makeShortAddress(mAddress) + '\'' +
                ", mType=" + mType +
                ", mIp='" + mIp + '\'' +
                ", mHopAddress='" + AddressUtil.makeShortAddress(mHopAddress) + '\'' +
                ", mIsOnline=" + mIsOnline +
                ", time=" + time +
                ", hopCount=" + mHopCount +
                ", id=" + getId() +
                '}';
    }

    public static boolean isBtNode(RoutingEntity routingEntity) {
        return routingEntity != null && routingEntity.getType() == Type.BT;
    }

    public static boolean isBleNode(RoutingEntity routingEntity) {
        return routingEntity != null && routingEntity.getType() == Type.BLE;
    }

    public static boolean isWiFiNode(String routingEntityId) {
        return isWiFiNode(RouteManager.getInstance().getEntityByAddress(routingEntityId));
    }

    public static boolean isWiFiNode(RoutingEntity routingEntity) {
        return routingEntity != null && (routingEntity.getType() == Type.WiFi ||
                routingEntity.getType() == Type.HB || routingEntity.getType() == Type.HS || routingEntity.getType() == Type.INTERNET);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof RoutingEntity)) return false;
        RoutingEntity o = (RoutingEntity) obj;
        return o.getAddress().equals(this.getAddress());
    }

    @Override
    public int hashCode() {
        return mAddress.hashCode();
    }
}
