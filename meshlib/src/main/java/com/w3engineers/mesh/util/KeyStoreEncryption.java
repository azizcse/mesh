package com.w3engineers.mesh.util;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

@RequiresApi(api = Build.VERSION_CODES.M)
public class KeyStoreEncryption {
    static {

        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder("MyKeyAlias",
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }


    public static byte[] keyStoreEncrypt(byte[] dataToEncrypt) {
        try {

            // 1
            //Get the key
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry("MyKeyAlias", null);
            SecretKey secretKey = secretKeyEntry.getSecretKey();
            // 2
            //Encrypt data
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] ivBytes = cipher.getIV();
            byte[] encryptedBytes = cipher.doFinal(dataToEncrypt);
            Encryption obj = new Encryption(ivBytes,encryptedBytes);
            return new Gson().toJson(obj).getBytes();

        } catch (Throwable e) {

        }
        return null;
    }

    public static byte[] keystoreDecrypt(byte[] encryptedDate) {
        try {

            // 1
            //Get the key
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry("MyKeyAlias", null);
            SecretKey secretKey = secretKeyEntry.getSecretKey();

            String byteToText = new String(encryptedDate);
            Encryption encryption = new Gson().fromJson(byteToText, Encryption.class);

            // 3
            //Decrypt data
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, encryption.getIvBytes());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(encryption.getEncrypted());

        } catch (Throwable e) {

        }
        return null;
    }
}
