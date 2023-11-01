package com.w3engineers.mesh.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import com.w3engineers.mesh.App;
import com.w3engineers.models.SupportedApp;
import com.w3engineers.purchase.db.clientinfo.ClientInfoEntity;
import com.w3engineers.purchase.util.ClientInfoSyncUtil;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Generate application key like md5, sha1 , sha256</p>
 * Developer <Azizul Islam>
 */

public class AppSecurityUtil {

    /**
     * KEY constant
     */
    public interface KeyType {
        String MD5 = "MD5";
        String SHA1 = "SHA1";
        String SHA256 = "SHA256";
    }

    private static List<SupportedApp> appList = new ArrayList<>();
    private static Map<String, String> allowedAppShaKeyMap;

    /**
     * <h1>Put your debug or release app fingerprint</h1>
     *
     * Developer must add SHA256 key before release service apk
     * to support specific client app
     */
    static {
        allowedAppShaKeyMap = new HashMap<>();
        allowedAppShaKeyMap.put("com.w3engineers.ext.viper",
                "7D:15:67:46:2D:D5:F4:51:DE:88:26:F9:AC:1F:FC:81:35:EE:54:3A:D9:7B:25:71:CE:B3:9F:6D:D8:EF:F3:02");
    }

    /**
     * <p>Check existing app id and sha256 key </p>
     *
     * @param appId  (required) : String this is the client app package name
     * @param shaKey (required) : String the client app sha256 key found by programmatically
     * @return boolean if match return true , false otherwise
     */
    public static boolean isSupportedAppIdAndSha256(Context context, String appId, String shaKey) {
        /*readSupportedAppFile(context);

        if (appList.isEmpty()) {
            return false;
        }
        Log.e("TMeshService", "App :" +appId+" Sha :"+shaKey);
        for (SupportedApp item : appList) {
            if(appId.equals(item.getAppId()) && shaKey.equals(item.getSha256())){
                Log.e("TMeshService", "App :" +appId+" Valide app");
                return true;
            }
        }*/

        /*
         * We are taking client information from local database
         * */

        ClientInfoEntity entity = ClientInfoSyncUtil.getInstance().getClientInfoByToken(appId);

        if (entity != null) {
            if (entity.shaKey.equals(shaKey)) {
                return true;
            }
        }

        //Comment out the previous code

        /*if (!allowedAppShaKeyMap.containsKey(appId)) return false;
        String allowedShawKey = allowedAppShaKeyMap.get(appId);
        if (shaKey.equals(allowedShawKey)) return true;*/
        Log.e("TMeshService", "App :" + appId + " In Valide app");
        return false;
    }


    /**
     * <h1>Generate expected key</h1>
     * <p>
     * Key type takes as parameter and generate the desire key
     *
     * @param context Context (required)
     * @param keyType String like: SHA1, SHA256, MD5
     * @param appId   String actually application package name
     * @return String expected key
     */
    @SuppressLint("PackageManagerGetSignatures") // test purpose
    public static String getClientAppFingerPrint(Context context, String keyType, String appId) {
        try {
            final PackageInfo info = context.getPackageManager()
                    .getPackageInfo(appId, PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                final MessageDigest md = MessageDigest.getInstance(keyType);
                md.update(signature.toByteArray());

                final byte[] digest = md.digest();
                final StringBuilder toRet = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    if (i != 0) toRet.append(":");
                    int b = digest[i] & 0xff;
                    String hex = Integer.toHexString(b);
                    if (hex.length() == 1) toRet.append("0");
                    toRet.append(hex);
                }

                Log.e(AppSecurityUtil.class.getSimpleName(), keyType + " " + toRet.toString());
                return toRet.toString().toUpperCase();
            }
        } catch (PackageManager.NameNotFoundException e1) {
            Log.e("name not found", e1.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("no such an algorithm", e.toString());
        } catch (Exception e) {
            Log.e("exception", e.toString());
        }
        return "";
    }

    private static void readSupportedAppFile(Context context) {
        File localDir = context.getFilesDir();
        String filePath = localDir.getAbsolutePath() + "/cofig/sapp/config_json.json";
        if (new File(filePath).exists()) {
            if (appList.isEmpty()) {
                loadSupportedAppList(filePath);
            }
        }
        copyFileToPrivateDir(context);
    }

    private static void copyFileToPrivateDir(Context context) {
        try {
            InputStream stream = context.getAssets().open("config_json.zip");
            File fileDir = context.getFilesDir();
            String localAssetDir = fileDir.getAbsolutePath() + "/config/";
            Log.e("local_dir", "Dir :" + localAssetDir);
            File localDir = new File(localAssetDir);

            localDir.mkdirs();
            File savedFile = createFileFromInputStream(stream, localAssetDir);

            ZipFile zipFile = new ZipFile(savedFile.getAbsolutePath());
            String extractPath = localDir.toString() + "/sapp/";

            File exFile = new File(extractPath);
            exFile.mkdir();

            if (zipFile.isEncrypted()) {
                String md5 = AppSecurityUtil.getClientAppFingerPrint(
                        context,
                        AppSecurityUtil.KeyType.MD5, context.getPackageName());
                zipFile.setPassword(md5);
            }

            zipFile.extractAll(extractPath);

            File check = new File(extractPath + "/config_json.json");
            if (check.exists()) {
                loadSupportedAppList(check.getAbsolutePath());
            } else {
                Log.e("local_dir", "Local file not exist");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ZipException e) {

        }
    }

    private static File createFileFromInputStream(InputStream inputStream, String path) {
        try {
            File f = new File(path, "/config_json.zip");

            OutputStream out = new FileOutputStream(f);

            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.close();
            inputStream.close();
            return f;
        } catch (IOException e) {
            //Logging exception
        }
        return null;
    }

    public static String loadSupportedAppList(String filePath) {
        String json = null;
        try {
            InputStream is = new FileInputStream(new File(filePath));
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

            JSONObject jo = new JSONObject(json);
            JSONArray jsonArray = jo.getJSONArray("app");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject app = (JSONObject) jsonArray.get(i);
                appList.add(new SupportedApp(app.getString("type"), app.getString("app_id"),
                        app.getString("sha")));

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (JSONException e) {

        }
        return json;
    }


}
