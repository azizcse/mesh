package com.w3engineers.eth.data.remote.parse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.w3engineers.eth.data.helper.callback.AppUpdateAppInfoUploadCallback;

import java.math.BigInteger;
import java.util.List;


public class ParseManager {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private static final Object mutex = new Object();
    private static ParseManager sInstance;

    private ParseManager(String parseUrl, String parseAppId, Context context) {
        mContext = context;
        Parse.initialize(new Parse.Configuration.Builder(mContext)
                .applicationId(parseAppId)
                .clientKey("")
                .server(parseUrl)
                .enableLocalDataStore()
                .build()
        );
    }

    public static ParseManager on(String parseUrl, String parseAppId, Context context) {
        synchronized (mutex) {
            if (sInstance == null) sInstance = new ParseManager(parseUrl, parseAppId, context);
        }
        return sInstance;
    }

    public static ParseManager getInstance() {
        return sInstance;
    }

    public void sendAppShareCount() {
//        ParseObject object = new ParseMapper().AppShareCountToParse(model);
//        object.getUpdatedAt();
//        object.saveEventually(e -> sendResponse(e == null));
    }

    public void sendTransaction() {

       /* ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, "XYZ");

        parseObject.saveInBackground(e -> {
            if (e == null) {
//                MeshLog.v("data saved");
            } else {
//                MeshLog.v(e.getMessage());
            }
        });*/
    }

    private void saveObject(ParseObject object, String tx_hash) {

        ParseQuery<ParseObject> parseQuery = ParseQuery.getQuery(ParseConstant.Transaction.TABLE);

        parseQuery.whereEqualTo(ParseConstant.Transaction.TX_HASH, tx_hash);

        parseQuery.findInBackground((objects, e) -> {
            if (e == null) {
                if (objects == null || objects.isEmpty()) {
                    object.saveInBackground(e1 -> {
                        if (e1 == null) {
                            Log.v("parseserver", "saved");
                        } else {
                            Log.v("parseserver", e1.getMessage());
                        }
                    });
                } else {
                    Log.v("parseserver", "transaction already exist");
                }
            } else {
                Log.v("parseserver", "Feed back query Error: " + e.getMessage());
            }
        });
    }

   /* public void sendBalanceApprovedLog(String tx_hash, String owner, String spender, String value, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.APPROVE_TOKEN);


        parseObject.put(ParseConstant.BALANCE_APPROVE.OWNER, owner);
        parseObject.put(ParseConstant.BALANCE_APPROVE.SPENDER, spender);
        parseObject.put(ParseConstant.BALANCE_APPROVE.VALUE, value);

        saveObject(parseObject, tx_hash);
    }*/

    public void sendChannelCreatedLog(String tx_hash, String sender, String receiver, String deposit, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.CREATE_CHANNEL);

        parseObject.put(ParseConstant.CHANNEL_CREATE.SENDER_ADDRESS, sender);
        parseObject.put(ParseConstant.CHANNEL_CREATE.RECEIVER_ADDRESS, receiver);
        parseObject.put(ParseConstant.CHANNEL_CREATE.DEPOSIT, deposit);

