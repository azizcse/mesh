package com.w3engineers.purchase.manager;

import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.w3engineers.eth.data.helper.PreferencesHelperPaylib;
import com.w3engineers.eth.data.remote.EthereumService;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.purchase.dataplan.DataPlanManager;
import com.w3engineers.purchase.db.DatabaseService;
import com.w3engineers.purchase.db.networkinfo.NetworkInfo;
import com.w3engineers.purchase.helper.PreferencesHelperDataplan;
import com.w3engineers.purchase.helper.crypto.CryptoHelper;
import com.w3engineers.purchase.util.EthereumServiceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

import io.reactivex.Flowable;

public class PurchaseManager {

    protected PayController payController;
    protected EthereumService ethService;
    protected Context mContext;
    protected DatabaseService databaseService;
    protected PreferencesHelperDataplan preferencesHelperDataplan;
    protected PreferencesHelperPaylib preferencesHelperPaylib;
    private static PurchaseManager purchaseManager;
    protected DataPlanManager.DataPlanListener dataPlanListener;

    PurchaseManager(){
        payController = PayController.getInstance();
        mContext = MeshApp.getContext();
        ethService = EthereumServiceUtil.getInstance(mContext).getEthereumService();
        ethService.setCredential(payController.getCredentials());
        databaseService = DatabaseService.getInstance(mContext);
        preferencesHelperDataplan = PreferencesHelperDataplan.on();
        preferencesHelperPaylib = PreferencesHelperPaylib.onInstance(mContext);
    }

    public static PurchaseManager getInstance(){
        if (purchaseManager == null){
            purchaseManager = new PurchaseManager();
        }
        return purchaseManager;
    }


    protected void setEndPointInfoInJson(JSONObject jsonObject, int endPoint) throws JSONException {
        jsonObject.put(PurchaseConstants.JSON_KEYS.END_POINT_TYPE, endPoint);
    }

    public LiveData<Double> getTotalEarn(String myAddress, int endpoint) {
        try {
            return databaseService.getTotalEarnByUser(myAddress, endpoint);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public EthereumService getEthService(){
        return ethService;
    }

    public int getEndpoint(){
        return preferencesHelperPaylib.getEndpointMode();
    }

    public void setEndpoint(int endpoint){
        preferencesHelperPaylib.setEndPointMode(endpoint);
    }

    public LiveData<Double> getTotalSpent(String myAddress, int endPoint) {

        try {
            return databaseService.getTotalSpentByUser(myAddress, endPoint);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<Double> getTotalPendingEarning(String myAddress, int endpoint) {
        try {
            return databaseService.getTotalPendingEarningBySeller(myAddress, PurchaseConstants.CHANNEL_STATE.OPEN, endpoint);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Flowable<List<NetworkInfo>> getNetworkInfoByNetworkType() {
        try {
            return databaseService.getNetworkInfoByNetworkType();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void setDataPlanListener(DataPlanManager.DataPlanListener dataPlanListener) {
        this.dataPlanListener = dataPlanListener;
    }

    public void setParseInfo(String parseUrl, String parseAppId) {
        ethService.setParseInfo(parseUrl, parseAppId);
    }

    protected void setObserverForRequest(int type, int endPointType) {
        MeshLog.v("setObserverForRequest " + type);
        switch (type) {
            case PurchaseConstants.REQUEST_TYPES.APPROVE_ZERO:
            case PurchaseConstants.REQUEST_TYPES.APPROVE_TOKEN:
                long approveBlock = preferencesHelperDataplan.getBalanceApprovedBlock();
                ethService.logBalanceApproved(approveBlock, endPointType);
                break;
            case PurchaseConstants.REQUEST_TYPES.CREATE_CHANNEL:
                long createblock = preferencesHelperDataplan.getChannelCreatedBlock();
                ethService.logChannelCreated(createblock, endPointType);
                break;
            case PurchaseConstants.REQUEST_TYPES.CLOSE_CHANNEL:
                long closeBlock = preferencesHelperDataplan.getChannelClosedBlock();
                ethService.logChannelClosed(closeBlock, endPointType);
                break;
            case PurchaseConstants.REQUEST_TYPES.TOPUP_CHANNEL:
                long topupBlock = preferencesHelperDataplan.getChannelTopupBlock();
                ethService.logChannelToppedUp(topupBlock, endPointType);
                break;
            case PurchaseConstants.REQUEST_TYPES.WITHDRAW_CHANNEL:
                long withdrawnBlock = preferencesHelperDataplan.getChannelWithdrawnBlock();
                ethService.logChannelWithdrawn(withdrawnBlock, endPointType);
                break;
            case PurchaseConstants.REQUEST_TYPES.BUY_TOKEN:
                long buyTokenBlock = preferencesHelperDataplan.getTokenMintedBlock();
                ethService.logTokenMinted(buyTokenBlock, endPointType);
                break;
            default:
                break;

        }
    }

    protected String getRequestData(){
        String ownerPublicKey = ethService.getGiftDonatePublicKey();
        if (!TextUtils.isEmpty(ownerPublicKey)){
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("address", ethService.getAddress());
                jsonObject.put("endpoint", getEndpoint());
                jsonObject.put("devsecret", PurchaseConstants.DEV_SECRET);

                String encryptedMessage = CryptoHelper.encryptMessage(payController.walletService.getPrivateKey(), ownerPublicKey, jsonObject.toString());

                return encryptedMessage;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
