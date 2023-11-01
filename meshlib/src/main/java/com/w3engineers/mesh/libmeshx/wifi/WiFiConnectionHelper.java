package com.w3engineers.mesh.libmeshx.wifi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.PatternMatcher;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.w3engineers.mesh.R;
import com.w3engineers.mesh.libmeshx.wifid.APCredential;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifidirect.connector.Android10ConnectState;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


/**
 * All native WiFi adapter related helper functions
 */
public class WiFiConnectionHelper {

    private WifiManager mWifiManager;
    private WifiManager.WifiLock mWifiLock = null;
    private Context mContext;
    private Android10ConnectState android10ConnectState;
    public WiFiConnectionHelper(Context context, Android10ConnectState android10ConnectState) {
        this.android10ConnectState = android10ConnectState;
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF, context.getString(R.string.app_name));
    }

    public boolean disconnect() {
        if (mWifiManager != null) {
            // Because we are adding target SDK is 28 so now we can ignore this condition
            // Todo for all when change the SDK 28 to upper. We have to work on here

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                // TODO: 1/22/2020 Will add removeSuggestions API upon upgrading API level over 29

                // it is temporary
                return mWifiManager.disconnect();
            } else {
                return mWifiManager.disconnect();
            }
        }
        return false;
    }

    /**
     * Disable all configured wifi network. We want to stop any automatic connection.
     */
    public void disableAllConfiguredWiFiNetworks() {
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {
            Timber.d("Connection_log disabling");
            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (wifiConfiguration != null && wifiConfiguration.networkId != -1) {
                    boolean isDisabled = mWifiManager.disableNetwork(wifiConfiguration.networkId);
                    Timber.d("isDisabled:%s", isDisabled);
                }
            }
        }
    }

    public boolean disableConfiguredWiFiNetwork(int networkId) {
        if (networkId != -1) {
            return mWifiManager.disableNetwork(networkId);
        }
        return false;
    }

    public int getConnectedNetworkId() {
        return getConnectionInfo().getNetworkId();
    }

    /**
     * Disable all configured wifi network. We want to stop any automatic connection.
     */
    public int getConfiguredWiFiNetworkId(String SSID) {
        if (TextUtils.isEmpty(SSID)) {
            return -1;
        }
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {

            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (wifiConfiguration != null && wifiConfiguration.networkId != -1) {
                    if (SSID.equals(wifiConfiguration.SSID)
                            || wifiConfiguration.SSID.equals("\"" + SSID + "\"")) {
                        return wifiConfiguration.networkId;
                    }
                }
            }
        }

        return -1;
    }

    final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            android10ConnectState.onAttemptFinish(true);
            MeshLog.e("Android_10 connection success :");
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            android10ConnectState.onAttemptFinish(false);
            MeshLog.e("Android_10 Add network suggestion failed :");
        }
    };

    public boolean connect(APCredential credential) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {


            WifiNetworkSuggestion networkSuggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(credential.mSSID)
                            .setWpa2Passphrase(credential.mPassPhrase)
                            .setIsAppInteractionRequired(false)
                            .build();


            List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
            suggestionsList.add(networkSuggestion);

            int status = mWifiManager.addNetworkSuggestions(suggestionsList);

            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                MeshLog.e("Android_10 Add network suggestion failed :"+status);
                if(status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE){
                    mWifiManager.removeNetworkSuggestions(suggestionsList);
                    status = mWifiManager.addNetworkSuggestions(suggestionsList);
                    MeshLog.e("Android_10 Add network suggestion failed :"+status);
                }
            }

            NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setWpa2Passphrase(credential.mPassPhrase)
                    .setSsidPattern(new PatternMatcher("DIRECT-", PatternMatcher.PATTERN_PREFIX))
                    .setBssidPattern(MacAddress.fromString(credential.macAddress), MacAddress.fromString("ff:ff:ff:00:00:00"))
                    .setBssid(MacAddress.fromString(credential.macAddress))
                    .setIsHiddenSsid(true)
                    .setSsid(credential.mSSID)
                    .build();

            NetworkRequest request =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .setNetworkSpecifier(specifier)
                            .build();

            final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            MeshLog.v("Android_10 Attempt to connect :"+credential.mSSID);
            connectivityManager.requestNetwork(request, networkCallback);

           /*WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
           builder.setSsid(ssid);
           builder.setWpa2Passphrase(passPhrase);

           WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();

           NetworkRequest.Builder networkRequestBuilder1 = new NetworkRequest.Builder();
           networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
           networkRequestBuilder1.setNetworkSpecifier(wifiNetworkSpecifier);

           NetworkRequest nr = networkRequestBuilder1.build();
           System.out.println("Android sdk version is 29 above NetworkRequest");
           final ConnectivityManager cm = (ConnectivityManager)
                   mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
           System.out.println("Android sdk version is 29 above ConnectivityManager");
           ConnectivityManager.NetworkCallback networkCallback = new
                   ConnectivityManager.NetworkCallback()
                   {
                       @Override
                       public void onAvailable(Network network)
                       {
                           super.onAvailable(network);
                           System.out.println("onAvailabile" + network);
                           cm.bindProcessToNetwork(network);

                       }

                       @Override
                       public void onLosing(@NonNull Network network, int maxMsToLive)
                       {
                           super.onLosing(network, maxMsToLive);
                           System.out.println("onLosing" + network);
                       }

                       @Override
                       public void onLost(@NonNull Network network)
                       {
                           super.onLost(network);
                           System.out.println("onLost" + network);
                       }

                       @Override
                       public void onUnavailable()
                       {
                           super.onUnavailable();
                           System.out.println("onUnavaliable");
                       }
                   };
           System.out.println("Android sdk version is 29 above NetworkCallback");
           cm.requestNetwork(nr, networkCallback);
*/

            /*NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setWpa2Passphrase(passPhrase)
                    .setSsidPattern(new PatternMatcher("DIRECT-", PatternMatcher.PATTERN_PREFIX))
                    .setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
                    .setBssid(MacAddress.fromString(macAddress))
                    .setIsHiddenSsid(true)
                    .setSsid(ssid)
                    .build();

            NetworkRequest request =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .setNetworkSpecifier(specifier)
                            .build();

            final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                }

            };

            connectivityManager.requestNetwork(request, networkCallback);*/

            return true;
        } else {


            //Timber.d("Connection_log Initial-%s", ssid);

            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.status = WifiConfiguration.Status.ENABLED;
            wifiConfig.SSID = String.format("\"%s\"", credential.mSSID);
            wifiConfig.preSharedKey = String.format("\"%s\"", credential.mPassPhrase);

            //// TODO: 7/17/2019
            //It automates SSID connection whenever available so it gives us huge performance benefit.
            //To leverage the benefit we need to have according support. Would add later
            //wifiConfig.hiddenSSID = true;

            int networkId = getConfiguredWiFiNetworkId(credential.mSSID);
            if (networkId != -1) {
                wifiConfig.networkId = networkId;
                //Timber.d("Connection_log %s-%s", networkId, ssid);
                networkId = mWifiManager.updateNetwork(wifiConfig);
                //Timber.d("Connection_log %s", networkId);
                if (networkId == -1) {
                    networkId = this.mWifiManager.addNetwork(wifiConfig);
                    //Timber.d("Connection_log %s", networkId);
                }
            } else {
                networkId = this.mWifiManager.addNetwork(wifiConfig);
                //Timber.d("Connection_log %s-%s", networkId, ssid);
            }
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(networkId, true);
            boolean status = mWifiManager.reconnect();
            MeshLog.i("[p2p_process]: connection attempt status :" + status + " Id :" + networkId);
            return status;
        }
    }

    public WifiInfo getConnectionInfo() {

        if (mWifiManager == null) {
            return null;
        }
        return mWifiManager.getConnectionInfo();
    }

    public boolean isWiFiOn() {

        return mWifiManager != null && mWifiManager.isWifiEnabled();
    }

    public void removeNetwork(int netId) {
        if (netId > 0 && mWifiManager != null) {
            boolean status = mWifiManager.removeNetwork(netId);
            mWifiManager.saveConfiguration();
            MeshLog.i("Network Removed Status::" + status);
        }

    }

    public void setHighBand() {
        if (!mWifiLock.isHeld()) {
            MeshLog.v("WiFi locked:" + mWifiLock);
            mWifiLock.acquire();
        }
    }

    public void releaseHighBand() {
        if (mWifiLock.isHeld()) {
            MeshLog.v("WiFi lock release called:" + mWifiLock);
            mWifiLock.release();
        }
    }

    @SuppressLint("MissingPermission")
    public void forgetNetworks() {

        int networkId = mWifiManager.getConnectionInfo().getNetworkId();
        if (networkId != -1) {
            mWifiManager.removeNetwork(networkId);
            mWifiManager.saveConfiguration();
        } else {
            List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                //int networkId = wifiManager.getConnectionInfo().getNetworkId();
                mWifiManager.removeNetwork(i.networkId);
                mWifiManager.saveConfiguration();
            }
        }

    }

}