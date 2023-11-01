package com.w3engineers.purchase.ui.wallet;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.purchase.db.SharedPref;
import com.w3engineers.purchase.manager.PurchaseConstants;


public class AddressLayout extends Dialog implements View.OnClickListener {

    public Activity c;
    // public Dialog d;
    private String TAG = "Address Layout";
    public Button copyBtn;
    Bitmap bitmap;
    String address;

    public AddressLayout(Activity a, String address) {
        super(a);

        this.c = a;
        this.address = address;

        //TODO
        String bitmapString = SharedPref.read(PurchaseConstants.GIFT_KEYS.ADDRESS_BITMAP);
        byte [] encodeByte = Base64.decode(bitmapString, Base64.DEFAULT);
        bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "AddressLayout: oncreate");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(com.w3engineers.mesh.R.layout.address_layout);

        copyBtn = (Button) findViewById(com.w3engineers.mesh.R.id.copyBtn);
        copyBtn.setOnClickListener(this);


        ImageView qrImage = findViewById(com.w3engineers.mesh.R.id.qrImage);
        qrImage.setImageBitmap(bitmap);


        TextView walletAddressLabel = findViewById(com.w3engineers.mesh.R.id.walletAddressLabel);
        walletAddressLabel.setText(address);
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(address, address);
        clipboard.setPrimaryClip(clip);

        Toaster.showShort("address copied");
    }
}