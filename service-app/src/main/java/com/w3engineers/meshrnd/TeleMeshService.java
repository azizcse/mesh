package com.w3engineers.meshrnd;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Network;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guardsquare.dexguard.rasp.callback.DetectionReport;
import com.guardsquare.dexguard.runtime.report.TamperDetectorReport;
import com.letbyte.core.meshfilesharing.api.MeshFile;
import com.letbyte.core.meshfilesharing.api.MeshFileCommunicator;
import com.letbyte.core.meshfilesharing.api.MeshFileEventListener;
import com.letbyte.core.meshfilesharing.api.support.mesh.SupportTransportManager;
import com.w3engineers.eth.data.remote.EthereumService;
import com.w3engineers.eth.data.remote.parse.AppUpdateAppParseInfo;
import com.w3engineers.eth.data.remote.parse.ParseManager;
import com.w3engineers.eth.util.data.NetworkMonitor;
import com.w3engineers.ext.strom.BuildConfig;
import com.w3engineers.ext.strom.util.helper.NotificationUtil;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.helper.BroadcastDataHelper;
import com.w3engineers.helper.ContentMetaGsonBuilder;
import com.w3engineers.helper.callback.BroadcastCallback;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.TransportState;
import com.w3engineers.mesh.UserState;
import com.w3engineers.mesh.ViperCommunicator;
import com.w3engineers.mesh.apkupdate.Constants;
import com.w3engineers.mesh.apkupdate.ServiceUpdate;
import com.w3engineers.mesh.apkupdate.ServiceUpdateDialog;
import com.w3engineers.mesh.apkupdate.TSAppInstaller;
import com.w3engineers.mesh.datasharing.database.DatabaseService;
import com.w3engineers.mesh.datasharing.database.message.Message;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.peers.PeersEntity;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.manager.AppUpdateAppDataManager;
import com.w3engineers.mesh.manager.BoundServiceManager;
import com.w3engineers.mesh.meshlog.ui.meshloghistory.MeshLogHistoryActivity;
import com.w3engineers.mesh.model.AppVersion;
import com.w3engineers.mesh.model.Broadcast;
import com.w3engineers.mesh.model.BroadcastAck;
import com.w3engineers.mesh.model.HandshakeInfo;
import com.w3engineers.mesh.ui.TeleMeshServiceMainActivity;
import com.w3engineers.mesh.ui.profilechoice.ProfileChoiceActivity;
import com.w3engineers.mesh.util.AppBackupUtil;
import com.w3engineers.mesh.util.AppInfoUtil;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.CredentialUtils;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.GsonUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.mesh.util.JsonBuilder;
import com.w3engineers.mesh.util.JsonDataBuilder;
import com.w3engineers.mesh.util.LocationTracker;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.util.NotifyUtil;
import com.w3engineers.mesh.util.Utils;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.mesh.workmanager.RefreshJobWorker;
import com.w3engineers.meshrnd.databinding.DialogServiceAppInstallProgressBinding;
import com.w3engineers.models.AppUpdateAppInfoModel;
import com.w3engineers.models.BroadcastData;
import com.w3engineers.models.ContentMetaInfo;
import com.w3engineers.models.DiscoveredNodeInfo;
import com.w3engineers.models.FileData;
import com.w3engineers.models.MeshControlConfig;
import com.w3engineers.models.MessageData;
import com.w3engineers.models.PendingContentInfo;
import com.w3engineers.models.UserInfo;
import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.dataplan.DataPlanManager;
import com.w3engineers.purchase.db.appupdateappinfo.AppUpdateInfoEntity;
import com.w3engineers.purchase.db.broadcast.BroadcastEntity;
import com.w3engineers.purchase.db.clientinfo.ClientInfoEntity;
import com.w3engineers.purchase.db.content.Content;
import com.w3engineers.purchase.db.handshaking_track.HandshakeTrackEntity;
import com.w3engineers.purchase.helper.PreferencesHelperDataplan;
import com.w3engineers.purchase.helper.crypto.CryptoHelper;
import com.w3engineers.purchase.manager.PurchaseManager;
import com.w3engineers.purchase.manager.PurchaseManagerBuyer;
import com.w3engineers.purchase.manager.PurchaseManagerSeller;
import com.w3engineers.purchase.model.ConfigurationCommand;
import com.w3engineers.purchase.util.ClientInfoSyncUtil;
import com.w3engineers.purchase.util.ConfigSyncUtil;
import com.w3engineers.purchase.util.EthereumServiceUtil;
import com.w3engineers.purchase.wallet.WalletManager;
import com.w3engineers.walleter.wallet.WalletService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.guardsquare.dexguard.runtime.detection.*;

/**
 * The service is started by calling {@link #startService(Intent)} from an app. It may also be
 * called with {@link #startForegroundService(Intent)} from an app because we do make a call to
 * {@link #startForeground(int, Notification)} to register the notification tray.
 * <p>
 * After starting the service, it should be bound to so that it remains long running, otherwise from
 * what I understand Android can garbage collect it. We may need to experiment more to determine if
 * it can still be garbage collected even with being bound.
 * <p>
 * The service is terminated by sending a broadcast intent with action
 * {@link #stopService(Intent)} which should kill the service as long as it wasn't bound with the
 * BIND_AUTO_CREATE_FLAG. Any cleanup that must be done should be executed from onDestroy. In order
 * to prevent a RemoteException in the BoundClient when the service shuts down, the BoundClient also
 * listens for the same intent and unbinds itself from the service. However, if the order of this
 * is unlucky a Remote Exception can still occur on shutdown.
 *
 * <p>NOTE:
 * The background service is kind of weird. If you don't have a foreground context which is attached
 * to the background service, Android can garbage collect it, or restrict its ability to run
 * according to a bunch of posts I've been reading. As a result, one solution that has been proposed
 * that "appears" to work with some limited testing I've tried (and the way we also do it in
 * present-day TeleMesh) is to spawn a foreground service (ie: a notification) after launching.
 * Because the user can interact with this if they like, it is possible to have a long running
 * background service.</p>
 *
 * @version $Version:$
 */

