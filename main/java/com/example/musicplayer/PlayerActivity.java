package com.example.musicplayer;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlayerActivity extends AppCompatActivity {

    private ImageView ivBack, ivPoster, ivPlayPause, ivPlayMode, ivLike, ivDownload;
    private TextView tvSongName, tvArtistName, tvCurrentTime, tvTotalTime;
    private SeekBar sbProgress;

    private PlayerApiService apiService;
    private MediaPlayer mediaPlayer;
    private ObjectAnimator recordAnimator;
    private Handler handler = new Handler();

    private String currentSongId = "";
    private String currentSongUrl = "";
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private static final int CREATE_FILE_REQUEST_CODE = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        currentSongId = getIntent().getStringExtra("songId");
        if (currentSongId == null) currentSongId = "33894312";

        initViews();
        initRetrofit();
        setupListeners();
        initRecordAnimation();
        loadSongData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        ivPoster = findViewById(R.id.iv_poster);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        ivLike = findViewById(R.id.iv_like);
        ivDownload = findViewById(R.id.iv_download); // 布局中对应的下载图标
        tvSongName = findViewById(R.id.tv_song_name);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        sbProgress = findViewById(R.id.sb_progress);
        mediaPlayer = new MediaPlayer();
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlayerApiService.class);
    }

    private void initRecordAnimation() {
        recordAnimator = ObjectAnimator.ofFloat(findViewById(R.id.cv_record), "rotation", 0f, 360f);
        recordAnimator.setDuration(20000);
        recordAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        recordAnimator.setInterpolator(new LinearInterpolator());
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        ivPlayPause.setOnClickListener(v -> togglePlayPause());

        // 🌟 红心收藏逻辑
        ivLike.setOnClickListener(v -> {
            String uid = SpUtils.getUserId(this);
            isLiked = !isLiked;
            apiService.toggleLike(currentSongId, uid, isLiked).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        ivLike.setImageResource(isLiked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
                        Toast.makeText(PlayerActivity.this, isLiked ? "已添加收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        });

        // 🌟 下载逻辑：调用系统文件选择器
        ivDownload.setOnClickListener(v -> {
            if (currentSongUrl.isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/mpeg");
            intent.putExtra(Intent.EXTRA_TITLE, tvSongName.getText().toString() + ".mp3");
            startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
        });

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) mediaPlayer.seekTo(seekBar.getProgress());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) performDownload(currentSongUrl, uri);
        }
    }

    private void performDownload(String urlString, Uri uri) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                try (InputStream in = conn.getInputStream();
                     OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    runOnUiThread(() -> Toast.makeText(this, "歌曲已保存", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadSongData() {
        apiService.getSongDetail(currentSongId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONObject song = json.getJSONArray("songs").getJSONObject(0);
                    tvSongName.setText(song.getString("name"));
                    tvArtistName.setText(song.getJSONArray("ar").getJSONObject(0).getString("name"));
                    Glide.with(PlayerActivity.this).load(song.getJSONObject("al").getString("picUrl")).into(ivPoster);
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        apiService.getSongUrl(currentSongId, "exhigh").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    currentSongUrl = json.getJSONArray("data").getJSONObject(0).getString("url");
                    prepareAndPlay(currentSongUrl);
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void prepareAndPlay(String url) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                sbProgress.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration()));
                togglePlayPause();
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            mediaPlayer.pause();
            recordAnimator.pause();
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            if (recordAnimator.isStarted()) recordAnimator.resume(); else recordAnimator.start();
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            updateSeekBar();
        }
        isPlaying = !isPlaying;
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            sbProgress.setProgress(mediaPlayer.getCurrentPosition());
            tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private String formatTime(int ms) {
        int sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
        handler.removeCallbacksAndMessages(null);
    }
}