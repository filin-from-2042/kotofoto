package com.kotophoto;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraFragment extends Fragment implements View.OnClickListener{

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_AUDIO_PERMISSION = 3;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String TAG = "CameraFragment";

    ResizedTextureView mTextureView;
    ImageView mPhotoPreview;
    private String mLastPhotoFile;
    private Sound mSound;
    private Camera2Worker mWorker;



    public static CameraFragment newInstance()
    {
        return new CameraFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        mTextureView = v.findViewById(R.id.textureView);
        mPhotoPreview = v.findViewById(R.id.photoPreviewIV);
        v.findViewById(R.id.capture).setOnClickListener(this);
        v.findViewById(R.id.photoPreviewIV).setOnClickListener(this);
        v.findViewById(R.id.playBtn).setOnClickListener(this);
        v.findViewById(R.id.soundSettingBtn).setOnClickListener(this);
        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        removeLastPhotoPW();
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
        boolean apRequested = sp.getBoolean(getString(R.string.audio_permission_requested_key),false);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && !apRequested)
        {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        }
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
            return;
        }

        mWorker = new Camera2Worker(getContext(),mTextureView);
        mWorker.setCapturedListener(new Camera2Worker.CapturedListener() {
            @Override
            public void onWorkerCaptureComplete(@NonNull File mFile) {
                showToast("Saved: " + String.valueOf(mFile));
                mLastPhotoFile = mFile.getPath();
                renderLastPhotoPreview();
            }
        });
        mWorker.start();

        SoundPool.Builder sBuilder = new SoundPool.Builder();
        sBuilder.setMaxStreams(1);
        sBuilder.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
        SoundPool soundPool = sBuilder.build();
        mSound = new Sound(getContext(),soundPool);
    }

    @Override
    public void onPause() {
        mWorker.stop();
        super.onPause();
    }

    @Override
    public void onStop() {
        if(mLastPhotoFile!=null)
        {
            SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(getString(R.string.last_captured_photo_key), mLastPhotoFile);
            editor.commit();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        removeLastPhotoPW();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key),Context.MODE_PRIVATE);
        mLastPhotoFile = sp.getString(getString(R.string.last_captured_photo_key),null);
        renderLastPhotoPreview();
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_camera_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_storage_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else if(requestCode == REQUEST_AUDIO_PERMISSION) {

            // признак, что запрос на доступ к микрофону уже делалася
            try {
                SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(getString(R.string.audio_permission_requested_key), true);
                editor.commit();
            } catch (Exception ex){
                ex.printStackTrace();
            }

            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                // shouldShowRequestPermissionRationale - показывать окно с пояснением, если пользователь не дал прав
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO))
                {
                    ConfirmationDialog cd = ConfirmationDialog.newInstance(REQUEST_AUDIO_PERMISSION);
                    cd.show(getChildFragmentManager(), FRAGMENT_DIALOG);
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capture: {
                mWorker.capture();

                break;
            }
            case R.id.photoPreviewIV:{
                File file  = new File(mLastPhotoFile);
                Intent intent = new Intent(Intent.ACTION_VIEW)//
                        .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                        android.support.v4.content.FileProvider.getUriForFile(getActivity(),getActivity().getPackageName() + ".provider", file) : Uri.fromFile(file),
                                "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(intent);
                break;
            }
            case R.id.soundSettingBtn:{
                Intent intent = new Intent(getActivity(),SoundSettingActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.playBtn:{
                mSound.play();
                break;
            }
        }
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog cd = ConfirmationDialog.newInstance(REQUEST_CAMERA_PERMISSION);
            cd.show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ConfirmationDialog cd = ConfirmationDialog.newInstance(REQUEST_STORAGE_PERMISSION);
            cd.show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    private void renderLastPhotoPreview()
    {
        if(mLastPhotoFile!=null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPhotoPreview.setImageBitmap(PictureUtils.getRotatedPictureBitmap(mLastPhotoFile, 100, 100));
                }
            });
        }
    }

    public void removeLastPhotoPW()
    {
        SharedPreferences sp = getActivity().getSharedPreferences(getString(R.string.kotophoto_sp_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(getString(R.string.last_captured_photo_key));
        editor.commit();
    }

    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        private static final String ARG_PERMISSION_TYPE = "permissionType";

        public static ConfirmationDialog newInstance(int permissionType)
        {
            ConfirmationDialog cd = new ConfirmationDialog();
            Bundle args = new Bundle();
            args.putSerializable(ARG_PERMISSION_TYPE,permissionType);
            cd.setArguments(args);
            return cd;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
            int permissionType = getArguments().getInt(ARG_PERMISSION_TYPE);
            if(permissionType==REQUEST_CAMERA_PERMISSION){
                adBuilder = adBuilder
                        .setMessage(R.string.request_camera_permission)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                        REQUEST_CAMERA_PERMISSION);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Activity activity = parent.getActivity();
                                        if (activity != null) {
                                            activity.finish();
                                        }
                                    }
                                });
            } else if (permissionType==REQUEST_STORAGE_PERMISSION){
                adBuilder = adBuilder
                        .setMessage(R.string.request_storage_permission)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                parent.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_STORAGE_PERMISSION);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Activity activity = parent.getActivity();
                                        if (activity != null) {
                                            activity.finish();
                                        }
                                    }
                                });
            } else if(permissionType==REQUEST_AUDIO_PERMISSION){
                adBuilder = adBuilder
                        .setMessage(R.string.request_audio_permission)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                parent.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                                        REQUEST_AUDIO_PERMISSION);
                            }
                        });
            }
            return adBuilder.create();
        }
    }

}