public class TeleMeshService extends Service implements LinkStateListener, UserState,
        WalletService.WalletLoadListener/*, ConfigSyncUtil.ConfigSyncCallback*/,
        MeshFileEventListener, BroadcastCallback/*, ClientInfoSyncUtil.ClientInfoSyncListener*/ {

    private final String TAG = "TMeshService";

    public static final String ACTION_STOP_SERVICE = "stop_service";
    public static final String ACTION_APP_UPDATE_REQUEST = "app_update_request";
    public static final String ACTION_APP_UPDATE_SUCCESS = "app_update_success";

    public static final String USER_LOCATION_UPDATE = "user_location_update";
    private WorkManager mWorkManager;
    private Notification notification;
    private TransportManagerX transportManagerX;

    private final int APP_PORT = 3250;
    private static ViperCommunicator mViperCommunicator;
    private MeshFileCommunicator mMeshFileCommunicator;
    //  private ConcurrentLinkedQueue<String> discoveredNodeIdsQueue;
    private Map<String, String> userIdNameMap;

    List<String> messageIdList = new ArrayList<>();

    private boolean isServiceBounded = false;

    private String KEY_NOTIFY = "n", KEY_MSG = "m", KEY_HMAC_AUTH = "a", KEY_HMAC_RES = "h";
    private String KEY_USER_PING = "p", KEY_USER_INFO = "u", KEY_CREDENTIALS_CONFIGURATION = "c";
    private int NOT_NOTIFY = 0, NOTIFY = 1;

    private HashMap<String, DiscoveredNodeInfo> nodeMap = new HashMap<>();
    private ArrayList<String> liveUserIds = new ArrayList<>();
    public static List<MeshControlConfig> meshControlConfigs;
    private int mInterval = 1000 * 60 * 1; // 1 min by default, can be changed later
    private Handler mHandler;

    /**
     * This client info contains all client information
     * like client apps permission to use feature
     */
    public static List<ClientInfoEntity> clientInfoList;

    public static final String TAG_CONFIGURATION_REFRESH_JOB = "tag_configuration_refresh_job";
    private HashMap<String, String> receiverContentMap = new HashMap<>();


    protected com.w3engineers.purchase.db.DatabaseService databaseService;

    /**
     * This map used to track the client app has new update available but
     * the client app not running.
     * <p>
     * Insert: When client app nut running.
     * Get: When client app again bind;
     */
    public static ConcurrentHashMap<String, AppUpdateAppInfoModel> inAppUpdateDataMap = new ConcurrentHashMap<>();

    /**
     * This map is used for track for two things
     * 1. for track the update apk sender
     * 2. for track sender apk path that need to be delete when it
     * is completed or failed
     */
    private static ConcurrentHashMap<String, String> inAppUpdatePathMap = new ConcurrentHashMap<>();

    /**
     * This hash map is used for tracking app update receiver id of current app update app process.
     * Because the we need this id at the end of file transfer. where we don't get this id
     * size value until the apk transfer success or failed
     */
    private static ConcurrentHashMap<String, String> inAppUpdateAppReceiverMap = new ConcurrentHashMap<>();

    /**
     * Here use this token for sending or receiving buy,sell
     * related information (Pay) only for Telemesh.
     * <p>
     * If we want to make it dynamically we have to update pay message
     * sending process and need to add APP_SPECIFIC_TOKEN
     * <p>
     * For now we only used Telemesh static token for buy sell information
     */
    private final String TELEMESH_APP_TOKEN = "com.w3engineers.unicef.telemesh";

    private static ConcurrentHashMap<String, ViperCommunicator> viperCommunicatorList = new ConcurrentHashMap<>();
    //private String appName;

    /**
     * <p>Note: From the {@link Service} documentation: A Service is not a separate process.
     * The Service object itself does not imply it is running in its own process;   otherwise
     * specified, it runs in the same process as the application it is part of.
     * <p>
     * A Service is not a thread. It is not a means itself to do work off of the main thread
     * (to avoid Application Not Responding errors).
     * <p>
     * Thus, we must create a thread here and run everything inside the thread in order to ensure
     * there is actually a service thread running and we are not running on the UI thread by
     * accident. We should take the convention that all UI code should use the well-known android
     * functions for running on UI thread explicitly.
     * </p>
     */

    // Service app update element
//    private String appDownloadId = "";
    private String appPath;
    private static DialogServiceAppInstallProgressBinding binding;
    private static AlertDialog appUpdateProgressDialog;
    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
        mContext = this;

        getDataBaseService().updateAllPeersOnlineStatus(false);

        mHandler = new Handler();
        startLocationUpdatingTask();

/*        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        registerReceiver(mGpsSwitchStateReceiver, filter);*/
    }

    /**
     * This function is called: 1) when the service is first created. 2) When the user clicks the
     * notification and 3) if a jackass developer tries to start / connect or bind to the service.
     * <p>
     * The first two cases are the good path. If the user clicks the notification we want
     * to show the activity so that they can interact with the control for the service.
     * <p>
     * I think we can handle the bad case by making sure the service is not advertised externally.
     * This is a property of the service tag in the AndroidManifest. I think we can set exported to
     * false or something.
     *
     * <p>Note: I was expecting the onStart method to be called, but it never is. This method is
     * called when the service is started the first time and any subsequent time the app gets
     * focus. It turns out that was deprecated in API5.</p>
     *
     * @param intent  the intent that caused the service to be started, resumed, etc.
     * @param flags   not sure what goes in here, should figure out if any of it is useful
     * @param startId same as flags.
     * @return START_STICKY is returned to prevent Android from garbage collecting this service even
     * if all of the apps which are bound to it are dead.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Service start command.");
        if (intent == null || intent.getAction() == null) {
            Log.v(TAG, "FRESH START of Service onStartCommand");

            //entry point for the service
            //Mesh service need to start
            //Toast.makeText(this, "SERVICE STARTED", Toast.LENGTH_LONG).show();
        } else {
            if (intent.getAction().equals(ACTION_STOP_SERVICE)) {
                if (!isServiceBounded) {
                    Log.v(TAG, "NOTIFICATION CLICK");
                    destroyFullService();
                } else {
                    HandlerUtil.postForeground(() -> Toast.makeText(getApplicationContext(),
                            "Please stop client app first", Toast.LENGTH_LONG).show());
                }
            } else if (intent.getAction().equals(ACTION_APP_UPDATE_REQUEST)) {
                Log.v(TAG, "App update click");
                String receiverId = intent.getStringExtra(TeleMeshService.class.getName());
                if (transportManagerX != null) {
                    String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                    com.w3engineers.mesh.util.NotificationUtil.removeNotification(getApplicationContext(), myUserId);
                    appUpdateRequest(receiverId, getApplicationContext().getPackageName());
                }
            } else if (intent.getAction().equals(ACTION_APP_UPDATE_SUCCESS)) {
                String appDownloadedPath = intent.getStringExtra(Constants.InAppUpdate.APP_PATH_KEY);
                String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                com.w3engineers.mesh.util.NotificationUtil.removeNotification(getApplicationContext(), myUserId);
                ServiceUpdateDialog.getInstance().showAppInstaller(getApplicationContext(), appDownloadedPath);

            }
        }

        return START_STICKY;
    }


    /**
     * <p>Note:
     * This should only be called when the service is first created and bound to, but should likely
     * check to see what happens when multiple apps bind to the service (if we end up using AIDL).
     * If we just use TCP or some other IPC mechanism we should only have the wallet app binding to
     * this service, but we could have malicious devs also trying to bind to it, so we may wish to
     * restrict binding in some way.</p>
     *
     * @param intent parameters to construct the service
     * @return the bound service reference.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        /*
         * Setup the foreground tray notification service which will keep Android OS happy that we
         * haven't just abandoned a background service with no way for user to interact with it.
         */
        //TODO client app security impl
        // Bind request handle from here
        Log.v(TAG, "onBind() method called");

        isServiceBounded = true;
        BoundServiceManager.on(getApplicationContext()).setClientBindStatus(isServiceBounded);

        return iTmCommunicator;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        MeshLog.e("Service onUnbind.");
        isServiceBounded = false;
        Log.v(TAG, "Unbind Test: " + intent.getPackage());
        BoundServiceManager.on(getApplicationContext()).setClientBindStatus(isServiceBounded);
        // Todo We don't know specific which app is unbinding now.
        mViperCommunicator = null;
        return super.onUnbind(intent);
    }


    /**
     * <p>Note:
     * cleanup point for the service. Note: Android cleanups are usually time bounded so it
     * should be as efficient as possible. We also need to make sure that no threads are left
     * running in any of the modules. We should probably add tests which specifically verify that
     * this is the case. Interrupt is used to kill any long running or blocking I/O like calls
     * (to break out of loops) before the safe cleanup.</p>
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Service destroyed.");
        if (transportManagerX != null) {
            transportManagerX.stopMesh();
        }

        //   unregisterReceiver(mGpsSwitchStateReceiver);
        LocationTracker.getInstance(this).stopListener();
    }


    /*******************************************************************/
    /********************* Wallet load callback ************************/
    /*******************************************************************/
    @Override
    public void onWalletLoaded(String walletAddress, String publicKey, String appToken) {

        SharedPref.write(Utils.KEY_NODE_ADDRESS, walletAddress);
        SharedPref.write(Utils.KEY_WALLET_PUB_KEY, publicKey);

        try {
            iTmCommunicator.startMesh(appToken);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //TODO mesh start
    }

    @Override
    public void onErrorOccurred(String message, String appToken) {
        notifyStartMesh(appToken, "", false, "Wallet load failed");
    }

    @Override
    public void onErrorOccurred(int code, String appToken) {
        notifyStartMesh(appToken, "", false, "Wallet load failed");
    }

    private void notifyStartMesh(String appToken, String address, boolean isSuccess, String message) {
        ViperCommunicator communicator = viperCommunicatorList.get(appToken);
        if (communicator != null) {
            try {
                communicator.onStartTeleMeshService(isSuccess, address, message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void walletCreationProcess(String appToken, ViperCommunicator viperCommunicator) {

        try {

            String walletAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);
            Log.v(TAG, "Creation  " + walletAddress);
            if (TextUtils.isEmpty(walletAddress) || !WalletService.getInstance(getApplicationContext()).isWalletExists()) {
                // User not registered
                if (isWalletCreationEnable(appToken)) {
                    Log.v(TAG, "Creation  not allow");
                    viperCommunicator.onStartTeleMeshService(false, "no_id", "Wallet not found");
                } else {

                    String defaultPass = Utils.ServiceConstant.DEFAULT_PASSWORD;
                    SharedPref.write(Utils.KEY_WALLET_PASS, defaultPass);
                    Log.v(TAG, "attempt to create op 1 :" + defaultPass);
                    WalletService.getInstance(getApplicationContext()).createOrLoadWallet(defaultPass, appToken, TeleMeshService.this);
                }
            } else {
                // User already registered
                String walletPass = SharedPref.read(Utils.KEY_WALLET_PASS);
                Log.v(TAG, "attempt to create op 2  :" + walletPass);
                WalletService.getInstance(getApplicationContext()).createOrLoadWallet(walletPass, appToken, TeleMeshService.this);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
            Log.v(TAG, "wallet creation RemoteException");
        }
    }

    private void startWalletCreationActivity() {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ProfileChoiceActivity.class);
        intent.setAction(ProfileChoiceActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    private void startWalletActivity(byte[] pictureData) {
        WalletManager.openActivity(getApplicationContext(), pictureData);
    }

    private void startDataPlanActivity(String appToken) {
        DataPlanManager.openActivity(getApplicationContext(), appToken);
    }

    private void startMeshLogActivity(Context context) {
        Intent intent = new Intent(context, MeshLogHistoryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void serviceInitiated(String tokenName) {
        ServiceUpdate.getInstance(this).initLinkStateListener(this);
        ServiceUpdate.getInstance(this).checkForUpdate(this);

        String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);

        try {

            if (isMeshDiscoverEnable(tokenName)) {

                List<PeersEntity> peersEntities = getDataBaseService().getAllOnlinePeers(myUserId, tokenName);

                if (peersEntities != null && peersEntities.size() > 0) {
                    List<UserInfo> userInfoList = new ArrayList<>();

                    for (PeersEntity peersEntity : peersEntities) {
                        UserInfo userInfo = getUserInfoByPeersEntity(peersEntity);
                        userInfoList.add(userInfo);
                    }

                    passUserInfoToCommunicator(userInfoList, tokenName);
                }

                pingUserInfoForNewBindApp(tokenName, peersEntities);

            }


            if (isAppDownloadEnable(tokenName)) {
                List<PeersEntity> peersEntities = getDataBaseService().getAllOnlinePeers(myUserId, tokenName);
                if (peersEntities != null && peersEntities.size() > 0) {

                    PeersEntity myPeersEntity = getDataBaseService().getPeersById(myUserId, tokenName);

                    UserInfo userInfo = JsonBuilder.on().formJson(myPeersEntity.getAppUserInfo());

                    for (PeersEntity peersEntity : peersEntities) {
                        // check it is local user or not and online
                        if (transportManagerX != null) {
                            int status = transportManagerX.getLinkTypeById(peersEntity.address);
                            sendUserExternalData(peersEntity.address, (status != 0 && status != RoutingEntity.Type.INTERNET));
                        }
                    }
                }
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        try {

            if (transportManagerX != null) {
                if (isMeshMessageEnable(tokenName)) {

                    ViperCommunicator communicator = getCommunicatorByToken(tokenName);

                    List<Content> pendingContentMessage = databaseService.getAllPendingContent(tokenName);
                    if (pendingContentMessage != null) {
                        for (Content content : pendingContentMessage) {
                            if (communicator != null) {
                                PendingContentInfo pendingContentInfo = new PendingContentInfo();

                                ContentMetaInfo contentMetaInfo = null;
                                try {
                                    contentMetaInfo = ContentMetaGsonBuilder.getInstance()
                                            .prepareContentMetaObj(content.getContentMetaInfo());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                pendingContentInfo.setContentId(content.getContentId());
                                pendingContentInfo.setSenderId(content.getSenderId());
                                pendingContentInfo.setContentPath(content.getContentPath());

                                pendingContentInfo.setProgress(content.getProgress());
                                pendingContentInfo.setState(content.getState());

                                pendingContentInfo.setContentMetaInfo(contentMetaInfo);
                                pendingContentInfo.setIncoming(content.isIncoming());
                                communicator.onPendingFileReceive(pendingContentInfo);

                                try {
                                    if (!content.isIncoming() && content.getState() == stateSuccess) {
                                        databaseService.deleteContentsByContentId(tokenName, content.getContentId());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (contentMetaInfo != null && content.getState() == stateInProgress) {

                                } else {
                                    databaseService.deleteIncomingContentsByContentId(tokenName, content.getContentId(), true);
                                }
                            }
                        }
                    }

                    List<Message> pendingMessages = transportManagerX.getAllIncomingPendingMessage(tokenName);
                    // Todo get correct communicator - tarikul
                    if (pendingMessages != null) {
                        for (Message item : pendingMessages) {
                            if (communicator != null) {
                                communicator.onDataReceived(item.senderId, item.data);
                                transportManagerX.deletePendingMessage(item.messageId);
                            }
                        }
                    }

                    List<BroadcastEntity> broadcastEntities = BroadcastDataHelper.getInstance().getUnreceivedBroadcast(tokenName);

                    if (!broadcastEntities.isEmpty()) {
                        for (BroadcastEntity broadcastEntity : broadcastEntities) {
                            sendDataToApp(broadcastEntity.toBroadcastData(), false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setRefreshWorkerRequest() {

        try {
            ListenableFuture<List<WorkInfo>> forUniqueWork = WorkManager.getInstance().getWorkInfosByTag(TAG_CONFIGURATION_REFRESH_JOB);
            List<WorkInfo> workInfos = forUniqueWork.get();

            if (workInfos == null || workInfos.isEmpty()) {

                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                PeriodicWorkRequest refreshCpnWork =
                        new PeriodicWorkRequest.Builder(RefreshJobWorker.class, 12, TimeUnit.HOURS)
                                .addTag(TAG_CONFIGURATION_REFRESH_JOB)
                                .setConstraints(constraints)
                                .setBackoffCriteria(
                                        BackoffPolicy.LINEAR,
                                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                        TimeUnit.MILLISECONDS).build();

                WorkManager.getInstance().enqueue(refreshCpnWork);
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ITmCommunicator.Stub iTmCommunicator = new ITmCommunicator.Stub() {
        @Override
        public void startTeleMeshService(ViperCommunicator viperCommunicator, String appToken, UserInfo userInfo) throws RemoteException {

            // Client information load first time;
            // We load to the client information first in database
            String clientInfoData = loadJSONFromAsset(getApplicationContext(), CredentialUtils.getClientInfoFile());
            Log.e("Client_config","Config json : "+clientInfoData);
            ClientInfoSyncUtil.getInstance().loadFirstTimeClientInformation(clientInfoData);

            //Todo tariqul we canc check Registration key validity here
            // TODO open this area when the Telemesh or others app SHA value will be added in AppSecurityUtil.isSupportedAppIdAndSha256
            // Security check in release mode
            /*boolean isDebug = ((getApplicationContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
            Log.v(TAG, "setViperCommunicator() successful: " + appToken + " Debug :" + isDebug);
            if (!isDebug) {
                int callerProcessId = Binder.getCallingUid();
                String callerAppId = getApplicationContext().getPackageManager().getNameForUid(callerProcessId);
                Log.v(TAG, "startTeleMeshService() Calling application Id :: " + callerAppId + "  Process id ::" + callerProcessId);
                String shaKey = AppSecurityUtil.getClientAppFingerPrint(getApplicationContext(), AppSecurityUtil.KeyType.SHA256, callerAppId);
                Log.v(TAG, "Sha key : " + shaKey);

                if (TextUtils.isEmpty(shaKey)) {
                    throw new RemoteException("Client app SHA256 key not found");
                }

                // Through exception if app is not permitted
                if (!AppSecurityUtil.isSupportedAppIdAndSha256(getApplicationContext(), callerAppId, shaKey)) {
                    throw new RemoteException("Application is not supported");
                }
            }*/

//            new Delegate().checkAppTamper();

            com.w3engineers.purchase.db.SharedPref.on(getApplicationContext());

            mViperCommunicator = viperCommunicator;
            viperCommunicatorList.put(appToken, viperCommunicator);

            String configurationData = loadJSONFromAsset(getApplicationContext(), CredentialUtils.getConfigurationFile());
            Log.v(TAG, "configurationData : " + configurationData.toString());

            ConfigSyncUtil.getInstance().loadFirstTimeData(getApplicationContext(), configurationData);

            Log.d(TAG, "Setp 1");
            BoundServiceManager.on(getApplicationContext()).startSelfBind(true);


            // This line is responsible to download client information from server
            ClientInfoSyncUtil.getInstance().syncClientInformationFromServer();

            Log.d(TAG, "Setp 2");

            // Check any app update request available or not. When client not live
            processPendingAppUpdateRequest(appToken);

            Log.d(TAG, "Setp 3");

            savePeersInfoInService(userInfo, true);

            Log.d(TAG, "Setp 4");
            //TODO if already running then send back from here
            walletCreationProcess(appToken, viperCommunicator);

            Log.d(TAG, "Setp 5");

            // Init network listener for App update app info section
            AppUpdateAppDataManager.initNetWorkListener();

        }

        @Override
        public void openWalletCreationUI(String appTokenName) throws RemoteException {

            Utils.INTENT_APP_TOKEN = appTokenName;
            Log.v(TAG, "Create wallet page called");
            HandlerUtil.postForeground(() -> {
                startWalletCreationActivity();
            });
        }

        @Override
        public void openDataplanUI(String appToken) throws RemoteException {
            if (isMeshBlockChainEnable(appToken)) {
                startDataPlanActivity(appToken);
            }
        }

        @Override
        public void openWalletUI(String appToken, byte[] pictureData) throws RemoteException {
            if (isMeshBlockChainEnable(appToken)) {
                startWalletActivity(pictureData);
            }
        }

        @Override
        public void openSellerInterfaceUI(String appToken) throws RemoteException {
            if (isMeshBlockChainEnable(appToken)) {

            }
        }

        @Override
        public void openMeshLogUI() throws RemoteException {
            startMeshLogActivity(getApplicationContext());
        }

        @Override
        public void triggerReSyncConfiguration(String appToken) throws RemoteException {

        }

        @Override
        public boolean startMesh(String appToken) {

            String nodeAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);
            String publicKey = SharedPref.read(Utils.KEY_WALLET_PUB_KEY);

            updateSelfUserInfo(nodeAddress, publicKey);
            BroadcastDataHelper.getInstance().setBroadcastCallback(TeleMeshService.this);

            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                try {
                    communicator.onStartTeleMeshService(true, nodeAddress, "Wallet loaded");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            NotifyUtil.removeBroadcastNotification(appToken);
            if (transportManagerX == null) {
                String networkSSID = getApplicationContext().getString(R.string.network_ssid);
                startTransport(nodeAddress, publicKey, networkSSID);
                setRefreshWorkerRequest();
                BroadcastDataHelper.getInstance().failedContentBroadcastForMyDisconnection();
            } else {
                onTransportInit(nodeAddress, publicKey, TransportState.SUCCESS, "Init Success");
            }

            serviceInitiated(appToken);
            updateMyLocation();
//            testappupdateapp();
            return true;
        }

        @Override
        public void onStartForeground(boolean isNeeded) {
            if (isNeeded) {
                startForeGround();
            } else {
                stopForeground(true);
            }
        }

        @Override
        public void sendData(MessageData messageData) {
            dataSendToMesh(messageData.getSenderID(), messageData.getReceiverID(), messageData.getMessageID(),
                    messageData.getMsgData(), messageData.isNotificationNeeded(), messageData.getAppToken());
        }

        @Override
        public int getLinkTypeById(String nodeID) {
            if (transportManagerX != null) {
                return transportManagerX.getLinkTypeById(nodeID);
            }
            return 0;
        }

        @Override
        public void sendAppUpdateRequest(FileData fileData) throws RemoteException {
            if (isAppDownloadEnable(fileData.getAppToken())) {
                if (transportManagerX != null) {
                    String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                    appUpdateRequest(fileData.getReceiverID(), fileData.getAppToken());
                }
            }
        }

        @Override
        public void saveUserInfo(UserInfo userInfo) {
            SharedPref.write(Constant.KEY_USER_ID, userInfo.getAddress());

            savePeersInfoInService(userInfo, true);

            if (transportManagerX != null) {
                transportManagerX.saveUserInfo("");
            }
        }

        @Override
        public void saveOtherUserInfo(UserInfo userInfo) {
            updateOtherUserInfo(userInfo);
        }

        @Override
        public void stopMesh() {
            transportManagerX.stopMesh();
        }

        @Override
        public List<String> getInternetSellers(String appToken) {
            // TODO add mesh config - mimosaha
            if (transportManagerX != null) {
                return transportManagerX.getInternetSellers();
            }
            return new ArrayList<>();
        }

        @Override
        public int getUserMeshRole() throws RemoteException {
            return DataPlanManager.getInstance().getDataPlanRole();
        }

        @Override
        public String getFirstAppToken() throws RemoteException {
            for (Map.Entry<String, ViperCommunicator> stringViperCommunicatorEntry : viperCommunicatorList.entrySet()) {
                return stringViperCommunicatorEntry.getKey();
            }
            return "";
        }

        @Override
        public void restartMesh(int newRole) {

            String networkSSID = getResources().getString(R.string.network_ssid);

            if (transportManagerX == null) {
                MeshLog.v("sellerMode tms transportManagerX is null");

                String address = SharedPref.read(Constant.KEY_USER_ID);
                String publicKey = SharedPref.read(Constant.KEY_PUBLIC_KEY);
                startTransport(address, publicKey, networkSSID);
            } else {
                MeshLog.v("sellerMode tms transportManagerX restart is call");
                transportManagerX.restart(newRole);
            }

        }

        @Override
        public void destroyService() throws RemoteException {
            destroyFullService();
        }

        @Override
        public void allowPermissions(List<String> missingPermissions) throws RemoteException {
            MeshLog.v("allowPermissions");
            transportManagerX.requestPermission(missingPermissions);
        }

        @Override
        public void isLocalUseConnected(String userId) throws RemoteException {
            transportManagerX.isUserAvailable(userId, TeleMeshService.this::onUserConnected);
        }

        @Override
        public String sendFile(FileData fileData) throws RemoteException {
            MeshLog.v("FILE_SPEED_TEST_3 " + Calendar.getInstance().getTime());
            if (mMeshFileCommunicator != null) {
                String fileTransferJsonStr = mMeshFileCommunicator.sendFile(fileData.getReceiverID(), fileData.getFilePath(), fileData.getMsgMetaData(), fileData.getAppToken());
//              fileTransferJsonStr = fileTransferId.equals(String.valueOf(MeshFile.UNKNOWN_FILE_ID)) ? null : fileTransferId;
                contentSendStart(fileTransferJsonStr, fileData.getReceiverID(), fileData.getAppToken(), fileData.getMsgMetaData());
                return fileTransferJsonStr;
            }
            return null;
        }


        @Override
        public String sendInAppUpdateFile(FileData fileData) throws RemoteException {
            if (mMeshFileCommunicator != null) {

                fileData.setFilePath(AppBackupUtil.moveFileToRoot(fileData.getFilePath(), getApplicationContext()));
                String fileTransferId = mMeshFileCommunicator.sendFile(fileData.getReceiverID(), fileData.getFilePath(), fileData.getMsgMetaData(), fileData.getAppToken());
                fileTransferId = fileTransferId.equals(String.valueOf(MeshFile.UNKNOWN_FILE_ID)) ? null : fileTransferId;
                if (fileTransferId != null) {
                    inAppUpdatePathMap.put(fileTransferId, fileData.getFilePath());
                    inAppUpdateAppReceiverMap.put(fileTransferId, fileData.getReceiverID());
                }

                return fileTransferId;
            }
            return null;
        }

        @Override
        public void sendFileResumeRequest(FileData fileData) throws RemoteException {
            if (mMeshFileCommunicator != null) {
                mMeshFileCommunicator.sendFileResumeRequest(fileData.getContentId(), fileData.getAppToken(), fileData.getMsgMetaData());
            }
        }

        @Override
        public void removeSendContent(FileData fileData) throws RemoteException {
            if (databaseService != null) {
                databaseService.deleteContentsByContentId(fileData.getAppToken(), fileData.getContentId());
            }
        }


        @Override
        public void sendLocalBroadcast(BroadcastData broadcastData) throws RemoteException {
            BroadcastDataHelper.getInstance().sendBroadCastMessage(broadcastData);

        }

        @Override
        public int checkUserConnectivityStatus(String userId) throws RemoteException {
            if (transportManagerX != null) {
                if (transportManagerX.isOnline(userId)) {
                    return transportManagerX.getTypeById(userId);
                }
            }
            return 0;
        }

        @Override
        public boolean isNetworkOnline() throws RemoteException {
            return NetworkMonitor.isOnline();
        }

        @Override
        public Network getNetwork() throws RemoteException {
            return NetworkMonitor.getNetwork();
        }
    };

    @Override
    public int getPeerConnectionType(String peerId) {
        if (transportManagerX != null) {
            return transportManagerX.getLinkTypeById(peerId);
        }
        return 0;
    }

    @Override
    public TransportManagerX getTransPort() {
        return transportManagerX;
    }

    @Override
    public boolean isDirectConnected(String peerId) {
        if (transportManagerX != null) {
            if (transportManagerX.isOnline(peerId)) {
                int connectionType = transportManagerX.getTypeById(peerId);
                return connectionType == RoutingEntity.Type.BLE || connectionType == RoutingEntity.Type.WiFi;
            }
        }
        return false;
    }

    private void destroyFullService() {

        try {
            databaseService.updateAllContentStatus(stateInProgress, stateFailed);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mViperCommunicator != null) {
            try {
                mViperCommunicator.destroyFullService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        BoundServiceManager.on(getApplicationContext()).unBindService();
        stopForeground(true);
        stopSelf();

        Process.killProcess(Process.myPid());
    }

    private void startTransport(String address, String publicKey, String networkSSID) {
        if (transportManagerX == null) {
            Log.v(TAG, "FRESH START of Service onCreate");
            /*transportManagerX = TransportManagerX.on(getApplicationContext(), APP_PORT, address, publicKey, userInfo,
                    networkSSID, CredentialUtils.getSignalServerLink(), this);
            transportManagerX.startMesh();*/

            databaseService = com.w3engineers.purchase.db.DatabaseService
                    .getInstance(getApplicationContext());

            transportManagerX = SupportTransportManager.getInstance().getTransportManager(
                    getApplicationContext(), APP_PORT, address, publicKey,
                    networkSSID, CredentialUtils.getSignalServerLink(), this);

            transportManagerX.startMesh();

            // File communicator for file messaging
            mMeshFileCommunicator = SupportTransportManager.getInstance().getMeshFileCommunicator();

            // File status listener
            mMeshFileCommunicator.setEventListener(this);

        } else {
            Log.v(TAG, "Restart of Service onCreate");
            transportManagerX.restart();
        }
    }

    /**
     * <h1>Build notification to make OS happy to remain service alive </h1>
     */
    private void buildNotification() {

        String stopServiceText = "Stop Service";
        // Make notification show big text.
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(getResources().getString(R.string.noti_content_title));
        bigTextStyle.bigText(getResources().getString(R.string.stop_service));

        Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        // Add Play button intent in notification.
        Intent stopServiceIntent = new Intent(this, TeleMeshService.class);
        stopServiceIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent pendingStopServiceIntent = PendingIntent.getService(this, 0, stopServiceIntent, 0);
        NotificationCompat.Action stopServiceAction = new NotificationCompat.Action
                (com.w3engineers.mesh.R.drawable.ic_rm_off_black_24dp, stopServiceText, pendingStopServiceIntent);

        // Create notification builder.
        NotificationCompat.Builder builder = NotificationUtil.getBuilder(this);
        builder.setContentTitle(getResources().getString(R.string.service_back)).setWhen(System.currentTimeMillis()).
                setSmallIcon(com.w3engineers.mesh.R.drawable.ic_rm_notif_icon_black_24dp).setLargeIcon(largeIconBitmap).
                addAction(stopServiceAction).setStyle(bigTextStyle).
                setContentText(getResources().getString(R.string.expand_service)).
                setAutoCancel(false);//This one can be true as Activity Pause/Resume would re appear
        // the notification normally unless developer modify the behavior

        // Build the notification.
        notification = builder.build();

        // Start foreground service.
        startForeGround();
    }

    /**
     * <p>Purpose to show bound service notification
     * and able to call from manager</p>
     */
    public void startForeGround() {
        startForeground(1, notification);
    }

    /**
     * <p>Purpose to remove bound service notification
     * and able to call from manager</p>
     */
    public void stopForeGround() {
        stopForeground(true);
    }

    public void dataSendToMesh(String senderId, String receiverId, String messageId, byte[] data, boolean isNeeded, String appToken) {
        try {

                /*int caller = Binder.getCallingUid();
                String callerId = getApplicationContext().getPackageManager().getNameForUid(caller);
                Log.e(TAG, "startTeleMeshService() Calling application Id :: "+callerId+"  Process id ::"+caller);
                String shaKey = ShaKeyUtil.getAppFingerPrint(getApplicationContext(), "SHA256", callerId);
                Log.e(TAG, "Sha key : "+shaKey);*/

            String plainString = new String(data);
            String userPublicKey = getUserPublicKeyFromDb(receiverId);

            if (!TextUtils.isEmpty(userPublicKey)) {
                MeshLog.v("Before encryption " + plainString);
                String encryptedMessage = CryptoHelper.encryptMessage(WalletService.getInstance(getApplicationContext()).getPrivateKey(), userPublicKey, plainString);
                MeshLog.v("Encrypted message " + encryptedMessage);
                if (isMeshMessageEnable(appToken)) {

                    JSONObject jo = new JSONObject();
                    if (isNeeded) {
                        jo.put(KEY_NOTIFY, NOTIFY);
                        messageIdList.add(messageId);
                    } else {
                        jo.put(KEY_NOTIFY, NOT_NOTIFY);
                    }

                    jo.put(Constant.KEY_APP_TOKEN, appToken);
                    jo.put(KEY_MSG, encryptedMessage);

                    transportManagerX.sendTextMessage(senderId, receiverId, messageId, jo.toString().getBytes());
                }
            } else {
                MeshLog.v("User public key not found " + senderId);
            }
        } catch (JSONException e) {
            HandlerUtil.postForeground(() -> showToast("JSON exception on send msg"));
        }
    }

    @Override
    public void sendDataToApp(BroadcastData broadcastData, boolean isReceivedMode) {
        ViperCommunicator communicator = getCommunicatorByToken(broadcastData.getAppToken());

        if (communicator != null) {
            try {
                communicator.receiveBroadcast(broadcastData);
                BroadcastDataHelper.getInstance().updateReceivedBroadcast(broadcastData.getBroadcastId());
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                viperCommunicatorList.remove(broadcastData.getAppToken());
            }
        } else {
            viperCommunicatorList.remove(broadcastData.getAppToken());
        }
        if (isReceivedMode) {
            NotifyUtil.showBroadcastNotification(broadcastData.getAppToken());
        }
    }


    /******************************************************************************/
    /*************************** Library Call back ********************************/
    /******************************************************************************/

    @Override
    public void onTransportInit(String nodeId, String publicKey, TransportState
            transportState, String msg) {
        MeshLog.v("onTransportInit called");
        // FIXME optimize this for supporting multiple apps
        initPaymentSection();

        try {
            if (mViperCommunicator != null) {
                mViperCommunicator.onTeleServiceStarted(transportState == TransportState.SUCCESS, nodeId, publicKey, msg);
            } else {
                MeshLog.e("mViperCommunicator NULL");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocalUserConnected(String nodeId, String publicKey) {

        Log.v(TAG, "PING_PROCESS (1): " + nodeId);

        startGeneralAction(nodeId, true);
    }

    private void onLocalUserProcess(String nodeId, UserInfo userInfoObj, String tokenName) {

        if (userInfoObj == null || !nodeId.startsWith("0x")) {
            MeshLog.v("Received wrong user info local");
            return;
        }

        passUserInfoToCommunicator(userInfoObj, tokenName);
        savePeersInfoInService(userInfoObj, false);
        BroadcastDataHelper.getInstance().pingBroadcastHandshaking(userInfoObj.getAddress(), tokenName);
    }

    public void passUserInfoToCommunicator(UserInfo userInfoObj, String appToken) {

        List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfoObj);

        passUserInfoToCommunicator(userInfoList, appToken);
    }

    public void passUserInfoToCommunicator(List<UserInfo> userInfoList, String appToken) {

        ViperCommunicator communicator = getCommunicatorByToken(appToken);

        if (communicator != null) {
            try {
                communicator.onUserInfoReceive(userInfoList);
                MeshLog.e("User info receive : and send to viper:: " + userInfoList.get(0).getUserName());
            } catch (RemoteException e) {
                e.printStackTrace();
                MeshLog.e("User info receive exception: " + userInfoList.get(0).getUserName());
                viperCommunicatorList.remove(appToken);
            }
        } else {
            viperCommunicatorList.remove(appToken);
        }
    }

    @Override
    public void onRemoteUserConnected(String nodeId, String publicKey) {

        startGeneralAction(nodeId, false);
    }

    @Override
    public void onUserDisconnected(String nodeId) {

        liveUserIds.remove(nodeId);
        nodeMap.remove(nodeId);

        getDataBaseService().updatePeerStatus(nodeId, false);

        try {
            databaseService.updateContentStatusByUserId(nodeId, stateInProgress, stateFailed);
        } catch (Exception e) {
            e.printStackTrace();
        }

        MeshLog.e("Remote User disconnected =" + nodeId);
        Iterator it = viperCommunicatorList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            ViperCommunicator communicator = (ViperCommunicator) pair.getValue();
            try {
                if (communicator != null) {
                    communicator.onPeerRemoved(nodeId);
                } else {
                    viperCommunicatorList.remove(pair.getKey().toString());
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                viperCommunicatorList.remove(pair.getKey().toString());
            }
        }
        BroadcastDataHelper.getInstance().failedContentBroadcastForDisconnect(nodeId);
    }

    @Override
    public void onMessageReceived(String senderId, byte[] data) {
        MeshLog.e("Message received from =" + senderId);
        String stringMsg = new String(data);

        MeshLog.v("PING_PROCESS (3): Message " + stringMsg);

        try {
            JSONObject jo = new JSONObject(stringMsg);

            int needToNotify = jo.getInt(KEY_NOTIFY);

            String token = jo.getString(Constant.KEY_APP_TOKEN);
            String message = jo.getString(KEY_MSG);

            MeshLog.v("Before decryption " + message);

            String userPublicKey = getUserPublicKeyFromDb(senderId);

            if (!TextUtils.isEmpty(userPublicKey)) {
                String decryptedMessage = CryptoHelper.decryptMessage(WalletService.getInstance(getApplicationContext()).getPrivateKey(), userPublicKey, message);
                MeshLog.v("Decrypted message " + decryptedMessage);

                if (isMeshMessageEnable(token)) {
                    if (needToNotify == NOTIFY) {
                        int totalMsgRcv = SharedPref.readInt(Constant.PreferenceKeys.TOTAL_MESSAGE_RCV);
                        SharedPref.write(Constant.PreferenceKeys.TOTAL_MESSAGE_RCV, totalMsgRcv + 1);
                    }
                    try {
                        ViperCommunicator communicator = getCommunicatorByToken(token);
                        if (communicator != null) {
                            communicator.onDataReceived(senderId, decryptedMessage.getBytes());
                        } else {
                            transportManagerX.savePendingMessage(senderId, decryptedMessage.getBytes(), token);
                            viperCommunicatorList.remove(token);
                            if (needToNotify == NOTIFY) {

                                PeersEntity peersEntity = getDataBaseService().getPeersById(senderId, token);
                                if (peersEntity != null) {
                                    UserInfo userInfo = getUserInfoByPeersEntity(peersEntity);
                                    NotifyUtil.showNotification(userInfo);
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        viperCommunicatorList.remove(token);
                        transportManagerX.savePendingMessage(senderId, decryptedMessage.getBytes(), token);
                        try {
                            if (needToNotify == NOTIFY) {

                                PeersEntity peersEntity = getDataBaseService().getPeersById(senderId, token);
                                if (peersEntity != null) {
                                    UserInfo userInfo = getUserInfoByPeersEntity(peersEntity);
                                    NotifyUtil.showNotification(userInfo);
                                }
                            }
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }


            } else {
                MeshLog.v("User public not found");
            }
        } catch (JSONException e) {
            e.printStackTrace();
//            HandlerUtil.postForeground(() -> showToast("JSON exception on receive msg"));
        }

    }

//    @Override
//    public String onBalanceVerify(long fileSize, List<String> hopList) {
//        return PurchaseManagerBuyer.getInstance().canSendFile(fileSize, hopList);
//    }

    @Override
    public void onMessageDelivered(String messageId, int status) {
      /*  MeshLog.e("Message delivered id =" + messageId);
        try {
            JSONObject obj = new JSONObject(messageId);
            if (status == Constant.MessageStatus.RECEIVED) {
                if (messageIdList.contains(messageId)) {
                    int totalMsgSent = SharedPref.readInt(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT);
                    SharedPref.write(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT, totalMsgSent + 1);
                }
            }

            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                communicator.onAckReceived(messageIdJson, status);
            }
        } catch (RemoteException | JSONException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onMessageDelivered(String messageId, int status, String appToken) {
        MeshLog.e("Message delivered id =" + messageId + "  " + appToken);
        try {
            if (status == Constant.MessageStatus.RECEIVED) {
                if (messageIdList.contains(messageId)) {
                    int totalMsgSent = SharedPref.readInt(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT);
                    SharedPref.write(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT, totalMsgSent + 1);
                }
            }

            if (isMeshMessageEnable(appToken)) {
                ViperCommunicator communicator = getCommunicatorByToken(appToken);
                if (communicator != null) {
                    MeshLog.v("send message to aidl");
                    communicator.onAckReceived(messageId, status);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProbableSellerDisconnected(String sellerId) {

    }

    @Override
    public void onMessagePayReceived(String sender, byte[] paymentData) {

    }

    @Override
    public void onPayMessageAckReceived(String sender, String receiver, String messageId) {

    }

    @Override
    public void buyerInternetMessageReceived(String sender, String receiver, String messageId,
                                             String messageData, long dataLength, boolean isIncoming, boolean isFile) {

    }

//    @Override
//    public void onCurrentSellerId(String sellerId) {
//
//    }

    /*@Override
    public void onLogTextReceive(String text) throws RemoteException {
        if (mViperCommunicator != null) {
            mViperCommunicator.onReceiveLog(text);
        }
    }*/

    public void onServiceApkDownloadNeeded(boolean isNeeded) throws RemoteException {
        MeshLog.v("onServiceApkDownloadNeeded " + isNeeded);
        // TODO add mesh config - mimosaha

        // Here we send the app update information to all bounded application
        for (Map.Entry<String, ViperCommunicator> stringViperCommunicatorEntry : viperCommunicatorList.entrySet()) {

            ViperCommunicator communicator = (ViperCommunicator) ((Map.Entry) stringViperCommunicatorEntry).getValue();
            if (communicator != null) {
                try {
                    communicator.onServiceUpdateNeeded(isNeeded);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    viperCommunicatorList.remove(((Map.Entry) stringViperCommunicatorEntry).getKey().toString());
                }
            } else {
                viperCommunicatorList.remove(((Map.Entry) stringViperCommunicatorEntry).getKey().toString());
            }
        }
    }

    @Override
    public void onHandshakeInfoReceived(HandshakeInfo handshakeInfo) {

        int type = handshakeInfo.getType();

        switch (type) {
            case JsonDataBuilder.HANDSHAKE_PING:
                processUserInfo(handshakeInfo);
                break;

            case JsonDataBuilder.HANDSHAKE_BROADCAST:
                BroadcastDataHelper.getInstance().processBroadcastHandshaking(handshakeInfo);
                break;

            case JsonDataBuilder.HANDSHAKE_INFO:
                processUserExternalData(handshakeInfo);
                break;

            case JsonDataBuilder.HANDSHAKE_CONFIG:
                processClientAppConfiguration(handshakeInfo);
                break;
        }
    }

    @Override
    public void onBroadcastMessageReceive(Broadcast broadcast) {
        Log.v("MIMO_SAHA:", "Broadcast Receive");

        sendBroadcastReceivedACK(broadcast.getBroadcastId(), broadcast.getSenderId());
        BroadcastDataHelper.getInstance().onBroadcastReceive(broadcast.getBroadcastId());
    }

    @Override
    public void onBroadcastACKMessageReceived(BroadcastAck broadcastAck) {
        BroadcastDataHelper.getInstance().broadcastAckReceive(broadcastAck, false);
    }

    private void sendBroadcastReceivedACK(String broadcastId, String senderId) {
        BroadcastDataHelper.getInstance().broadcastAckSend(broadcastId, senderId);
    }

    @Override
    public boolean onBroadcastSaveAndExist(Broadcast broadcast) {
        return BroadcastDataHelper.getInstance().onReceiveBroadcastAndExistCheck(broadcast);
    }

    @Override
    public void onReceivedAckSend(String broadcastID, String senderId) {
        sendBroadcastReceivedACK(broadcastID, senderId);
    }

    @Override
    public void onInterruption(int details) {
        // Todo We have to send info to all app?
        try {
            if (mViperCommunicator != null) {
                mViperCommunicator.onInterruption(details, null);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        /*if(details == LinkStateListener.LOCATION_PROVIDER_OFF) {

            transportManagerX.requestPermission();

        }*/ /*else if(mMeshStateListener != null) {
            mMeshStateListener.onInterruption(details);
        }*/
    }

    @Override
    public void onInterruption(List<String> missingPermissions) {
        MeshLog.v("onInterruption service " + missingPermissions);
        try {
            // Todo we have to send info to all App?
            if (mViperCommunicator != null) {
                mViperCommunicator.onInterruption(-1, missingPermissions);
            } else {
                MeshLog.v("mViperCommunicator service null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
//        transportManagerX.requestPermission(missingPermissions);
    }

    private void processOthersAppVersion(String senderId, String appToken, int version,
                                         String versionName, String appSize) {

        //Todo need to update version name and size in Database
        getDataBaseService().updatePeerAppVersion(senderId, appToken, version);

        try {

            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                FileData fileData = new FileData().setReceiverID(senderId).setAppVersion(version)
                        .setVersionName(versionName).setAppSize(appSize);

                communicator.receiveOtherAppVersion(fileData);

                String myAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                AppInfoUtil.processClientAppInformation(getApplicationContext(), myAddress, senderId,
                        appToken, version, versionName);
            } else {
                String appName = AppInfoUtil.getAppNameWhenVersionLow(getApplicationContext(), appToken, version);
                if (appName != null) {
                    AppUpdateAppInfoModel model = new AppUpdateAppInfoModel();
                    model.packageName = appToken;
                    model.senderId = senderId;
                    model.version = version;
                    model.versionName = versionName;
                    model.appSize = appSize;
                    inAppUpdateDataMap.put(appToken, model);

                    com.w3engineers.mesh.util.NotificationUtil
                            .showClientAppUpdateAvailableNotification(getApplicationContext(), appToken, appName);
                }
            }
        } catch (RemoteException e) {
            viperCommunicatorList.remove(appToken);
            e.printStackTrace();
        }
    }

    private void updateSelfUserInfo(String walletAddress, String publicKey) {

        try {
            List<PeersEntity> peersEntities = getDataBaseService().getSelfPeers();

            if (peersEntities != null && !peersEntities.isEmpty()) {

                for (int i = 0; i < peersEntities.size(); i++) {

                    boolean isUpdated = false;
                    PeersEntity peersEntity = peersEntities.get(i);
                    String userInfoText = "";

                    String oldAddress = peersEntity.getAddress();
                    String oldAppToken = peersEntity.getAppToken();

                    if (!TextUtils.equals(peersEntity.getAddress(), walletAddress) && !TextUtils.isEmpty(walletAddress)) {
                        peersEntity.setAddress(walletAddress);
                        isUpdated = true;
                    }

                    if (!TextUtils.equals(peersEntity.getPublicKey(), publicKey) && !TextUtils.isEmpty(publicKey)) {
                        peersEntity.setPublicKey(publicKey);
                        isUpdated = true;
                    }

                    if (isUpdated) {

                        userInfoText = peersEntity.getAppUserInfo();

                        UserInfo userInfo = JsonBuilder.on().formJson(userInfoText);

                        userInfo.setAddress(walletAddress);
                        userInfo.setPublicKey(publicKey);

                        userInfoText = JsonBuilder.on().toJson(userInfo);
                        peersEntity.setAppUserInfo(userInfoText);
                    }

                    if (isUpdated && !TextUtils.isEmpty(userInfoText)) {

                        getDataBaseService().deleteOldSelfInfo(oldAddress, oldAppToken);
                        getDataBaseService().insertPeersEntity(peersEntity);
                    }
                }
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void savePeersInfoInService(UserInfo userInfo, boolean isMe) {
        try {
            String userAddress = userInfo.getAddress();
            String userToken = userInfo.getAppToken();

            PeersEntity peersEntity = getDataBaseService().getPeersById(userAddress, userToken);

            if (peersEntity == null) {
                peersEntity = new PeersEntity();
                if (isMe) {
                    userInfo.setRegTime(System.currentTimeMillis());
                }
            }

            if (TextUtils.isEmpty(userInfo.getAddress()) && isMe) {

                String walletAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                if (TextUtils.isEmpty(walletAddress)) {
                    walletAddress = userInfo.getAppToken();
                }

                userInfo.setAddress(walletAddress);
            }

            // User address/id
            if (!TextUtils.equals(peersEntity.getAddress(), userInfo.getAddress())
                    && !TextUtils.isEmpty(userInfo.getAddress())) {
                peersEntity.setAddress(userInfo.getAddress());
            }

            // User app token
            if (!TextUtils.equals(peersEntity.getAppToken(), userInfo.getAppToken())
                    && !TextUtils.isEmpty(userInfo.getAppToken())) {
                peersEntity.setAppToken(userInfo.getAppToken());
            }

            // User public key
            if (!TextUtils.equals(peersEntity.getPublicKey(), userInfo.getPublicKey())
                    && !TextUtils.isEmpty(userInfo.getPublicKey())) {
                peersEntity.setPublicKey(userInfo.getPublicKey());
            }

            if (peersEntity.getUserLatitude() != userInfo.getLatitude()) {
                peersEntity.setUserLatitude(userInfo.getLatitude());
            }

            if (peersEntity.getUserLongitude() != userInfo.getLongitude()) {
                peersEntity.setUserLongitude(userInfo.getLongitude());
            }

            if (!TextUtils.equals(peersEntity.getPublicKey(), userInfo.getPublicKey())
                    && !TextUtils.isEmpty(userInfo.getPublicKey())) {
                peersEntity.setPublicKey(userInfo.getPublicKey());
            }

            peersEntity.setMe(isMe);
            peersEntity.setOnlineStatus(true);

            // User info
            String userInfoText = JsonBuilder.on().toJson(userInfo);

            if (!TextUtils.equals(peersEntity.getAppUserInfo(), userInfoText)
                    && !TextUtils.isEmpty(userInfoText)) {
                peersEntity.setAppUserInfo(userInfoText);
            }

            getDataBaseService().insertPeersEntity(peersEntity);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();

            MeshLog.e("User info insert exception: " + userInfo.getUserName());
        }
    }

    public void updateMyLocationAndShareIntoMesh(double latitude, double longitude) {
        try {
            List<PeersEntity> peersEntities = getDataBaseService().getSelfPeers();

            double oldLatitude = 0, oldLongitude = 0;

            if (peersEntities != null && !peersEntities.isEmpty()) {

                for (int i = 0; i < peersEntities.size(); i++) {

                    boolean isUpdated = false;
                    PeersEntity peersEntity = peersEntities.get(i);
                    String userInfoText = "";

                    oldLatitude = peersEntity.getUserLatitude();
                    oldLongitude = peersEntity.getUserLongitude();

                    if (peersEntity.getUserLatitude() != latitude && latitude > 0) {
                        peersEntity.setUserLatitude(latitude);
                        isUpdated = true;
                    }

                    if (peersEntity.getUserLongitude() != longitude && longitude > 0) {
                        peersEntity.setUserLongitude(longitude);
                        isUpdated = true;
                    }

                    if (isUpdated) {

                        userInfoText = peersEntity.getAppUserInfo();

                        UserInfo userInfo = JsonBuilder.on().formJson(userInfoText);

                        userInfo.setLatitude(latitude);
                        userInfo.setLongitude(longitude);

                        userInfoText = JsonBuilder.on().toJson(userInfo);
                        peersEntity.setAppUserInfo(userInfoText);
                    }

                    if (isUpdated) {
                        getDataBaseService().insertPeersEntity(peersEntity);
                    }
                }
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Ping mechanism and receive user data

    private void pingUserInfoForNewBindApp(String tokenName, List<PeersEntity> peersEntities) {
        ArrayList<String> backUpLiveUser = new ArrayList<>(liveUserIds);

        if (peersEntities != null && !peersEntities.isEmpty()) {
            for (PeersEntity peersEntity : peersEntities) {
                backUpLiveUser.remove(peersEntity.getAddress());
            }
        }

        for (String userId : backUpLiveUser) {
            pingHandshaking(userId, tokenName);
        }
    }

    private void processUserInfoById(String userId, boolean isLocalDiscover) {
        try {

            if (!liveUserIds.contains(userId)) {
                liveUserIds.add(userId);
            }

            getDataBaseService().updatePeerStatus(userId, true);

            for (String tokenName : viperCommunicatorList.keySet()) {

                if (isMeshDiscoverEnable(tokenName)) {

                    PeersEntity peersEntity = getDataBaseService().getPeersById(userId, tokenName);

                    MeshLog.v("PING_PROCESS (12): token " + userId);
                    MeshLog.v("PING_PROCESS (2): token " + tokenName + " peers: " + peersEntity);

                    if (peersEntity == null || TextUtils.isEmpty(peersEntity.getAppToken())) {
                        pingHandshaking(userId, tokenName);
                    } else {

                        UserInfo userInfo = getUserInfoByPeersEntity(peersEntity);
                        onLocalUserProcess(userId, userInfo, tokenName);
                    }

                }
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*private void requestForHmac(String receiverId, String publicKey, boolean isLocal) {
        String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);

        if (TextUtils.isEmpty(myUserId))
            return;

        try {

            String messageId = UUID.randomUUID().toString();

            DiscoveredNodeInfo discoveredNodeInfo = new DiscoveredNodeInfo()
                    .setPubKey(publicKey)
                    .setLocal(isLocal);

            nodeMap.put(receiverId, discoveredNodeInfo);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_USER_PING, true);
            jsonObject.put(KEY_HMAC_AUTH, true);

            Log.d(TAG, "Hmac checking - " + receiverId);
            //transportManagerX.sendMessage(myUserId, receiverId, messageId, jsonObject.toString().getBytes());
            transportManagerX.sendHmacRequest(myUserId, receiverId, true);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }*/

    /*private void processHmacRequest(String requesterId) {
        String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);

        if (TextUtils.isEmpty(myUserId))
            return;

        try {

            String messageId = UUID.randomUUID().toString();
            String address = SharedPref.read(Utils.KEY_WALLET_PUB_KEY);
            String hmacValue = HmacGeneration.getInstance().prepareSignature(this, address);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_USER_PING, true);
            jsonObject.put(KEY_HMAC_RES, hmacValue);

            transportManagerX.sendMessage(myUserId, requesterId, messageId, jsonObject.toString().getBytes());

            transportManagerX.sendHmacResponse(myUserId, requesterId, hmacValue);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /*private void responseForHmac(String nodeId, String hmacValue) {
        if (!TextUtils.isEmpty(hmacValue)) {
            DiscoveredNodeInfo discoveredNodeInfo = nodeMap.get(nodeId);
            if (discoveredNodeInfo != null) {
                try {
                    String generateHmac = HmacGeneration.getInstance().prepareSignature(this,
                            discoveredNodeInfo.getPubKey());

                    if (!TextUtils.isEmpty(generateHmac)) {
                        if (generateHmac.equals(hmacValue)) {
                            startGeneralAction(nodeId, discoveredNodeInfo.getPubKey(),
                                    discoveredNodeInfo.isLocal());
                            Log.d(TAG, "Hmac valid - " + nodeId);
                            return;
                        } else {
                            Log.d(TAG, "Hmac invalid - " + nodeId);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Log.e(TAG, "Hmac checking interrupt - " + nodeId);
    }*/

    private void startGeneralAction(String nodeId, boolean isLocal) {
        processUserInfoById(nodeId, isLocal);
        sendPendingMessage(nodeId);
        sendUserExternalData(nodeId, isLocal);
    }

    private void sendPendingMessage(String nodeId) {
        HandlerUtil.postBackground(() -> {
            // Do something after 5s = 5000ms
            try {
                List<Message> messageList = transportManagerX.getAllOutgoingPendingMessage(nodeId);

                String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);

                if (messageList != null && messageList.size() > 0) {
                    for (Message message : messageList) {

                        String senderId = message.getSenderId();
                        String receiverId = message.getReceiverId();

                        if (senderId.equals(myUserId) && receiverId.equals(nodeId)) {

                            transportManagerX.sendTextMessage(senderId, receiverId,
                                    message.getMessageId(), message.getData());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    ////////////////////// User Ping //////////////////////////

    private void pingHandshaking(String receiverId, String token) {

        String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);

        if (TextUtils.isEmpty(myUserId))
            return;

        HandshakeInfo handshakeInfo = new HandshakeInfo(myUserId, receiverId, receiverId, JsonDataBuilder.HANDSHAKE_PING);
        handshakeInfo.setAppToken(token);

        transportManagerX.sendHandshakeInfo(handshakeInfo, true);
    }

    private void userInfoHandshaking(String receiverId, String tokenName) {

        String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);

        if (TextUtils.isEmpty(myUserId))
            return;

        try {
            PeersEntity peersEntity = getDataBaseService().getPeersById(myUserId, tokenName);

            if (peersEntity == null)
                return;

            UserInfo userInfo = getUserInfoByPeersEntity(peersEntity);
            String myUserInfo = JsonBuilder.on().toJson(userInfo);

            HandshakeInfo handshakeInfo = new HandshakeInfo(myUserId, receiverId, receiverId, JsonDataBuilder.HANDSHAKE_PING);
            handshakeInfo.setAppToken(tokenName);
            handshakeInfo.setProfileData(myUserInfo);

            transportManagerX.sendHandshakeInfo(handshakeInfo, true);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processUserInfo(HandshakeInfo handshakeInfo) {

        if (isMeshDiscoverEnable(handshakeInfo.getAppToken())) {

            if (TextUtils.isEmpty(handshakeInfo.getProfileData())) {
                userInfoHandshaking(handshakeInfo.getSenderId(), handshakeInfo.getAppToken());

            } else {
                UserInfo userInfoObj = JsonBuilder.on().formJson(handshakeInfo.getProfileData());
                onLocalUserProcess(handshakeInfo.getSenderId(), userInfoObj, handshakeInfo.getAppToken());
            }
        }
    }

    ////////////////////// User Config //////////////////////////

    private void sendUserExternalData(String receiverId, boolean isLocalDiscover) {

        try {

            if (!isLocalDiscover)
                return;

            HandshakeTrackEntity prevHandshakeTrackEntity = databaseService.getHandshakeTrackData(receiverId);
            if (prevHandshakeTrackEntity != null) {
                long lastUpdateTime = prevHandshakeTrackEntity.handshakeTime;
                long timeDiff = System.currentTimeMillis() - lastUpdateTime;
                if (timeDiff < Constant.HANDSHAKE_TIME_DIFFERENCE) {
                    return;
                }
            }

            HandshakeTrackEntity handshakeTrackEntity = new HandshakeTrackEntity();
            handshakeTrackEntity.userId = receiverId;
            handshakeTrackEntity.handshakeTime = System.currentTimeMillis();
            databaseService.insertHandshakeTrack(handshakeTrackEntity);

            String myAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);
            List<PeersEntity> peersEntities = getDataBaseService().getSelfPeers();

            HandshakeInfo handshakeInfo = new HandshakeInfo(myAddress, receiverId, receiverId, JsonDataBuilder.HANDSHAKE_INFO);
            List<AppVersion> appVersions = new ArrayList<>();

            for (PeersEntity peersEntity : peersEntities) {
                UserInfo userInfo = JsonBuilder.on().formJson(peersEntity.getAppUserInfo());

                AppVersion appVersion = new AppVersion(userInfo.getVersionCode(), userInfo.getVersionName(),
                        userInfo.getAppSize(), userInfo.getAppToken());

                appVersions.add(appVersion);
            }

            AppVersion appVersion = new AppVersion(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME,
                    "", getApplicationContext().getPackageName());

            appVersions.add(appVersion);

            int configVersion = PreferencesHelperDataplan.on().getConfigVersion();

            handshakeInfo.setAppData(appVersions);
            handshakeInfo.setLatitude(peersEntities.get(0).userLatitude);
            handshakeInfo.setLongitude(peersEntities.get(0).userLongitude);
            handshakeInfo.setClientAppConfigVersion(configVersion);

            String handshakeInfoText = GsonUtil.on().handshakeInfoToString(handshakeInfo);

            handshakeTrackEntity.userId = receiverId;
            handshakeTrackEntity.handshakeData = handshakeInfoText;
            databaseService.insertHandshakeTrack(handshakeTrackEntity);

            transportManagerX.sendHandshakeInfo(handshakeInfo, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appUpdateRequest(String receiverId, String token) {
        String myAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);

        HandshakeInfo handshakeInfo = new HandshakeInfo(myAddress, receiverId, receiverId, JsonDataBuilder.HANDSHAKE_INFO);
        handshakeInfo.setAppToken(token);

        transportManagerX.sendHandshakeInfo(handshakeInfo, false);
    }

    private void sendClientAppConfiguration(String receiverId, String configurationData) {
        String myAddress = SharedPref.read(Utils.KEY_NODE_ADDRESS);
        String credentialsData = PreferencesHelperDataplan.on().getConfigData();

        HandshakeInfo handshakeInfo = new HandshakeInfo(myAddress, receiverId, receiverId, JsonDataBuilder.HANDSHAKE_CONFIG);
        handshakeInfo.setCredentialsConfig(credentialsData);
        handshakeInfo.setClientAppConfig(configurationData);

        transportManagerX.sendHandshakeInfo(handshakeInfo, false);
    }

    private void processUserExternalData(HandshakeInfo handshakeInfo) {
        try {

            String senderId = handshakeInfo.getSenderId();
            String appToken = handshakeInfo.getAppToken();

            if (TextUtils.isEmpty(appToken)) {
                List<PeersEntity> peersEntities = getDataBaseService().getAllPeerById(senderId);

                Double latitude = handshakeInfo.getLatitude();
                Double longitude = handshakeInfo.getLongitude();

                for (PeersEntity peersEntity : peersEntities) {
                    if (latitude != null) {
                        peersEntity.setUserLatitude(latitude);
                    }
                    if (longitude != null) {
                        peersEntity.setUserLongitude(longitude);
                    }
                    getDataBaseService().insertPeersEntity(peersEntity);
                }

                List<AppVersion> appVersions = handshakeInfo.getAppData();

                if (appVersions != null) {
                    for (AppVersion appVersion : appVersions) {
                        processVersionMessage(appVersion, senderId, handshakeInfo.getReceiverId());
                    }
                }

                Integer version = handshakeInfo.getClientAppConfigVersion();

                if (version != null) {

                    if (version < ClientInfoSyncUtil.getInstance().getMyClientInfoVersion()) {
                        String clientsData = ClientInfoSyncUtil.getInstance().getAllClientInfo();
                        sendClientAppConfiguration(senderId, clientsData);
                    }
                }
            } else {
                processAppDataRequest(appToken, senderId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processVersionMessage(AppVersion appVersion, String sender, String receiver) {
        int versionCode = appVersion.getVersionCode();
        String appToken = appVersion.getAppToken();
        String versionName = appVersion.getVersionName();
        String appSize = appVersion.getAppSize();

        if (appToken.equals(getApplicationContext().getPackageName())) {
            HandlerUtil.postForeground(() -> {
                // Service app receive a version
                if (isNeedToUpdate(appVersion.getVersionCode())) {
                    // Show a alert dialog that a new version available
                    if (TeleMeshServiceMainActivity.getInstance() != null) {
                        showInAppUpdateDialog(TeleMeshServiceMainActivity.getInstance(), sender);
                    } else {
                        com.w3engineers.mesh.util.NotificationUtil.showUpdateConfirmationNotification(
                                getApplicationContext(), receiver, sender);
                    }
                }
            });
        } else {
            if (isAppDownloadEnable(appToken)) {
                processOthersAppVersion(sender, appToken, versionCode, versionName, appSize);
            }
        }
    }

    private void processAppDataRequest(String appToken, String sender) {

        if (appToken.equals(getApplicationContext().getPackageName())) {

            String apkPath = AppBackupUtil.backupApkAndGetPath(getApplicationContext());
            if (apkPath != null && mMeshFileCommunicator != null) {
                String metaData = "";
                String fileData = mMeshFileCommunicator.sendFile(sender, apkPath, metaData.getBytes(),
                        getApplicationContext().getPackageName());

                if (fileData != null) {

                    try {
                        JSONObject jsonObject = new JSONObject(fileData);
                        boolean success = jsonObject.getBoolean("success");
                        String msg = jsonObject.getString("msg");
                        if (success) {
                            inAppUpdatePathMap.put(msg, apkPath);
                        } else {
                            HandlerUtil.postForeground(() -> {
                                Toaster.showShort(msg);
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } else {
            if (isAppDownloadEnable(appToken)) {
                ViperCommunicator communicator = getCommunicatorByToken(appToken);
                if (communicator != null) {
                    try {
                        communicator.onAppUpdateRequest(sender);
                        //Todo rocky if communicator null then prepare apk from here not need to send client side
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        MeshLog.e("App update request error: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void processClientAppConfiguration(HandshakeInfo handshakeInfo) {
        String clientAppData = handshakeInfo.getClientAppConfig();
        if (!TextUtils.isEmpty(clientAppData)) {
            ClientInfoSyncUtil.getInstance().processClientInformation(clientAppData);
        }
        String configText = handshakeInfo.getCredentialsConfig();
        if (!TextUtils.isEmpty(configText)) {
            ConfigurationCommand configurationCommand = new Gson().fromJson(configText, ConfigurationCommand.class);
            ConfigSyncUtil.getInstance().updateConfigCommandFile(getApplicationContext(), configurationCommand);
        }
    }

    private UserInfo getUserInfoByPeersEntity(PeersEntity peersEntity) {

        String appUserInfo = peersEntity.getAppUserInfo();

        UserInfo userInfo;

        if (!TextUtils.isEmpty(appUserInfo)) {
            userInfo = JsonBuilder.on().formJson(appUserInfo);
        } else {
            userInfo = new UserInfo();
        }

        userInfo.setAddress(peersEntity.getAddress());
        userInfo.setPublicKey(peersEntity.getPublicKey());

        userInfo.setLatitude(peersEntity.getUserLatitude());
        userInfo.setLongitude(peersEntity.getUserLongitude());

        return userInfo;
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void updateOtherUserInfo(UserInfo userInfo) {
        try {

            PeersEntity currentUser = getDataBaseService().getPeersById(userInfo.getAddress(), userInfo.getAppToken());

            if (currentUser != null) {

                String userInfoData = currentUser.getAppUserInfo();

                UserInfo retrievedUserInfo = JsonBuilder.on().formJson(userInfoData);

                if (!TextUtils.equals(userInfo.getUserName(), retrievedUserInfo.getUserName())
                        && !TextUtils.isEmpty(userInfo.getUserName())) {
                    retrievedUserInfo.setUserName(userInfo.getUserName());
                }

                if (userInfo.getAvatar() != retrievedUserInfo.getAvatar()
                        && userInfo.getAvatar() > 0) {
                    retrievedUserInfo.setAvatar(userInfo.getAvatar());
                }

                userInfoData = JsonBuilder.on().toJson(retrievedUserInfo);

                currentUser.setAppUserInfo(userInfoData);

                getDataBaseService().insertPeersEntity(currentUser);

            }
        } catch (ExecutionException | InterruptedException e) {
            MeshLog.e("Other User info update exception: " + e.getMessage());
        }
    }

    /**
     * Get each viper communicator by application token
     *
     * @param appToken String
     * @return @{ViperCommunicator}
     */
    private ViperCommunicator getCommunicatorByToken(String appToken) {
        return viperCommunicatorList.get(appToken);
    }


    @Override
    public void onUserConnected(String userId, boolean isConnected) {
        Log.v(TAG, "User connection state =" + isConnected);
        if (!isConnected) {
            onUserDisconnected(userId);
            HandlerUtil.postForeground(() -> Toast.makeText(getApplicationContext(),
                    "User not connected", Toast.LENGTH_SHORT).show());

        }
    }

    private MeshControlConfig getMeshControlConfig(String appToken) {

        try {

            if (meshControlConfigs == null || meshControlConfigs.isEmpty()) {
                String meshControlData = loadJSONFromAsset(getApplicationContext(), "MeshControlConfig.json");
                Type founderListType = new TypeToken<ArrayList<MeshControlConfig>>() {
                }.getType();
                meshControlConfigs = new Gson().fromJson(meshControlData, founderListType);
            }

            if (meshControlConfigs != null) {

                for (MeshControlConfig meshControlConfig : meshControlConfigs) {
                    if (TextUtils.equals(meshControlConfig.getAppToken(), appToken))
                        return meshControlConfig;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private ClientInfoEntity getClientInformation(String appToken) {

        try {
            if (clientInfoList == null || clientInfoList.isEmpty()) {
                clientInfoList = ClientInfoSyncUtil.getInstance().getAllClientInformation();
            }

            // we are given extra checking that clientInfoList empty or not
            if (clientInfoList != null) {
                for (ClientInfoEntity entity : clientInfoList) {
                    if (entity.appToken.equals(appToken)) {
                        return entity;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean isMeshDiscoverEnable(String appToken) {
        //MeshControlConfig meshControlConfig = getMeshControlConfig(appToken);
        ClientInfoEntity entity = getClientInformation(appToken);
        return entity != null && entity.isDiscoveryEnable;
    }

    private boolean isMeshMessageEnable(String appToken) {
        //MeshControlConfig meshControlConfig = getMeshControlConfig(appToken);
        ClientInfoEntity entity = getClientInformation(appToken);
        return entity != null && entity.isMessageEnable;
    }

    private boolean isMeshBlockChainEnable(String appToken) {
        //MeshControlConfig meshControlConfig = getMeshControlConfig(appToken);
        ClientInfoEntity entity = getClientInformation(appToken);
        return entity != null && entity.isBlockchainEnable;
    }

    private boolean isAppDownloadEnable(String appToken) {
        //MeshControlConfig meshControlConfig = getMeshControlConfig(appToken);
        ClientInfoEntity entity = getClientInformation(appToken);
        return entity != null && entity.isAppDownloadEnable;
    }

    public boolean isWalletCreationEnable(String appToken) {
        //MeshControlConfig meshControlConfig = getMeshControlConfig(appToken);
        ClientInfoEntity entity = getClientInformation(appToken);
        return entity != null && entity.isWalletCreationEnable;
    }

    private String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while( (line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
            Log.e("Client_config","json :"+jsonStringBuilder.toString());
            is.close();
            json = jsonStringBuilder.toString();
            /*int size = is.available();
            Log.e("Client_config","config length :"+size);
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            Log.e("Client_config","return string  :"+json);*/
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private boolean isNeedToUpdate(int versionCode) {
        return versionCode > BuildConfig.VERSION_CODE;
    }

    public void initPaymentSection() {
        MeshLog.v("initPaymentSection");
        if (PreferencesHelperDataplan.on().getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_SELLER) {
            PurchaseManagerSeller.getInstance();
        }

        if (PreferencesHelperDataplan.on().getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER) {
            PurchaseManagerBuyer.getInstance();
        }

        PurchaseManager.getInstance().setParseInfo(CredentialUtils.getParseUrl(), CredentialUtils.getParseAppId());

        EthereumServiceUtil.getInstance(getApplicationContext());
    }

    private String getUserPublicKeyFromDb(String userAddress) {
        try {
            return getDataBaseService().getPeersPublicKey(userAddress);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    /*
     * File message status listener
     * */

    @Override
    public void onFileProgress(String fileTransferId, int percentProgress, String appToken) {
        if (appToken.equals(getApplicationContext().getPackageName())) {
            Context context = TeleMeshServiceMainActivity.getInstance();
            if (context != null) {
                ServiceUpdateDialog.getInstance().updateProgressDialog(percentProgress);
            } else {
                String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                com.w3engineers.mesh.util.NotificationUtil.updateProgress(getApplicationContext(), myUserId, percentProgress);
            }
        } else {
            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                try {
                    communicator.onFileProgress(fileTransferId, percentProgress);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    viperCommunicatorList.remove(appToken);
                    contentSharePending(fileTransferId, percentProgress, appToken);
                }
            } else {
                contentSharePending(fileTransferId, percentProgress, appToken);
            }
        }
    }

    @Override
    public void onFileTransferFinish(String fileTransferId, String appToken) {
        if (appToken.equals(getApplicationContext().getPackageName())) {
            TSAppInstaller.isAppUpdating = false;
            Context context = TeleMeshServiceMainActivity.getInstance();

            if (inAppUpdatePathMap.containsKey(fileTransferId)) {
                // Means it is the sender
                deleteBackUpFile(inAppUpdatePathMap.remove(fileTransferId));
                return;
            }

            if (context != null) {
                ServiceUpdateDialog.getInstance().closeDialog(context.getString(R.string.app_update_success));
                ServiceUpdateDialog.getInstance().showAppInstaller(context, appPath);
            } else {
                String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                com.w3engineers.mesh.util.NotificationUtil.removeNotification(getApplicationContext(), myUserId);
                com.w3engineers.mesh.util.NotificationUtil.showSuccessErrorNotification(getApplicationContext(), true, appPath, myUserId);
            }
        } else {
            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                try {
                    contentShareEnd(fileTransferId, true, appToken, true);
                    communicator.onFileTransferFinish(fileTransferId);

                    // We have to delete shared app if it exist
                    if (inAppUpdatePathMap.containsKey(fileTransferId)) {
                        deleteBackUpFile(inAppUpdatePathMap.remove(fileTransferId));

                        if (inAppUpdateAppReceiverMap.containsKey(fileTransferId)) {
                            String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                            AppUpdateAppDataManager.saveAppUpdateAppInfo(myUserId,
                                    inAppUpdateAppReceiverMap.remove(fileTransferId), appToken);
                        }
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                    viperCommunicatorList.remove(appToken);
                    contentShareEnd(fileTransferId, true, appToken, false);
                }
            } else {
                contentShareEnd(fileTransferId, true, appToken, false);
            }
        }
    }

    @Override
    public void onFileTransferError(String fileTransferId, String appToken, String errorMessage) {
        if (appToken.equals(getApplicationContext().getPackageName())) {
            TSAppInstaller.isAppUpdating = false;

            if (inAppUpdatePathMap.containsKey(fileTransferId)) {
                // Means it is the sender
                deleteBackUpFile(inAppUpdatePathMap.remove(fileTransferId));

                Context context = TeleMeshServiceMainActivity.getInstance();
                if (context != null) {
                    ServiceUpdateDialog.getInstance().closeDialog(context.getString(R.string.app_update_error));
                } else {
                    String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                    com.w3engineers.mesh.util.NotificationUtil.removeNotification(getApplicationContext(), myUserId);
                    com.w3engineers.mesh.util.NotificationUtil.showSuccessErrorNotification(getApplicationContext(), false, "", myUserId);
                }
            }
        } else {
            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                try {
                    contentShareEnd(fileTransferId, false, appToken, true);
                    communicator.onFileTransferError(fileTransferId, errorMessage);

                    // We have to delete shared app if it exist
                    if (inAppUpdatePathMap.containsKey(fileTransferId)) {
                        deleteBackUpFile(inAppUpdatePathMap.remove(fileTransferId));
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                    viperCommunicatorList.remove(appToken);
                    contentShareEnd(fileTransferId, false, appToken, false);
                }
            } else {
                contentShareEnd(fileTransferId, false, appToken, false);
            }
        }
    }

    @Override
    public void onFileReceiveStarted(String sourceAddress, String fileTransferId, String
            filePath, byte[] msgMetaData, String appToken) {

        MeshLog.v("FILE_SPEED_TEST_11 " + Calendar.getInstance().getTime());
        receiverContentMap.put(fileTransferId, sourceAddress);
        if (appToken.equals(getApplicationContext().getPackageName())) {

//            appDownloadId = fileTransferId;
            appPath = filePath;
            if (TeleMeshServiceMainActivity.getInstance() != null) {
                ServiceUpdateDialog.getInstance().showAppUpdateProgressDialog(TeleMeshServiceMainActivity.getInstance());
            } else {
                String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
                com.w3engineers.mesh.util.NotificationUtil.showAppUpdateProgress(getApplicationContext(), myUserId);
            }

        } else {
            ViperCommunicator communicator = getCommunicatorByToken(appToken);
            if (communicator != null) {
                try {
                    contentShareStart(sourceAddress, fileTransferId, filePath, msgMetaData, appToken, true);
                    FileData fileData = new FileData().setSourceAddress(sourceAddress).setFileTransferId(fileTransferId)
                            .setFilePath(filePath).setMsgMetaData(msgMetaData);
                    communicator.onFileReceiveStarted(fileData);
                } catch (Exception e) {
                    e.printStackTrace();
                    viperCommunicatorList.remove(appToken);
                    contentShareStart(sourceAddress, fileTransferId, filePath, msgMetaData, appToken, false);
                }
            } else {
                contentShareStart(sourceAddress, fileTransferId, filePath, msgMetaData, appToken, false);
            }
        }
    }

    private final int stateFailed = 0, stateInProgress = 1, stateSuccess = 2;
    private final byte SUCCESS_CONTENT_MESSAGE = 0x18;

    // Done
    private void contentSendStart(String contentData, String userId, String appToken, byte[] meta) {
        try {
            if (!TextUtils.isEmpty(contentData)) {

                JSONObject jsonObject = new JSONObject(contentData);
                boolean success = jsonObject.getBoolean("success");
                String contentId = jsonObject.getString("msg");


                if (!success)
                    return;

                String metaText = null;

                try {
                    metaText = new String(meta);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Content content = new Content();
                content.setSenderId(userId);
                content.setContentId(contentId);

                content.setState(stateInProgress);
                content.setIncoming(false);
                content.setAppToken(appToken);
                content.setContentMetaInfo(metaText);

                if (databaseService != null) {
                    databaseService.insertOrUpdateContent(content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void contentShareStart(String userId, String contentId, String path, byte[] metaInfo,
                                   String appToken, boolean isAppActive) {
        try {

            // Meta info - messageId. contentType-thumb|real, messageType
            /*try {

                MeshLog.v("metaText " + metaText);
                ContentMetaInfo contentMetaInfo = ContentMetaGsonBuilder.getInstance()
                        .prepareContentMetaObj(metaText);
//                isOnlyContent = contentMetaInfo.getIsContent();
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            if (!isAppActive) {

                String metaText = null;

                if (metaInfo != null) {
                    metaText = new String(metaInfo);
                }

                Content content = new Content();
                content.setSenderId(userId);
                content.setContentId(contentId);
                content.setContentPath(path);

                content.setProgress(0);
                content.setState(stateInProgress);
                content.setContentMetaInfo(metaText);

                content.setIncoming(true);
                content.setAppToken(appToken);

                if (databaseService != null) {
                    databaseService.insertOrUpdateContent(content);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void contentSharePending(String contentId, int progress, String appToken) {

        try {
            Content content = databaseService.getContentMessageByContentId(contentId);
            if (content != null) {

                content.setState(stateInProgress);
                content.setProgress(progress);
            } else {

                content = new Content();
                content.setContentId(contentId);
                content.setProgress(progress);
                content.setState(stateInProgress);

                content.setIncoming(true);
                content.setAppToken(appToken);
            }

            databaseService.insertOrUpdateContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void contentShareEnd(String contentId, boolean isFinished, String appToken, boolean isAppEnable) {
        try {
            Content content = databaseService.getContentMessageByContentId(contentId);
            if (content != null) {

                if (isAppEnable) {
                    try {

                        databaseService.deleteContentsByContentId(appToken, contentId);
                        return;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                content.setState(isFinished ? stateSuccess : stateFailed);

                if (isFinished && content.isIncoming()) {

                    String userId = receiverContentMap.get(contentId);
                    if (!TextUtils.isEmpty(userId)) {
                        sendNotify(contentId, userId, SUCCESS_CONTENT_MESSAGE, appToken);
                    }
                }
            } else {
                // TODO for sender side effect
                content = new Content();
                content.setContentId(contentId);
                content.setState(isFinished ? stateSuccess : stateFailed);
                content.setIncoming(true);
                content.setAppToken(appToken);
            }
            databaseService.insertOrUpdateContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNotify(String messageId, String userId, byte type, String appToken) {
        if (TextUtils.isEmpty(messageId) || TextUtils.isEmpty(userId))
            return;
        try {
            String myUserId = SharedPref.read(Utils.KEY_NODE_ADDRESS);
            String sendId = UUID.randomUUID().toString();
            byte[] data = getDataFormatToJson(type, messageId);
            BoundServiceManager.on(getApplicationContext()).sendData(myUserId, userId, sendId,
                    data, false, appToken);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getDataFormatToJson(byte dataType, String data) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("t", dataType);
            jsonObject.put("d", data);

            return jsonObject.toString().getBytes();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Service app update section
    private void showInAppUpdateDialog(Context context, String receiverId) {
        // Checking service app is downloading from internet or not
        // Or checking service app is already start download from locally
        // If not we will show App update dialog confirmation
        if (!TSAppInstaller.isAppUpdating) {

            // Todo Control we have to make decision when we will show dialog and when notification

            TSAppInstaller.isAppUpdating = true;

            DialogUtil.showConfirmationDialog(context,
                    context.getString(R.string.app_update),
                    "New version " + context.getString(R.string.is_available_for_telemesh),
                    context.getString(R.string.cancel),
                    context.getString(R.string.update),
                    new DialogUtil.DialogButtonListener() {
                        @Override
                        public void onClickPositive() {
                            if (transportManagerX != null) {
                                appUpdateRequest(receiverId, getApplicationContext().getPackageName());
                            }
                        }

                        @Override
                        public void onCancel() {

                        }

                        @Override
                        public void onClickNegative() {
                            TSAppInstaller.isAppUpdating = false;
                        }
                    });
        }
    }

    @Override
    public void onBroadcastFileTransferFinish(String broadcastId, String broadcastText, String
            contentPath, String senderId, String appToken) {

        Log.v("MIMO_SAHA:", "Broadcast Finish");
        sendBroadcastReceivedACK(broadcastId, senderId);

        BroadcastDataHelper.getInstance().broadcastContentSavedConfirmation(broadcastId, contentPath);
    }

    @Override
    public void onSenderBroadcastFileTransferFinish(String broadcastId, String userId, String
            contentPath) {
        BroadcastDataHelper.getInstance().broadcastContentSendConfirmation(broadcastId, userId, contentPath);
    }

    @Override
    public void onBroadcastFileTransferError(String broadcastId, String fileTransferId, String
            appToken, String userId) {
        // onBroadcastFileTransferError
        BroadcastDataHelper.getInstance().failedBroadcastContent(broadcastId, userId);
        MeshLog.i("onBroadcastFileTransferError ");
    }

    private void deleteBackUpFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                boolean isDeleted = file.delete();
                MeshLog.v(" BackUpFileDelete: isDeleted: " + isDeleted);
            }
        } catch (Exception e) {
            MeshLog.e(" BackUpFileDelete: error: " + e.getMessage());
        }
    }

    private String getOwnAddress() {
        return SharedPref.read(Utils.KEY_NODE_ADDRESS);
    }

    private void processPendingAppUpdateRequest(String appToken) {
        // Check that app token has any pending app update app request.
        if (!TextUtils.isEmpty(appToken)) {
            AppUpdateAppInfoModel appUpdateDataModel = inAppUpdateDataMap.remove(appToken);
            if (appUpdateDataModel != null) {
                // Check that the actual sender is online or not.
                if (transportManagerX.isUserConnected(appUpdateDataModel.senderId)) {
                    ViperCommunicator communicator = getCommunicatorByToken(appToken);
                    if (communicator != null) {
                        try {
                            FileData fileData = new FileData().setReceiverID(appUpdateDataModel.senderId)
                                    .setAppVersion(appUpdateDataModel.version).setVersionName(appUpdateDataModel.versionName)
                                    .setAppSize(appUpdateDataModel.appSize);
                            communicator.receiveOtherAppVersion(fileData);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public DatabaseService getDataBaseService() {
        return DatabaseService.getInstance(getApplicationContext());
    }

/*    public void setUserLocationUpdateRequest() {
        try {

            ListenableFuture<List<WorkInfo>> forUniqueWork = WorkManager.getInstance(this).getWorkInfosByTag(USER_LOCATION_UPDATE);
            List<WorkInfo> workInfos = forUniqueWork.get();

            if (workInfos == null || workInfos.isEmpty()) {
                // Create charging constraint
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                PeriodicWorkRequest locationUpdate = new PeriodicWorkRequest.Builder(LocationUpdateWorker.class, 20, TimeUnit.MINUTES)
                        .addTag(USER_LOCATION_UPDATE)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .build();

                mWorkManager.enqueue(locationUpdate);
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }*/

    private boolean isRunning = false;

    Runnable mLocationChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateMyLocation();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mLocationChecker, mInterval);
            }
        }
    };

    private void updateMyLocation() {
        LocationTracker.getInstance(mContext).getLocation();
        double latitude = LocationTracker.getInstance(mContext).getLatitude();
        double longitude = LocationTracker.getInstance(mContext).getLongitude();

        //Log.v("MIMO_SAHA:", "Lat: " + latitude + " Lang: " + longitude);
        updateMyLocationAndShareIntoMesh(latitude, longitude);
    }

    void startLocationUpdatingTask() {
        mLocationChecker.run();
    }

/*    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (isGpsEnabled || isNetworkEnabled) {
                    //location is enabled
                    Log.e("gps_staus", "gps has been on");
                    LocationTracker.getInstance(mContext).getLocation();
                }
            }
        }
    };*/

    private class Delegate {
        private void checkAppTamper() {
            final int OK = 1;
            int appChanged = TamperDetector.checkApk(mContext, OK);
            int certificateChanged =
                    CertificateChecker.checkCertificate(mContext, OK);

            FileChecker fileChecker = new FileChecker(mContext);

            int primaryDexChanged =
                    fileChecker.checkFile("classes.dex", OK);

            int anyFileChanged =
                    fileChecker.checkAllFiles(OK);

            if (appChanged != OK ||
                    certificateChanged != OK ||
                    primaryDexChanged != OK) {

                HandlerUtil.postForeground(() -> {
                    Toaster.showShort("App is tampered. No support available");
                });

                destroyFullService();
                Process.killProcess(Process.myPid());
            }
        }
    }

    public static void raspCallback(DetectionReport detectionReport) {
        // isTampered return if apk tampered or certificate tampered or file (dex) file tampered
        if (detectionReport.isTampered()) {
            HandlerUtil.postForeground(() -> {
                Toaster.showShort("App is tampered. No support available");
            });

            //destroyFullService();
            Process.killProcess(Process.myPid());
        }

    }

    public static String getAuid()
    {
        // Implementation of how the AUID is generated and retrieved is left to the app developer.

        return SharedPref.read(Utils.KEY_NODE_ADDRESS);
    }

    public static void testappupdateapp () {
        AppUpdateInfoEntity a= AppUpdateAppDataManager.getAppUpdateInfo();
        AppUpdateAppParseInfo info = AppUpdateAppDataManager.convertEntityToParseInfo(a);
        ParseManager.getInstance().sendAppUpdateAppInfo(a.id, info, id -> com.w3engineers.purchase.db.DatabaseService
                .getInstance(MeshApp.getContext())
                .updateSyncedAppUpdateInformation(id));
    }
}
