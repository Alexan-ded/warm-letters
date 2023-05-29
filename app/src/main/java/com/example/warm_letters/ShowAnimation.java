package com.example.warm_letters;

import android.content.Intent;
import android.net.Uri;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import com.github.penfeizhou.animation.apng.APNGDrawable;
import java.io.File;

public class ShowAnimation extends AppCompatActivity {

    protected ImageView handwritingAnimation;
    protected ImageButton shareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_animation);

        handwritingAnimation = findViewById(R.id.handwritingAnimation);
        shareButton = findViewById(R.id.shareButton);

        File file = (File) getIntent().getSerializableExtra("file");
        if (file == null) {
            throw new RuntimeException("Error: Animation file is null");
        }

        APNGDrawable apngDrawable = APNGDrawable.fromFile(file.getPath());
        handwritingAnimation.setImageDrawable(apngDrawable);

        shareButton.setOnClickListener(view -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
            );
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

    }
}