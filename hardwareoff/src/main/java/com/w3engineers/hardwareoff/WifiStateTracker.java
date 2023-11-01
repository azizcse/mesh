package com.w3engineers.hardwareoff;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.hardwareoff.callback.WifiHotspotCallback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WifiStateTracker {

    public static final int AP_STATE_DISABLING = 10;
    public static final int AP_STATE_DISABLED = 11;
    private static final int AP_STATE_ENABLING = 12;
    private static final int AP_STATE_ENABLED = 13;
    private static final int AP_STATE_FAILED = 14;
    private static final int WIFI_AP_STATE_UNKNOWN = -1;

    private Context mContext;
    private WifiManager wifiManager;
    private Callback mCallback;
    private final String TAG = "HardwareTest";

    private static WifiStateTracker sInstance;
    private ConnectivityManager mConnectivityManager;
    private static WifiManager.LocalOnlyHotspotReservation mLocalOnlyHotspotReservation;

    private WifiHotspotCallback mWifiHotspotCallback;

    public static WifiStateTracker getInstance() {
        if (sInstance == null) {
            sInstance = new WifiStateTracker();
        }
        return sInstance;
    }

    public void setWifiHotspotCallback(WifiHotspotCallback wifiHotspotCallback) {
        mWifiHotspotCallback = wifiHotspotCallback;
    }

    void initWifiStateTracker(Context context, Callback callback) {
        this.mContext = context;
        this.mCallback = callback;
        wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        }

        //offWifi();
    }

    void offWifi() {
        if (wifiManager.isWifiEnabled()) {
            if (!HardwareTrackerUtils.isDisableCalled) {
                boolean isOff = wifiManager.setWifiEnabled(false);
                HardwareTrackerUtils.isDisableCalled = true;
                Log.d("HardwareTest", "set wifiManager disable  :: " + isOff);
            }
        } else {
            mCallback.onGetWifiOffCallback();
        }
    }

    void onWifi() {
        if (wifiManager != null) {
            if (wifiManager.isWifiEnabled()) {
                mCallback.onGetWifiEnableCallback();
                Log.d("HardwareTest", "wifiManager is enable");
            } else {
                if (!HardwareTrackerUtils.isEnableCalled) {
                    boolean status = wifiManager.setWifiEnabled(true);
                    HardwareTrackerUtils.isEnableCalled = true;
                    Log.d("HardwareTest", "set wifiManager enable :: " + status);
                }
            }
        } else {
            Log.d("HardwareTest", "wifiManager is null");
        }
    }

    boolean isWifiOff() {
        if (wifiManager != null) {
            return !wifiManager.isWifiEnabled();
        }
        return false;
    }

    void offWifiHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isHotspotEnabled()) {
                try {
                    Method method = mConnectivityManager.getClass().getDeclaredMethod("stopTethering", int.class);
                    method.invoke(mConnectivityManager, ConnectivityManager.TYPE_MOBILE);
                    //  mCallback.onDisabledHotspot();
                } catch (Exception e) {
                    Log.e(TAG, e.toString(), e); // shouldn't happen
                }
            } else {
                mCallback.onGetWifiHotspotCallback();
            }
        } else {
            if (isHotspotEnabled()) {
                WifiConfiguration apConfig = getWifiApConfiguration();

                try {
                    Method method = wifiManager.getClass().getMethod("setWifiApEnabled",
                            WifiConfiguration.class, boolean.class);
                    method.invoke(wifiManager, apConfig, false);
                    // mCallback.onDisabledHotspot();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString(), e);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString(), e);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString(), e);
                }
            } else {
                mCallback.onGetWifiHotspotCallback();
            }
        }

    }

    private WifiConfiguration getWifiApConfiguration() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            return (WifiConfiguration) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e); // shouldn't happen
            return null;
        }
    }

    private int getApState() {
        int state = WIFI_AP_STATE_UNKNOWN;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                Method method2 = wifiManager.getClass().getMethod("getWifiApState");
                state = (Integer) method2.invoke(wifiManager);
            } catch (NoSuchMethodException e) {
                e.getCause();
            } catch (InvocationTargetException e2) {
                e2.getCause();
            } catch (IllegalAccessException e3) {
                e3.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        } else {//From Oreo
            state = mLocalOnlyHotspotReservation == null ? WIFI_AP_STATE_UNKNOWN : AP_STATE_ENABLED;
        }

        return state;
    }

    boolean isHotspotEnabled() {
        int wifiAPState = getApState();
        return wifiAPState == AP_STATE_ENABLED || wifiAPState == AP_STATE_ENABLING;
    }

    /**
     * Enable Hotspot and report through a callback
     */
    void enableHotSpot() {

        if(!isHotspotEnabled()) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long checkAfter = 500;
                        int maxTry = 10;

                        boolean isEnabled = triggerAp(true, checkAfter, maxTry);

                        if(mWifiHotspotCallback != null) {
                            if(isEnabled) {
                                mWifiHotspotCallback.onEnabledHotspot(HardwareStateManager.AP_SSID,
                                        HardwareStateManager.AP_PRESHARED_KEY);
                            } else {
                                Log.e(TAG, "After "+((checkAfter * maxTry)/1000) + "seconds AP " +
                                        "still not enabled");
                            }
                        }
                    }
                }).start();

            } else {

                wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                                                      @Override
                                                      public void onFailed(int reason) {
                                                          super.onFailed(reason);
                                                          Log.d(TAG, "Local AP request failed:"+reason);
                                                          mWifiHotspotCallback.onError(reason);
                                                          mLocalOnlyHotspotReservation = null;
                                                      }

                                                      @Override
                                                      public void onStarted(final WifiManager.LocalOnlyHotspotReservation reservation) {
                                                          super.onStarted(reservation);
                                                          if(reservation != null) {
                                                              Log.d(TAG, reservation.toString());
                                                              mLocalOnlyHotspotReservation = reservation;
                                                              if (mWifiHotspotCallback != null
                                                                      && reservation.getWifiConfiguration() != null) {
                                                                  WifiConfiguration wifiConfiguration =
                                                                          reservation.getWifiConfiguration();

                                                                  mWifiHotspotCallback.onEnabledHotspot(
                                                                          wifiConfiguration.SSID,
                                                                          wifiConfiguration.preSharedKey);
                                                              }
                                                          }
                                                      }

                                                      @Override
                                                      public void onStopped() {
                                                          super.onStopped();
                                                          Log.d(TAG, "onStopped");
                                                          if(mWifiHotspotCallback != null) {
                                                              mWifiHotspotCallback.onDisabledHotspot();
                                                          }
                                                          mLocalOnlyHotspotReservation = null;
                                                      }
                                                  },
                        null);
            }
        }
    }

    public void disableHotspot() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                    long checkAfter = 500;
                    int maxTry = 10;
                    if(triggerAp(false, checkAfter, maxTry)) {
                        if(mWifiHotspotCallback != null) {
                            mWifiHotspotCallback.onDisabledHotspot();
                        } else {
                            Log.e(TAG, "After "+((checkAfter * maxTry)/1000) + "seconds AP " +
                                    "still not disabled");
                        }
                    }
                } else {
                    if (mLocalOnlyHotspotReservation != null) {
                        mLocalOnlyHotspotReservation.close();
                    }
                }
            }
        }).start();
    }

    private boolean triggerAp(boolean isToOn, long checkAfter, int maxTry) {

        WifiConfiguration wificonfiguration = getWifiConfiguration(HardwareStateManager.AP_SSID,
                HardwareStateManager.AP_PRESHARED_KEY);
        try {
            Method method1 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            Object isSuccess = method1.invoke(wifiManager, wificonfiguration, isToOn);
            Method method2 = wifiManager.getClass().getMethod("getWifiApState");
            method2.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        int state = getApState();
        if (isToOn) {
            while (maxTry > 0 && (state == AP_STATE_ENABLING || state == AP_STATE_DISABLED || state == AP_STATE_FAILED)) {
                //Log.d(TAG, (isEnable ? "enabling" : "disabling") + " wifi ap: waiting, pass: " + (10 - loopMax));
                try {
                    Thread.sleep(checkAfter);
                    maxTry--;
                } catch (Exception e) {
                }
                state = getApState();
            }
        } else {
            while (maxTry > 0 && (state == AP_STATE_DISABLING || state == AP_STATE_ENABLED || state == AP_STATE_FAILED)) {
                //Log.d(TAG, (isEnable ? "enabling" : "disabling") + " wifi ap: waiting, pass: " + (10 - loopMax));
                try {
                    Thread.sleep(checkAfter);
                    maxTry--;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                state = getApState();
            }
        }

        boolean currentApState = isHotspotEnabled();
        return (isToOn && currentApState) || (!isToOn && !currentApState);
    }

    private WifiConfiguration getWifiConfiguration(String ssidName, String password) {

        if(TextUtils.isEmpty(password) || TextUtils.isEmpty(ssidName)) {
            return null;
        }

        WifiConfiguration newConfig = new WifiConfiguration();
        newConfig.SSID = ssidName;
        newConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//        newConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        newConfig.allowedKeyManagement.set(4);
        newConfig.preSharedKey = password;
        return newConfig;
    }
}
