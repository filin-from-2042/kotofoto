package com.kotophoto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RecordSoundDialogFragment extends DialogFragment implements Button.OnClickListener {


    private static final String ERROR_FRAGMENT_DIALOG = "errorDialog";
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    boolean isRecording = false;
    boolean isPlaying = false;
    // метка отмены воспроизведения. Нужна для отмены воспроизведения, т.к. после cdTimer.cancel() таймер выключается не сразу
    boolean isPlayingCanceled = false;
    String tmpFilePath;

    Button recordBtn;
    Button playBtn;
    Button removeBtn;

    TextView recordTimer;
    TextView playTimer;
    CountDownTimer cdTimer;

    RelativeLayout recordLayout;
    RelativeLayout playerLayout;

    AlertDialog alertDialog;
    EditText recordNameET;

    Button positiveBtn;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_fragment_record_sound,null);

        recordLayout = v.findViewById(R.id.recordL);
        recordBtn = v.findViewById(R.id.recordBtn);
        recordTimer= v.findViewById(R.id.timerRecordTV);

        playerLayout= v.findViewById(R.id.playL);
        playBtn = v.findViewById(R.id.playBtn);
        playTimer= v.findViewById(R.id.timerPlayTV);
        removeBtn = v.findViewById(R.id.removeBtn);

        recordBtn.setOnClickListener(this);
        playBtn.setOnClickListener(this);
        removeBtn.setOnClickListener(this);

        recordNameET = v.findViewById(R.id.newRecordName);
        recordNameET.setText(getFreeFileName());

        tmpFilePath = getActivity().getExternalCacheDir().getAbsolutePath()+"/"+getString(R.string.record_sound_tmp_filename)+".3gp";

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.record_sound_dialog_title)
                .setView(v)
                .setNegativeButton(R.string.record_sound_dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(isRecording) stopRecording();
                        if(isPlaying) stopPlaying();
                        removeTempFile();
                        RecordSoundDialogFragment.this.getDialog().cancel();
                    }
                })
                .setPositiveButton(R.string.record_sound_dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // TODO проверка имени файла на совпадение с именами существующими звуков
                        if(isRecording) stopRecording();
                        if(isPlaying) stopPlaying();
                        File tmpFile = new File(tmpFilePath);
                        if(tmpFile.exists())
                        {
                            String newFilePath = Global.getAppPublicFolder(getContext())+"/" + recordNameET.getText() + ".3gp";
                            tmpFile.renameTo(new File(newFilePath));
                            Fragment fragment = getTargetFragment();
                            Intent intent = new Intent();
                            intent.putExtra(String.valueOf(getTargetRequestCode()), newFilePath);
                            if (fragment != null)
                                fragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
                        }
                    }
                });
        alertDialog = builder.create();

        return  alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        positiveBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        recordNameET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(positiveBtn!=null) {
                    if (Global.checkGlobalFileExistence(getContext(), recordNameET.getText().toString())) {
                        positiveBtn.setEnabled(false);
                        recordNameET.setError(getString(R.string.record_sound_file_exist_message));
                    }else positiveBtn.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.recordBtn:{
                if(isRecording)
                {
                    stopRecording();

                    recordLayout.setVisibility(View.GONE);
                    playerLayout.setVisibility(View.VISIBLE);
                    removeBtn.setVisibility(View.VISIBLE);
                }
                else startRecording();
                break;
            }
            case R.id.playBtn:{
                    if(isPlaying) stopPlaying();
                    else startPlaying();
                    break;
                }
            case R.id.removeBtn:{
                stopPlaying();
                removeTempFile();
                recordTimer.setText("00:00");
                recordLayout.setVisibility(View.VISIBLE);
                playerLayout.setVisibility(View.GONE);
                removeBtn.setVisibility(View.GONE);
                break;
            }
        }
    }

    public void onStop() {
        super.onStop();
        stopRecording();
        stopPlaying();
    }

    private void startRecording() {
        isRecording = true;

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // TODO разобраться с другими форматами записей, желателен wav
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(tmpFilePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        cdTimer = new CountDownTimer(30000,1000) {
            @Override
            public void onTick(long l) {
                recordTimer.setText(String.format(Locale.US,"00:%02d",  ((30000-l) / 1000)));
            }

            @Override
            public void onFinish() {
            }
        };
        cdTimer.start();

        recordBtn.setBackground(getContext().getDrawable(R.drawable.ic_stop_sl));
    }

    private void stopRecording() {
        isRecording = false;

        if(cdTimer!=null) cdTimer.cancel();

        recordBtn.setBackground(getContext().getDrawable(R.drawable.ic_record_sl));

        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private void startPlaying() {
        isPlaying = true;
        isPlayingCanceled = false;
        playBtn.setBackground(getContext().getDrawable(R.drawable.ic_stop_sl));
        //TODO у MedaiPlayer есть механиз таймера воспроизведения, нужно переделать на него
        player = new MediaPlayer();
        AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build();
        player.setAudioAttributes(aa);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
        try {
            player.setDataSource(tmpFilePath);
            player.prepare();
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        cdTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long l) {
                if(!isPlayingCanceled) playTimer.setText(String.format(Locale.US,"00:%02d",  ((30000-l) / 1000)));
                else playTimer.setText("00:00");
            }

            @Override
            public void onFinish() {
                stopPlaying();
            }
        };
        cdTimer.start();
    }

    private void stopPlaying() {
        isPlaying = false;
        isPlayingCanceled = true;
        playBtn.setBackground(getContext().getDrawable(R.drawable.ic_play_sl));
        if(cdTimer!=null) cdTimer.cancel();
        playTimer.setText("00:00");
        //TODO Плеер при остановке воспроизведения не пересоздавать каждый раз, а использовать reset
        if(player!=null){
            player.release();
            player = null;
        }
    }

    private void removeTempFile()
    {
        File file = new File(tmpFilePath);
        if(file.exists()) file.delete();
    }

    /**
     * Генерирует новое имя файла звукозаписи, с учетом уже существующих
     * @return
     */
    private String getFreeFileName()
    {
        File dir = new File(Global.getAppPublicFolder(getContext()));
        final String soundFilesPattern = getString(R.string.record_sound_filename_pattern);

        String[] files = dir.list(
                new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.startsWith(soundFilesPattern);
                    }
                });
        int counter = 0;
        for (String file:files)
        {
            Matcher match = Pattern.compile(soundFilesPattern+"-(\\d+)\\.{1}[^\\.]+$").matcher(file);
            if (match.find())
            {
                String matchStr =match.group(1);
                int fileNumber = Integer.parseInt(matchStr);
                if(fileNumber>counter) counter = fileNumber;
            }
        }
        counter++;
        return getString(R.string.record_sound_filename_pattern)+"-"+String.valueOf(counter);
    }

}