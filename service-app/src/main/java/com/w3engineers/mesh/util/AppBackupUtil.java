package com.w3engineers.mesh.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

public class AppBackupUtil {
    private static String backupFolder = ".backup";
    private static long appSize = 0;

    /**
     * get application info and preparing a apk and save it in local storage
     *
     * @param context - Need an application context for getting package name
     * @return - saved apk path
     */
    @Nullable
    public static String backupApkAndGetPath(@NonNull Context context) {

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List pkgAppsList = context.getPackageManager().queryIntentActivities(mainIntent, 0);

        for (Object object : pkgAppsList) {

            ResolveInfo resolveInfo = (ResolveInfo) object;
            File appFile = new File(resolveInfo.activityInfo.applicationInfo.publicSourceDir);

            try {

                String file_name = resolveInfo.loadLabel(context.getPackageManager()).toString();

                if (file_name.equalsIgnoreCase(context.getString(com.w3engineers.mesh.R.string.app_name)) &&
                        appFile.toString().contains(context.getPackageName())) {

                    // we are concating file name with time stamp for generating unique path.
                    file_name += System.currentTimeMillis();

                    File file = new File(context.getFilesDir().toString() + "/" +
                            context.getString(com.w3engineers.mesh.R.string.app_name));

                    //Todo we have to check for android 11
                    file.mkdirs();
                    // Preparing a backup apk folder and it is hidden
                    File backUpFolder = new File(file.getAbsolutePath() + "/" + backupFolder);

                    /*if (backUpFolder.exists()) {
                        boolean isDeleted = backUpFolder.delete();
                        MeshLog.i("Is backup folder deleted: " + isDeleted);
                    }*/

                    //Todo we have to check for android 11
                    backUpFolder.mkdirs();
                    backUpFolder = new File(backUpFolder.getPath() + "/" + file_name + ".apk");

                    backUpFolder.createNewFile();

                    InputStream in = new FileInputStream(appFile);
                    OutputStream out = new FileOutputStream(backUpFolder);

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    return backUpFolder.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static String moveFileToRoot(String mainFilePath, Context context) {
        File file = new File(context.getFilesDir().toString() + "/" +
                context.getString(com.w3engineers.mesh.R.string.app_name));
        //Todo we have to check for android 11
        file.mkdirs();
        // Preparing a backup apk folder and it is hidden
        File backUpFolder = new File(file.getAbsolutePath() + "/" + backupFolder);

                    /*if (backUpFolder.exists()) {
                        boolean isDeleted = backUpFolder.delete();
                        MeshLog.i("Is backup folder deleted: " + isDeleted);
                    }*/

        backUpFolder.mkdirs();

        File mainFile = new File(mainFilePath);

        appSize = mainFile.length();

        File destinationFile = new File(backUpFolder.getPath(), mainFile.getName());


        try {
            FileChannel inChannel = new FileInputStream(mainFile).getChannel();
            FileChannel outChannel = new FileOutputStream(destinationFile).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } catch (IOException e) {
                e.printStackTrace();
                return mainFilePath;
            } finally {
                if (inChannel != null) {
                    inChannel.close();
                }
                outChannel.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
            return mainFilePath;
        }

        return destinationFile.getAbsolutePath();
    }

    public static String getAppSize(){
        return String.valueOf(appSize);
    }

}
