package com.w3engineers.meshrnd.ui.meshlog;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.meshrnd.util.HandlerUtil;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.FragmentMessageBinding;
import com.w3engineers.meshrnd.model.MeshLogModel;
import com.w3engineers.meshrnd.ui.base.BaseFragment;
import com.w3engineers.meshrnd.ui.createuser.CreateUserActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MeshLogFragment extends BaseFragment implements View.OnClickListener {
    private FragmentMessageBinding binding;
    private String[] logArray = {"All", "Info", "Warning", "Error", "Special"};
    private final int ALL = 0, INFO = 1, WARNING = 2, ERROR = 3, SPECIAL = 4;


    private int type = ALL;

    private MeshLogAdapter mAdapter;

    private List<MeshLogModel> logList;

    private boolean isScrollOff;

    private int searchPosition;

    private boolean isNormalSearchOn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_message, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logList = new ArrayList<>();
        initAdapter();
        initSearch();
        initAdvanceSearch();
        initCheckBoxListener();

        binding.textViewRestart.setOnClickListener(this);
        binding.textViewClear.setOnClickListener(this);
        binding.imageViewSearchClear.setOnClickListener(this);
        binding.imageViewAdvanceSearchClear.setOnClickListener(this);

        readFile();

        MeshLog.sMeshLogListener = mMeshLogListener;

       /* binding.scrollView.post(new Runnable() {
            @Override
            public void run() {
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });*/


        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String logName = logArray[position];
                binding.editTextAdvanceSearch.setText("");
                binding.editTextSearch.setText("");
                showItem(logName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            MeshLog.sMeshLogListener = mMeshLogListener;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_view_left:
                leftSectionSearch();
                break;
            case R.id.image_view_right:
                rightSectionSearch();
                break;
            case R.id.text_view_restart:
                restartApp();
                break;
            case R.id.text_view_clear:
                mAdapter.clear();
                logList.clear();
                // We can remove text file also
                // MeshLog.clearLog();
                break;
            case R.id.image_view_advance_search_clear:
                if (!TextUtils.isEmpty(binding.editTextAdvanceSearch.getText().toString().trim())) {
                    binding.editTextAdvanceSearch.setText("");
                }
                break;
            case R.id.image_view_search_clear:
                if (!TextUtils.isEmpty(binding.editTextSearch.getText().toString().trim())) {
                    binding.editTextSearch.setText("");
                }
                break;
        }
    }

    private void restartApp() {
        if (getActivity() == null) return;
        Intent mStartActivity = new Intent(getActivity(), CreateUserActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getActivity(), mPendingIntentId, mStartActivity,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    private void showItem(String logItem) {
        if (logItem.equalsIgnoreCase(logArray[2])) {
            type = WARNING;
        } else if (logItem.equalsIgnoreCase(logArray[1])) {
            type = INFO;

        } else if (logItem.equalsIgnoreCase(logArray[3])) {
            type = ERROR;

        } else if (logItem.equalsIgnoreCase(logArray[4])) {
            type = SPECIAL;
        } else {
            type = ALL;
        }

        HandlerUtil.postForeground(() -> filterByLogTag(type), 300);

    }

    private void readFile() {
/*        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() +
                "/MeshRnD");*/
        Constant.Directory directoryContainer =  new Constant.Directory();
        String sdCard = directoryContainer.getParentDirectory() + Constant.Directory.MESH_LOG;
        File directory = new File(sdCard);

        File file = new File(directory, Constant.CURRENT_LOG_FILE_NAME);

        try {

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                int logType = 0;
                if (line.startsWith(MeshLog.INFO)) {
                    logType = INFO;
                } else if (line.startsWith(MeshLog.WARNING)) {
                    logType = WARNING;
                } else if (line.startsWith(MeshLog.ERROR)) {
                    logType = ERROR;
                } else if (line.startsWith(MeshLog.SPECIAL)) {
                    logType = SPECIAL;
                }

                MeshLogModel model = new MeshLogModel(logType, line);
                mAdapter.addItemToPosition(model, 0);
                logList.add(0, model);
            }


            br.close();

            scrollSmoothly();

        } catch (IOException e) {
            e.printStackTrace();
        }

        //binding.textView.setText(text.toString());
        //mainText = binding.textView.getText().toString();
    }


    public synchronized void newMessageFound(final String msg) {

        final int logType;
        if (msg.startsWith(MeshLog.INFO)) {
            logType = INFO;
        } else if (msg.startsWith(MeshLog.WARNING)) {
            logType = WARNING;
        } else if (msg.startsWith(MeshLog.ERROR)) {
            logType = ERROR;
        } else if (msg.startsWith(MeshLog.SPECIAL)) {
            logType = SPECIAL;
        } else {
            logType = 0;
        }

        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {

                MeshLogModel model = new MeshLogModel(logType, msg);
                if (type == ALL || type == logType) {


                    if (isNormalSearchOn) {
                        String searchText = binding.editTextSearch.getText().toString().trim();
                        if (model.getLog().contains(searchText)) {
                            mAdapter.addItemToPosition(model, 0);
                        }
                    } else {
                        mAdapter.addItemToPosition(model, 0);
                    }

                    if (!isScrollOff) {
                        scrollSmoothly();
                    }
                }
                logList.add(0, model);

                // binding.textView.setText(text.toString());
                // mainText = binding.textView.getText().toString();
            });
        }
    }

    MeshLog.MeshLogListener mMeshLogListener = text -> {
        if (Text.isNotEmpty(text)) {
            newMessageFound(text);
        }
    };

    private void initSearch() {
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (TextUtils.isEmpty(s.toString())) {
                    binding.editTextAdvanceSearch.setEnabled(true);
                } else {
                    binding.editTextAdvanceSearch.setEnabled(false);
                }

                HandlerUtil.postForeground(() -> {
                    mAdapter.getFilter().filter(s);
                    if (TextUtils.isEmpty(s)) {
                        isNormalSearchOn = false;
                        // mAdapter.clear();
                        //mAdapter.addItem(logList);
                    } else {
                        isNormalSearchOn = true;
                        //mAdapter.getFilter().filter(s);
                    }
                    mAdapter.notifyDataSetChanged();
                }, 500);

            }
        });
    }

    private void initAdvanceSearch() {
        binding.editTextAdvanceSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString())) {
                    binding.imageViewLeft.setEnabled(false);
                    binding.imageViewRight.setEnabled(false);
                    searchPosition = 0;

                    binding.editTextSearch.setEnabled(true);

                } else {
                    binding.imageViewLeft.setEnabled(true);
                    binding.imageViewRight.setEnabled(true);

                    binding.editTextSearch.setEnabled(false);
                }


                HandlerUtil.postForeground(() -> {
                    mAdapter.advanceSearch(s.toString());
                    mAdapter.notifyDataSetChanged();
                }, 500);
            }
        });

        binding.imageViewLeft.setOnClickListener(this);
        binding.imageViewRight.setOnClickListener(this);

        binding.imageViewLeft.setEnabled(false);
        binding.imageViewRight.setEnabled(false);
    }

    private void initCheckBoxListener() {
        binding.checkBoxScroll.setOnCheckedChangeListener((buttonView, isChecked) -> isScrollOff = isChecked);
    }

    private void initAdapter() {
        binding.recyclerViewLog.setHasFixedSize(true);
        binding.recyclerViewLog.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new MeshLogAdapter(getActivity());
        binding.recyclerViewLog.setAdapter(mAdapter);
        mAdapter.clear();
    }

    private void filterByLogTag(int tag) {
        try {
            mAdapter.clear();
            if (tag == ALL) {
                mAdapter.addItem(logList);
            } else {
                for (MeshLogModel meshLogModel : logList) {
                    if (meshLogModel.getType() == tag) {
                        mAdapter.addItem(meshLogModel);
                    }
                }
            }

            scrollSmoothly();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void scrollSmoothly() {
        binding.recyclerViewLog.smoothScrollToPosition(0);

       /* int index = mAdapter.getItemCount() - 1;
        if (index > 0) {
            binding.recyclerViewLog.smoothScrollToPosition(index);
        }*/
    }

    private void scrollSmoothlyByPosition(int position) {
        if (position >= 0 && position < mAdapter.getMatchedPosition().size()) {
            binding.recyclerViewLog.smoothScrollToPosition(mAdapter.getMatchedPosition().get(position));
        }
    }


    private void leftSectionSearch() {
        if (searchPosition > 0) {
            searchPosition--;
            scrollSmoothlyByPosition(searchPosition);
        }
    }

    private void rightSectionSearch() {
        // Log.d("AdvanceSearchTest", "totalItem: " + mAdapter.getMatchedPosition().size()+" current position: "+searchPosition);
        if (searchPosition < mAdapter.getMatchedPosition().size() - 1) {
            searchPosition++;
            scrollSmoothlyByPosition(searchPosition);
        }
    }
}
