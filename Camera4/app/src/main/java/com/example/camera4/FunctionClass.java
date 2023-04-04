package com.example.camera4;

import android.view.View;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

public class FunctionClass {
    public static void showSnackBar(View view, String message, boolean is_long) {
        Snackbar.make(view, message, is_long ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT)
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .show();
    }

    public static void showSnackBar(View view, String message) {
        showSnackBar(view, message, false);
    }
}
