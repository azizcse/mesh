package com.w3engineers.mesh.workmanager;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.purchase.util.ConfigSyncUtil;

public class RefreshJobWorker extends Worker {

    private static int i = 0;
    private Context context;

    public RefreshJobWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        MeshLog.v("RefreshJobWorker called doWork");
        ConfigSyncUtil.getInstance().startConfigurationSync(context, false, NetworkMonitor.getNetwork());
        return Result.success();
    }

}
