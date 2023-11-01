package com.w3engineers.mesh.db;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.datasharing.helper.CommonUtil;
import com.w3engineers.mesh.util.Constant;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;


/**
 * SharedPreference helper
 */

public class SharedPref {

   // private final static String PREFERENCE_NAME = "mesh_rnd";
    private static SharedPreferences preferences;
    private static SharedPref sharedPref;
    private static BehaviorSubject<Integer> totalMessageSent = BehaviorSubject.create();
    private static BehaviorSubject<Integer> totalMessageRcv = BehaviorSubject.create();

    private SharedPref() {
    }
/*
    public SharedPref(Context mContext) {
        preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }
*/

    public static SharedPref on(Context context) {
        if (BuildConfig.DEBUG) {
            String PACKAGE_NAME = context.getApplicationContext().getPackageName();
            preferences = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
            if (sharedPref == null) {
                sharedPref = new SharedPref();
            }
        } else {
            if (!CommonUtil.isEmulator()) {
                String PACKAGE_NAME = context.getApplicationContext().getPackageName();
                preferences = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
                if (sharedPref == null) {
                    sharedPref = new SharedPref();
                }
            }
        }

        return sharedPref;
    }

    public static boolean write(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static boolean write(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(key, value);

        return editor.commit();
    }

    public static boolean write(String key, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);

        if (key.equalsIgnoreCase(Constant.PreferenceKeys.TOTAL_MESSAGE_SENT)){
            totalMessageSent.onNext(value);
        }

        if (key.equalsIgnoreCase(Constant.PreferenceKeys.TOTAL_MESSAGE_RCV)){
            totalMessageRcv.onNext(value);
        }

        return editor.commit();
    }

    public static boolean write(String key, long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    public static boolean write(String key, double value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, Double.doubleToRawLongBits(value));
        return editor.commit();
        //return new Editor(_editor.putLong(key, Double.doubleToRawLongBits(value)));
    }


    public static String read(String key) {
        return preferences.getString(key, "");
    }

    public static String read(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public static long readLong(String key) {
        return preferences.getLong(key, 0l);
    }

    public static double readDouble(String key) {
        return Double.longBitsToDouble(preferences.getLong(key, Double.doubleToRawLongBits(0.0)));
    }

    public static int readInt(String key) {
        return readInt(key, 0);
    }

    public static int readInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    @Nullable
    public static Flowable<Integer> getTotalMessageSent() {
        return totalMessageSent.toFlowable(BackpressureStrategy.LATEST);
    }

    @Nullable
    public static Flowable<Integer> getTotalMessageRcv() {
        return totalMessageRcv.toFlowable(BackpressureStrategy.LATEST);
    }

    public static boolean readBoolean(String key) {
        return readBoolean(key, false);
    }

    public static boolean readBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public static boolean readBooleanWithDefaultValue(String key, boolean value) {
        return preferences.getBoolean(key, value);
    }

    public boolean readSettingsBoolean(String key) {
        return preferences.getBoolean(key, true);
    }

    public static boolean readBooleanDefaultTrue(String key) {
        return preferences.getBoolean(key, true);
    }

    public static boolean contains(String key) {
        return preferences.contains(key);
    }

    /**
     * Remove all saved shared Preference data of app.
     */
    public static void removeAllPreferenceData() {
        preferences.edit().clear().apply();
    }

    public static void removeSpecificItem(String key) {
        preferences.edit().remove(key).apply();
    }

}