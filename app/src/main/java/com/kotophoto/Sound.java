package com.kotophoto;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.SoundPool;
import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sound {

    private String soundName;
    private String fileName;
    private SoundPool soundPool;
    private int soundPoolId;
    private Integer playingStreamId;
    /**
     * признак пользовательского звука
      */
    private boolean isCustom = true;

    public String getSoundName() {
        return soundName;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isCustom() {
        return isCustom;
    }

    /**
     * Звук инициализируертся по текущему в настройках
     * @param context
     * @param iSoundPool
     */
    public Sound(Context context, SoundPool iSoundPool){
        SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
        String fileName = sp.getString(context.getString(R.string.selected_sound_key),"");
        if(!fileName.isEmpty()) init(context,fileName, iSoundPool);
    }

    public Sound(Context context, String fileName, SoundPool iSoundPool) {
        init(context,fileName, iSoundPool);
    }

    /**
     * Инициализация звук по имени файла
     * @param context
     * @param iFileName
     * @param iSoundPool
     */
    private void init(Context context, String iFileName, SoundPool iSoundPool)
    {
        fileName = iFileName;
        // имя звука
        soundName = Global.getNameFromFilename(fileName);
        if(soundName==null) soundName = fileName;

        soundPool = iSoundPool;
        try {
            // если звук в assets, значит внутренний
            AssetManager assetManager = context.getAssets();
            String[] internalSounds = assetManager.list(context.getString(R.string.SOUNDS_FOLDER));
            if(internalSounds!=null && internalSounds.length>0)
            {
                for (String soundPath : internalSounds) {
                    if(soundPath.equals(fileName)) isCustom = false;
                }
            }
            if(!isCustom) {
                AssetFileDescriptor descriptor = context.getAssets().openFd(getFullPath(context));
                soundPoolId = soundPool.load(descriptor, 1);
            } else {
                soundPoolId = soundPool.load(getFullPath(context),1);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Возвращает абсолютный путь к звуку
     * @param context
     * @return
     */
    @Nullable
    public String getFullPath(Context context)
    {
        String path;
        if (isCustom) {
            path = Global.getAppPublicFolder(context) + "/" + fileName;
        } else {
            path = context.getString(R.string.SOUNDS_FOLDER) + "/" + fileName;
        }
        return path;
    }

    public void play()
    {
        playingStreamId=soundPool.play(soundPoolId,1,1,1,0,1);
    }

    public void stop()
    {
        if(playingStreamId!=null) soundPool.stop(playingStreamId);
    }

}
