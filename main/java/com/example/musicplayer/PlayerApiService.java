package com.example.musicplayer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PlayerApiService {

    // 1. 获取歌曲详情 (包含海报 al.picUrl, 歌手 ar, 歌名)
    @GET("/song/detail")
    Call<ResponseBody> getSongDetail(@Query("ids") String ids);

    // 2. 获取音乐真实播放链接
    @GET("/song/url/v1")
    Call<ResponseBody> getSongUrl(@Query("id") String id, @Query("level") String level);

    // 3. 获取逐字歌词
    @GET("/lyric/new")
    Call<ResponseBody> getLyricNew(@Query("id") String id);
}