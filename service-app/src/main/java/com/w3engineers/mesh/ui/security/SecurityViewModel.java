package com.w3engineers.mesh.ui.security;

import android.app.Application;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jakewharton.rxbinding2.widget.RxTextView;
import com.w3engineers.ext.strom.application.ui.base.BaseRxAndroidViewModel;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.ImageUtil;
import com.w3engineers.mesh.util.Utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SecurityViewModel extends BaseRxAndroidViewModel {

    @NonNull
    public MutableLiveData<String> textChangeLiveData = new MutableLiveData<>();


    public SecurityViewModel(@NonNull Application application) {
        super(application);
    }

    boolean storeData(String address, String pubKey, String password) {
        SharedPref.write(Utils.KEY_NODE_ADDRESS, address);
        SharedPref.write(Utils.KEY_WALLET_PASS, password);
        SharedPref.write(Utils.KEY_WALLET_PUB_KEY, pubKey);
        ImageUtil.generateQRCodeForWalletAddress(address);

        return true;
    }

    void textEditControl(@NonNull EditText editText) {
        getCompositeDisposable().add(RxTextView.afterTextChangeEvents(editText)
                .map(input -> input.editable() + "")
                .debounce(100, TimeUnit.MILLISECONDS, Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe(text -> textChangeLiveData.postValue(text), Throwable::printStackTrace));
    }

    boolean isValidPassword(final String password) {

        if (password != null) {
            Pattern pattern;
            Matcher matcher;

            final String PASSWORD_PATTERN = "^(?=.*?[A-Za-z])(?=.*?[0-9])(?=.*?[@$%&#_()=+?»«<>£§€{}\\\\[\\\\]-]).{8,}$";

            pattern = Pattern.compile(PASSWORD_PATTERN);
            matcher = pattern.matcher(password);

            return matcher.matches();
        } else {
            return false;
        }
    }

    boolean isValidChar(final String password) {

        Pattern pattern;
        Matcher matcher;

        final String PASSWORD_PATTERN = "^(?=.*?[A-Za-z]).{8,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();
    }

    boolean isDigitPassword(final String password) {

        Pattern pattern;
        Matcher matcher;

        final String PASSWORD_PATTERN = "^(?=.*?[0-9]).{8,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();
    }

    boolean isValidSpecial(final String password) {

        Pattern pattern;
        Matcher matcher;

        final String PASSWORD_PATTERN = "^(?=.*?[@$%&#_()=+?»«<>£§€{}\\\\[\\\\]-]).{8,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();
    }
}
