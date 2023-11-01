package com.w3engineers.purchase.util;

import com.w3engineers.eth.data.constant.PayLibConstant;
import com.w3engineers.eth.data.helper.PreferencesHelperPaylib;
import com.w3engineers.mesh.MeshApp;

public class PurchaseManagerUtil {
    public static String getCurrencyTypeMessage(String message) {
        String currencyMode = getEndPointCurrency();
        return String.format(message, currencyMode);
    }

    public static String getEndPointCurrency() {
        int endPoint = PreferencesHelperPaylib.onInstance(MeshApp.getContext()).getEndpointMode();
        if (endPoint == PayLibConstant.END_POINT_TYPE.ETH_ROPSTEN) {
            return MeshApp.getContext().getString(com.w3engineers.mesh.R.string.eth);
        } else if (endPoint == PayLibConstant.END_POINT_TYPE.ETC_KOTTI) {
            return MeshApp.getContext().getString(com.w3engineers.mesh.R.string.etc);
        } else if (endPoint == PayLibConstant.END_POINT_TYPE.TETH_PRIVATE) {
            return MeshApp.getContext().getString(com.w3engineers.mesh.R.string.teth);
        } else {
            return MeshApp.getContext().getString(com.w3engineers.mesh.R.string.teth);
        }
        //return endPoint == PayLibConstant.END_POINT_TYPE.ETH_ROPSTEN ? MeshApp.getContext().getString(com.w3engineers.mesh.R.string.eth) : MeshApp.getContext().getString(com.w3engineers.mesh.R.string.etc);
    }
}
