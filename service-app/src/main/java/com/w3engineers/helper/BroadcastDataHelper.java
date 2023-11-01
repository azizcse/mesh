package com.w3engineers.helper;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.letbyte.core.meshfilesharing.api.support.mesh.SupportTransportManager;
import com.w3engineers.ext.strom.App;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.helper.callback.BroadcastCallback;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.peers.PeersEntity;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.TimeUtil;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.purchase.db.DatabaseService;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;
import com.w3engineers.purchase.db.broadcast_track.BroadcastTrackEntity;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public class BroadcastDataHelper {

    private static BroadcastDataHelper broadcastDataHelper = new BroadcastDataHelper();
    private static String KEY_BROADCAST_EXPIRE = "e";
    private static String KEY_META_DATA = "rawData";

    private BroadcastCallback broadcastCallback = null;
    private DatabaseService databaseService;
    private com.w3engineers.mesh.datasharing.database.DatabaseService appDatabaseService;
    private Queue<BroadcastEntity> mBroadcastDataQueue;

    private BroadcastDataHelper() {
        databaseService = DatabaseService.getInstance(MeshApp.getContext());
        appDatabaseService = com.w3engineers.mesh.datasharing.database.DatabaseService.getInstance(MeshApp.getContext());
        mBroadcastDataQueue = new LinkedList<>();
    }

    public static BroadcastDataHelper getInstance() {
        return broadcastDataHelper;
    }

    public void setBroadcastCallback(BroadcastCallback broadcastCallback) {
        this.broadcastCallback = broadcastCallback;
    }

    private void sendBroadcastDataToApp(BroadcastData broadcastData) {
        if (broadcastCallback != null) {
            broadcastCallback.sendDataToApp(broadcastData, true);
        }
    }

    public void sendBroadCastMessage(BroadcastData broadcastData) {
        long expiryOnLocalTime = TimeUtil.getServerTimeToMillis(broadcastData.getExpiryTime());

        MeshLog.i("ExpiryOnLocalTime: " + expiryOnLocalTime);
        MeshLog.i("CurrentTime: " + System.currentTimeMillis());

        BroadcastEntity broadcastEntity = new BroadcastEntity().toBroadcastEntity(broadcastData, getOwnAddress(),
                expiryOnLocalTime, Utils.BroadcastReceiveStatus.RECEIVED);
        databaseService.insertBroadcastMessage(broadcastEntity);

        List<RoutingEntity> connectedDirectUsers = RouteManager.getInstance().getConnectedDirectUsers();
        // expiryOnLocalTime = 0; no expiry set for this Broadcast-Content
        // expiryOnLocalTime time > current time; It should broadcast; as still it has validity
        if (expiryOnLocalTime == 0 || expiryOnLocalTime >= System.currentTimeMillis()) {

            if (CollectionUtil.hasItem(connectedDirectUsers)) {
                mBroadcastDataQueue.add(broadcastEntity);
                distributeAmongLocalConnectedUsers(connectedDirectUsers);
            }
        } else {
            MeshLog.i("Broadcast content validity expire!!!");
        }
    }

    public void distributeAmongLocalConnectedUsers(List<RoutingEntity> connectedDirectUsers) {

        Iterator<BroadcastEntity> broadcastEntityIterator = mBroadcastDataQueue.iterator();

        while (broadcastEntityIterator.hasNext()) {

            if (CollectionUtil.hasItem(connectedDirectUsers)) {
                BroadcastEntity broadcastEntity = mBroadcastDataQueue.poll();

                for (RoutingEntity routingEntity : connectedDirectUsers) {
                    if (broadcastEntity != null) {

                        try {

                            if (broadcastEntity.latitude != 0 && broadcastEntity.longitude != 0) {

                                PeersEntity peersEntity = appDatabaseService.getPeersById(routingEntity.
                                        getAddress(), broadcastEntity.appToken);

                                if (peersEntity.getUserLatitude() != 0 && peersEntity.getUserLongitude() != 0 && LocationHelper.getInstance()
                                        .userDistanceIsInRatio(peersEntity.getUserLatitude(), peersEntity.getUserLongitude(),
                                                broadcastEntity.latitude, broadcastEntity.longitude, broadcastEntity.broadcastRange)) {

                                    Broadcast broadcast = broadcastEntity.toBroadcast(getOwnAddress()).setReceiverId(routingEntity.getAddress());
                                    SupportTransportManager.getInstance().getMeshFileCommunicator().sendBroadcast(broadcast);

                                    trackBroadcastUsers(broadcastEntity.broadcastId, routingEntity.getAddress(), Utils.BroadcastReceiveStatus.PROGRESS);

                                }

                            } else {

                                Broadcast broadcast = broadcastEntity.toBroadcast(getOwnAddress()).setReceiverId(routingEntity.getAddress());
                                SupportTransportManager.getInstance().getMeshFileCommunicator().sendBroadcast(broadcast);

                                trackBroadcastUsers(broadcastEntity.broadcastId, routingEntity.getAddress(), Utils.BroadcastReceiveStatus.PROGRESS);
                            }
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void sendBroadcastData(BroadcastEntity broadcastEntity, String userId) {
        if (broadcastCallback != null && broadcastCallback
                .getPeerConnectionType(userId) != RoutingEntity.Type.INTERNET) {

            Broadcast broadcast = broadcastEntity.toBroadcast(getOwnAddress()).setReceiverId(userId);
            SupportTransportManager.getInstance().getMeshFileCommunicator().sendBroadcast(broadcast);

            int status = Utils.BroadcastReceiveStatus.PROGRESS;
            trackBroadcastUsers(broadcastEntity.broadcastId, userId, status);
        }
    }

    // When new user connected
    private void sendUnsentBroadcast(List<String> broadcastIds, String userId) {
        if (broadcastCallback != null && broadcastCallback.isDirectConnected(userId)) {

            List<BroadcastEntity> unsentBroadcast = getUnsentBroadcast(broadcastIds);
            MeshLog.i("UnsentBroadcast: " + unsentBroadcast.toString());

            if (CollectionUtil.hasItem(unsentBroadcast)) {
                for (BroadcastEntity broadcastEntity : unsentBroadcast) {
                    // expiryOnLocalTime = 0; no expiry set for this Broadcast-Content
                    // expiryOnLocalTime time > current time; It should broadcast; as still it has validity

                    if (broadcastEntity.broadcastExpireTime == 0 || broadcastEntity.broadcastExpireTime >= System.currentTimeMillis()) {

                        if (broadcastEntity.latitude != 0 && broadcastEntity.longitude != 0) {
                            try {
                                PeersEntity peersEntity = appDatabaseService.getPeersById(userId, broadcastEntity.appToken);

                                if (peersEntity.getUserLatitude() != 0 && peersEntity.getUserLongitude() != 0
                                        && LocationHelper.getInstance().userDistanceIsInRatio(peersEntity.getUserLatitude(),
                                        peersEntity.getUserLongitude(), broadcastEntity.latitude, broadcastEntity.longitude, broadcastEntity.broadcastRange)) {

                                    sendBroadcastData(broadcastEntity, userId);
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            sendBroadcastData(broadcastEntity, userId);
                        }
                    }
                }
            }
        }
    }

    public void pingBroadcastHandshaking(String receiverId, String token) {

        if (broadcastCallback != null && broadcastCallback.isDirectConnected(receiverId)
                && broadcastCallback.getPeerConnectionType(receiverId) != RoutingEntity.Type.INTERNET) {

            List<String> unsentBroadcastIds = getUnsentBroadcastIds(receiverId, token);

            if (!unsentBroadcastIds.isEmpty()) {
                HandshakeInfo handshakeInfo = new HandshakeInfo(getOwnAddress(), receiverId, receiverId, JsonDataBuilder.HANDSHAKE_BROADCAST);
                handshakeInfo.setAppToken(token);
                handshakeInfo.setBroadcastIds(unsentBroadcastIds);

                broadcastCallback.getTransPort().sendHandshakeInfo(handshakeInfo, false);
            }
        }
    }

    public void processBroadcastHandshaking(HandshakeInfo handshakeInfo) {
        List<String> broadcastIds = handshakeInfo.getBroadcastIds();

        if (broadcastIds != null && !broadcastIds.isEmpty()) {

            for (String broadcastId : broadcastIds) {
                trackBroadcastUsers(broadcastId, handshakeInfo.getSenderId(),
                        Utils.BroadcastReceiveStatus.RECEIVED);
            }

            List<String> syncBroadcastIds = receivedBroadcastIds(broadcastIds, handshakeInfo.getAppToken());

            List<String> requestedBroadcastIds = new ArrayList<>(broadcastIds);

            for (String broadcastId : syncBroadcastIds) {
                requestedBroadcastIds.remove(broadcastId);
            }

            HandshakeInfo newHandshakeInfo = new HandshakeInfo(getOwnAddress(), handshakeInfo.getSenderId(), handshakeInfo.getSenderId(), JsonDataBuilder.HANDSHAKE_BROADCAST);
            newHandshakeInfo.setAppToken(handshakeInfo.getAppToken());
            newHandshakeInfo.setSyncBroadcastIds(syncBroadcastIds);
            newHandshakeInfo.setRequestBroadcastIds(requestedBroadcastIds);

            broadcastCallback.getTransPort().sendHandshakeInfo(newHandshakeInfo, false);
        } else {

            List<String> syncBroadcastIds = handshakeInfo.getSyncBroadcastIds();
            List<String> requestedBroadcastIds = handshakeInfo.getRequestBroadcastIds();

            if (syncBroadcastIds != null && !syncBroadcastIds.isEmpty()) {

                for (String broadcastId : syncBroadcastIds) {
                    trackBroadcastUsers(broadcastId, handshakeInfo.getSenderId(),
                            Utils.BroadcastReceiveStatus.RECEIVED);
                }
            }

            if (requestedBroadcastIds != null && !requestedBroadcastIds.isEmpty()) {
                sendUnsentBroadcast(requestedBroadcastIds, handshakeInfo.getSenderId());
            }
        }
    }

    public void onBroadcastReceive(String broadcastId) {
        try {
            BroadcastEntity broadcastEntity = databaseService.getBroadcastEntity(broadcastId);

            BroadcastAck broadcastAck = new BroadcastAck()
                    .setBroadcastId(broadcastEntity.broadcastId)
                    .setSenderId(broadcastEntity.broadcastUserId);

            broadcastAckReceive(broadcastAck, true);

            List<RoutingEntity> connectedUsers = fetchTargetUsersToLocalBroadcast(broadcastEntity.broadcastUserId);

            // expiryOnLocalTime = 0; no expiry set for this Broadcast-Content
            // expiryOnLocalTime time > current time; It should broadcast; as still it has validity
            if (broadcastEntity.broadcastExpireTime == 0 || broadcastEntity.broadcastExpireTime >= System.currentTimeMillis()) {
                if (CollectionUtil.hasItem(connectedUsers)) {
                    // Store on Queue
                    BroadcastDataHelper.getInstance().addBroadcastDataToQueue(broadcastEntity);
                    // broadcast the content locally
                    BroadcastDataHelper.getInstance().distributeAmongLocalConnectedUsers(connectedUsers);
                }
            } else {
                MeshLog.i("Broadcast Content expired");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastAckSend(String broadcastId, String senderId) {
        if (broadcastCallback != null && broadcastCallback.getTransPort() != null) {

            BroadcastAck broadcastAck = new BroadcastAck()
                    .setBroadcastId(broadcastId).setSenderId(getOwnAddress())
                    .setReceiverId(senderId).setType(JsonDataBuilder.APP_BROADCAST_ACK_MESSAGE);

            byte[] broadcastAckData = GsonUtil.on().broadcastAckToString(broadcastAck).getBytes();

            broadcastCallback.getTransPort().sendMessage(senderId, broadcastAckData);
        }
    }

    public void broadcastAckReceive(BroadcastAck broadcastAck, boolean isReceiveMode) {
        if (!TextUtils.isEmpty(broadcastAck.getBroadcastId())) {
            try {
                BroadcastTrackEntity broadcastTrackEntity = databaseService.getBroadcastEntityByReceiver(
                        broadcastAck.getBroadcastId(), broadcastAck.getSenderId());
                if (broadcastTrackEntity != null) {
                    broadcastTrackEntity.broadcastSendStatus = Utils.BroadcastReceiveStatus.RECEIVED;
                    databaseService.insertBroadcastTrack(broadcastTrackEntity);
                } else {
                    if (isReceiveMode) {
                        trackBroadcastUsers(broadcastAck.getBroadcastId(), broadcastAck.getSenderId(),
                                Utils.BroadcastReceiveStatus.RECEIVED);
                    }
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onReceiveBroadcastAndExistCheck(Broadcast broadcast) {
        try {

            BroadcastEntity broadcastEntity = databaseService.getBroadcastEntity(broadcast.getBroadcastId());

            if (broadcastEntity == null) {
                int receiveStatus = Utils.BroadcastReceiveStatus.RECEIVED;
                if (!TextUtils.isEmpty(broadcast.getContentPath()))
                    receiveStatus = Utils.BroadcastReceiveStatus.PROGRESS;

                broadcastEntity = new BroadcastEntity().toBroadcastEntity(broadcast);
                broadcastEntity.broadcastReceiveStatus = receiveStatus;
                databaseService.insertBroadcastMessage(broadcastEntity);

                if (receiveStatus == Utils.BroadcastReceiveStatus.RECEIVED) {
                    sendBroadcastDataToApp(toBroadcastData(broadcast));
                }

                return false;
            } else {

                int receiveStatus = broadcastEntity.broadcastReceiveStatus;
                if (receiveStatus == Utils.BroadcastReceiveStatus.FAILED) {

                    receiveStatus = Utils.BroadcastReceiveStatus.RECEIVED;
                    if (!TextUtils.isEmpty(broadcast.getContentPath()))
                        receiveStatus = Utils.BroadcastReceiveStatus.PROGRESS;

                    broadcastEntity.broadcastReceiveStatus = receiveStatus;
                    databaseService.insertBroadcastMessage(broadcastEntity);

                    if (receiveStatus == Utils.BroadcastReceiveStatus.RECEIVED) {
                        sendBroadcastDataToApp(toBroadcastData(broadcast));
                    }

                    return false;
                }

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private BroadcastData toBroadcastData(Broadcast broadcast) {
        BroadcastData broadcastData = new BroadcastData();

        broadcastData.setBroadcastId(broadcast.getBroadcastId());
        broadcastData.setMetaData(broadcast.getBroadcastMeta());
        broadcastData.setContentPath(broadcast.getContentPath());
        broadcastData.setAppToken(broadcast.getAppToken());

        broadcastData.setLatitude(broadcast.getLatitude());
        broadcastData.setLongitude(broadcast.getLongitude());
        broadcastData.setExpiryTime("" + broadcast.getExpiryTime());
        broadcastData.setRange(broadcast.getRange());

        return broadcastData;
    }

    private void trackBroadcastUsers(String broadcastId, String userId, int sendStatus) {
        BroadcastTrackEntity broadcastTrackEntity = new BroadcastTrackEntity();
        broadcastTrackEntity.broadcastMessageId = broadcastId;
        broadcastTrackEntity.broadcastTrackUserId = userId;
        broadcastTrackEntity.broadcastSendStatus = sendStatus;

        databaseService.insertBroadcastTrack(broadcastTrackEntity);
    }

    public void updateReceivedBroadcast(String broadcastId) {
        databaseService.updateBroadcastStatus(broadcastId, Utils.BroadcastReceiveStatus.DELIVERED);
    }

    public List<BroadcastEntity> getUnreceivedBroadcast(String tokenName) {
        try {
            return databaseService.getUnreceivedBroadcast(tokenName, Utils.BroadcastReceiveStatus.RECEIVED);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<BroadcastEntity> getUnsentBroadcast(List<String> broadcastIds) {
        try {
            return databaseService.getUnsentBroadcast(broadcastIds);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<String> getUnsentBroadcastIds(String userId, String tokenName) {
        try {
            return databaseService.getUnsentBroadcastIds(userId, tokenName);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<String> receivedBroadcastIds(List<String> ids, String appToken) {
        try {
            return databaseService.receivedBroadcastIds(ids, appToken);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private String getOwnAddress() {
        return SharedPref.read(Utils.KEY_NODE_ADDRESS);
    }

    public void broadcastContentSavedConfirmation(String broadcastId, String contentPath) {

        // scanMediaToNextShare(contentPath);
        Log.v("MIMO_SAHA:", "Broadcast Confirmation");
        try {
            BroadcastEntity broadcastEntity = databaseService.getBroadcastEntity(broadcastId);
            broadcastEntity.broadcastReceiveStatus = Utils.BroadcastReceiveStatus.RECEIVED;
            broadcastEntity.broadcastContentPath = contentPath;

            databaseService.insertBroadcastMessage(broadcastEntity);
            MeshLog.i("Broadcast Content Saved Confirmation Received");
            onBroadcastReceive(broadcastId);

            sendBroadcastDataToApp(broadcastEntity.toBroadcastData());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void scanMediaToNextShare(String contentPath) {
        File file = new File(contentPath);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        App.getContext().sendBroadcast(mediaScanIntent);
    }

    public void broadcastContentSendConfirmation(String broadcastId, String userId, String contentPath) {
        trackBroadcastUsers(broadcastId, userId, Utils.BroadcastReceiveStatus.RECEIVED);
    }

    @Nullable
    public List<RoutingEntity> fetchTargetUsersToLocalBroadcast(String senderId) {

        if (TextUtils.isEmpty(senderId)) {
            MeshLog.e("Sender found NULL !!!");
            return null;
        }

        List<RoutingEntity> connectedUsers = null;

        RoutingEntity senderEntity = RouteManager.getInstance().getShortestPath(senderId);

        if (senderEntity != null) {
            if (senderEntity.getType() == RoutingEntity.Type.HB) {
                // active BT direct users
                connectedUsers = RouteManager.getInstance().getUsersByType(RoutingEntity.Type.BT);
                // active wifi direct users
                connectedUsers.addAll(RouteManager.getInstance().getUsersByType(RoutingEntity.Type.WiFi));

            } else if (senderEntity.getType() == RoutingEntity.Type.BT) {
                // active AdHoc direct users
                connectedUsers = RouteManager.getInstance().getUsersByType(RoutingEntity.Type.HB);
                // active wifi direct users
                connectedUsers.addAll(RouteManager.getInstance().getUsersByType(RoutingEntity.Type.WiFi));

            } else if (senderEntity.getType() == RoutingEntity.Type.WiFi) {
                // active BLE direct users
                connectedUsers = RouteManager.getInstance().getUsersByType(RoutingEntity.Type.BLE);
                // active AdHoc direct users
                //connectedUsers.addAll(RouteManager.getInstance().getUsersByType(RoutingEntity.Type.HB));
            } else if (senderEntity.getType() == RoutingEntity.Type.BLE) {
                // active BLE direct users
                connectedUsers = RouteManager.getInstance().getUsersByType(RoutingEntity.Type.WiFi);
                // active Wifi direct users
               // connectedUsers.addAll(RouteManager.getInstance().getUsersByType(RoutingEntity.Type.WiFi));
            } else {
                MeshLog.i("Sender should verified :: " + senderEntity.toString());
            }
        }
        return connectedUsers;
    }

    // add content on Queue
    public void addBroadcastDataToQueue(BroadcastEntity broadcastEntity) {
        if (mBroadcastDataQueue != null) {
            mBroadcastDataQueue.add(broadcastEntity);
        }
    }

    public void failedBroadcastContent(String broadcastId, String userId) {
        MeshLog.e("failedBroadcastContent");
        try {
            databaseService.deleteBroadcastTrack(broadcastId, userId);

            BroadcastEntity broadcastEntity = databaseService.getBroadcastEntity(broadcastId);

            if (broadcastEntity != null && broadcastEntity.broadcastReceiveStatus == Utils.BroadcastReceiveStatus.PROGRESS) {
                broadcastEntity.broadcastReceiveStatus = Utils.BroadcastReceiveStatus.FAILED;
                databaseService.insertBroadcastMessage(broadcastEntity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void failedContentBroadcastForDisconnect(String userId) {
        MeshLog.e("failedContentBroadcastForDisconnect");
        try {
            databaseService.deleteBroadcastTrackForDisconnect(userId, Utils.BroadcastReceiveStatus.PROGRESS);

            databaseService.updateBroadcastStatus(userId, Utils.BroadcastReceiveStatus.PROGRESS,
                    Utils.BroadcastReceiveStatus.FAILED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void failedContentBroadcastForMyDisconnection() {

        MeshLog.e("failedContentBroadcastForMyDisconnection");

        try {
            databaseService.deleteTrackDuringMyDisconnection(Utils.BroadcastReceiveStatus.PROGRESS);

            databaseService.updateBroadcastStatus(Utils.BroadcastReceiveStatus.PROGRESS,
                    Utils.BroadcastReceiveStatus.FAILED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
