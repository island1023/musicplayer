package com.example.musicplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class VideoFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private LinearLayout areaContainer;

    private VideoApiService apiService;
    private String currentArea = "全部";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        initViews(view);
        initRetrofit();
        setupTabs();

        // 默认加载第一个 Tab（MV 排行）的数据
        loadMvRanking();

        return view;
    }

    private void initViews(View v) {
        tabLayout = v.findViewById(R.id.tab_video_type);
        recyclerView = v.findViewById(R.id.rv_video_list);
        areaContainer = v.findViewById(R.id.ll_area_container);

        // 设置视频列表为竖向滑动
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 动态初始化 MV 地区的筛选按钮
        String[] areas = {"全部", "内地", "港台", "欧美", "日本", "韩国"};
        for (String area : areas) {
            TextView tv = new TextView(getContext());
            tv.setText(area);
            tv.setTextColor(Color.parseColor("#888888"));
            tv.setTextSize(14f);
            tv.setPadding(40, 20, 40, 20);

            // 点击地区标签，重新加载该地区的 MV
            tv.setOnClickListener(view -> {
                currentArea = area;
                Toast.makeText(getContext(), "正在加载 " + area + " MV", Toast.LENGTH_SHORT).show();
                loadMvRanking();
            });

            areaContainer.addView(tv);
        }
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // 🌟 修改点：强制转换为 View 后再设置可见性
                    ((View) areaContainer.getParent()).setVisibility(View.VISIBLE);
                    loadMvRanking();
                } else {
                    // 🌟 修改点：强制转换为 View 后再设置可见性
                    ((View) areaContainer.getParent()).setVisibility(View.GONE);
                    loadRecommendVideos();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }


    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://你的IP地址:3000/") // TODO: 请确保替换为你的实际后端IP
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(VideoApiService.class);
    }

    // ================== 网络请求与数据解析 ==================

    // 1. 加载 MV 排行榜
    private void loadMvRanking() {
        apiService.getMvRanking(currentArea, 20, 0).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray data = jsonObject.getJSONArray("data");
                        List<VideoAdapter.VideoItem> items = new ArrayList<>();

                        // 解析网易云的 MV 列表 JSON
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject mv = data.getJSONObject(i);
                            items.add(new VideoAdapter.VideoItem(
                                    mv.getString("id"),
                                    mv.getString("name"),        // MV 名称
                                    mv.getString("cover"),       // MV 封面图
                                    mv.getString("artistName")   // 歌手名
                            ));
                        }

                        // 将解析好的数据交给 Adapter 渲染
                        if (getContext() != null) {
                            recyclerView.setAdapter(new VideoAdapter(getContext(), items));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "数据解析异常", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 2. 加载推荐视频列表
    private void loadRecommendVideos() {
        apiService.getRecommendVideos(0).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray datas = jsonObject.getJSONArray("datas");
                        List<VideoAdapter.VideoItem> items = new ArrayList<>();

                        // 网易云推荐视频接口的数据结构嵌套得比较深，需要剥离一层 "data"
                        for (int i = 0; i < datas.length(); i++) {
                            JSONObject itemData = datas.getJSONObject(i).getJSONObject("data");

                            items.add(new VideoAdapter.VideoItem(
                                    // 视频ID可能是 vid 也可能是 id
                                    itemData.optString("vid", itemData.optString("id")),
                                    itemData.getString("title"),                         // 视频标题
                                    itemData.getString("coverUrl"),                      // 视频封面图
                                    itemData.getJSONObject("creator").getString("nickname") // UP主昵称
                            ));
                        }

                        // 将解析好的数据交给 Adapter 渲染
                        if (getContext() != null) {
                            recyclerView.setAdapter(new VideoAdapter(getContext(), items));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "数据解析异常", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}