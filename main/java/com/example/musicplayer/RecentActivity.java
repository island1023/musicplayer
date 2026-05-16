package com.example.musicplayer;

import android.os.Bundle;
import android.widget.ImageView;
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

public class RecentActivity extends AppCompatActivity {

    private ListView lvRecentSongs;
    private UserApiService apiService;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);

        currentUid = SpUtils.getUserId(this);
        lvRecentSongs = findViewById(R.id.lv_recent_songs);
        ImageView ivBack = findViewById(R.id.iv_back);

        ivBack.setOnClickListener(v -> finish());

        apiService = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(UserApiService.class);

        if (!currentUid.isEmpty()) {
            fetchRecentSongs();
        } else {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchRecentSongs() {
        // 调用您要求的 /record/recent/song 接口，获取最近播放的 100 首歌曲
        apiService.getRecentSongs(100).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());

                        // 网易云 API 正常返回码为 200
                        if (jsonObject.optInt("code") != 200) {
                            Toast.makeText(RecentActivity.this, "获取失败，请重试", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 将解析工作交给专门的方法处理
                        parseAndDisplaySongs(jsonObject);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(RecentActivity.this, "数据解析异常", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(RecentActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 将刚才补充的解析逻辑完整写入到这里
    private void parseAndDisplaySongs(JSONObject jsonObject) {
        try {
            // 第一层：获取最外面的 data 对象
            JSONObject dataObj = jsonObject.optJSONObject("data");
            if (dataObj != null) {
                // 第二层：获取 list 数组
                JSONArray listArray = dataObj.optJSONArray("list");

                if (listArray == null || listArray.length() == 0) {
                    Toast.makeText(RecentActivity.this, "暂无最近播放记录", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Map<String, String>> dataList = new ArrayList<>();
                for (int i = 0; i < listArray.length(); i++) {
                    // 第三层：核心！最近播放接口的歌曲信息嵌套在一个叫 "data" 的字段里
                    JSONObject songWrapper = listArray.getJSONObject(i).optJSONObject("data");

                    if (songWrapper != null) {
                        String songName = songWrapper.optString("name", "未知歌曲");
                        String artistName = "未知歌手";

                        // 解析歌手数组 (ar)
                        JSONArray arArray = songWrapper.optJSONArray("ar");
                        if (arArray != null && arArray.length() > 0) {
                            artistName = arArray.getJSONObject(0).optString("name", "未知歌手");
                        }

                        Map<String, String> map = new HashMap<>();
                        map.put("title", songName);
                        map.put("artist", artistName);
                        dataList.add(map);
                    }
                }

                // 将整理好的数据通过 SimpleAdapter 渲染到界面上
                SimpleAdapter adapter = new SimpleAdapter(
                        RecentActivity.this, dataList, android.R.layout.simple_list_item_2,
                        new String[]{"title", "artist"}, new int[]{android.R.id.text1, android.R.id.text2}
                );

                // 确保在主线程(UI线程)更新视图
                runOnUiThread(() -> lvRecentSongs.setAdapter(adapter));

            } else {
                Toast.makeText(RecentActivity.this, "暂无最近播放记录", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(RecentActivity.this, "列表数据解析失败", Toast.LENGTH_SHORT).show();
        }
    }
}