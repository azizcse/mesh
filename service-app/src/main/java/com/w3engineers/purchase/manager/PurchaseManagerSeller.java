package com.w3engineers.purchase.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.w3engineers.eth.contracts.RaidenMicroTransferChannels;
import com.w3engineers.eth.contracts.TmeshToken;
import com.w3engineers.eth.data.remote.EthereumService;
import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.eth.util.helper.HandlerUtil;
import com.w3engineers.ext.strom.util.helper.Toaster;

import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.manager.BoundServiceManager;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.dataplan.DataPlanManager;
import com.w3engineers.purchase.db.buyerpendingmessage.BuyerPendingMessage;
import com.w3engineers.purchase.db.datausage.Datausage;
import com.w3engineers.purchase.db.purchase.Purchase;
import com.w3engineers.purchase.db.purchaserequests.PurchaseRequests;
import com.w3engineers.purchase.helper.PreferencesHelperDataplan;
import com.w3engineers.purchase.ui.dataplan.TestDataPlanActivity;
import com.w3engineers.purchase.ui.util.DialogUtil;
import com.w3engineers.purchase.ui.util.NotificationUtil;
import com.w3engineers.purchase.util.EthereumServiceUtil;
import com.w3engineers.purchase.util.PurchaseManagerUtil;
import com.w3engineers.purchase.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;

//import com.w3engineers.eth.util.helper.InternetUtil;

public class PurchaseManagerSeller extends PurchaseManager implements PayController.PayControllerListenerForSeller, EthereumService.TransactionObserver {
    private static PurchaseManagerSeller purchaseManagerSeller;
//    private BuyerPendingMessageListener buyerPendingMessageListener;

    private WalletManager.WalletListener walletListener;
    private AlertDialog datalimitAlert = null;

    private PurchaseManagerSeller() {
        super();

        setPayControllerListener();
        ethService.setTransactionObserver(this);
        if (preferencesHelperDataplan.getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_SELLER) {
            sendFailedPurchaseRequest();
            setObserverForPendingRequest();
        }
    }

    public static PurchaseManagerSeller getInstance() {
        if (purchaseManagerSeller == null) {
            purchaseManagerSeller = new PurchaseManagerSeller();
        }
        return purchaseManagerSeller;
    }

    public void setWalletListener(WalletManager.WalletListener walletListener) {
        this.walletListener = walletListener;
    }


