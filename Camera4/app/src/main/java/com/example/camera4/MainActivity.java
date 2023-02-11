package com.example.camera4;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Rational;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import java.io.ByteArrayOutputStream;

import android.Manifest;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    protected int REQUEST_CODE_PERMISSIONS = 101;
    protected String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};


    TextureView textureView;

    protected static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        textureView = (TextureView) findViewById(R.id.view_finder);

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {// мб тут косяк
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
    }

//    protected final ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
//            new ActivityResultContracts.TakePicture(),
//            result -> {
//                if (result.getResultCode() == RESULT_OK) {
//                    Bundle extras = result.getData().getExtras();
//                    Bitmap imageBitmap = (Bitmap) extras.get("data");
//                    // ...
//                }
//            }
//    );

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            // todo startActivityForResult is not recommended
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//            cameraResultLauncher.launch(takePictureIntent);
            String msg = "bebra bebra";
            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Bitmap photo = (Bitmap) data.getExtras().get("data"); // this is the picture taken

            // todo here or later image resolution is always 187x250

            // Convert the image to a JPEG format
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Send the image to the server
            sendImageToServer(imageBytes);
        }
    }

    protected void sendImageToServer(final byte[] imageBytes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // put your server IP and Port here
                    URL url = new URL("http://<IP>:<Port>/");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "image/jpeg");
                    connection.setRequestProperty("Content-Length", String.valueOf(imageBytes.length));

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(imageBytes);
                    outputStream.flush();
                    outputStream.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        inputStream.close();
                        Log.d("Response", response.toString());
                    } else {
                        Log.e("Response Error", "Failed with response code: " + responseCode);
                    }
                } catch (IOException e) {
                    Log.e("Error", e.getMessage(), e);
                }
            }
        }).start();
    }

    protected void startCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight());

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                }
        );
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).
                setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {// мб тут косяк
            @Override
            public void onClick(View view) {


            }
        });
        CameraX.bindToLifecycle(this, preview, imgCap);

    }

    protected void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cx = w / 2f;
        float cy = h / 2f;

        // int rotationDgr;
        int rotation = (int) textureView.getRotation();

        // отсутвует часть кода, вомзожны проблемы

        mx.postRotate((float) rotation, cx, cy);
        textureView.setTransform(mx);
    }

    protected boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}


