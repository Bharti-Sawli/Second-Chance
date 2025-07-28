package com.example.secondchance;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class OrderPlacedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_placed);

        // Initialize views
        VideoView videoView = findViewById(R.id.videoView);
        TextView orderMessage = findViewById(R.id.orderMessage);
        Button backButton = findViewById(R.id.backButton);
        ImageView backIcon = findViewById(R.id.backIcon); // Initialize back icon

        // Set up the video
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.order_placed));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true); // Optional: Loop the video
                videoView.start();
            }
        });

        // Set order message with payment method and phone number if available
        String paymentMethod = getIntent().getStringExtra("paymentMethod");
        String phone = getIntent().getStringExtra("phone");
        if (paymentMethod != null && phone != null) {
            orderMessage.setText("Order Successfully Placed\nwith " + paymentMethod + "!");
        } else if (paymentMethod != null) {
            orderMessage.setText("Order Successfully Placed with " + paymentMethod + "!");
        } else {
            orderMessage.setText("Order Successfully Placed!");
        }

        // Back icon click listener
        backIcon.setOnClickListener(v -> {
            Intent intent = new Intent(OrderPlacedActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Back button click listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OrderPlacedActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoView videoView = findViewById(R.id.videoView);
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoView videoView = findViewById(R.id.videoView);
        if (videoView != null && !videoView.isPlaying()) {
            videoView.start();
        }
    }
}