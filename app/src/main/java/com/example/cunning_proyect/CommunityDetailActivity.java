package com.example.cunning_proyect;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CommunityDetailActivity extends AppCompatActivity {

    private String communityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_detail);

        // Recibir el nombre de la comunidad
        communityName = getIntent().getStringExtra("COMMUNITY_NAME");
        TextView tvHeader = findViewById(R.id.tvHeaderName);
        tvHeader.setText(communityName);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_map) {
                selectedFragment = new CommunityMapFragment();
            } else if (itemId == R.id.nav_chat) { // Ya no hay "else if info"
                selectedFragment = new CommunityChatFragment();
            }

            if (selectedFragment != null) {
                // Pasar el nombre de la comunidad al fragmento
                Bundle args = new Bundle();
                args.putString("COMM_NAME", communityName);
                selectedFragment.setArguments(args);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Cargar el mapa por defecto al abrir
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }
}