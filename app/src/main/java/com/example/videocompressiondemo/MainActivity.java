package com.example.videocompressiondemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.sql.SQLOutput;

public class MainActivity extends AppCompatActivity {
    static final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";
    private static final String TAG = "MainActivity";
    private static final int REQUEST_VIDEO_COMPRESS = 0x01;
    private static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    private VideoView mVideo;
    Uri uri;
    public Compressor com;
    String mFinalPath;

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        com = new Compressor(this);

        Intent extraIntent = getIntent();

        requestPermission();
        Button galleryButton = findViewById(R.id.galleryButton);
        if (galleryButton != null) {
            galleryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickFromGallery();
                }
            });
        }

        Button recordButton = findViewById(R.id.cameraButton);
        if (recordButton != null) {
            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openVideoCapture();
                }
            });
        }


        mVideo = findViewById(R.id.timeLine);
        if (extraIntent.getParcelableExtra("imageUri") != null) {
            uri = extraIntent.getParcelableExtra("imageUri");
            System.out.println("----uri----"+uri.getPath());
            mVideo.setVisibility(View.VISIBLE);
            mVideo.setVideoURI(uri);
            mVideo.requestFocus();
            mVideo.start();
        }


    }

    private void openVideoCapture() {
//        Intent videoCapture = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//        startActivityForResult(videoCapture, REQUEST_VIDEO_TRIMMER);

        final int durationLimit = 59;
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);
        startActivityForResult(intent, REQUEST_VIDEO_COMPRESS);
    }

    private void pickFromGallery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getString(R.string.permission_read_storage_rationale), REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            Intent intent = new Intent();
            intent.setTypeAndNormalize("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.label_select_video)), REQUEST_VIDEO_COMPRESS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_COMPRESS) {
                final Uri selectedUri = data.getData();
               String path = FileUtils.getPath(this, selectedUri);
                if (selectedUri != null) {
                    Toast.makeText(this, selectedUri.getPath(), Toast.LENGTH_SHORT).show();
                    System.out.println("------path------"+path);
                    /* to do compression here */
                    final String [] cmd = {"-y", "-i", ""+path+"", "-s", "640x352", "-r", "25",
                            "-vcodec", "mpeg4", "-b:v", "150k", "-b:a", "96k",
                            "-ac", "2", "-ar", "44100", "-aspect", "16:9",""+getDestinationPath()+""};
                    com.loadBinary(new InitListener() {
                        @Override
                        public void onLoadSuccess() {
                            com.execCommand(cmd, new CompressListener() {
                                @Override
                                public void onExecSuccess(String message) {
                                    Log.i("----Success-----",message);
                                    Toast.makeText(MainActivity.this, "Compressed Successfully", Toast.LENGTH_SHORT).show();
                                    mVideo.setVisibility(View.VISIBLE);
                                    mVideo.setVideoURI(Uri.parse(mFinalPath));
                                    mVideo.requestFocus();
                                    mVideo.start();
                                }

                                @Override
                                public void onExecFail(String reason) {
                                    Log.i("----Fail-----",reason);
                                }

                                @Override
                                public void onExecProgress(String message) {
                                    Log.i("-----progress----",message);
                                }
                            });

                        }

                        @Override
                        public void onLoadFail(String reason) {

                        }
                    });


                } else {
                    Toast.makeText(MainActivity.this, R.string.toast_cannot_retrieve_selected_video, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    /**
     * Requests given permission.
     * If the permission has been denied previously, a Dialog will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestPermission(final String permission, String rationale, final int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.permission_title_rationale));
            builder.setMessage(rationale);
            builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
                }
            });
            builder.setNegativeButton(getString(R.string.label_cancel), null);
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private String getDestinationPath() {
        if (mFinalPath == null) {
            File folder = Environment.getExternalStorageDirectory();
            mFinalPath = folder.getPath() + File.separator;
            mFinalPath =mFinalPath+"output.mp4";
            Log.d(TAG, "Using default path " + mFinalPath);
        }
        return mFinalPath;
    }

}
