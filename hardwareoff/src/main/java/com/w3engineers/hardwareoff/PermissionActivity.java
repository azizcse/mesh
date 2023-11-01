package com.w3engineers.hardwareoff;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionActivity extends AppCompatActivity {
    private AlertDialog mAlertDialog;
    public static final int REQUEST_ID_WRITE_SETTINGS = 909;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askPermission();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ID_WRITE_SETTINGS) {
            askPermission();
        }
    }

    private void askPermission() {
        if (checkAndRequestHotspotPermission()) {
            WifiStateTracker.getInstance().offWifiHotspot();

            finish();
        }
    }


    /**
     * Check and Request System write permission
     *
     * @return permission status
     */
    private boolean checkAndRequestHotspotPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Required Permissions");
                    builder.setMessage("This app require system setting to use awesome feature.Please allow modification.");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Take Me To SETTINGS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            mAlertDialog.cancel();
                            openSettings();
                        }
                    });
                    mAlertDialog = builder.create();
                    if (!mAlertDialog.isShowing()) {
                        builder.show();
                    }
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        String packageName = getPackageName();
        intent.setData(Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, REQUEST_ID_WRITE_SETTINGS);
    }
}
