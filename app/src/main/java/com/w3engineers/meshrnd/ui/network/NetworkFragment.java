package com.w3engineers.meshrnd.ui.network;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.w3engineers.mesh.bluetooth.BTManager;
import com.w3engineers.mesh.db.SharedPref;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.mesh.util.GroupDevice;
import com.w3engineers.meshrnd.ConnectionManager;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.FragmentNetworkBinding;
import com.w3engineers.meshrnd.ui.base.BaseFragment;
import com.w3engineers.meshrnd.ui.base.ItemClickListener;

import java.util.List;
import java.util.Objects;

public class NetworkFragment extends BaseFragment implements ItemClickListener<GroupDevice> {
    private FragmentNetworkBinding binding;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_network, container, false);

        //binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // binding.progressBar.setVisibility(ViewUtil.GONE);
        //binding.textTitle.setText(SharedPref.onMessageReceived(Constants.KEY_USER_NAME) + "'s address");
        binding.userAddress.setText(SharedPref.read(Constant.KEY_USER_ID));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDeviceInfo();
    }


    public void updateDeviceInfo() {
        /*String name = SharedPref.onMessageReceived(Constants.DEVICE_NAME);
        boolean isMaster = SharedPref.readBoolean(Constant.KEY_P2P_MASTER);
        String group = "Client";
        if (isMaster) {
            group = "Master";
        }
        binding.textName.setText(name + " =:" + group);*/
        binding.tvWifiDirect.setText("WIFI link count: " + ConnectionManager.on().getLinkCount(1));
        binding.tvWifMesh.setText("WIFI Mesh link count: " + ConnectionManager.on().getLinkCount(2));
        binding.tvBleDirect.setText("BLE link count: " + ConnectionManager.on().getLinkCount(3));
        binding.tvBleMesh.setText("BLE Mesh link count: " + ConnectionManager.on().getLinkCount(4));
        binding.networkName.setText("BT - Current name: " + BTManager.
                getInstance(Objects.requireNonNull(getActivity()).getApplicationContext()).getName()+
                "-bcast name:"+SharedPref.read(Constant.KEY_DEVICE_BLE_NAME));

       /* List<String> uiUserlinkList = ConnectionManager.on().getUserIdList();
        StringBuilder userListString = new StringBuilder();
        if (uiUserlinkList != null) {
            for (String item : uiUserlinkList) {
                userListString.append("\n").append(item);
            }
        }


        List<String> wifiUserLinkList = ConnectionManager.on().getWifiLinkIds();
        StringBuilder wifiUserLinkString = new StringBuilder();
        if (wifiUserLinkList != null) {
            for (String item : wifiUserLinkList) {
                wifiUserLinkString.append("\n").append(item);
            }
        }


        List<String> bleUserLinkList = ConnectionManager.on().getBleLinkIds();
        StringBuilder bleUserLinkString = new StringBuilder();
        if (bleUserLinkList != null) {
            for (String item : bleUserLinkList) {
                bleUserLinkString.append("\n").append(item);
            }
        }


        List<String> bleMeshUserLinkList = ConnectionManager.on().getBleMeshLinkIds();
        StringBuilder bleMeshUserLinkString = new StringBuilder();
        if (bleMeshUserLinkList != null) {
            for (String item : bleMeshUserLinkList) {
                bleMeshUserLinkString.append("\n").append(item);
            }
        }

        binding.tvNetworkDetails.setText("UI user IDS: " + "\n" + userListString + "\n \nWifi IDS:\n " + wifiUserLinkString + "BT. IDS: \n" + bleUserLinkString + "\n\nBT. Mesh IDS: \n" + bleMeshUserLinkString);
 */   }


    @Override
    public void onItemClick(View view, GroupDevice item) {

    }
}
