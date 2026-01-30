package com.example.cunning_proyect;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private ArrayList<Community> communityList;

    public CommunityAdapter(ArrayList<Community> communityList) {
        this.communityList = communityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Community community = communityList.get(position);
        holder.tvName.setText(community.getName());
        holder.tvDesc.setText(community.getDescription());


        if (!community.getImageUri().isEmpty()) {
            holder.imgBg.setImageURI(Uri.parse(community.getImageUri()));
        } else {
            holder.imgBg.setImageResource(R.drawable.madrid_placeholder); // si falla se pone la imagen generica (o si no hay imagen)
        }


        holder.itemView.setOnClickListener(v -> {

        });
        // En CommunityAdapter.java, dentro de onBindViewHolder
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommunityDetailActivity.class);
            intent.putExtra("COMMUNITY_NAME", community.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return communityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageView imgBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCommunityName);
            tvDesc = itemView.findViewById(R.id.tvCommunityDesc);
            imgBg = itemView.findViewById(R.id.imgCommunityBackground);
        }
    }
}