        saveObject(parseObject, tx_hash);
    }

    public void sendChannelToppedUpLog(String tx_hash, String sender, String receiver, String openBlockNumber, String addedDeposit, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.TOPUP_CHANNEL);

        parseObject.put(ParseConstant.CHANNEL_TOPUP.SENDER_ADDRESS, sender);
        parseObject.put(ParseConstant.CHANNEL_TOPUP.RECEIVER_ADDRESS, receiver);
        parseObject.put(ParseConstant.CHANNEL_TOPUP.OPEN_BLOCH_NUMBER, openBlockNumber);
        parseObject.put(ParseConstant.CHANNEL_TOPUP.ADDED_DEPOSIT, addedDeposit);

        saveObject(parseObject, tx_hash);
    }

    public void sendChannelClosedLog(String tx_hash, String sender, String receiver, String openBlockNumber, String balance, String receiverToken, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.CLOSE_CHANNEL);

        parseObject.put(ParseConstant.CHANNEL_CLOSE.SENDER_ADDRESS, sender);
        parseObject.put(ParseConstant.CHANNEL_CLOSE.RECEIVER_ADDRESS, receiver);
        parseObject.put(ParseConstant.CHANNEL_CLOSE.OPEN_BLOCH_NUMBER, openBlockNumber);
        parseObject.put(ParseConstant.CHANNEL_CLOSE.BALANCE, balance);
        parseObject.put(ParseConstant.CHANNEL_CLOSE.RECEIVER_TOKENS, receiverToken);

        saveObject(parseObject, tx_hash);
    }

    public void sendChannelWithdrawnLog(String tx_hash, String sender, String receiver, String openBlockNumber, String withdrawnBalance, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.WITHDRAW_CHANNEL);

        parseObject.put(ParseConstant.CHANNEL_WITHDRAW.SENDER_ADDRESS, sender);
        parseObject.put(ParseConstant.CHANNEL_WITHDRAW.RECEIVER_ADDRESS, receiver);
        parseObject.put(ParseConstant.CHANNEL_WITHDRAW.OPEN_BLOCH_NUMBER, openBlockNumber);
        parseObject.put(ParseConstant.CHANNEL_WITHDRAW.WITHDRAWN_BALANCE, withdrawnBalance);

        saveObject(parseObject, tx_hash);
    }


    public void sendEtherGifted(String tx_hash, String sender, String receiver, String value, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.ETHER_GIFTED);

        parseObject.put(ParseConstant.ETH_GIFT.SENDER_ADDRESS, sender);
        parseObject.put(ParseConstant.ETH_GIFT.RECEIVER_ADDRESS, receiver);
        parseObject.put(ParseConstant.ETH_GIFT.VALUE, value);

        saveObject(parseObject, tx_hash);
    }

    public void sendTokenGifted(String tx_hash, String sender, String receiver, String value, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);
        parseObject.put(ParseConstant.Transaction.PURPOSE, ParseConstant.REQUEST_TYPES.TOKEN_GIFTED);

        parseObject.put(ParseConstant.ETH_GIFT.SENDER_ADDRESS, sender);
        parseObject.put(ParseConstant.ETH_GIFT.RECEIVER_ADDRESS, receiver);
        parseObject.put(ParseConstant.ETH_GIFT.VALUE, value);

        saveObject(parseObject, tx_hash);
    }

    public void sendAppUpdateAppInfo(int storeId, AppUpdateAppParseInfo appParseInfo, AppUpdateAppInfoUploadCallback callback) {

        ParseObject parseObject = new ParseObject(ParseConstant.APP_UPDATE_APP.TABLE);
        parseObject.put(ParseConstant.APP_UPDATE_APP.PACKAGE_NAME, appParseInfo.packageName);
        parseObject.put(ParseConstant.APP_UPDATE_APP.COUNT, 1);

        double appSize = ((double) appParseInfo.appSize / (1024 * 1024));
        appSize = Math.round(appSize * 100.0) / 100.0;

        parseObject.put(ParseConstant.APP_UPDATE_APP.APP_SIZE, appSize);
        parseObject.put(ParseConstant.APP_UPDATE_APP.APP_NAME, appParseInfo.appName);
        parseObject.put(ParseConstant.APP_UPDATE_APP.SENDER_ID, appParseInfo.senderUserId);
        parseObject.put(ParseConstant.APP_UPDATE_APP.RECEIVER_ID, appParseInfo.receiverId);
        parseObject.put(ParseConstant.APP_UPDATE_APP.SENDER_VERSION_NAME, appParseInfo.senderAppVersionName);
        parseObject.put(ParseConstant.APP_UPDATE_APP.RECEIVER_VERSION_NAME, appParseInfo.receiverAppVersionName);
        parseObject.put(ParseConstant.APP_UPDATE_APP.SENDER_VERSION_CODE, appParseInfo.senderAppVersionCode);
        parseObject.put(ParseConstant.APP_UPDATE_APP.RECEIVER_VERSION_CODE, appParseInfo.receiverAppVersionCode);
        parseObject.put(ParseConstant.APP_UPDATE_APP.IS_CHECKING, appParseInfo.isChecking);
        parseObject.put(ParseConstant.APP_UPDATE_APP.APP_UPDATE_TIME, appParseInfo.timestamp);

        parseObject.saveInBackground(e -> {
            if (e == null) {
                if (storeId != 0 && callback != null) {
                    callback.onGetAppUpdateInfoSuccessListener(storeId);
                }
                Log.d("AppUpdateInfo", "App update app info saved in parse server successfully");
            } else {
                Log.e("AppUpdateInfo", "Parse server error: " + e.getMessage());
            }
        });

    }

   /* public void sendTokenMinteLog(String tx_hash, String to, String num, String log) {
        ParseObject parseObject = new ParseObject(ParseConstant.Transaction.TABLE);

        parseObject.put(ParseConstant.Transaction.TX_HASH, tx_hash);
        parseObject.put(ParseConstant.Transaction.LOG, log);

        parseObject.put(ParseConstant.TOKEN_MINTED.TO, to);
        parseObject.put(ParseConstant.TOKEN_MINTED.NUM, num);

        saveObject(parseObject, tx_hash);
    }*/
}
