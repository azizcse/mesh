package com.w3engineers.mesh.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.w3engineers.meshrnd.R;
import com.w3engineers.walleter.wallet.Web3jWalletHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

public class WalletAddressHelper {
    private static String walletSuffixDir;
    public static String DEFAULT_ADDRESS;
    public static String CURRENT_ADDRESS;

    private static String readWalletAddress(Context context) {
        walletSuffixDir = "wallet/" + context.getResources().getString(R.string.wallet_directory_name);

        String filePath = Web3jWalletHelper.onInstance(context).getWalletDir(walletSuffixDir);
        File directory = new File(filePath);
        StringBuilder result = new StringBuilder();
        File[] list = directory.listFiles();
        if (list != null && list.length > 0) {
            File walletFile = list[0];
            if (walletFile.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(walletFile);
                    char current;
                    while (fis.available() > 0) {
                        current = (char) fis.read();
                        result.append(current);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fis != null)
                        try {
                            fis.close();
                        } catch (IOException ignored) {
                        }
                }
            }
        }

        if (TextUtils.isEmpty(result.toString())) {
            return result.toString();
        }

        try {
            JSONObject walletObj = new JSONObject(result.toString());
            result = new StringBuilder("0x" + walletObj.optString("address"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

    private static String readDefaultAddress(Context context) {
        File file;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            file = new File(context.getExternalFilesDir("").toString() + "/" +
                    context.getString(R.string.wallet_directory_name) + "/" + Utils.ServiceConstant.DEFAULT_ADDRESS);
        } else {
            file = new File(Environment.getExternalStorageDirectory().toString() + "/" +
                    context.getString(R.string.wallet_directory_name) + "/" + Utils.ServiceConstant.DEFAULT_ADDRESS);
        }


        File addressFile = new File(file, Utils.ServiceConstant.DEFAULT_ADDRESS_FILE);

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(addressFile));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text.toString();
    }

    public static SpannableString getWalletSpannableString(Context context) {
        String walletText = context.getResources().getString(R.string.file_exit);
        String address = WalletAddressHelper.readWalletAddress(context);

        DEFAULT_ADDRESS = readDefaultAddress(context);
        CURRENT_ADDRESS = address;

        String walletExistMessage = String.format(walletText, address);
        SpannableString spannableString = new SpannableString(walletExistMessage);


        spannableString.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.hint_text_color)),
                1, 15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int startIndex = walletExistMessage.length() - address.length();

        spannableString.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.hint_text_color)),
                startIndex, walletExistMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    public static void writeDefaultAddress(String address, Context context) {
        File file;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            file = new File(context.getExternalFilesDir("") + "/" +
                    context.getString(R.string.app_name));
        } else {
            file = new File(Environment.getExternalStorageDirectory().toString() + "/" +
                    context.getString(R.string.app_name));
        }


        if (!file.exists()) {
            file.mkdirs();
        }

        File directory = new File(file, Utils.ServiceConstant.DEFAULT_ADDRESS);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File defaultAddressFile = new File(directory, Utils.ServiceConstant.DEFAULT_ADDRESS_FILE);

        try {
            defaultAddressFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(defaultAddressFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            outputStreamWriter.append(address);
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
