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

        // 🌟 听歌识曲点击事件
        findViewById(R.id.btn_audio_match).setOnClickListener(v -> {
            Toast.makeText(this, "正在录音并生成指纹...", Toast.LENGTH_LONG).show();
            // 这里演示调用识曲接口，实际FP指纹需要通过录音算法生成
            startAudioMatchTask("MOCK_FP_DATA_FROM_RECORDER");
        });

        findViewById(R.id.tv_search_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchResultActivity.class);
            intent.putExtra("keyword", etSearch.getText().toString());
            startActivity(intent);
        });

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

    private void startAudioMatchTask(String fp) {
        apiService.audioMatch(3, fp).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray result = json.getJSONObject("data").getJSONArray("result");
                    if (result.length() > 0) {
                        String songId = result.getJSONObject(0).getJSONObject("song").getString("id");
                        Intent intent = new Intent(SearchActivity.this, PlayerActivity.class);
                        intent.putExtra("songId", songId);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(SearchActivity.this, "未能识别歌曲", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void loadInitialData() {
        apiService.getToplistDetail().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray list = json.getJSONArray("list");
                        List<JSONObject> charts = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject chart = list.getJSONObject(i);
                            if (chart.has("tracks")) charts.add(chart);
                        }
                        rvToplistGallery.setAdapter(new ToplistAdapter(charts));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private class ToplistAdapter extends RecyclerView.Adapter<ToplistAdapter.VH> {
        private List<JSONObject> data;
        public ToplistAdapter(List<JSONObject> data) { this.data = data; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_search_toplist, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            try {
                JSONObject chart = data.get(position);
                holder.name.setText(chart.getString("name"));
                holder.list.removeAllViews();
                JSONArray tracks = chart.getJSONArray("tracks");
                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject song = tracks.getJSONObject(i);
                    TextView tv = new TextView(SearchActivity.this);
                    tv.setText((i + 1) + "  " + song.getString("first") + " - " + song.getString("second"));
                    tv.setTextColor(Color.WHITE);
                    tv.setPadding(0, 15, 0, 15);
                    holder.list.addView(tv);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView name; LinearLayout list;
            public VH(View v) { super(v); name = v.findViewById(R.id.tv_chart_name); list = v.findViewById(R.id.ll_songs_list); }
        }
    }
}