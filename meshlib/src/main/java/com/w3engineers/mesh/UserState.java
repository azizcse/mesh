package com.w3engineers.mesh;
/**
 * Transport state manages
 */
public interface UserState {
    void onUserConnected(String userId, boolean isConnected);
}
