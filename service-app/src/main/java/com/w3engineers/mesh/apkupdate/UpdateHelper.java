package com.w3engineers.mesh.apkupdate;

import androidx.annotation.NonNull;
import android.util.Log;


import com.w3engineers.mesh.App;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.ui.TeleMeshServiceMainActivity;
import com.w3engineers.mesh.util.HandlerUtil;

import java.util.concurrent.TimeUnit;

public class UpdateHelper {
    private static UpdateHelper mUpdateHelper = new UpdateHelper();

    @NonNull
    public static UpdateHelper getInstance() {
        return mUpdateHelper;
    }

    public void downloadApkFile() {
        Log.d("TeleMeshMainActivity", "DdownloadApkFile");
        HandlerUtil.postForeground(() -> {
            if (!ServiceUpdate.getInstance(App.getContext()).isAppUpdating()) {
                //InAppUpdate.getInstance(TeleMeshApplication.getContext()).setAppUpdateProcess(true);
                if (TeleMeshServiceMainActivity.getInstance() == null) return;

                if (SharedPref.readBoolean(Constants.preferenceKey.ASK_ME_LATER)) {
                    long saveTime = SharedPref.readLong(Constants.preferenceKey.ASK_ME_LATER_TIME);
                    long dif = System.currentTimeMillis() - saveTime;
                    long days = dif / (24 * 60 * 60 * 1000);

                    if (days <= 2) return;
                }
                ServiceUpdate.getInstance(App.getContext()).checkForUpdate(TeleMeshServiceMainActivity.getInstance());
            }
        }, TimeUnit.SECONDS.toMillis(30));
    }
}
