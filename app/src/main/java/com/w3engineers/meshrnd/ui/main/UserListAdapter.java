package com.w3engineers.meshrnd.ui.main;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.ViewDataBinding;

import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ItemDiscoveredUserBinding;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.ui.base.BaseAdapter;
import com.w3engineers.meshrnd.ui.base.BaseViewHolder;

import java.util.List;

/**
 * * ============================================================================
 * * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Sikder Faysal Ahmed on [15-Jan-2019 at 5:14 PM].
 * * Email: sikderfaysal@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: meshrnd.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [15-Jan-2019 at 5:14 PM].
 * * --> <Second Editor> on [15-Jan-2019 at 5:14 PM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [15-Jan-2019 at 5:14 PM].
 * * --> <Second Reviewer> on [15-Jan-2019 at 5:14 PM].
 * * ============================================================================
 **/
public class UserListAdapter extends BaseAdapter<UserModel> {


    private final Context mContext;

    private final ConnectionManager connectionManager;

    public UserListAdapter(Context context) {
        this.mContext = context;
        connectionManager = ConnectionManager.on();
    }

    @Override
    public boolean isEqual(UserModel left, UserModel right) {
        if ((left == null && right == null) || (left != null && left.getUserId() == null &&
                right.getUserId() == null)) {
            return false;
        }
        return left != null && right != null && left.getUserId().equals(right.getUserId());
    }

    @Override
    public BaseViewHolder newViewHolder(ViewGroup parent, int viewType) {
        return new UserViewHolder(inflate(parent, R.layout.item_discovered_user));
    }

    public void removeItem(String userId) {
        List<UserModel> userModelList = getItems();

        for (UserModel item : userModelList) {
            if (item == null || TextUtils.isEmpty(item.getUserId()))
                continue;

            if (item.getUserId().equals(userId)) {
                removeItem(item);
                MeshLog.i("User removed from UI ::" + AddressUtil.makeShortAddress(userId));
                return;
            } else {

            }
        }
    }

    public class UserViewHolder extends BaseViewHolder<UserModel> {

        public UserViewHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(UserModel item, ViewDataBinding viewDataBinding) {
            if (item != null) {
                ItemDiscoveredUserBinding binding = (ItemDiscoveredUserBinding) viewDataBinding;
                if (!TextUtils.isEmpty(item.getUserName())) {
                    binding.userName.setText(item.getUserName());
                } else {
                    binding.userName.setText("Anonymous");
                }
                int color = item.mIsLocallyAlive ?
                        mContext.getResources().getColor(R.color.colorPrimary) :
                        mContext.getResources().getColor(R.color.colorDarkGray);
                binding.buttonSendFile.setTextColor(color);
                binding.buttonSingleMsg.setTextColor(color);
                binding.buttonLrgMsg.setTextColor(color);
                binding.textViewTime.setText(item.getUserId());
                String connectionType = connectionManager.getMultipleConnectionType(item.getUserId());

                binding.userLinkType.setText(connectionType);

                if (item.isSent()) {
                    binding.userCard.setBackgroundColor(mContext.getResources().getColor(R.color.colorBackgroundDark));
                } else {
                    binding.userCard.setBackgroundColor(mContext.getResources().getColor(R.color.colorWhite));
                }
                binding.userCard.setOnClickListener(this);
                setClickListener(binding.buttonSingleMsg, binding.userLinkType,
                        binding.buttonSendFile, binding.buttonLrgMsg);
            }
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getItem(getAdapterPosition()));
        }
    }

    public int getOnlineItemCount() {
        int count = 0;
        for (UserModel userModel : getItems()) {
            if (userModel != null && userModel.mIsLocallyAlive) {
                count++;
            }
        }
        return count;
    }
}
