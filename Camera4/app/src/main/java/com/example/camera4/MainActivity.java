package com.example.camera4;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

// Background calculations can mess up with snackbar bruuuuuuuh

// TODO server connection settings
// TODO permissions
// TODO package name
// TODO project refactoring
// TODO button feedback
// TODO E/example.camera: open libmigui.so failed! dlopen - dlopen failed: library "libmigui.so" not found
// TODO delete file in destructor?
// TODO save picture from camera to MediaStore

// TODO photo picker rotation

// TODO final animation


// create a file, get its uri, if nothing saved there -> delete file by its uri, if success, good

// is_pending for api >= 29, MediaStore.Images.Media.insertImage for lower

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

        /*

        cameraButton.setOnClickListener(view -> {
            if (!isImageSent.get()) {
                showSnackBar("зачилься другалёк, картинка отправляется");
                return;
            }
            isImageSent.set(false);
//            if (photoUri == null) {
//                photoUri = FileProvider.getUriForFile(
//                        this,
//                        BuildConfig.APPLICATION_ID + ".provider",
//                        generateTempPhotoFile()
//                );
//            }
//            cameraResultLauncher.launch(photoUri);
        });

        cameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {

                        photoUri = savePhotoToMediaStore();


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

         */

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


        ActivityResultLauncher<Uri> bebra = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result) {

                // Insert the picture into the MediaStore
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, "My Image");
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);

                    ContentResolver resolver = getContentResolver();
                    imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    try (OutputStream os = resolver.openOutputStream(imageUri)) {
                        // Save the image data to the output stream
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(imageUri, values, null, null);

                } else {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = "IMG_" + timeStamp + ".jpg";
                    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    File imageFile = new File(storageDir, fileName);

                    try (OutputStream os = new FileOutputStream(imageFile)) {
                        // Save the image data to the output stream
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    imageUri = Uri.fromFile(imageFile);

                    // Insert the picture into the MediaStore
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                }
            }
        });

        try {
            File photka = createImageFile();
            imageUri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photka
                );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        bebra.launch(imageUri);

    }








    protected Uri imageUri;












    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
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
