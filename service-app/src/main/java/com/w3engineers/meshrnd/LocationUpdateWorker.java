package com.w3engineers.meshrnd;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.w3engineers.mesh.manager.BoundServiceManager;

class LocationUpdateWorker extends Worker {
    public LocationUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        BoundServiceManager.on(getApplicationContext()).getClientBindStatus();
        return Result.success();
    }
}
