package com.example.camera4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageProcess {
    protected final String serverURL;
    protected Executor executor;
    protected Context context;
    protected AtomicBoolean is_image_sent;
    protected String filename;
    protected Uri image_uri = null;

    public ImageProcess(Context context, AtomicBoolean is_image_sent, String filename) {
        this.executor = Executors.newSingleThreadExecutor();

        this.context = context;
        this.is_image_sent = is_image_sent;
        this.filename = filename;

        Properties prop = new Properties();
        try {
            InputStream input = this.context.getAssets().open("config/config.yaml");
            prop.load(new InputStreamReader(input));
        } catch (IOException e) {
            e.printStackTrace();
            showToast("config error");
        }
        this.serverURL = "http://" + prop.getProperty("ip_address") + ":" + prop.getProperty("port") + "/";
    }

    public ImageProcess(Context context, AtomicBoolean is_image_sent, Uri image_uri) {
        this(context, is_image_sent, "");
        this.image_uri = image_uri;
    }

    public void processImageFromCamera() {
        executor.execute(() -> {
            Bitmap rotatedBitmap = rotateBitmap(filename);
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
                                .openInputStream(image_uri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                showToast("Error: file not found");
            }
            assert bitmap != null;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] image_bytes = byteArrayOutputStream.toByteArray();
            sendImageToServer(image_bytes);
        });
    }

    protected void sendImageToServer(final byte[] imageBytes) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(serverURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "image/jpeg");
                connection.setRequestProperty("Content-Length", String.valueOf(imageBytes.length));
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(imageBytes);
                outputStream.flush();
                outputStream.close();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {

                    for (int ii = 0; ii < 5; ++ii) {
                        Vector<Long> time_consuming_vector = new Vector<Long>();
                        for (long i = 0; i < 10_000_000; ++i) {
                            time_consuming_vector.add(i);
                            time_consuming_vector.clear();
                        }
                    }

                    showToast("Image sent to server for processing");
                } else {
                    showToast("Filed to send image to server");
                    Log.e("Response Error", "Failed with response code: " + responseCode);
                }
            } catch (SocketTimeoutException e) {
                showToast("Server timeout");
            } catch (IOException e) {
                showToast("Server communication error");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            is_image_sent.set(true);
        }).start();
    }

    private void showToast(final String message) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // We're on the main thread, no need to create a new looper
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } else {
            // We're on a background thread, need to create a new looper
            HandlerThread handlerThread = new HandlerThread("ImageProcessHandlerThread");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            Handler handler = new Handler(looper);
            handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }
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
}
