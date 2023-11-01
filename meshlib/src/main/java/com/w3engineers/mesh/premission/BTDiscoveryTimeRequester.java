package com.w3engineers.mesh.premission;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.w3engineers.mesh.R;
import com.w3engineers.mesh.TransportManager;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.MeshLog;

/**
 * Bluetooth discoverable time requester
 */
public class BTDiscoveryTimeRequester extends AppCompatActivity {

    public static final int REQUEST_ENABLE_DSC = 107;
    private static final String IS_BT_DISCOVERABLE_WARNING_DISPLAYED =
            "IS_BT_DISCOVERABLE_WARNING_DISPLAYED";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isWarningAccepted = SharedPref.readBoolean(IS_BT_DISCOVERABLE_WARNING_DISPLAYED);
        if (isWarningAccepted) {
            requestDiscoverableTimePeriod();
        } else {
            displayWarning();
        }

    }

    private void displayWarning() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = String.format(getString(R.string.bt_discoverable_warning_dialog_message),
                getString(R.string.app_name), getString(R.string.bt_discoverable_warning_proceed_button));

        builder.setMessage(message)
                .setPositiveButton("Proceed", (dialog, id) -> {
                    requestDiscoverableTimePeriod();
                    SharedPref.write(IS_BT_DISCOVERABLE_WARNING_DISPLAYED, true);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                });
        builder.create();
        builder.setCancelable(false);

        builder.show();
    }

    //Set Bluetooth Discoverability Unbounded
    private void requestDiscoverableTimePeriod() {
        if (PreferencesHelper.on().getDataShareMode() != PreferencesHelper.INTERNET_USER) {
            MeshLog.v("requestDiscoverableTimePeriod alert");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            startActivityForResult(intent, REQUEST_ENABLE_DSC);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_DSC) {
            if (resultCode == 3600) {
                TransportManager transportManager = TransportManagerX.getInstance();

                if (transportManager != null) {
                    transportManager.isBtEnabled = true;
                }
            }
        }

        setResult(resultCode);
        finish();
    }
}
