package com.w3engineers.purchase.ui.tokenguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ItemLinkBinding;
import com.w3engineers.purchase.model.PointLink;
import com.w3engineers.purchase.ui.base.BaseAdapter;
import com.w3engineers.purchase.ui.base.BaseViewHolder;

public class PointGuidelineAdapter extends BaseAdapter<PointLink> {

    @Override
    public boolean isEqual(PointLink left, PointLink right) {
        return false;
    }

    @Override
    public BaseViewHolder newViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemLinkBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_link, parent, false);
        return new TokenGuidelineVH(binding);
    }

    class TokenGuidelineVH extends BaseViewHolder<PointLink> {
        ItemLinkBinding binding;

        TokenGuidelineVH(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(PointLink item, ViewDataBinding viewDataBinding) {
            binding = (ItemLinkBinding) viewDataBinding;

            binding.textViewLink.setText(item.getHeader());

            setClickListener(binding.getRoot());
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getItem(getAdapterPosition()));
        }
    }
}
