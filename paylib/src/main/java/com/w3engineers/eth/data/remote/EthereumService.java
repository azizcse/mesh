package com.w3engineers.eth.data.remote;

import android.content.Context;
import android.net.Network;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.w3engineers.eth.contracts.RaidenMicroTransferChannels;
import com.w3engineers.eth.contracts.TmeshToken;
import com.w3engineers.eth.data.helper.PreferencesHelperPaylib;
import com.w3engineers.eth.data.helper.model.EthGift;
import com.w3engineers.eth.data.helper.model.PayLibNetworkInfo;
import com.w3engineers.eth.data.remote.parse.ParseManager;
import com.w3engineers.eth.util.data.NetworkMonitor;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EthereumService implements BlockRequest.BlockTransactionObserver, EthGift.EthGiftListener {

    private static EthereumService instance;
    private Context mContext;
    private Executor executor;
    private Credentials credentials;

    private String TAG = "EthereumService";

    private TransactionObserver transactionObserver;
    private TransactionObserverBuyer transactionObserverBuyer;
    private Network network;
    private HashMap<Integer, BlockRequest> blockRequests = null;
    EthGift ethGift;
    private String giftDonateUrl;
    private String giftDonateUser;
    private String giftDonatePass;
    private String giftDonatePublicKey;
    private ParseManager parseManager;
    private NetworkInfoCallback networkInfoCallback;
//    private boolean usingAdhocInternet;

    private EthereumService(Context context, NetworkInfoCallback networkInfoCallback, String giftDonateUrl, String giftDonateUser, String getGiftDonatePass, String giftPublicKey) {


        Log.v("EthereumService Auth", giftDonateUrl + " " + giftDonateUser + " " + getGiftDonatePass + " " + giftPublicKey);

        mContext = context.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
        this.giftDonateUrl = giftDonateUrl;
        this.giftDonateUser = giftDonateUser;
        this.giftDonatePass = getGiftDonatePass;
        this.giftDonatePublicKey = giftPublicKey;

        if (networkInfoCallback == null) {
            throw new NullPointerException("NetworkInfoCallback shouldn't null");
        }

        this.networkInfoCallback = networkInfoCallback;

        if (blockRequests == null) {
            blockRequests = new HashMap<>();

            List<PayLibNetworkInfo> payLibNetworkInfos = networkInfoCallback.getNetworkInfo();

            if (payLibNetworkInfos == null) return;

            for (PayLibNetworkInfo payLibNetworkInfo : payLibNetworkInfos) {
                BlockRequest blockRequestETH = new BlockRequest(payLibNetworkInfo.tokenAddress,
                        payLibNetworkInfo.channelAddress,
                        payLibNetworkInfo.networkUrl, mContext,
                        payLibNetworkInfo.gasPrice, payLibNetworkInfo.gasLimit, EthereumService.this);

                blockRequests.put(payLibNetworkInfo.networkType, blockRequestETH);
            }
            Log.v("BlockRequest", "1 " + blockRequests.size());
            ethGift = EthGift.on(blockRequests, EthereumService.this);
        }

        /*this.usingAdhocInternet = isAdhohcConnected;
        if (isAdhohcConnected){
            network = WiFiDataNetworkUtil.getConnectedWiFiNetwork(context);
            if (network != null){
                for (BlockRequest value : blockRequests.values()) {
                    value.setNetworkInterface(network);
                }
            }
        } else {
            CellularDataNetworkUtil.on(mContext, new CellularDataNetworkUtil.CellularDataNetworkListenerForPurchase() {
                @Override
                public void onAvailable(Network network1) {
                    if (!usingAdhocInternet){
                        network = network1;
                        Log.i(TAG, "onAvailable: " + network.toString());

                        for (BlockRequest value : blockRequests.values()) {
                            value.setNetworkInterface(network);
                        }
                    }
                }

                @Override
                public void onLost() {
                    if (!usingAdhocInternet) {
                        network = null;
                    }
                }
            }).initMobileDataNetworkRequest();
        }*/
    }

    public void changeNetworkInterface(Network network_) {
        Log.i(TAG, "network changed: " + network);

        this.network = network_;
        Log.i(TAG, "networktype: " + network);

        if (this.network != null) {
            for (BlockRequest value : blockRequests.values()) {
                value.setNetworkInterface(network);
            }
            Log.i(TAG, "onAvailable: " + network.toString());
        }
    }

    public void setGIftDonateUrl(String giftUrl, String giftUser, String giftPass, String giftPublicKey) {
        this.giftDonateUrl = giftUrl;
        this.giftDonateUser = giftUser;
        this.giftDonatePass = giftPass;
        this.giftDonatePublicKey = giftPublicKey;

        Log.v("setGIftDonateUrl Auth", giftDonateUrl + " " + giftDonateUser + " " + giftDonatePass + " " + giftPublicKey);


        blockRequests = new HashMap<>();

        //As gift donate url has been changed, there is a possibility that networks info have also been changed. So we need to reinitialize them again.

        List<PayLibNetworkInfo> payLibNetworkInfos = networkInfoCallback.getNetworkInfo();

        for (PayLibNetworkInfo payLibNetworkInfo : payLibNetworkInfos) {
            BlockRequest blockRequestETH = new BlockRequest(payLibNetworkInfo.tokenAddress,
                    payLibNetworkInfo.channelAddress,
                    payLibNetworkInfo.networkUrl, mContext,
                    payLibNetworkInfo.gasPrice, payLibNetworkInfo.gasLimit, EthereumService.this);

            blockRequests.put(payLibNetworkInfo.networkType, blockRequestETH);
        }

        Log.v("BlockRequest", "2 " + blockRequests.size());
        ethGift = EthGift.on(blockRequests, EthereumService.this);


        if (network == null) {

            network = NetworkMonitor.getConnectedMobileNetwork(this.mContext);
//            Log.i(TAG, "onAvailable: " + network.toString());


            if (network != null){
                for (BlockRequest value : blockRequests.values()) {
                    value.setNetworkInterface(network);
                }
            }

            /*CellularDataNetworkUtil.on(mContext, new CellularDataNetworkUtil.CellularDataNetworkListenerForPurchase() {
                @Override
                public void onAvailable(Network network1) {
                    network = network1;
                    Log.i(TAG, "onAvailable: " + network.toString());

                    for (BlockRequest value : blockRequests.values()) {
                        value.setNetworkInterface(network);
                    }
                }

                @Override
                public void onLost() {
                    network = null;
                }
            }).initMobileDataNetworkRequest();*/
        } else {
            for (BlockRequest value : blockRequests.values()) {
                value.setNetworkInterface(network);
            }
        }


        if (credentials != null) {
            setCredential(credentials);
        }


    }

    @Override
    public void onRequestCompleted(String address, int endpoint, boolean status, TransactionReceipt ethTxReceipt, TransactionReceipt tknTxReceipt, double ethValue, double tknValue) {
        if (transactionObserver != null)
            transactionObserver.onGiftCompleted(address, endpoint, status, ethValue, tknValue);

        if (transactionObserverBuyer != null) {
            transactionObserverBuyer.onGiftCompleted(address, endpoint, status, ethValue, tknValue);
        }

        if (status && ethTxReceipt != null) {

            if (parseManager != null) {
                JSONObject log = new JSONObject();
                try {
                    log.put("address", ethTxReceipt.getFrom());
                    log.put("blockHash", ethTxReceipt.getBlockHash());
                    log.put("blockNumber", ethTxReceipt.getBlockNumber());
                    log.put("data", ethTxReceipt.getLogs());
                    log.put("transactionHash", ethTxReceipt.getTransactionHash());
                    log.put("transactionIndex", ethTxReceipt.getTransactionIndex());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //todo majorarif set real value
                parseManager.sendEtherGifted(ethTxReceipt.getTransactionHash(), ethTxReceipt.getFrom(), ethTxReceipt.getTo(), getWeiValue(ethValue).toString(), log.toString());
            }
        }

        if (status && tknTxReceipt != null) {
            if (parseManager != null) {
                JSONObject log = new JSONObject();
                try {
                    log.put("address", tknTxReceipt.getFrom());
                    log.put("blockHash", tknTxReceipt.getBlockHash());
                    log.put("blockNumber", tknTxReceipt.getBlockNumber());
                    log.put("data", tknTxReceipt.getLogs());
                    log.put("transactionHash", tknTxReceipt.getTransactionHash());
                    log.put("transactionIndex", tknTxReceipt.getTransactionIndex());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //todo majorarif set real value
                parseManager.sendTokenGifted(tknTxReceipt.getTransactionHash(), tknTxReceipt.getFrom(), tknTxReceipt.getTo(), getWeiValue(tknValue).toString(), log.toString());
            }
        }
    }

    public void setParseInfo(String parseUrl, String parseAppId) {
        if (parseManager == null) {
            parseManager = ParseManager.on(parseUrl, parseAppId, mContext);
        }
    }

    public interface NetworkInfoCallback {
        List<PayLibNetworkInfo> getNetworkInfo();
    }

    synchronized public static EthereumService getInstance(Context context, NetworkInfoCallback networkInfoCallback, String giftDonateUrl, String giftDonateUser, String giftDonatePass, String giftPublicKey) {
        if (instance == null) {
            synchronized (EthereumService.class) {
                if (instance == null) {
                    instance = new EthereumService(context, networkInfoCallback, giftDonateUrl, giftDonateUser, giftDonatePass, giftPublicKey);
                }
            }
        }
        return instance;
    }

    public String getGiftDonatePublicKey() {
        return this.giftDonatePublicKey;
    }

    public void setCredential(Credentials credential) {
        this.credentials = credential;
//        Log.i(TAG, "privatekey: " + credential.getEcKeyPair().getPrivateKey().toString(16));
//        Log.i(TAG, "address: " + credential.getAddress());
        saveAddress(this.credentials.getAddress());
        for (BlockRequest value : blockRequests.values()) {
            value.setCredential(credential);
        }
    }

    public void saveAddress(String address) {
        PreferencesHelperPaylib.onInstance(mContext).saveAddress(address);
    }

    public String getAddress() {
        return PreferencesHelperPaylib.onInstance(mContext).getAddress();
    }

    public interface ReqEther {
        void onEtherRequested(int responseCode);
    }

    /*public void requestEther(String address, int endPointType, final ReqEther listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {

                OkHttpClient client = new OkHttpClient();

                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                RequestBody body = RequestBody.create(mediaType, "address="+address+"&endpoint="+endPointType);
                Request request = new Request.Builder()
                        .url(giftDonateUrl + "eth")
                        .post(body)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("Postman-Token", "cd5a9cac-258a-4ef9-8bb1-e20201a24e3a")
                        .build();

                *//*OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(faucetDonateUrl + address)
                        .get()
                        .addHeader("cache-control", "no-cache")
                        .build();*//*

                try {
                    Response response = client.newCall(request).execute();
                    if (response != null) {
                        listener.onEtherRequested(response.code());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    listener.onEtherRequested(400);
                }
            }
        });
    }*/


    public interface GiftEther {
        void onEtherGiftRequested(boolean success, String msg, String ethTX, String tknTx, String failedBy, double ethValue, double tikenValue);
    }

    public void requestGiftEther(String address, int endPointType, String encodedData, String userPublicKey, final GiftEther listener) {
        if (network == null) {
            listener.onEtherGiftRequested(false, "network error", null, null, "system", 0, 0);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        Double balance = blockRequests.get(endPointType).getUserEthBalance(address);
                        Integer nonce = blockRequests.get(endPointType).getUserNonce(address);

                        if (balance != null && balance > 0) {
                            listener.onEtherGiftRequested(false, "already have balance", null, null, "admin", 0, 0);
                        } else if (nonce != null && nonce > 0) {
                            listener.onEtherGiftRequested(false, "already have transactions", null, null, "admin", 0, 0);
                        } else if (TextUtils.isEmpty(giftDonateUrl)) {
                            listener.onEtherGiftRequested(false, "configuration error, please try again later", null, null, "system", 0, 0);
                        } else {

                            String authString = giftDonateUser + ":" + giftDonatePass;
                            String encodedAuth = Base64.encodeToString(authString.getBytes(UTF_8), Base64.NO_WRAP);
                            Log.v("Auth", encodedAuth);

                            OkHttpClient client = new OkHttpClient.Builder().socketFactory(network.getSocketFactory()).build();
                            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                            RequestBody body = RequestBody.create(mediaType, "data=" + encodedData + "&public=" + userPublicKey);

                            Request request = new Request.Builder()
                                    .url(giftDonateUrl)
                                    .post(body)
                                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                                    .addHeader("cache-control", "no-cache")
                                    .addHeader("Authorization", "Basic " + encodedAuth)
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (response != null) {
                                if (response.code() == 200) {
                                    String jsonData = response.body().string();
                                    JSONObject Jobject = new JSONObject(jsonData);
                                    if (Jobject.getBoolean("success")) {
                                        JSONObject result = Jobject.getJSONObject("data");
                                        String ethTx = result.getString("ethTX");
                                        String tknTx = result.getString("tokenTx");
                                        double ethValue = result.optDouble("ethValue");
                                        double tknValue = result.optDouble("tokenValue");
                                        listener.onEtherGiftRequested(true, null, ethTx, tknTx, null, ethValue, tknValue);
                                        ethGift.add(address, ethTx, tknTx, endPointType, ethValue, tknValue);
                                    } else {
                                        listener.onEtherGiftRequested(false, Jobject.getString("data"), null, null, Jobject.getString("failedby"), 0, 0);
                                    }
                                } else {
                                    listener.onEtherGiftRequested(false, "network error", null, null, "system", 0, 0);
                                }
                            } else {
                                listener.onEtherGiftRequested(false, "network error", null, null, "system", 0, 0);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        listener.onEtherGiftRequested(false, e.getMessage(), null, null, "system", 0, 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onEtherGiftRequested(false, e.getMessage(), null, null, "system", 0, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        listener.onEtherGiftRequested(false, e.getMessage(), null, null, "system", 0, 0);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        listener.onEtherGiftRequested(false, e.getMessage(), null, null, "system", 0, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onEtherGiftRequested(false, e.getMessage(), null, null, "system", 0, 0);
                    }
                }
            });
        }
    }

    /*public double getMyTokenBalance() {
        double tokenValue = PreferencesHelperPaylib.onInstance(mContext).getTokenBalance();
        return tokenValue;
    }*/

    /*public void setMyTokenBalance(double value) {
        PreferencesHelperPaylib.onInstance(mContext).setTokenBalance(value);
    }*/

    public Double getUserTokenAllowance(final String owner, int endpointType) throws ExecutionException, InterruptedException {
        if (network == null)
            return null;

        return blockRequests.get(endpointType).getUserTokenAllowance(owner);
    }

    public Tuple2<Double, Double> getChannelInfo(final String sender, final String receiver, final long blockNumber, int endpointType) throws ExecutionException, InterruptedException {

        if (network == null)
            return null;

        return blockRequests.get(endpointType).getChannelInfo(sender, receiver, blockNumber);
    }

    public String getBalanceProof(String receiver, long blockNumber, double balance, int endpointType) {
        return blockRequests.get(endpointType).getBalanceProof(receiver, blockNumber, balance);
    }

    public String verifyBalanceProofSignature(String receiver, long blockNumber, double balance, String balanceProof, int endpointType) throws ExecutionException, InterruptedException {

        if (network == null)
            return null;

        return blockRequests.get(endpointType).verifyBalanceProofSignature(receiver, blockNumber, balance, balanceProof);
    }

    public String getClosingHash(String sender, long blockNumber, double balance, int endpointType) {
        return blockRequests.get(endpointType).getClosingHash(sender, blockNumber, balance);
    }

    public String getOwner() {
        return this.credentials.getAddress();
    }

//    public double getMyEthBalance() {
//        double ethValue = PreferencesHelperPaylib.onInstance(mContext).getEtherBalance();
//
//        return ethValue;
//    }

    /*public void setMyEthBalance(double value) {
        PreferencesHelperPaylib.onInstance(mContext).setEtherBalance(value);
    }*/

    public Double getUserTokenBalance(String address, int endpointType) throws ExecutionException, InterruptedException {
        if (network == null)
            return null;

        return blockRequests.get(endpointType).getUserTokenBalance(address);
    }

    public Double getUserEthBalance(String address, int endpointType) throws ExecutionException, InterruptedException {
        if (network == null)
            return null;

        return blockRequests.get(endpointType).getUserEthBalance(address);
    }

    public Integer getUserNonce(String address, int endpointType) throws ExecutionException, InterruptedException {
        if (network == null)
            return null;

        return blockRequests.get(endpointType).getUserNonce(address);
    }

    public String approve(double value, int nonce, int endpointType) throws Exception {
        return blockRequests.get(endpointType).approve(value, nonce);
    }

    public String createChannel(String sellerAddress, double deposit, int nonce, int endpointType) throws Exception {
        return blockRequests.get(endpointType).createChannel(sellerAddress, deposit, nonce);
    }

    public String topup(String receiver, long blockNumber, double added_deposit, int nonce, int endpointType) throws Exception {
        return blockRequests.get(endpointType).topup(receiver, blockNumber, added_deposit, nonce);
    }

    public String close(String receiver, long block_number, double balance, String balance_signature, String closing_signature, int nonce, int endpointType) throws Exception {
        return blockRequests.get(endpointType).close(receiver, block_number, balance, balance_signature, closing_signature, nonce);
    }

    public String withdraw(long block_number, double balance, String balance_signature, int nonce, int endpointType) throws Exception {
        return blockRequests.get(endpointType).withdraw(block_number, balance, balance_signature, nonce);
    }

    public String buyToken(double value, int nonce, int endpointType) throws Exception {
        return blockRequests.get(endpointType).buyToken(value, nonce);
    }

    public TransactionReceipt getTransactionReceipt(String hash, int endpointType) throws Exception {
        if (network == null)
            return null;

        return blockRequests.get(endpointType).getTransactionReceiptByHash(hash);
    }

    public interface SubmitRequestListener {
        void onRequestSubmitted(String hash, int forRId);

        void onRequestSubmitError(String msg, int forRId);
    }

    public void submitRequest(String hexValue, int forRId, int endpointType, SubmitRequestListener listener) {
        Log.i(TAG, "submitRequest: " + hexValue);
        if (network != null) {

            blockRequests.get(endpointType).submitRequest(hexValue, forRId, new BlockRequest.SubmitRequestListener() {
                @Override
                public void onRequestSubmitted(String hash, int forRId) {
                    listener.onRequestSubmitted(hash, forRId);
                }

                @Override
                public void onRequestSubmitError(String msg, int forRId) {
                    listener.onRequestSubmitError(msg, forRId);
                }
            });
        } else {
            listener.onRequestSubmitError("network configuration error.", forRId);
        }
    }

    public interface TransactionObserver {
        void onBalanceApprovedLog(TmeshToken.ApprovalEventResponse typedResponse);

        void onChannelCreatedLog(RaidenMicroTransferChannels.ChannelCreatedEventResponse typedResponse);

        void onChannelToppedUpLog(RaidenMicroTransferChannels.ChannelToppedUpEventResponse typedResponse);

        void onChannelClosedLog(RaidenMicroTransferChannels.ChannelSettledEventResponse typedResponse);

        void onChannelWithdrawnLog(RaidenMicroTransferChannels.ChannelWithdrawEventResponse typedResponse);

        void onTokenMintedLog(TmeshToken.MintedEventResponse typedResponse);

        void onTokenTransferredLog(TmeshToken.TransferEventResponse typedResponse);

        void onGiftCompleted(String address, int endpoint, boolean Status, double ethValue, double tknValue);
    }

    public interface TransactionObserverBuyer {
        void onGiftCompleted(String address, int endpoint, boolean Status, double ethValue, double tknValue);

        void onChannelClosedLog(RaidenMicroTransferChannels.ChannelSettledEventResponse typedResponse);
    }

    public void setTransactionObserver(TransactionObserver transactionObserver) {
        this.transactionObserver = transactionObserver;
    }

    public void setTransactionObserverBuyer(TransactionObserverBuyer transactionObserverBuyer) {
        this.transactionObserverBuyer = transactionObserverBuyer;
    }

    public void logBalanceApproved(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logBalanceApproved(blockNumber);
    }

    public void logTokenMinted(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logTokenMinted(blockNumber);
    }

    public void logChannelCreated(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logChannelCreated(blockNumber);
    }

    public void logChannelToppedUp(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logChannelToppedUp(blockNumber);
    }

    public void logChannelClosed(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logChannelClosed(blockNumber);
    }

    public void logChannelWithdrawn(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logChannelWithdrawn(blockNumber);
    }

    public void logTokenTransferred(long blockNumber, int endpointType) {
        blockRequests.get(endpointType).logTokenTransferred(blockNumber);
    }

    @Override
    public void onBalanceApprovedLog(TmeshToken.ApprovalEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onBalanceApprovedLog(typedResponse);

        /*if (parseManager != null){
            String log = new Gson().toJson(typedResponse.log);
            parseManager.sendBalanceApprovedLog(typedResponse.log.getTransactionHash(), typedResponse._owner, typedResponse._spender, typedResponse._value.toString(), log);
        }*/
    }

    @Override
    public void onChannelCreatedLog(RaidenMicroTransferChannels.ChannelCreatedEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onChannelCreatedLog(typedResponse);

        if (parseManager != null && typedResponse._receiver_address.equalsIgnoreCase(getAddress())) {
            String log = new Gson().toJson(typedResponse.log);
            parseManager.sendChannelCreatedLog(typedResponse.log.getTransactionHash(), typedResponse._sender_address, typedResponse._receiver_address, typedResponse._deposit.toString(), log);
        }
    }

    @Override
    public void onChannelToppedUpLog(RaidenMicroTransferChannels.ChannelToppedUpEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onChannelToppedUpLog(typedResponse);

        if (parseManager != null && typedResponse._receiver_address.equalsIgnoreCase(getAddress())) {
            String log = new Gson().toJson(typedResponse.log);
            parseManager.sendChannelToppedUpLog(typedResponse.log.getTransactionHash(), typedResponse._sender_address, typedResponse._receiver_address, typedResponse._open_block_number.toString(), typedResponse._added_deposit.toString(), log);
        }
    }

    @Override
    public void onChannelClosedLog(RaidenMicroTransferChannels.ChannelSettledEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onChannelClosedLog(typedResponse);

        if (transactionObserverBuyer != null)
            transactionObserverBuyer.onChannelClosedLog(typedResponse);

        if (parseManager != null) {
            String log = new Gson().toJson(typedResponse.log);
            parseManager.sendChannelClosedLog(typedResponse.log.getTransactionHash(), typedResponse._sender_address, typedResponse._receiver_address, typedResponse._open_block_number.toString(), typedResponse._balance.toString(), typedResponse._receiver_tokens.toString(), log);
        }
    }

    @Override
    public void onChannelWithdrawnLog(RaidenMicroTransferChannels.ChannelWithdrawEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onChannelWithdrawnLog(typedResponse);

        if (parseManager != null && typedResponse._receiver_address.equalsIgnoreCase(getAddress())) {
            String log = new Gson().toJson(typedResponse.log);
            parseManager.sendChannelWithdrawnLog(typedResponse.log.getTransactionHash(), typedResponse._sender_address, typedResponse._receiver_address, typedResponse._open_block_number.toString(), typedResponse._withdrawn_balance.toString(), log);
        }
    }

    @Override
    public void onTokenMintedLog(TmeshToken.MintedEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onTokenMintedLog(typedResponse);


        /*if (parseManager != null){
            String log = new Gson().toJson(typedResponse.log);
            parseManager.sendTokenMinteLog(typedResponse.log.getTransactionHash(), typedResponse._to, typedResponse._num.toString(), log);
        }*/
    }

    @Override
    public void onTokenTransferredLog(TmeshToken.TransferEventResponse typedResponse) {
        if (transactionObserver != null)
            transactionObserver.onTokenTransferredLog(typedResponse);
    }

    public BigInteger getWeiValue(double value) {
        BigDecimal weiTokenValue = Convert.toWei(BigDecimal.valueOf(value), Convert.Unit.ETHER);
        BigInteger b = weiTokenValue.toBigInteger();
        Log.i(TAG, "weiTokenValue: " + b.toString());
        return b;
    }

    public double getETHorTOKEN(BigInteger value) {
        BigDecimal tokenValue = Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER);
        Log.i(TAG, "tokenValue: " + tokenValue.doubleValue());
        return tokenValue.doubleValue();
    }

    public void getStatusOfGift(String fromAddress, String ethTranxHash, String tknTranxHash, int endPointType, double ethValue, double tknValue) {
        ethGift.add(fromAddress, ethTranxHash, tknTranxHash, endPointType, ethValue, tknValue);
    }

}
