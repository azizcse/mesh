package com.w3engineers.meshrnd.ui.history;

import android.content.Context;
import androidx.databinding.ViewDataBinding;
import android.view.View;
import android.view.ViewGroup;

import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ItemChattedUserBinding;
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
public class ChattedUserListAdapter extends BaseAdapter<UserModel> {


    private Context mContext;


    private ConnectionManager connectionManager;

    public ChattedUserListAdapter(Context context) {
        this.mContext = context;
        connectionManager = ConnectionManager.on();
    }

    @Override
    public boolean isEqual(UserModel left, UserModel right) {
        if (left.getUserId() == null || right.getUserId() == null) {
            return false;
        }
        if (left.getUserId().equals(right.getUserId())) {
            return true;
        }

        return false;
    }

    @Override
    public BaseViewHolder newViewHolder(ViewGroup parent, int viewType) {
        return new UserViewHolder(inflate(parent, R.layout.item_chatted_user));
    }

    public void removeItem(String userId) {
        List<UserModel> userModelList = getItems();

        for (UserModel item : userModelList) {
            if (item.getUserId().equals(userId)) {
                removeItem(item);
                return;
            }
        }
    }

    public class UserViewHolder extends BaseViewHolder<UserModel> {

        public UserViewHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(UserModel item, ViewDataBinding viewDataBinding) {
            ItemChattedUserBinding binding = (ItemChattedUserBinding) viewDataBinding;
            binding.userCard.setOnClickListener(this);
            binding.userName.setText(item.getUserName());
            binding.textViewTime.setText(item.getUserId());

            binding.userNameSmall.setText(connectionManager.getConnectionType(item.getUserId()));
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getItem(getAdapterPosition()));
        }
    }
}
