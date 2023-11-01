package com.w3engineers.mesh.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.LocationManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.guardsquare.dexguard.runtime.detection.CertificateChecker;
import com.guardsquare.dexguard.runtime.detection.FileChecker;
import com.guardsquare.dexguard.runtime.detection.HookDetector;
import com.guardsquare.dexguard.runtime.detection.RootDetector;
import com.guardsquare.dexguard.runtime.detection.TamperDetector;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.App;
import com.w3engineers.mesh.apkupdate.ApkUpdateScheduler;
import com.w3engineers.mesh.apkupdate.Constants;
import com.w3engineers.mesh.apkupdate.ServiceUpdate;
import com.w3engineers.mesh.apkupdate.TSAppInstaller;
import com.w3engineers.mesh.apkupdate.UpdateHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.manager.BoundServiceManager;
import com.w3engineers.mesh.ui.profilechoice.ProfileChoiceActivity;
import com.w3engineers.mesh.util.CommonUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.CredentialUtils;
import com.w3engineers.mesh.util.DexterPermissionHelper;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.LocationTracker;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.StorageUtil;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.meshrnd.BuildConfig;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityTelemeshServiceBinding;
import com.w3engineers.purchase.util.ClientInfoSyncUtil;

public class TeleMeshServiceMainActivity extends AppCompatActivity implements View.OnClickListener, DexterPermissionHelper.PermissionCallback {
    private static final int RC_APP_UPDATE = 106;
    private final String TAG = TeleMeshServiceMainActivity.class.getSimpleName();
    private ActivityTelemeshServiceBinding mBinding;
    private TeleMeshMainActivityModel mViewModel;
    private static TeleMeshServiceMainActivity sInstance;
    private ApkUpdateScheduler sheduler;
    AppUpdateManager mAppUpdateManager;
    Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("TeleMeshMainActivity", "onCreate called");
        mContext = this;
        sInstance = this;
        /*Delegate delegateObj = new Delegate();
        delegateObj.checkAppTemper();
        delegateObj.detectHookAndRoot();*/

        ServiceUpdate.getInstance(this).setAppUpdateProcess(false);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_telemesh_service);

        setTitle();

        mBinding.buttonLaunchApp.setOnClickListener(this);
        mBinding.buttonStopService.setOnClickListener(this);
        mBinding.appUpdateView.setOnClickListener(this);
