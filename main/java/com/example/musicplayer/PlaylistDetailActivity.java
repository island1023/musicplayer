package com.example.musicplayer;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PlaylistDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String playlistId = getIntent().getStringExtra("playlistId");
        Toast.makeText(this, "进入歌单详情页，歌单ID：" + playlistId, Toast.LENGTH_SHORT).show();
    }
}