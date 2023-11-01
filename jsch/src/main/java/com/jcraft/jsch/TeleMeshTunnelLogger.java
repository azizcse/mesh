package com.jcraft.jsch;


public class TeleMeshTunnelLogger implements Logger {
    @Override
    public boolean isEnabled(int level) {
        return true;
    }

    @Override
    public void log(int level, String message) {
        System.err.printf("%s\t%s%n", level, message);

    }
}
