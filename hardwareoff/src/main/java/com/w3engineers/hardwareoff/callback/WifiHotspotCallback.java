package com.w3engineers.hardwareoff.callback;

/*
 *  ****************************************************************************
 *  * Created by : Md Tariqul Islam on 8/6/2019 at 6:00 PM.
 *  * Email : tariqul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md Tariqul Islam on 8/6/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */

public interface WifiHotspotCallback {
    void onEnabledHotspot(String ssid, String key);
    void onDisabledHotspot();
    void onError(int reason);
}
