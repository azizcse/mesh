package com.w3engineers.mesh.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;

import com.w3engineers.mesh.datasharing.database.DatabaseService;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;

import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class TeleMeshMainActivityModel extends ViewModel {
    private Context mContext;
    private CompositeDisposable compositeDisposable;

    MutableLiveData<Integer> totalMessageSent = new MutableLiveData<>();
    MutableLiveData<Integer> totalMessageRcv = new MutableLiveData<>();

    public void setConText(Context context) {
        mContext = context;
        compositeDisposable = new CompositeDisposable();
    }


    public LiveData<Integer> getCount() {
        String userId = SharedPref.read(Constant.KEY_USER_ID);
        return DatabaseService.getInstance(mContext).getPeersCount(userId);
    }


    public void setMessageSentObserver(){
        compositeDisposable.add(Objects.requireNonNull(SharedPref.getTotalMessageSent())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    totalMessageSent.postValue(integer);
                }, Throwable::printStackTrace));
    }

    public void setMessageRcvObserver(){
        compositeDisposable.add(Objects.requireNonNull(SharedPref.getTotalMessageRcv())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    totalMessageRcv.postValue(integer);
                }, Throwable::printStackTrace));
    }

}
