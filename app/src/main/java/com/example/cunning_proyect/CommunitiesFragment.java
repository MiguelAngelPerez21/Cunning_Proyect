package com.example.cunning_proyect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class CommunitiesFragment extends Fragment {

    private RecyclerView rvCommunities;
    private CommunityAdapter adapter;
    private ArrayList<Community> communityList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Asegúrate que el nombre del layout es correcto (fragment_communities_list o fragment_communities)
        return inflater.inflate(R.layout.fragment_communities_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rvCommunities = view.findViewById(R.id.rvCommunities);
        rvCommunities.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommunityAdapter(getContext(), communityList);
        rvCommunities.setAdapter(adapter);

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

                                // Recuperamos el Creador ID (Puede ser null en comunidades viejas)
                                String creadorId = doc.getString("creadorId");
                                if (creadorId == null) creadorId = "anonimo";

                                double lat = 0;
                                double lon = 0;
                                if (doc.contains("latitud")) lat = doc.getDouble("latitud");
                                if (doc.contains("longitud")) lon = doc.getDouble("longitud");

                                // 🔥 CORRECCIÓN AQUÍ: Pasamos los 6 argumentos al constructor
                                Community comm = new Community(nombre, desc, fotoUrl, lat, lon, creadorId);

                                // 🔥 IMPORTANTE: Guardamos el ID del documento para poder editarlo después
                                comm.setId(doc.getId());

                                communityList.add(comm);

                            } catch (Exception e) {
                                Log.e("FIREBASE_ERROR", "Error leyendo: " + e.getMessage());
                            }
                        }
                        adapter.notifyDataSetChanged();
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
    }
}