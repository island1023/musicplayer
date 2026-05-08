package com.example.musicplayer;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String videoId = getIntent().getStringExtra("videoId");
        Toast.makeText(this, "进入视频播放页，视频ID：" + videoId, Toast.LENGTH_SHORT).show();
    }
}