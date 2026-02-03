package com.example.cunning_proyect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class PickLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapPicker);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button btnConfirm = findViewById(R.id.btnConfirmLocation);
        btnConfirm.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LAT", selectedLocation.latitude);
                resultIntent.putExtra("LON", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Selecciona un punto en el mapa primero", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Centrar en Madrid por defecto
        LatLng madrid = new LatLng(40.4168, -3.7038);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 10));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Ubicación Seleccionada"));
            selectedLocation = latLng;
        });
    }
}