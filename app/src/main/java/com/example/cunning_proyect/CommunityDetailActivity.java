package com.example.cunning_proyect;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CommunityDetailActivity extends AppCompatActivity {

    private String communityName;
    private double commLat;
    private double commLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_detail);

        // 1. RECIBIMOS LOS DATOS
        communityName = getIntent().getStringExtra("COMMUNITY_NAME");
        commLat = getIntent().getDoubleExtra("COMM_LAT", 40.4168);
        commLon = getIntent().getDoubleExtra("COMM_LON", -3.7038);

        TextView tvHeader = findViewById(R.id.tvHeaderName);
        tvHeader.setText(communityName);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_map) {
                selectedFragment = new CommunityMapFragment();
            } else if (itemId == R.id.nav_chat) {
                selectedFragment = new CommunityChatFragment();
            }

            if (selectedFragment != null) {
                // Preparamos el paquete de datos para el fragmento
                Bundle args = new Bundle();
                args.putString("COMM_NAME", communityName);
                args.putDouble("ARG_LAT", commLat);
                args.putDouble("ARG_LON", commLon);
                selectedFragment.setArguments(args);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Carga inicial del mapa
        if (savedInstanceState == null) {
            Fragment mapFragment = new CommunityMapFragment();
            Bundle args = new Bundle();
            args.putString("COMM_NAME", communityName);
            args.putDouble("ARG_LAT", commLat);
            args.putDouble("ARG_LON", commLon);
            mapFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, mapFragment)
                    .commit();
        }
    }
}