package com.example.musicplayer;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SearchResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 暂时不设置布局，只接收一下传过来的关键词打印出来
        String keyword = getIntent().getStringExtra("keyword");
        Toast.makeText(this, "进入搜索结果页，搜索词：" + keyword, Toast.LENGTH_SHORT).show();
    }
}