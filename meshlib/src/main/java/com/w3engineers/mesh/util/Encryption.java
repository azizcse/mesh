package com.w3engineers.mesh.util;

public class Encryption {
    private byte[] ivBytes;
    private byte[] encrypted;

    public Encryption(byte[] ivBytes, byte[] encrypted) {
        this.ivBytes = ivBytes;
        this.encrypted = encrypted;
    }

    public byte[] getIvBytes() {
        return ivBytes;
    }

    public void setIvBytes(byte[] ivBytes) {
        this.ivBytes = ivBytes;
    }

    public byte[] getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(byte[] encrypted) {
        this.encrypted = encrypted;
    }
}
