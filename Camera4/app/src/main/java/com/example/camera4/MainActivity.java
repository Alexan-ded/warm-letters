package com.example.camera4;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO Activity -> FragmentActivity?
// TODO Permissions
// TODO Use TakePicture from ActivityResultContract, avoid reinventing its features

public class MainActivity extends AppCompatActivity {

    protected ImageButton camera_button;
    protected ImageButton gallery_button;
    protected ImageView picture_received;
    ActivityResultLauncher<Intent> camera_result_launcher;
    ActivityResultLauncher<Intent> gallery_result_launcher;

    protected final String APP_TAG = "temp";
    protected String image_file_name = "photo.jpg";
    protected File image_file;

    protected AtomicBoolean is_image_sent = new AtomicBoolean(true);

    protected int REQUEST_CODE_PERMISSIONS = 101;

    // TODO external storage is permission is not needed?
    protected String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        camera_button = findViewById(R.id.camera_button);
        gallery_button = findViewById(R.id.gallery_button);
        picture_received = findViewById(R.id.server_received_image);

        camera_button.setOnClickListener(view -> {
            if (!is_image_sent.get()) {
                Toast.makeText(MainActivity.this,
                        "зачилься другалёк, картинка отправляется",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            is_image_sent.set(false);
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                // Create a File reference for future access
                image_file = getPhotoFileUri(image_file_name);

                Uri fileProvider = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider", image_file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
                camera_result_launcher.launch(takePictureIntent);
//                camera_result_launcher.launch(fileProvider);
            } catch (ActivityNotFoundException e) {
                Log.e("Error", e.getMessage(), e);
            }
        });

        camera_result_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap picture_bitmap = BitmapFactory.decodeFile(image_file.getAbsolutePath());
                        ImageProcess process = new ImageProcess(MainActivity.this,
                                image_file.getAbsolutePath(),
                                is_image_sent);
                        process.processImage();
                    } else {
                        Toast.makeText(this,
                                "Error while taking picture",
                                Toast.LENGTH_SHORT).show();
                        is_image_sent.set(true);
                    }
                }
        );

        gallery_button.setOnClickListener(view -> {
            getImageLauncher.launch("image/*");
//            try {
//                // Create a File reference for future access
////                ImageFile = getPhotoFileUri(imageFileName);
////
////                Uri fileProvider = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", ImageFile);
//
////                ImageProcess process = new ImageProcess(MainActivity.this, image_file.getAbsolutePath(), is_image_sent);
////                process.processImage();
//
//                Intent i = new Intent();
//                i.setType("image/*");
//                i.setAction(Intent.ACTION_GET_CONTENT);
//
//                gallery_result_launcher.launch(i);
//
//            } catch (ActivityNotFoundException e) {
//                Log.e("Error", e.getMessage(), e);
//            }
        });

        gallery_result_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        // TODO
                        return;
                    }

                    Intent data = result.getData();
                    if (data == null || data.getData() == null) {
                        // TODO
                        return;
                    }


                    Uri selectedImageUri = data.getData();
                    Bitmap selectedImageBitmap = null;
                    try {
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(),
                                selectedImageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

//                    ImageProcess process = new ImageProcess(MainActivity.this,
//                            "/storage/emulated/0/DCIM/Camera/IMG_20230327_215835.jpg",
//                            is_image_sent);
//                    process.processImage();

//                    String bebra = getImageFilePath(selectedImageUri);

                    Toast.makeText(this, "tmp", Toast.LENGTH_SHORT).show();

                });
    }

    // First, create an instance of ActivityResultLauncher
    ActivityResultLauncher<String> getImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                // Handle the returned image URI here
                String imagePath = getRealPathFromUri(uri);
                Log.d("ImagePath", imagePath);
            });


    // Finally, define a helper method to get the real path from the content URI
    public String getRealPathFromUri(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
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
