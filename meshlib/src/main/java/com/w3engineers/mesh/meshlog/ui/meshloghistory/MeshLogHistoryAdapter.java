package com.w3engineers.mesh.meshlog.ui.meshloghistory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.w3engineers.mesh.R;
import com.w3engineers.mesh.databinding.ItemMeshHistoryBinding;
import com.w3engineers.mesh.meshlog.data.model.MeshLogHistoryModel;
import com.w3engineers.mesh.meshlog.ui.base.BaseAdapter;
import com.w3engineers.mesh.meshlog.ui.base.BaseViewHolder;


/**
 * Mesh Log history Adapter
 */


public class MeshLogHistoryAdapter extends BaseAdapter<MeshLogHistoryModel> {
    @Override
    public boolean isEqual(MeshLogHistoryModel left, MeshLogHistoryModel right) {
        return false;
    }

    @Override
    public BaseViewHolder newViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMeshHistoryBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_mesh_history, parent, false);
        return new MeshLogHistoryVH(binding);
    }

    class MeshLogHistoryVH extends BaseViewHolder<MeshLogHistoryModel> {
        private ItemMeshHistoryBinding binding;

        public MeshLogHistoryVH(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
            binding = (ItemMeshHistoryBinding) viewDataBinding;
        }

        @Override
        public void bind(MeshLogHistoryModel item, ViewDataBinding viewDataBinding) {
            binding.textViewName.setText(item.getName());

            setClickListener(binding.getRoot());
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getItem(getAdapterPosition()));
        }
    }
}
