package com.w3engineers.mesh.tunnel;

import android.content.Context;
import android.net.Network;

import com.w3engineers.mesh.bluetooth.ConnectionStateListener;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.httpservices.NanoHTTPServer;
import com.w3engineers.mesh.queue.MeshLibMessageEventQueue;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.queue.MessageDispatcher;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.WiFiUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RemoteManager implements NanoHTTPServer.HttpDataListenerFromInternet {

    private MessageDispatcher mMessageDispatcher;
    private ConnectionStateListener remoteListener;
    private ExecutorService callableExecutor;
    private MeshLibMessageEventQueue messageEventQueue;
    private final Context mContext;
    private String myUserId;

    public RemoteManager(ConnectionStateListener connectionStateListener, MessageDispatcher messageDispatcher, Context mContext, MeshLibMessageEventQueue discoveryEventQueue, String myUserId) {
        remoteListener = connectionStateListener;
        mMessageDispatcher = messageDispatcher;
        this.mContext = mContext;
        callableExecutor = Executors.newFixedThreadPool(1);
        this.messageEventQueue = discoveryEventQueue;
        NanoHTTPServer.setHttpDataListenerForInternet(this::receivedInternetData);
        this.myUserId = myUserId;
    }


    public Integer postData(String userId, String data) throws ExecutionException, InterruptedException {
//        MeshLog.v("FILE_SPEED_TEST_4.5 " + Calendar.getInstance().getTime());
        String url = "http://" + userId + "." + TunnelConstant.dotRemoteUrl +
                ":" + TunnelConstant.serverHTTPPort;
        MeshLog.e("Remote Manager postData to :: " + url + " data: " + data);

        Future<Integer> futureTask = callableExecutor.submit(() -> {

            OkHttpClient client;
            OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS);


            Network network = WiFiUtil.getConnectedMobileNetwork(this.mContext);
            if (network != null) {
                builder.socketFactory(network.getSocketFactory());
            } else {
                MeshLog.e("Network null not able to set mobile socketfactory");
            }

            builder.retryOnConnectionFailure(true);
            client = builder.build();


//            OkHttpClient client = new OkHttpClient.Builder()
//                    .connectTimeout(30, TimeUnit.SECONDS)
//                    .writeTimeout(30, TimeUnit.SECONDS)
//                    .readTimeout(30, TimeUnit.SECONDS)
//                    .build();


            RequestBody formBody = new FormBody.Builder()
                    .add("internet_data", data)
                    .build();

            int responseCode = 0;

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("x-api-key", "PIHesVZTJ0")
                    .addHeader("id", myUserId)
                    .build();

            try {

                Response response = client.newCall(request).execute();
                if (response != null) {
                    responseCode = response.code() == 200 ? 1 : 0;
                    MeshLog.i("SSH TUNNEL response: " + response.toString());
                    MeshLog.i("SSH TUNNEL responseCode " + responseCode);
                    response.body().close();
                }
            } catch (IOException e) {
                MeshLog.e("Remote manger Exception" + e.toString());
                e.printStackTrace();
            }
            return responseCode;
        });

        return futureTask.get();
    }


    @Override
    public void receivedInternetData(String ipAddress, String data, String immediateSender) {
        MeshLog.v("FILE_SPEED_TEST_9 " + Calendar.getInstance().getTime());
        try {
            JSONObject jsonObject = new JSONObject(data);
//            mMessageDispatcher.addReceiveMessage(MessageBuilder.buildRemoteReceiveMessage(this, jsonObject));
            int type = jsonObject.optInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
            jsonObject.put(JsonDataBuilder.KEY_BUDDY_ID, immediateSender);

            if (JsonDataBuilder.isAppMessage(type)) {
                messageEventQueue.addAppMessageInQueue(() -> processReceivedData(jsonObject));
            } else {
                messageEventQueue.execute(() -> processReceivedData(jsonObject));
            }
            /*new Thread(new Runnable() {
                @OverrideBle
                public void run() {
                    processReceivedData(jsonObject);
                }
            }).start();*/

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void processReceivedData(JSONObject jsonObject) {
        MeshLog.e("processReceivedData" + jsonObject.toString());
        int type = jsonObject.optInt(JsonDataBuilder.KEY_MESSAGE_TYPE);
        String buddyId = jsonObject.optString(JsonDataBuilder.KEY_BUDDY_ID);
        switch (type) {
            case JsonDataBuilder.APP_MESSAGE:

                MeshLog.e("APP_MESSAGE");
                String senderId = jsonObject.optString(JsonDataBuilder.KEY_SENDER_ID);
                String receiverId = jsonObject.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                String messageId = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                String message = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE);


                remoteListener.onMessageReceived(senderId, receiverId, messageId, message.getBytes(), null, buddyId);


            case JsonDataBuilder.REMOTE_USER_CONNECTED:
                List<String> users = new ArrayList<>();
                users.add(jsonObject.optString(JsonDataBuilder.KEY_SENDER_INFO));
                if (users.size() > 0) {
                    remoteListener.onReceivedRemoteUsersId(users);
                }
                break;

            case JsonDataBuilder.REMOTE_USER_DISCONNECTED:
                String leaveUserID = jsonObject.optString(JsonDataBuilder.KEY_SENDER_INFO);
                if (leaveUserID != null) {
                    remoteListener.onReceivedRemoteUsersLeaveId(leaveUserID);
                }
                break;

            case JsonDataBuilder.REMOTE_USERS_CONNECTED:
                JSONArray jsonArray = (JSONArray) jsonObject.opt(JsonDataBuilder.KEY_SENDER_INFO);
                ArrayList<String> remoteUserList = new ArrayList<>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            remoteUserList.add(jsonArray.getString(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if (remoteUserList.size() > 0) {
                        remoteListener.onReceivedRemoteUsersId(remoteUserList);
                    }
                }
                break;
            case JsonDataBuilder.REMOTE_USERS_DETAILS_REQUEST:
                String userInfoInfo = jsonObject.optString(JsonDataBuilder.KEY_SENDER_INFO);
                String connectedBuyers = jsonObject.optString(JsonDataBuilder.KEY_CONNECTED_BUYERS);
                remoteListener.onReceivedUserDetailsFromRemoteUser(userInfoInfo, connectedBuyers);
                break;
            case JsonDataBuilder.REMOTE_USERS_DETAILS_RESPONSE:
                userInfoInfo = jsonObject.optString(JsonDataBuilder.KEY_SENDER_INFO);
                connectedBuyers = jsonObject.optString(JsonDataBuilder.KEY_CONNECTED_BUYERS);
                remoteListener.onReceivedUserDetailsResponseFromRemoteUser(userInfoInfo, connectedBuyers);
                break;
            case JsonDataBuilder.ACK_MESSAGE:
                senderId = jsonObject.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jsonObject.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                messageId = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                String ackBody = jsonObject.optString(JsonDataBuilder.KEY_ACK_BODY);
                int status = jsonObject.optInt(JsonDataBuilder.KEY_ACK_STATUS);
                remoteListener.onReceivedMsgAck(senderId, receiverId, messageId, status, ackBody, null);
                break;
            case JsonDataBuilder.APP_FILE_MESSAGE:
                senderId = jsonObject.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jsonObject.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                message = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE);
                messageId = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                remoteListener.onFileMessageReceived(senderId, receiverId, messageId, message, buddyId);
                break;
            case JsonDataBuilder.BUYER_FILE_MESSAGE:
                senderId = jsonObject.optString(JsonDataBuilder.KEY_SENDER_ID);
                receiverId = jsonObject.optString(JsonDataBuilder.KEY_RECEIVER_ID);
                message = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE);
                int fileMessageType = jsonObject.optInt(JsonDataBuilder.KEY_BUYER_FILE_ACK);
                messageId = jsonObject.optString(JsonDataBuilder.KEY_MESSAGE_ID);
                remoteListener.onReceiveBuyerFileMessage(senderId, receiverId, message, fileMessageType, buddyId, messageId);
                break;

            case JsonDataBuilder.BUYER_LIST:
                senderId = jsonObject.optString(JsonDataBuilder.KEY_SENDER_ID);
                String dataInfo = jsonObject.optString(JsonDataBuilder.KEY_BUYERS_DETAILS_INFO);
                ConcurrentLinkedQueue<RoutingEntity> userList = GsonUtil.on().getEntityQueue(dataInfo);

                remoteListener.onInternetUserReceived(senderId, userList);

                break;

            case JsonDataBuilder.BUYER_LEAVE:
                String leaveUserId = jsonObject.optString(JsonDataBuilder.KEY_LEAVE_NODE_ID);
                String hopId = jsonObject.optString(JsonDataBuilder.KEY_HOP_NODE_ID);
                remoteListener.onMeshLinkDisconnect(leaveUserId, hopId);
                break;

            case JsonDataBuilder.HANDSHAKE_PING:
                HandshakeInfo handshakeInfo = GsonUtil.on().handshakeInfoFromString(jsonObject.toString());
                remoteListener.onHandshakeInfoReceived(handshakeInfo);
                break;
        }


    }
}
