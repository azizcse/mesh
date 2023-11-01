package com.w3engineers.mesh.ui.importwallet;

import android.Manifest;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.w3engineers.ext.strom.application.ui.base.BaseActivity;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.manager.BoundServiceManager;
import com.w3engineers.mesh.ui.profilechoice.ProfileChoiceActivity;
import com.w3engineers.mesh.ui.security.SecurityActivity;
import com.w3engineers.mesh.util.CustomDialogUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.mesh.util.WalletAddressHelper;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityImportWalletBinding;
import com.w3engineers.walleter.wallet.WalletCreateManager;

import java.util.List;

/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

public class ImportWalletActivity extends BaseActivity {

    private ActivityImportWalletBinding mBinding;
    private ImportWalletViewModel mViewModel;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_import_wallet;
    }

    @Override
    protected void startUI() {
        mBinding = (ActivityImportWalletBinding) getViewDataBinding();
        mViewModel = getViewModel();
        initView();
    }

    @Override
    protected int statusBarColor() {
        return R.color.colorPrimaryDark;
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        switch (view.getId()) {
            case R.id.image_view_back:
                finish();
                break;
            case R.id.button_continue:
//                continueAction();
                checkPermissionAndContinue();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        startWalletCreationActivity();
        super.onBackPressed();
    }

    private void startWalletCreationActivity() {
        Intent intent = new Intent(this, ProfileChoiceActivity.class);
        startActivity(intent);
    }

    private void checkPermissionAndContinue(){
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
                            continueAction();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> checkPermissionAndContinue()).onSameThread().check();
    }

    private void continueAction() {
        if (mBinding.editTextPassword.getText() != null &&
                mBinding.editTextPassword.getText().length() >= 8) {
            CustomDialogUtil.showProgressDialog(ImportWalletActivity.this);
            HandlerUtil.postBackground(() -> gotoProfileCreatePage(), 100);
        } else {
            Toaster.showShort(getResources().getString(R.string.enter_eight_digit_password));
        }
    }

    private void initView() {
        setClickListener(mBinding.buttonContinue);

        if (!isWalletImported() && !TextUtils.isEmpty(WalletAddressHelper.CURRENT_ADDRESS)
                && !TextUtils.isEmpty(WalletAddressHelper.DEFAULT_ADDRESS)
                && WalletAddressHelper.CURRENT_ADDRESS.equals(WalletAddressHelper.DEFAULT_ADDRESS.trim())) {
            mBinding.textViewPinInstruction.setText("Your default password is:  " + Utils.ServiceConstant.DEFAULT_PASSWORD);
        }
    }

    private void gotoProfileCreatePage() {

        String password = mBinding.editTextPassword.getText().toString();

        if (!isWalletImported()) {

            WalletCreateManager.getInstance().loadWallet(this, password, Utils.INTENT_APP_TOKEN, new WalletCreateManager.WalletListener() {
                @Override
                public void onSuccess(String walletAddress, String publicKey, String appToken) {
                    successWalletResponse(walletAddress, publicKey, password, appToken);
                }

                @Override
                public void onError(String message, String appToken) {
                    failedWalletResponse(message);
                }
            });
        } else {

            //FIXME need to check why file copy create some problem
            WalletCreateManager.getInstance().importWallet(this, password, getWalletPath(), Utils.INTENT_APP_TOKEN, new WalletCreateManager.WalletListener() {
                @Override
                public void onSuccess(String walletAddress, String publicKey, String appToken) {
                    successWalletResponse(walletAddress, publicKey, password, appToken);
                }

                @Override
                public void onError(String message, String appToken) {
                    failedWalletResponse(message);
                }
            });
        }

    }

    public void failedWalletResponse(String message) {
        runOnUiThread(() -> {
            CustomDialogUtil.dismissProgressDialog();
            Toaster.showShort(message);
        });
    }

    public void successWalletResponse(String address, String publickKey, String password, String appToken) {
        if (mViewModel.storeData(address, password, publickKey)) {

            runOnUiThread(() -> {
                CustomDialogUtil.dismissProgressDialog();
                BoundServiceManager.on(getApplicationContext()).startMeshService(appToken);
                finish();
            });
        }
    }

    private boolean isWalletImported() {
        Intent intent = getIntent();
        return intent.getBooleanExtra(ImportWalletActivity.class.getSimpleName(), false);
    }

    private String getWalletPath(){
        return getIntent().getStringExtra("wallet_path");
    }

    private ImportWalletViewModel getViewModel() {
        return ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ImportWalletViewModel(getApplication());
            }
        }).get(ImportWalletViewModel.class);
    }
}
