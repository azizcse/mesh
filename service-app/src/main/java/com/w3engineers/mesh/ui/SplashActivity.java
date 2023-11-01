package com.w3engineers.mesh.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.meshrnd.R;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY_MS = 1000;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        startMainActivity();
    }

    private void startMainActivity(){
        HandlerUtil.postForeground(new Runnable() {
            @Override
            public void run() {

                startActivity(new Intent(SplashActivity.this, TeleMeshServiceMainActivity.class));
                finish();
                // close splash activity

            }
        }, SPLASH_DELAY_MS);
    }
}
