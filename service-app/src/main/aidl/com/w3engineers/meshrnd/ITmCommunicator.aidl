package com.w3engineers.meshrnd;
import com.w3engineers.mesh.ViperCommunicator;
import com.w3engineers.models.UserInfo;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.models.FileData;
import com.w3engineers.models.MessageData;
import android.net.Network;

interface ITmCommunicator {
    void startTeleMeshService(in ViperCommunicator viperCommunicator, in String appToken,in UserInfo userInfo);
    boolean startMesh(in String appToken);
    void onStartForeground(in boolean isNeeded);

    void sendData(in MessageData messageData);

    void sendAppUpdateRequest(in FileData fileData);

    void isLocalUseConnected(in String userId);
    void restartMesh(in int newRole);
    void stopMesh();
    void destroyService();

    void saveUserInfo(in UserInfo userInfo);
    void saveOtherUserInfo(in UserInfo userInfo);
    void triggerReSyncConfiguration(in String appToken);
    void allowPermissions(in List<String> permissions);

    int  getLinkTypeById(in String nodeID);
    List<String> getInternetSellers(in String appToken);
    int getUserMeshRole();
    String getFirstAppToken();

    void openWalletCreationUI(in String appToken);
    void openDataplanUI(in String appToken);
    void openWalletUI(in String appToken, in byte[] pictureData);
    void openSellerInterfaceUI(in String appToken);
    void openMeshLogUI();

    String sendFile(in FileData fileData);
    String sendInAppUpdateFile(in FileData fileData);
    void sendFileResumeRequest(in FileData fileData);
    void removeSendContent(in FileData fileData);

    void sendLocalBroadcast(in BroadcastData broadcastData);

    int checkUserConnectivityStatus(in String userId);

    boolean isNetworkOnline();
    Network getNetwork();
}
