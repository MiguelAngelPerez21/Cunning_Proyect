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


        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("incidencias")
                .whereEqualTo("comunidadId", community.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalIncidencias = queryDocumentSnapshots.size(); // 🔥 Cuenta TODAS
                    int activasUrgentes = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.contains("urgencia")) {
                            Long urgencia = doc.getLong("urgencia");
                            if (urgencia != null && urgencia == 3) {
                                activasUrgentes++;
                            }
                        }
                    }

                    if (totalIncidencias > 0) {
                        // Muestra el total de incidencias
                        holder.tvCommActive.setText(totalIncidencias + " incidencias en la zona");
                        holder.tvCommActive.setTextColor(android.graphics.Color.parseColor("#AAAAAA")); // Gris clarito

                        if (activasUrgentes > 0) {
                            holder.tvCommBadge.setText(activasUrgentes + " 🔥");
                            holder.tvCommBadge.setVisibility(View.VISIBLE);
                        } else {
                            holder.tvCommBadge.setVisibility(View.GONE);
                        }
                    } else {
                        holder.tvCommActive.setText("✅ Sin incidencias");
                        holder.tvCommActive.setTextColor(android.graphics.Color.parseColor("#00C853")); // Verde
                        holder.tvCommBadge.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    holder.tvCommActive.setText("⚠️ Error datos");
                    holder.tvCommBadge.setVisibility(View.GONE);
                });

        // 🔥 AL HACER CLIC: ABRIMOS EL MAPA DIRECTAMENTE 🔥
        holder.itemView.setOnClickListener(v -> {
            // 1. Preparamos los datos para el mapa
            CommunityMapFragment mapFragment = new CommunityMapFragment();
            Bundle args = new Bundle();
            args.putString("COMM_ID", community.getId());
            args.putString("COMM_CREATOR", community.getCreadorId());
            args.putString("COMM_NAME", community.getNombre());
            args.putDouble("COMM_LAT", community.getLatitud());
            args.putDouble("COMM_LON", community.getLongitud());
            mapFragment.setArguments(args);

            // 2. Cambiamos la pantalla al Fragmento del Mapa
            if (context instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) context;
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, mapFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvCommActive, tvCommBadge;
        ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCommName);
            tvDesc = itemView.findViewById(R.id.tvCommDesc);
            imgIcon = itemView.findViewById(R.id.imgCommIcon);
            // 🔥 Nuevos campos para las estadísticas dinámicas
            tvCommActive = itemView.findViewById(R.id.tvCommActive);
            tvCommBadge = itemView.findViewById(R.id.tvCommBadge);
        }
    }
}