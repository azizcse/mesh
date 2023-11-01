package com.w3engineers.mesh;

/*
 *  ****************************************************************************
 *  * Created by : Md. Azizul Islam on 1/14/2019 at 5:06 PM.
 *  * Email : azizul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md. Azizul Islam on 1/14/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */


import android.annotation.SuppressLint;
import android.content.Context;

import com.w3engineers.purchase.db.SharedPref;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class App extends MeshApp {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        SharedPref.on(getApplicationContext());
    }

    public static Context getContext() {
        return mContext;
    }


}
