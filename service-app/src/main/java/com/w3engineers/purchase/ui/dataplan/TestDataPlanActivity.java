package com.w3engineers.purchase.ui.dataplan;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.w3engineers.ext.strom.application.ui.base.BaseActivity;
import com.w3engineers.ext.strom.util.helper.Toaster;
import com.w3engineers.mesh.datasharing.util.PurchaseConstants;
import com.w3engineers.mesh.datasharing.util.Util;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.DialogUtil;
import com.w3engineers.mesh.util.HandlerUtil;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.TestActivityDataPlanBinding;
import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.dataplan.DataPlanManager;
import com.w3engineers.purchase.helper.PreferencesHelperDataplan;
import com.w3engineers.purchase.model.Seller;
import com.w3engineers.purchase.ui.util.ExpandableButton;
import com.w3engineers.purchase.ui.util.NotificationUtil;
import com.w3engineers.purchase.wallet.WalletManager;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;

public class TestDataPlanActivity extends BaseActivity implements DataPlanManager.DataPlanListener {

    private TestSellerListAdapter sellerListAdapter;
    private DataPlanViewModel viewModel;
    private DataLimitModel dataLimitModel;

    private RadioButton[] dataLimitRadioButtons;

    private Switch[] roleSwitches;
    private ExpandableButton[] expandableButtons;

//    private Calendar myCalendar;

    private ProgressDialog progressDialog;

    private int mCurrentRole;

    private TestActivityDataPlanBinding mBinding;
    private View view;
    private SimpleDateFormat sdf;
    private String appToken;


    @Override
    protected int getLayoutId() {
        return R.layout.test_activity_data_plan;
    }

    @Override
    protected int statusBarColor() {
        return com.w3engineers.meshrnd.R.color.colorPrimaryDark;
    }


    @Override
    public void startUI() {
        mBinding = (TestActivityDataPlanBinding) getViewDataBinding();

        setTitle();

        Intent intent = getIntent();
        if (intent != null) {
            appToken = intent.getStringExtra(TestDataPlanActivity.class.getSimpleName());
        }

        mBinding.localButton.setTopViewGone();
        mBinding.internetOnlyButton.setBottomViewGone();

        setListenerForAllExpandable();

        initSwitchListener();

        initAll();


        initRecyclerView();

        loadUI();

        setEventListener();

        parseIntent();
    }

    private void initAll() {

        sdf = new SimpleDateFormat("dd/MM/yy");

        viewModel = getViewModel();

        progressDialog = new ProgressDialog(this);
        dataLimitModel = DataLimitModel.getInstance(getApplicationContext());
//        myCalendar = Calendar.getInstance();
        mCurrentRole = DataPlanManager.getInstance().getDataPlanRole();
        dataLimitModel.setInitialRole(mCurrentRole);

        setClickListener(mBinding.buttonSave);

        DataPlanManager.getInstance().setDataPlanListener(this);


        expandableButtons = new ExpandableButton[]{mBinding.localButton, mBinding.sellDataButton, mBinding.buyDataButton, mBinding.internetOnlyButton};
        roleSwitches = new Switch[]{mBinding.switchButtonLocal, mBinding.switchButtonSeller, mBinding.switchButtonBuyer, mBinding.switchButtonInternet};
        dataLimitRadioButtons = new RadioButton[]{mBinding.unlimited, mBinding.limitTo};
    }

