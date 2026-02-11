package com.example.cunning_proyect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CommunitiesFragment extends Fragment {

    private RecyclerView rvCommunities;
    private CommunityAdapter adapter;
    private ArrayList<Community> communityList = new ArrayList<>();
    private FirebaseFirestore db;

    // 🔥 Referencias a la UI (Nombre y Estadísticas)
    private TextView tvUserName, tvAvatar, tvStatReports, tvStatActive, tvStatGroups;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_communities_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Inicializamos los textos
        tvUserName = view.findViewById(R.id.tvUserName);
        tvAvatar = view.findViewById(R.id.imgAvatar);
        tvStatReports = view.findViewById(R.id.tvStatReports);
        tvStatActive = view.findViewById(R.id.tvStatActive);
        tvStatGroups = view.findViewById(R.id.tvStatGroups);

        rvCommunities = view.findViewById(R.id.rvCommunities);
        rvCommunities.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommunityAdapter(getContext(), communityList);
        rvCommunities.setAdapter(adapter);

        // 🔥 1. Cargar el Perfil del Usuario Real
        setupUserProfile();

        // 🔥 2. Cargar las estadísticas de incidencias
        loadIncidentsStatistics();

        // 3. Cargar las comunidades (Esto actualizará la estadística de "Grupos")
        loadCommunitiesFromFirebase();

        View btnAdd = view.findViewById(R.id.btnFloatingAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                if (getActivity() instanceof IncidentsActivity) {
                    ((IncidentsActivity) getActivity()).showNewCommunityDialog();
                }
            });
        }
    }

    // --- 👤 METODO PARA CARGAR EL NOMBRE Y AVATAR ---
    private void setupUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();

            // Si el nombre está vacío, usamos el inicio de su correo (ej: pb@gmail.com -> pb)
            if (name == null || name.isEmpty()) {
                String email = user.getEmail();
                if (email != null && email.contains("@")) {
                    name = email.substring(0, email.indexOf("@"));
                } else {
                    name = "Usuario";
                }
            }

            // Ponemos el nombre en el saludo
            if (tvUserName != null) {
                tvUserName.setText("Hola, " + name);
            }

            // Ponemos sus iniciales (2 letras) en el avatar circular
            if (tvAvatar != null) {
                if (name.length() >= 2) {
                    tvAvatar.setText(name.substring(0, 2).toUpperCase());
                } else {
                    tvAvatar.setText(name.toUpperCase());
                }
            }
        }
    }

    // --- 📊 METODO PARA CARGAR ESTADÍSTICAS DE INCIDENCIAS ---
    private void loadIncidentsStatistics() {
        // Leemos la colección de incidencias enteras
        db.collection("incidencias").get().addOnSuccessListener(queryDocumentSnapshots -> {

            // Total de reportes = Cantidad de documentos en la nube
            int totalReports = queryDocumentSnapshots.size();

            // Incidencias "Activas" (Vamos a contar las que tengan Urgencia Alta = 3)
            int activasUrgentes = 0;

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.contains("urgencia")) {
                    Long urgencia = doc.getLong("urgencia");
                    if (urgencia != null && urgencia == 3) {
                        activasUrgentes++;
                    }
                }
            }

            // Actualizamos los numeritos en la pantalla
            if (tvStatReports != null) tvStatReports.setText(String.valueOf(totalReports));
            if (tvStatActive != null) tvStatActive.setText(String.valueOf(activasUrgentes));

        }).addOnFailureListener(e -> {
            Log.e("STATS", "Error cargando estadísticas: " + e.getMessage());
        });
    }

    // --- 🏢 METODO PARA CARGAR LAS COMUNIDADES (YA LO TENÍAS) ---
    public void loadCommunitiesFromFirebase() {
        db.collection("comunidades")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    communityList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                String nombre = doc.getString("nombre");
                                String desc = doc.getString("descripcion");
                                String fotoUrl = doc.getString("fotoUrl");

                                String creadorId = doc.getString("creadorId");
                                if (creadorId == null) creadorId = "anonimo";

                                double lat = 0;
                                double lon = 0;
                                if (doc.contains("latitud")) lat = doc.getDouble("latitud");
                                if (doc.contains("longitud")) lon = doc.getDouble("longitud");

                                Community comm = new Community(nombre, desc, fotoUrl, lat, lon, creadorId);
                                comm.setId(doc.getId());

                                communityList.add(comm);
                            } catch (Exception e) {
                                Log.e("FIREBASE_ERROR", "Error leyendo: " + e.getMessage());
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // 🔥 ACTUALIZAR ESTADÍSTICA DE GRUPOS:
                        // La cantidad de grupos es el tamaño de esta lista que acabamos de descargar
                        if (tvStatGroups != null) {
                            tvStatGroups.setText(String.valueOf(communityList.size()));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void loadCommunities() {
        loadCommunitiesFromFirebase();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCommunitiesFromFirebase();
        loadIncidentsStatistics(); // Recargamos también las estadísticas al volver a la pantalla
    }
}