package com.w3engineers.purchase.util;

import android.content.Context;
import android.net.Network;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.apkupdate.RetrofitInterface;
import com.w3engineers.mesh.apkupdate.RetrofitService;
import com.w3engineers.mesh.util.CredentialUtils;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.purchase.db.DatabaseService;
import com.w3engineers.purchase.db.SharedPref;
import com.w3engineers.purchase.db.clientinfo.ClientInfoEntity;
import com.w3engineers.purchase.model.ClientInfoModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 * <p>
 * Purpose: The main purpose of this class is to get the approved client information from server
 * And save the client information in local database. We are not save the client information in
 * local file because of security.
 * <p>
 * This class is also responsible to save initial client information from asset
 */
public class ClientInfoSyncUtil {

    /**
     * This variable is checking that each session will check server only one
     */
    private boolean isAlreadyChecked;
//    private ClientInfoSyncListener mListener;

    private static class SingletonHelper {
        public static ClientInfoSyncUtil instance = new ClientInfoSyncUtil();

    }

    private ClientInfoSyncUtil() {

    }

    public static ClientInfoSyncUtil getInstance() {
        return SingletonHelper.instance;
    }

    public void syncClientInformationFromServer() {
        NetworkMonitor.setNetworkInterfaceListeners((isOnline, network, isWiFi) -> {
            if (isOnline) {
                if (!isAlreadyChecked) {
                    retrofitTaskForClintIno(network);
                }

            }
        });
    }
    public void syncClientInformationFromServer(Context context) {
        Network network = NetworkMonitor.getConnectedMobileNetwork(context);
        retrofitTaskForClintIno(network);
    }


    public void loadFirstTimeClientInformation(String clientData) {
        int myClientInfoVersion = SharedPref.readInt(Utils.KEY_CLIENT_INFO_VERSION);
        ClientInfoModel model = new Gson().fromJson(clientData, ClientInfoModel.class);

        if (myClientInfoVersion < model.version) {
            SharedPref.write(Utils.KEY_CLIENT_INFO_VERSION, model.version);

            List<ClientInfoEntity> clientInfoList = ClientInfoEntity.convertClintDataToEntity(model);
            for (ClientInfoEntity entity : clientInfoList) {
                DatabaseService.getInstance(MeshApp.getContext()).insertClientInformation(entity);
            }
        }
    }

    public int getMyClientInfoVersion() {
        return SharedPref.readInt(Utils.KEY_CLIENT_INFO_VERSION);
    }

    public List<ClientInfoEntity> getAllClientInformation() {
        try {
            return DatabaseService.getInstance(MeshApp.getContext()).getAllClientInformation();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ClientInfoEntity getClientInfoByToken(String token) {
        return DatabaseService.getInstance(MeshApp.getContext()).getClientInformationByToken(token);
    }

    public String getAllClientInfo() {
        try {
            List<ClientInfoEntity> clientList = DatabaseService.getInstance(MeshApp.getContext()).getAllClientInformation();
            ClientInfoModel model = ClientInfoEntity.convertDBToClientData(clientList);
            model.version = getMyClientInfoVersion();

            return new Gson().toJson(model);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void processClientInformation(String clientInfoRawData) {
        ClientInfoModel model = new Gson().fromJson(clientInfoRawData, ClientInfoModel.class);
        int myClientInfoVersion = SharedPref.readInt(Utils.KEY_CLIENT_INFO_VERSION);

        if (myClientInfoVersion < model.version) {
            SharedPref.write(Utils.KEY_CLIENT_INFO_VERSION, model.version);

            List<ClientInfoEntity> clientInfoList = ClientInfoEntity.convertClintDataToEntity(model);
            for (ClientInfoEntity entity : clientInfoList) {
                DatabaseService.getInstance(MeshApp.getContext()).insertClientInformation(entity);
            }
        }

    }

    private void retrofitTaskForClintIno(android.net.Network network) {
        RetrofitInterface downloadService = RetrofitService.createService(RetrofitInterface.class,
                CredentialUtils.getFileRepoLink(), network);
        Call<ResponseBody> call = downloadService.downloadFileByUrl("clientInfo.json");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isAlreadyChecked = true;
                new ClintInfoDownloadTask(response.body())
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    private class ClintInfoDownloadTask extends AsyncTask<String, Void, String> {

        private ResponseBody responseBody;

        public ClintInfoDownloadTask(ResponseBody responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        protected String doInBackground(String... params) {
            BufferedReader reader = null;
            try {

                InputStream stream = responseBody.byteStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            MeshLog.v("Client information " + s);

            processClientInformation(s);
        }
    }

}
