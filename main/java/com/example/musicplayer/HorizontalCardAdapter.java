package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class HorizontalCardAdapter extends RecyclerView.Adapter<HorizontalCardAdapter.CardViewHolder> {

    private Context context;
    private List<CardItem> itemList;

    public static class CardItem {
        public String id;
        public String imageUrl;
        public String title;
        public String playCount;

        public CardItem(String id, String imageUrl, String title) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.title = title;
        }
    }

    public HorizontalCardAdapter(Context context, List<CardItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_horizontal_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardItem item = itemList.get(position);
        holder.tvTitle.setText(item.title);

        Glide.with(context).load(item.imageUrl).into(holder.ivCover);


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlaylistDetailActivity.class);
            intent.putExtra("playlistId", item.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvPlayCount;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvPlayCount = itemView.findViewById(R.id.tv_play_count);
        }
    }
}