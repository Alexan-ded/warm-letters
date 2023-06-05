package com.example.warm_letters;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FunctionClass {
    public static void showAnimation(Context context, InputStream inputStream, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            fileOutputStream.close();
            Intent intent = new Intent(context, ShowAnimation.class);
            intent.putExtra("file", file);
            context.startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void showSnackBar(View view, String message, boolean is_long) {
        Snackbar.make(view, message, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT)
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .show();
    }

    public static void showSnackBar(View view, String message) {
        showSnackBar(view, message, false);
    }
}
