package com.w3engineers.purchase.helper.crypto;


import com.w3engineers.mesh.util.MeshLog;

import java.math.BigInteger;

public class CryptoHelper {

    public static String encrypt(String privateKey, String otherPartyPoint, byte[] plainText) {
        BigInteger sharedSecret = ECDSA.generateBasicSharedSecret(privateKey, otherPartyPoint);
        String secretKey = sharedSecret.toString(16);
        MeshLog.v("CryptoHelper(encrypt): secretKey: " + secretKey);

        String encoded = EncryptUtil.encrypt(secretKey, plainText);
        return encoded;
    }

    public static String decrypt(String privateKey, String otherPartyPoint, String cipherText) {
        BigInteger sharedSecret = ECDSA.generateBasicSharedSecret(privateKey, otherPartyPoint);

        String secretKey = sharedSecret.toString(16);
        MeshLog.v("CryptoHelper(decrypt): secretKey: " + secretKey);

        String decrypted = EncryptUtil.decrypt(secretKey, cipherText);
        return decrypted;
    }

    public static String decryptMessage(String myPrivateKey, String othersPublicKey, String message) {
        return CryptoHelper.decrypt(myPrivateKey, othersPublicKey, message);
    }

    public static String encryptMessage(String myPrivateKey, String othersPublicKey, String message) {
        return CryptoHelper.encrypt(myPrivateKey, othersPublicKey, message.getBytes());
    }
}
