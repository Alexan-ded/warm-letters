package com.example.camera4;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import java.io.File;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

// Background calculations can mess up with snackbar bruuuuuuuh

// TODO server connection settings
// TODO permissions
// TODO package name
// TODO project refactoring
// TODO button feedback
// TODO naming refactoring

// TODO photo picker rotation

// TODO final animation

public class MainActivity extends AppCompatActivity {

    protected View mainView;
    protected ImageButton camera_button;
    protected ImageButton gallery_button;
    protected ImageView picture_received;
    ActivityResultLauncher<Uri> camera_result_launcher;
    ActivityResultLauncher<PickVisualMediaRequest> gallery_result_launcher;
    protected File image_file;
    protected AtomicBoolean is_image_sent = new AtomicBoolean(true);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainView = findViewById(android.R.id.content);
        camera_button = findViewById(R.id.camera_button);
        gallery_button = findViewById(R.id.gallery_button);
        picture_received = findViewById(R.id.server_received_image);

        camera_button.setOnClickListener(view -> {
            if (!is_image_sent.get()) {
                showSnackBar("зачилься другалёк, картинка отправляется");
                return;
            }
            is_image_sent.set(false);
            image_file = getPhotoFileUri();
            Uri fileProvider = FileProvider.getUriForFile(
                    MainActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    image_file
            );
            camera_result_launcher.launch(fileProvider);
        });

        camera_result_launcher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        ImageProcess process = new ImageProcess(this,
                                mainView,
                                is_image_sent,
                                image_file.getAbsolutePath()
                        );
                        process.processImageFromCamera();
                    } else {
                        showSnackBar("No photo taken");
                        is_image_sent.set(true);
                    }
                }
        );

        gallery_button.setOnClickListener(view -> {
            if (!is_image_sent.get()) {
                showSnackBar("зачилься другалёк, картинка отправляется");
                return;
            }
            is_image_sent.set(false);
            gallery_result_launcher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()
            );
        });

        gallery_result_launcher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    // Callback is invoked after the user selects a media item or closes the
                    // photo picker.
                    if (uri != null) {
                        Log.d("PhotoPicker", "Selected URI: " + uri);
                        ImageProcess process = new ImageProcess(
                                this,
                                mainView,
                                is_image_sent,
                                uri
                        );
                        process.processImageFromGallery();
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                        showSnackBar("No image selected");
                        is_image_sent.set(true);
                    }
                });
    }

    // Returns the File for a photo stored on disk given the fileName
    protected File getPhotoFileUri() {
        final String APP_TAG = "temp";
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                APP_TAG
        );

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(APP_TAG, "failed to create directory");
            showSnackBar("failed to create directory");
        }

        // Return the file target for the photo based on filename
        return new File(mediaStorageDir.getPath() + File.separator + "photo.jpg");
    }

    protected void showSnackBar(String message) {
        FunctionClass.showSnackBar(mainView, message);
    }
}
