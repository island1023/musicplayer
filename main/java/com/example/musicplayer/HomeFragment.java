package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

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
import android.content.Intent;

public class HomeFragment extends Fragment {

    private ImageView ivMenu, ivSearch;
    private TabLayout tabHomeCategory;
    private LinearLayout layoutRecommend, layoutMusic, layoutRadio;

    // 轮播图和各个横向列表
    private ViewPager2 vpBanner;
    private RecyclerView rvMusicPlaylists, rvMusicAlbums, rvRecommendPlaylists, rvRadioHot;

    private HomeApiService apiService;

    // 防止重复请求的标记位
    private boolean isRecommendLoaded = false;
    private boolean isMusicLoaded = false;
    private boolean isRadioLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initRetrofit();
        setupListeners();

        // 默认加载中间“音乐”频道的数据
        fetchMusicData();

        return view;
    }

    private void initViews(View view) {
        ivMenu = view.findViewById(R.id.iv_menu);
        ivSearch = view.findViewById(R.id.iv_search);
        tabHomeCategory = view.findViewById(R.id.tab_home_category);

        layoutRecommend = view.findViewById(R.id.layout_recommend);
        layoutMusic = view.findViewById(R.id.layout_music);
        layoutRadio = view.findViewById(R.id.layout_radio);

        vpBanner = view.findViewById(R.id.vp_banner);
        rvMusicPlaylists = view.findViewById(R.id.rv_music_playlists);
        rvMusicAlbums = view.findViewById(R.id.rv_music_albums);
        rvRecommendPlaylists = view.findViewById(R.id.rv_recommend_playlists);
        rvRadioHot = view.findViewById(R.id.rv_radio_hot);

        // 初始化横向 RecyclerView 配置
        setupHorizontalRecyclerView(rvMusicPlaylists);
        setupHorizontalRecyclerView(rvMusicAlbums);
        setupHorizontalRecyclerView(rvRecommendPlaylists);
        setupHorizontalRecyclerView(rvRadioHot);

        // 默认选中“音乐” Tab (索引为 1)
        tabHomeCategory.getTabAt(1).select();
    }

    private void setupHorizontalRecyclerView(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3000/") // TODO: 替换为实际后端IP
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(HomeApiService.class);
    }

    private void setupListeners() {
        // Tab 切换加载对应数据
        tabHomeCategory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutRecommend.setVisibility(View.GONE);
                layoutMusic.setVisibility(View.GONE);
                layoutRadio.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0:
                        layoutRecommend.setVisibility(View.VISIBLE);
                        if (!isRecommendLoaded) fetchRecommendData();
                        break;
                    case 1:
                        layoutMusic.setVisibility(View.VISIBLE);
                        if (!isMusicLoaded) fetchMusicData();
                        break;
                    case 2:
                        layoutRadio.setVisibility(View.VISIBLE);
                        if (!isRadioLoaded) fetchRadioData();
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 搜索按钮点击事件
        ivSearch.setOnClickListener(v -> Toast.makeText(getContext(), "即将跳转搜索页", Toast.LENGTH_SHORT).show());

        // 🌟 新增：左侧侧边栏菜单(三道杠)点击 -> 跳转到设置页
        ivMenu.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    // ================== 网络请求与 JSON 解析 ==================

    // 1. 加载【音乐】Tab 的数据 (轮播图、甄选歌单、新碟上架)
    private void fetchMusicData() {
        isMusicLoaded = true;

        // 获取轮播图 (type=1 代表 Android 端)
        apiService.getBanner(1).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray banners = jsonObject.getJSONArray("banners");
                        List<String> bannerUrls = new ArrayList<>();
                        for (int i = 0; i < banners.length(); i++) {
                            // 提取图片的 URL
                            bannerUrls.add(banners.getJSONObject(i).getString("pic"));
                        }
                        // 设置给 ViewPager2 轮播图
                        vpBanner.setAdapter(new BannerAdapter(bannerUrls));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        // 获取甄选歌单
        apiService.getPersonalized(6).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray result = jsonObject.getJSONArray("result");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject obj = result.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(
                                    obj.getString("id"),
                                    obj.getString("picUrl"),
                                    obj.getString("name")
                            ));
                        }
                        rvMusicPlaylists.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });

        // 获取新碟上架
        apiService.getTopAlbum("ALL", "new", 2024, 1, 6, 0).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray albums = jsonObject.getJSONObject("weekData").getJSONArray("albums"); // 网易云新碟层级结构
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < albums.length(); i++) {
                            JSONObject obj = albums.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(
                                    obj.getString("id"),
                                    obj.getString("picUrl"),
                                    obj.getString("name")
                            ));
                        }
                        rvMusicAlbums.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // 2. 加载【推荐】Tab 数据 (需要登录后调用)
    private void fetchRecommendData() {
        isRecommendLoaded = true;
        apiService.getRecommendResource().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray recommend = jsonObject.getJSONArray("recommend");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < recommend.length(); i++) {
                            JSONObject obj = recommend.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(
                                    obj.getString("id"),
                                    obj.getString("picUrl"),
                                    obj.getString("name")
                            ));
                        }
                        rvRecommendPlaylists.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    Toast.makeText(getContext(), "获取每日推荐需先登录", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // 3. 加载【电台】Tab 数据
    private void fetchRadioData() {
        isRadioLoaded = true;
        apiService.getDjHot(6, 0).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray djRadios = jsonObject.getJSONArray("djRadios");
                        List<HorizontalCardAdapter.CardItem> items = new ArrayList<>();
                        for (int i = 0; i < djRadios.length(); i++) {
                            JSONObject obj = djRadios.getJSONObject(i);
                            items.add(new HorizontalCardAdapter.CardItem(
                                    obj.getString("id"),
                                    obj.getString("picUrl"),
                                    obj.getString("name")
                            ));
                        }
                        rvRadioHot.setAdapter(new HorizontalCardAdapter(getContext(), items));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // ================== 轮播图适配器 (内部类) ==================
    private class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private List<String> imageUrls;

        public BannerAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            // 设置轮播图内部图片的宽高和缩放
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new BannerViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
            Glide.with(holder.imageView.getContext())
                    .load(imageUrls.get(position))
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls != null ? imageUrls.size() : 0;
        }

        class BannerViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public BannerViewHolder(@NonNull View itemView) {
                super(itemView);
                this.imageView = (ImageView) itemView;
            }
        }
    }
}