//        mBinding.walletButton.setOnClickListener(this);
//        mBinding.dataplanButton.setOnClickListener(this);

        mViewModel = ViewModelProviders.of(this).get(TeleMeshMainActivityModel.class);
        mViewModel.setConText(this);


        mBinding.textViewTeleMeshLink.setMovementMethod(LinkMovementMethod.getInstance());
        stripUnderlines(mBinding.textViewTeleMeshLink);

        checkPermission();

        //Use application context instead of activity context
        if (BoundServiceManager.on(App.getContext()).isPermissionNeeded()) {
            showPermissionPopupForXiaomi();
        }

        initObserver();

       /* if (!CommonUtil.isLocationGpsOn(this)) {
            CommonUtil.showGpsOrLocationOffPopup(TeleMeshServiceMainActivity.this);
        }*/
        sheduler = ApkUpdateScheduler.getInstance().connectivityRegister();

        //  registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        registerReceiver(mGpsSwitchStateReceiver, filter);

        showInAppUpdateButton();

        //startService(new Intent(this, TeleMeshService.class));
    }

    private void initObserver() {
        mBinding.tvMsgSent.setText(String.valueOf(SharedPref.on(this).readInt(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT, 0)));
        mBinding.tvMsgReceived.setText(String.valueOf(SharedPref.on(this).readInt(Constant.PreferenceKeys.TOTAL_MESSAGE_RCV, 0)));

        if (mViewModel != null) {
            mViewModel.getCount().observe(this, new Observer<Integer>() {
                @Override
                public void onChanged(@Nullable Integer integer) {
                    mBinding.tvNodeConnected.setText(String.valueOf(integer));
                }
            });

            mViewModel.setMessageSentObserver();
            mViewModel.totalMessageSent.observe(this, integer -> {
                mBinding.tvMsgSent.setText(String.valueOf(integer));
            });

            mViewModel.setMessageRcvObserver();
            mViewModel.totalMessageRcv.observe(this, integer -> {
                mBinding.tvMsgReceived.setText(String.valueOf(integer));
            });
        }
    }

    private void showInAppUpdateButton() {
        mBinding.appUpdateView.setVisibility(View.GONE);
        if (Constants.IS_DATA_ON) {
            long version = SharedPref.readLong(Constants.preferenceKey.UPDATE_APP_VERSION);
            if (version > BuildConfig.VERSION_CODE) {
                mBinding.appUpdateView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setTitle() {
        setSupportActionBar(mBinding.toolbar);
        mBinding.toolbar.setTitle(getResources().getString(R.string.tel_service));
    }

    private void checkPermission() {
/*        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TeleMeshServiceMainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 108);
        } else {
            Toast.makeText(mContext, "You need have granted permission", Toast.LENGTH_SHORT).show();
            //  locationTracker = new LocationTracker(mContext, MainActivity.this);
            LocationTracker.onInstance(mContext, TeleMeshServiceMainActivity.this);
        }*/


  /*      if (PermissionUtil.init(this).request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        }*/

        // For android 11 it cannot request background location permission and
        // normal location permission at the same time.
        // For android 11 not ACCESS_COARSE_LOCATION
        // So for android 11 it we are requesting Background location permission
        // After normal permission accepted

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DexterPermissionHelper.getInstance().requestForPermission(this, this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            DexterPermissionHelper.getInstance().requestForPermission(this, this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        } else {
            DexterPermissionHelper.getInstance().requestForPermission(this, this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    public void checkUpdate() {
        showInAppUpdateButton();
        mAppUpdateManager = AppUpdateManagerFactory.create(this);

        mAppUpdateManager.registerListener(installStateUpdatedListener);

        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                try {
                    mAppUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE, this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }


            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            } else {
                //  Log.e(TAG, "checkForAppUpdateAvailability: something else");
                checkPermission();
                UpdateHelper.getInstance().downloadApkFile();
            }
        });
    }

    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.layout_main),
                        "New app is ready!",
                        Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("Install", view -> {
            if (mAppUpdateManager != null) {
                mAppUpdateManager.completeUpdate();
            }
        });


        snackbar.setActionTextColor(getResources().getColor(R.color.accent));
        snackbar.show();
    }

    InstallStateUpdatedListener installStateUpdatedListener = new
            InstallStateUpdatedListener() {
                @Override
                public void onStateUpdate(InstallState state) {
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate();
                    } else if (state.installStatus() == InstallStatus.INSTALLED) {
                        if (mAppUpdateManager != null) {
                            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                        }

                    } else {
                        Log.i(TAG, "InstallStateUpdatedListener: state: " + state.installStatus());
                    }
                }
            };

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MeshLog.e("request code :: " + requestCode);
        if (requestCode == 100) {
            DialogUtil.dismissDialog();
        } else if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "onActivityResult: app download failed");
            }
        }
    }

    private void launchClientApp() {
        Intent intent = Utils.getInstance().getAppPackage(this);
        if (intent == null) {
            Toaster.init(R.color.telemeshColorPrimary);
            Toaster.showShort(getResources().getString(R.string.install_app));
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void showPermissionPopUp() {
        DialogUtil.showConfirmationDialog(this,
                getResources().getString(R.string.permission),
                getResources().getString(R.string.permission_message),
                null,
                getString(R.string.ok),
                new DialogUtil.DialogButtonListener() {
                    @Override
                    public void onClickPositive() {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onClickNegative() {

                    }
                });
    }

    public static TeleMeshServiceMainActivity getInstance() {
        return sInstance;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (BoundServiceManager.on(App.getContext()).isServiceRunning()) {
            mBinding.buttonStopService.setEnabled(true);
            mBinding.buttonStopService.setTextColor(getResources().getColor(R.color.white));
            Log.e("buttonEnable::", "enable true");
        } else {
            mBinding.buttonStopService.setEnabled(false);
            mBinding.buttonStopService.setTextColor(getResources().getColor(R.color.colorBackgroundDark));
            Log.e("buttonEnable::", "enable false");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("TeleMeshMainActivity", "onPause called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //boundServiceManager.disconnectFromService();

        sInstance = null;
        unregisterReceiver(mGpsSwitchStateReceiver);
        LocationTracker.getInstance(mContext).stopListener();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_launch_app:
                launchClientApp();
                //startActivity(new Intent(TeleMeshServiceMainActivity.this, ProfileChoiceActivity.class));
//                ClientInfoSyncUtil.getInstance().syncClientInformationFromServer(getApplicationContext());
                break;

            case R.id.button_stop_service:
                if (!BoundServiceManager.on(App.getContext()).getClientBindStatus()) {
                    if (BoundServiceManager.on(App.getContext()).isServiceRunning()) {
                        BoundServiceManager.on(App.getContext()).stopTmService(this);
                        mBinding.buttonStopService.setEnabled(false);
                        mBinding.buttonStopService.setTextColor(getResources().getColor(R.color.colorBackgroundDark));
                        Log.e("buttonEnable::", "enable false from click");
                    }
                } else {
                    HandlerUtil.postForeground(() -> Toast.makeText(App.getContext(),
                            "Please stop client app first", Toast.LENGTH_LONG).show());
                }

                break;

            case R.id.appUpdateView:
                if (StorageUtil.getFreeMemory() > Constants.MINIMUM_SPACE) {
                    downloadServiceApp();
                } else {
                    Toaster.showShort(getResources().getString(R.string.phone_storage_not_enough));
                }
                break;

            /*case R.id.wallet_button:
                WalletManager.openActivity(TeleMeshServiceMainActivity.this, null);
                break;
            case R.id.dataplan_button:
                DataPlanManager.openActivity(TeleMeshServiceMainActivity.this);*/
        }
    }

    private void stripUnderlines(TextView textView) {
        Spannable s = new SpannableString(textView.getText());
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
        textView.setText(s);
    }

    @Override
    public void onPermissionGranted() {
        //   locationTracker = new LocationTracker(mContext, MainActivity.this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION).withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                    startLocationOperation();

                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                    if (permissionDeniedResponse.isPermanentlyDenied()) {
                        CommonUtil.showPermissionPopUp(TeleMeshServiceMainActivity.this);
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).check();
        } else {
            startLocationOperation();
        }


    }

    private void startLocationOperation() {
        LocationTracker.getInstance(mContext).getLocation();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (LocationTracker.getInstance(mContext).canGetLocation()) {

                    double latitude = LocationTracker.getInstance(mContext).getLatitude();
                    double longitude = LocationTracker.getInstance(mContext).getLongitude();

                    SharedPref.write(Constant.PreferenceKeys.STORED_LATITUDE, latitude);
                    SharedPref.write(Constant.PreferenceKeys.STORED_LONGITUDE, longitude);

                    // \n is for new line
                    // Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                } else {
                    // Can't get location.
                    // GPS or network is not enabled.
                    // Ask user to enable GPS/network in settings.
                    //  LocationTracker.getInstance().showSettingsAlert();
                    if (!DialogUtil.isDialogShowing()) {
                        CommonUtil.showGpsOrLocationOffPopup(TeleMeshServiceMainActivity.this);
                    }
                }
            }
        });


        // Check if GPS enabled
        if (LocationTracker.getInstance(mContext).canGetLocation()) {

            double latitude = LocationTracker.getInstance(mContext).getLatitude();
            double longitude = LocationTracker.getInstance(mContext).getLongitude();

            SharedPref.write(Constant.PreferenceKeys.STORED_LATITUDE, latitude);
            SharedPref.write(Constant.PreferenceKeys.STORED_LONGITUDE, longitude);

            // \n is for new line
            //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        } else {
            // Can't get location.
            // GPS or network is not enabled.
            // Ask user to enable GPS/network in settings.
            //  LocationTracker.getInstance().showSettingsAlert();
            if (!DialogUtil.isDialogShowing()) {
                CommonUtil.showGpsOrLocationOffPopup(TeleMeshServiceMainActivity.this);
            }
        }
    }

    private class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    private void downloadServiceApp() {
        if (WiFiUtil.isWifiConnected(this)) {
            WiFiUtil.isInternetAvailable((message, isConnected) -> {
                startNetworkSelectionProcess(isConnected, this);

            });
        } else {
            startNetworkSelectionProcess(false, this);
        }
    }

    private void startNetworkSelectionProcess(boolean isWifiNetworkAvailable, Context context) {
        if (isWifiNetworkAvailable) {
            TSAppInstaller.downloadApkFile(this, CredentialUtils.getFileRepoLink(), null);
        } else {
            NetworkMonitor.setNetworkInterfaceListeners(new NetworkMonitor.NetworkInterfaceListener() {
                @Override
                public void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi) {
                    if (isOnline && network != null) {
                        TSAppInstaller.downloadApkFile(TeleMeshServiceMainActivity.this, CredentialUtils.getFileRepoLink(), network);
                    }
                }
            });


            /*CellularDataNetworkUtil.on(context, new CellularDataNetworkUtil.CellularDataNetworkListenerForPurchase() {
                @Override
                public void onAvailable(Network network) {
                    TSAppInstaller.downloadApkFile(TeleMeshServiceMainActivity.this, CredentialUtils.getFileRepoLink(), network);
                }

                @Override
                public void onLost() {

                }
            }).initMobileDataNetworkRequest();*/
        }
    }


    /**
     * Following broadcast receiver is to listen the Location button toggle state in Android.
     */
