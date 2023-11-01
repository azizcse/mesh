package com.w3engineers.walleter.wallet;
 
/*
============================================================================
Copyright (C) 2020 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

public class WalletCreateManager {

    private static WalletCreateManager walletCreateManager = null;

    public static WalletCreateManager getInstance() {
        if (walletCreateManager == null) {
            walletCreateManager = new WalletCreateManager();
        }
        return walletCreateManager;
    }

    public interface WalletListener {
        void onSuccess(String walletAddress, String publicKey, String appToken);
        void onError(String message, String appToken);
    }

    public void createWallet(Context context, String password, String appToken, WalletListener walletListener){
        if (TextUtils.isEmpty(password)){
            walletListener.onError("Password cant be empty", appToken);
        }else if (password.length() < 8){
            walletListener.onError("Password can't be smaller then 8 character", appToken);
        }else {

            if (WalletService.getInstance(context).isWalletExists()) {
                WalletService.getInstance(context).deleteExistsWallet();
            }

            WalletService.getInstance(context).createOrLoadWallet(password, appToken, new WalletService.WalletLoadListener() {
                @Override
                public void onWalletLoaded(String walletAddress, String publicKey, String appToken) {
                    walletListener.onSuccess(walletAddress, publicKey, appToken);
                }

                @Override
                public void onErrorOccurred(String message, String appToken) {
                    walletListener.onError(message, appToken);
                }

                @Override
                public void onErrorOccurred(int code, String appToken) {
                    walletListener.onError("Permission not allow", appToken);
                }
            });
        }
    }

    public void loadWallet(Context context, String password, String appToken, WalletListener walletListener){
        if (TextUtils.isEmpty(password)){
            walletListener.onError("Password cant be empty", appToken);
        }else if (password.length() < 8){
            walletListener.onError("Password can't be smaller then 8 character", appToken);
        }else {

            WalletService.getInstance(context).createOrLoadWallet(password, appToken, new WalletService.WalletLoadListener() {
                @Override
                public void onWalletLoaded(String walletAddress, String publicKey, String appToken) {
                    walletListener.onSuccess(walletAddress, publicKey, appToken);
                }

                @Override
                public void onErrorOccurred(String message, String appToken) {
                    walletListener.onError(message, appToken);
                }

                @Override
                public void onErrorOccurred(int code, String appToken) {

                }
            });
        }
    }

    public void importWallet(Context context, String password, String fileUri, String appToken,  WalletListener walletListener){
        if (TextUtils.isEmpty(password)){
            walletListener.onError("Password cant be empty", appToken);
        }else if (password.length() < 8){
            walletListener.onError("Password can't be smaller then 8 character", appToken);
        }else {
            WalletService mWalletService =  WalletService.getInstance(context);

            mWalletService.importWallet(password, fileUri, new WalletService.WalletImportListener() {
                @Override
                public void onWalletImported(String walletAddress, String publicKey) {
                    walletListener.onSuccess(walletAddress, publicKey, appToken);
                }

                @Override
                public void onError(String message) {
                    walletListener.onError(message, appToken);
                }
            });
        }
    }

    /*

    *//**
     * This api is used to create wallet
     * @param context
     * @param password
     * @param listener
     *//*


    *//**
     * This api is used to load wallet
     * @param context
     * @param password
     * @param listener
     *//*

    *//**
     * This api is used to import wallet
     * @param context
     * @param password
     * @param fileUri
     * @param listener
     *//*
    */
}
