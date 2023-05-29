package com.example.warm_letters;

import android.widget.Button;
import android.widget.TextView;
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

import java.io.File;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

// Background calculations can mess up the snackbar bruuuuuuuh
// OnDestroy is not guaranteed to be called bruuuuuuuuuuuuuuuh

/* TODO: todos here. Least -> most important

TODO server connection settings
TODO put colors to colors.xml
TODO background tasks interruption in onPause
TODO implement callback when image sent to server instead of using AtomicBool
TODO change button pictures
TODO confirmation window on exit
TODO camera permission if API < 26 (works on 26, maybe even lower, not 24 tho)
TODO make minsdk 27 lol

TODO back button
TODO open with button

 */

public class MainActivity extends AppCompatActivity {

    protected View mainView;
    protected TextView mainText;
    protected ImageButton cameraButton;
    protected ImageButton galleryButton;
    protected Button openFileButton;
    protected ActivityResultLauncher<Uri> cameraResultLauncher;
    protected ActivityResultLauncher<PickVisualMediaRequest> photoPickerResultLauncher;
    protected ActivityResultLauncher<String> filePickerResultLauncher;
    protected Uri photoUri = null;
    protected AtomicBoolean isImageSent = new AtomicBoolean(true);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainView = findViewById(android.R.id.content);
        mainText = findViewById(R.id.mainText);
        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);
        openFileButton = findViewById(R.id.openFileButton);

        cameraButton.setOnClickListener(view -> {
            if (!isImageSent.get()) {
                showSnackBar("Image is still being processed");
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
                        ImageProcess imageProcess = new ImageProcess(
                                this,
                                mainView,
                                isImageSent,
                                photoUri
                        );
                        imageProcess.processImage();
                        // TODO callback when the data from the server is retrieved
                        // TODO ShowActivity on callback
                        // TODO also delete temp photo here
                    } else {
                        showSnackBar("No photo taken");
                        isImageSent.set(true);
                    }
                }
        );

        galleryButton.setOnClickListener(view -> {
            if (!isImageSent.get()) {
                showSnackBar("Image is still being processed");
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
                        imageProcess.processImage();
                        // TODO callback when the data from the server is retrieved
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                        showSnackBar("No image selected");
                        isImageSent.set(true);
                    }
                }
        );

        openFileButton.setOnClickListener(view -> {
            // TODO ("application/bebr")
            filePickerResultLauncher.launch("*/*");

        });

        filePickerResultLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        Log.d("FilePicker", "No file selected");
                        showSnackBar("No file selected");
                        return;
                    }
                    File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    File file = new File(mediaStorageDir, "animation_.png");
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        FunctionClass.createFile(this, inputStream, file);
                    } catch (FileNotFoundException e) {
                        showSnackBar("Error: File not found");
                        e.printStackTrace();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getContentResolver().delete(photoUri, null, null) == 0) {
            Log.e("FileDeletion", "Failed to delete photo");
        } else {
            Log.e("FileDeletion", "Photo deleted");
        }
    }

    protected Uri createPhotoUri() {
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photoFile =
                new File(mediaStorageDir.getPath() + File.separator + "photo.jpg");
        return FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                photoFile
        );
    }

    protected void showSnackBar(String message) {
        FunctionClass.showSnackBar(mainView, message);
    }
}
