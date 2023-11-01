/*
package com.w3engineers.internet.webrtc;

import android.util.Log;

import com.w3engineers.internet.InternetLink;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

*/
/*
 *  ****************************************************************************
 *  * Created by : Md Tariqul Islam on 10/22/2019 at 6:19 PM.
 *  * Email : tariqul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md Tariqul Islam on 10/22/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 *//*



public class PeerConnectionHolder {
    private static final String TAG = "InternetMsg ";
    private static HashMap<String, PeerConnectionHelper> connectionHolderList = new HashMap<>();

    public static void addPeerConnection(String userId, PeerConnectionHelper connectionHelper) {

        if (!connectionHolderList.containsKey(userId)) {
            connectionHolderList.put(userId, connectionHelper);
        }
    }

    public static PeerConnectionHelper getPeerConnection(String userId) {

        if (connectionHolderList.containsKey(userId)) {
            return connectionHolderList.get(userId);
        }
        return null;
    }

    public static void removePeerConnection(String userId) {
        connectionHolderList.remove(userId);
    }

    public static void closeSingleConnection(String userId) {
        MeshLog.v(TAG + "  Removing internet user for override local user");
        PeerConnectionHelper connectionHelper = connectionHolderList.get(userId);
        if (connectionHelper != null) {
            connectionHelper.getPeerConnection().close();
            connectionHelper.getLocalDataChannel().close();

            removePeerConnection(userId);
        }
    }

    public static void removeAll() {
        MeshLog.v(TAG + " PeerConnectionHolder removeAll");


        closeAllDataChannel();
        connectionHolderList.clear();
        InternetLink internetLink = InternetLink.getInstance();
        if (internetLink != null) {
            internetLink.clearSelfBuyerUserList();
        }


    }

    public static void closeAllDataChannel() {

        List<String> keyList = new ArrayList<>();
        keyList.addAll(connectionHolderList.keySet());

        for (String key : keyList) {
            PeerConnectionHelper connectionHelper = connectionHolderList.get(key);
            if (connectionHelper != null) {
                connectionHelper.getPeerConnection().close();
                connectionHelper.getLocalDataChannel().close();
            }
        }

    }

    public static HashMap<String, PeerConnectionHelper> getConnectionHolderList() {
        return connectionHolderList;
    }

}
*/