    private void checkAndCloseMesh(int role) {
        if (mCurrentRole == role) {
            DialogUtil.showConfirmationDialog(this, "Switching Mode?", "You can not turn off this mode, rather switch to another mode.", null,getResources().getString(com.w3engineers.mesh.R.string.ok) , new DialogUtil.DialogButtonListener() {
                @Override
                public void onClickPositive() {
                    roleSwitches[role].setChecked(true);
                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onClickNegative() {

                }
            });

//            DataPlanManager.getInstance().closeMesh(DataPlanConstants.USER_ROLE.MESH_STOP);
//            mCurrentRole = DataPlanConstants.USER_ROLE.MESH_STOP;
        }else {
            expandableButtons[role].setTextColor(getResources().getColor(R.color.data_plan_unselected_text));
        }

    }


    private void initSwitchListener() {
        mBinding.switchButtonLocal.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            dataPlanRadioClicked(DataPlanConstants.USER_ROLE.MESH_USER);

//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch On", Toast.LENGTH_SHORT).show();
                        } else {

                            checkAndCloseMesh(DataPlanConstants.USER_ROLE.MESH_USER);
//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch Off", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        mBinding.switchButtonSeller.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            dataPlanRadioClicked(DataPlanConstants.USER_ROLE.DATA_SELLER);

//                          Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch On", Toast.LENGTH_SHORT).show();
                        } else {

                            checkAndCloseMesh(DataPlanConstants.USER_ROLE.DATA_SELLER);
//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch Off", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        mBinding.switchButtonBuyer.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {

                            dataPlanRadioClicked(DataPlanConstants.USER_ROLE.DATA_BUYER);


//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch On", Toast.LENGTH_SHORT).show();
                        } else {
                            checkAndCloseMesh(DataPlanConstants.USER_ROLE.DATA_BUYER);
//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch Off", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        mBinding.switchButtonInternet.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            dataPlanRadioClicked(DataPlanConstants.USER_ROLE.INTERNET_USER);

//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch On", Toast.LENGTH_SHORT).show();
                        } else {
                            checkAndCloseMesh(DataPlanConstants.USER_ROLE.INTERNET_USER);
//                            Toast.makeText(TestDataPlanActivity.this,
//                                    "Switch Off", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }

    private void setListenerForAllExpandable() {
        mBinding.localButton.setCallbackListener(new ExpandableButton.ExpandableButtonListener() {
            @Override
            public void onViewExpanded() {
                //Toast.makeText(TestDataPlanActivity.this, "local Button Expanded", Toast.LENGTH_SHORT).show();
                mBinding.localButton.setBackgroundColor(getResources().getColor(R.color.white));
                collapseView(mBinding.localButton);

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.localButton)) {
                    mBinding.localButton.setTextColor(getResources().getColor(R.color.data_plan_selected_text));
                }
            }

            @Override
            public void onViewCollapsed() {
                mBinding.localButton.setBackgroundColor(getResources().getColor(R.color.collapse_button_color));

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.localButton)) {
                    mBinding.localButton.setTextColor(getResources().getColor(R.color.data_plan_unselected_text));
                }
                //Toast.makeText(TestDataPlanActivity.this, "local Button Collapsed", Toast.LENGTH_SHORT).show();
            }
        });

        mBinding.sellDataButton.setCallbackListener(new ExpandableButton.ExpandableButtonListener() {
            @Override
            public void onViewExpanded() {
                //Toast.makeText(TestDataPlanActivity.this, "seller Button Expanded", Toast.LENGTH_SHORT).show();
                mBinding.sellDataButton.setBackgroundColor(getResources().getColor(R.color.white));
                collapseView(mBinding.sellDataButton);

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.sellDataButton)) {
                    mBinding.sellDataButton.setTextColor(getResources().getColor(R.color.data_plan_selected_text));
                }
            }

            @Override
            public void onViewCollapsed() {
                mBinding.sellDataButton.setBackgroundColor(getResources().getColor(R.color.collapse_button_color));

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.sellDataButton)) {
                    mBinding.sellDataButton.setTextColor(getResources().getColor(R.color.data_plan_unselected_text));
                }
                //Toast.makeText(TestDataPlanActivity.this, "seller Button Collapsed", Toast.LENGTH_SHORT).show();
            }
        });

        mBinding.buyDataButton.setCallbackListener(new ExpandableButton.ExpandableButtonListener() {
            @Override
            public void onViewExpanded() {
                //Toast.makeText(TestDataPlanActivity.this, "buyer Button  Button Expanded", Toast.LENGTH_SHORT).show();
                mBinding.buyDataButton.setBackgroundColor(getResources().getColor(R.color.white));
                collapseView(mBinding.buyDataButton);

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.buyDataButton)) {
                    mBinding.buyDataButton.setTextColor(getResources().getColor(R.color.data_plan_selected_text));
                }
            }

            @Override
            public void onViewCollapsed() {
                mBinding.buyDataButton.setBackgroundColor(getResources().getColor(R.color.collapse_button_color));

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.buyDataButton)) {
                    mBinding.buyDataButton.setTextColor(getResources().getColor(R.color.data_plan_unselected_text));
                }
                // Toast.makeText(TestDataPlanActivity.this, "buyer Button Button Collapsed", Toast.LENGTH_SHORT).show();
            }
        });

        mBinding.internetOnlyButton.setCallbackListener(new ExpandableButton.ExpandableButtonListener() {
            @Override
            public void onViewExpanded() {
                // Toast.makeText(TestDataPlanActivity.this, "internet Only Button Expanded", Toast.LENGTH_SHORT).show();
                mBinding.internetOnlyButton.setBackgroundColor(getResources().getColor(R.color.white));
                collapseView(mBinding.internetOnlyButton);

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.internetOnlyButton)) {
                    mBinding.internetOnlyButton.setTextColor(getResources().getColor(R.color.data_plan_selected_text));
                }
            }

            @Override
            public void onViewCollapsed() {
                mBinding.internetOnlyButton.setBackgroundColor(getResources().getColor(R.color.collapse_button_color));

                if (mCurrentRole == DataPlanConstants.USER_ROLE.MESH_STOP || !expandableButtons[mCurrentRole].equals(mBinding.internetOnlyButton)) {
                    mBinding.internetOnlyButton.setTextColor(getResources().getColor(R.color.data_plan_unselected_text));
                }
                // Toast.makeText(TestDataPlanActivity.this, "internetOnly Button Collapsed", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setTitle() {

        setSupportActionBar(mBinding.toolbar);
        mBinding.toolbar.setTitle(getResources().getString(com.w3engineers.mesh.R.string.data_plan));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mBinding.toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
    }

    private void collapseView(ExpandableButton button) {
        if (button.getId() == R.id.localButton) {
            mBinding.sellDataButton.collapseView();
            mBinding.buyDataButton.collapseView();
            mBinding.internetOnlyButton.collapseView();
        } else if (button.getId() == R.id.sellDataButton) {
            mBinding.localButton.collapseView();
            mBinding.buyDataButton.collapseView();
            mBinding.internetOnlyButton.collapseView();
        } else if (button.getId() == R.id.buyDataButton) {
            mBinding.localButton.collapseView();
            mBinding.sellDataButton.collapseView();
            mBinding.internetOnlyButton.collapseView();
        } else if (button.getId() == R.id.internetOnlyButton) {
            mBinding.localButton.collapseView();
            mBinding.sellDataButton.collapseView();
            mBinding.buyDataButton.collapseView();
        }
    }


    public void childClicked(View view) {
        ((TextView) view).setText("Task Completed (Expandable Button color changed)");
        mBinding.buyDataButton.setBarColor(Color.parseColor("#297e55"));
    }

    private DataPlanViewModel getViewModel() {
        return ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new DataPlanViewModel(getApplication());
            }
        }).get(DataPlanViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (DataPlanManager.getInstance().getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER) {
            prepareSellerData();
//        }
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);

        if (view.getId() == R.id.button_save) {
            checkSharingLimit();
        } else if (view.getId() == R.id.status) {
            Seller seller = (Seller) view.getTag();
            onButtonClickListener(seller);
        }
    }

    @Override
    public void onConnectingWithSeller(String sellerAddress) {
        runOnUiThread(() -> {
            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.PURCHASING);
                    item.setBtnEnabled(false);
                    getAdapter().addItem(item);
                    return;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_wallet, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_wallet) {
            WalletManager.openActivity(this, null);
        }else {
            finish();
        }

        return false;
    }

        @Override
    public void onPurchaseFailed(String sellerAddress, String msg) {
        runOnUiThread(() -> {
            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.PURCHASE);
                    item.setBtnEnabled(true);
                    getAdapter().addItem(item);
                    Toaster.showLong(msg);
                    return;
                }
            }
        });
    }

    @Override
    public void onPurchaseSuccess(String sellerAddress, double purchasedData, long blockNumber) {
        runOnUiThread(() -> {
            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.CLOSE);
                    item.setBtnEnabled(true);
                    item.setPurchasedData(purchasedData);
                    item.setBlockNumber(blockNumber);
                    getAdapter().addItem(item);
                    return;
                }
            }
        });
    }

    @Override
    public void onPurchaseClosing(String sellerAddress) {
        runOnUiThread(() -> {
            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.CLOSING);
                    item.setBtnEnabled(false);
                    getAdapter().addItem(item);
                    return;
                }
            }
        });
    }

    @Override
    public void onPurchaseCloseFailed(String sellerAddress, String msg) {
        runOnUiThread(() -> {
            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.CLOSE);
                    item.setBtnEnabled(true);
                    getAdapter().addItem(item);
                    Toaster.showLong(msg);
                    return;
                }
            }
        });
    }

    @Override
    public void onTopUpFailed(String sellerAddress, String msg) {
        runOnUiThread(() -> {
            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.TOP_UP);
                    item.setBtnEnabled(true);
                    getAdapter().addItem(item);
                    Toaster.showLong(msg);
                    return;
                }
            }
        });
    }

    @Override
    public void onRoleSwitchCompleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onLimitFinished(boolean isFullyFinished, String message) {
        mBinding.dataLimitError.setTextColor(isFullyFinished ? Color.RED : Color.argb(255, 255, 140, 0));
        mBinding.dataLimitError.setText(message);
        mBinding.dataLimitError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPurchaseCloseSuccess(String sellerAddress) {
        runOnUiThread(() -> {
            //TODO remove item if seller not connected

            for (Seller item : getAdapter().getItems()) {
                if (item.getId().equalsIgnoreCase(sellerAddress)) {
                    item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.PURCHASE);
                    item.setBtnEnabled(true);
                    getAdapter().addItem(item);
                    break;
                }
            }
        });
    }

    @Override
    public void showToastMessage(String msg) {
        runOnUiThread(() -> Toaster.showLong(msg));
    }

    @Override
    public void onBalancedFinished(String sellerAddress, int remain) {
        runOnUiThread(() -> {
            if (remain == 1) {
                for (Seller item : getAdapter().getItems()) {
                    if (item.getId().equalsIgnoreCase(sellerAddress)) {
                        item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.TOP_UP);
                        item.setBtnEnabled(true);
                        getAdapter().addItem(item);
                        break;
                    }
                }
            } else {
                for (Seller item : getAdapter().getItems()) {
                    if (item.getId().equalsIgnoreCase(sellerAddress)) {
                        item.setBtnText(DataPlanConstants.SELLERS_BTN_TEXT.PURCHASE);
                        item.setBtnEnabled(true);
                        getAdapter().addItem(item);
                        break;
                    }
                }
            }
        });
    }

    private void prepareSellerData() {

        viewModel.allSellers.observe(this, sellers -> {
            if (sellers != null) {
                getAdapter().clear();
                getAdapter().addItems(sellers);
            }
        });

        viewModel.getAllSellers();
    }


    private TestSellerListAdapter getAdapter() {
        return (TestSellerListAdapter) mBinding.testDataSellerList.getAdapter();
    }

    private String getKey(int prev, int cur) {
        return prev + "" + cur;
    }

    private void dataPlanRadioClicked(int type) {
        if (mCurrentRole == type)
            return;
        setRoleTasks(mCurrentRole, type);
    }


    private void setRoleTasks(int prev, int newRole) {

        progressDialog.setMessage(getResources().getString(com.w3engineers.mesh.R.string.switching_role));
        progressDialog.setCancelable(false);
        progressDialog.show();

        mCurrentRole = newRole;
        viewModel.roleSwitch(mCurrentRole, prev);


        if (prev != DataPlanConstants.USER_ROLE.MESH_STOP){
            roleSwitches[prev].setChecked(false);
            expandableButtons[mCurrentRole].setTextColor(Color.argb(255, 0, 141, 255));
        }
    }

    private void initRecyclerView() {
        mBinding.testDataSellerList.setItemAnimator(null);
        mBinding.testDataSellerList.setHasFixedSize(true);
        mBinding.testDataSellerList.setLayoutManager(new LinearLayoutManager(this));

        sellerListAdapter = new TestSellerListAdapter(this);
        mBinding.testDataSellerList.setAdapter(sellerListAdapter);
    }

    private void loadUI() {

        if (mCurrentRole != DataPlanConstants.USER_ROLE.MESH_STOP ) {
            roleSwitches[DataPlanManager.getInstance().getDataPlanRole()].setChecked(true);
            expandableButtons[DataPlanManager.getInstance().getDataPlanRole()].setTextColor(Color.argb(255, 0, 141, 255));

            HandlerUtil.postForeground(new Runnable() {
                @Override
                public void run() {
                    expandableButtons[DataPlanManager.getInstance().getDataPlanRole()].expandView();
                }
            }, 500);
        }

        dataLimitRadioButtons[dataLimitModel.isDataLimited() ? 1 : 0].setChecked(true);

        setDataLimitEnabled(dataLimitModel.isDataLimited());

        long sharedData = DataPlanManager.getInstance().getSellAmountData();

        if (sharedData <= 0) {
            dataLimitModel.setSharedData(convertMegabytesToBytes(10));
            mBinding.range.setText("10");
        } else {
            int amount = (int) convertBytesToMegabytes(sharedData);
            mBinding.range.setText(amount + "");
        }

        float dataPerMb = PreferencesHelperDataplan.on().getPerMbTokenValue();
        mBinding.sellDataTextView.setText(String.format(getResources().getString(R.string.sell_your_data_info), dataPerMb + ""));
        mBinding.buyDataTextView.setText(String.format(getResources().getString(R.string.buy_data_info), dataPerMb + ""));

        if (dataLimitModel.isDataLimited()){

            long remainingData = DataPlanManager.getInstance().getRemainingData();
            if (remainingData <= PurchaseConstants.SELLER_MINIMUM_ERROR_DATA){
                showDatalimitError(this.getString(R.string.data_limit_error));
            } else if (remainingData < Constant.SELLER_MINIMUM_WARNING_DATA) {
                showDatalimitWarning(this.getString(R.string.data_limit_warning));
            }
        }

        disableSaveButton();
    }

    private void showDatalimitWarning(String msg){
        mBinding.dataLimitError.setTextColor(Color.argb(255, 255, 140, 0));
        mBinding.dataLimitError.setText(msg);
        mBinding.dataLimitError.setVisibility(View.VISIBLE);
    }

    private void showDatalimitError(String msg){
        mBinding.dataLimitError.setTextColor(Color.RED);
        mBinding.dataLimitError.setText(msg);
        mBinding.dataLimitError.setVisibility(View.VISIBLE);
    }

    public double convertBytesToMegabytes(long bytes) {
        return (double) bytes / (1024.0 * 1024.0);
    }

    private long convertMegabytesToBytes(double mb) {
        return (long) mb * 1024 * 1024;
    }

    private void setDataLimitEnabled(boolean value) {
        mBinding.range.setEnabled(value);
    }

    private void setEventListener() {
        mBinding.range.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0) {
                    enableSaveButton();
                } else {
                    disableSaveButton();
                }
            }
        });

