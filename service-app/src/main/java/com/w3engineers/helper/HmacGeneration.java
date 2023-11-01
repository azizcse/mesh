package com.w3engineers.helper;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;

import com.w3engineers.meshrnd.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacGeneration {

    private static HmacGeneration hmacGeneration = new HmacGeneration();

    public static HmacGeneration getInstance() {
        return hmacGeneration;
    }

    public String prepareSignature(Context context, String address) throws NoSuchAlgorithmException, InvalidKeyException {
        TreeMap<String, String> map = new TreeMap<>();
        String ADDRESS = "Address";
        map.put(ADDRESS, address);

        String key = getKeyHash(context);

        return buildSignature(map, key);
    }

    private String getKeyHash(Context context) {
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID,
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                String KEY_SHA_256 = "SHA256";
                md = MessageDigest.getInstance(KEY_SHA_256);
                md.update(signature.toByteArray());
                return new String(Base64.encode(md.digest(), 0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String buildSignature(TreeMap<String, String> formParameters, String secretKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        // Build message from parameters
        String message = TextUtils.join("+", formParameters.values());
        message += "+" + secretKey;
        // Sign
        return hmacSha256Base64(message, secretKey);
    }

    private String hmacSha256Base64(String message, String secretKey) throws
            NoSuchAlgorithmException, InvalidKeyException {
        // Prepare hmac sha256 cipher algorithm with provided secretKey
        Mac hmacSha256;
        String HMAC_SHA_256_COMBINE = "HmacSHA256";
        try {
            hmacSha256 = Mac.getInstance(HMAC_SHA_256_COMBINE);
        } catch (NoSuchAlgorithmException nsae) {
            String HMAC_SHA_256 = "HMAC-SHA-256";
            hmacSha256 = Mac.getInstance(HMAC_SHA_256);
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256_COMBINE);
        hmacSha256.init(secretKeySpec);
        // Build and return signature
        return Base64.encodeToString(hmacSha256.doFinal(message.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
    }
}
