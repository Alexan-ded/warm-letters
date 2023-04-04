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

// TODO photo picker rotation

// TODO final animation

public class MainActivity extends AppCompatActivity {

    protected View mainView;
    protected ImageButton cameraButton;
    protected ImageButton galleryButton;
    protected ImageView pictureReceived;
    ActivityResultLauncher<Uri> cameraResultLauncher;
    ActivityResultLauncher<PickVisualMediaRequest> photoPickerResultLauncher;
    protected File imageFile;
    protected AtomicBoolean isImageSent = new AtomicBoolean(true);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainView = findViewById(android.R.id.content);
        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);
        pictureReceived = findViewById(R.id.pictureReceived);

        cameraButton.setOnClickListener(view -> {
            if (!isImageSent.get()) {
                showSnackBar("зачилься другалёк, картинка отправляется");
                return;
            }
            isImageSent.set(false);
            imageFile = getPhotoFileUri();
            Uri fileProvider = FileProvider.getUriForFile(
                    MainActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    imageFile
            );
            cameraResultLauncher.launch(fileProvider);
        });

        cameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        ImageProcess imageProcess = new ImageProcess(this,
                                mainView,
                                isImageSent,
                                imageFile.getAbsolutePath()
                        );
                        imageProcess.processImageFromCamera();
                    } else {
                        showSnackBar("No photo taken");
                        isImageSent.set(true);
                    }
                }
        );

        galleryButton.setOnClickListener(view -> {
            if (!isImageSent.get()) {
                showSnackBar("зачилься другалёк, картинка отправляется");
                return;
            }
            isImageSent.set(false);
            photoPickerResultLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()
            );
        });

        photoPickerResultLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    // Callback is invoked after the user selects a media item or closes the
                    // photo picker.
                    if (uri != null) {
                        ImageProcess imageProcess = new ImageProcess(
                                this,
                                mainView,
                                isImageSent,
                                uri
                        );
                        imageProcess.processImageFromGallery();
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                        showSnackBar("No image selected");
                        isImageSent.set(true);
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
