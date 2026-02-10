package com.example.cunning_proyect;

import android.os.Bundle;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; // Necesario para cambiar fragmentos
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private Context context;
    private List<Community> list;

    public CommunityAdapter(Context context, List<Community> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Community community = list.get(position);

        holder.tvName.setText(community.getNombre());
        holder.tvDesc.setText(community.getDescripcion());

        String fotoUrl = community.getFotoUrl();
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            try { holder.imgIcon.setImageURI(Uri.parse(fotoUrl)); }
            catch (Exception e) { holder.imgIcon.setImageResource(android.R.drawable.ic_menu_myplaces); }
        } else {
            holder.imgIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        // 🔥 AL HACER CLIC: ABRIMOS EL MAPA DIRECTAMENTE 🔥
        holder.itemView.setOnClickListener(v -> {
            // 1. Preparamos los datos para el mapa
            CommunityMapFragment mapFragment = new CommunityMapFragment();
            Bundle args = new Bundle();
            args.putString("COMM_ID", community.getId());       // ID para borrar
            args.putString("COMM_CREATOR", community.getCreadorId()); // Creador para permiso
            args.putString("COMM_NAME", community.getNombre());
            args.putDouble("COMM_LAT", community.getLatitud());
            args.putDouble("COMM_LON", community.getLongitud());
            mapFragment.setArguments(args);

            // 2. Cambiamos la pantalla al Fragmento del Mapa
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, mapFragment) // 'fragment_container' es el ID común del hueco principal
                        .addToBackStack(null) // Para poder volver atrás
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCommName);
            tvDesc = itemView.findViewById(R.id.tvCommDesc);
            imgIcon = itemView.findViewById(R.id.imgCommIcon);
        }
    }
}