package com.kotophoto;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import  android.support.v4.app.FragmentManager;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.frameL);

        if(fragment==null)
        {
            fragment = CameraFragment.newInstance();
            fm.beginTransaction().add(R.id.frameL,fragment).commit();
        }
    }
}
