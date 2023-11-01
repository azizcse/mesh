package com.w3engineers.mesh.queue;


import com.w3engineers.mesh.bluetooth.BleLink;
import com.w3engineers.mesh.queue.messages.BTDiscoveryMessage;
import com.w3engineers.mesh.queue.messages.BTMessage;
import com.w3engineers.mesh.queue.messages.WiFiDiscoverMessage;
import com.w3engineers.mesh.queue.messages.WiFiMessage;

import org.json.JSONObject;

/**
 * Message builder for Wifi and Bluetooth
 */
public class MessageBuilder {

    public static WiFiMessage buildMeshWiFiMessage(String ip, byte[] data) {
        WiFiMessage wiFiMessage = new WiFiMessage();
        wiFiMessage.mIp = ip;
        wiFiMessage.mData = data;
        return wiFiMessage;
    }

    public static WiFiMessage buildMeshWiFiMessage(String messageId, String ip, byte[] data) {
        WiFiMessage wiFiMessage = new WiFiMessage();
        wiFiMessage.mIp = ip;
        wiFiMessage.mData = data;
        wiFiMessage.messageId = messageId;
        return wiFiMessage;
    }

    public static WiFiDiscoverMessage buildWiFiDiscoveryMeshMessage(String ip, WiFiDiscoverMessage.DataPuller dataPuller) {
        WiFiDiscoverMessage wiFiDiscoverMessage = new WiFiDiscoverMessage(dataPuller);
        wiFiDiscoverMessage.mIp = ip;
        return wiFiDiscoverMessage;
    }

    /* public static WiFiMessage buildMeshWiFiMessage(String ip, Frames.Frame frame) {
         WiFiMessage wiFiMessage = new WiFiMessage();
         wiFiMessage.mIp = ip;
         wiFiMessage.mFrame = frame;
         return wiFiMessage;
     }*/
    public static WiFiMessage buildMeshWiFiMessage(String ip, JSONObject jo) {
        WiFiMessage wiFiMessage = new WiFiMessage();
        wiFiMessage.mIp = ip;
        wiFiMessage.jsonObject = jo;
        return wiFiMessage;
    }

    /* public static WiFiDiscoverMessage buildWiFiDiscoveryMeshMessage(String ip, Frames.Frame frame) {
         WiFiDiscoverMessage wiFiDiscoverMessage = new WiFiDiscoverMessage();
         wiFiDiscoverMessage.mIp = ip;
         wiFiDiscoverMessage.mFrame = frame;
         return wiFiDiscoverMessage;
     }*/
    public static WiFiDiscoverMessage buildWiFiDiscoveryMeshMessage(String ip, JSONObject jo) {
        WiFiDiscoverMessage wiFiDiscoverMessage = new WiFiDiscoverMessage();
        wiFiDiscoverMessage.mIp = ip;
        wiFiDiscoverMessage.jsonObject = jo;
        return wiFiDiscoverMessage;
    }

    public static BTMessage buildMeshBtMessage(BleLink link, byte[] data) {

        BTMessage btMessage = new BTMessage();
        btMessage.mData = data;
        btMessage.mBleLink = link;
        return btMessage;
    }

    /*public static BTMessage buildMeshBtMessage(BleLink link, Frames.Frame frame) {

        BTMessage btMessage = new BTMessage();
        btMessage.mFrame = frame;
        btMessage.mBleLink = link;
        return btMessage;
    }*/

    public static BTMessage buildMeshBtMessage(BleLink link, JSONObject jo) {

        BTMessage btMessage = new BTMessage();
        btMessage.jsonObject = jo;
        btMessage.mBleLink = link;
        return btMessage;
    }

    /*public static BTDiscoveryMessage buildMeshBtDiscoveryMessage(BleLink link, Frames.Frame frame) {

        BTDiscoveryMessage btDiscoveryMessage = new BTDiscoveryMessage();
        btDiscoveryMessage.mFrame = frame;
        btDiscoveryMessage.mBleLink = link;
        return btDiscoveryMessage;
    }*/

    public static BTDiscoveryMessage buildMeshBtDiscoveryMessage(BleLink link, JSONObject jo) {

        BTDiscoveryMessage btDiscoveryMessage = new BTDiscoveryMessage();
        btDiscoveryMessage.jsonObject = jo;
        btDiscoveryMessage.mBleLink = link;
        return btDiscoveryMessage;
    }

    public static BTDiscoveryMessage buildMeshBtDiscoveryMessage(BleLink link, BTDiscoveryMessage.DataPuller dataPuller) {

        BTDiscoveryMessage btDiscoveryMessage = new BTDiscoveryMessage(dataPuller);
        btDiscoveryMessage.mBleLink = link;
        return btDiscoveryMessage;
    }

    /*public static BaseMeshMessage buildMeshInternetMessage(InternetLink link, String sender, String receiver, String messageId, byte[] data) {

        InternetMessage internetMessage = new InternetMessage();
        internetMessage.mData = data;
        internetMessage.mInternetLink = link;
        internetMessage.mSenderId = sender;
        internetMessage.mReceiverId = receiver;
        internetMessage.mMessageId = messageId;
        internetMessage.mIsAckMessage = false;
        return internetMessage;
    }*/


    /*public static BaseMeshMessage buildInternetAckMessage(InternetLink link, String sender, String receiver, String messageId) {

        InternetMessage internetMessage = new InternetMessage();
        internetMessage.mInternetLink = link;
        internetMessage.mSenderId = sender;
        internetMessage.mReceiverId = receiver;
        internetMessage.mMessageId = messageId;
        internetMessage.mIsAckMessage = true;
        return internetMessage;
    }*/


    /*public static InternetMessage buildInternetReceiveMessage(InternetLink internetLink, String message, int type, String sender, String receiver) {
        InternetMessage internetMessage = new InternetMessage();
        internetMessage.mInternetLink = internetLink;
        internetMessage.mData = message.getBytes();
        internetMessage.mType = type;
        internetMessage.mSenderId = sender;
        internetMessage.mReceiverId = receiver;
        return internetMessage;
    }*/
}
