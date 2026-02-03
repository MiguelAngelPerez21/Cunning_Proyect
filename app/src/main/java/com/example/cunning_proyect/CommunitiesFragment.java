package com.example.cunning_proyect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import android.app.Activity;

// Copia aquí la lógica de visualización de comunidades
public class CommunitiesFragment extends Fragment {

    private RecyclerView rvCommunities;
    private CommunityAdapter adapter;
    private DatabaseHelper db;
    private ArrayList<Community> communityList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_communities_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new DatabaseHelper(requireContext());
        rvCommunities = view.findViewById(R.id.rvCommunities);
        rvCommunities.setLayoutManager(new LinearLayoutManager(getContext()));

        loadCommunities();

        // Botón flotante para añadir (el "+" azul de tu foto)
        view.findViewById(R.id.btnFloatingAdd).setOnClickListener(v -> {
            if(getActivity() instanceof IncidentsActivity) {
                ((IncidentsActivity) getActivity()).showNewCommunityDialog();
            }
        });
    }

    // Método público para refrescar desde la Activity principal
    public void loadCommunities() {
        communityList = db.getAllCommunities();
        // Si está vacía, creamos la de ejemplo
        if (communityList.isEmpty()) {
            Community defaultComm = new Community("Madrid Centro", "Incidencias en M-30", "", 40.4168, -3.7038);
            db.addCommunity(defaultComm);
            communityList.add(defaultComm);
        }
        adapter = new CommunityAdapter(communityList);
        rvCommunities.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCommunities();
    }
}