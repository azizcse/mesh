package com.w3engineers.walleter.wallet;

import android.content.Context;
import android.os.Build;
import android.util.Log;


import com.snatik.storage.Storage;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;


/**
 * Created by Monir Zzaman on 10/30/2017.
 * Purpose: To create wallet and to load wallet
 */

class Web3jWallet {
    private Context mContext;
    private Storage storage; // this is the library class for internal and external storage implementations ---    compile 'com.snatik:storage:2.1.0'
    private String TAG = "Web3jWallet";

    public Web3jWallet(Context context) {
        this.mContext = context.getApplicationContext();
        storage = new Storage(context);
    }

    protected String createKeystoreFile(String password, String keyStoreFolderName) {
        String walletDir = getKeystoreDir(keyStoreFolderName);
        Log.v("Wallet", "8");
        storage.createDirectory(walletDir);
        Log.v("Wallet", "9");
        File file = new File(walletDir);
        Log.v("Wallet", "10");
        try {
            if (!file.exists()) {
                Log.v("Wallet", "11");
                //Todo we have to check for android 11
                boolean isCreated = file.mkdir();
                Log.e("Wallet", "Status " + isCreated);
            }
            if (file.exists() && file.isDirectory()) {
                Log.v("Wallet", "12");
                String keyStoreFileName = WalletUtils.generateLightNewWalletFile(password, file);
                Log.v("Wallet", "createKeystoreFile: " + keyStoreFileName);
                return keyStoreFileName;
            }
        } catch (Exception e) {
            Log.v("Wallet", "13 " + e);
            e.printStackTrace();
        }
        return null;
    }


    protected String getKeystoreDir(String dir) {
        String parent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parent = mContext.getExternalFilesDir("").getPath();
        } else {
            parent = storage.getExternalStorageDirectory();
        }
        String walletTest = WalletUtils.getTestnetKeyDirectory();
        String walletDir = parent + walletTest + File.separator + dir;
        return walletDir;
    }


    protected Credentials loadKeystoreFile(String password, String keyStoreFolderName, String keyStoreFileName) {
        try {
            String walletPath = getKeystorePath(keyStoreFolderName, keyStoreFileName);
            Log.e("path_wallet", walletPath);
            Credentials credentials = WalletUtils.loadCredentials(password, walletPath);
            return credentials;
        } catch (IOException | CipherException e) {
            return null;
        }
    }

    protected String getKeystorePath(String keyStoreFolderName, String keyStoreFileName) {
        String walletDir = getKeystoreDir(keyStoreFolderName);
        return walletDir + File.separator + keyStoreFileName;
    }

}
