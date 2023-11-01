package com.w3engineers.meshrnd.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.util.AppLog;


public class AppService extends Service {


    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.v("App service is started");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.v("App service onStartCommand() called");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        AppLog.v("App service onTaskRemoved() called");
        ConnectionManager.on().stopMesh();
        super.onTaskRemoved(rootIntent);
    }

}
