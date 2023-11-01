package com.w3engineers.meshrnd.ui.stat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.route.RouteManager;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityConnectivityStatBinding;
import com.w3engineers.meshrnd.databinding.FragmentNetworkBinding;
import com.w3engineers.meshrnd.ui.base.BaseFragment;

import java.util.Collections;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ConnectionStatFragment extends BaseFragment {
    private CompositeDisposable disposables;
    private ActivityConnectivityStatBinding mBinding;
    private TableViewAdapter tableViewAdapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.activity_connectivity_stat, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initRecyclerview();
        disposables = new CompositeDisposable();
        getDownloadedSongsData();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initRecyclerview() {
        tableViewAdapter = new TableViewAdapter(getActivity());
        mBinding.recyclerViewTable.setHasFixedSize(true);
        mBinding.recyclerViewTable.setLayoutManager(new LinearLayoutManager(getContext()));
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
                }, e -> Toast.makeText(getActivity(), getResources().getString(R.string.no_data_found), Toast.LENGTH_SHORT).show()));

    }

    @Override
    public void onStop() {
        super.onStop();
        // Using clear will clear all, but can accept new disposable
        disposables.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Using dispose will clear all and set isDisposed = true, so it will not accept any new disposable
        disposables.dispose();
    }

}
