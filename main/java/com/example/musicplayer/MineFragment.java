package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
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

    private String currentUid;
    private UserApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        currentUid = SpUtils.getUserId(requireContext());
        initViews(view);
        initRetrofit();
        setupListeners(view);
        setupTabs();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!currentUid.isEmpty()) fetchUserInfo();
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
        apiService = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create()).build().create(UserApiService.class);
    }

    private void setupListeners(View view) {
        View.OnClickListener profileClickListener = v -> startActivity(new Intent(getActivity(), ProfileActivity.class));
        ivAvatar.setOnClickListener(profileClickListener);
        tvNickname.setOnClickListener(profileClickListener);

        view.findViewById(R.id.ll_social_stats).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SocialActivity.class);
            intent.putExtra("uid", currentUid);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_recent).setOnClickListener(v -> startActivity(new Intent(getActivity(), RecentActivity.class)));
        view.findViewById(R.id.btn_local).setOnClickListener(v -> startActivity(new Intent(getActivity(), ImportLocalActivity.class)));

        view.findViewById(R.id.tv_liked_songs).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LikedSongsActivity.class);
            intent.putExtra("uid", currentUid);
            startActivity(intent);
        });
        view.findViewById(R.id.tv_import_local).setOnClickListener(v -> startActivity(new Intent(getActivity(), ImportLocalActivity.class)));
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutMusic.setVisibility(View.GONE);
                layoutPodcast.setVisibility(View.GONE);
                layoutNotes.setVisibility(View.GONE);

                if (tab.getPosition() == 0) layoutMusic.setVisibility(View.VISIBLE);
                else if (tab.getPosition() == 1) {
                    layoutPodcast.setVisibility(View.VISIBLE);
                    fetchUserDj(); // 拉取播客真实数据
                } else if (tab.getPosition() == 2) {
                    layoutNotes.setVisibility(View.VISIBLE);
                    fetchUserEvent(); // 拉取笔记真实数据
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void fetchUserInfo() {
        apiService.getUserDetail(currentUid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        int level = jsonObject.optInt("level", 0);
                        JSONObject profile = jsonObject.getJSONObject("profile");

                        String avatarUrl = profile.optString("avatarUrl");

                        requireActivity().runOnUiThread(() -> {
                            tvNickname.setText(profile.optString("nickname"));
                            tvLevel.setText("Lv." + level);
                            tvFollows.setText(profile.optInt("follows") + " 关注");
                            tvFans.setText(profile.optInt("followeds") + " 粉丝");

                            // 🌟 修复头像加载逻辑：强制指定上下文，加载错误时保持 logo
                            if (!avatarUrl.isEmpty()) {
                                Glide.with(requireContext()).load(avatarUrl).placeholder(R.drawable.ic_logo).circleCrop().into(ivAvatar);
                            }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }



    private void showCreatePlaylistDialog() {
        String[] types = {"普通歌单", "共享歌单", "隐私歌单"};
        final String[] selectedType = {"NORMAL"};
        final String[] isPrivacy = {""};

        new AlertDialog.Builder(getContext())
                .setTitle("选择歌单类别")
                .setSingleChoiceItems(types, 0, (dialog, which) -> {
                    if (which == 1) selectedType[0] = "SHARED";
                    else if (which == 2) isPrivacy[0] = "10";
                    else selectedType[0] = "NORMAL";
                })
                .setPositiveButton("下一步", (dialog, which) -> {
                    // 弹出名称输入框
                    EditText et = new EditText(getContext());
                    new AlertDialog.Builder(getContext()).setTitle("输入歌单名称").setView(et)
                            .setPositiveButton("创建", (d, w) -> {
                                String name = et.getText().toString();
                                apiService.createPlaylist(name, isPrivacy[0], selectedType[0]).enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                        try {
                                            JSONObject res = new JSONObject(response.body().string());
                                            String pid = res.optJSONObject("playlist").optString("id");
                                            // 创建成功，跳转到歌曲添加页面
                                            Intent intent = new Intent(getActivity(), AddSongsActivity.class);
                                            intent.putExtra("pid", pid);
                                            startActivity(intent);
                                        } catch (Exception e) { e.printStackTrace(); }
                                    }
                                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                                });
                            }).show();
                }).show();
    }

    // 动态生成真实的播客界面
    private void fetchUserDj() {
        apiService.getUserDj(currentUid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    layoutPodcast.removeAllViews(); // 清空旧数据
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray djs = json.optJSONArray("djRadios"); // 修正解析字段为 djRadios (网易云返回格式)

                    if (djs == null || djs.length() == 0) {
                        TextView tv = new TextView(getContext());
                        tv.setText("暂无播客"); tv.setTextColor(Color.WHITE); tv.setPadding(0,40,0,0);
                        layoutPodcast.addView(tv);
                        return;
                    }
                    for (int i = 0; i < djs.length(); i++) {
                        TextView tv = new TextView(getContext());
                        tv.setText("🎧 " + djs.getJSONObject(i).getString("name"));
                        tv.setTextColor(Color.WHITE); tv.setPadding(0, 30, 0, 30);
                        layoutPodcast.addView(tv);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // 动态生成真实的动态/笔记界面
    private void fetchUserEvent() {
        apiService.getUserEvent(currentUid, 30, -1).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    layoutNotes.removeAllViews();
                    JSONObject json = new JSONObject(response.body().string());
                    JSONArray events = json.optJSONArray("events");

                    if (events == null || events.length() == 0) {
                        TextView tv = new TextView(getContext());
                        tv.setText("暂无动态笔记"); tv.setTextColor(Color.WHITE); tv.setPadding(0,40,0,0);
                        layoutNotes.addView(tv);
                        return;
                    }
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        JSONObject msgJson = new JSONObject(event.getString("json")); // 动态内容嵌套在一个叫 json 的字符串里
                        TextView tv = new TextView(getContext());
                        tv.setText("📝 " + msgJson.optString("msg", "分享了内容"));
                        tv.setTextColor(Color.WHITE); tv.setPadding(0, 30, 0, 30);
                        layoutNotes.addView(tv);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }
}