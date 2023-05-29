package com.example.warm_letters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageProcess {

    protected Executor executor = null;
    protected Context context = null;
    protected View view = null;
    protected AtomicBoolean isImageSent = null;
    protected Uri imageUri = null;

    protected final String SERVER_URL;

    public ImageProcess(Context context, View view, AtomicBoolean isImageSent, Uri imageUri) {
        this.executor = Executors.newSingleThreadExecutor();
        this.context = context;
        this.view = view;
        this.isImageSent = isImageSent;
        this.imageUri = imageUri;

        this.SERVER_URL = getServerURL();
    }

    public void processImage() {
        executor.execute(() -> {
            Bitmap rotatedBitmap = rotateBitmap(imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] image_bytes = byteArrayOutputStream.toByteArray();
            sendImageToServer(image_bytes);
        });
    }

    protected void sendImageToServer(final byte[] imageBytes) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SERVER_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "image/jpeg");
                connection.setRequestProperty("Content-Length", String.valueOf(imageBytes.length));
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(60_000);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(imageBytes);
                outputStream.flush();
                showSnackBar("Image sent to server for processing");
                outputStream.close();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    File mediaStorageDir = context.getExternalFilesDir(
                            Environment.DIRECTORY_PICTURES);
                    File file = new File(mediaStorageDir, "animation.png");
                    InputStream inputStream = connection.getInputStream();
                    FunctionClass.createFile(context, inputStream, file);
                } else {
                    Log.e("Response Error", "Failed with response code: " + responseCode);
                    showSnackBar("Error: Wrong response code");
                }
            } catch (SocketTimeoutException e) {
                showSnackBar("Server timeout");
            } catch (IOException e) {
                showSnackBar("Server communication error");
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                isImageSent.set(true);
                // get out of here
            }
        }).start();
    }

    protected Bitmap rotateBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
            ExifInterface exif = new ExifInterface(
                    context.getContentResolver().openInputStream(uri));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
            }
            if (rotationAngle != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotationAngle);
                bitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showSnackBar("Error: Image not found");
        } catch (IOException e) {
            e.printStackTrace();
            showSnackBar("Error: exif.getAttributeInt");
        }
        return bitmap;
    }

    protected String getServerURL() {
        Properties properties = new Properties();
        try {
            assert context != null;
            InputStream input = context.getAssets().open("config/config.yaml");
            properties.load(new InputStreamReader(input));
        } catch (IOException e) {
            e.printStackTrace();
            showSnackBar("Error: Missing config");
            throw new RuntimeException("Missing config");
        }
        return "http://"
                + properties.getProperty("ip_address")
                + ":"
                + properties.getProperty("port")
                + "/";
    }

    protected void showSnackBar(String message) {
        FunctionClass.showSnackBar(view, message);
    }
}
