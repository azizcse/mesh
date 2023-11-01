package com.w3engineers.mesh.manager;

import android.content.pm.ApplicationInfo;
import android.net.Network;
import android.util.Log;

import com.w3engineers.eth.data.helper.callback.AppUpdateAppInfoUploadCallback;
import com.w3engineers.eth.data.remote.parse.AppUpdateAppParseInfo;
import com.w3engineers.eth.data.remote.parse.ParseManager;
import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.util.AppInfoUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.purchase.db.DatabaseService;
import com.w3engineers.purchase.db.appupdateappinfo.AppUpdateInfoEntity;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class AppUpdateAppDataManager {

    public static void saveAppUpdateAppInfo(String myUserId, String receiverId, String packageName) {

        /*
         * First we will check the -> app checking info <- from local database.
         * The main logic is here we definitely get data from local DB.
         * Because before app update we already saved both side information in local DB
         * And it does not matter that app checking info saved or not.
         *
         * Because we don't delete app checking information from database just update
         * */


        //Check client app and service app both are release or not
        boolean isServiceAppDebug = ((MeshApp.getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        boolean isClientAppDebug = AppInfoUtil.isClientAppDebuggable(packageName);

        if (isClientAppDebug || isServiceAppDebug) {
            MeshLog.i("Service app in debug: " + isServiceAppDebug + "  Client app in debug: " + isClientAppDebug);
            // We will return here because both app not in release
            //return;
        }

        AppUpdateInfoEntity previousAppCheckingInfo = DatabaseService.getInstance(MeshApp.getContext())
                .getCurrentAppCheckingInfo(myUserId, receiverId, packageName);

        if (NetworkMonitor.isOnline()) {
            AppUpdateAppParseInfo info = convertEntityToParseInfo(previousAppCheckingInfo);
            info.isChecking = false;
            info.timestamp = System.currentTimeMillis();
            ParseManager.getInstance().sendAppUpdateAppInfo(previousAppCheckingInfo.id, info, id -> DatabaseService
                    .getInstance(MeshApp.getContext())
                    .updateSyncedAppUpdateInformation(id));
        } else {
            previousAppCheckingInfo.id = 0; // Reset ID  for new Info
            previousAppCheckingInfo.isChecking = false;
            previousAppCheckingInfo.timeStamp = System.currentTimeMillis();

            DatabaseService.getInstance(MeshApp.getContext()).insertAppUpdateAppInfo(previousAppCheckingInfo);
        }
    }

    public static void saveAppCheckingInfo(AppUpdateInfoEntity entity) {

        //Check client app and service app both are release or not
        boolean isServiceAppDebug = ((MeshApp.getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        boolean isClientAppDebug = AppInfoUtil.isClientAppDebuggable(entity.packageName);

        if (isClientAppDebug || isServiceAppDebug) {
            MeshLog.i("Service app in debug: " + isServiceAppDebug + "  Client app in debug: " + isClientAppDebug);
            // We will return here because both app not in release
            //return;
        }

        // First we always save app checking info in local database.
        DatabaseService.getInstance(MeshApp.getContext()).insertAppUpdateAppInfo(entity);

        if (NetworkMonitor.isOnline()) {
            AppUpdateAppParseInfo info = convertEntityToParseInfo(entity);
            ParseManager.getInstance().sendAppUpdateAppInfo(0, info, null);

            AppUpdateInfoEntity result = DatabaseService
                    .getInstance(MeshApp.getContext())
                    .getCurrentAppCheckingInfo(entity.myUserId, entity.receiverUserId, entity.packageName);

            DatabaseService
                    .getInstance(MeshApp.getContext())
                    .updateSyncedAppUpdateInformation(result.id);
        }
    }

    public static void initNetWorkListener() {
        NetworkMonitor.setNetworkInterfaceListeners((isOnline, network, isWiFi) -> {
            Log.d("AppUpdateInfo", "IsOnline: " + isOnline);
            if (isOnline) {
                try {
                    List<AppUpdateInfoEntity> appUpdateInfoList = DatabaseService
                            .getInstance(MeshApp.getContext())
                            .getAllAppUpdateAppInfo();

                    if (appUpdateInfoList != null) {
                        for (AppUpdateInfoEntity entity : appUpdateInfoList) {
                            if (ParseManager.getInstance() != null) {

                                AppUpdateAppParseInfo info = convertEntityToParseInfo(entity);

                                ParseManager.getInstance().sendAppUpdateAppInfo(entity.id, info, id -> {
                                    if (entity.isChecking) {
                                        DatabaseService
                                                .getInstance(MeshApp.getContext())
                                                .deleteAppUpdateAppInfo(id);
                                    } else {
                                        DatabaseService
                                                .getInstance(MeshApp.getContext())
                                                .updateSyncedAppUpdateInformation(id);
                                    }

                                });
                            }
                        }
                    }

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    Log.e("AppUpdateInfo", "Database fetch error: " + e.getMessage());
                }
            }
        });
    }

    public static AppUpdateAppParseInfo convertEntityToParseInfo(AppUpdateInfoEntity entity) {
        AppUpdateAppParseInfo info = new AppUpdateAppParseInfo();
        info.appName = entity.appName;
        info.appSize = entity.appSize;
        info.isChecking = entity.isChecking;
        info.packageName = entity.packageName;
        info.receiverAppVersionCode = entity.receiverVersionCode;
        info.receiverAppVersionName = entity.receiverVersionName;
        info.receiverId = entity.receiverUserId;
        info.senderAppVersionCode = entity.selfVersionCode;
        info.senderAppVersionName = entity.selfVersionName;
        info.senderUserId = entity.myUserId;
        info.timestamp = entity.timeStamp;

        return info;
    }


    public static AppUpdateInfoEntity getAppUpdateInfo(){
        AppUpdateInfoEntity entity = new AppUpdateInfoEntity();

        entity.appName = "Test Update";
        entity.appSize = 20;

        entity.packageName = "com.w3engineers.testupdate";

        entity.isChecking = false;

        entity.myUserId = "0xf57d787f3ca95e2fd9cc782c85f6bcd3d6d779d9";
        entity.receiverUserId = "0xc1a5185c807038a32a4c6ca020826fee85d88fde";

        entity.selfVersionCode = 200;
        entity.selfVersionName = "2.0.0";

        entity.receiverVersionCode = 200;
        entity.receiverVersionName = "1.0.0";

        entity.timeStamp = System.currentTimeMillis();
        return entity;
    }

}
