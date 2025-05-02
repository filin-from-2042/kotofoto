package com.kotophoto;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;
import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Global {
    /**
     * Возвращает абсолютный путь к публичной директории приложения
     * @param context
     * @return
     */
    public static String getAppPublicFolder(Context context)
    {
        return Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.folder_name);
    }

    /**
     * Устанавливает текущим звуком первый встроенный звук
     * @param context
     */
    public static void resetSelectedSoundToDefault(Context context)
    {
        SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
        AssetManager assetManager = context.getAssets();
        try {
            String[] soundsPaths = assetManager.list(context.getString(R.string.SOUNDS_FOLDER));
            if(soundsPaths!=null && soundsPaths.length>0) {
                String defaultSound = soundsPaths[0];
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(context.getString(R.string.selected_sound_key), defaultSound);
                editor.commit();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Проверяет существование звукового файла и возвращает true если такой файл уже существует, false если нет.
     * @param context
     * @param checkedFilename
     * @return
     */
    public static boolean checkGlobalFileExistence(Context context, String checkedFilename)
    {
        try {
            // звуки в assets
            AssetManager assets = context.getAssets();
            String assetsSoundFolder = context.getString(R.string.SOUNDS_FOLDER);
            String[] internalSounds = assets.list(assetsSoundFolder);
            if(internalSounds!=null && internalSounds.length>0) {
                for (String soundFileName : internalSounds) {

                    if(getNameFromFilename(soundFileName).equals(checkedFilename)) return true;
                }
            }
            // звуки в публичной директории приложения
            String[] publicSounds = Global.getPublicSoundsList(context);
            for (String sound : publicSounds)
            {
                if(getNameFromFilename(sound).equals(checkedFilename)) return true;
            }

            return false;
        } catch (Exception ex){
            ex.printStackTrace();
            return true;
        }
    }

    /**
     * Возвращает массив с названиями файлов, находящихся в публичной директории приложения, включая расширение
     * @param context
     * @return
     */
    public static String[] getPublicSoundsList(Context context)
    {
        File publicSoundFolder = new File(Global.getAppPublicFolder(context));
        return publicSoundFolder.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if(s.contains(".3gp") || s.contains(".wav")) return true;
                else return false;
            }
        });
    }

    /**
     * Возвращает имя файла из переданного полного имени файла с расширением
     * @param filename
     * @return
     */
    public static String getNameFromFilename(String filename)
    {
        String name = filename;
        Matcher match = Pattern.compile("(.+)\\.([^\\.]+)$").matcher(filename);
        if (match.find( )) {
            name = match.group(1);
        }
        return name;
    }
}
