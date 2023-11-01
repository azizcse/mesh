package com.w3engineers.meshrnd.ui.Nearby;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.FragmentNearbyBinding;
import com.w3engineers.meshrnd.model.MessageModel;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.test.filesharing.FileReadWriteTest;
import com.w3engineers.meshrnd.ui.base.BaseFragment;
import com.w3engineers.meshrnd.ui.base.ItemClickListener;
import com.w3engineers.meshrnd.ui.chat.ChatActivity;
import com.w3engineers.meshrnd.ui.chat.ChatDataProvider;
import com.w3engineers.meshrnd.ui.main.UserListAdapter;
import com.w3engineers.meshrnd.ui.nav.BottomMenuHelper;
import com.w3engineers.meshrnd.util.Constants;
import com.w3engineers.meshrnd.util.HandlerUtil;
import com.w3engineers.meshrnd.util.TimeUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import timber.log.Timber;

public class NearbyFragment extends BaseFragment implements ItemClickListener<UserModel>, NearbyCallBack {

    private final int REQUEST_FILE_PICK = 202;

    private FragmentNearbyBinding binding;
    private UserListAdapter nearbyAdapter;

    private String mReceiverId;
    private UserModel mUserModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_nearby, container, false);
        nearbyAdapter = new UserListAdapter(getActivity());
        nearbyAdapter.setItemClickListener(this);

        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recyclerView.setNestedScrollingEnabled(false);
        binding.recyclerView.setAdapter(nearbyAdapter);

        binding.myIdTv.setText("My ID :" + SharedPref.read(Constant.KEY_USER_ID));


        return binding.getRoot();
    }


    @Override
    public void onResume() {
        super.onResume();
        List<UserModel> list = ChatDataProvider.On().getAllUser();
        Collections.sort(list);
        nearbyAdapter.clear();
        nearbyAdapter.addItem(list);
        if (nearbyAdapter.getItemCount() > 0) {
            binding.progressBar.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        if (list.size() > 0) {
            int userCount = nearbyAdapter.getOnlineItemCount();
            BottomMenuHelper.showBadge(getActivity(), Objects.requireNonNull(getActivity()).
                    findViewById(R.id.navigation), R.id.navigation_nearby, "" + userCount);
        } else {
            BottomMenuHelper.removeBadge(Objects.requireNonNull(getActivity()).findViewById(R.id.navigation), R.id.navigation_nearby);
        }

        ConnectionManager.on().initListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // ConnectionManager.on().initListener(null);
    }

    @Override
    public void onItemClick(View view, UserModel item) {
        if (item != null) {
            switch (view.getId()) {
                case R.id.user_card:
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(UserModel.class.getName(), item);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    break;
                case R.id.user_link_type:

                    //ConnectionManager.on().initHighBandWith(item.getUserId());

                    break;
                case R.id.button_single_msg:
                    sendHelloMessage(item, "Hello Bro\n" + TimeUtil.parseMillisToTime
                            (System.currentTimeMillis()));
                    break;

                case R.id.button_lrg_msg:
                    sendHelloMessage(item, Constants.LARGE_MESSAGE);

                    break;
                case R.id.button_send_file:
                    //show file UI and send
                    Timber.d("Sending file...");

                    mReceiverId = item.getUserId();
                    mUserModel = item;

                    intent = new Intent(getActivity(), FilePickerActivity.class);
                    intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                            .setShowFiles(true)
                            .setCheckPermission(true)
                            .setSingleClickSelection(true)
                            .setSkipZeroSizeFiles(true)
                            .setShowVideos(true)
                            .setShowImages(true)
                            .build());
                    startActivityForResult(intent, REQUEST_FILE_PICK);

                    break;
            }
        } else {
            Toast.makeText(getActivity(), "User model is null", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUserFound(UserModel model) {
        HandlerUtil.postForeground(() -> {
            binding.progressBar.setVisibility(View.GONE);
            nearbyAdapter.addItem(model);

            Activity activity = getActivity();
            if (activity != null) {
                if (nearbyAdapter.getItemCount() > 0) {
                    BottomMenuHelper.showBadge(activity, activity.findViewById(R.id.navigation),
                            R.id.navigation_nearby, String.valueOf(nearbyAdapter.getOnlineItemCount()));
                } else {
                    BottomMenuHelper.removeBadge(activity.findViewById(R.id.navigation), R.id.navigation_nearby);
                }
            }
        });
    }

    @Override
    public void onDisconnectUser(String userId) {
        UserModel userModel = ChatDataProvider.On().getUserInfoById(userId);
        HandlerUtil.postForeground(() -> {
            nearbyAdapter.addItem(userModel);
            if (getActivity() == null) return;
            if (nearbyAdapter.getItemCount() > 0) {
                BottomMenuHelper.showBadge(getActivity(), Objects.requireNonNull(getActivity()).findViewById(R.id.navigation),
                        R.id.navigation_nearby, String.valueOf(nearbyAdapter.getOnlineItemCount()));
            } else {
                if (getActivity().findViewById(R.id.navigation) != null) {
                    BottomMenuHelper.removeBadge(Objects.requireNonNull(getActivity()).findViewById(R.id.navigation), R.id.navigation_nearby);
                }
            }
        });
    }

    public void updateSentMessageScreen(UserModel userModel) {
        HandlerUtil.postForeground(() -> nearbyAdapter.addItem(userModel));
    }

    public void resetScreen() {
        HandlerUtil.postForeground(() -> {
            for (UserModel model : nearbyAdapter.getItems()) {
                model.setSent(false);
            }
            nearbyAdapter.notifyDataSetChanged();

        });
    }

    public void sendHelloMessage(UserModel model, String message) {
        MessageModel messageModel = new MessageModel();
        messageModel.message = message;
        messageModel.incoming = false;
        messageModel.friendsId = model.getUserId();
        messageModel.messageId = UUID.randomUUID().toString();
        ConnectionManager.on().sendMessage(model.getUserId(), messageModel);
        ChatDataProvider.On().insertMessage(messageModel, model);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_FILE_PICK) {
                if (data != null) {
                    ArrayList<MediaFile> files = data.getParcelableArrayListExtra(
                            FilePickerActivity.MEDIA_FILES);

                    if (CollectionUtil.hasItem(files)) {

                        if (Text.isNotEmpty(mReceiverId)) {
                            for (MediaFile mediaFile : files) {

                                MessageModel messageModel = new MessageModel();
                                messageModel.message = mediaFile.getPath();
                                messageModel.incoming = false;
                                messageModel.friendsId = mReceiverId;
                                messageModel.messageType = mediaFile.getMediaType() ==
                                        MediaFile.TYPE_IMAGE ? ChatActivity.IMAGE_MESSAGE :
                                        mediaFile.getMediaType() == MediaFile.TYPE_VIDEO ?
                                                ChatActivity.VIDEO_MESSAGE : -1;

                                String fileData = ConnectionManager.on().sendFile(mReceiverId, mediaFile.getPath(), 0);

                                try {
                                    JSONObject jsonObject = new JSONObject(fileData);
                                    boolean success =  jsonObject.getBoolean("success");
                                    String msg = jsonObject.getString("msg");
                                    if (success){
                                        messageModel.messageId = msg;
                                        MeshLog.e("FileMessageTest", "File message id: " + messageModel.messageId);
                                        ChatDataProvider.On().insertMessage(messageModel, mUserModel);
                                    } else {
                                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception ex){
                                    ex.printStackTrace();
                                }
                            }
                            //to test file read write
//                            testFileRW(mediaFile.getPath());
                        }
                    }
                }
            }
        }
    }

    @VisibleForTesting
    private void testFileRW(String filePath) {
        new FileReadWriteTest().testFileRW(getActivity(), filePath);
    }
}
