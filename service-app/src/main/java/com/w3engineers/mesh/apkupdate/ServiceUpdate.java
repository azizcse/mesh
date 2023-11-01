package com.w3engineers.mesh.apkupdate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Network;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CompoundButton;

import androidx.databinding.DataBindingUtil;

import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.ui.TeleMeshServiceMainActivity;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.CredentialUtils;
import com.w3engineers.mesh.util.StorageUtil;
import com.w3engineers.mesh.wifi.WiFiUtil;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.meshrnd.BuildConfig;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.DialogAppUpdateWarningBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServiceUpdate {

    private static Context mContext;
    private static final String MAIN_JSON = "update-service.json";
    private static boolean isAppUpdateProcessStart;

    LinkStateListener linkStateListener;

    // lock the default constructor
    private ServiceUpdate() {
        // we can do initial stuff or first time stuff in here
    }

    public static ServiceUpdate getInstance(Context context) {
        mContext = context;
        return Singleton.INSTANCE;
    }

    public void initLinkStateListener(LinkStateListener listener) {
        this.linkStateListener = listener;
    }

    private static class Singleton {
        @SuppressLint("StaticFieldLeak")
        private static ServiceUpdate INSTANCE = new ServiceUpdate();
    }

    public void checkForUpdate(Context context) {

        if (!isNeedToCheck()) return;

        if (isAppUpdating()) return;
        setAppUpdateProcess(true);

        Log.d("TeleMeshMainActivity", "All condition full filled");

        if (WiFiUtil.isWifiConnected(mContext)) {
            WiFiUtil.isInternetAvailable((message, isConnected) -> {
                startNetworkSelectionProcess(isConnected, context);

            });
        } else {
            startNetworkSelectionProcess(false, context);
        }

    }

    private void startNetworkSelectionProcess(boolean isWifiNetworkAvailable, Context context) {
        Log.d("TeleMeshMainActivity", "Network selection process done");
        if (isWifiNetworkAvailable) {
            startDownloadProcess(context, null);
        } else {
            NetworkMonitor.setNetworkInterfaceListeners(new NetworkMonitor.NetworkInterfaceListener() {
                @Override
                public void onNetworkAvailable(boolean isOnline, Network network, boolean isWiFi) {
                    if (isOnline && network != null) {
                        startDownloadProcess(context, network);
                    }
                }
            });
            /*CellularDataNetworkUtil.on(mContext, new CellularDataNetworkUtil.CellularDataNetworkListenerForPurchase() {
                @Override
                public void onAvailable(Network network) {
                    startDownloadProcess(context, network);
                }

                @Override
                public void onLost() {

                }
            }).initMobileDataNetworkRequest();*/
        }
    }

    private void startDownloadProcess(Context context, Network network) {
        Log.d("TeleMeshMainActivity", "Download process datrt");
        RetrofitInterface downloadService = RetrofitService.createService(RetrofitInterface.class,
                CredentialUtils.getFileRepoLink(), network);
        Call<ResponseBody> call = downloadService.downloadFileByUrl(MAIN_JSON);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    new JsonTask(context, network).execute(response.body());
                } else {
                    setAppUpdateProcess(false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setAppUpdateProcess(false);
                Log.e("TeleMeshMainActivity", "Error: " + t.getMessage());
            }
        });
    }

    public boolean isNeedToCheck() {
        long saveTime = SharedPref.readLong(Constant.PreferenceKeys.SERVICE_UPDATE_CHECK_TIME);
        long dif = System.currentTimeMillis() - saveTime;
        long days = dif / (24 * 60 * 60 * 1000);
        return saveTime == 0 || days > 23;
    }

    @SuppressLint("StaticFieldLeak")
    private class JsonTask extends AsyncTask<ResponseBody, String, String> {
        Context context;
        private Network network;

        public JsonTask(Context context, Network network) {
            this.context = context;
            this.network = network;
        }

        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected String doInBackground(ResponseBody... params) {
            BufferedReader reader = null;
            try {

                InputStream stream = params[0].byteStream();

                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    //here u ll get whole response...... :-)
                }
                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            SharedPref.write(Constant.PreferenceKeys.SERVICE_UPDATE_CHECK_TIME, System.currentTimeMillis());

            if (!TSAppInstaller.isAppUpdating) {
                showAppInstallDialog(result, context, network);
            }
        }
    }

    public void setAppUpdateProcess(boolean isUpdating) {
        isAppUpdateProcessStart = isUpdating;
    }

    public boolean isAppUpdating() {
        return isAppUpdateProcessStart;
    }


    public void showAppInstallDialog(String json, Context context, Network network) {
        try {
            if (json == null) return;
            JSONObject jsonObject = new JSONObject(json);
            long versionCode = jsonObject.optLong(Constants.InAppUpdate.LATEST_VERSION_CODE_KEY);

            Log.e("TeleMeshMainActivity", "version code: " + versionCode);

            if (versionCode <= BuildConfig.VERSION_CODE) return;

            if (!(context instanceof Activity) || TeleMeshServiceMainActivity.getInstance() == null) {
                try {
                    linkStateListener.onServiceApkDownloadNeeded(true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return;
            }
            long version = jsonObject.optLong(Constants.InAppUpdate.LATEST_VERSION_CODE_KEY);
            String releaseNote = jsonObject.optString(Constants.InAppUpdate.RELEASE_NOTE_KEY);
            String url = CredentialUtils.getFileRepoLink();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            DialogAppUpdateWarningBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_app_update_warning, null, false);

            builder.setView(binding.getRoot());

            String message = mContext.getResources().getString(R.string.a_new_version) + " " + version + " " + mContext.getResources().getString(R.string.is_available_for_telemesh);

            binding.textViewMessage.setText(message);
            binding.textViewReleaseNote.setText(releaseNote);

            binding.buttonCancel.setText(mContext.getResources().getString(R.string.cancel));
            binding.buttonUpdate.setText(mContext.getResources().getString(R.string.update));
            binding.textViewWarning.setText(mContext.getResources().getString(R.string.do_you_want_to_update));
            binding.checkboxAskMeLater.setText(mContext.getResources().getString(R.string.ask_me_later));
            binding.textViewTitle.setText(mContext.getResources().getString(R.string.app_update));

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);


            binding.buttonCancel.setOnClickListener(v -> {
                setAppUpdateProcess(false);
                dialog.dismiss();
                // SharedPref.getSharedPref(context).write(Constants.preferenceKey.UPDATE_APP_VERSION, versionCode);
                SharedPref.write(Constants.preferenceKey.UPDATE_APP_VERSION, version);
            });


            //  SharedPref sharedPref = SharedPref.getSharedPref(App.getContext());

            binding.checkboxAskMeLater.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPref.write(Constants.preferenceKey.ASK_ME_LATER, isChecked);
                    SharedPref.write(Constants.preferenceKey.ASK_ME_LATER_TIME, System.currentTimeMillis());
                }
            });

            binding.buttonUpdate.setOnClickListener(v -> {
                dialog.dismiss();

                if (StorageUtil.getFreeMemory() > Constants.MINIMUM_SPACE) {
                    TSAppInstaller.downloadApkFile(context, url, network);
                    // requestMultiplePermissions(context, url);
                } else {
                    Toaster.showShort(mContext.getResources().getString(R.string.phone_storage_not_enough));
                }

            });

            dialog.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
