package com.example.musicplayer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvToplistGallery;
    private SearchApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.et_search);
        rvToplistGallery = findViewById(R.id.rv_toplist_gallery);
        rvToplistGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_audio_match).setOnClickListener(v -> Toast.makeText(this, "正在开启听歌识曲...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.tv_search_btn).setOnClickListener(v -> performSearch(etSearch.getText().toString()));

        initRetrofit();
        loadInitialData();
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://你的IP地址:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SearchApiService.class);
    }

    private void loadInitialData() {
        apiService.getDefaultSearchKeyword().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        etSearch.setHint(json.getJSONObject("data").getString("showKeyword"));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        apiService.getToplistDetail().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray list = json.getJSONArray("list");
                        List<JSONObject> allOfficialCharts = new ArrayList<>();

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject chart = list.getJSONObject(i);
                            if (chart.has("tracks") && chart.getJSONArray("tracks").length() > 0) {
                                allOfficialCharts.add(chart);
                            }
                        }
                        rvToplistGallery.setAdapter(new ToplistAdapter(allOfficialCharts));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void performSearch(String key) {
        if (key.isEmpty()) key = etSearch.getHint().toString();

        // 🌟 消除搜索 TODO：真正跳转到搜索结果页
        Intent intent = new Intent(SearchActivity.this, SearchResultActivity.class);
        intent.putExtra("keyword", key);
        startActivity(intent);
    }

    private class ToplistAdapter extends RecyclerView.Adapter<ToplistAdapter.VH> {
        private List<JSONObject> data;
        public ToplistAdapter(List<JSONObject> data) { this.data = data; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_search_toplist, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            try {
                JSONObject chart = data.get(position);
                String chartName = chart.getString("name");
                String chartId = chart.getString("id");

                holder.name.setText(chartName);


                holder.name.setOnClickListener(v -> {
                    Intent intent = new Intent(SearchActivity.this, PlaylistDetailActivity.class);
                    intent.putExtra("playlistId", chartId);
                    startActivity(intent);
                });

                holder.list.removeAllViews();
                JSONArray tracks = chart.getJSONArray("tracks");

                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject song = tracks.getJSONObject(i);
                    String songName = song.getString("first");
                    String artistName = song.getString("second");
                    final int songIndex = i; // 固定索引，供内部类使用

                    TextView tv = new TextView(SearchActivity.this);
                    tv.setText((i + 1) + "  " + songName + " - " + artistName);
                    tv.setTextColor(i < 3 ? Color.WHITE : Color.parseColor("#888888"));
                    tv.setPadding(0, 20, 0, 20);
                    tv.setTextSize(14f);
                    tv.setSingleLine(true);
                    tv.setClickable(true);
                    tv.setBackgroundResource(android.R.attr.selectableItemBackground);

                    // 🌟 消除 TODO 2：点击单曲获取真实 ID 并跳转到播放页
                    tv.setOnClickListener(v -> {
                        Toast.makeText(SearchActivity.this, "正在获取歌曲资源...", Toast.LENGTH_SHORT).show();

                        // 发起网络请求获取完整的榜单数据，提取出被点击歌曲的真实 ID
                        apiService.getTopList(chartId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    try {
                                        JSONObject json = new JSONObject(response.body().string());
                                        JSONArray fullTracks = json.getJSONObject("playlist").getJSONArray("tracks");
                                        // 根据之前点击的位置，拿出真实 ID
                                        String realSongId = fullTracks.getJSONObject(songIndex).getString("id");

                                        // 跳转到播放页
                                        Intent intent = new Intent(SearchActivity.this, PlayerActivity.class);
                                        intent.putExtra("songId", realSongId);
                                        startActivity(intent);

                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                            }
                            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                        });
                    });

                    holder.list.addView(tv);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name;
            LinearLayout list;
            public VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_chart_name);
                list = v.findViewById(R.id.ll_songs_list);
            }
        }
    }
}