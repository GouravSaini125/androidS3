package com.example.s3test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    final private String TAG = "myTag";
    final private String SECRET = "XXXXXXXXXXXXXXXXXXXX";
    final private String KEY = "XXXXXXXXXXXXXXXXXXXXX";
    private static int RESULT_LOAD = 1;
    private static int REQUEST_DIRECTORY = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AWSMobileClient.getInstance().initialize(this).execute();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);

        findViewById(R.id.uFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });

        findViewById(R.id.uFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFolder();
            }
        });

    }

    private void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_DIRECTORY);
    }

    public void selectFile() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            assert selectedImage != null;
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String path = cursor.getString(columnIndex);
            cursor.close();

            File file = new File(path);

            Log.d(TAG, path);

            uploadFile(file, null);

        }

        if (requestCode == REQUEST_DIRECTORY && resultCode == RESULT_OK && null != data) {
            String FileName = data.getData().getLastPathSegment();
            Log.d(TAG, FileName);
            String[] arr = FileName.split(":");
            Log.d(TAG, arr[0]);

            String path;
            if (arr[0].equals("primary")) {
                path = Environment.getExternalStorageDirectory().toString() + "/" + arr[1];
            } else {
                path = "/storage/" + arr[0] + "/" + arr[1];
            }

            String[] splits = arr[1].split("/");
            String folder = splits[splits.length - 1];

            Log.d(TAG, "Path: " + path);
            File directory = new File(path);
            File[] files = directory.listFiles();
            Log.d(TAG, "Size: " + files.length);
            for (int i = 0; i < files.length; i++) {
                Log.d(TAG, "FileName:" + files[i].getName());
                File file = new File(files[i].getAbsolutePath());
                uploadFile(file, folder);
            }

        }

    }


    public void uploadFile(File file, String folder) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(KEY, SECRET);
        AmazonS3Client s3Client = new AmazonS3Client(credentials);

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .build();

        String fold;
        if (folder != null) {
            fold = "ffmpeg-uploads/" + folder + "/";
        } else {
            fold = "ffmpeg-uploads/";
        }

        TransferObserver uploadObserver =
                transferUtility.upload(fold + file.getName(), file);

        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Log.d(TAG, "Uploaded!!!!");
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                Log.d(TAG, percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "Failed!!!!" + id + ex.getMessage());

            }

        });
        //if above not triggered
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            Log.d(TAG, "Uploaded!!!!");
        }

    }


}
