package com.example.musicplayer;

import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlayerActivity extends AppCompatActivity {

    private ImageView ivBack, ivPoster, ivPlayPause, ivPlayMode, ivEq;
    private TextView tvSongName, tvArtistName, tvCurrentTime, tvTotalTime, tvLyrics;
    private SeekBar sbProgress;

    private PlayerApiService apiService;
    private MediaPlayer mediaPlayer;
    private ObjectAnimator recordAnimator;
    private Handler handler = new Handler();

    private String currentSongId = "";
    private boolean isPlaying = false;

    // 播放模式: 0列表循环, 1单曲循环, 2随机播放
    private int playMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // 假设从上个页面通过 Intent 传来了歌曲 ID
        currentSongId = getIntent().getStringExtra("songId");
        if (currentSongId == null) currentSongId = "33894312"; // 测试兜底数据

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
        ivPlayMode = findViewById(R.id.iv_play_mode);
        ivEq = findViewById(R.id.iv_eq);

        tvSongName = findViewById(R.id.tv_song_name);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvLyrics = findViewById(R.id.tv_lyrics);
        sbProgress = findViewById(R.id.sb_progress);

        mediaPlayer = new MediaPlayer();
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://你的IP:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlayerApiService.class);
    }

    // 设置黑胶唱片无限旋转动画
    private void initRecordAnimation() {
        recordAnimator = ObjectAnimator.ofFloat(findViewById(R.id.cv_record), "rotation", 0f, 360f);
        recordAnimator.setDuration(20000); // 转一圈20秒
        recordAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        recordAnimator.setInterpolator(new LinearInterpolator());
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // 播放/暂停
        ivPlayPause.setOnClickListener(v -> togglePlayPause());

        // 切换播放模式
        ivPlayMode.setOnClickListener(v -> {
            playMode = (playMode + 1) % 3;
            String[] modes = {"列表循环", "单曲循环", "随机播放"};
            Toast.makeText(this, modes[playMode], Toast.LENGTH_SHORT).show();
            // TODO: 根据模式更换图标，并在 MediaPlayer.setOnCompletionListener 中处理逻辑
        });

        // 音效按钮（模拟）
        ivEq.setOnClickListener(v -> Toast.makeText(this, "打开原生系统均衡器(Equalizer)", Toast.LENGTH_SHORT).show());

        // 拖动进度条
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

    private void loadSongData() {
        // 1. 获取海报和歌曲信息
        apiService.getSongDetail(currentSongId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONObject song = json.getJSONArray("songs").getJSONObject(0);

                    tvSongName.setText(song.getString("name"));
                    tvArtistName.setText(song.getJSONArray("ar").getJSONObject(0).getString("name") + " >");

                    // 提取海报并加载
                    String picUrl = song.getJSONObject("al").optString("picUrl");
                    if (!picUrl.isEmpty()) {
                        Glide.with(PlayerActivity.this).load(picUrl).into(ivPoster);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        // 2. 获取实际播放链接并准备 MediaPlayer
        apiService.getSongUrl(currentSongId, "exhigh").enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String playUrl = json.getJSONArray("data").getJSONObject(0).getString("url");
                    prepareAndPlayMedia(playUrl);
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        // 3. 获取并解析逐字歌词
        fetchAndParseLyrics();
    }

    private void prepareAndPlayMedia(String url) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync(); // 异步准备，防止阻塞主线程

            mediaPlayer.setOnPreparedListener(mp -> {
                sbProgress.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration()));
                togglePlayPause(); // 准备好后自动播放
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;

        if (isPlaying) {
            mediaPlayer.pause();
            ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            recordAnimator.pause();
        } else {
            mediaPlayer.start();
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            if (recordAnimator.isStarted()) recordAnimator.resume(); else recordAnimator.start();
            updateSeekBar();
        }
        isPlaying = !isPlaying;
    }

    // 更新进度条 UI
    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            sbProgress.setProgress(currentPosition);
            tvCurrentTime.setText(formatTime(currentPosition));
            handler.postDelayed(this::updateSeekBar, 1000); // 每秒更新一次
        }
    }

    // 根据你提供的 yrc 格式文档编写的基础解析器
    private void fetchAndParseLyrics() {
        apiService.getLyricNew(currentSongId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    // 如果存在 yrc (逐字歌词)
                    if (json.has("yrc")) {
                        String rawYrc = json.getJSONObject("yrc").getString("lyric");
                        // 此处仅做简单提取演示，实际需要用正则分离时间戳和文字
                        // 你的文档说明格式如: [16210,3460](16210,670,0)还
                        tvLyrics.setText("成功获取逐字歌词数据长度: " + rawYrc.length() + "\n(实际渲染需自定义 View 支持卡拉OK变色)");
                    } else if (json.has("lrc")) {
                        // 降级使用普通歌词
                        tvLyrics.setText(json.getJSONObject("lrc").getString("lyric"));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // 毫秒转 mm:ss
    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}