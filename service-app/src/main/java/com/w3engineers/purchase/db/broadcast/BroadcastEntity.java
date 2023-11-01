package com.w3engineers.purchase.db.broadcast;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.purchase.db.TableInfo;


@Entity(tableName = TableInfo.TABLE_BROADCAST,
        indices = {@Index(value = {TableInfo.Column.BROADCAST_ID}, unique = true)})
public class BroadcastEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = TableInfo.Column.ID)
    public int id;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_ID)
    public String broadcastId;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_USER_ID)
    public String broadcastUserId;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_META_DATA)
    public String broadcastMetadata;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_CONTENT_PATH)
    public String broadcastContentPath;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_LATITUDE)
    public double latitude;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_LONGITUDE)
    public double longitude;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_RANGE)
    public double broadcastRange;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_EXPIRE_TIME)
    public long broadcastExpireTime;

    @ColumnInfo(name = TableInfo.Column.APP_TOKEN)
    public String appToken;

    @ColumnInfo(name = TableInfo.Column.BROADCAST_RECEIVE_STATUS)
    public int broadcastReceiveStatus;

    public BroadcastEntity(String broadcastId, String broadcastUserId, String broadcastMetadata,
                           String broadcastContentPath, String broadcastContentMeta, long expireTime,
                           String appToken, double latitude, double longitude, double range) {
        this.broadcastId = broadcastId;
        this.broadcastUserId = broadcastUserId;
        this.broadcastMetadata = broadcastMetadata;
        this.broadcastContentPath = broadcastContentPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.broadcastRange = range;
        this.broadcastExpireTime = expireTime;
        this.appToken = appToken;
    }

    public BroadcastEntity toBroadcastEntity(BroadcastData broadcastData, String userId, long expiryTime, int status) {
        this.broadcastId = broadcastData.getBroadcastId();
        this.broadcastUserId = userId;
        this.broadcastMetadata = broadcastData.getMetaData();
        this.broadcastContentPath = broadcastData.getContentPath();
        this.latitude = broadcastData.getLatitude();
        this.longitude = broadcastData.getLongitude();
        this.broadcastRange = broadcastData.getRange();
        this.broadcastExpireTime = expiryTime;
        this.appToken = broadcastData.getAppToken();
        this.broadcastReceiveStatus = status;

        return this;
    }

    public BroadcastData toBroadcastData() {
        BroadcastData broadcastData = new BroadcastData();
        broadcastData.setBroadcastId(broadcastId);
        broadcastData.setMetaData(broadcastMetadata);
        broadcastData.setContentPath(broadcastContentPath);
        broadcastData.setAppToken(appToken);

        broadcastData.setLatitude(latitude);
        broadcastData.setLongitude(longitude);
        broadcastData.setExpiryTime(broadcastExpireTime + "");
        broadcastData.setRange(broadcastRange);

        return broadcastData;
    }

    public BroadcastEntity toBroadcastEntity(Broadcast broadcast) {
        this.broadcastId = broadcast.getBroadcastId();
        this.broadcastUserId = broadcast.getSenderId();
        this.broadcastMetadata = broadcast.getBroadcastMeta();
        this.broadcastContentPath = broadcast.getContentPath();
        this.latitude = broadcast.getLatitude();
        this.longitude = broadcast.getLongitude();
        this.broadcastRange = broadcast.getRange();
        this.broadcastExpireTime = broadcast.getExpiryTime();
        this.appToken = broadcast.getAppToken();
        return this;
    }

    public Broadcast toBroadcast(String senderId) {
        return new Broadcast().setBroadcastId(broadcastId)
                .setSenderId(senderId)
                .setAppToken(appToken)
                .setBroadcastMeta(broadcastMetadata)
                .setContentPath(broadcastContentPath)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setRange(broadcastRange)
                .setExpiryTime(broadcastExpireTime)
                .setType(JsonDataBuilder.APP_BROADCAST_MESSAGE);
    }

    public BroadcastEntity() {
    }
}
