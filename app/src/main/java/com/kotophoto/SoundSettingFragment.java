package com.kotophoto;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;

public class SoundSettingFragment extends Fragment {

    String TAG = "SoundSettingFragment";
    RadioGroup mRadioGroup;
    MenuItem saveBtn;
    MenuItem removeCustomSound;
    Sound mSelectedSound;
    int RECORD_SOUND_REQUEST_CODE = 1;
    String RECORD_SOUND_DIALOG_TAG = "RSTag";
    public static SoundSettingFragment newInstance()
    {
        return new SoundSettingFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_sound_setting,container,false);

        mRadioGroup = v.findViewById(R.id.soundContainerRG);
        bindSoundList();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.sound_setting_menu, menu);
        saveBtn = menu.findItem(R.id.save);
        removeCustomSound = menu.findItem(R.id.removeCustomSound);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
        {
            MenuItem addBtn = menu.findItem(R.id.addNewSound);
            addBtn.setVisible(false);
        }
        if(mSelectedSound!=null && mSelectedSound.isCustom()) removeCustomSound.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.addNewSound: {
                if(mSelectedSound!=null) mSelectedSound.stop();
                FragmentManager fm = getFragmentManager();
                RecordSoundDialogFragment dialog =  new RecordSoundDialogFragment();
                dialog.setTargetFragment(SoundSettingFragment.this,RECORD_SOUND_REQUEST_CODE);
                if(fm!=null) dialog.show(fm,RECORD_SOUND_DIALOG_TAG);
                return true;
            }
            case R.id.save: {
                if(mSelectedSound!=null) {
                    SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor spEditor = sp.edit();
                    spEditor.putString(getString(R.string.selected_sound_key), mSelectedSound.getFileName());
                    spEditor.apply();
                    getActivity().onBackPressed();
                }
                return true;
            }
            case R.id.removeCustomSound: {
                if(mSelectedSound!=null) {
                    File file = new File(mSelectedSound.getFullPath(getContext()));
                    if(file.exists()) file.delete();

                    Global.resetSelectedSoundToDefault(getContext());
                    bindSoundList();
                    removeCustomSound.setVisible(false);
                    saveBtn.setVisible(false);
                }
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RECORD_SOUND_REQUEST_CODE)
        {
            if(resultCode == Activity.RESULT_OK) bindSoundList();
        }
    }

    /**
     * Заполняет список звуков
     */
    protected void bindSoundList()
    {
        AssetManager assetManager = getActivity().getAssets();
        try {
            mRadioGroup.removeAllViews();
            SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
            String selectedSound = sp.getString(getString(R.string.selected_sound_key),null);
            // встроенные звуки приложения
            String[] internalSounds = assetManager.list(getString(R.string.SOUNDS_FOLDER));
            // пользовательские звуки приложения
            String[] publicSounds = Global.getPublicSoundsList(getContext());

            SoundPool.Builder sBuilder = new SoundPool.Builder();
            sBuilder.setMaxStreams(1);
            sBuilder.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
            SoundPool soundPool = sBuilder.build();

            addSoundsFromArr(publicSounds, soundPool, selectedSound);
            addSoundsFromArr(internalSounds, soundPool, selectedSound);

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Добавляет звуки в виде радиокнопок в RadioGroup view
     * @param soundsArray
     * @param soundPool
     * @param selectedSound
     */
    public void addSoundsFromArr(String[] soundsArray, SoundPool soundPool, String selectedSound)
    {
        if(soundsArray!=null && soundsArray.length>0)
        {
            RadioButton btn;
            for (String soundPath : soundsArray) {
                btn = addRadioBtn(soundPool, soundPath);
                mRadioGroup.addView(btn);
                if(soundPath.equals(selectedSound))
                {
                    mRadioGroup.check(btn.getId());
                    mSelectedSound = new Sound(getContext(),soundPath, soundPool);
                }
            }
        }

    }

    /**
     * Создает и возвращает радиокнопку, соответствующую звуку по переданным параметрам
     * @param soundPool
     * @param soundPath
     */
    private RadioButton addRadioBtn( SoundPool soundPool, String soundPath) {
        RadioButton btn = new RadioButton(getContext());

        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(10, 10, 10, 10);
        btn.setLayoutParams(params);
        btn.setTextSize(16);

        final Sound sound = new Sound(getContext(),soundPath,soundPool);

        btn.setText(sound.getSoundName());
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sound.play();

                mSelectedSound = sound;
                saveBtn.setVisible(true);
                removeCustomSound.setVisible(sound.isCustom());
            }
        });
        return btn;
    }
}