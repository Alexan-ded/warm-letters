package com.example.camera4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
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
    protected final String SERVER_URL;

    protected Executor executor = null;
    protected Context context = null;
    protected View view = null;
    protected AtomicBoolean isImageSent = null;
    protected String fileName = null;
    protected Uri imageUri = null;

    public ImageProcess(Context context, View view, AtomicBoolean isImageSent) {
        this.executor = Executors.newSingleThreadExecutor();
        this.context = context;
        this.view = view;
        this.isImageSent = isImageSent;

        this.SERVER_URL = getServerURL();
    }
    
    public ImageProcess(Context context, View view, AtomicBoolean isImageSent, String fileName) {
        this(context, view, isImageSent);
        this.fileName = fileName;
    }

    public ImageProcess(Context context, View view, AtomicBoolean isImageSent, Uri imageUri) {
        this(context, view, isImageSent);
        this.imageUri = imageUri;
    }

    public void processImageFromCamera() {
        executor.execute(() -> {
            Bitmap rotatedBitmap = rotateBitmap(fileName);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] image_bytes = byteArrayOutputStream.toByteArray();
            sendImageToServer(image_bytes);
        });
    }

    public void processImageFromGallery() {
        executor.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(context.getContentResolver()
                                .openInputStream(imageUri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                showSnackBar("Error: file not found");
            }
            assert bitmap != null;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] image_bytes = byteArrayOutputStream.toByteArray();
            sendImageToServer(image_bytes);




//            // Get the input stream for the image URI
//            InputStream inputStream = null;
//            try {
//                inputStream = context.getContentResolver().openInputStream(imageUri);
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//
//            // Get the orientation information from the image file
//            ExifInterface exif = null;
//            try {
//                exif = new ExifInterface(inputStream);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
//
//            // Decode the image and apply the appropriate rotation
//            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
////            bitmap = rotateBitmap(bitmap, orientation);
//
//            // Use the rotated bitmap as needed
////            imageView.setImageBitmap(bitmap);





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
                connection.setReadTimeout(30_000);

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(imageBytes);
                outputStream.flush();
                outputStream.close();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

//                    for (int ii = 0; ii < 5; ++ii) {
//                        Vector<Long> time_consuming_vector = new Vector<Long>();
//                        for (long i = 0; i < 100_000_000; ++i) {
//                            time_consuming_vector.add(i);
//                            time_consuming_vector.clear();
//                        }
//                    }

                    showSnackBar("Image sent to server for processing");
                } else {
                    FunctionClass.showSnackBar(view, "Filed to send image to server");
                    Log.e("Response Error", "Failed with response code: " + responseCode);
                }
            } catch (SocketTimeoutException e) {
                showSnackBar("Server timeout");
            } catch (IOException e) {
                showSnackBar("Server communication error");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            isImageSent.set(true);
        }).start();
    }

    protected void showSnackBar(String message) {
        FunctionClass.showSnackBar(view, message);
    }

    private Bitmap rotateBitmap(String filename) {
        Bitmap bitmap = BitmapFactory.decodeFile(filename);
        try {
            ExifInterface exif = new ExifInterface(filename);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    protected String getServerURL() {
        Properties properties = new Properties();
        try {
            InputStream input = this.context.getAssets().open("config/config.yaml");
            properties.load(new InputStreamReader(input));
        } catch (IOException e) {
            e.printStackTrace();
            showSnackBar("config error");
        }
        return "http://" + properties.getProperty("ip_address") + ":" + properties.getProperty("port") + "/";
    }
}
