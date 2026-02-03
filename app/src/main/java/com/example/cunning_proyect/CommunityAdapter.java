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

    private ArrayList<Community> list;

    public CommunityAdapter(ArrayList<Community> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño corregido (item_community)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Community community = list.get(position);
        holder.tvName.setText(community.getName());
        holder.tvDesc.setText(community.getDescription());

        if (community.getImageUri() != null && !community.getImageUri().isEmpty()) {
            holder.imgIcon.setImageURI(Uri.parse(community.getImageUri()));
        } else {
            holder.imgIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommunityDetailActivity.class);
            intent.putExtra("COMMUNITY_NAME", community.getName());
            intent.putExtra("COMM_LAT", community.getLatitude());
            intent.putExtra("COMM_LON", community.getLongitude());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vinculamos con los IDs del XML item_community.xml
            tvName = itemView.findViewById(R.id.tvCommName);
            tvDesc = itemView.findViewById(R.id.tvCommDesc);
            imgIcon = itemView.findViewById(R.id.imgCommIcon);
        }
    }
}