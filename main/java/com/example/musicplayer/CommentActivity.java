package com.example.musicplayer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CommentActivity extends AppCompatActivity {
    private String resourceId;
    private int resourceType; // 0:歌曲, 1:MV, 5:视频
    private CommentApiService apiService;
    private EditText etComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        resourceId = getIntent().getStringExtra("id");
        resourceType = getIntent().getIntExtra("type", 0);

        etComment = findViewById(R.id.et_comment);
        RecyclerView recyclerView = findViewById(R.id.rv_comments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        initRetrofit();
        loadComments();

        findViewById(R.id.btn_send).setOnClickListener(v -> sendComment());
    }

    private void initRetrofit() {
        apiService = new Retrofit.Builder()
                .baseUrl("http://你的IP:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(CommentApiService.class);
    }

    private void loadComments() {
        apiService.getComments(resourceType, resourceId, 2, 1, 20, null)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        // TODO: 解析评论列表，展示头像、昵称、内容、点赞数
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
    }

    private void sendComment() {
        String content = etComment.getText().toString();
        if (content.isEmpty()) return;

        apiService.handleComment(1, resourceType, resourceId, content, null)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(CommentActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                            etComment.setText("");
                            loadComments(); // 刷新列表
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
    }
}