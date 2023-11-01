package com.w3engineers.meshrnd.ui.Nearby;

import com.w3engineers.meshrnd.model.UserModel;

public interface NearbyCallBack {
    void onUserFound(UserModel model);

    void onDisconnectUser(String userId);

}
