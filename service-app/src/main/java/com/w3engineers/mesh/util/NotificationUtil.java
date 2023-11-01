package com.w3engineers.mesh.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.apkupdate.Constants;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.TeleMeshService;


/**
 * <p>Purpose to show notification from service layer
 * when client app not bounded with service app then
 * service app will show notification for the specific
 * user or app </p>
 */
public class NotificationUtil {
    private static final String CHANNEL_NAME = "client-msg";
    private static final String CHANNEL_ID = "notification_channel";
    private static final int MAX = 100;

    private static NotificationCompat.Builder progressBuilder;

    public static void showNotification(Context context, String userId, String userName) {
        HandlerUtil.postForeground(() -> {

            Intent intent = Utils.getInstance().getAppPackage(context);
            if (intent == null) {
                Toaster.init(R.color.telemeshColorPrimary);
                Toaster.showShort("Please install Telemesh App");
                return;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            NotificationCompat.Builder builder = buildNotification(context);
            builder.setContentIntent(pendingIntent);
            prepareNotification(builder, userName);
            showNotification(context, builder, userId);
        });
    }

    /**
     * This notification will show when client app not running but service app running.
     * And app update available.
     * When  user click on this notification then Client app will be open.
     * <p>
     * After client app open again client app will bind with service app
     * and get a popup for app update app available information
     *
     * @param context     Service app context
     * @param packageName Client app package name
     */
    public static void showClientAppUpdateAvailableNotification(Context context, String packageName, String appName) {
        HandlerUtil.postBackground(() -> {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

                NotificationCompat.Builder builder = buildNotification(context);
                builder.setContentIntent(pendingIntent);
                prepareAppUpdateAppNotification(builder, appName);
                showNotification(context, builder, packageName);
            }
        });

    }

    /**
     * This notification is using to show app update available information
     * for Service app.
     * For showing notification we are using self user ID because may be
     * other user ID have other notification like message
     *
     * @param context  App context
     * @param myUSerId Self id to set notification ID
     */
    public static void showUpdateConfirmationNotification(Context context, String myUSerId, String receiverId) {
        HandlerUtil.postForeground(() -> {
            NotificationCompat.Builder builder = buildNotification(context);

            prepareAppUpdateUpdateNotification(builder, context, receiverId);

            showNotification(context, builder, myUSerId);
        });
    }

    public static void showAppUpdateProgress(Context context, String myUserId) {
        HandlerUtil.postForeground(() -> {
            progressBuilder = buildNotification(context);
            prepareProgressNotification(progressBuilder, context);
            showNotification(context, progressBuilder, myUserId);
        });
    }

    public static void updateProgress(Context context, String myUserId, int progress) {
        HandlerUtil.postForeground(() -> {
            if (progressBuilder == null) return;

            NotificationManager notificationManager = (NotificationManager) context.getSystemService
                    (Context.NOTIFICATION_SERVICE);
            int notifyId = Math.abs(myUserId.hashCode());
            progressBuilder.setProgress(MAX, progress, false);
            if (notificationManager != null) {
                notificationManager.notify(notifyId, progressBuilder.build());
            }
        });
    }

    public static void showSuccessErrorNotification(Context context, boolean isSuccess, String appPath, String myUserId) {
        HandlerUtil.postForeground(() -> {
            NotificationCompat.Builder builder = buildNotification(context);
            prepareSuccessErrorNotification(builder, context, isSuccess, appPath);
            showNotification(context, builder, myUserId);
        });
    }

    private static NotificationCompat.Builder buildNotification(Context context) {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            //channel.enableVibration(true);

            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    private static void prepareNotification(NotificationCompat.Builder builder, String userName) {

        builder.setWhen(System.currentTimeMillis())
                .setContentText("New message from " + userName)
                .setContentTitle("Telemesh")
                .setAutoCancel(true)
                .setTicker(userName)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(com.w3engineers.mesh.R.drawable.ic_launcher_mesh_rnd)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);
    }

    private static void prepareAppUpdateAppNotification(NotificationCompat.Builder builder, String appName) {
        builder.setWhen(System.currentTimeMillis())
                .setContentText("An update version of " + appName + " is available. You can download without Internet!.")
                .setContentTitle("Offline App update available")
                .setAutoCancel(true)
                .setTicker(appName)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(com.w3engineers.mesh.R.drawable.ic_launcher_mesh_rnd)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);
    }

    private static void showNotification(Context context, NotificationCompat.Builder builder, String userId) {
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService
                (Context.NOTIFICATION_SERVICE);
        int notifyId = Math.abs(userId.hashCode());
        if (notificationManager != null) {
            notificationManager.notify(notifyId, notification);
        }
    }

    private static void prepareAppUpdateUpdateNotification(NotificationCompat.Builder builder, Context context, String receiverId) {
        Intent appUpdateServiceIntent = new Intent(context, TeleMeshService.class);
        appUpdateServiceIntent.setAction(TeleMeshService.ACTION_APP_UPDATE_REQUEST);
        appUpdateServiceIntent.putExtra(TeleMeshService.class.getName(), receiverId);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, appUpdateServiceIntent, PendingIntent.FLAG_ONE_SHOT);

        builder.setWhen(System.currentTimeMillis())
                .setContentTitle(context.getString(R.string.app_update))
                .setContentText("New version " + context.getString(R.string.is_available_for_telemesh))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.mipmap.ic_wallet, context.getString(R.string.update), pendingIntent);
    }

    private static void prepareProgressNotification(NotificationCompat.Builder builder, Context context) {
        builder.setWhen(System.currentTimeMillis())
                .setContentTitle(context.getString(R.string.downloading_service_please_wait))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.mipmap.ic_launcher);
        builder.setProgress(MAX, 0, false);
    }

    private static void prepareSuccessErrorNotification(NotificationCompat.Builder builder, Context context, boolean isSuccess, String appPath) {

        String title;
        String message;

        if (isSuccess) {
            title = context.getString(R.string.success_title);
            message = context.getString(R.string.app_update_success_message);
        } else {
            title = context.getString(R.string.error_title);
            message = context.getString(R.string.app_update_error_message);
        }

        builder.setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.mipmap.ic_launcher);

        if (isSuccess) {
            Intent appUpdateServiceIntent = new Intent(context, TeleMeshService.class);
            appUpdateServiceIntent.setAction(TeleMeshService.ACTION_APP_UPDATE_SUCCESS);
            appUpdateServiceIntent.putExtra(Constants.InAppUpdate.APP_PATH_KEY, appPath);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, appUpdateServiceIntent, PendingIntent.FLAG_ONE_SHOT);

            builder.addAction(R.mipmap.ic_wallet, context.getString(R.string.app_install), pendingIntent);
        }

    }


    public static void removeNotification(Context context, String myUserId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService
                (Context.NOTIFICATION_SERVICE);
        int notifyId = Math.abs(myUserId.hashCode());
        if (notificationManager != null) {
            notificationManager.cancel(notifyId);
        }
    }

}
