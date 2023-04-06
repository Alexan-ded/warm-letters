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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

// Background calculations can mess up with snackbar bruuuuuuuh

// TODO server connection settings
// TODO package name
// TODO project refactoring
// TODO button feedback
// TODO E/example.camera: open libmigui.so failed! dlopen - dlopen failed: library "libmigui.so" not found
// TODO delete file in destructor? (create temp file)

// TODO photo picker rotation

// TODO final animation

public class MainActivity extends AppCompatActivity {

    protected View mainView;
    protected ImageButton cameraButton;
    protected ImageButton galleryButton;
    protected ImageView pictureReceived;
    protected ActivityResultLauncher<Uri> cameraResultLauncher;
    protected ActivityResultLauncher<PickVisualMediaRequest> photoPickerResultLauncher;
    protected Uri photoUri = null;
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
            if (photoUri == null) {
                photoUri = createPhotoUri();
            }
            cameraResultLauncher.launch(photoUri);
        });

        cameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        ImageProcess imageProcess = new ImageProcess(this,
                                mainView,
                                isImageSent,
                                photoUri
                        );
                        imageProcess.processImageFromGallery();
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

    private Uri createPhotoUri() {
        Uri res = null;
        try {
            File photoFile = File.createTempFile(
                    "image",
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
            res = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    photoFile
            );
        } catch (IOException e) {
            e.printStackTrace();
            showSnackBar("Error: Failed to create photo Uri");
        }
        return res;
    }



    // Returns the File for a photo stored on disk given the fileName
    protected File generateTempPhotoFile() {
        final String DIR_NAME = "temp";
        File mediaStorageDir = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                DIR_NAME
        );
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(DIR_NAME, "Error: Failed to create a directory to save the photo");
            showSnackBar("Error: Failed to create a directory to save the photo");
        }
        return new File(mediaStorageDir.getPath() + File.separator + "photo.jpg");
    }

    protected void showSnackBar(String message) {
        FunctionClass.showSnackBar(mainView, message);
    }
}
