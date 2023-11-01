package com.w3engineers.meshrnd.ui.createuser;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityCreateUserBinding;
import com.w3engineers.meshrnd.ui.nav.BottomNavActivity;

import java.util.UUID;

public class CreateUserActivity extends AppCompatActivity implements View.OnClickListener {


    private ActivityCreateUserBinding mBInding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBInding = DataBindingUtil.setContentView(this, R.layout.activity_create_user);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        toolbar.setTitle("MeshRnd");
        mBInding.createUser.setOnClickListener(this);

        CheckLogin();

    }

    private void CheckLogin() {
        if (!TextUtils.isEmpty(SharedPref.read(Constant.KEY_USER_NAME))
                && !TextUtils.isEmpty(SharedPref.read(Constant.KEY_USER_ID))) {

            Intent intent = new Intent(this, BottomNavActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.create_user) {
            if (!mBInding.editTextName.getText().toString().equals("")) {
                String uuid = UUID.randomUUID().toString();
                goNext(uuid);
                /*ProgressDialog dialog = ProgressDialog.show(this, "",
                        "Creating account. Please wait...", true);
                EthereumService.getInstance(CreateUserActivity.this).createWallet("123456789", new EthereumService.Listener() {
                    @Override
                    public void onWalletCreated(String walletName, String walletAddress) {
                        dialog.dismiss();
                        goNext(walletAddress);
                    }

                    @Override
                    public void onWalletLoaded(String walletAddress) {
                        dialog.dismiss();
                        goNext(walletAddress);
                    }
                });*/

            } else {
                Toast.makeText(this, "Enter your name", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goNext(String userId) {
        SharedPref.write(Constant.KEY_USER_NAME, mBInding.editTextName.getText().toString());
        //SharedPref.write(Constant.KEY_USER_ID, userId);
        Intent intent = new Intent(CreateUserActivity.this, BottomNavActivity.class);
        startActivity(intent);
        finish();
    }
}
