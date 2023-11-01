package com.w3engineers.mesh.util;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.w3engineers.ext.strom.App;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Provides SDK log related service
 */
public class MeshLog {

    public interface MeshLogListener {
        void onNewLog(String text);
    }

    public static MeshLogListener sMeshLogListener;
    private static LinkStateListener linkStateListener;

    private static String TAG = "MeshLog";

    public static final String INFO = "(I)";
    public static final String WARNING = "(W)";
    public static final String ERROR = "(E)";
    public static final String SPECIAL = "(S)";
    public static final String PAYMENT = "(P)";
    public static final String SSH = "(sh)";

    private static OutputStreamWriter sStreamWriter;

    private static String addTimeWithType(String type, String msg) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        return type.concat(" ").concat(currentTime).concat(": ").concat(msg);
    }

    public static void initListener(LinkStateListener listener) {
        linkStateListener = listener;
    }

    public static void clearLog() {
        writeText("", false);
    }


    public static void p(String msg) {
        String m = addTimeWithType(PAYMENT, msg);
        e(TAG, m);
        writeText(m, true);
    }

    public static void o(String msg) {
        p(msg);
//        e(TAG, msg);
//        writeText(msg, true);
    }

    public static void k(String msg) {
        String m = addTimeWithType(SPECIAL, msg);
        e(TAG, m);
        writeText(m, true);
    }

    public static void v(String msg) {
        String m = addTimeWithType(SPECIAL, msg);
        v(TAG, m);
        writeText(m, true);
    }

    public static void ssh(String msg) {
        String m = addTimeWithType(SPECIAL, msg);
        v(TAG, m);
        writeText(m, true);
    }

    public static void mm(String msg) {
        String m = addTimeWithType(SPECIAL, msg);

        e(TAG, m);
        writeText(m, true);
    }

    public static void i(String msg) {
        String m = addTimeWithType(INFO, msg);
        i(TAG, m);
        writeText(m, true);
    }


    public static void e(String msg) {
        String m = addTimeWithType(ERROR, msg);
        e(TAG, m);
        writeText(m, true);
    }

    public static void w(String msg) {
        String m = addTimeWithType(PAYMENT, msg);
        w(TAG, m);
        writeText(m, true);
    }


    private static void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    private static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    private static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }


    public static void destroy() {
        if (sStreamWriter != null) {
            try {
                sStreamWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Executor sExecutor = Executors.newFixedThreadPool(3);

    private static void writeText(String text, boolean isAppend) {
        //This log method is used in meshSDK and viper both. Viper adds some more with this. It
        // should have some clean path
        // TODO: 12/20/2019 We will separate log process completely. For now it is just being
        //maintained to be workable in all projects. Monir

        Context context = App.getContext();

        //Todo we just commented this code because of Viper not support 29 yet

/*        if (context != null && context.getPackageName().equals("com.w3engineers." +
                "meshservice")) {
            if (linkStateListener != null) {
                try {
                    linkStateListener.onLogTextReceive(text);
                } catch (RemoteException e) {
//                    e.printStackTrace();
                }
            }
        } else {*/
            sExecutor.execute(() -> {

                if (sMeshLogListener != null) {
                    sMeshLogListener.onNewLog(text);
                }

                if (linkStateListener != null) {
                    try {
                        linkStateListener.onLogTextReceive(text);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }


                try {
                    if (sStreamWriter == null) {

                        //String sdCard = Constant.Directory.PARENT_DIRECTORY + Constant.Directory.MESH_LOG;
                        Constant.Directory directoryContainer =  new Constant.Directory();
                        String sdCard = directoryContainer.getParentDirectory() + Constant.Directory.MESH_LOG;
                        File directory = new File(sdCard);
                        if (!directory.exists()) {
                            directory.mkdirs();
                        }
                        if (Constant.CURRENT_LOG_FILE_NAME == null) {
                            String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                            SharedPref.write(Constant.KEY_CURRENT_LOG_FILE_NAME, currentDate);
                            Constant.CURRENT_LOG_FILE_NAME = currentDate + ".txt";
                        }
                        File file = new File(directory, Constant.CURRENT_LOG_FILE_NAME);
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream fOut = new FileOutputStream(file, isAppend);

                        sStreamWriter = new
                                OutputStreamWriter(fOut);
                    }

                    sStreamWriter.write("\n" + text);
                    sStreamWriter.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
       // }

    }

}