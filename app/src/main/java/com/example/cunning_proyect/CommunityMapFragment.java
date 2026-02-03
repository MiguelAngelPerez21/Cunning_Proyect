package com.example.cunning_proyect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityMapFragment extends Fragment {

    private GoogleMap mMap;
    private Dialog creationDialog;
    private DatabaseHelper db; // Base de Datos

    // Variables temporales formulario
    private int selectedUrgency = 2;
    private LatLng selectedLocation = null;
    private Bitmap selectedBitmap = null;
    private boolean isPickingLocation = false;
    private ImageView imgEvidencePreview;
    private String currentCommunityName = "Unknown";

    // --- REQUISITO PSP: HILOS EN SEGUNDO PLANO ---
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Modelo de datos interno para el marcador
    private static class IncidentData {
        String title, desc;
        int urgency;
        Bitmap image; // Para mostrar en RAM
        String imagePath; // Ruta en disco (BD)
        int votesUp = 0;
        int votesDown = 0;

        IncidentData(String t, String d, int u, Bitmap i, String path) {
            title = t; desc = d; urgency = u; image = i; imagePath = path;
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1 && result.getData() != null) {
                    if(imgEvidencePreview != null) imgEvidencePreview.setImageURI(result.getData().getData());
                    // Convertir URI a Bitmap para poder guardarlo luego
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), result.getData().getData());
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1 && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        selectedBitmap = (Bitmap) extras.get("data");
                        if(imgEvidencePreview != null) imgEvidencePreview.setImageBitmap(selectedBitmap);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) openCamera();
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar BD
        db = new DatabaseHelper(requireContext());

        // Obtener nombre de la comunidad actual
        if (getArguments() != null) {
            currentCommunityName = getArguments().getString("COMM_NAME", "Unknown");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(callback);

        view.findViewById(R.id.btnReportIncident).setOnClickListener(v -> {
            selectedUrgency = 2;
            if (mMap != null) selectedLocation = mMap.getCameraPosition().target;
            selectedBitmap = null;
            showIncidentDialog();
        });
    }

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            mMap = googleMap;

            // Centrar mapa en la comunidad
            double lat = 40.4168;
            double lon = -3.7038;
            if (getArguments() != null) {
                lat = getArguments().getDouble("ARG_LAT", 40.4168);
                lon = getArguments().getDouble("ARG_LON", -3.7038);
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));

            // CARGAR INCIDENCIAS DE LA BD (PMDM)
            loadIncidentsFromDB();

            mMap.setOnMapClickListener(latLng -> {
                if (isPickingLocation) {
                    selectedLocation = latLng;
                    isPickingLocation = false;
                    creationDialog.show();
                    updateCoordinatesText(creationDialog);
                }
            });

            mMap.setOnMarkerClickListener(marker -> {
                Object tag = marker.getTag();
                if (tag instanceof IncidentData) showDetailSheet((IncidentData) tag);
                return true;
            });
        }
    };

    // --- CARGAR DATOS DE SQLITE ---
    private void loadIncidentsFromDB() {
        Cursor cursor = db.getIncidentsForCommunity(currentCommunityName);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Leer datos de la columna (ajusta índices según tu DatabaseHelper)
                // Orden en CREATE: id, title, description, urgency, latitude, longitude, imageUri, communityName...
                String title = cursor.getString(1);
                String desc = cursor.getString(2);
                int urgency = cursor.getInt(3);
                double lat = cursor.getDouble(4);
                double lon = cursor.getDouble(5);
                String imgPath = cursor.getString(6);

                // Cargar imagen de disco si existe
                Bitmap bmp = null;
                if (imgPath != null && !imgPath.isEmpty()) {
                    File imgFile = new File(imgPath);
                    if(imgFile.exists()){
                        bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    }
                }

                // Añadir al mapa
                LatLng loc = new LatLng(lat, lon);
                float hue = BitmapDescriptorFactory.HUE_ORANGE;
                if(urgency == 1) hue = BitmapDescriptorFactory.HUE_GREEN;
                if(urgency == 3) hue = BitmapDescriptorFactory.HUE_RED;

                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(loc)
                        .title(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));

                if (m != null) {
                    m.setTag(new IncidentData(title, desc, urgency, bmp, imgPath));
                }

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void showIncidentDialog() {
        if (creationDialog == null) {
            creationDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            creationDialog.setContentView(R.layout.dialog_new_incident);
        }

        Button btnLow = creationDialog.findViewById(R.id.btnLow);
        Button btnMid = creationDialog.findViewById(R.id.btnMid);
        Button btnHigh = creationDialog.findViewById(R.id.btnHigh);
        Button btnPickOnMap = creationDialog.findViewById(R.id.btnPickOnMap);

        View.OnClickListener urgencyListener = v -> {
            btnLow.setAlpha(0.5f); btnMid.setAlpha(0.5f); btnHigh.setAlpha(0.5f);
            v.setAlpha(1.0f);
            if (v == btnLow) selectedUrgency = 1;
            else if (v == btnMid) selectedUrgency = 2;
            else selectedUrgency = 3;
        };
        btnLow.setOnClickListener(urgencyListener);
        btnMid.setOnClickListener(urgencyListener);
        btnHigh.setOnClickListener(urgencyListener);
        btnMid.performClick();

        if (btnPickOnMap != null) {
            btnPickOnMap.setOnClickListener(v -> {
                isPickingLocation = true;
                creationDialog.hide();
                Toast.makeText(getContext(), "Toca el lugar exacto en el mapa", Toast.LENGTH_LONG).show();
            });
        }

        imgEvidencePreview = creationDialog.findViewById(R.id.imgEvidencePreview);
        LinearLayout layoutPhoto = creationDialog.findViewById(R.id.layoutPhoto);
        if (layoutPhoto != null) layoutPhoto.setOnClickListener(v -> showPhotoOptions());

        Button btnPublish = creationDialog.findViewById(R.id.btnPublishInc);

        // --- LÓGICA DE PUBLICAR CON HILOS ---
        btnPublish.setOnClickListener(v -> {
            EditText etTitle = creationDialog.findViewById(R.id.etIncTitle);
            EditText etDesc = creationDialog.findViewById(R.id.etIncDesc);
            String title = etTitle.getText().toString();
            String desc = etDesc.getText().toString();

            if(title.isEmpty()) { etTitle.setError("Requerido"); return; }

            // Si hay foto, guardar en segundo plano (PSP)
            if (selectedBitmap != null) {
                Toast.makeText(getContext(), "Procesando imagen...", Toast.LENGTH_SHORT).show();
                saveImageAndPublish(selectedBitmap, title, desc);
            } else {
                // Sin foto, guardar directo
                saveIncidentToDBAndMap(title, desc, "", selectedLocation);
            }
        });

        creationDialog.findViewById(R.id.btnClose).setOnClickListener(v -> creationDialog.dismiss());
        creationDialog.findViewById(R.id.btnCancelInc).setOnClickListener(v -> creationDialog.dismiss());

        updateCoordinatesText(creationDialog);
        creationDialog.show();
    }

    // --- HILO EN SEGUNDO PLANO PARA GUARDAR FOTO ---
    private void saveImageAndPublish(Bitmap bitmap, String title, String desc) {
        executorService.execute(() -> {
            try {
                // Operación pesada de E/S
                File path = new File(requireContext().getFilesDir(), "incidents");
                if (!path.exists()) path.mkdirs();

                String fileName = "INC_" + System.currentTimeMillis() + ".jpg";
                File file = new File(path, fileName);

                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos); // Compresión
                fos.close();

                String savedPath = file.getAbsolutePath();

                // Volver al hilo principal para actualizar UI y BD
                mainHandler.post(() -> {
                    saveIncidentToDBAndMap(title, desc, savedPath, selectedLocation);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(getContext(), "Error guardando imagen", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveIncidentToDBAndMap(String title, String desc, String imgPath, LatLng loc) {
        // 1. Guardar en SQLite
        boolean success = db.addIncident(title, desc, selectedUrgency, loc.latitude, loc.longitude, imgPath, currentCommunityName);

        if (success) {
            // 2. Añadir al mapa visualmente
            addMarkerToMap(title, desc, selectedUrgency, loc, imgPath);
            creationDialog.dismiss();
            creationDialog = null;
            Toast.makeText(getContext(), "Incidencia publicada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error BD", Toast.LENGTH_SHORT).show();
        }
    }

    private void addMarkerToMap(String title, String desc, int urgency, LatLng loc, String imgPath) {
        if (loc == null) return;
        float hue;
        if (urgency == 1) hue = BitmapDescriptorFactory.HUE_GREEN;
        else if (urgency == 2) hue = BitmapDescriptorFactory.HUE_ORANGE;
        else hue = BitmapDescriptorFactory.HUE_RED;

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(loc)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));

        if (marker != null) marker.setTag(new IncidentData(title, desc, urgency, selectedBitmap, imgPath));
    }

    private void updateCoordinatesText(Dialog dialog) {
        TextView tvLat = dialog.findViewById(R.id.tvLat);
        TextView tvLon = dialog.findViewById(R.id.tvLon);
        if (tvLat != null && selectedLocation != null) {
            tvLat.setText(String.format("Lat: %.4f", selectedLocation.latitude));
            tvLon.setText(String.format("Lon: %.4f", selectedLocation.longitude));
        }
    }

    private void showPhotoOptions() {
        String[] options = {"Hacer Foto", "Elegir de Galería"};
        new AlertDialog.Builder(getContext())
                .setTitle("Evidencia")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera();
                        else requestCameraPermission.launch(Manifest.permission.CAMERA);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);
                    }
                }).show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void showDetailSheet(IncidentData data) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        sheet.setContentView(R.layout.dialog_incident_detail);

        TextView t = sheet.findViewById(R.id.tvDetailTitle);
        TextView desc = sheet.findViewById(R.id.tvDetailDesc);
        TextView urg = sheet.findViewById(R.id.tvDetailUrgency);
        ImageView img = sheet.findViewById(R.id.imgDetailEvidence);
        TextView tvUp = sheet.findViewById(R.id.tvVoteUpCount);
        TextView tvDown = sheet.findViewById(R.id.tvVoteDownCount);
        View btnUp = sheet.findViewById(R.id.btnVoteUp);
        View btnDown = sheet.findViewById(R.id.btnVoteDown);

        t.setText(data.title);
        desc.setText(data.desc);
        tvUp.setText("👍 Sí (" + data.votesUp + ")");
        tvDown.setText("👎 No (" + data.votesDown + ")");

        if (data.urgency == 1) {
            urg.setText("URGENCIA BAJA"); urg.setBackgroundResource(R.drawable.bg_urgency_low);
        } else if (data.urgency == 2) {
            urg.setText("URGENCIA MEDIA"); urg.setBackgroundResource(R.drawable.bg_urgency_medium);
        } else {
            urg.setText("URGENCIA ALTA"); urg.setBackgroundResource(R.drawable.bg_urgency_high);
        }

        if (data.image != null) {
            img.setImageBitmap(data.image);
            img.setVisibility(View.VISIBLE);
        } else {
            // Si no hay bitmap en RAM, intentar cargar de disco
            if (data.imagePath != null && !data.imagePath.isEmpty()) {
                File imgFile = new File(data.imagePath);
                if(imgFile.exists()) {
                    img.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                    img.setVisibility(View.VISIBLE);
                } else img.setVisibility(View.GONE);
            } else img.setVisibility(View.GONE);
        }

        btnUp.setOnClickListener(v -> {
            data.votesUp++;
            tvUp.setText("👍 Sí (" + data.votesUp + ")");
        });

        btnDown.setOnClickListener(v -> {
            data.votesDown++;
            tvDown.setText("👎 No (" + data.votesDown + ")");
        });

        sheet.findViewById(R.id.btnCloseDetail).setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }
}