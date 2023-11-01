package com.w3engineers.mesh.manager;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.PermissionUtil;
import com.w3engineers.meshrnd.ITmCommunicator;
import com.w3engineers.meshrnd.TeleMeshService;
import com.w3engineers.models.MessageData;

/**
 * <p>This classes sole job is to create a bound service connection to the BackgroundService.
 * By binding to the service, the service will continue to run in the background even when the
 * original Activity / App which created it is off the screen or even fully shut down.
 *
 * @version $Version:$</p>
 */

public class BoundServiceManager {
    /**
     * Instance variable
     */
    private final String TAG = BoundServiceManager.class.getSimpleName();
    private static BoundServiceManager boundServiceManager;
    private ITmCommunicator iTmCommunicator;
    // To use for start service
    private Context mContext;
    private boolean isStartFromService = false;
    private boolean isServiceBoundedWithClient = false;

    private static boolean isServiceBounded = false;

    private BoundServiceManager(Context context) {
        this.mContext = context;
    }

    public static BoundServiceManager on(Context context) {

        if (boundServiceManager == null) {
            boundServiceManager = new BoundServiceManager(context);
        }
        return boundServiceManager;
    }

    public void startSelfBind(boolean isFromService) {
        if (!isServiceBounded) {
            Log.d("TMeshService", "Attempt to start service");
            Intent mServiceIntent = new Intent(mContext, TeleMeshService.class);
            isStartFromService = isFromService;
            mContext.bindService(mServiceIntent, mConnection, Service.BIND_AUTO_CREATE);
        }
    }

    public void unBindService() {
        if (mConnection != null) {
            mContext.unbindService(mConnection);
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isServiceBounded = true;
            Log.d("TMeshService", "onServiceConnected called");
            iTmCommunicator = ITmCommunicator.Stub.asInterface(service);
            onStartForeground(isStartFromService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBounded = false;
            iTmCommunicator = null;
        }
    };


    public void onStartForeground(boolean isNeeded) {
        try {
            if (iTmCommunicator != null) {
                iTmCommunicator.onStartForeground(isNeeded);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(TAG, "isMyServiceRunning? +true");
                return true;
            }
        }
        Log.d(TAG, "isMyServiceRunning? +false");
        return false;
    }

    public boolean isPermissionNeeded() {
        String manufacturer = android.os.Build.MANUFACTURER;
        try {

            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                if (!SharedPref.readBoolean(Constant.PreferenceKeys.IS_SETTINGS_PERMISSION_DONE)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isPermissionAllow() {
        if (PermissionUtil.init(mContext).isAllowed(Manifest.permission.ACCESS_FINE_LOCATION)
                && PermissionUtil.init(mContext).isAllowed(Manifest.permission.ACCESS_COARSE_LOCATION)
                && !isPermissionNeeded()) {
            return true;

        }
        return false;
    }

    public void stopTmService(Context mContext) {
        onStartForeground(false);
        unBindService();
        mContext.stopService(new Intent(mContext, TeleMeshService.class));
        iTmCommunicator = null;

    }

    public void startMeshService(String appToken) {
        if (iTmCommunicator != null) {
            try {
                iTmCommunicator.startMesh(appToken);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFirstAppToken() {
        if (iTmCommunicator != null) {
            try {
                iTmCommunicator.getFirstAppToken();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public void sendData(String senderId, String receiverId, String messageId, byte[] data,
                         boolean isNeeded, String appToken) {
        if (iTmCommunicator != null) {
            try {
                MessageData messageData = new MessageData().setSenderID(senderId).setReceiverID(receiverId)
                                          .setMessageID(messageId).setMsgData(data).setNotificationNeeded(isNeeded)
                                          .setAppToken(appToken);
                iTmCommunicator.sendData(messageData);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isServiceRunning() {
        return iTmCommunicator == null ? false : true;
    }

    public void setClientBindStatus(boolean status) {
        isServiceBoundedWithClient = status;
    }

    public boolean getClientBindStatus() {
        return isServiceBoundedWithClient;
    }
}
