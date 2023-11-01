package com.w3engineers.meshrnd.ui.nav;


public interface MeshStateListener {
    void onMessageReceived(String messageId);
    void onInterruption(int details);
}
