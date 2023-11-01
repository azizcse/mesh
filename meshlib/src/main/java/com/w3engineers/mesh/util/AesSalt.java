package com.w3engineers.mesh.util;

public class AesSalt {
    private byte[] salt;
    private byte[] iv;
    private byte[] enc;

    public AesSalt(byte[] salt, byte[] iv, byte[] enc) {
        this.salt = salt;
        this.iv = iv;
        this.enc = enc;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getEnc() {
        return enc;
    }

    public void setEnc(byte[] enc) {
        this.enc = enc;
    }
}
