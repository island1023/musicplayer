package com.example.musicplayer;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApiService {

    // 1. 发送验证码
    @GET("/captcha/sent")
    Call<ResponseBody> sendCaptcha(@Query("phone") String phone);

    // 2. 验证验证码
    @GET("/captcha/verify")
    Call<ResponseBody> verifyCaptcha(@Query("phone") String phone, @Query("captcha") String captcha);

    // 3. 手机号登录 (支持密码或验证码，不需要的传空字符串即可)
    @POST("/login/cellphone")
    Call<ResponseBody> loginCellphone(
            @Query("phone") String phone,
            @Query("password") String password,
            @Query("captcha") String captcha
    );

    // 4. 刷新登录
    @GET("/login/refresh")
    Call<ResponseBody> refreshLogin();

    // 5. 检测手机号码是否已注册
    @GET("/cellphone/existence/check")
    Call<ResponseBody> checkPhoneExistence(@Query("phone") String phone);

    // 6. 注册 (同时可修改密码)
    @POST("/register/cellphone")
    Call<ResponseBody> register(
            @Query("phone") String phone,
            @Query("password") String password,
            @Query("captcha") String captcha,
            @Query("nickname") String nickname
    );
}