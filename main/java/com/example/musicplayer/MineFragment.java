package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MineFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvNickname, tvLevel, tvFollows, tvFans;
    private TabLayout tabLayout;
    private LinearLayout layoutMusic, layoutPodcast, layoutNotes;

    private UserApiService apiService;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        // 1. 从本地记事本中读取真实保存的 UID
        currentUid = SpUtils.getUserId(requireContext());

        initViews(view);
        initRetrofit();
        setupTabs();

        // 2. 如果 UID 不为空，说明已登录，拉取网络数据
        if (!currentUid.isEmpty()) {
            fetchUserInfo();
        } else {
            tvNickname.setText("未登录");
        }

        return view;
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvLevel = view.findViewById(R.id.tv_level);
        tvFollows = view.findViewById(R.id.tv_follows);
        tvFans = view.findViewById(R.id.tv_fans);

        tabLayout = view.findViewById(R.id.tab_layout);
        layoutMusic = view.findViewById(R.id.layout_music);
        layoutPodcast = view.findViewById(R.id.layout_podcast);
        layoutNotes = view.findViewById(R.id.layout_notes);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://你的IP地址:3000/") // 替换为你的服务器地址
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(UserApiService.class);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutMusic.setVisibility(View.GONE);
                layoutPodcast.setVisibility(View.GONE);
                layoutNotes.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0: layoutMusic.setVisibility(View.VISIBLE); break;
                    case 1: layoutPodcast.setVisibility(View.VISIBLE); break;
                    case 2: layoutNotes.setVisibility(View.VISIBLE); break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void fetchUserInfo() {
        apiService.getUserDetail(currentUid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);

                        // 提取网易云返回的数据
                        int level = jsonObject.optInt("level", 0);
                        JSONObject profile = jsonObject.getJSONObject("profile");
                        String nickname = profile.optString("nickname", "未知名单");
                        String avatarUrl = profile.optString("avatarUrl", "");
                        int follows = profile.optInt("follows", 0);
                        int fans = profile.optInt("followeds", 0);

                        // 更新到 UI
                        tvNickname.setText(nickname);
                        tvLevel.setText("Lv." + level);
                        tvFollows.setText(follows + " 关注");
                        tvFans.setText(fans + " 粉丝");

                        // 使用 Glide 渲染圆形头像
                        if (!avatarUrl.isEmpty() && isAdded()) {
                            Glide.with(requireContext())
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .into(ivAvatar);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "数据解析异常", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "获取用户信息失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "网络异常", Toast.LENGTH_SHORT).show();
            }
        });
    }
}