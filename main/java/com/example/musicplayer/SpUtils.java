package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils {
    private static final String SP_NAME = "music_config";
    private static final String KEY_USER_ID = "userId";

    // 保存 UID
    public static void saveUserId(Context context, String userId) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_USER_ID, userId).apply();
    }

    // 获取 UID
    public static String getUserId(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        // 如果没有保存过，默认返回空字符串 ""
        return sp.getString(KEY_USER_ID, "");
    }
}