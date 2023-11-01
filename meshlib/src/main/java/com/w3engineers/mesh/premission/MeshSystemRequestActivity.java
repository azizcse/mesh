package com.w3engineers.mesh.premission;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.R;
import com.w3engineers.mesh.TransportManager;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.MeshLog;

import java.util.Arrays;
import java.util.List;

import static com.w3engineers.mesh.premission.BTDiscoveryTimeRequester.REQUEST_ENABLE_DSC;

/**
 * This activity ensure all provided permission from user.
 * Additionally if not configured only to
 * check system permission it ensures Location provider and BT discoverability activeness
 */
public class MeshSystemRequestActivity extends AppCompatActivity {

    public static final String PERMISSION_REQUEST = "PERMISSION_REQUEST";
    public static final String PERMISSION_REQUEST_ONLY_SYSTEM_PERMISSION = "PERMISSION_REQUEST_" +
            "ONLY_SYSTEM_PERMISSION";
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    public static final int REQUEST_SYSTEM_PERMISSIONS = 106;
    public static final int REQUEST_OVERLAY_PERMISSSION = 202;
    public static final String REQUEST_CODE = "request_code";
    private AlertDialog mAlertDialog;
    private PermissionHelper mPermissionHelper = new PermissionHelper();
    public static final List<String> MESH_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
            Arrays.asList(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION)
            : Arrays.asList(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION);

    private List<String> mRequestedPermissions;
    private boolean mIsSystemPermissionOnly;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor();
        int requestFrom = getIntent().getIntExtra(Constant.DIS_REQUEST, 0);
        MeshLog.e("PermissionActivity:: " + requestFrom);

