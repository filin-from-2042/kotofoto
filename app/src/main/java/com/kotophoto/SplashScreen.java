package com.kotophoto;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // звук по-умолчанию
        SharedPreferences sp = getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
        String selectedSound = sp.getString(getString(R.string.selected_sound_key),null);
        if(selectedSound==null) Global.resetSelectedSoundToDefault(this);

        // директории
        File photoFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + getString(R.string.folder_name));
        if (!photoFolder.exists()) {
            photoFolder.mkdirs();
        }

        File soundFolder = new File( Global.getAppPublicFolder(this));
        if (!soundFolder.exists()) {
            soundFolder.mkdirs();
        }

        Intent intent = new Intent(SplashScreen.this, CameraActivity.class);
        startActivity(intent);
        finish();
    }
}
