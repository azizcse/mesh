package com.w3engineers.mesh.apkupdate;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.util.Log;

import com.w3engineers.mesh.App;
import com.w3engineers.mesh.manager.AppUpdateAppDataManager;
import com.w3engineers.mesh.ui.TeleMeshServiceMainActivity;


public class ApkUpdateScheduler {

    @SuppressLint("StaticFieldLeak")
    private static ApkUpdateScheduler apkUpdateScheduler = new ApkUpdateScheduler();
    private Context context;
    private NoInternetCallback noInternetCallback;

    protected int DEFAULT = 0, WIFI = 1, DATA = 2, AP = 3;

    private ApkUpdateScheduler() {
        context = App.getContext();
    }

    @NonNull
    public static ApkUpdateScheduler getInstance() {
        return apkUpdateScheduler;
    }

    public ApkUpdateScheduler connectivityRegister() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(new NetworkCheckReceiver(), intentFilter);
        return this;
    }

    public void initNoInternetCallback(NoInternetCallback callback) {
        this.noInternetCallback = callback;
    }

    public class NetworkCheckReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                boolean noConnectivity = intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                if (!noConnectivity) {

                    int state = getNetworkState();
                    Log.d("TeleMeshMainActivity", "Connectivity state: " + state);
                    if (state == DATA || state == WIFI || state == DEFAULT) {
                        Log.d("TeleMeshMainActivity", "net available ");
                        Constants.IS_DATA_ON = true;
                        if (!Constants.IS_APK_DOWNLOADING_START) {
                            // Constants.IS_APK_DOWNLOADING_START = true;
                            //  UpdateHelper.getInstance().downloadApkFile();

                            if (TeleMeshServiceMainActivity.getInstance() != null) {
                                TeleMeshServiceMainActivity.getInstance().checkUpdate();
                            }
                        }
                    } else {
                        Constants.IS_DATA_ON = false;
                    }
                } else {
                    Log.d("TeleMeshMainActivity", "No connectivity ");
                    // No action needed
                    Constants.IS_DATA_ON = false;
                }

                //  sendNoInternetCallbackToUi(Constants.IS_DATA_ON);
            }
        }
    }


    private void sendNoInternetCallbackToUi(boolean haveInternet) {
        if (noInternetCallback != null) {
            noInternetCallback.onGetAvailableInternet(haveInternet);
        }
    }

    protected int getNetworkState() {
        ConnectivityManager connectivitymanager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = connectivitymanager.getAllNetworkInfo();

        for (NetworkInfo netInfo : networkInfo) {

            /*if (netInfo.getTypeName().equalsIgnoreCase("WIFI"))
                if (netInfo.isConnected())
                    return WIFI;*/
            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                if (netInfo.isConnected())
                    return DATA;
        }
        return 0;
    }
}
