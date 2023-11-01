package com.w3engineers.mesh.premission;


import android.app.AlertDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Provides APP restart message for user
 */
public class RestartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Sorry! The device requires a restart to perform well");
        builder1.setCancelable(false);
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

}
