package com.w3engineers.mesh.ui.importwallet;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;

import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.ImageUtil;
import com.w3engineers.mesh.util.Utils;

/*
 * ============================================================================
 * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * ============================================================================
 */

public class ImportWalletViewModel extends AndroidViewModel {

    public ImportWalletViewModel(@NonNull Application application) {
        super(application);
    }

    boolean storeData(@Nullable String address, String password, String publicKey) {

        SharedPref.write(Utils.KEY_NODE_ADDRESS, address);
        SharedPref.write(Utils.KEY_WALLET_PASS, password);
        SharedPref.write(Utils.KEY_WALLET_PUB_KEY, publicKey);

        ImageUtil.generateQRCodeForWalletAddress(address);

        return true;
    }
}