/*    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                if (CommonUtil.isLocationGpsOn(context)) {
                    CommonUtil.dismissDialog();
                } else {
                    CommonUtil.showGpsOrLocationOffPopup(TeleMeshServiceMainActivity.this);
                }
            }
        }
    };*/

    /**
     * Following broadcast receiver is to listen the Location button toggle state in Android.
     */
    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (isGpsEnabled || isNetworkEnabled) {
                    //location is enabled
                    Log.e("gps_staus", "gps has been on");

                    LocationTracker.getInstance(mContext).getLocation();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtil.dismissDialog();
                        }
                    });
                } else {
                    //location is disabled
                    Log.e("gps_staus", "gps has been off");
                    if (!DialogUtil.isDialogShowing()) {
                        CommonUtil.showGpsOrLocationOffPopup(TeleMeshServiceMainActivity.this);
                    }
                }
            }
        }
    };

    private class Delegate {
        final int OK = 1;

        private void checkAppTemper() {

            int appChanged = TamperDetector.checkApk(mContext, OK);
            int certificateChanged =
                    CertificateChecker.checkCertificate(mContext, OK);

            FileChecker fileChecker = new FileChecker(mContext);

            int primaryDexChanged =
                    fileChecker.checkFile("classes.dex", OK);

            int anyFileChanged =
                    fileChecker.checkAllFiles(OK);

            if (appChanged != OK ||
                    certificateChanged != OK ||
                    primaryDexChanged != OK) {

                HandlerUtil.postForeground(() -> {
                    Toaster.showShort("App is tampered. No support available");
                });

                finishAffinity();
                System.exit(0);
            }
        }

        private void detectHookAndRoot() {

            // Let the DexGuard runtime library detect whether the application is being
            // hooked.
            if (HookDetector.isApplicationHooked(mContext) != OK) {
                HandlerUtil.postForeground(() -> {
                    Toaster.showShort("Application is hooked by other App");
                });
            }

            //Root detection
            RootDetector.checkDeviceRooted(mContext, this::rootDetectionCallback);
        }

        private void rootDetectionCallback(int okValue, int returnValue) {
            if (okValue != returnValue) {
                HandlerUtil.postForeground(() -> {
                    Toaster.showShort("Rooted device detected");
                });
            }
        }
    }

}
