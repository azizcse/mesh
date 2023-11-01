package com.w3engineers.meshrnd.ui.chat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.UserState;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.meshrnd.App;
import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityChatBinding;
import com.w3engineers.meshrnd.model.MessageModel;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.ui.Nearby.NearbyCallBack;
import com.w3engineers.meshrnd.ui.base.ItemClickListener;
import com.w3engineers.meshrnd.util.Constants;
import com.w3engineers.meshrnd.util.HandlerUtil;
import com.w3engineers.meshrnd.util.TimeUtil;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * * ============================================================================
 * * Copyright (C) 2019 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Sikder Faysal Ahmed on [15-Jan-2019 at 1:01 PM].
 * * Email: sikderfaysal@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: meshrnd.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [15-Jan-2019 at 1:01 PM].
 * * --> <Second Editor> on [15-Jan-2019 at 1:01 PM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [15-Jan-2019 at 1:01 PM].
 * * --> <Second Reviewer> on [15-Jan-2019 at 1:01 PM].
 * * ============================================================================
 **/
public class ChatActivity extends AppCompatActivity implements View.OnClickListener,
        MessageListener, NearbyCallBack, DbUpdate, UserState, ItemClickListener<MessageModel> {

    private ActivityChatBinding mBinding;
    private ChatAdapter mChatAdapter;
    private UserModel mUserModel;
    private MenuItem status, repeatedMsg;
    private Timer mTimer;
    private boolean isRepeatModeOn;

    private final int REQUEST_IMAGE_PICK = 202;
    private final int REQUEST_VIDEO_PICK = 302;
    public static int IMAGE_MESSAGE = 1;
    public static int VIDEO_MESSAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGuiWithUserdata();
        initAdapter();
        setUserInfo();
        ConnectionManager.on().initMessageListener(this);
        ConnectionManager.on().initNearByCallBackForChatActivity(this);
        ChatDataProvider.On().setUpdateListener(this::updateUI);
        fetchAllConversationWithThisUser();

        ConnectionManager.on().isUserAvailable(mUserModel.getUserId(), this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_active_status, menu);
        status = menu.findItem(R.id.menu_active_state);
        menu.findItem(R.id.menu_send_large_message).setEnabled(true);
        repeatedMsg = menu.findItem(R.id.menu_send_continuous_msg).setEnabled(true);
        status.setEnabled(true);
        List<UserModel> list = ConnectionManager.on().getUserList();
        Collections.sort(list);
        for (UserModel userModel : list) {
            if (mUserModel.getUserId().equalsIgnoreCase(userModel.getUserId())) {
                String connectionType = ConnectionManager.on().getConnectionType(mUserModel.getUserId());
                //  status.setTitle(getString(R.string.status_online));
                status.setTitle(connectionType);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                if (data != null) {
                    Uri uri = data.getData();
                    String path;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        path = getImageRealPath(getContentResolver(), uri);
                    } else {
                        path = uri.getPath();
                    }

                    buildAndSendFileMessage(path, IMAGE_MESSAGE);
                }
            } else if (requestCode == REQUEST_VIDEO_PICK) {
                if (data != null) {
                    Uri uri = data.getData();
                    String path;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        path = getVideoPath(getContentResolver(), uri);
                    } else {
                        path = uri.getPath();
                    }
                    buildAndSendFileMessage(path, VIDEO_MESSAGE);
                    Toast.makeText(this, "We will implement soon", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void buildAndSendFileMessage(String filePath, int fileType) {
        if (Text.isNotEmpty(mUserModel.getUserId())) {

            MessageModel messageModel = new MessageModel();
            messageModel.message = filePath;
            messageModel.incoming = false;
            messageModel.friendsId = mUserModel.getUserId();
            messageModel.messageType = fileType;

            String fileData = ConnectionManager.on().sendFile(mUserModel.getUserId(), filePath, fileType);

            try {
                JSONObject jsonObject = new JSONObject(fileData);
                boolean success =  jsonObject.getBoolean("success");
                String msg = jsonObject.getString("msg");
                if (success){
                    messageModel.messageId = msg;
                    MeshLog.e("FileMessageTest", "File message id: " + messageModel.messageId);

                    ChatDataProvider.On().insertMessage(messageModel, mUserModel);
                    mChatAdapter.addItem(messageModel);
                    scrollSmoothly();
                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private void initGuiWithUserdata() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_chat);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mUserModel = (UserModel) getIntent().getSerializableExtra(UserModel.class.getName());
        mBinding.imageButtonSend.setOnClickListener(this);
        mBinding.imageButtonCamera.setOnClickListener(this);
        mBinding.imageButtonVideo.setOnClickListener(this);
    }

    private void initAdapter() {
        mChatAdapter = new ChatAdapter(this);
        mChatAdapter.setItemClickListener(this);
        mBinding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerViewMessage.setAdapter(mChatAdapter);
    }

    private void fetchAllConversationWithThisUser() {
        //resendFailedMessage(mUserModel.getUserId());
        mChatAdapter.addItem(ChatDataProvider.On().getAllConversation(mUserModel.getUserId()));
        scrollSmoothly();
    }

/*    private void resendFailedMessage(String userId) {
        List<MessageModel> SendingFailedMessage = ChatDataProvider.On().getSendFailedConversation(userId);
        if (SendingFailedMessage.size() > 0) {
            for (MessageModel message : SendingFailedMessage) {
                ConnectionManager.on().sendMessage(mUserModel.getUserId(), message);
            }
        }
    }*/

    private void setUserInfo() {
        getSupportActionBar().setTitle(mUserModel.getUserName());
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.drawable_reg_page_shape));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConnectionManager.on().initMessageListener(null);
        ConnectionManager.on().initNearByCallBackForChatActivity(null);
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.image_button_camera:

            /*Intent intent = new Intent(ChatActivity.this, FilePickerActivity.class);
            intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                    .setCheckPermission(true)
                    .setSingleClickSelection(true)
                    .setSingleChoiceMode(true)
                    .setSkipZeroSizeFiles(true)
                    .build());
            startActivityForResult(intent, REQUEST_FILE_PICK);*/

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, REQUEST_IMAGE_PICK);

                break;
            case R.id.image_button_video:
                Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
                videoPickerIntent.setType("video/*");
                startActivityForResult(videoPickerIntent, REQUEST_VIDEO_PICK);
                break;
            case R.id.image_button_send:
                String inputValue = mBinding.edittextMessageInput.getText().toString().trim();
                if (TextUtils.isEmpty(inputValue)) return;
                mBinding.edittextMessageInput.setText("");
                MessageModel messageModel = new MessageModel();
                messageModel.message = inputValue + "\n" + TimeUtil.parseMillisToTime(System.currentTimeMillis());
                messageModel.incoming = false;
                messageModel.friendsId = mUserModel.getUserId();
                messageModel.messageId = UUID.randomUUID().toString();

                ChatDataProvider.On().insertMessage(messageModel, mUserModel);
                mChatAdapter.addItem(messageModel);
                scrollSmoothly();

                ConnectionManager.on().sendMessage(mUserModel.getUserId(), messageModel);

                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConnectionManager.on().initListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_delete_all_messages) {
            long removedItems = ChatDataProvider.On().removeAllConversation(mUserModel.getUserId());
            Toast.makeText(this, "Deleted " + removedItems + " entries", Toast.LENGTH_SHORT).show();
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.menu_send_large_message) {
            sendMessage(Constants.LARGE_MESSAGE);
        } else if (itemId == R.id.menu_send_continuous_msg) {
            if (isRepeatModeOn) {
                isRepeatModeOn = false;
                if (mTimer != null) {
                    mTimer.cancel();
                }
                repeatedMsg.setIcon(R.drawable.ic_action_playback_repeat);
            } else {
                isRepeatModeOn = true;
                Toast.makeText(this, "Repeated message started", Toast.LENGTH_SHORT).show();
                repeatedMsg.setIcon(R.drawable.ic_action_cancel);
                repeatMessage();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void repeatMessage() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendMessage(Constants.LARGE_MESSAGE);
            }
        }, 0, 1 * 1000);
    }

    private void sendMessage(String message) {
        MessageModel messageModel = new MessageModel();
        messageModel.message = message + "\n" + TimeUtil.parseMillisToTime(System.currentTimeMillis());
        messageModel.incoming = false;
        messageModel.friendsId = mUserModel.getUserId();
        messageModel.messageId = UUID.randomUUID().toString();

        ChatDataProvider.On().insertMessage(messageModel, mUserModel);
        runOnUiThread(() -> {
            mChatAdapter.addItem(messageModel);
            scrollSmoothly();
        });
        ConnectionManager.on().sendMessage(mUserModel.getUserId(), messageModel);
    }

    private void scrollSmoothly() {
        int index = mChatAdapter.getItemCount() - 1;
        if (index > 0) {
            mBinding.recyclerViewMessage.smoothScrollToPosition(index);
        }
    }

    @Override
    public void onMessageReceived(MessageModel message) {
        if (message.friendsId.equalsIgnoreCase(mUserModel.getUserId())) {
            HandlerUtil.postForeground(() -> {
                mChatAdapter.addItem(message);
                scrollSmoothly();
            });
        }
    }

    @Override
    public void onMessageDelivered() {
        updateUI();
    }

    @Override
    public void onFileProgressReceived(String fileMessageId, int progress) {
        runOnUiThread(() -> {
            //MeshLog.e("FileMessageTest", "File message id: " + fileMessageId);
            mChatAdapter.updateProgress(fileMessageId, progress);
        });
    }

    @Override
    public void onFileTransferEvent(String fileMessageId, boolean isSuccess) {
        runOnUiThread(() -> {
            if (isSuccess) {
                //ToastUtil.showShort(this, "File message received");
                mChatAdapter.updateProgress(fileMessageId, 100);
                mChatAdapter.notifyDataSetChanged();
            } else {
                mChatAdapter.setFileError(fileMessageId);
                mChatAdapter.notifyDataSetChanged();
                Toaster.showShort("Message sending failed");
            }
        });
    }

    @Override
    public void onUserFound(UserModel model) {
        if (model.getUserId().equalsIgnoreCase(mUserModel.getUserId())) {
            // runOnUiThread(() -> mBinding.edittextMessageInput.setEnabled(true));
            runOnUiThread(() -> {
                if (status != null) {
                    String connectionType = ConnectionManager.on().getConnectionType(model.getUserId());
                    status.setTitle(connectionType);
                }
            });

            // resendFailedMessage(model.getUserId());
        }
    }

    @Override
    public void onDisconnectUser(String userId) {
        if (userId == null || userId.isEmpty()) return;
        if (userId.equalsIgnoreCase(mUserModel.getUserId())) {
            runOnUiThread(() -> {
/*                mBinding.edittextMessageInput.setEnabled(false);
                mBinding.edittextMessageInput.setHint("Currently," + " "+ mUserModel.getUserName() + " " + "is not available ");
                mBinding.edittextMessageInput.setHintTextColor(getResources().getColor(R.color.colorAccent));*/

           /*     if (status != null) {
                    status.setTitle(getString(R.string.status_offline));
                }*/
            });
        }
    }


    @Override
    public void updateUI() {
        if (mChatAdapter != null) {
            runOnUiThread(() -> {
                mChatAdapter.clear();
                List<MessageModel> messageModelList = ChatDataProvider.On().getAllConversation(mUserModel.getUserId());
                mChatAdapter.addItem(messageModelList);
                mBinding.recyclerViewMessage.smoothScrollToPosition(messageModelList.size());
            });
        }
    }

    @Override
    public void onUserConnected(String userId, boolean isConnected) {
        if (!isConnected) {
            runOnUiThread(() -> {
                mBinding.edittextMessageInput.setEnabled(false);
                Toast.makeText(ChatActivity.this, "User not connected", Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onItemClick(View view, MessageModel item) {
        if (view.getId() == R.id.image_view_retry) {
            HandlerUtil.postBackground(() -> {

                boolean status = ConnectionManager.on().resumeFile(item.messageId, App.getContext().getPackageName());
                Log.d("FileMessageTest", "File resume request: " + status);
                runOnUiThread(() -> {
                    if (status) {
                        item.messageStatus = item.incoming ? Constant.MessageStatus.RECEIVING :
                                Constant.MessageStatus.SENDING;
                        Toaster.showShort("Message sending again");
                        mChatAdapter.notifyDataSetChanged();
                    } else {
                        Toaster.showShort("Message sending failed");
                        item.messageStatus = Constant.MessageStatus.FAILED;
                        mChatAdapter.notifyDataSetChanged();
                    }

                });
            });
        }
    }

    private String getImageRealPath(ContentResolver contentResolver, Uri uri) {
        String ret = "";

        // Query the uri with condition.
        Cursor cursor = contentResolver.query(uri, null, null, null, null);

        if (cursor != null) {
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {
                // Get columns name by uri type.
                String columnName = MediaStore.Images.Media.DATA;
                Log.d("UriTest: ", "uri: " + uri);
                if (uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Images.Media.DATA;
                }


                // Get column index.
                int imageColumnIndex = cursor.getColumnIndex(columnName);

                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex);
            }
        }

        return ret;
    }

    private String getVideoPath(ContentResolver contentResolver, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else return null;
    }
}
