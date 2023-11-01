package com.w3engineers.meshrnd.ui.history;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.FragmentDatabaseBinding;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.ui.base.BaseFragment;
import com.w3engineers.meshrnd.ui.base.ItemClickListener;
import com.w3engineers.meshrnd.ui.chat.ChatActivity;
import com.w3engineers.meshrnd.ui.chat.ChatDataProvider;
import com.w3engineers.meshrnd.util.AppLog;

import java.util.List;

public class HistoryFragment extends BaseFragment implements ItemClickListener<UserModel> {
    private FragmentDatabaseBinding binding;
    private ChattedUserListAdapter chattedUserListAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_database, container, false);
        chattedUserListAdapter = new ChattedUserListAdapter(getActivity());
        chattedUserListAdapter.setItemClickListener(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recyclerView.setAdapter(chattedUserListAdapter);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        List<UserModel> list = ChatDataProvider.On().getAllUser();
        chattedUserListAdapter.clear();
        chattedUserListAdapter.addItem(list);
        binding.deviceDetails.setText(AppLog.deviceHistory());

        ConnectionManager.on().initListener(this);

    }

    @Override
    public void onItemClick(View view, UserModel item) {
        if (item != null) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(UserModel.class.getName(), item);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            Toast.makeText(getActivity(), "User model is null", Toast.LENGTH_SHORT).show();
        }
    }
}
