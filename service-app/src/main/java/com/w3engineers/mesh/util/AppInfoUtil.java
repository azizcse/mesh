package com.w3engineers.mesh.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.manager.AppUpdateAppDataManager;
import com.w3engineers.purchase.db.appupdateappinfo.AppUpdateInfoEntity;

import java.io.File;
import java.text.DecimalFormat;

/**
 * This class is use to find client app additional information from service
 * layer. Because we have client app package name so by tis we can extract all
 * additional information from device.
 * <p>
 * Motif: We don;t need to transfer every information in client side. We can handle
 * it from here like client app version, name, size etc.
 * <p>
 * Developed: Md Tariqul Islam
 */
public class AppInfoUtil {

    /**
     * This client app information means App name,
     * version,
     * size etc.
     * <p>
     * Here we will check if the received version is lower then we will ad an entry from this
     * side.
     * NOTE: We will not send info to server if the version is greater or equal.
     */
    public static void processClientAppInformation(Context context, String myId, String senderId,
                                                   String appToken, int version, String versionName) {

        //First get self app information
        String myAppVersionName;
        int myAppVersion;
        String appName;
        long updatedAppSize = 0;
        try {

            PackageInfo pInfo = context.getPackageManager().getPackageInfo(appToken, 0);
            myAppVersion = pInfo.versionCode;
            myAppVersionName = pInfo.versionName;

            if (myAppVersion <= version) return;

            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(pInfo.packageName, 0);

            long size = new File(applicationInfo.publicSourceDir).length();
            if (size > 0) {
                updatedAppSize = size;
            }

            appName = context.getPackageManager().getApplicationLabel(applicationInfo).toString();

            AppUpdateInfoEntity entity = new AppUpdateInfoEntity();

            entity.appName = appName;
            entity.appSize = updatedAppSize;

            entity.packageName = appToken;

            entity.isChecking = true; // It is just version checking. Not app update yet

            entity.myUserId = myId;
            entity.receiverUserId = senderId;

            entity.selfVersionCode = myAppVersion;
            entity.selfVersionName = myAppVersionName;

            entity.receiverVersionCode = version;
            entity.receiverVersionName = versionName;

            entity.timeStamp = System.currentTimeMillis();

            AppUpdateAppDataManager.saveAppCheckingInfo(entity);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * Purpose: To check app version when client app
     * is not running but service is.
     * If self version is low the return with app name and prepare an notification
     * <p>
     * If appName is Null then we can understand that self version is high or same
     *
     * @param context  Application context
     * @param appToken App package name
     * @param version  Received app version
     * @return App Name
     */
    public static String getAppNameWhenVersionLow(Context context, String appToken, int version) {
        int myAppVersion;
        MeshLog.v("APP TOKEN  " + appToken);
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(appToken, 0);
            myAppVersion = pInfo.versionCode;

            if (myAppVersion < version) {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(pInfo.packageName, 0);


                return context.getPackageManager().getApplicationLabel(applicationInfo).toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isClientAppDebuggable(String packageName) {
        boolean debuggable = false;
        PackageManager pm = MeshApp.getContext().getPackageManager();
        try {
            ApplicationInfo appinfo = pm.getApplicationInfo(packageName, 0);
            debuggable = (0 != (appinfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return debuggable;
    }

    private static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
