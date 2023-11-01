package com.w3engineers.mesh.linkcash;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.db.meta.TableMeta;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.util.AddressUtil;

/**
 * POJO for NodeInfo
 */

@Entity(tableName = TableMeta.TableNames.NODE_INFO)
public class NodeInfo implements Cloneable {
    @SerializedName("id")
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = TableMeta.ColumnNames.COL_USER_ID)
    private String userId;
    @SerializedName("hid")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_HOP_ID)
    private String hopId;
    @SerializedName("ip")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_IP_ADDRESS)
    private String ipAddress;
    @SerializedName("pk")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_PUBLIC_KEY)
    private String publicKey;
    @SerializedName("um")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_USER_MODE)
    private int userMode;
    @SerializedName("ut")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_USER_TYPE)
    private int userType;
    /*@SerializedName("sn")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_SSID_NAME)
    private String ssidName;
    @SerializedName("bn")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_BT_NAME)
    private String bleName;*/
    /*@SerializedName("ui")
    private String userInfo;*/

    @SerializedName("t")
    @ColumnInfo(name = TableMeta.ColumnNames.COL_CREATED_TIME)
    public long mGenerationTime = System.currentTimeMillis();

    @SerializedName("on")
    @Ignore
    public boolean isOnline;

    @SerializedName("ib")
    @Ignore
    public boolean isBleNode;

    public NodeInfo(String userId, /*String ssidName, String bleName, */String publicKey, int userMode, int userType) {
        this.userId = userId;
       /* this.ssidName = ssidName;
        this.bleName = bleName;*/
        this.publicKey = publicKey;
        this.userMode = userMode;
        this.userType = userType;
    }

    public NodeInfo() {
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }


    public void setUserType(int userType) {
        this.userType = userType;
    }

    public int getUserType() {
        return this.userType;
    }

    public int getUserMode() {
        return userMode;
    }

    public void setUserMode(int userMode) {
        this.userMode = userMode;
    }

   /* public String getSsidName() {
        return ssidName;
    }

    public String getBleName() {
        return bleName;
    }

    public void setBleName(String bleName) {
        this.bleName = bleName;
    }

    public void setSsidName(String ssidName) {
        this.ssidName = ssidName;
    }
*/
    /*public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }*/

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHopId() {
        return hopId;
    }

    public void setHopId(String hopId) {
        this.hopId = hopId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "userId='" + AddressUtil.makeShortAddress(userId) + '\'' +
                ", hopId='" + AddressUtil.makeShortAddress(hopId) + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", userMode=" + userMode +
                ", userType=" + userType +
               /* ", ssidName='" + ssidName + '\'' +
                ", bleName='" + bleName + '\'' +*/
                '}';
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static class Builder {
        private String userId;
        private String hopId;
        private String ipAddress;
        private String publicKey;
        private int userMode;
        private int userType;
        /*private String ssidName;
        private String bleName;
        private String userInfo;*/
        private long mTime;
        private boolean isBle;

        public Builder() {
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setHopId(String hopId) {
            this.hopId = hopId;
            return this;
        }

        public Builder setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder setPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder setUserMode(int userMode) {
            this.userMode = userMode;
            return this;
        }

        public Builder setUserType(int userType) {
            this.userType = userType;
            return this;
        }

       /* public Builder setSsidName(String ssidName) {
            this.ssidName = ssidName;
            return this;
        }

        public Builder setBleName(String bleName) {
            this.bleName = bleName;
            return this;
        }

        public Builder setUserInfo(String userInfo) {
            this.userInfo = userInfo;
            return this;
        }*/

        public Builder setTime(long time) {
            this.mTime = time;
            return this;
        }

        public Builder setBle(boolean isBle) {
            this.isBle = isBle;
            return this;
        }

        public synchronized NodeInfo build() {
            NodeInfo nodeInfo = new NodeInfo();
            if (!TextUtils.isEmpty(userId)) {
                nodeInfo.setUserId(this.userId);
            }
            if (!TextUtils.isEmpty(hopId)) {
                nodeInfo.setHopId(this.hopId);
            }

            if (!TextUtils.isEmpty(ipAddress)) {
                nodeInfo.setIpAddress(this.ipAddress);
            }
            /*if (!TextUtils.isEmpty(bleName)) {
                nodeInfo.setBleName(this.bleName);
            }
            if (!TextUtils.isEmpty(ssidName)) {
                nodeInfo.setSsidName(this.ssidName);
            }*/
            if (!TextUtils.isEmpty(publicKey)) {
                nodeInfo.setPublicKey(this.publicKey);
            }
            /*if (!TextUtils.isEmpty(userInfo)) {
                nodeInfo.setUserInfo(this.userInfo);
            }*/
            if (userMode >= 0) {
                nodeInfo.setUserMode(userMode);
            }
            if (userType >= 0) {
                nodeInfo.setUserType(userType);
            }

            if (mTime > 0) {
                nodeInfo.mGenerationTime = mTime;
            }

            nodeInfo.isBleNode = isBle;

            return nodeInfo;
        }
    }
}
