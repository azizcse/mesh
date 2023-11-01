package com.w3engineers.mesh.ui.profilechoice;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.w3engineers.ext.strom.application.ui.base.BaseActivity;
import com.w3engineers.mesh.App;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.manager.BoundServiceManager;
import com.w3engineers.mesh.ui.importwallet.ImportWalletActivity;
import com.w3engineers.mesh.ui.security.SecurityActivity;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.mesh.util.WalletAddressHelper;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityProfileChoiceBinding;
import com.w3engineers.walleter.wallet.WalletService;

import java.util.List;

/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

public class ProfileChoiceActivity extends BaseActivity {

    private ActivityProfileChoiceBinding mBinding;
    private ProfileChoiceViewModel mViewModel;
    private boolean isNewWalletCall;
    private final int XIAOMI_REQUEST_HANDLE = 100;
    private final int PICKFILE_REQUEST_CODE = 103;
    private String appToken;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_profile_choice;
    }

    @Override
    protected void startUI() {
        mBinding = (ActivityProfileChoiceBinding) getViewDataBinding();
        mViewModel = getViewModel();

        initView();
        if (BoundServiceManager.on(App.getContext()).isPermissionNeeded()) {
            showPermissionPopupForXiaomi();
        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        if (view.getId() == R.id.button_create_account) {
            isNewWalletCall = true;
            requestMultiplePermissions();
        } else if (view.getId() == R.id.button_import_account) {
            isNewWalletCall = false;
            requestMultiplePermissions();
        }
    }

    @Override
    public void onBackPressed() {
        Utils.getInstance().backOperation(this);
        super.onBackPressed();
    }

    private void initView() {
        setClickListener(mBinding.buttonCreateAccount, mBinding.buttonImportAccount);
    }

    private ProfileChoiceViewModel getViewModel() {
        return ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ProfileChoiceViewModel(getApplication());
            }
        }).get(ProfileChoiceViewModel.class);
    }

    private void showPermissionPopupForXiaomi() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(Html.fromHtml("<b>" + "<font color='#FF7F27'>Please allow permissions</font>" + "</b>"));
        builder.setMessage(getString(R.string.permission_xiomi));
        builder.setPositiveButton(Html.fromHtml("<b>" + getString(R.string.ok) + "<b>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                SharedPref.write(Constant.PreferenceKeys.IS_SETTINGS_PERMISSION_DONE, true);
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), XIAOMI_REQUEST_HANDLE);
                DialogUtil.dismissDialog();
            }
        });
        builder.create();
        builder.show();
    }

    protected void requestMultiplePermissions() {

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        if (report.areAllPermissionsGranted()) {
                            if (isNewWalletCall) {
                                createNewWallet();
                                // TODO open alert for create new or use existing one from sd card
                            } else {
                                openFileChooser();
                                // TODO start import profile
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> requestMultiplePermissions()).onSameThread().check();
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(Utils.ServiceConstant.FILE_TYPE);
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PICKFILE_REQUEST_CODE) {
            String path = getPath(data.getData());
            if (!TextUtils.isEmpty(path)) {
                Intent intent = new Intent(this, ImportWalletActivity.class);
                intent.putExtra(ImportWalletActivity.class.getSimpleName(), true);
                intent.putExtra("wallet_path", path);
                startActivity(intent);
                finish();
            }else {
                Toast.makeText(this, "Wallet path not found", Toast.LENGTH_LONG).show();
            }
           /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getContentResolver().takePersistableUriPermission(Utils.WALLET_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }*/
        } else if (requestCode == XIAOMI_REQUEST_HANDLE) {
            DialogUtil.dismissDialog();
        } else {
            Toast.makeText(this, "Wallet path not selected", Toast.LENGTH_LONG).show();
        }
    }


    public String getPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /*@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //reload my activity with permission granted or use the features that required the permission
            Intent intent = new Intent(this, ImportWalletActivity.class);
            intent.putExtra(ImportWalletActivity.class.getSimpleName(), true);
            startActivity(intent);
            finish();
        } else {
            Log.v("MIMO_SAHA:", "Import issue ");
        }
    }
*/
    private void createNewWallet() {
        if (WalletService.getInstance(this).isWalletExists()) {
            showWarningDialog();
        } else {
            startActivity(new Intent(ProfileChoiceActivity.this, SecurityActivity.class));
            finish();
        }
    }

    private void showWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(Html.fromHtml("<b>" + getString(R.string.alert_title_text) + "</b>"));
        builder.setMessage(WalletAddressHelper.getWalletSpannableString(this).toString());
        builder.setPositiveButton(Html.fromHtml("<b>" + getString(R.string.button_postivive) + "<b>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                startActivity(new Intent(ProfileChoiceActivity.this, ImportWalletActivity.class));
                finish();
            }
        });
        builder.setNegativeButton(Html.fromHtml("<b>" + getString(R.string.negative_button) + "<b>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                startActivity(new Intent(ProfileChoiceActivity.this, SecurityActivity.class));
                finish();
            }
        });
        builder.create();
        builder.show();
    }

}
