package com.w3engineers.mesh.ui.security;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

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
import com.w3engineers.mesh.util.CustomDialogUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.mesh.util.WalletAddressHelper;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivitySecurityBinding;
import com.w3engineers.walleter.wallet.WalletCreateManager;

import java.util.List;

/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

public class SecurityActivity extends BaseActivity {

    private ActivitySecurityBinding mBinding;
    private SecurityViewModel mViewModel;
    private boolean isDefaultPassword;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_security;
    }

    @Override
    protected int statusBarColor() {
        return R.color.colorPrimaryDark;
    }

    @Override
    protected void startUI() {
        mBinding = (ActivitySecurityBinding) getViewDataBinding();
        mViewModel = getViewModel();

        initView();
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        switch (view.getId()) {
            case R.id.button_next:
                Utils.hideKeyboardFrom(this, mBinding.editTextBoxPassword);
                isValidPassword(mBinding.editTextBoxPassword.getText().toString(), false);
                break;
            case R.id.button_skip:
                Utils.hideKeyboardFrom(this, mBinding.editTextBoxPassword);
                isValidPassword(null, true);
                break;
            case R.id.text_view_show_password:
                updatePasswordVisibility();
                break;
        }
    }

    private void initView() {
        setClickListener(mBinding.buttonNext, mBinding.buttonSkip, mBinding.textViewShowPassword);
        mViewModel.textChangeLiveData.observe(this, this::nextButtonControl);
        mViewModel.textEditControl(mBinding.editTextBoxPassword);

        mBinding.editTextBoxPassword.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        mBinding.editTextBoxPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                Utils.hideKeyboardFrom(this, mBinding.editTextBoxPassword);
                isValidPassword(mBinding.editTextBoxPassword.getText().toString(), false);
                return true;
            }
            return false;
        });
    }

    private void nextButtonControl(String nameText) {
        if (!TextUtils.isEmpty(nameText) && nameText.length() >= Utils.ServiceConstant.MINIMUM_PASSWORD_LIMIT) {

            mBinding.buttonNext.setBackgroundResource(R.drawable.ractangular_gradient_blue);
            mBinding.buttonNext.setTextColor(getResources().getColor(R.color.white));
            mBinding.buttonNext.setClickable(true);
        } else {
            mBinding.buttonNext.setBackgroundResource(R.drawable.ractangular_white);
            mBinding.buttonNext.setTextColor(getResources().getColor(R.color.new_user_button_color));
            mBinding.buttonNext.setClickable(false);
        }
    }

    protected void requestMultiplePermissions(boolean isSkip) {

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

                            CustomDialogUtil.showProgressDialog(SecurityActivity.this);

                            HandlerUtil.postBackground(() -> goNext(isSkip), 100);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(
                            List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> requestMultiplePermissions(isSkip)).onSameThread().check();
    }



    protected void goNext(boolean isSkip) {

        String password = mBinding.editTextBoxPassword.getText() + "";

        if (TextUtils.isEmpty(password) || isSkip) {
            password = Utils.ServiceConstant.DEFAULT_PASSWORD;
            isDefaultPassword = true;
        }

        String finalPassword = password;
        Log.i("WalletService", " start wallet creation");
        WalletCreateManager.getInstance().createWallet(this, password, Utils.INTENT_APP_TOKEN, new WalletCreateManager.WalletListener() {
            @Override
            public void onSuccess(String walletAddress, String publicKey, String appToken) {
                processCompleted(walletAddress, publicKey, finalPassword, appToken);
            }

            @Override
            public void onError(String message, String appToken) {
                runOnUiThread(() -> {
                    CustomDialogUtil.dismissProgressDialog();
                    Toaster.showShort(message);
                });
            }
        });
    }

    public void processCompleted(String address, String publickKey, String finalPassword, String appToken) {

        CustomDialogUtil.dismissProgressDialog();

        if (mViewModel.storeData(address, publickKey, finalPassword)) {

            runOnUiThread(() -> {
                if (isDefaultPassword) {
                    WalletAddressHelper.writeDefaultAddress(address, SecurityActivity.this);
                }

                CustomDialogUtil.dismissProgressDialog();
                BoundServiceManager.on(getApplicationContext()).startMeshService(appToken);
                finish();
            });
        }
    }

    public void isValidPassword(final String password, boolean isSkip) {

        if (mViewModel.isValidPassword(password) || isSkip) {
            requestMultiplePermissions(isSkip);
        } else {
            if (!mViewModel.isValidChar(password)) {
                Toaster.showShort("Letter missing in password");
                return;
            }

            if (!mViewModel.isDigitPassword(password)) {
                Toaster.showShort("Digit missing in password");
                return;
            }

            if (!mViewModel.isValidSpecial(password)) {
                Toaster.showShort("Special character missing");
                return;
            }
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

    private SecurityViewModel getViewModel() {
        return ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new SecurityViewModel(getApplication());
            }
        }).get(SecurityViewModel.class);
    }

    private void updatePasswordVisibility() {
        String currentText = mBinding.textViewShowPassword.getText().toString();
        if (currentText.equals(getResources().getString(R.string.show_password))) {
            mBinding.textViewShowPassword.setText(getResources().getString(R.string.hide_password));
            mBinding.editTextBoxPassword.setPasswordShow(true);
        } else {
            mBinding.textViewShowPassword.setText(getResources().getString(R.string.show_password));
            mBinding.editTextBoxPassword.setPasswordShow(false);
        }
    }
}
