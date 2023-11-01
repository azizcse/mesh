package com.w3engineers.meshrnd.ui.nav;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.letbyte.core.meshfilesharing.data.FilePacket;
import com.letbyte.core.meshfilesharing.data.db.DatabaseService;
import com.letbyte.core.meshfilesharing.helper.Const;
import com.letbyte.core.meshfilesharing.helper.FileHelper;
import com.w3engineers.ext.strom.util.collections.CollectionUtil;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.datasharing.helper.PreferencesHelper;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.meshlog.ui.meshloghistory.MeshLogHistoryActivity;
import com.w3engineers.mesh.premission.MeshSystemRequestActivity;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.AesSaltEncryption;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.mesh.wifi.dispatch.LinkStateListener;
import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityCreateGroupBinding;
import com.w3engineers.meshrnd.model.MessageModel;
import com.w3engineers.meshrnd.model.UserModel;
import com.w3engineers.meshrnd.service.AppService;
import com.w3engineers.meshrnd.ui.Nearby.NearbyFragment;
import com.w3engineers.meshrnd.ui.base.BaseFragment;
import com.w3engineers.meshrnd.ui.chat.ChatDataProvider;
import com.w3engineers.meshrnd.ui.meshlog.MeshLogFragment;
import com.w3engineers.meshrnd.ui.network.NetworkFragment;
import com.w3engineers.meshrnd.ui.stat.ConnectionStatFragment;
import com.w3engineers.meshrnd.ui.stat.ConnectivityStatActivity;
import com.w3engineers.meshrnd.util.Constants;
import com.w3engineers.meshrnd.util.HandlerUtil;
import com.w3engineers.meshrnd.util.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BottomNavActivity extends AppCompatActivity implements MeshStateListener {

    private static final String TAG = BottomNavActivity.class.getSimpleName();
    private final int SYSTEM_PRMISSION_REQUEST_CODE = 101;

    private BaseFragment mCurrentFragment;
    private BaseFragment baseFragment;
    private ActivityCreateGroupBinding mBinding;
    private MenuItem myDataPlanMenuItem;
    TextView connectedUser;
    BottomNavigationView navigation;

    private ConnectionManager connectionManager;

    /*
     * All message sending process
     * */
    private MenuItem msgSendingStatusMenuItem;
    private boolean isAllMessageProcessClicked;
    private int msgSendCount = 0;
    private int userCount;
    private HashMap<String, UserModel> messageMap;
    private final int REQUEST_IMAGE_PICK = 202;


    public interface StateListener {
        void onInit(boolean isSuccess);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ChatDataProvider.On().updateNodesOffline();
        //MeshLog.clearLog();
        //  Constant.CURRENT_LOG_FILE_NAME = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()) + ".txt";
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_group);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        connectionManager = ConnectionManager.on();
        connectionManager.setMeshStateListener(this);
        connectionManager.init();

        CheckPermissionAndStartLibrary();

        ChatDataProvider.On().updateIncompleteMessageAsFailed();

    }


    private void CheckPermissionAndStartLibrary() {

        commitFragment(R.id.fragment_container, new NearbyFragment());
        navigation.setSelectedItemId(R.id.navigation_nearby);

        getSupportActionBar().setTitle("Me" + " : " + SharedPref.read(Constant.KEY_USER_NAME) + " ( " + AddressUtil.makeShortAddress(SharedPref.read(Constant.KEY_USER_ID)) + " ) ");

        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.drawable_reg_page_shape));
        connectionManager = ConnectionManager.on();
        connectionManager.initListener(this);
        startService();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0, len = permissions.length; i < len; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                // user rejected the permission
                boolean showRationale = shouldShowRequestPermissionRationale(permission);
                if (!showRationale) {
                    //  Toast.makeText(this,"Permission denaid", Toast.LENGTH_LONG).show();
                    Toast.makeText(BottomNavActivity.this, "Please allow permission from setting", Toast.LENGTH_LONG).show();
                } else {
                    CheckPermissionAndStartLibrary();
                }
            } else {
                CheckPermissionAndStartLibrary();
            }
        }

    }


    private void startService() {
        Intent intent = new Intent(this, AppService.class);
        startService(intent);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
        baseFragment = null;
        switch (item.getItemId()) {
            case R.id.navigation_network:
                baseFragment = (NetworkFragment) getSupportFragmentManager()
                        .findFragmentByTag(NetworkFragment.class.getName());
                if (baseFragment == null) {
                    baseFragment = new NetworkFragment();
                }
                break;
            case R.id.navigation_nearby:
                baseFragment = (NearbyFragment) getSupportFragmentManager()
                        .findFragmentByTag(NearbyFragment.class.getName());
                if (baseFragment == null) {
                    baseFragment = new NearbyFragment();
                }
                break;
            case R.id.navigation_stat:
                baseFragment = (ConnectionStatFragment) getSupportFragmentManager()
                        .findFragmentByTag(ConnectionStatFragment.class.getName());
                if (baseFragment == null) {
                    baseFragment = new ConnectionStatFragment();
                }
                break;

            case R.id.navigation_message:
                baseFragment = (MeshLogFragment) getSupportFragmentManager()
                        .findFragmentByTag(MeshLogFragment.class.getName());
                if (baseFragment == null) {
                    baseFragment = new MeshLogFragment();
                }
                break;

        }
        commitFragment(R.id.fragment_container, baseFragment);
        return true;
    };

    /**
     * Commit child fragment of BaseFragment on a frameLayout
     *
     * @param viewId       int value
     * @param baseFragment BaseFragment object
     * @return void
     */
    protected void commitFragment(int viewId, BaseFragment baseFragment) {
        if (baseFragment == null) return;

        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(viewId, baseFragment, baseFragment.getClass().getName())
                    .addToBackStack(baseFragment.getClass().getName())
                    .commit();
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
        }

        mCurrentFragment = baseFragment;
    }

    private void sendAllHelloMessage() {
        List<UserModel> list = ConnectionManager.on().getUserList();
        userCount = list.size();
        MessageModel messageModel = new MessageModel();
        messageModel.message = "Hello Bro\n" + TimeUtil.parseMillisToTime(System.currentTimeMillis());
        messageModel.incoming = false;

        if (!list.isEmpty()) {
            runOnUiThread(() -> msgSendingStatusMenuItem.setTitle("0"));
            messageMap = new HashMap<>();
            if (mCurrentFragment instanceof NearbyFragment) {
                ((NearbyFragment) mCurrentFragment).resetScreen();
            }
        }

        HandlerUtil.postForeground(() -> {
            msgSendCount = 0;

            for (UserModel model : list) {
                messageModel.friendsId = model.getUserId();
                String msgId = UUID.randomUUID().toString();
                messageModel.messageId = msgId;

                messageMap.put(msgId, model);

                ConnectionManager.on().sendMessage(model.getUserId(), messageModel);

                ChatDataProvider.On().insertMessage(messageModel, model);
            }
        }, 1000); // we added delay because to show reset menu text
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bottom_nav, menu);

        msgSendingStatusMenuItem = menu.findItem(R.id.menu_msg_sending_status);
        //myDataPlanMenuItem = menu.getItem(1);

        // msgSendingStatusMenuItem.setVisible(false);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       /* if (item.getItemId() == R.id.menu_data_plan_setting) {
            startActivity(new Intent(this, DataPlanActivity.class));
        } else*/
        if (item.getItemId() == R.id.menu_resume_file_test) {

            //Send resume request to last failed file iff targetted node available
            new Thread(() -> {

                List<FilePacket> filePackets = DatabaseService.getInstance(getApplicationContext())
                        .getFilePackets(Const.FileStatus.FAILED);
                if (CollectionUtil.hasItem(filePackets)) {
                    FilePacket filePacket = filePackets.get(0);
                    ConnectionManager.on().resumeFile(
                            filePacket.mSourceAddress + FileHelper.FILE_ID_SEPARATOR +
                                    filePacket.mFileId, this.getPackageName());
                }
            }).start();

        } else if (item.getItemId() == R.id.menu_network_reform_test) {
            connectionManager.networkReformTest();
        } else if (item.getItemId() == R.id.menu_log_history) {
            //startActivity(new Intent(this, MeshLogHistoryActivity.class));
            byte[] encrypted = AesSaltEncryption.encrypt("Hello messager ".getBytes());
            byte[] decrypt = AesSaltEncryption.decrypt(encrypted);

            Log.e("Print_log", "Here :" + new String(decrypt));
        } else if (item.getItemId() == R.id.menu_send_all_message) {
            isAllMessageProcessClicked = true;
            sendAllHelloMessage();
        } else if (item.getItemId() == R.id.menu_get_nodeInfo) {
            Toast.makeText(this, "Details printed in Log", Toast.LENGTH_SHORT).show();
            ConnectionManager.on().stopAdhoc();
        } else if (item.getItemId() == R.id.menu_ble_message) {
            sendBleMessage();
        } else if (item.getItemId() == R.id.menu_user_delete) {
            connectionManager.deleteAllUsers();
        } else if (item.getItemId() == R.id.menu_content_broadcast) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, REQUEST_IMAGE_PICK);
        } else if (item.getItemId() == R.id.menu_mode_selection) {
            openUserModeDialog();
        } else if (item.getItemId() == R.id.menu_bt_message) {
            sendBTClassicMessage();
        }


        /*else if (item.getItemId() == R.id.menu_conn_stat){
           Intent intent = new Intent(this, ConnectivityStatActivity.class);
           startActivity(intent);
        }*/

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionManager != null) {
            connectionManager.stopMesh();
        }
    }

    @Override
    public void onMessageReceived(String messageId) {
        if (messageMap == null) return;

        UserModel userModel = messageMap.get(messageId);
        if (isAllMessageProcessClicked && userModel != null) {
            runOnUiThread(() -> {
                msgSendCount++;
                msgSendingStatusMenuItem.setTitle(msgSendCount + "/" + userCount);

                if (mCurrentFragment instanceof NearbyFragment) {
                    userModel.setSent(true);
                    ((NearbyFragment) mCurrentFragment).updateSentMessageScreen(userModel);
                    messageMap.remove(messageId);
                }

                if (userCount == msgSendCount) {
                    //msgSendingStatusMenuItem.setVisible(false);
                    // invalidateOptionsMenu();
                    isAllMessageProcessClicked = false;
                }
            });
        }

    }

    @Override
    public void onInterruption(int details) {

        switch (details) {
            case LinkStateListener.USER_DISABLED_WIFI:
                Toaster.showLong(String.format(getString(
                        com.w3engineers.mesh.R.string.wifi_off_user_warning),
                        getString(com.w3engineers.mesh.R.string.app_name)));
                break;

            case LinkStateListener.USER_DISABLED_BT:
                Toaster.showLong(String.format(getString(com.w3engineers.mesh.R.string.bt_off_user_warning),
                        getString(com.w3engineers.mesh.R.string.app_name)));
                break;

            case MeshSystemRequestActivity.REQUEST_SYSTEM_PERMISSIONS:
                ArrayList<String> permission = new ArrayList<>(1);
                permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                Intent intent = new Intent();
                intent.putExtra(MeshSystemRequestActivity.PERMISSION_REQUEST, permission);
                intent.putExtra(MeshSystemRequestActivity.PERMISSION_REQUEST_ONLY_SYSTEM_PERMISSION,
                        true);
                intent.setClass(getApplicationContext(), MeshSystemRequestActivity.class);
                intent.setAction(MeshSystemRequestActivity.class.getName());
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivityForResult(intent, SYSTEM_PRMISSION_REQUEST_CODE);
                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SYSTEM_PRMISSION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ConnectionManager.on().init();
        }

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

                    connectionManager.sendBroadcast(path);
                }
            }
        }
    }

    private void sendBleMessage() {
        MessageModel messageModel = new MessageModel();
        String dummyMessage = android.os.Build.MODEL + " Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop";
//        messageModel.message = android.os.Build.MODEL + " This is a text message";
        messageModel.message = dummyMessage;
        messageModel.friendsId = "frendsID";
        String msgId = UUID.randomUUID().toString();
        messageModel.messageId = msgId;
        MeshLog.v("[BLE_PROCESS] ble test message send from demo app");
        ConnectionManager.on().sendMessage("dummy", messageModel);
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

    private void openUserModeDialog() {
        String[] modes = {"Local", "Seller", "Buyer", "Internet"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select your mode Bro");

        builder.setSingleChoiceItems(modes, PreferencesHelper.on().getDataShareMode(), (dialog, which) -> {
            connectionManager.restartMesh(which);
            dialog.dismiss();
        });

        builder.show();
    }


    private void sendBTClassicMessage(){
        ConnectionManager.on().sendBtClassicMessage(Constants.LARGE_MESSAGE);
    }
}
