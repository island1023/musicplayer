package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DiscoverApiService {
    // 获取曲风列表 (包含摇滚、治愈等)
    @GET("/style/list")
    Call<ResponseBody> getStyleList();

    // 备用：获取热门歌单分类 (通常也包含摇滚、治愈、欧美等分类，且带封面图)
    @GET("/playlist/hot")
    Call<ResponseBody> getHotPlaylistTags();

    // 根据曲风获取歌单
    @GET("/style/playlist")
    Call<ResponseBody> getPlaylistsByStyle(@Query("tagId") int tagId, @Query("size") int size);
}