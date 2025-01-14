package com.w3engineers.purchase.ui.dataplan;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.ViewDataBinding;

import com.w3engineers.ext.strom.application.ui.base.BaseAdapter;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ItemSellerViewBinding;
import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.manager.PurchaseConstants;
import com.w3engineers.purchase.model.Seller;


class TestSellerListAdapter extends BaseAdapter<Seller> {

    private final int SELLERS = 4;
    private View.OnClickListener clickListener;

    public TestSellerListAdapter(View.OnClickListener onClickListener) {
        this.clickListener = onClickListener;
    }

/*    @Override
    public int getItemViewType(int position) {

        Seller seller = getItem(position);

        switch (seller.getId()) {
            case "1":
                return DataPlanConstants.SELLER_LABEL.ONLINE_NOT_PURCHASED;

            case "2":
                return DataPlanConstants.SELLER_LABEL.ONLINE_PURCHASED;

            case "3":
                return DataPlanConstants.SELLER_LABEL.OFFLINE_PURCHASED;

            default:
                return SELLERS;
        }
    }*/

    @Override
    public boolean isEqual(Seller left, Seller right) {
        return !TextUtils.isEmpty(left.getId())
                && !TextUtils.isEmpty(right.getId())
                && left.getId().equals(right.getId());
    }

    @Override
    public BaseAdapterViewHolder<Seller> newViewHolder(ViewGroup parent, int viewType) {

//        switch (viewType) {
            /*case DataPlanConstants.SELLER_LABEL.ONLINE_NOT_PURCHASED:
                return new TagViewModel(inflate(parent, R.layout.item_label_new_seller));
            case DataPlanConstants.SELLER_LABEL.ONLINE_PURCHASED:
                return new TagViewModel(inflate(parent, R.layout.item_label_online_seller));
            case DataPlanConstants.SELLER_LABEL.OFFLINE_PURCHASED:
                return new TagViewModel(inflate(parent, R.layout.item_label_offline_seller));
            case SELLERS:*/
                return new SellerViewModel(inflate(parent, R.layout.item_seller_view));
//        }

//        return null;
    }

    private class TagViewModel extends BaseAdapterViewHolder<Seller> {

        TagViewModel(@NonNull ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(@NonNull Seller item) {

        }
    }

    private String convertTwoDigitString(double value) {
        String result = String.format("%.2f", value);
        return result;
    }

    private class SellerViewModel extends BaseAdapterViewHolder<Seller> {
        ItemSellerViewBinding itemDataSellerBinding;

        SellerViewModel(@NonNull ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
            this.itemDataSellerBinding = (ItemSellerViewBinding) viewDataBinding;
        }

        @Override
        public void bind(@NonNull Seller seller) {

            Context context = itemDataSellerBinding.userName.getContext();
            itemDataSellerBinding.userName.setText(seller.getName());

            String usedDataInfo =  String.format(context.getResources().getString(com.w3engineers.mesh.R.string.used_s), " " + convertTwoDigitString(seller.getUsedData()));

            String totalDataInfo = String.format(context.getResources().getString(R.string.total_purchase) , " " + convertTwoDigitString(seller.getPurchasedData()));

            itemDataSellerBinding.sellerMeshStatus.setBackgroundResource(activeStatusResource(seller.getLabel(), seller.getId()));

            itemDataSellerBinding.userUseAmount.setText(usedDataInfo);
            itemDataSellerBinding.userTotalAmount.setText(totalDataInfo);

            itemDataSellerBinding.status.setText(seller.getBtnText());
            itemDataSellerBinding.status.setEnabled(seller.isBtnEnabled());

            itemDataSellerBinding.status.setBackground(seller.isBtnEnabled() ? (seller.getBtnText().equalsIgnoreCase(PurchaseConstants.SELLERS_BTN_TEXT.CLOSE) ? ContextCompat.getDrawable(context, R.drawable.gradient_color_close_button) : ContextCompat.getDrawable(context, R.drawable.gradient_color_primary_and_dark)) : ContextCompat.getDrawable(context, R.drawable.rectangular_gray_small));
            itemDataSellerBinding.status.setTextColor(seller.isBtnEnabled() ? Color.WHITE : context.getResources().getColor(com.w3engineers.mesh.R.color.grey_text) );

            int padding =  itemDataSellerBinding.status.getPaddingTop();
            itemDataSellerBinding.status.setPadding(padding, padding, padding, padding);

            itemDataSellerBinding.status.setTag(seller);
            itemDataSellerBinding.status.setOnClickListener(clickListener);
        }
    }

    private int activeStatusResource(int label, String nodeID) {
        if (label == DataPlanConstants.SELLER_LABEL.OFFLINE_PURCHASED) {
            return R.mipmap.ic_offline;
        } else {
            int userType = TransportManagerX.getInstance().getLinkTypeById(nodeID);
            if (userType == 8 || userType == 9) {
                return R.mipmap.ic_hb_online;
            } else {
                return R.mipmap.ic_mesh_online;
            }
        }
    }
}
