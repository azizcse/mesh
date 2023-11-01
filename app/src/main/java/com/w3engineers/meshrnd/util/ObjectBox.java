package com.w3engineers.meshrnd.util;

import android.content.Context;

import com.w3engineers.meshrnd.BuildConfig;
import com.w3engineers.meshrnd.model.MyObjectBox;

import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

/**
 * This class represents the entry for ObjectBox which is used for local data persistent management
 */
public class ObjectBox {
    private static BoxStore sBoxStore;

    public static void init(Context context) {
        sBoxStore = MyObjectBox.builder()
                .androidContext(context.getApplicationContext())
                .build();
       if (BuildConfig.DEBUG) {
           boolean started = new AndroidObjectBrowser(sBoxStore).start(context);
       }
    }

    public static BoxStore get() {
        return sBoxStore;
    }
}
