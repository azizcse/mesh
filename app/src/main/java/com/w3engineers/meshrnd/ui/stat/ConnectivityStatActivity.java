package com.w3engineers.meshrnd.ui.stat;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityConnectivityStatBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public class ConnectivityStatActivity extends AppCompatActivity {
    private CompositeDisposable disposables;
    private ActivityConnectivityStatBinding mBinding;
    private TableViewAdapter tableViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_connectivity_stat);
        initRecyclerview();

        disposables = new CompositeDisposable();
        getDownloadedSongsData();
    }

    private void initRecyclerview() {
        tableViewAdapter = new TableViewAdapter(this);
        mBinding.recyclerViewTable.setHasFixedSize(true);
        mBinding.recyclerViewTable.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewTable.setAdapter(tableViewAdapter);
    }

    /**
     * Get data from routing table
     */
    private void getDownloadedSongsData() {
        disposables.add(RouteManager.getInstance().getAllLinkPath()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(connectionList -> {
                    if (connectionList != null) {
                        Collections.sort(connectionList);
                        if (tableViewAdapter != null) {
                            tableViewAdapter.addItems(connectionList);
                        }
                    }
                }, e -> Toast.makeText(this, getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show()));

    }

    @Override
    public void onStop() {
        super.onStop();
        // Using clear will clear all, but can accept new disposable
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Using dispose will clear all and set isDisposed = true, so it will not accept any new disposable
        disposables.dispose();
    }

}
