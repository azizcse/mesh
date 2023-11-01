package com.w3engineers.purchase.ui.dataplan;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.w3engineers.ext.strom.application.ui.base.BaseRxAndroidViewModel;
import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.dataplan.DataPlanManager;
import com.w3engineers.purchase.model.Seller;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

class DataPlanViewModel extends BaseRxAndroidViewModel {

    public DataPlanViewModel(@NonNull Application application) {
        super(application);
    }

    void roleSwitch(int newRole, int previousRole) {
        DataPlanManager.getInstance().roleSwitch(newRole, previousRole);
    }

    MutableLiveData<List<Seller>> allSellers = new MutableLiveData<>();

    void getAllSellers() {

        getCompositeDisposable().add(DataPlanManager.getInstance().getAllSellers()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sellers -> {

                    List<Seller> onlineNotPurchased = new ArrayList<>();
                    List<Seller> onlinePurchased = new ArrayList<>();
                    List<Seller> offlinePurchased = new ArrayList<>();

                    List<Seller> finalList = new ArrayList<>();

                    for (Seller seller : sellers) {
                        if (seller.getLabel() == DataPlanConstants.SELLER_LABEL.ONLINE_NOT_PURCHASED) {
                            onlineNotPurchased.add(seller);
                        } else if (seller.getLabel() == DataPlanConstants.SELLER_LABEL.ONLINE_PURCHASED) {
                            onlinePurchased.add(seller);
                        } else if (seller.getLabel() == DataPlanConstants.SELLER_LABEL.OFFLINE_PURCHASED) {
                            offlinePurchased.add(seller);
                        }
                    }

                    if (onlinePurchased.size() > 0) {
//                        finalList.add(getLabelSeller(DataPlanConstants.SELLER_LABEL.ONLINE_PURCHASED));
                        finalList.addAll(onlinePurchased);
                    }

                    if (onlineNotPurchased.size() > 0) {
//                        finalList.add(getLabelSeller(DataPlanConstants.SELLER_LABEL.ONLINE_NOT_PURCHASED));
                        finalList.addAll(onlineNotPurchased);
                    }

                    if (offlinePurchased.size() > 0) {
//                        finalList.add(getLabelSeller(DataPlanConstants.SELLER_LABEL.OFFLINE_PURCHASED));
                        finalList.addAll(offlinePurchased);
                    }
                    allSellers.postValue(finalList);

                }, Throwable::printStackTrace));

        DataPlanManager.getInstance().processAllSeller(getApplication().getApplicationContext());
    }

    private Seller getLabelSeller(int tag) {
        return new Seller().setId("" + tag);
    }
}
