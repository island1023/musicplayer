package com.example.musicplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImportLocalActivity extends AppCompatActivity {

    private ListView lvLocalSongs;
    private UserApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_local);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish()); // 修复无法回退的问题
        lvLocalSongs = findViewById(R.id.lv_local_songs);

        apiService = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(UserApiService.class);

        checkPermission();
    }

    private void checkPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
        } else {
            loadLocalMusic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) loadLocalMusic();
    }

    private void loadLocalMusic() {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION},
                MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null);

        if (cursor != null && cursor.getCount() > 0) {
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                    this, android.R.layout.simple_list_item_2, cursor,
                    new String[]{MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                    new int[]{android.R.id.text1, android.R.id.text2}, 0
            );
            lvLocalSongs.setAdapter(adapter);

            lvLocalSongs.setOnItemClickListener((parent, view, position, id) -> {
                cursor.moveToPosition(position);
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) / 1000;

                Toast.makeText(this, "正在云端匹配: " + title, Toast.LENGTH_SHORT).show();
                apiService.matchLocalSong(title, artist, duration).enqueue(new Callback<ResponseBody>() {
                    @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Toast.makeText(ImportLocalActivity.this, "匹配请求发送成功", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
            });
        } else {
            Toast.makeText(this, "本地暂无音乐文件", Toast.LENGTH_SHORT).show();
        }
    }
}