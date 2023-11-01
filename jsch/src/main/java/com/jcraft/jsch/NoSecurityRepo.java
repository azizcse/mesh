package com.jcraft.jsch;

public   class NoSecurityRepo implements HostKeyRepository {
    @Override
    public int check(String host, byte[] key) {
        return HostKeyRepository.OK;
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {

    }

    @Override
    public void remove(String host, String type) {

    }

    @Override
    public void remove(String host, String type, byte[] key) {

    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "NoSecurityRepo";
    }

    @Override
    public HostKey[] getHostKey() {
        return new HostKey[0];
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        return new HostKey[0];
    }

}