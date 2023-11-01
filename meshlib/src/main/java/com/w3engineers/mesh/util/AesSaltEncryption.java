package com.w3engineers.mesh.util;

import android.security.keystore.KeyProperties;

import com.google.gson.Gson;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AesSaltEncryption {
    private static String ALGORTIHM_TYPE = "PBKDF2WithHmacSHA1";
    private static String CPR_TRANSFORMATION = "AES/CBC/PKCS7Padding";
    //API 23+ //https://miro.medium.com/max/2068/1*MNcknQeCrJMhTWx9JlpnKg.png

    public static byte[] encrypt(byte[] data) {
        try {
            byte[] salt = new byte[256];
            new SecureRandom().nextBytes(salt);

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CPR_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(salt), new IvParameterSpec(iv));
            AesSalt aesSalt = new AesSalt(salt, iv, cipher.doFinal(data));
            return new Gson().toJson(aesSalt).getBytes();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] decrypt(byte[] data) {
        AesSalt aesSalt = new Gson().fromJson(new String(data), AesSalt.class);
        byte[] salt = aesSalt.getSalt();
        byte[] iv = aesSalt.getIv();
        byte[] encrypted = aesSalt.getEnc();
        try {
            Cipher cipher = Cipher.getInstance(CPR_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(salt), new IvParameterSpec(iv));
            return cipher.doFinal(encrypted);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static Key getSecretKey(byte[] salt) {
        try {
            PBEKeySpec pbKeySpec = new PBEKeySpec(P2PUtil.ENCRYPT_PASSWORD.toCharArray(), salt, 1324, 256);
            byte[] keyBytes = SecretKeyFactory.getInstance(ALGORTIHM_TYPE).generateSecret(pbKeySpec).getEncoded();
            return new SecretKeySpec(keyBytes, KeyProperties.KEY_ALGORITHM_AES);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
