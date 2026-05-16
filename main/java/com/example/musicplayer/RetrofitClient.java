package com.example.musicplayer;

import android.content.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    // 🌟 改为接收 Context 以便操作 SharedPreferences
    public static AuthApiService getApi(Context context) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    // 1. 自动添加持久化 Cookie 到请求头
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            String savedCookie = SpUtils.getCookie(context);
                            Request.Builder builder = chain.request().newBuilder();
                            if (!savedCookie.isEmpty()) {
                                // 注入 Cookie 到 Header
                                builder.addHeader("Cookie", savedCookie);
                            }
                            return chain.proceed(builder.build());
                        }
                    })
                    // 2. 登录请求成功后，自动捕获并持久化新的 Cookie
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response originalResponse = chain.proceed(chain.request());
                            // 如果是登录相关的接口返回了 Set-Cookie
                            if (!originalResponse.headers("Set-Cookie").isEmpty()) {
                                StringBuilder cookieStr = new StringBuilder();
                                for (String header : originalResponse.headers("Set-Cookie")) {
                                    // 简单拼接所有 Cookie
                                    cookieStr.append(header).append(";");
                                }
                                SpUtils.saveCookie(context, cookieStr.toString());
                            }
                            return originalResponse;
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(AuthApiService.class);
    }
}