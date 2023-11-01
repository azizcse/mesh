package com.w3engineers.meshrnd.ui.main;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.PersistableBundle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.w3engineers.mesh.util.Constant;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ActivityMainBinding;
import com.w3engineers.meshrnd.ui.nav.BottomNavActivity;
import com.w3engineers.mesh.db.SharedPref;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;

    private UserListAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mBinding.progressBar.setVisibility(View.GONE);
        //mBinding.scanBtn.setOnClickListener(this);
        //mBinding.scanBluetooth.setOnClickListener(this);
        //mBinding.scanDirect.setOnClickListener(this);

        mAdapter = new UserListAdapter(this);
        mBinding.recyclerView.setAdapter(mAdapter);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //mAdapter.setItemClickListener(this);


        mBinding.textTitle.setText(SharedPref.read(Constant.KEY_USER_NAME)+"'s address");

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_item, menu);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.broadcast_user:
               startActivity(new Intent(this, BottomNavActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
