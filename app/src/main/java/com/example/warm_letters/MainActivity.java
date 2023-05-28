package com.example.warm_letters;

import android.widget.ImageView;
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

import com.github.penfeizhou.animation.apng.APNGDrawable;
import java.io.File;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

// Background calculations can mess up the snackbar bruuuuuuuh
// OnDestroy is not guaranteed to be called bruuuuuuuuuuuuuuuh

/* TODO: todos here. Least -> most important

TODO server connection settings
TODO put colors to colors.xml
TODO background tasks interruption in onPause
TODO implement callback when image sent to server instead of using AtomicBool
TODO package name
TODO change button pictures
TODO confirmation window on exit
TODO camera permission if API < 26 (works on 26, maybe even lower, not 24 tho)
TODO make minsdk 27 lol

TODO final animation

 */

public class MainActivity extends AppCompatActivity {

    protected View mainView;
    protected ImageButton cameraButton;
    protected ImageButton galleryButton;
    protected TextView mainText;
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
        mainText = findViewById(R.id.mainText);

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
                });
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
