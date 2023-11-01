package com.w3engineers.mesh.util;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.App;
import com.w3engineers.mesh.db.users.UserEntity;
import com.w3engineers.meshrnd.R;

import com.w3engineers.models.UserInfo;

import java.util.HashMap;


/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

public class NotifyUtil {
    private static final String CHANNEL_NAME = "tele_mesh";
    private static final String CHANNEL_ID = "notification_channel_3";
    private static HashMap<String, Integer> broadcastNotificationMap = new HashMap<>();

    public static void showNotification(UserInfo userInfo) {
        Context context = App.getContext();

        Intent intent = Utils.getInstance().getAppPackage(context);
        if (intent == null) {
            Toaster.init(R.color.telemeshColorPrimary);
            Toaster.showShort("Please install Telemesh App");
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        String message = "You have a message";



        if (userInfo != null) {


            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = getNotificationBuilder(context);
            builder.setContentIntent(pendingIntent);
            prepareNotification(builder, message, userInfo.getAvatar(), userInfo.getUserName());

            showNotification(context, builder, userInfo.getAddress());
        }
    }

    public static void showBroadcastNotification(String appToken) {
        Context context = App.getContext();
        Intent intent = Utils.getInstance().getAppByPackage(context, appToken);
        if (intent == null) {
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Integer notificationCount = broadcastNotificationMap.get(appToken);
        if (notificationCount == null) {
            notificationCount = 0;
        }
        notificationCount = notificationCount + 1;

        broadcastNotificationMap.put(appToken, notificationCount);

        String message = String.format(context.getResources().getString(R.string.broadcast_notification),
                notificationCount);
        String appName = context.getResources().getString(R.string.app_name);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = getNotificationBuilder(context);
        builder.setContentIntent(pendingIntent);

        prepareSimpleNotification(builder, message, appName);
        showNotification(context, builder, appToken);
    }

    public static void removeBroadcastNotification(String appToken) {
        broadcastNotificationMap.remove(appToken);
        clearNotification(appToken);
    }


    private static NotificationCompat.Builder getNotificationBuilder(Context context) {
        String channelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            channelId = channel.getId();
        } else {
            channelId = CHANNEL_ID;
        }

        return new NotificationCompat.Builder(context, channelId);
    }

    private static void prepareNotification(NotificationCompat.Builder builder, String message, int avatarIndex, String userName) {


        Bitmap imageBitmap = ImageUtil.getUserImageBitmap(avatarIndex);


        builder.setWhen(TimeUtil.toCurrentTime())
                .setContentText(message+" from "+userName)
                .setContentTitle(userName)
                .setTicker(userName)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[0])
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.mipmap.ic_app_launcher)
                .setLargeIcon(imageBitmap);

//        if (SharedPref.getSharedPref(App.getContext()).readBoolean(Constants.preferenceKey.IS_NOTIFICATION_ENABLED)) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);
//        }

    }

    private static void prepareSimpleNotification(NotificationCompat.Builder builder, String message, String appName) {

        Bitmap imageBitmap = BitmapFactory.decodeResource(App.getContext().getResources(), R.mipmap.ic_launcher);

        builder.setWhen(TimeUtil.toCurrentTime())
                .setContentText(message)
                .setContentTitle(appName)
                .setTicker(appName)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[0])
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.mipmap.ic_app_launcher)
                .setLargeIcon(imageBitmap);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);
    }

    /**
     * Responsible to show notification
     *
     * @param builder(required) need to
     * @param id(required)      notification id
     */
    private static void showNotification(Context context, NotificationCompat.Builder builder, String id) {

        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyId = Math.abs(id.hashCode());
        if (notificationManager != null) {
            notificationManager.notify(notifyId, notification);
        }
    }

    public static void clearNotification(String appToken) {
        Context context = App.getContext();
        if (Text.isNotEmpty(appToken)) {
            int notificationId = Math.abs(appToken.hashCode());
            NotificationManager manager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(notificationId);
            }
        }
    }

    public static void removeAllNotification() {
        Log.d("NotificationTest", "notification removed method call");
        NotificationManager notificationManager = (NotificationManager) App.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager!=null){
            notificationManager.cancelAll();
        }
    }
}
