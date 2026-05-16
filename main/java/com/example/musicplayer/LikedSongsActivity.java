package com.example.musicplayer;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LikedSongsActivity extends AppCompatActivity {

    private String currentUid;
    private ListView lvLikedSongs;
    private UserApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_songs);

        currentUid = SpUtils.getUserId(this);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        lvLikedSongs = findViewById(R.id.lv_liked_songs);

        apiService = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(UserApiService.class);

        if (!currentUid.isEmpty()) {
            fetchLikeList();
        } else {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLikeList() {
        apiService.getLikeList(currentUid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());

                        if(json.optInt("code") != 200) {
                            Toast.makeText(LikedSongsActivity.this, "获取失败", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray idsArray = json.optJSONArray("ids");
                        if (idsArray == null || idsArray.length() == 0) {
                            Toast.makeText(LikedSongsActivity.this, "暂无喜欢的音乐", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 将所有 ID 拼成逗号分隔的字符串
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < Math.min(idsArray.length(), 50); i++) {
                            sb.append(idsArray.getLong(i)).append(",");
                        }
                        fetchSongDetails(sb.toString().substring(0, sb.length() - 1));

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LikedSongsActivity.this, "解析红心列表异常", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void fetchSongDetails(String ids) {
        apiService.getSongDetail(ids).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray songs = json.getJSONArray("songs");
                    List<Map<String, String>> list = new ArrayList<>();

                    for (int i = 0; i < songs.length(); i++) {
                        JSONObject song = songs.getJSONObject(i);
                        Map<String, String> map = new HashMap<>();
                        map.put("title", song.getString("name"));
                        map.put("artist", song.getJSONArray("ar").getJSONObject(0).getString("name"));
                        list.add(map);
                    }

                    SimpleAdapter adapter = new SimpleAdapter(
                            LikedSongsActivity.this, list, android.R.layout.simple_list_item_2,
                            new String[]{"title", "artist"}, new int[]{android.R.id.text1, android.R.id.text2}
                    );
                    runOnUiThread(() -> lvLikedSongs.setAdapter(adapter));

                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }
}