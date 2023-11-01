package com.w3engineers.mesh.apkupdate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.DialogServiceAppInstallProgressBinding;

import java.io.File;

public class ServiceUpdateDialog {
    private static ServiceUpdateDialog sInstance;

    private DialogServiceAppInstallProgressBinding binding;
    private AlertDialog appUpdateProgressDialog;

    public static ServiceUpdateDialog getInstance() {
        if (sInstance == null) {
            sInstance = new ServiceUpdateDialog();
        }
        return sInstance;
    }

    private ServiceUpdateDialog() {
    }


    public void showAppUpdateProgressDialog(Context context) {
        HandlerUtil.postForeground(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            binding = DataBindingUtil.inflate(inflater, R.layout.dialog_service_app_install_progress, null, false);
            builder.setView(binding.getRoot());
            appUpdateProgressDialog = builder.create();
            appUpdateProgressDialog.setCancelable(false);
            appUpdateProgressDialog.show();
        });
    }

    public void updateProgressDialog(int progress) {
        HandlerUtil.postForeground(() -> {
            if (appUpdateProgressDialog != null && appUpdateProgressDialog.isShowing()) {
                binding.progressBar.setProgress(progress);
            }
        });
    }

    public void closeDialog(String message) {
        HandlerUtil.postForeground(() -> {
            Toaster.showShort(message);
            if (appUpdateProgressDialog != null && appUpdateProgressDialog.isShowing()) {
                appUpdateProgressDialog.dismiss();
            }
        });
    }

    /**
     * This method is responsible for showing install dialog
     */
    public void showAppInstaller(Context context, String appPath) {
        File destinationFile = new File(appPath);
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String packageName = context.getPackageName() + ".provider";
            Uri apkUri = FileProvider.getUriForFile(context, packageName, destinationFile);
            intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            Log.d("InAppUpdateTest", "app uri: " + apkUri.getPath());
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d("InAppUpdateTest", "app install process start");
        } else {
            Uri apkUri = Uri.fromFile(destinationFile);
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }


}