//        DatePickerDialog.OnDateSetListener date = (view, year, monthOfYear, dayOfMonth) -> {
//
//            myCalendar.set(Calendar.YEAR, year);
//            myCalendar.set(Calendar.MONTH, monthOfYear);
//            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
//            mBinding.toDate.setText(sdf.format(myCalendar.getTimeInMillis()));
//            mBinding.fromDate.setText(sdf.format(System.currentTimeMillis()));
//
//            enableSaveButton();
//        };


//        mBinding.toDate.setOnClickListener(v -> {
//            if (dataLimitModel.getToDate() > myCalendar.getTimeInMillis() - 1000) {
//                myCalendar.setTimeInMillis(dataLimitModel.getToDate());
//            }
//
//            DatePickerDialog toDatePickerDialog = new DatePickerDialog(TestDataPlanActivity.this, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH));
//            toDatePickerDialog.getDatePicker().setMinDate(dataLimitModel.getFromDate() > System.currentTimeMillis() ? dataLimitModel.getFromDate() : System.currentTimeMillis());
//            toDatePickerDialog.show();
//        });
    }

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(TestDataPlanActivity.class.getName())) {
            if (intent.getBooleanExtra(TestDataPlanActivity.class.getName(), false)) {
                NotificationUtil.removeSellerNotification(this);
                showSellerWarningDialog(intent.getIntExtra(DataPlanConstants.IntentKeys.NUMBER_OF_ACTIVE_BUYER, 0));
            }
        }
    }

    private void showSellerWarningDialog(int activeBuyer) {

        if (dataLimitModel.isDataLimited()){
            long remainingData = DataPlanManager.getInstance().getRemainingData();
            if (remainingData <= PurchaseConstants.SELLER_MINIMUM_ERROR_DATA){
                long sharedData = DataPlanManager.getInstance().getSellAmountData();

                DialogUtil.showConfirmationDialog(TestDataPlanActivity.this, "Data Limit exceeded!",
                        "Your data shared limit" + " " + Util.humanReadableByteCount(sharedData) + " " + "exceeded, there are" + " " + activeBuyer + " " + "active buyer." + "Do you want to increase your shared data limit? If not then all the active channel will be closed",
                        getResources().getString(com.w3engineers.mesh.R.string.no),
                        getResources().getString(com.w3engineers.mesh.R.string.yes),
                        new DialogUtil.DialogButtonListener() {
                            @Override
                            public void onClickPositive() {

                            }

                            @Override
                            public void onCancel() {
                            }

                            @Override
                            public void onClickNegative() {
                                DataPlanManager.getInstance().closeAllActiveChannel();
                            }
                        });
            }else if (remainingData <= Constant.SELLER_MINIMUM_WARNING_DATA){
                DialogUtil.showConfirmationDialog(TestDataPlanActivity.this, "Data Limit almost exceeded!",
                        "You have only" + " " + Util.humanReadableByteCount(remainingData) + " " + "remaining shared data, please increase limit before finishing all",
                        null,
                        getResources().getString(com.w3engineers.mesh.R.string.ok),
                        new DialogUtil.DialogButtonListener() {
                            @Override
                            public void onClickPositive() {

                            }

                            @Override
                            public void onCancel() {
                            }

                            @Override
                            public void onClickNegative() {
                            }
                        });
            }
        }
    }

    public void onButtonClickListener(Seller item) {
        if (DataPlanManager.getInstance().getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER) {
            String btnText = item.getBtnText();
            if (btnText.toLowerCase().equals(DataPlanConstants.SELLERS_BTN_TEXT.CLOSE.toLowerCase())) {
                showDisconnectConfirmation(item);
            } else {
                showInputDialog(item);
            }
        } else {
            Toaster.showShort("This feature is available only for a Buyer");
        }


    }

    private void showDisconnectConfirmation(Seller seller) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(com.w3engineers.mesh.R.layout.disconnect_confirmation, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);
        TextView bodyText, stopAndRefund, onlyStop, cancel;
        bodyText = promptsView.findViewById(com.w3engineers.mesh.R.id.bodyText);
        stopAndRefund = promptsView.findViewById(com.w3engineers.mesh.R.id.tv_stop_and_refund);

        cancel = (TextView) promptsView.findViewById(com.w3engineers.mesh.R.id.tv_cancel);

        bodyText.setText("You have already used up " + seller.getUsedData() + "MB of " + seller.getPurchasedData()
                + "MB. Are you sure to stop data usage now?");

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        stopAndRefund.setOnClickListener(v -> {
            DataPlanManager.getInstance().closePurchase(seller.getId());
            alertDialog.cancel();
        });
        cancel.setOnClickListener(v -> {
            alertDialog.cancel();
        });

        alertDialog.show();
    }

    private void showInputDialog(Seller seller) {
        runOnUiThread(() -> {
            LayoutInflater li = LayoutInflater.from(TestDataPlanActivity.this);
            View promptsView = li.inflate(com.w3engineers.mesh.R.layout.text_input_dataamount, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TestDataPlanActivity.this);
            alertDialogBuilder.setView(promptsView);

            TextView tvOk, tvCancel;
            final EditText userInput = (EditText) promptsView.findViewById(com.w3engineers.mesh.R.id.et_user_input);
            tvOk = (TextView) promptsView.findViewById(com.w3engineers.mesh.R.id.tv_ok);
            tvCancel = (TextView) promptsView.findViewById(com.w3engineers.mesh.R.id.tv_cancel);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.setCancelable(false);
            tvOk.setOnClickListener(v -> {
                String inputText = userInput.getText().toString();
                if (inputText.length() > 0) {
                    double amount = Double.valueOf(inputText);
                    if (amount > 0) {
                        DataPlanManager.getInstance().initPurchase(amount, seller.getId());
                    } else {
                        Toaster.showShort("Data amount should be bigger than zero.");
                    }
                } else {
                    Toaster.showShort("Data amount required.");
                }
                alertDialog.cancel();
            });

            tvCancel.setOnClickListener(v -> {
                alertDialog.cancel();
            });

            alertDialog.show();
        });
    }

    public void onRadioUnlimitedButtonClicked(View view) {
        setDataLimitEnabled(false);
//        mBinding.dataUsageLimited.setVisibility(View.INVISIBLE);
//        mBinding.dataUsageUnlimited.setVisibility(View.VISIBLE);

//        mBinding.textViewDataLimitWarning.setVisibility(View.GONE);
        enableSaveButton();



    }

    public void onRadioLimitedButtonClicked(View view) {
        setDataLimitEnabled(true);
//        mBinding.dataUsageLimited.setVisibility(View.VISIBLE);
//        mBinding.dataUsageUnlimited.setVisibility(View.INVISIBLE);
        enableSaveButton();
    }

    private void disableSaveButton() {
        mBinding.buttonSave.setEnabled(false);
//        int paddingTopBottom = mBinding.saveButton.getPaddingTop();
//        int paddingLeftRight = mBinding.saveButton.getTotalPaddingLeft();
        mBinding.buttonSave.setBackground(ContextCompat.getDrawable(TestDataPlanActivity.this, R.drawable.rectangular_gray_small));
        mBinding.buttonSave.setTextColor(TestDataPlanActivity.this.getResources().getColor(R.color.colorEtBorder));
//        mBinding.buttonSave.setPadding(paddingLeftRight, paddingTopBottom, paddingLeftRight, paddingTopBottom);
    }

    private void enableSaveButton() {
        mBinding.buttonSave.setEnabled(true);
//        int paddingTopBottom = mBinding.saveButton.getPaddingTop();
//        int paddingLeftRight = mBinding.saveButton.getTotalPaddingLeft();
        mBinding.buttonSave.setBackground(ContextCompat.getDrawable(TestDataPlanActivity.this, R.drawable.gradient_color_primary_and_dark));
        mBinding.buttonSave.setTextColor(Color.WHITE);
//        mBinding.saveButton.setPadding(paddingLeftRight, paddingTopBottom, paddingLeftRight, paddingTopBottom);
    }

    private void checkSharingLimit() {

        if (mBinding.limitTo.isChecked()) {
            try {
                long from = dataLimitModel.getFromDate();
                long sharedData = dataLimitModel.getSharedData();

                long tempSharedData = convertMegabytesToBytes(Integer.valueOf(mBinding.range.getText().toString()));

                if (tempSharedData <= 0){
                    showDatalimitError(this.getString(R.string.data_limit_validation_text));
                } else {
                    if (from == 0 || sharedData == 0){
                        mBinding.dataLimitError.setVisibility(View.INVISIBLE);
                        dataLimitModel.setFromDate(System.currentTimeMillis());
                        dataLimitModel.setSharedData(tempSharedData);
                        dataLimitModel.setDataLimited(true);
                        disableSaveButton();
                        DataPlanManager.resumeMessaging();
                        if (tempSharedData < Constant.SELLER_MINIMUM_WARNING_DATA){
                            showDatalimitWarning(this.getString(R.string.data_limit_warning));
                        }
                    } else {
                        long usedData = DataPlanManager.getInstance().getUsedData(this, from);
                        if (sharedData <= (usedData + PurchaseConstants.SELLER_MINIMUM_ERROR_DATA)){
                            mBinding.dataLimitError.setVisibility(View.INVISIBLE);
                            dataLimitModel.setFromDate(System.currentTimeMillis());
                            dataLimitModel.setSharedData(tempSharedData);
                            dataLimitModel.setDataLimited(true);
                            disableSaveButton();
                            DataPlanManager.resumeMessaging();
                            if (tempSharedData < Constant.SELLER_MINIMUM_WARNING_DATA){
                                showDatalimitWarning(this.getString(R.string.data_limit_warning));
                            }
                        }else {
                            if (tempSharedData <= usedData) {
                                showDatalimitError(this.getString(R.string.data_limit_larger_needed));
                            } else {
                                mBinding.dataLimitError.setVisibility(View.INVISIBLE);
                                dataLimitModel.setSharedData(tempSharedData);
                                dataLimitModel.setDataLimited(true);
                                disableSaveButton();
                                DataPlanManager.resumeMessaging();
                                if (tempSharedData - usedData < Constant.SELLER_MINIMUM_WARNING_DATA){
                                    showDatalimitWarning(this.getString(R.string.data_limit_warning));
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            mBinding.dataLimitError.setVisibility(View.INVISIBLE);
            dataLimitModel.setDataLimited(false);
            dataLimitModel.setFromDate(0);
            dataLimitModel.setSharedData(0l);
            disableSaveButton();
            DataPlanManager.resumeMessaging();
        }
    }

//    public long getDayWiseTimeStamp(long timeStamp) {
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(timeStamp);
//        cal.set(Calendar.HOUR_OF_DAY, 0); //set hours to zero
//        cal.set(Calendar.MINUTE, 0); // set minutes to zero
//        cal.set(Calendar.SECOND, 0); //set seconds to zero
//        return (cal.getTimeInMillis() / 1000) * 1000;
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//        MeshLog.v("option menu created ");
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_wallet, menu);
//        return true;
//    }
//
//    /*
//     * (non-Javadoc)
//     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
//     */
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.menu_wallet) {
//            WalletManager.openActivity(this, null);
//        }
//        else if (item.getItemId() == android.R.id.home){
//            onBackPressed();
//        }
//        return false;
//    }
}
