package com.w3engineers.purchase.ui.wallet;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.w3engineers.eth.data.helper.PreferencesHelperPaylib;
import com.w3engineers.ext.strom.application.ui.base.BaseRxViewModel;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.purchase.db.networkinfo.WalletInfo;
import com.w3engineers.purchase.wallet.WalletManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class WalletViewModel extends BaseRxViewModel {

    public MutableLiveData<WalletInfo> networkMutableLiveData = new MutableLiveData<>();
    private WalletManager walletManager;

    public WalletViewModel() {
        walletManager = WalletManager.getInstance();
    }

    public void getCurrencyAmount() {
        getCompositeDisposable().add(walletManager.getNetworkInfoByNetworkType()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(walletInfos -> {
                    for (WalletInfo walletInfo : walletInfos) {

                        if (walletInfo.networkType == PreferencesHelperPaylib.onInstance(MeshApp.getContext()).getEndpointMode()) {
                            networkMutableLiveData.postValue(walletInfo);
                        }
                    }
                }, Throwable::printStackTrace));
    }

    public LiveData<Double> getTotalEarn() {
        return walletManager.getTotalEarn(walletManager.getMyAddress(), walletManager.getMyEndpoint());
    }

    public LiveData<Double> getTotalSpent() {
        return walletManager.getTotalSpent(walletManager.getMyAddress(), walletManager.getMyEndpoint());
    }

    public LiveData<Double> getTotalPendingEarning() {
        return walletManager.getTotalPendingEarning(walletManager.getMyAddress(), walletManager.getMyEndpoint());
    }

    public LiveData<Integer> getDifferentNetworkData(String myAddress) {
        return walletManager.getDifferentNetworkData(walletManager.getMyAddress(), walletManager.getMyEndpoint());
    }
}
