package com.w3engineers.walleter.wallet;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.w3engineers.ext.strom.util.helper.data.local.SharedPref;
import com.w3engineers.walleter.R;
import com.w3engineers.walleter.wallet.helper.crypto.ECDSA;

import org.web3j.crypto.Credentials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WalletService {

    private final String TAG = getClass().getSimpleName();

    private Context mContext;
    private final String walletSuffixDir;
    public static final String DEFAULT_PASSWORD = "mesh_123";
    public static final int ERROR_CODE_NO_PERMISSION = 5;
    private volatile Credentials mCredentials;
    private final String WALLET_ADDRESS = "wallet_address";
    private final String WALLET_FILE_NAME = "wallet_name";
    private final String PUBLIC_KEY = "public_key";
    private final String PRIVATE_KEY = "private_key";
    private final String WALLET_PASSWORD_KEY = "wallet_pass_key";
    private final String KEY_USER_ID = "mesh_id";
    private static WalletService walletService;

    private SharedPref mSharedPref;
    private Handler mHandler;
    private HandlerThread mHandlerThread;


    public interface WalletLoadListener {
        void onWalletLoaded(String walletAddress, String publicKey, String appToken);

        void onErrorOccurred(String message, String appToken);
        void onErrorOccurred(int code, String appToken);
    }

    public interface WalletCreateListener {
        void onWalletCreated(String walletAddress, String publicKey);

        void onError(String message);
    }

    public interface WalletImportListener {
        void onWalletImported(String walletAddress, String publicKey);

        void onError(String message);
    }


    private WalletService(Context context) {
        mSharedPref = SharedPref.getSharedPref(context);

        mHandlerThread = new HandlerThread("wallet_thread", Thread.MAX_PRIORITY);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        this.mContext = context;
        walletSuffixDir = "wallet/" + mContext.getString(R.string.wallet_directory_name);
    }

    public static WalletService getInstance(Context context) {
        if (walletService == null) {
            walletService = new WalletService(context);
        }

        return walletService;
    }

    public boolean isWalletExists() {
        boolean isWalletExists = false;

        String filePath = Web3jWalletHelper.onInstance(mContext).getWalletDir(walletSuffixDir);
        File directory = new File(filePath);

        File[] list = directory.listFiles();
        if (list != null) {
            for (File f : list) {
                String name = f.getName();
                if (name.endsWith(".json")) {
                    mSharedPref.write(WALLET_FILE_NAME, name);
                    isWalletExists = true;
                    break;
                }
            }
        }

        return isWalletExists;
    }

    public void createOrLoadWallet(final String password, String appToken, final WalletLoadListener listener) {

        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            mSharedPref.write(WALLET_PASSWORD_KEY, password);
            mHandler.post(() -> {
                String existAddress = mSharedPref.read(WALLET_ADDRESS);
                if (!TextUtils.isEmpty(existAddress) && isWalletExists()) {
                    Log.i(TAG, " Ethereum address already exist");
                    listener.onWalletLoaded(existAddress, mSharedPref.read(PUBLIC_KEY),appToken);
                    initCredential();
                    return;
                }

                if (isWalletExists()) {
                    String keyStoreFileName = mSharedPref.read(WALLET_FILE_NAME);
                    loadWalletFromKeystore(password, keyStoreFileName);

                    if (mCredentials != null) {

                        listener.onWalletLoaded(mCredentials.getAddress(), mSharedPref.read(PUBLIC_KEY),appToken);

                    } else {
                        listener.onErrorOccurred("WalletManager credential load failed", appToken);
                    }
                } else {
                    Log.i(TAG, " Wallet not exist.............");
                    String keyStoreFileName = Web3jWalletHelper.onInstance(mContext).createWallet(password, walletSuffixDir);

                    if (keyStoreFileName != null) {

                        loadWalletFromKeystore(password, keyStoreFileName);

                        if (mCredentials != null) {
                            Log.i(TAG, " Wallet generated.............");
                            listener.onWalletLoaded(mCredentials.getAddress(), mSharedPref.read(PUBLIC_KEY), appToken);


                        } else {
                            Log.i(TAG, " Wallet generated error .............");
                            listener.onErrorOccurred("WalletManager credential load failed", appToken);
                        }
                    } else {

                        listener.onErrorOccurred("WalletManager file generate failed",appToken);
                    }
                }
            });
        } else {
            listener.onErrorOccurred(WalletService.ERROR_CODE_NO_PERMISSION,appToken);
        }
    }

    public String getPublicKey(){
        return mSharedPref.read(PUBLIC_KEY);
    }

    public void importWallet(String password, String fileUri, WalletImportListener listener) {
        if (isWalletExists()) {
            // delete wallet file
            deleteExistsWallet();
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                    String savePtah = WalletService.getInstance(mContext).getWalletDirectory();
                copyFile(fileUri, savePtah);

                if (isWalletExists()) {
                    mSharedPref.write(WALLET_PASSWORD_KEY, password);
                    String keyStoreFileName = mSharedPref.read(WALLET_FILE_NAME);
                    loadWalletFromKeystore(password, keyStoreFileName);

                    if (mCredentials != null) {
                        listener.onWalletImported(mCredentials.getAddress(), mSharedPref.read(PUBLIC_KEY));
                    } else {
                        listener.onError("Wallet import failed");
                    }
                } else {
                    listener.onError("Wallet import failed");
                }
            }
        });

    }

    /**
     * This is used to copy the content of file
     *
     * @param source
     * @param destination
     */
    private void copyFile(String source, String destination) {
        try {
            InputStream inputStream = new FileInputStream(new File(source));//mConText.getContentResolver().openInputStream(source);
            if (inputStream != null) {
                String ret = "";
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();

                File mainFile = new File(source);

                File outputDir = new File(destination);
                if (!outputDir.exists()) {
                    //Todo we have to check for android 11
                    boolean isSuccess = outputDir.mkdirs();
                    Log.e("Create","Success :"+isSuccess);
                }
                File outputFile = File.createTempFile(mainFile.getName(), ".json", outputDir);

                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(ret.getBytes());
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public boolean deleteExistsWallet() {
        boolean isWalletExists = false;

        String filePath = Web3jWalletHelper.onInstance(mContext).getWalletDir(walletSuffixDir);
        File directory = new File(filePath);

        File[] list = directory.listFiles();
        if (list != null) {
            for (File f : list) {
                f.delete();
            }
        }

        return isWalletExists;
    }


    private void loadWalletFromKeystore(String password, String keyStoreFileName) {
        mCredentials = Web3jWalletHelper.onInstance(mContext).getWallet(password, walletSuffixDir, keyStoreFileName);
        if (mCredentials != null){
            mSharedPref.write(WALLET_ADDRESS, mCredentials.getAddress());
            mSharedPref.write(KEY_USER_ID, mCredentials.getAddress());
            //mSharedPref.write(PUBLIC_KEY, mCredentials.getEcKeyPair().getPublicKey().toString(16));
            mSharedPref.write(KEY_USER_ID, mCredentials.getAddress());
            mSharedPref.write(PRIVATE_KEY, mCredentials.getEcKeyPair().getPrivateKey().toString(16));
            mSharedPref.write(PUBLIC_KEY, ECDSA.getHexEncodedPoint(mSharedPref.read(PRIVATE_KEY)));

            Log.e(TAG, "publickey::" + ECDSA.getHexEncodedPoint(mSharedPref.read(PRIVATE_KEY)) +
                    "\n" + "Length " + ECDSA.getHexEncodedPoint(mSharedPref.read(PRIVATE_KEY)).length());
        }
    }

    public Credentials getCredentials() {
        if (mCredentials == null) {
            String privateKey = mSharedPref.read(PRIVATE_KEY);
            if (TextUtils.isEmpty(privateKey)) {
                String keyStoreFileNmae = mSharedPref.read(WALLET_FILE_NAME);
                String password = mSharedPref.read(WALLET_PASSWORD_KEY);

                loadWalletFromKeystore(password, keyStoreFileNmae);
            } else {
                mCredentials = Credentials.create(privateKey);
            }
        }
        return mCredentials;
    }

    private void initCredential() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getCredentials();
            }
        }).start();

    }

    public String getPrivateKey() {
        String privateKey = mSharedPref.read(PRIVATE_KEY);
        return privateKey;
    }

    public String getWalletFilePath() {
        String filePath = Web3jWalletHelper.onInstance(mContext).getWalletDir(walletSuffixDir);
        File directory = new File(filePath);

        File[] list = directory.listFiles();
        if (list != null) {
            for (File f : list) {
                String name = f.getName();
                if (name.endsWith(".json")) {
                    return f.getAbsolutePath();
                }
            }
        }
        return null;
    }

    public String getWalletDirectory() {
        String filePath = Web3jWalletHelper.onInstance(mContext).getWalletDir(walletSuffixDir);
        return filePath;
    }

}
