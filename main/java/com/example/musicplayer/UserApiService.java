package com.example.musicplayer;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface UserApiService {

    // 1. 初始化昵称
    @GET("/activate/init/profile")
    Call<ResponseBody> initProfile(@Query("nickname") String nickname);

    // 2. 重复昵称检测
    @GET("/nickname/check")
    Call<ResponseBody> checkNickname(@Query("nickname") String nickname);

    // 3. 获取用户详情 (极其重要，用于渲染页面头部)
    @GET("/user/detail")
    Call<ResponseBody> getUserDetail(@Query("uid") String uid);

    // 4. 获取账号信息
    @GET("/user/account")
    Call<ResponseBody> getUserAccount();

    // 5. 获取用户信息 , 歌单，收藏，mv, dj 数量
    @GET("/user/subcount")
    Call<ResponseBody> getUserSubcount();

    // 6. 获取用户绑定信息
    @GET("/user/binding")
    Call<ResponseBody> getUserBinding(@Query("uid") String uid);

    // 7. 更新用户信息
    @GET("/user/update")
    Call<ResponseBody> updateUser(
            @Query("gender") int gender,
            @Query("birthday") long birthday,
            @Query("nickname") String nickname,
            @Query("province") int province,
            @Query("city") int city,
            @Query("signature") String signature
    );

    // 8. 更新头像 (支持文件上传)
    @Multipart
    @POST("/avatar/upload")
    Call<ResponseBody> uploadAvatar(
            @Query("imgSize") int imgSize,
            @Part MultipartBody.Part imgFile
    );

    // 9. 获取用户歌单
    @GET("/user/playlist")
    Call<ResponseBody> getUserPlaylist(
            @Query("uid") String uid,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    // 10. 获取用户电台 (播客)
    @GET("/user/dj")
    Call<ResponseBody> getUserDj(@Query("uid") String uid);

    // 11. 获取用户关注列表
    @GET("/user/follows")
    Call<ResponseBody> getUserFollows(
            @Query("uid") String uid,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    // 12. 获取用户粉丝列表
    @GET("/user/followeds")
    Call<ResponseBody> getUserFolloweds(
            @Query("uid") String uid,
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    // 13. 获取用户动态 (笔记)
    @GET("/user/event")
    Call<ResponseBody> getUserEvent(
            @Query("uid") String uid,
            @Query("limit") int limit,
            @Query("lasttime") long lasttime
    );
}