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

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

// TODO Activity -> FragmentActivity?

public class MainActivity extends AppCompatActivity {

    protected ImageButton camera_button;
    protected ImageView picture_received;
    ActivityResultLauncher<Intent> camera_result_launcher;

    protected final String APP_TAG = "temp";
    protected String imageFileName = "photo.jpg";
    protected File ImageFile;

    protected AtomicBoolean is_image_sent = new AtomicBoolean(true);

    protected int REQUEST_CODE_PERMISSIONS = 101;

    // TODO external storage is permission is not needed?
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
                if (!is_image_sent.get()) {
                    Toast.makeText(MainActivity.this, "зачилься другалёк, картинка отправляется", Toast.LENGTH_SHORT).show();
                    return;
                }
                is_image_sent.set(false);
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    // Create a File reference for future access
                    ImageFile = getPhotoFileUri(imageFileName);

                    Uri fileProvider = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", ImageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);

                    camera_result_launcher.launch(takePictureIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e("Error", e.getMessage(), e);
                }
            }
        });

        camera_result_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        new ImageProcess(MainActivity.this, ImageFile.getAbsolutePath(), is_image_sent).processImage();
                    } else {
                        Toast.makeText(this, "Error while taking picture", Toast.LENGTH_SHORT).show();
                        is_image_sent.set(true);
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

    protected boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}

