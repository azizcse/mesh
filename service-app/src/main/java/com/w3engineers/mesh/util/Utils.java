package com.w3engineers.mesh.util;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.App;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.manager.BoundServiceManager;
import com.w3engineers.mesh.ui.security.SecurityActivity;
import com.w3engineers.meshrnd.TeleMeshService;
import com.w3engineers.walleter.wallet.WalletCreateManager;
import com.w3engineers.walleter.wallet.WalletService;

import java.util.List;

public class Utils {

    public static final String KEY_NODE_ADDRESS = "tel_node_address";
    public static final String KEY_WALLET_PASS = "tel_wallet_pass";
    public static final String KEY_WALLET_PUB_KEY = "tel_pub_key";
    public static final String KEY_WALLET_QRCODE = "tel_wallet_qr";


    public static final String KEY_CLIENT_INFO_VERSION = "tel_client_info_version";

    public static String INTENT_APP_TOKEN;

    @Nullable
    private static Utils utils;

    private Utils() {
    }

    @NonNull
    public static Utils getInstance() {
        if (utils == null) {
            utils = new Utils();
        }
        return utils;
    }


    public interface ServiceConstant {
        String DEFAULT_ADDRESS = ".defaultAddress";
        String DEFAULT_ADDRESS_FILE = "defaultAddressFile.txt";
        int MINIMUM_PASSWORD_LIMIT = 8;
        String DEFAULT_PASSWORD = "mesh_123";
        String FILE_TYPE = "application/*";
    }

    public interface BroadcastReceiveStatus {
        int PROGRESS = 1;
        int RECEIVED = 2;
        int DELIVERED = 3;
        int FAILED = 4;
    }

    public Intent getAppPackage(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(Constant.TELEMESH_PACKAGE);
        if (intent == null) {
            intent = context.getPackageManager().getLaunchIntentForPackage(Constant.VIPER_PACKAGE);
        }
        return intent;
    }

    public Intent getAppByPackage(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(Constant.TELEMESH_PACKAGE);
    }

    public static void hideKeyboardFrom(@NonNull Context context, @NonNull View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void backOperation(Activity activity) {
        Dexter.withActivity(activity)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        if (report.areAllPermissionsGranted()) {

                            String defaultPass = Utils.ServiceConstant.DEFAULT_PASSWORD;
                            SharedPref.write(Utils.KEY_WALLET_PASS, defaultPass);
                            WalletService.getInstance(activity).createOrLoadWallet(defaultPass, Utils.INTENT_APP_TOKEN, new WalletService.WalletLoadListener() {
                                @Override
                                public void onWalletLoaded(String walletAddress, String publicKey, String appToken) {
                                    SharedPref.write(Utils.KEY_NODE_ADDRESS, walletAddress);
                                    SharedPref.write(Utils.KEY_WALLET_PUB_KEY, publicKey);

                                    BoundServiceManager.on(activity).startMeshService(appToken);
                                }

                                @Override
                                public void onErrorOccurred(String message, String appToken) {

                                }

                                @Override
                                public void onErrorOccurred(int code, String appToken) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> backOperation(activity)).onSameThread().check();
    }

}