    //***************************************************//
    //******************Private Methods******************//
    //***************************************************//
    private void sendFailedPurchaseRequest() {
        try {
            List<String> failedRequestRequesters = databaseService.getFailedRequestByUser(PurchaseConstants.REQUEST_STATE.RECEIVED);
            for (String requester : failedRequestRequesters) {
                pickAndSubmitRequest(requester);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setObserverForPendingRequest() {
        try {
            List<PurchaseRequests> pendingRequestList = databaseService.getBuyerPendingRequest(PurchaseConstants.REQUEST_STATE.PENDING);
            for (PurchaseRequests purchaseRequests : pendingRequestList) {
                setObserverForRequest(purchaseRequests.requestType, purchaseRequests.blockChainEndpoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMessagReceivedState(String userAddress) throws ExecutionException, InterruptedException {
        BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageByUser(PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.IN_PROGRESS, userAddress);
        if (buyerPendingMessage != null) {
            buyerPendingMessage.status = PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.RECEIVED;
            databaseService.updateBuyerPendingMessage(buyerPendingMessage);

        }
    }

    private void resumeUserPendingMessage(String userAddress) {
        MeshLog.o("************** resume *******");
        try {
            setMessagReceivedState(userAddress);
            sendInternetMessageToBuyer(userAddress);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isQueueing = false;
    private void checkMessageIsInProgressAndSend(String userAddress) {
        MeshLog.o("### attemp check ###");
        MeshLog.v("Message Queuing 2");
        try {
            BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageByUser(PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.IN_PROGRESS, userAddress);
            if (buyerPendingMessage == null){
                if(!isQueueing) {
                    isQueueing = true;
                    MeshLog.v("Message Queuing 3");
                    MeshLog.o("### no msg is in queue ###");
                    sendInternetMessageToBuyer(userAddress);
                }else {
                    MeshLog.v("Atke achi");
                }
            } else {
                MeshLog.o("### message in queue ###");
                if (buyerPendingMessage.updateTime < System.currentTimeMillis() - (sellerWaitingTime + 5000)){
                    MeshLog.o("### resending qued message ###");
                    resumeUserPendingMessage(userAddress);
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sentMessageInProgressState(BuyerPendingMessage buyerPendingMessage) {

        MeshLog.o("********* sentMessageInProgressState ****");

        buyerPendingMessage.status = PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.IN_PROGRESS;
        databaseService.updateBuyerPendingMessage(buyerPendingMessage);

    }

    //call for next message and send
    private boolean sendInternetMessageToBuyer(String userAddress) {
        MeshLog.v("Message Queuing 4");
        MeshLog.o("*********send internet to buyer ****");
        try {

            BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageByUser(PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.RECEIVED, userAddress);
            if (buyerPendingMessage != null) {
                MeshLog.v("Message Queuing 5");
                sentMessageInProgressState(buyerPendingMessage);
                isQueueing = false;
                processsQueue(buyerPendingMessage);
                return true;
            } else {
                isQueueing = false;
                MeshLog.o("no pending message... ");
            }
        } catch (ExecutionException e) {
            MeshLog.o("Exception in send internet message (ExecutionException): " + e);
//            return false;
        } catch (InterruptedException e) {
            MeshLog.o("Exception in send internet message(InterruptedException): " + e);
//            return false;
        }
        isQueueing = false;
        return false;
    }

    //send database queue head to buyer
    private void processsQueue(BuyerPendingMessage buyerPendingMessage) {
        MeshLog.o("### processsQueue ");
        MeshLog.v("Message Queuing 6");
        long dataSize = buyerPendingMessage.dataSize;
        String msg_id = buyerPendingMessage.msgId;
        String owner = "";
        if (buyerPendingMessage.isIncomming) {
            owner = buyerPendingMessage.owner;
        } else {
            owner = buyerPendingMessage.sender;
        }
        if (buyerPendingMessage.status == PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.IN_PROGRESS) {
            MeshLog.v("Message Queuing 7");
            JSONObject jObject = new JSONObject();
            try {
                jObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());

                jObject.put(PurchaseConstants.JSON_KEYS.DATA_SIZE, dataSize);
                jObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_ID, msg_id);
                jObject.put(PurchaseConstants.JSON_KEYS.IS_INCOMING, buyerPendingMessage.isIncomming);


                setRequestTimeout(msg_id, owner, PurchaseConstants.TimeoutPurpose.BUYER_PENDING_MESSAGE);
                payController.sendBuyerPendingMessageInfo(jObject, owner);

                MeshLog.o("### msg in progress ## msg_id:" + buyerPendingMessage.msgId);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private void processPurchaseRequest(String from, int endPointType) throws ExecutionException, InterruptedException {
        Double ethBalance = ethService.getUserEthBalance(from, endPointType);
        Double tokenBalance = ethService.getUserTokenBalance(from, endPointType);
        Double allowance = ethService.getUserTokenAllowance(from, endPointType);
        Integer nonce = ethService.getUserNonce(from, endPointType);
        long remainingData = DataPlanManager.getInstance().getRemainingData();

        if (ethBalance == null || tokenBalance == null || nonce == null || allowance == null) {
            sendPurchaseInitError(from, "Network error, please try again later.");
        } else {
            sendPurchaseInitOk(from, ethBalance, tokenBalance, nonce, allowance, endPointType, remainingData);
        }
    }

    private boolean havePendingTransactionInBlockChain(String address, List<PurchaseRequests> purchaseRequests, int endPointType) {
        boolean pendingStatus = false;
        boolean receivedStatus = false;
        try {
            if (purchaseRequests != null && purchaseRequests.size() > 0) {

                for (int i = purchaseRequests.size() - 1; i >= 0; i--) {

                    PurchaseRequests purchaseRequest = purchaseRequests.get(i);

                    String transactionHash = purchaseRequest.trxHash;

                    if (!TextUtils.isEmpty(transactionHash)) {

                        TransactionReceipt transactionReceipt = ethService.getTransactionReceipt(transactionHash, endPointType);

                        if (transactionReceipt != null) {

                            String transactionStatus = transactionReceipt.getStatus();

                            if (!TextUtils.isEmpty(transactionStatus)) {

                                if (transactionStatus.equals("0x1")) {
                                    // No need any operation
                                } else if (transactionStatus.equals("0x0")) {
                                    databaseService.deletePurchaseRequest(purchaseRequest);
                                } else {
                                    pendingStatus = true;
                                }

                            } else {
                                pendingStatus = true;
                            }

                        } else {
                            // If transaction is in pending mode
                            pendingStatus = true;
                        }

                    } else{
                        receivedStatus = true;
                    }
                }
            }

            if (receivedStatus && !pendingStatus){
                pickAndSubmitRequest(address);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pendingStatus || receivedStatus;
    }

    private void syncWithBuyer(String buyerAddress) {

        MeshLog.v("syncWithBuyer called");
        try {
            Purchase purchase = databaseService.getPurchaseByState(PurchaseConstants.CHANNEL_STATE.OPEN, buyerAddress, ethService.getAddress());
            if (purchase != null) {


                    if (NetworkMonitor.isOnline()){
                        TransactionReceipt transactionReceipt = ethService.getTransactionReceipt(purchase.trxHash, purchase.blockChainEndpoint);
                        if (transactionReceipt != null && transactionReceipt.getBlockNumber().longValue() != purchase.openBlockNumber){
                            MeshLog.v("BlockNumber changed old: " + purchase.openBlockNumber +" new: " + transactionReceipt.getBlockNumber().longValue());

                            purchase.openBlockNumber = transactionReceipt.getBlockNumber().longValue();
                            databaseService.updatePurchase(purchase);
                        }

                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                    jsonObject.put(PurchaseConstants.JSON_KEYS.BUYER_ADDRESS, buyerAddress);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.OPEN_BLOCK, purchase.openBlockNumber);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.USED_DATA, purchase.usedDataAmount);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.TOTAL_DATA, purchase.totalDataAmount);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.BPS_BALANCE, purchase.balance);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_BPS, purchase.balanceProof);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_CHS, purchase.closingHash);
                    jsonObject.put(PurchaseConstants.JSON_KEYS.TRX_HASH, purchase.trxHash);

                    setEndPointInfoInJson(jsonObject, purchase.blockChainEndpoint);

                    payController.sendSyncMessageToBuyer(jsonObject, buyerAddress);

            }
        } catch (Exception  e) {
            e.printStackTrace();
        }
    }

    private void giftEtherRequestSubmitted(boolean isSuccess, String message, String ethTrnxHash, String tknTrnxHash, int endPointType, String receiverAddress, String failedBy, double ethValue, double tknValue) {
        MeshLog.v("giftEther giftEtherRequestSubmitted");
        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_REQUEST_IS_SUBMITTED, isSuccess);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_REQUEST_SUBMIT_MESSAGE, message);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_ETH_HASH_REQUEST_SUBMIT, ethTrnxHash);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_TKN_HASH_REQUEST_SUBMIT, tknTrnxHash);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_TKN_FAILED_BY, failedBy);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_ETH_BALANCE, ethValue);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_TKN_BALANCE, tknValue);


            setEndPointInfoInJson(jsonObject, endPointType);

            payController.sendGiftRequestSubmitted(jsonObject, receiverAddress);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void giftEtherResponse(boolean isSuccess, double ethBalance, double tknBalance, int endPointType, String receiverAddress, double giftEtherValue, double giftTokenValue) {
        MeshLog.v("giftEther giftEtherResponse");

        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_REQUEST_IS_SUBMITTED, isSuccess);

            jsonObject.put(PurchaseConstants.INFO_KEYS.ETH_BALANCE, ethBalance);
            jsonObject.put(PurchaseConstants.INFO_KEYS.TKN_BALANCE, tknBalance);

            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_ETH_BALANCE, giftEtherValue);
            jsonObject.put(PurchaseConstants.JSON_KEYS.GIFT_TKN_BALANCE, giftTokenValue);

            setEndPointInfoInJson(jsonObject, endPointType);

            payController.sendGiftBalance(jsonObject, receiverAddress);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendBlockChainResponse(PurchaseRequests purchaseRequest, boolean success, String msg) {
        MeshLog.v("sendBlockChainResponse");
        if (purchaseRequest.requesterAddress.equalsIgnoreCase(ethService.getAddress())) {
            switch (purchaseRequest.requestType) {
                case PurchaseConstants.REQUEST_TYPES.BUY_TOKEN:

                        if (success) {

                            if (walletListener != null) {
                                walletListener.onTokenRequestResponse(true, "Purchase request sent, Balance will be updated soon.");
                            }
                        } else {

                            if (walletListener != null) {
                                walletListener.onTokenRequestResponse(false, msg);
                            }
                        }

                    break;

                case PurchaseConstants.REQUEST_TYPES.WITHDRAW_CHANNEL:

                    if (walletListener != null) {

//                        walletListener.onRequestSubmitted(success, msg);
                    }

                    break;
                case PurchaseConstants.REQUEST_TYPES.CLOSE_CHANNEL:
                    if (!success) {
                        try {
                            Purchase purchase = databaseService.getPurchaseByState(PurchaseConstants.CHANNEL_STATE.CLOSING, purchaseRequest.buyerAddress, ethService.getAddress());

                            if (purchase != null){
                                purchase.state = PurchaseConstants.CHANNEL_STATE.OPEN;
                                databaseService.updatePurchase(purchase);
                                syncWithBuyer(purchaseRequest.buyerAddress);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }


                    }
                default:
                    break;
            }

        } else {

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                jsonObject.put(PurchaseConstants.JSON_KEYS.REQUEST_SUCCESS, success);
                jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_TEXT, msg);
                jsonObject.put(PurchaseConstants.JSON_KEYS.REQUEST_TYPE, purchaseRequest.requestType);


                payController.sendBlockChainResponse(jsonObject, purchaseRequest.buyerAddress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void pickAndSubmitRequest(String address) {
        try {
            PurchaseRequests purchaseRequest = databaseService.pickNextRequest(address, PurchaseConstants.REQUEST_STATE.RECEIVED);
            if (purchaseRequest != null) {
                MeshLog.v("purchaseRequest" + purchaseRequest.toString());
                setObserverForRequest(purchaseRequest.requestType, purchaseRequest.blockChainEndpoint);

                ethService.submitRequest(purchaseRequest.signedMessage, purchaseRequest.rid,purchaseRequest.blockChainEndpoint, new EthereumService.SubmitRequestListener() {
                    @Override
                    public void onRequestSubmitted(String hash, int forRId) {
                        //TODO Need to check
                        //TODO Need to update transaction with single query --Major Arif 10/07/2019
                        if (!TextUtils.isEmpty(hash)) {
                            try {
                                PurchaseRequests purchaseRequestLatest = databaseService.getPurchaseRequestById(forRId);
                                if (purchaseRequestLatest.state < PurchaseConstants.REQUEST_STATE.PENDING) {
                                    purchaseRequestLatest.trxHash = hash;
                                    purchaseRequestLatest.state = PurchaseConstants.REQUEST_STATE.PENDING;
                                    databaseService.updatePurchaseRequest(purchaseRequestLatest);

                                    sendBlockChainResponse(purchaseRequestLatest, true, "Request submitted");
                                }
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                PurchaseRequests purchaseRequestLatest = databaseService.getPurchaseRequestById(forRId);
                                if (purchaseRequestLatest.state < PurchaseConstants.REQUEST_STATE.PENDING) {
                                    sendBlockChainResponse(purchaseRequestLatest, false, "Request submission error");
                                    databaseService.deletePurchaseRequest(purchaseRequestLatest);
                                }
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    @Override
                    public void onRequestSubmitError(String msg, int forRId) {
                        MeshLog.v(msg);
                        try {
                            PurchaseRequests purchaseRequestLatest = databaseService.getPurchaseRequestById(forRId);
                            if (purchaseRequestLatest.state < PurchaseConstants.REQUEST_STATE.PENDING) {
                                sendBlockChainResponse(purchaseRequestLatest, false, msg);
                                databaseService.deletePurchaseRequest(purchaseRequestLatest);
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    }
                });
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendPurchaseInitError(String buyerAddress, String msg) {
        MeshLog.v("sendPurchaseInitError");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
            jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_TEXT, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        payController.sendInitPurchaseError(jsonObject, buyerAddress);
    }

    private void sendPurchaseInitOk(String buyerAddress, double ethBalance, double tokenBalance, int nonce, double allowance, int endPointType, long remainingData) {

        MeshLog.v("sendPurchaseInitOk");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fr", ethService.getAddress());
            jsonObject.put("eth", ethBalance);
            jsonObject.put("tkn", tokenBalance);
            jsonObject.put("nonce", nonce);
            jsonObject.put("allowance", allowance);
            jsonObject.put(PurchaseConstants.INFO_KEYS.SHARED_DATA, remainingData);

            setEndPointInfoInJson(jsonObject, endPointType);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        payController.sendInitPurchaseOkay(jsonObject, buyerAddress);
    }

    private void sendDisconnectedBySeller(String buyerAddress){
        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
            jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_TEXT, "Seller does not have enough Data.");

            payController.sendDisconnectedBySeller(jsonObject, buyerAddress);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //***************************************************//
    //******************Public Medthods******************//
    //***************************************************//
    public void destroyObject() {
        payController.setSellerListener(null);
        purchaseManagerSeller = null;
        walletListener = null;
    }

    public void setPayControllerListener() {
        payController.setSellerListener(this);
    }

    public void getMyBalanceInfo() {

        if (NetworkMonitor.isOnline()) {
            try {

                int endPointType = getEndpoint();

                Double etherBalance = ethService.getUserEthBalance(ethService.getAddress(), endPointType);
                Double tokenBalance = ethService.getUserTokenBalance(ethService.getAddress(), endPointType);
                if (etherBalance != null) {
                    EthereumServiceUtil.getInstance(mContext).updateCurrency(endPointType, etherBalance);
                }

                if (tokenBalance != null) {
                    EthereumServiceUtil.getInstance(mContext).updateToken(endPointType, tokenBalance);
                }

                if (etherBalance == null || tokenBalance == null) {
                    if(walletListener != null) {
                        walletListener.onBalanceInfo(false, "Can't reach network, please try again later.");
                    }
                } else {

                    if(walletListener != null) {
                        walletListener.onBalanceInfo(true, "Balance updated");
                    }
                }
            } catch (Exception e) {
                if(walletListener != null) {
                    walletListener.onBalanceInfo(false, e.getMessage());
                }
            }
        } else {
            if(walletListener != null) {
                walletListener.onBalanceInfo(false, "No internet found on this device.");
            }
        }
    }

   /* public void sendEtherRequest() {
        if (InternetUtil.isNetworkConnected(mContext)) {
            int endpointType = getEndpoint();
            ethService.requestEther(ethService.getAddress(), endpointType, new EthereumService.ReqEther() {
                @Override
                public void onEtherRequested(int responseCode) {
                    if (responseCode == 200) {
                        if (walletListener != null) {
                            walletListener.onEtherRequestResponse(true, Util.getCurrencyTypeMessage("Request has been sent, %s will be added to your address soon."));
                        }
                    } else {

                        if (walletListener != null) {
                            walletListener.onEtherRequestResponse(false, Util.getCurrencyTypeMessage("%s request rejected!"));
                        }
                    }
                }
            });
        } else {

            if (walletListener != null) {
                walletListener.onEtherRequestResponse(false, "No internet found on this device.");
            }
        }
    }*/

    public void sendTokenRequest() {

        try {

            int endPointType = getEndpoint();
//            double token = EthereumServiceUtil.getInstance(mContext).getToken(endPointType);
//            double currency = EthereumServiceUtil.getInstance(mContext).getCurrency(endPointType);

            if (NetworkMonitor.isOnline()) {
                try {

                    Integer nonce = ethService.getUserNonce(ethService.getAddress(), endPointType);
                    if (nonce != null) {
                        String buyTokenSignedMessage = ethService.buyToken(PurchaseConstants.BUY_TOKEN_ETHER_VALUE, nonce, endPointType);

                        PurchaseRequests purchaseRequest = new PurchaseRequests();
                        purchaseRequest.buyerAddress = ethService.getAddress();
                        purchaseRequest.requesterAddress = ethService.getAddress();
                        purchaseRequest.requestType = PurchaseConstants.REQUEST_TYPES.BUY_TOKEN;
                        purchaseRequest.signedMessage = buyTokenSignedMessage;
                        purchaseRequest.requestValue = PurchaseConstants.BUY_TOKEN_ETHER_VALUE;
                        purchaseRequest.nonce = nonce;
                        purchaseRequest.state = PurchaseConstants.REQUEST_STATE.RECEIVED;
                        purchaseRequest.blockChainEndpoint = endPointType;

                        databaseService.insertPurchaseRequest(purchaseRequest);
                        pickAndSubmitRequest(purchaseRequest.requesterAddress);

                    } else {

                        if (walletListener != null) {
                            walletListener.onTokenRequestResponse(false, "Can't reach network, please try again later.");
                        }
                    }
                }  catch (Exception e) {

                    if (walletListener != null) {
                        walletListener.onTokenRequestResponse(false, e.getMessage());
                    }
                }
            } else {

                if (walletListener != null) {
                    walletListener.onTokenRequestResponse(false, "No internet found on this device.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiveData<Integer> getTotalOpenChannel() throws ExecutionException, InterruptedException {
        return databaseService.getTotalOpenChannel(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN);
    }

    public void getAllOpenDrawableBlock() {

        try {
            int endPointType = getEndpoint();
            List<PurchaseRequests> purchaseRequests = databaseService.getUserIncompleteRequests(ethService.getAddress(), PurchaseConstants.REQUEST_STATE.PENDING, endPointType);

            if (purchaseRequests != null && purchaseRequests.size() > 0) {
                if (havePendingTransactionInBlockChain(ethService.getAddress(), purchaseRequests, endPointType)) {
                    return;
                }
            }


            List<Purchase> allOpenDrawablePurchaseList = databaseService.getAllOpenDrawableBlock(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN, endPointType);

            if (allOpenDrawablePurchaseList != null && allOpenDrawablePurchaseList.size() > 0) {

                Double ethBalance = ethService.getUserEthBalance(ethService.getAddress(), endPointType);
                Integer nonce = ethService.getUserNonce(ethService.getAddress(), endPointType);

                if (ethBalance != null && nonce != null && ethBalance > 0) {

                    for (Purchase purchase : allOpenDrawablePurchaseList) {
                        String signedMessage = ethService.withdraw(purchase.openBlockNumber, purchase.balance, purchase.balanceProof, nonce, purchase.blockChainEndpoint);

                        PurchaseRequests purchaseRequest = new PurchaseRequests();
                        purchaseRequest.buyerAddress = purchase.buyerAddress;
                        purchaseRequest.requesterAddress = ethService.getAddress();
                        purchaseRequest.requestType = PurchaseConstants.REQUEST_TYPES.WITHDRAW_CHANNEL;
                        purchaseRequest.signedMessage = signedMessage;
                        purchaseRequest.requestValue = purchase.balance;
                        purchaseRequest.nonce = nonce;
                        purchaseRequest.state = PurchaseConstants.REQUEST_STATE.RECEIVED;
                        purchaseRequest.blockChainEndpoint = purchase.blockChainEndpoint;
                        databaseService.insertPurchaseRequest(purchaseRequest);
                        nonce++;
                    }
                    pickAndSubmitRequest(ethService.getAddress());
                } else {
                    MeshLog.v("User can't withdraw, cause: balance or nonce error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeAllActiveBuyerChannel() {
        try {

            int endPointType = getEndpoint();

            List<Purchase> activeChannelList = databaseService.getAllActiveChannel(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN, endPointType);

            Double etherBalance = ethService.getUserEthBalance(ethService.getAddress(), endPointType);
            Integer nonce = ethService.getUserNonce(ethService.getAddress(), endPointType);

            if (etherBalance != null && nonce != null && etherBalance > 0) {
                for (Purchase purchase : activeChannelList) {
                    purchase.state = PurchaseConstants.CHANNEL_STATE.CLOSING;
                    databaseService.updatePurchase(purchase);

                    payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(purchase.buyerAddress);
                    sendDisconnectedBySeller(purchase.buyerAddress);


                    String signedMessage = ethService.close(purchase.sellerAddress, purchase.openBlockNumber, purchase.balance, purchase.balanceProof, purchase.closingHash, nonce, purchase.blockChainEndpoint);

                    PurchaseRequests purchaseRequest = new PurchaseRequests();
                    purchaseRequest.requesterAddress = ethService.getAddress();
                    purchaseRequest.buyerAddress = purchase.buyerAddress;
                    purchaseRequest.requestType = PurchaseConstants.REQUEST_TYPES.CLOSE_CHANNEL;
                    purchaseRequest.signedMessage = signedMessage;
                    purchaseRequest.requestValue = purchase.balance;
                    purchaseRequest.nonce = nonce;
                    purchaseRequest.state = PurchaseConstants.REQUEST_STATE.RECEIVED;
                    purchaseRequest.blockChainEndpoint = purchase.blockChainEndpoint;
                    databaseService.insertPurchaseRequest(purchaseRequest);
                    nonce++;
                }
                pickAndSubmitRequest(ethService.getAddress());
            } else {
                for (Purchase purchase : activeChannelList) {
                    payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(purchase.buyerAddress);
                    sendDisconnectedBySeller(purchase.buyerAddress);
                }

                HandlerUtil.postForeground(new Runnable() {
                    @Override
                    public void run() {
                        Toaster.showLong("User does not have enough balance to clse the purchases.");
                    }
                });
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean requestForGiftForSeller() {

        if (NetworkMonitor.isOnline()) {

            String address = ethService.getAddress();
            int endpoint = getEndpoint();
            int requestState = preferencesHelperDataplan.getEtherRequestStatus(endpoint);

            if (requestState == PurchaseConstants.GIFT_REQUEST_STATE.NOT_REQUESTED_YET || requestState == PurchaseConstants.GIFT_REQUEST_STATE.REQUESTED_TO_SELLER) {

                MeshLog.v("requestForGiftForSeller 1");
                long currentTime = new Date().getTime();
                long requestTime = preferencesHelperDataplan.getEtherRequestTimeStamp(endpoint);
                if (currentTime > (requestTime + 20000)) {
                    MeshLog.v("requestForGiftForSeller 3");
                    preferencesHelperDataplan.setRequestedForEther(PurchaseConstants.GIFT_REQUEST_STATE.REQUESTED_TO_SELLER, endpoint);
                    preferencesHelperDataplan.setEtherRequestTimeStamp(currentTime, endpoint);
                    String requestData = getRequestData();
                    if (requestData != null) {
                        MeshLog.v("requestForGiftForSeller 4");
                        ethService.requestGiftEther(address, endpoint, requestData, payController.walletService.getPublicKey(), new EthereumService.GiftEther() {
                            @Override
                            public void onEtherGiftRequested(boolean success, String msg, String ethTX, String tknTx, String failedBy, double ethValue, double tknValue) {
                                MeshLog.v("giftEther onEtherGiftRequested " + "success " + success + " msg " + msg + " ethTX " + ethTX + " tknTx " + tknTx + " failedby " + failedBy);

                                PreferencesHelperDataplan preferencesHelperDataplan = PreferencesHelperDataplan.on();

                                if (success) {
                                    MeshLog.v("requestForGiftForSeller 5");

                                    preferencesHelperDataplan.setRequestedForEther(PurchaseConstants.GIFT_REQUEST_STATE.GOT_TRANX_HASH, endpoint);
                                    preferencesHelperDataplan.setGiftEtherHash(ethTX, endpoint);
                                    preferencesHelperDataplan.setGiftTokenHash(tknTx, endpoint);
                                    preferencesHelperDataplan.setGiftEtherValue(ethValue, endpoint);
                                    preferencesHelperDataplan.setGiftTokenValue(tknValue, endpoint);

//                                String toastMessage = Util.getCurrencyTypeMessage("Congratulations!!!\nYou have been awarded 1 %s and 50 token.\nBalance will be added within few minutes.");
                                    String toastMessage = PurchaseManagerUtil.getCurrencyTypeMessage("Congratulations!!!\nYou have been awarded with " + tknValue + " points which will be added within few minutes."); //changed per decision

                                    sendGiftListener(success, false, toastMessage);

                                /*Activity currentActivity = MeshApp.getCurrentActivity();
                                if (currentActivity != null) {
                                    HandlerUtil.postForeground(() -> DialogUtil.showConfirmationDialog(currentActivity, "Gift Awarded!", toastMessage, null, "OK", null));
                                } else {
                                    //TODO send notifications
                                }*/
                                } else {
                                    MeshLog.v("giftEther giftRequestSubmitted " + msg);
                                    if (failedBy.equals("admin")) {
                                        preferencesHelperDataplan.setRequestedForEther(PurchaseConstants.GIFT_REQUEST_STATE.GOT_GIFT_ETHER, endpoint);
                                        getMyBalanceInfo();
                                    } else {
                                        preferencesHelperDataplan.setRequestedForEther(PurchaseConstants.GIFT_REQUEST_STATE.NOT_REQUESTED_YET, endpoint);

                                        sendGiftListener(success, false, msg);

                                    /*Activity currentActivity = MeshApp.getCurrentActivity();
                                    if (currentActivity != null) {
                                        HandlerUtil.postForeground(() -> DialogUtil.showConfirmationDialog(currentActivity, "Gift Awarded!", msg, null, "OK", null));
                                    }*/
                                    }
                                }
                            }
                        });
                    }
                    return true;
                }

            } else if (requestState == PurchaseConstants.GIFT_REQUEST_STATE.GOT_TRANX_HASH) {
                MeshLog.v("requestForGiftForSeller 2");

                String ethTranxHash = preferencesHelperDataplan.getGiftEtherHash(endpoint);
                String tknTranxHash = preferencesHelperDataplan.getGiftTokenHash(endpoint);

                if (!TextUtils.isEmpty(ethTranxHash) && !TextUtils.isEmpty(tknTranxHash)) {
                    double ethValue = preferencesHelperDataplan.getGiftEtherValue(endpoint);
                    double tknValue = preferencesHelperDataplan.getGiftTokenValue(endpoint);
                    ethService.getStatusOfGift(address, ethTranxHash, tknTranxHash, endpoint, ethValue, tknValue);
                    return true;
                }

            }
        }
        return false;
    }

    private void sendGiftListener(boolean status, boolean isGifted, String message) {
        if (walletListener != null) {
            walletListener.onGiftResponse(status, isGifted, message);
        }
    }


    //*********************************************************//
    //******************PayControllerListener******************//
    //*********************************************************//
    @Override
    public void onPurchaseInitRequested(String from, int endPointType) {
        MeshLog.v("onPurchaseInitRequested: " + from);
        try {
            List<PurchaseRequests> purchaseRequests = databaseService.getUserIncompleteRequests(from, PurchaseConstants.REQUEST_STATE.PENDING, endPointType);

            if (purchaseRequests != null && purchaseRequests.size() > 0) {

                if (havePendingTransactionInBlockChain(from, purchaseRequests, endPointType)) {
                    sendPurchaseInitError(from, "You already have " + purchaseRequests.size() + " pending requests.");
                } else {
                    processPurchaseRequest(from, endPointType);
                }
            } else {
                processPurchaseRequest(from, endPointType);
            }
        } catch (ExecutionException e) {
            MeshLog.v("ExecutionException " + e.getMessage());
            sendPurchaseInitError(from, e.getMessage());
        } catch (InterruptedException e) {
            MeshLog.v("InterruptedException " + e.getMessage());
            sendPurchaseInitError(from, e.getMessage());
        }
    }

    @Override
    public void onCreateChannelRequested(String from, JSONArray reqList, int endPointType) {
        MeshLog.v("onCreateChannelRequested: " + from + " , " + reqList.toString());
        try {
            for (int i = 0; i < reqList.length(); i++) {
                JSONObject job = (JSONObject) reqList.get(i);
                PurchaseRequests purchaseRequest = new PurchaseRequests();
                purchaseRequest.buyerAddress = from;
                purchaseRequest.requesterAddress = from;
                purchaseRequest.requestType = job.getInt(PurchaseConstants.JSON_KEYS.REQUEST_TYPE);
                purchaseRequest.signedMessage = job.getString(PurchaseConstants.JSON_KEYS.SIGNED_MESSAGE);
                purchaseRequest.requestValue = job.getDouble(PurchaseConstants.JSON_KEYS.REQUEST_VALUE);
                purchaseRequest.nonce = job.getInt(PurchaseConstants.JSON_KEYS.NONCE);
                purchaseRequest.state = PurchaseConstants.REQUEST_STATE.RECEIVED;
                purchaseRequest.blockChainEndpoint = endPointType;

                databaseService.insertPurchaseRequest(purchaseRequest);
            }

            pickAndSubmitRequest(from);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBuyTokenRequested(String from, JSONArray jsonArray, int endPointType) {

        MeshLog.v("onBuyTokenRequested: " + from + " , " + jsonArray.toString());
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject job = (JSONObject) jsonArray.get(i);
                PurchaseRequests purchaseRequest = new PurchaseRequests();
                purchaseRequest.buyerAddress = from;
                purchaseRequest.requesterAddress = from;
                purchaseRequest.requestType = job.getInt(PurchaseConstants.JSON_KEYS.REQUEST_TYPE);
                purchaseRequest.signedMessage = job.getString(PurchaseConstants.JSON_KEYS.SIGNED_MESSAGE);
                purchaseRequest.requestValue = job.getDouble(PurchaseConstants.JSON_KEYS.REQUEST_VALUE);
                purchaseRequest.nonce = job.getInt(PurchaseConstants.JSON_KEYS.NONCE);
                purchaseRequest.state = PurchaseConstants.REQUEST_STATE.RECEIVED;
                purchaseRequest.blockChainEndpoint = endPointType;

                databaseService.insertPurchaseRequest(purchaseRequest);
            }

            pickAndSubmitRequest(from);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBlockchainRequestReceived(String from, JSONArray jArray, int endPointType) {
        MeshLog.v("onBlockchainRequestReceived: " + from + " , " + jArray.toString());
        try {
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject job = (JSONObject) jArray.get(i);
                PurchaseRequests purchaseRequest = new PurchaseRequests();
                purchaseRequest.buyerAddress = from;
                purchaseRequest.requesterAddress = from;
                purchaseRequest.requestType = job.getInt(PurchaseConstants.JSON_KEYS.REQUEST_TYPE);
                purchaseRequest.signedMessage = job.getString(PurchaseConstants.JSON_KEYS.SIGNED_MESSAGE);
                purchaseRequest.requestValue = job.getDouble(PurchaseConstants.JSON_KEYS.REQUEST_VALUE);
                purchaseRequest.nonce = job.getInt(PurchaseConstants.JSON_KEYS.NONCE);
                purchaseRequest.state = PurchaseConstants.REQUEST_STATE.RECEIVED;
                purchaseRequest.blockChainEndpoint = endPointType;

                databaseService.insertPurchaseRequest(purchaseRequest);
            }
            pickAndSubmitRequest(from);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUserConnected(String address) {
        MeshLog.v("onUserConnected " + address);
        try {
            List<PurchaseRequests> list = databaseService.getUserCompletedRequested(address, PurchaseConstants.REQUEST_STATE.COMPLETED);
            if (list != null) {
                MeshLog.v("PurchaseRequests " + list.toString());

                if (list.size() > 0) {
                    for (PurchaseRequests p : list) {

                        String buyerAddress = p.buyerAddress;
                        JSONObject j = new JSONObject(p.responseString);
                        String mId = p.messageId;
                        switch (p.requestType) {
                            case PurchaseConstants.REQUEST_TYPES.CREATE_CHANNEL:
                                payController.sendCreateChannelOkay(j, buyerAddress, mId);
                                break;

                            case PurchaseConstants.REQUEST_TYPES.TOPUP_CHANNEL:
                                payController.sendTopupChannelOkay(j, buyerAddress, mId);
                                break;

                            case PurchaseConstants.REQUEST_TYPES.CLOSE_CHANNEL:
                                payController.sendChannelClosed(j, buyerAddress, mId);
                                break;

                            case PurchaseConstants.REQUEST_TYPES.BUY_TOKEN:
                                payController.sendBuyTokenResponse(j, buyerAddress, mId);
                                break;

                            default:
                                break;
                        }
                    }
                    //Todo: May be we can take a decision from here to send sync finally
                    //     purchaseRequestDone();

                } else {
                    //syncWithBuyer(address);
                }
            } else {
                //syncWithBuyer(address);
                MeshLog.v("no pending request");
            }
        } catch (Exception e) {
            MeshLog.v("Exception" + e.getMessage());
        }
    }

    @Override
    public void onUserDisconnected(String address) {

    }

    @Override
    public void onMessageAcknowledgmentReceived(String from, String messageId) {
        try {
            PurchaseRequests purchaseRequests = databaseService.getPurchaseRequestByMessageId(messageId);
            if (purchaseRequests != null) {
                purchaseRequests.state = PurchaseConstants.REQUEST_STATE.NOTIFIED;
                databaseService.updatePurchaseRequest(purchaseRequests);

                List<PurchaseRequests> list = databaseService.getUserCompletedRequested(from, PurchaseConstants.REQUEST_STATE.COMPLETED);
                if (list.size() == 0 && (purchaseRequests.requestType == PurchaseConstants.REQUEST_TYPES.CREATE_CHANNEL
                        || purchaseRequests.requestType == PurchaseConstants.REQUEST_TYPES.TOPUP_CHANNEL)){

//                    payController.getDataManager().onBuyerConnected(from);
//                    payController.transportManager.getInternetTransport().onBuyerConnected(from);
                    resumeUserPendingMessage(from);
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInfoQueryReceived(String fromAddress, String query, int purpose, int endPointType) {
        MeshLog.p("onInfoQueryReceived " + fromAddress + " " + query);

        JSONObject response = new JSONObject();
        try {
            response.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
            response.put(PurchaseConstants.JSON_KEYS.INFO_PURPOSE, purpose);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject infoJson = new JSONObject();

        String[] arrOfStr = query.split(",");

        Observable<String> observable = Observable.fromArray(arrOfStr);

        observable.subscribe(item -> {

//                    MeshLog.p("observe  " + item);
                    switch (item) {
                        case PurchaseConstants.INFO_KEYS.SHARED_DATA:
                            try {
                                long remainingData = DataPlanManager.getInstance().getRemainingData();
                                infoJson.put(item, remainingData);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case PurchaseConstants.INFO_KEYS.ETH_BALANCE:
                            Double ethBalance = ethService.getUserEthBalance(fromAddress, endPointType);
                            if (ethBalance != null) {
                                try {
                                    infoJson.put(item, ethBalance);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case PurchaseConstants.INFO_KEYS.TKN_BALANCE:
                            Double tknBalance = ethService.getUserTokenBalance(fromAddress, endPointType);
                            if (tknBalance != null) {
                                try {
                                    infoJson.put(item, tknBalance);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            break;
                        case PurchaseConstants.INFO_KEYS.ALOWANCE:
                            Double allowance = ethService.getUserTokenAllowance(fromAddress, endPointType);
                            if (allowance != null) {
                                try {
                                    infoJson.put(item, allowance);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case PurchaseConstants.INFO_KEYS.NONCE:
                            Integer nonce = ethService.getUserNonce(fromAddress, endPointType);
                            if (nonce != null) {
                                try {
                                    infoJson.put(item, nonce);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            break;
                        case PurchaseConstants.INFO_KEYS.PENDING_REQUEST_NUMBER:
                            List<PurchaseRequests> purchaseRequests = databaseService.getPendingRequestByUser(fromAddress, PurchaseConstants.REQUEST_STATE.PENDING);
                            try {
                                infoJson.put(item, purchaseRequests.size());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case PurchaseConstants.INFO_KEYS.PURCHASE_INFO:
                            //TODO add purchase info
                            break;
                        default:
                            break;
                    }

                }, error -> {
//                    MeshLog.p("Observable Error " + error.getMessage());
                    try {
                        response.put(PurchaseConstants.JSON_KEYS.MESSAGE_TEXT, error.getMessage());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    payController.sendInfoErrorMessage(response, fromAddress);
                },
                () -> {
//                    MeshLog.p("done");
//                    MeshLog.p("data " + infoJson.toString());
                    response.put(PurchaseConstants.JSON_KEYS.INFO_JSON, infoJson);
                    setEndPointInfoInJson(response, endPointType);
                    payController.sendInfoOkayMessage(response, fromAddress);

                });
    }

    @Override
    public void onPayForMessageOkReceived(String from, String msg_id, String bps, double bps_balance, long open_block) {
        MeshLog.o("-- onPayForMessageOkReceived --");
        MeshLog.v("Message Queuing 10");

        onPayMessageResponseReceived(from, msg_id);
        try {
            Purchase purchase = databaseService.getPurchaseByBlockNumber(open_block, from, ethService.getAddress());
            if (purchase != null) {


                if (bps_balance > purchase.deposit) {
                    //TODO what to do here
                } else {

                    MeshLog.v("Message Queuing 11");
                    MeshLog.v("Message id " + msg_id + "  bps_balance " + bps_balance + "  purchase.balance " + purchase.balance);
                    BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageById(msg_id);


                    double previousBalance = purchase.balance;
                    double addedBalance = bps_balance - previousBalance;

                    double dataSizeMB = Util.convertBytesToMegabytes(buyerPendingMessage.dataSize);
                    double dataPrice = PreferencesHelperDataplan.on().getPerMbTokenValue() * dataSizeMB;

                    MeshLog.v("addedBalance " + addedBalance + "  dataPrice " + dataPrice);

                    if (buyerPendingMessage.status == PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.SENT_PAID && TextUtils.isEmpty(buyerPendingMessage.msgData)){
                        checkMessageIsInProgressAndSend(from);
                        return;
                    }

                    if ("paid".equals(buyerPendingMessage.comment) || addedBalance >= dataPrice) {
                        MeshLog.o("-- balance ok --");
//                        String sender = ethService.verifyBalanceProofSignature(ethService.getAddress(), open_block, bps_balance, bps, purchase.blockChainEndpoint);
                        buyerPendingMessage.comment = "paid";

                        MeshLog.v("Message Queuing 12");
                        String address = buyerPendingMessage.owner;
                        if (!buyerPendingMessage.isIncomming) {
                            address = buyerPendingMessage.sender;

                        }
                        MeshLog.o("IsIncomming: " + buyerPendingMessage.isIncomming + " address: " + address);

//                                if (sender.equalsIgnoreCase(address)) {

                        if (preferencesHelperDataplan.getDataAmountMode() == DataPlanConstants.DATA_MODE.LIMITED) {
                            long fromDate = preferencesHelperDataplan.getSellFromDate();
                            long sharedData = preferencesHelperDataplan.getSellDataAmount();

                            MeshLog.v("Message Queuing 13");

                            long usedData = 0;
                            try {
                                usedData = databaseService.getDataUsageByDate(fromDate);
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (usedData >= sharedData-buyerPendingMessage.dataSize){
                                if (dataPlanListener != null){
                                    dataPlanListener.onLimitFinished(true, "Data limit exceeded.");
                                }

                                Activity currentActivity = MeshApp.getCurrentActivity();
                                if (currentActivity != null && (datalimitAlert == null || !datalimitAlert.isShowing())) {
                                    int numberOfActiveBuyer = databaseService.getTotalNumberOfActiveBuyer(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN);
                                     HandlerUtil.postForeground(() -> {
                                         datalimitAlert = DialogUtil.showConfirmationDialog(currentActivity, "Data Limit exceeded!", "Your data shared limit" + " " + Util.humanReadableByteCount(sharedData) + " " + "exceeded, there are" + " " + numberOfActiveBuyer + " " + "active buyer." + "Do you want to increase your shared data limit? If not then all the active channel will be closed", "No, Thanks", "Ok", new DialogUtil.DialogButtonListener() {
                                             @Override
                                             public void onClickPositive() {
                                                 datalimitAlert = null;
                                                 if (!(currentActivity instanceof TestDataPlanActivity)) {
                                                     DataPlanManager.openActivity(currentActivity, BoundServiceManager.on(currentActivity).getFirstAppToken());
                                                 }
                                             }

                                             @Override
                                             public void onCancel() {
                                                 datalimitAlert = null;
                                             }

                                             @Override
                                             public void onClickNegative() {
                                                 datalimitAlert = null;
                                                 closeAllActiveBuyerChannel();
                                             }
                                         });
                                     });
                                } else {
                                    int numberOfActiveBuyer = databaseService.getTotalNumberOfActiveBuyer(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN);
                                    NotificationUtil.showSellerWarningNotification(mContext, "Data Limit exceeded!", "You need to take action.", numberOfActiveBuyer);
                                }
                                setMessagReceivedState(from);

                                return;
                            }


                            if (usedData >= (sharedData - Constant.SELLER_MINIMUM_WARNING_DATA)) {
                                dataPlanListener.onLimitFinished(false, "Data limit almost exceeded.");

                                try {
                                    int numberOfActiveBuyer = databaseService.getTotalNumberOfActiveBuyer(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN);
                                    NotificationUtil.showSellerWarningNotification(mContext, "Data Limit exceeded!", "You need to take action.", numberOfActiveBuyer);

                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            MeshLog.v("Message Queuing 14");

                            Datausage datausage = new Datausage();
                            datausage.dataByte = buyerPendingMessage.dataSize;
                            datausage.purchaseId = purchase.pid;
                            datausage.purpose = PurchaseConstants.DATA_USAGE_PURPOSE.MESSAGE;
                            databaseService.insertDataUsage(datausage);

                            String payer = "";
                            if (buyerPendingMessage.isIncomming) {
                                payer = buyerPendingMessage.owner;
                            } else {
                                payer = buyerPendingMessage.sender;
                            }

                            String closingHash = ethService.getClosingHash(payer, open_block, bps_balance, purchase.blockChainEndpoint);

                            purchase.balanceProof = bps;
                            purchase.balance = bps_balance;
                            purchase.closingHash = closingHash;
                            purchase.usedDataAmount = purchase.usedDataAmount + dataSizeMB;
                            databaseService.updatePurchase(purchase);

                            MeshLog.o("balancemismatchcheck3 " + bps_balance + "  " + purchase.usedDataAmount);
                            databaseService.updateBuyerPendingMessage(buyerPendingMessage);

                            MeshLog.v("Message Queuing 15");

                            try {
                                MeshLog.v("Message Queuing 18");
                                JSONObject successJson = new JSONObject();
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAGE_BPS, bps);
                                successJson.put(PurchaseConstants.JSON_KEYS.BPS_BALANCE, bps_balance);
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAGE_CHS, closingHash);
                                successJson.put(PurchaseConstants.JSON_KEYS.OPEN_BLOCK, open_block);
                                successJson.put(PurchaseConstants.JSON_KEYS.DATA_SIZE, buyerPendingMessage.dataSize);
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAGE_ID, msg_id);
                                String msg_receiver = "";
                                if (buyerPendingMessage.isIncomming) {
                                    msg_receiver = buyerPendingMessage.owner;
                                } else {
                                    msg_receiver = buyerPendingMessage.sender;
                                }

                                setRequestTimeout(msg_id, msg_receiver, PurchaseConstants.TimeoutPurpose.PAY_FOR_MESSAGE_RESPONSE);
                                payController.sendPayForMessageResponse(successJson, msg_receiver);

                            } catch (JSONException e) {
                                //TODO what to do here
//                                        MeshLog.p("onPayForMessageOkReceived EX " + e.getMessage());
                            }


                        } else {

                            MeshLog.v("Message Queuing 19");

                            Datausage datausage = new Datausage();
                            datausage.dataByte = buyerPendingMessage.dataSize;
                            datausage.purchaseId = purchase.pid;
                            datausage.purpose = PurchaseConstants.DATA_USAGE_PURPOSE.MESSAGE;
                            databaseService.insertDataUsage(datausage);

                            String payer = "";
                            if (buyerPendingMessage.isIncomming) {
                                payer = buyerPendingMessage.owner;
                            } else {
                                payer = buyerPendingMessage.sender;
                            }
                            String closingHash = ethService.getClosingHash(payer, open_block, bps_balance, purchase.blockChainEndpoint);


                            purchase.balanceProof = bps;
                            purchase.balance = bps_balance;
                            purchase.closingHash = closingHash;
                            purchase.usedDataAmount = purchase.usedDataAmount + dataSizeMB;
                            databaseService.updatePurchase(purchase);

                            MeshLog.o("balancemismatchcheck3 " + bps_balance + "  " + purchase.usedDataAmount);
                            databaseService.updateBuyerPendingMessage(buyerPendingMessage);

                            MeshLog.v("Message Queuing 20");

                            try {
                                JSONObject successJson = new JSONObject();
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAGE_BPS, bps);
                                successJson.put(PurchaseConstants.JSON_KEYS.BPS_BALANCE, bps_balance);
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAGE_CHS, closingHash);
                                successJson.put(PurchaseConstants.JSON_KEYS.OPEN_BLOCK, open_block);
                                successJson.put(PurchaseConstants.JSON_KEYS.DATA_SIZE, buyerPendingMessage.dataSize);
                                successJson.put(PurchaseConstants.JSON_KEYS.MESSAGE_ID, msg_id);

                                String msg_receiver = "";
                                if (buyerPendingMessage.isIncomming) {
                                    msg_receiver = buyerPendingMessage.owner;
                                } else {
                                    msg_receiver = buyerPendingMessage.sender;
                                }

                                setRequestTimeout(msg_id, msg_receiver, PurchaseConstants.TimeoutPurpose.PAY_FOR_MESSAGE_RESPONSE);
                                payController.sendPayForMessageResponse(successJson, msg_receiver);
                            } catch (JSONException e) {
                                //TODO what to do here
//                                MeshLog.p("onPayForMessageOkReceived EX " + e.getMessage());
                            }
                        }
                    } else {
                        MeshLog.v("-- balance error! --");
                        syncWithBuyer(from);
//                        MeshLog.p("onPayForMessageOkReceived price is not correct");
                    }
                }
            } else {
                //TODO pick channel info, save purchase, then proceed.
            }
        } catch (Exception e) {
            e.printStackTrace();
            //TODO what to do here
//            MeshLog.p("onPayForMessageOkReceived Exception " + e.getMessage());
        }
    }

    @Override
    public void onPayForMessageErrorReceived(String from, String msg_id, String errorText) {
        MeshLog.v("onPayForMessageErrorReceived from buyer " + from + "  for message  " + msg_id + "  error  " + errorText);
        onPayMessageResponseReceived(from, msg_id);
        try {
            BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageById(msg_id);
            buyerPendingMessage.status = PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.SENT_NOT_PAID;
            databaseService.updateBuyerPendingMessage(buyerPendingMessage);

            if (buyerPendingMessage.isIncomming) {
                payController.getTransportManagerX().getRemoteTransport().onPaymentGotForIncomingMessage(false, buyerPendingMessage.owner, buyerPendingMessage.sender, buyerPendingMessage.msgId, buyerPendingMessage.msgData, buyerPendingMessage.isFile);
            } else {
                payController.getTransportManagerX().getRemoteTransport().onPaymentGotForOutgoingMessage(false, buyerPendingMessage.owner, buyerPendingMessage.sender, buyerPendingMessage.msgId, buyerPendingMessage.msgData, buyerPendingMessage.isFile);
            }

            payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(from);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSynBuyerOKReceive(String from, String sellerAddress, String bps, String trx_hash) {
        MeshLog.v("[Internet] buyer found in local");
        try {
            Purchase purchase = databaseService.getPurchaseByTrxHash(trx_hash);
            if (!TextUtils.isEmpty(bps) && !purchase.balanceProof.equalsIgnoreCase(bps)){
                purchase.balanceProof = bps;
                databaseService.updatePurchase(purchase);
            }

            if (DataPlanManager.getInstance().getDataAmountMode() == DataPlanConstants.DATA_MODE.UNLIMITED || DataPlanManager.getInstance().getRemainingData() > 0){
                payController.getTransportManagerX().getRemoteTransport().onBuyerConnected(from);
                resumeUserPendingMessage(from);
            } else {
                sendDisconnectedBySeller(from);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceivedEtherRequest(String from, int endpointType) {

        /*ethService.requestEther(from, endpointType, new EthereumService.ReqEther() {
            @Override
            public void onEtherRequested(int responseCode) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                    jsonObject.put(PurchaseConstants.JSON_KEYS.RESPONSE_CODE, responseCode);

                    payController.sendEtherRequestMessageResponse(jsonObject, from);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });*/
    }

    @Override
    public void onBuyerUpdateNotified(String msg_Id, String fromAddress) {
        MeshLog.v("Message Queuing 23");
        MeshLog.o("-- onBuyerUpdateNotified --");
        onPayMessageResponseReceived(fromAddress, msg_Id);
        try {
            BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageById(msg_Id);
            String msgData = buyerPendingMessage.msgData;

            buyerPendingMessage.status = PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.SENT_PAID;
            buyerPendingMessage.msgData = "";
            databaseService.updateBuyerPendingMessage(buyerPendingMessage);
            MeshLog.v("Message Queuing 24 " + msg_Id);

            if (buyerPendingMessage.isIncomming) {
                payController.getTransportManagerX().getRemoteTransport().onPaymentGotForIncomingMessage(true, buyerPendingMessage.owner, buyerPendingMessage.sender, buyerPendingMessage.msgId, msgData, buyerPendingMessage.isFile);
            } else {
                payController.getTransportManagerX().getRemoteTransport().onPaymentGotForOutgoingMessage(true, buyerPendingMessage.owner, buyerPendingMessage.sender, buyerPendingMessage.msgId, msgData, buyerPendingMessage.isFile);
            }

            checkMessageIsInProgressAndSend(fromAddress);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void requestForGiftEther(String fromAddress, int endPointType, String giftRequestData, String buyerPublicey) {
        MeshLog.v("giftEther requestForGiftEther");
        ethService.requestGiftEther(fromAddress, endPointType, giftRequestData, buyerPublicey, new EthereumService.GiftEther() {
            @Override
            public void onEtherGiftRequested(boolean success, String msg, String ethTX, String tknTx, String failedBy, double ethValue, double tknValue) {
                MeshLog.v("giftEther onEtherGiftRequested " + "success " + success + " msg " + msg +" ethTX " + ethTX + " tknTx " + tknTx);
                giftEtherRequestSubmitted(success, msg, ethTX, tknTx, endPointType, fromAddress, failedBy, ethValue, tknValue);
            }
        });
    }

    @Override
    public void requestForGiftEtherWithHash(String fromAddress, String ethTranxHash, String tknTranxHash, int endPointType, double ethValue, double tknValue) {
        MeshLog.v("giftEther requestForGiftEtherWithHash");
        ethService.getStatusOfGift(fromAddress, ethTranxHash, tknTranxHash, endPointType, ethValue, tknValue);
    }

    @Override
    public void timeoutCallback(TimeoutModel timeoutModel) {
        resumeUserPendingMessage(timeoutModel.getReceiverId());
    }

    @Override
    public void buyerInternetMessageReceived(String sender, String owner, String msg_id, String msgData, long dataSize, boolean isIncomming, boolean isFile) {
        MeshLog.v("FILE_SPEED_TEST_7 " + Calendar.getInstance().getTime());
        MeshLog.v("Message Queuing  1  " + msg_id);
        try {
            BuyerPendingMessage buyerPendingMessage = databaseService.getBuyerPendingMessageById(msg_id);
            if (buyerPendingMessage == null) {
                buyerPendingMessage = new BuyerPendingMessage();
                buyerPendingMessage.msgId = msg_id;
                buyerPendingMessage.msgData = msgData;
                buyerPendingMessage.owner = owner;
                buyerPendingMessage.sender = sender;
                buyerPendingMessage.dataSize = dataSize;
                buyerPendingMessage.status = PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.RECEIVED;
                buyerPendingMessage.isIncomming = isIncomming;
                buyerPendingMessage.isFile = isFile;
                databaseService.insertBuyerPendingMessage(buyerPendingMessage);
            } else {
                buyerPendingMessage.msgData = msgData;
                databaseService.updateBuyerPendingMessage(buyerPendingMessage);
            }

            if (buyerPendingMessage.status == PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.SENT_PAID){
                if (buyerPendingMessage.isIncomming) {
                    payController.getTransportManagerX().getRemoteTransport().onPaymentGotForIncomingMessage(true, buyerPendingMessage.owner, buyerPendingMessage.sender, buyerPendingMessage.msgId, buyerPendingMessage.msgData, buyerPendingMessage.isFile);

                } else {
                    payController.getTransportManagerX().getRemoteTransport().onPaymentGotForOutgoingMessage(true, buyerPendingMessage.owner, buyerPendingMessage.sender, buyerPendingMessage.msgId, buyerPendingMessage.msgData, buyerPendingMessage.isFile);
                }
            }

            if (buyerPendingMessage.status == PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.SENT_NOT_PAID){
                buyerPendingMessage.status = PurchaseConstants.BUYER_PENDING_MESSAGE_STATUS.RECEIVED;
                databaseService.updateBuyerPendingMessage(buyerPendingMessage);
            }

            if (buyerPendingMessage.isIncomming) {
                checkMessageIsInProgressAndSend(buyerPendingMessage.owner);
            } else {
                checkMessageIsInProgressAndSend(buyerPendingMessage.sender);
            }

        } catch (ExecutionException e) {

            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSyncBuyerToSellerReceived(String buyerAddress, String sellerAddress, long blockNumber, double usedDataAmount, double totalDataAmount, double balance, String bps, String chs, int endPointType, String trx_hash) {

        if (blockNumber > 0) {
            try {
                Purchase purchase = databaseService.getPurchaseByTrxHash(trx_hash);

                if (purchase == null) {
                    double deposit = totalDataAmount * PreferencesHelperDataplan.on().getPerMbTokenValue();
                    databaseService.insertPurchase(buyerAddress, sellerAddress, totalDataAmount,
                            usedDataAmount, blockNumber, deposit, bps, balance, chs, 0.0,
                            PurchaseConstants.CHANNEL_STATE.OPEN, endPointType, trx_hash);
                }

                syncWithBuyer(buyerAddress);

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
            syncWithBuyer(buyerAddress);
        }
    }

    @Override
    public void onChannelClosedByBuyer(String buyerAddress, long open_block, double withdrawnBalance) {
        try {
            Purchase purchase = databaseService.getPurchaseByBlockNumber(open_block, buyerAddress, ethService.getAddress());
            if (purchase != null){
                purchase.state = PurchaseConstants.CHANNEL_STATE.CLOSED;
                purchase.withdrawnBalance = withdrawnBalance;
                databaseService.updatePurchase(purchase);

                payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(buyerAddress);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBuyerChangedRole(String buyerId) {
        try {
            Purchase purchase = databaseService.getPurchaseByState(PurchaseConstants.CHANNEL_STATE.OPEN, buyerId, ethService.getAddress());
            if (purchase != null){
                payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(buyerId);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    //*********************************************************//
    //*****************EthereumServiceListener*****************//
    //*********************************************************//
    @Override
    public void onBalanceApprovedLog(TmeshToken.ApprovalEventResponse typedResponse) {
        MeshLog.v("onBalanceApprovedLog " + typedResponse._owner + " " + typedResponse._value.toString() + " data " + typedResponse.log.getData());
        String buyerAddress = typedResponse._owner;

        PurchaseRequests purchaseRequests = null;
        preferencesHelperDataplan.setBalanceApprovedBlock(typedResponse.log.getBlockNumber().longValue());

        try {

            purchaseRequests = databaseService.getRequestByTrxHash(typedResponse.log.getTransactionHash());
            if (purchaseRequests != null && purchaseRequests.state >= PurchaseConstants.REQUEST_STATE.COMPLETED) {
                return;
            }

            /*if (purchaseRequests == null) {
                double value = 0;
                if (typedResponse._value.compareTo(BigInteger.valueOf(0)) == 1) {
                    value = Convert.fromWei(new BigDecimal(typedResponse._value), Convert.Unit.ETHER).doubleValue();
                    purchaseRequests = databaseService.getPendingRequest(buyerAddress, value, PurchaseConstants.REQUEST_TYPES.APPROVE_TOKEN, PurchaseConstants.REQUEST_STATE.PENDING);
                } else {
                    purchaseRequests = databaseService.getPendingRequest(buyerAddress, value, PurchaseConstants.REQUEST_TYPES.APPROVE_ZERO, PurchaseConstants.REQUEST_STATE.PENDING);
                }
            }*/


            if (purchaseRequests != null) {
                purchaseRequests.state = PurchaseConstants.REQUEST_STATE.NOTIFIED;
                purchaseRequests.trxHash = typedResponse.log.getTransactionHash();
                purchaseRequests.trxBlock = typedResponse.log.getBlockNumber().longValue();

                databaseService.updatePurchaseRequest(purchaseRequests);

                pickAndSubmitRequest(purchaseRequests.requesterAddress);
            } else {
                MeshLog.v("onBalanceApprovedLog purchaseRequest not found");
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChannelCreatedLog(RaidenMicroTransferChannels.ChannelCreatedEventResponse typedResponse) {
        long blockNumber = typedResponse.log.getBlockNumber().longValue();
        MeshLog.v("onChannelCreatedLog " + typedResponse._sender_address + ", d: " + typedResponse._deposit.toString() + ", b: " + blockNumber);

        preferencesHelperDataplan.setChannelCreatedBlock(blockNumber);

        if (typedResponse._receiver_address.equalsIgnoreCase(ethService.getAddress())) {
            String buyerAddress = typedResponse._sender_address;

            double deposit = ethService.getETHorTOKEN(typedResponse._deposit);
            MeshLog.v("deposit " + deposit);
            try {
                PurchaseRequests purchaseRequests = databaseService.getRequestByTrxHash(typedResponse.log.getTransactionHash());
                if (purchaseRequests != null && purchaseRequests.state >= PurchaseConstants.REQUEST_STATE.COMPLETED) {

                    if (purchaseRequests.trxBlock != blockNumber){
                        long previousBlock = purchaseRequests.trxBlock;
                        purchaseRequests.trxBlock = blockNumber;
                        databaseService.updatePurchaseRequest(purchaseRequests);
                        Purchase purchase = databaseService.getPurchaseByBlockNumberAndState(previousBlock, PurchaseConstants.CHANNEL_STATE.OPEN, buyerAddress, ethService.getAddress());
                        if (purchase != null){
                            MeshLog.v("blockUpdatedSeller " + purchase.openBlockNumber + " new " + blockNumber);

                            purchase.openBlockNumber = blockNumber;
                            databaseService.updatePurchase(purchase);
                            syncWithBuyer(buyerAddress);
                        }
                    }
                    return;
                }

               /* if (purchaseRequests == null) {
                    purchaseRequests = databaseService.getPendingRequest(buyerAddress, deposit, PurchaseConstants.REQUEST_TYPES.CREATE_CHANNEL, PurchaseConstants.REQUEST_STATE.PENDING);
                }*/

                if (purchaseRequests != null) {
                    purchaseRequests.state = PurchaseConstants.REQUEST_STATE.COMPLETED;
                    purchaseRequests.trxHash = typedResponse.log.getTransactionHash();
                    purchaseRequests.trxBlock = blockNumber;

                    String closingHash = ethService.getClosingHash(buyerAddress, purchaseRequests.trxBlock, 0, purchaseRequests.blockChainEndpoint);
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                        jsonObject.put(PurchaseConstants.JSON_KEYS.OPEN_BLOCK, purchaseRequests.trxBlock);
                        jsonObject.put(PurchaseConstants.JSON_KEYS.DEPOSIT, deposit);
                        jsonObject.put(PurchaseConstants.JSON_KEYS.TRX_HASH, purchaseRequests.trxHash);
                        jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAGE_CHS, closingHash);


                    } catch (Exception e) {
                        MeshLog.v("Exception " + e.getMessage());
                    }
                    purchaseRequests.responseString = jsonObject.toString();
                    UUID messageId = UUID.randomUUID();
                    purchaseRequests.messageId = messageId.toString();

                    databaseService.updatePurchaseRequest(purchaseRequests);

                    double purchasedData = deposit / PreferencesHelperDataplan.on().getPerMbTokenValue();

                    databaseService.insertPurchase(buyerAddress, ethService.getAddress(), purchasedData, 0,
                            purchaseRequests.trxBlock, deposit, "", 0, closingHash, 0,
                            PurchaseConstants.CHANNEL_STATE.OPEN, purchaseRequests.blockChainEndpoint, purchaseRequests.trxHash);

                    pickAndSubmitRequest(purchaseRequests.requesterAddress);

                    setEndPointInfoInJson(jsonObject, purchaseRequests.blockChainEndpoint);

                    payController.sendCreateChannelOkay(jsonObject, purchaseRequests.buyerAddress, messageId.toString());


                } else {
                    MeshLog.v("onChannelCreatedLog purchaseRequest not found");
                }

            } catch (ExecutionException | InterruptedException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onChannelToppedUpLog(RaidenMicroTransferChannels.ChannelToppedUpEventResponse typedResponse) {
        MeshLog.v("onChannelToppedUpLog " + typedResponse._sender_address + " " + typedResponse._added_deposit.toString());

        preferencesHelperDataplan.setChannelTopupBlock(typedResponse.log.getBlockNumber().longValue());

        if (typedResponse._receiver_address.equalsIgnoreCase(ethService.getAddress())) {

            String buyerAddress = typedResponse._sender_address;

            double addedDeposit = ethService.getETHorTOKEN(typedResponse._added_deposit);
            MeshLog.v("addedDeposit " + addedDeposit);
            try {
                PurchaseRequests purchaseRequests = databaseService.getRequestByTrxHash(typedResponse.log.getTransactionHash());
                if (purchaseRequests != null && purchaseRequests.state >= PurchaseConstants.REQUEST_STATE.COMPLETED) {
                    return;
                }

                /*if (purchaseRequests == null) {
                    purchaseRequests = databaseService.getPendingRequest(buyerAddress, addedDeposit, PurchaseConstants.REQUEST_TYPES.TOPUP_CHANNEL, PurchaseConstants.REQUEST_STATE.PENDING);
                }*/

                if (purchaseRequests != null) {
                    purchaseRequests.state = PurchaseConstants.REQUEST_STATE.COMPLETED;
                    purchaseRequests.trxHash = typedResponse.log.getTransactionHash();
                    purchaseRequests.trxBlock = typedResponse.log.getBlockNumber().longValue();

                    Purchase topuppedPurchase = databaseService.getPurchaseByBlockNumber(typedResponse._open_block_number.longValue(), typedResponse._sender_address, typedResponse._receiver_address);

                    topuppedPurchase.deposit = topuppedPurchase.deposit + addedDeposit;
                    double totalData = topuppedPurchase.deposit / PreferencesHelperDataplan.on().getPerMbTokenValue();
                    topuppedPurchase.totalDataAmount = totalData;


                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                        jsonObject.put(PurchaseConstants.JSON_KEYS.OPEN_BLOCK, typedResponse._open_block_number.longValue());
                        jsonObject.put(PurchaseConstants.JSON_KEYS.DEPOSIT, topuppedPurchase.deposit);

                    } catch (Exception e) {
                        MeshLog.v("Exception " + e.getMessage());
                    }
                    purchaseRequests.responseString = jsonObject.toString();
                    UUID messageId = UUID.randomUUID();
                    purchaseRequests.messageId = messageId.toString();

                    databaseService.updatePurchaseRequest(purchaseRequests);

                    databaseService.updatePurchase(topuppedPurchase);

                    pickAndSubmitRequest(purchaseRequests.requesterAddress);
                    setEndPointInfoInJson(jsonObject, purchaseRequests.blockChainEndpoint);

                    payController.sendTopupChannelOkay(jsonObject, purchaseRequests.buyerAddress, messageId.toString());

                } else {
                    MeshLog.v("onChannelToppedUpLog purchaseRequest not found");
                }

            } catch (ExecutionException | InterruptedException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onChannelClosedLog(RaidenMicroTransferChannels.ChannelSettledEventResponse typedResponse) {
        MeshLog.v("onChannelClosedLog seller " + typedResponse._sender_address + " " + typedResponse._balance.toString());

        if (typedResponse._sender_address.equalsIgnoreCase(ethService.getAddress())){
            return;
        }

        preferencesHelperDataplan.setChannelClosedBlock(typedResponse.log.getBlockNumber().longValue());

        try {
            String buyerAddress = typedResponse._sender_address;
            double balance = ethService.getETHorTOKEN(typedResponse._balance);
            PurchaseRequests purchaseRequests = databaseService.getRequestByTrxHash(typedResponse.log.getTransactionHash());

            if (purchaseRequests != null && purchaseRequests.state >= PurchaseConstants.REQUEST_STATE.COMPLETED) {
                return;
            }

            /*if (purchaseRequests == null) {
                purchaseRequests = databaseService.getPendingRequest(buyerAddress, balance, PurchaseConstants.REQUEST_TYPES.CLOSE_CHANNEL, PurchaseConstants.REQUEST_STATE.PENDING);
            }*/

            if (purchaseRequests != null) {
                purchaseRequests.state = PurchaseConstants.REQUEST_STATE.COMPLETED;
                purchaseRequests.trxHash = typedResponse.log.getTransactionHash();
                purchaseRequests.trxBlock = typedResponse.log.getBlockNumber().longValue();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                jsonObject.put(PurchaseConstants.JSON_KEYS.OPEN_BLOCK, typedResponse._open_block_number.longValue());
                jsonObject.put(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS, typedResponse._receiver_address);

                purchaseRequests.responseString = jsonObject.toString();

                UUID messageId = UUID.randomUUID();
                purchaseRequests.messageId = messageId.toString();

                databaseService.updatePurchaseRequest(purchaseRequests);

                setEndPointInfoInJson(jsonObject, purchaseRequests.blockChainEndpoint);

                payController.sendChannelClosed(jsonObject, purchaseRequests.buyerAddress, messageId.toString());

                pickAndSubmitRequest(purchaseRequests.requesterAddress);

            } else {
                MeshLog.v("onChannelClosedLog seller purchaseRequest not found");
            }
            if (typedResponse._receiver_address.equalsIgnoreCase(ethService.getAddress())) {
                Purchase purchase = databaseService.getPurchaseByBlockNumber(typedResponse._open_block_number.longValue(), typedResponse._sender_address, typedResponse._receiver_address);
                if (purchase != null){
                    purchase.state = PurchaseConstants.CHANNEL_STATE.CLOSED;
                    purchase.withdrawnBalance = balance;
                    databaseService.updatePurchase(purchase);
                    payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(buyerAddress);
                }
            }

        } catch (ExecutionException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChannelWithdrawnLog(RaidenMicroTransferChannels.ChannelWithdrawEventResponse typedResponse) {
        MeshLog.v("onChannelWithdrawnLog " + typedResponse._sender_address + " " + typedResponse._withdrawn_balance.toString());

        preferencesHelperDataplan.setChannelWithdrawnBlock(typedResponse.log.getBlockNumber().longValue());

        if (typedResponse._receiver_address.equalsIgnoreCase(ethService.getAddress())) {

            try {
                String buyerAddress = typedResponse._sender_address;
                double withdrawnBalance = ethService.getETHorTOKEN(typedResponse._withdrawn_balance);
                PurchaseRequests purchaseRequests = databaseService.getRequestByTrxHash(typedResponse.log.getTransactionHash());

                if (purchaseRequests != null && purchaseRequests.state >= PurchaseConstants.REQUEST_STATE.COMPLETED) {
                    return;
                }

                /*if (purchaseRequests == null) {
                    purchaseRequests = databaseService.getPendingRequest(buyerAddress, withdrawnBalance, PurchaseConstants.REQUEST_TYPES.WITHDRAW_CHANNEL, PurchaseConstants.REQUEST_STATE.PENDING);
                }*/

                if (purchaseRequests != null) {
                    purchaseRequests.state = PurchaseConstants.REQUEST_STATE.NOTIFIED;
                    purchaseRequests.trxHash = typedResponse.log.getTransactionHash();
                    purchaseRequests.trxBlock = typedResponse.log.getBlockNumber().longValue();

                    databaseService.updatePurchaseRequest(purchaseRequests);


                    Purchase purchase = databaseService.getPurchaseByBlockNumber(typedResponse._open_block_number.longValue(), typedResponse._sender_address, typedResponse._receiver_address);
                    purchase.withdrawnBalance = withdrawnBalance;

                    databaseService.updatePurchase(purchase);

                    pickAndSubmitRequest(purchaseRequests.requesterAddress);

                } else {
                    MeshLog.v("onChannelCreatedLog purchaseRequest not found");
                }

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public void onTokenMintedLog(TmeshToken.MintedEventResponse typedResponse) {
        MeshLog.v("onTokenMinted " + typedResponse._to + " " + typedResponse._num.toString());

        preferencesHelperDataplan.setTokenMintedBlock(typedResponse.log.getBlockNumber().longValue());
        String tokenBuyerAddress = typedResponse._to;
        double value = ethService.getETHorTOKEN(typedResponse._num);

        try {
            PurchaseRequests purchaseRequests = databaseService.getRequestByTrxHash(typedResponse.log.getTransactionHash());

            if (purchaseRequests != null && purchaseRequests.state >= PurchaseConstants.REQUEST_STATE.COMPLETED) {
                return;
            }

            /*if (purchaseRequests == null) {
                purchaseRequests = databaseService.getPendingRequest(tokenBuyerAddress, value, PurchaseConstants.REQUEST_TYPES.BUY_TOKEN, PurchaseConstants.REQUEST_STATE.PENDING);
            }*/

            if (purchaseRequests != null) {

                purchaseRequests.trxHash = typedResponse.log.getTransactionHash();
                purchaseRequests.trxBlock = typedResponse.log.getBlockNumber().longValue();

                Double tokenValue = ethService.getUserTokenBalance(tokenBuyerAddress, purchaseRequests.blockChainEndpoint);
                Double etherValue = ethService.getUserEthBalance(tokenBuyerAddress, purchaseRequests.blockChainEndpoint);


                if (tokenBuyerAddress.equalsIgnoreCase(ethService.getAddress())) {

                    purchaseRequests.state = PurchaseConstants.REQUEST_STATE.NOTIFIED;
                    databaseService.updatePurchaseRequest(purchaseRequests);

                    if (tokenValue == null)
                        tokenValue = EthereumServiceUtil.getInstance(mContext).getToken(purchaseRequests.blockChainEndpoint);

                    if (etherValue == null)
                        etherValue = EthereumServiceUtil.getInstance(mContext).getCurrency(purchaseRequests.blockChainEndpoint);

                    EthereumServiceUtil.getInstance(mContext).updateCurrencyAndToken(purchaseRequests.blockChainEndpoint,etherValue, tokenValue);

                    if (tokenValue == null || etherValue == null) {

                        if (walletListener != null) {
                            walletListener.onTokenRequestResponse(true, "Congratulations, Your purchase has been completed, Please try to refresh after some time");
                        }
                    } else {

                        if (walletListener != null) {
                            walletListener.onTokenRequestResponse(false, "Congratulations, Token added to your account.");
                        }
                        EthereumServiceUtil.getInstance(mContext).updateToken(purchaseRequests.blockChainEndpoint, tokenValue);
                    }


                } else {
                    purchaseRequests.state = PurchaseConstants.REQUEST_STATE.COMPLETED;

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(PurchaseConstants.JSON_KEYS.MESSAME_FROM, ethService.getAddress());
                    if (tokenValue == null) {
                        tokenValue = typedResponse._num.doubleValue();
                    }
                    jsonObject.put(PurchaseConstants.INFO_KEYS.TKN_BALANCE, tokenValue);

                    if (etherValue != null){
                        jsonObject.put(PurchaseConstants.INFO_KEYS.ETH_BALANCE, etherValue);
                    }


                    purchaseRequests.responseString = jsonObject.toString();

                    UUID messageId = UUID.randomUUID();

                    purchaseRequests.messageId = messageId.toString();

                    databaseService.updatePurchaseRequest(purchaseRequests);

                    setEndPointInfoInJson(jsonObject, purchaseRequests.blockChainEndpoint);

                    payController.sendBuyTokenResponse(jsonObject, purchaseRequests.buyerAddress, messageId.toString());
                }

                pickAndSubmitRequest(purchaseRequests.requesterAddress);


            } else {
                MeshLog.v("onChannelCreatedLog purchaseRequest not found");
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTokenTransferredLog(TmeshToken.TransferEventResponse typedResponse) {
        MeshLog.v("onTokenTransferredLog " + typedResponse.log.getTransactionHash());

    }

    @Override
    public void onGiftCompleted(String address, int endpoint, boolean status, double ethValue, double tknValue) {

        MeshLog.v("giftEther onGiftCompleted address " + address + " endpoint " + endpoint +  " Status " + status);

        try{
            double ethBalance = ethService.getUserEthBalance(address, endpoint);
            double tknBalance = ethService.getUserTokenBalance(address, endpoint);

            if (address.equalsIgnoreCase(ethService.getAddress())){
                if (status) {

                    preferencesHelperDataplan.setRequestedForEther(PurchaseConstants.GIFT_REQUEST_STATE.GOT_GIFT_ETHER, endpoint);
                    preferencesHelperDataplan.setGiftEtherHash(null, endpoint);
                    preferencesHelperDataplan.setGiftTokenHash(null, endpoint);
                    databaseService.updateCurrencyAndToken(endpoint, ethBalance, tknBalance);

                    sendGiftListener(status, true, "Congratulations!!!\n"+tknValue+" Points have been added to your account.");

                    /*Activity currentActivity = MeshApp.getCurrentActivity();
                    if (currentActivity != null){
                        HandlerUtil.postForeground(() -> DialogUtil.showConfirmationDialog(currentActivity, "Gift Awarded!", "Congratulations!!!\nBalance has been added to your account.", null, "OK", null));
                    }else {
                        //TODO send notifications
                    }*/


                } else {
                    sendGiftListener(status, true, "Failed");
                    //TODO detect fail type
                    preferencesHelperDataplan.setRequestedForEther(PurchaseConstants.GIFT_REQUEST_STATE.NOT_REQUESTED_YET, endpoint);
                }
            }else {
                giftEtherResponse(status, ethBalance, tknBalance, endpoint, address, ethValue, tknValue);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public LiveData<Integer> getDifferentNetworkData(String myAddress, int endpoint) {
        try {
            return databaseService.getDifferentNetworkData(myAddress, endpoint);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    //*********************************************************//
    //**********************SELLER TIMER***********************//
    //*********************************************************//
    private Handler sellerHandler;
    private long sellerWaitingTime = 15 * 1000;
    private ConcurrentHashMap<String, TimeoutModel> sellerTimeoutObjMap = new ConcurrentHashMap<>();

    @SuppressLint("HandlerLeak")
    private void setRequestTimeout(String requestId, String receiverId, int purpose) {

        MeshLog.v("SellerTimer start " + requestId + " purpose " + purpose);

        TimeoutModel timeoutModel = sellerTimeoutObjMap.get(requestId);

        if (timeoutModel == null){
            MeshLog.v("SellerTimer timeout model null");
            int timeOutTrackingPoint = (int) System.currentTimeMillis();
            timeoutModel = new TimeoutModel()
                    .setTimeoutPointer(timeOutTrackingPoint)
                    .setPurpose(purpose)
                    .setReceiverId(receiverId)
                    .setCounter(1);

            if (sellerHandler == null){
                HandlerThread handlerThread = new HandlerThread("SellerHandlerThread");
                handlerThread.start();
                Looper looper = handlerThread.getLooper();
                sellerHandler = new Handler(looper) {
                    @Override
                    public void handleMessage(Message msg) {
                        MeshLog.v("SellerTimer timeout hit");

                        String requestId_ = (String) msg.obj;
                        TimeoutModel mapTimeoutModel = sellerTimeoutObjMap.get(requestId_);

                        if (mapTimeoutModel != null) {
                            MeshLog.v("SellerTimer model counter " + mapTimeoutModel.getCounter());
                            if (mapTimeoutModel.getCounter()<3){
                                timeoutCallback(mapTimeoutModel);
                            }else {
                                sellerTimeoutObjMap.remove(requestId_);
                            }
                        }
                    }
                };
            }

        } else {
          timeoutModel.setCounter(timeoutModel.getCounter()+1);
        }
        sellerTimeoutObjMap.put(requestId, timeoutModel);
        Message msg = sellerHandler.obtainMessage(timeoutModel.getTimeoutPointer(), requestId);
        sellerHandler.sendMessageDelayed(msg, sellerWaitingTime);
    }

    private void onPayMessageResponseReceived(String sender, String messageId) {

        MeshLog.v("onPayMessageResponseReceived s " + sender + " i " + messageId);
        if (!TextUtils.isEmpty(messageId)) {

            if (sellerTimeoutObjMap.containsKey(messageId)) {

                TimeoutModel timeoutModel = sellerTimeoutObjMap.get(messageId);

                if (timeoutModel != null) {

                    int timeOutTrackingPoint = timeoutModel.getTimeoutPointer();
                    if (sellerHandler != null && sellerHandler.hasMessages(timeOutTrackingPoint)) {
                        MeshLog.v("SellerTimer gotResponse " + messageId + " purpose " + timeoutModel.getPurpose());
                        sellerHandler.removeMessages(timeOutTrackingPoint);
                        sellerTimeoutObjMap.remove(messageId);
                    }
                }
            }
        }
    }

    public void resumeMessaging() {
        try {
            List<Purchase> purchases = databaseService.getAllActiveChannel(ethService.getAddress(), PurchaseConstants.CHANNEL_STATE.OPEN);
            for (Purchase p : purchases){
                if (payController.getTransportManagerX().isUserConnected(p.buyerAddress)){
                    checkMessageIsInProgressAndSend(p.buyerAddress);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }


}
