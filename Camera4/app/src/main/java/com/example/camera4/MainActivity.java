package com.example.camera4;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import java.io.ByteArrayOutputStream;

// TODO Activity -> FragmentActivity?

public class MainActivity extends AppCompatActivity {

    protected ImageButton camera_button;
    protected ImageView picture_received;
    ActivityResultLauncher<Intent> camera_result_launcher;

    protected final String APP_TAG = "temp";
    protected String photo_file_name = "photo.jpg";
    File photo_file;

    protected int REQUEST_CODE_PERMISSIONS = 101;

    // TODO external storage is unnecessary?
    protected String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        camera_button = findViewById(R.id.camera_button);
        picture_received = findViewById(R.id.server_received_image);

        // Anonymous class be replaced with a lambda, as OnClickListener is a functional interface
        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    // Create a File reference for future access
                    photo_file = getPhotoFileUri(photo_file_name);

                    Uri fileProvider = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", photo_file);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

                    camera_result_launcher.launch(takePictureIntent);
                    Toast.makeText(MainActivity.this, "bebra bebra", Toast.LENGTH_LONG).show();
                } catch (ActivityNotFoundException e) {
                    Log.e("Error", e.getMessage(), e);
                }
            }
        });

        camera_result_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // by this point we have the camera photo on disk
                        Bitmap photo = BitmapFactory.decodeFile(photo_file.getAbsolutePath());

                        // Convert the image to a JPEG format
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        photo.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                        byte[] image_bytes = byteArrayOutputStream.toByteArray();

                        // Send the image to the server
                        sendImageToServer(image_bytes);

                    } else {
                        Toast.makeText(this, "Error while taking picture", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Returns the File for a photo stored on disk given the fileName
    public File getPhotoFileUri(String fileName) {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(APP_TAG, "failed to create directory");
        }

        // Return the file target for the photo based on filename
        return new File(mediaStorageDir.getPath() + File.separator + fileName);
    }

    protected void sendImageToServer(final byte[] imageBytes) {
        new Thread(() -> {
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
                    connection.disconnect();
                    Log.d("Response", response.toString());
                } else {
                    Log.e("Response Error", "Failed with response code: " + responseCode);
                }
            } catch (IOException e) {
                Log.e("Error", e.getMessage(), e);
            }
        }).start();
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