        Intent intent = getIntent();
        if (intent != null) {
            mRequestedPermissions = intent.getStringArrayListExtra(PERMISSION_REQUEST);
            mIsSystemPermissionOnly = intent.getBooleanExtra(
                    PERMISSION_REQUEST_ONLY_SYSTEM_PERMISSION, false);

            mRequestedPermissions = mPermissionHelper.getNotGrantedPermissions(getApplicationContext(),
                    mRequestedPermissions);

            MeshLog.v("mRequestedPermissions " + mRequestedPermissions);

            requestMissingPermissionsWhileRequired();

        }

    }

    //Set Bluetooth Discoverability Unbounded
    private void requestDiscoverableTimePeriod() {
        MeshLog.v("requestDiscoverableTimePeriod ");
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            MeshLog.v("requestDiscoverableTimePeriod 1");
            Intent intent = new Intent(getApplicationContext(), BTDiscoveryTimeRequester.class);
            intent.putExtra(REQUEST_CODE, REQUEST_ENABLE_DSC);
            startActivityForResult(intent, REQUEST_ENABLE_DSC);
        } else if (!mIsSystemPermissionOnly) {
            MeshLog.v("requestDiscoverableTimePeriod 2");
            closeCurrentActivity(false);
        }

    }


    public boolean checkAndRequestLocationPermission() {
        if (!mPermissionHelper.isLocationProviderEnabled(getApplicationContext())) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                    builder.setTitle(R.string.permission_setting_title);
                    builder.setMessage(R.string.title_msg_system_setting_modification);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.text_positive_button, (dialog, which) -> {
                        dialog.cancel();
                        mAlertDialog.cancel();
                        openSettings();
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

    /**
     * Open system write setting
     */
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        String packageName = getPackageName();
        intent.setData(Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, REQUEST_SYSTEM_PERMISSIONS);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            DialogUtil.dismissDialog();
            requestMissingPermissionsWhileRequired();
        } else if (requestCode == REQUEST_SYSTEM_PERMISSIONS /*&& checkAndRequestHotspotPermission() && checkAndRequestLocationPermission()*/) {
            requestMissingPermissionsWhileRequired();
        } else if (requestCode == REQUEST_ENABLE_DSC) {
            if (resultCode == 3600) {
                closeCurrentActivity(true);
            } else {
                closeCurrentActivity(false);
            }
        }
    }

    private void closeCurrentActivity(boolean btEnabled) {
        MeshLog.v("closeCurrentActivity ");
        TransportManager transportManager = TransportManagerX.getInstance();

        if (btEnabled && transportManager != null) {
            transportManager.isBtEnabled = true;
        }
        if (transportManager != null) {
            transportManager.initMeshProcess();
        }

        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                // Check for both permissions
                requestMissingPermissionsWhileRequired();
            }
            break;

            default:
        }
    }

    private void requestMissingPermissionsWhileRequired() {

        MeshLog.v("requestMissingPermissionsWhileRequired ");
        //Ask system whether we should show permission dialog
        List<String> filteredPermissions = mPermissionHelper.getNotGrantedPermissions(
                this, mRequestedPermissions);
        if (CollectionUtil.hasItem(filteredPermissions)) {
            MeshLog.v("requestMissingPermissionsWhileRequired 1");

            //https://stackoverflow.com/a/4042464
            //Using size ZERO is better
            ActivityCompat.requestPermissions(this,
                    filteredPermissions.toArray(new String[0]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);

        } else if (!mPermissionHelper.hasPermissions(this, mRequestedPermissions)) {
            MeshLog.v("requestMissingPermissionsWhileRequired 2");
            Log.d("PermissionTest", " openSettingsDialog dialog open");
            openSettingsDialog();

        } else if (mIsSystemPermissionOnly) {
            MeshLog.v("requestMissingPermissionsWhileRequired 3");

            setResult(Activity.RESULT_OK);
            finish();

        } else {
            MeshLog.v("requestMissingPermissionsWhileRequired 4");

            if (checkAndRequestLocationPermission()) {

                //As we do not need BT for lazy network so turning BT discoverability initial
                // permission off
                requestDiscoverableTimePeriod();
                //closeCurrentActivity(false);
            }
        }
    }

    /**
     * Permission required window
     *
     * @param message    details message
     * @param okListener click listen handler
     */

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.title_ok, okListener)
                .setCancelable(false)
                .create()
                .show();
    }

    /**
     * System setting open for permanent denial
     */

    private void openSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_setting_title);
        builder.setMessage(R.string.permission_setting_des);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.text_positive_button, (dialog, which) -> {
            dialog.cancel();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, REQUEST_SYSTEM_PERMISSIONS);
        });
        builder.show();
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_location_permission)
                .setMessage(R.string.description_location_permission)
                .setCancelable(false)
                .setPositiveButton(R.string.text_okay_button, (dialog, id) ->
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                REQUEST_SYSTEM_PERMISSIONS));
        final AlertDialog alert = builder.create();
        alert.show();
    }


    private void openOverlayPermissionActivity() {

        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSSION);

    }

    private boolean isOverlayAllow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return true;
        }
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Window overlay");
            builder.setMessage("This permission is required to use the awesome feature");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                mAlertDialog.cancel();
                openOverlayPermissionActivity();
            });
            mAlertDialog = builder.create();
            if (!mAlertDialog.isShowing()) {
                builder.show();
            }

            return false;
        }

        return true;
    }

    private void setStatusBarColor() {
        int statusBarColor = R.color.dark_blue;
        if (statusBarColor > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(ContextCompat.getColor(this, statusBarColor));
            }
        }
    }

    public boolean isPermissionNeeded() {
        String manufacturer = android.os.Build.MANUFACTURER;
        try {

            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                return !SharedPref.readBoolean(Constant.PreferenceKeys.IS_SETTINGS_PERMISSION_DONE);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showPermissionPopupForXiaomi() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(Html.fromHtml("<b>" + "<font color='#FF7F27'>Please allow permissions</font>" + "</b>"));
        builder.setMessage(getString(R.string.permission_xiomi));
        builder.setPositiveButton(Html.fromHtml("<b>" + getString(R.string.ok) + "<b>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                SharedPref.write(Constant.PreferenceKeys.IS_SETTINGS_PERMISSION_DONE, true);
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 100);
                DialogUtil.dismissDialog();
            }
        });
        builder.create();
        builder.show();
    }
}
