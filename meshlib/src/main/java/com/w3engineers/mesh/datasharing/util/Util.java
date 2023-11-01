package com.w3engineers.mesh.datasharing.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.datasharing.database.DatabaseService;
import com.w3engineers.mesh.datasharing.database.message.Message;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.util.Constant;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Generic utility provider
 */
public class Util {

    private static final int DELETE_INTERVAL = 7;

    public static double convertBytesToMegabytes(long bytes) {
        double val = (double) bytes / (1024.0 * 1024.0);
//        MeshLog.p("val " + val);
        return val;
    }

    public static long convertMegabytesToBytes(double mb) {
        return (long) mb * 1024 * 1024;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        String st = String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
        return st;
    }

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, true);
    }


    public static String buildInternetSendingMessage(String sender, String originalReceiver, byte[] data) {

        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_MODE, PurchaseConstants.MESSAGE_MODE.INTERNET_SEND);
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_SENDER, sender);
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER, originalReceiver);
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_DATA, new String(data));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }

    public static String buildLocalMessage(byte[] data) {

        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_MODE, PurchaseConstants.MESSAGE_MODE.LOCAL);
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_DATA, new String(data));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }

    public static String buildInternetReceivingMessage(byte[] data, String sellerId) {

        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_MODE, PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE);
            js.put(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS, sellerId);
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_DATA, new String(data));

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }

    public static String buildInternetSendingAckBody(String originalReceiver) {
        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.ACK_MODE, PurchaseConstants.MESSAGE_MODE.INTERNET_SEND_ACK);
            js.put(PurchaseConstants.JSON_KEYS.MESSAGE_RECEIVER, originalReceiver);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }

    public static String buildInternetReceivingAckBody(String sellerId) {
        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.ACK_MODE, PurchaseConstants.MESSAGE_MODE.INTERNET_RECEIVE_ACK);
            js.put(PurchaseConstants.JSON_KEYS.SELLER_ADDRESS, sellerId);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }

    public static String buildLocalAckBody() {
        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.ACK_MODE, PurchaseConstants.MESSAGE_MODE.LOCAL_ACK);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }


    public static String buildWebRtcMessage(String senderId, String receiverId, String messageId, byte[] messageData) {

        JSONObject mainObject = new JSONObject();
        try {
            String message = new String(messageData);
            if (!TextUtils.isEmpty(message)) {
                JSONObject jsonObject = null;

                jsonObject = new JSONObject();
                jsonObject.put("text", message);
                jsonObject.put("txn", messageId);

                JSONObject headerObject = new JSONObject();
                headerObject.put(Constant.JsonKeys.TYPE, Constant.DataType.USER_MESSAGE);
                headerObject.put(Constant.JsonKeys.SENDER, senderId);
                headerObject.put(Constant.JsonKeys.RECEIVER, receiverId);

                mainObject.put(Constant.JsonKeys.HEADER, headerObject);
                mainObject.put(Constant.JsonKeys.MESSAGE, jsonObject);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mainObject.toString();
    }

    public static boolean saveMessage(String senderId, String receiverId, String messageId, byte[] data, Context mContext) {
        if (!senderId.equals(receiverId)) {

            Message message = null;
            try {
                message = DatabaseService.getInstance(mContext).getMessageById(messageId);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            if (message != null) {
                return true;
            } else {
                message = new Message();
                message.setReceiverId(receiverId);
                message.setSenderId(senderId);
                message.setMessageId(messageId);
                message.setData(data);
                message.isIncoming = false;
                message.setAppToken(getTokenFromData(data));

                DatabaseService.getInstance(mContext).insertMessage(message);
                return true;
            }
        }
        return false;
    }

    public static String parseRoleCode(int reason) {
        if (reason <= RoutingEntity.Type.LC && reason > -2) {
            switch (reason) {
                case RoutingEntity.Type.WiFi:
                    return "WiFi P2P";
                case RoutingEntity.Type.BT:
                    return "BT";
                case RoutingEntity.Type.WifiMesh:
                    return "Wifi Mesh";
                case RoutingEntity.Type.BtMesh:
                    return "Bt Mesh";
                case RoutingEntity.Type.INTERNET:
                    return "Internet";
                case RoutingEntity.Type.HB:
                    return "Hybrid";
                case RoutingEntity.Type.HB_MESH:
                    return "Hybrid_MESH";
                case RoutingEntity.Type.HS:
                    return "Hotspot";
                case RoutingEntity.Type.CLIENT:
                    return "Hotspot Client";
                case RoutingEntity.Type.GO:
                    return "Group Owner";
                case RoutingEntity.Type.LC:
                    return "Legacy Client";
                default:
                    return "NONE";
            }

        }
        return "UNKNOWN";
    }


    private static String getTokenFromData(byte[] data) {
        String msg = new String(data);
        try {
            JSONObject obj = new JSONObject(msg);
            return obj.optString(Constant.KEY_APP_TOKEN);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "";
    }


    @VisibleForTesting
    public static String constantsToString(int index, Integer... constants) {
        if (CollectionUtil.hasItem(constants) && index > -1 && index < constants.length) {

            List<Integer> integers = Arrays.asList(constants);

        }
        return null;
    }

    public static String parseRoleCode(int[] roles) {

        String rolesString = null;
        if (roles != null && roles.length > 0) {
            rolesString = "";
            String separator = ", ";
            for (int role : roles) {
                rolesString += parseRoleCode(role);
                rolesString += separator;
            }
            rolesString = rolesString.substring(0, rolesString.lastIndexOf(separator));
        }

        return rolesString;
    }

    public static boolean hasItemIn(int item, int[] list) {
        int position = -1;
        if (list != null && list.length > 0) {
            Arrays.sort(list);
            position = Arrays.binarySearch(list, item);
        }

        return position > -1;
    }

    public static void checkDeviceLogValidity() {
        String savedDate = SharedPref.read(Constant.KEY_CURRENT_LOG_FILE_NAME);
        if (TextUtils.isEmpty(savedDate)) {
            storeCurrentDate();
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        try {
            Date oldDate = format.parse(savedDate);

            String currentDateString = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            Date currentDate = format.parse(currentDateString);
            // diff calculation
            long diffDay = getDifference(currentDate, oldDate);
            // after every seven days previous log will delete
            if (diffDay > DELETE_INTERVAL) {
                deletePreviousLog();
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    // delete previous log history
    private static void deletePreviousLog() {
        Constant.Directory directoryContainer = new Constant.Directory();
        String sdCard = directoryContainer.getParentDirectory() + Constant.Directory.MESH_LOG;
        File dir = new File(sdCard);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null && children.length > 0) {
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
                // store recent date
                storeCurrentDate();
            } else {
                storeCurrentDate();
            }

        }
    }

    private static void storeCurrentDate() {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        SharedPref.write(Constant.KEY_CURRENT_LOG_FILE_NAME, currentDate);
    }

    public static long getDifference(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : " + endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        return different / daysInMilli;
    }

    public static String buildRemoteAckBody() {
        JSONObject js = new JSONObject();
        try {
            js.put(PurchaseConstants.JSON_KEYS.ACK_MODE, PurchaseConstants.MESSAGE_MODE.LOCAL_ACK);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return js.toString();
    }
}
