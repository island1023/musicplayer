package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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

public class StylePlaylistActivity extends AppCompatActivity {

    private int tagId;
    private String tagName;
    private RecyclerView rvPlaylists;
    private DiscoverApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_style_playlist);

        // 获取传过来的曲风数据
        tagId = getIntent().getIntExtra("tagId", 0);
        tagName = getIntent().getStringExtra("tagName");

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(tagName != null ? tagName + "歌单" : "曲风歌单");
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvPlaylists = findViewById(R.id.rv_playlists);
        rvPlaylists.setLayoutManager(new GridLayoutManager(this, 2)); // 两列网格展示

        initRetrofit();
        loadPlaylists();
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://你的IP:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(DiscoverApiService.class);
    }

    private void loadPlaylists() {
        // 请求该曲风下的 30 个歌单
        apiService.getPlaylistsByStyle(tagId, 30).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        // 网易云返回格式通常包裹在 data 中
                        JSONArray playlistArray = jsonObject.getJSONObject("data").getJSONArray("playlists");
                        List<PlaylistItem> items = new ArrayList<>();

                        for (int i = 0; i < playlistArray.length(); i++) {
                            JSONObject pl = playlistArray.getJSONObject(i);
                            items.add(new PlaylistItem(
                                    pl.optString("id"),
                                    pl.optString("name"),
                                    pl.optString("coverImgUrl")
                            ));
                        }
                        rvPlaylists.setAdapter(new PlaylistAdapter(items));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(StylePlaylistActivity.this, "解析失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(StylePlaylistActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 内部数据模型
    private static class PlaylistItem {
        String id; String name; String coverUrl;
        public PlaylistItem(String id, String name, String coverUrl) {
            this.id = id; this.name = name; this.coverUrl = coverUrl;
        }
    }

    // 内部适配器
    private class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {
        private List<PlaylistItem> data;
        public PlaylistAdapter(List<PlaylistItem> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_grid, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PlaylistItem item = data.get(position);
            holder.tvName.setText(item.name);
            Glide.with(StylePlaylistActivity.this).load(item.coverUrl).into(holder.ivCover);

            // 点击歌单事件 (预留跳转至 歌单详情页 的逻辑)
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(StylePlaylistActivity.this, "选中歌单: " + item.name, Toast.LENGTH_SHORT).show();
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivCover; TextView tvName;
            public VH(View v) {
                super(v);
                ivCover = v.findViewById(R.id.iv_cover);
                tvName = v.findViewById(R.id.tv_playlist_name);
            }
        }
    }
}