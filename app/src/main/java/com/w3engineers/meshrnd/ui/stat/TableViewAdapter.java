package com.w3engineers.meshrnd.ui.stat;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.w3engineers.mesh.db.routing.RoutingEntity;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.TableListItemBinding;

import java.util.ArrayList;
import java.util.List;

public class TableViewAdapter extends RecyclerView.Adapter<TableViewAdapter.RowViewHolder> {

    private List<RoutingEntity> routingEntityList;
    private Context context;

    public TableViewAdapter(Context ctx) {
        this.routingEntityList = new ArrayList<>();
        context = ctx;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TableListItemBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.table_list_item, parent, false);
        return new RowViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        //int rowPos = holder.getAdapterPosition();
        if (position == 0) {
            holder.bindHeader();
        } else {
            RoutingEntity routingEntity = routingEntityList.get(position - 1);
            holder.bindData(routingEntity);
        }
    }

    @Override
    public int getItemCount() {
        return routingEntityList.size() + 1; // one more to add header row
    }

/*    public void clear() {
        routingEntityList.clear();
        notifyDataSetChanged();
    }*/

    public void addItems(List<RoutingEntity> items) {
        routingEntityList.clear();
        routingEntityList.addAll(items);
        notifyDataSetChanged();
    }


    private void setHeaderBg(View view) {
        view.setBackgroundResource(R.drawable.table_header_cell_bg);
    }

    private void setContentBg(View view) {
        view.setBackgroundResource(R.drawable.table_content_cell_bg);
    }

    public class RowViewHolder extends RecyclerView.ViewHolder {
        public TableListItemBinding tableListItemBinding;

        public RowViewHolder(TableListItemBinding itemRowBinding) {
            super(itemRowBinding.getRoot());
            this.tableListItemBinding = itemRowBinding;
        }

        public void bindHeader() {
            setHeaderBg(tableListItemBinding.txtId);
            setHeaderBg(tableListItemBinding.txtPeer);
            setHeaderBg(tableListItemBinding.txtConnType);
            setHeaderBg(tableListItemBinding.txtHop);
            setHeaderBg(tableListItemBinding.txtHopCount);

            tableListItemBinding.txtId.setText("Id");
            tableListItemBinding.txtId.setTextColor(Color.WHITE);
            tableListItemBinding.txtPeer.setText("Peer");
            tableListItemBinding.txtPeer.setTextColor(Color.WHITE);
            tableListItemBinding.txtConnType.setText("Type");
            tableListItemBinding.txtConnType.setTextColor(Color.WHITE);
            tableListItemBinding.txtHop.setText("Hop");
            tableListItemBinding.txtHop.setTextColor(Color.WHITE);
            tableListItemBinding.txtHopCount.setText("H Count");
            tableListItemBinding.txtHopCount.setTextColor(Color.WHITE);
        }

        public void bindData(RoutingEntity routingEntity) {
            setContentBg(tableListItemBinding.txtId);
            setContentBg(tableListItemBinding.txtPeer);
            setContentBg(tableListItemBinding.txtConnType);
            setContentBg(tableListItemBinding.txtHop);
            setContentBg(tableListItemBinding.txtHopCount);

            tableListItemBinding.txtId.setText("" + routingEntity.getId());
            tableListItemBinding.txtPeer.setText(AddressUtil.makeShortAddress(routingEntity.getAddress()));
            tableListItemBinding.txtConnType.setText(ConnectionUtility.getConnectionType(routingEntity.getType()));
            tableListItemBinding.txtHop.setText(AddressUtil.makeShortAddress(routingEntity.getHopAddress()));
            tableListItemBinding.txtHopCount.setText("" + routingEntity.getHopCount());
        }
    }
}
