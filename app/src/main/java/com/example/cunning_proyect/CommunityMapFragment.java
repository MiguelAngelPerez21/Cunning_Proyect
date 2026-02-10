package com.example.cunning_proyect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CommunityMapFragment extends Fragment {

    private GoogleMap mMap;
    private Dialog creationDialog;
    private FirebaseHelper firebaseHelper;
    private FirebaseFirestore db;

    // Datos de la comunidad actual (Recibidos al hacer clic en la lista)
    private String commId;
    private String commCreatorId;
    private String commName;

    // Variables para crear incidencias
    private int selectedUrgency = 2;
    private LatLng selectedLocation = null;
    private Uri selectedUri = null;
    private boolean isPickingLocation = false;
    private ImageView imgEvidencePreview;

    // Modelo interno para los marcadores
    private static class IncidentData {
        String title, desc;
        int urgency;
        String imageUriLocal;
        IncidentData(String t, String d, int u, String uri) {
            title = t; desc = d; urgency = u; imageUriLocal = uri;
        }
    }

    // --- LAUNCHERS PARA FOTOS (Cámara y Galería) ---
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1 && result.getData() != null) {
                    selectedUri = result.getData().getData();
                    if(imgEvidencePreview != null) imgEvidencePreview.setImageURI(selectedUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1 && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap bmp = (Bitmap) extras.get("data");
                        if(imgEvidencePreview != null) imgEvidencePreview.setImageBitmap(bmp);
                        selectedUri = saveBitmapLocally(requireContext(), bmp);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> { if (isGranted) openCamera(); }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Asegúrate de usar el XML que tiene la papelera (el que te pasé antes)
        return inflater.inflate(R.layout.fragment_community_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseHelper = new FirebaseHelper();
        db = FirebaseFirestore.getInstance();

        // 1. RECUPERAR DATOS DE LA COMUNIDAD (Pasados desde el Adapter)
        if (getArguments() != null) {
            commId = getArguments().getString("COMM_ID");
            commCreatorId = getArguments().getString("COMM_CREATOR");
            commName = getArguments().getString("COMM_NAME");
        }

        // 2. CONFIGURAR EL BOTÓN DE BORRAR (PAPELERA)
        FloatingActionButton btnDelete = view.findViewById(R.id.btnDeleteCommunity);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonimo";

        // 🔥 LÓGICA CLAVE: SOLO EL CREADOR VE LA PAPELERA 🔥
        if (commCreatorId != null && commCreatorId.equals(currentUserId)) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDeleteCommunity());
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        // 3. INICIALIZAR EL MAPA
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(callback);

        // Botón de crear incidencia (+)
        view.findViewById(R.id.btnReportIncident).setOnClickListener(v -> {
            selectedUrgency = 2;
            if (mMap != null) selectedLocation = mMap.getCameraPosition().target;
            selectedUri = null;
            showIncidentDialog();
        });
    }

    // --- LOGICA DEL MAPA ---
    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            mMap = googleMap;

            // Centrar mapa en la comunidad seleccionada (si hay coordenadas)
            if (getArguments() != null) {
                double lat = getArguments().getDouble("COMM_LAT", 0);
                double lon = getArguments().getDouble("COMM_LON", 0);
                if (lat != 0 && lon != 0) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));
                } else {
                    // Si no tiene coordenadas, vamos a Madrid
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6));
                }
            }

            // Cargar incidencias (Pines en el mapa)
            loadIncidentsFromFirebase();

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

    // --- CARGAR INCIDENCIAS DE FIREBASE ---
    private void loadIncidentsFromFirebase() {
        db.collection("incidencias").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mMap.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String title = doc.getString("titulo");
                            String desc = doc.getString("descripcion");
                            Double lat = doc.getDouble("latitud");
                            Double lon = doc.getDouble("longitud");
                            String photoUrl = doc.getString("fotoUrl");
                            int urgency = 2; // Por defecto

                            if (lat != null && lon != null) {
                                Marker m = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(lat, lon))
                                        .title(title)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                                if (m != null) {
                                    m.setTag(new IncidentData(title, desc, urgency, photoUrl));
                                }
                            }
                        } catch (Exception e) {
                            Log.e("MAP", "Error loading marker: " + e.getMessage());
                        }
                    }
                });
    }

    // --- BORRAR COMUNIDAD ---
    private void confirmDeleteCommunity() {
        new AlertDialog.Builder(getContext())
                .setTitle("¿Eliminar Comunidad?")
                .setMessage("Se borrará para siempre. ¿Estás seguro?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (commId != null) {
                        db.collection("comunidades").document(commId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Comunidad borrada 🗑️", Toast.LENGTH_SHORT).show();
                                    // Volver atrás (cerrar mapa y volver a lista)
                                    if (getActivity() != null) {
                                        getActivity().getSupportFragmentManager().popBackStack();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al borrar", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- CREAR INCIDENCIA (Tu código original) ---
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
                Toast.makeText(getContext(), "Toca el mapa donde está la incidencia", Toast.LENGTH_LONG).show();
            });
        }

        imgEvidencePreview = creationDialog.findViewById(R.id.imgEvidencePreview);
        LinearLayout layoutPhoto = creationDialog.findViewById(R.id.layoutPhoto);
        if (layoutPhoto != null) layoutPhoto.setOnClickListener(v -> showPhotoOptions());

        Button btnPublish = creationDialog.findViewById(R.id.btnPublishInc);

        btnPublish.setOnClickListener(v -> {
            EditText etTitle = creationDialog.findViewById(R.id.etIncTitle);
            EditText etDesc = creationDialog.findViewById(R.id.etIncDesc);
            String title = etTitle.getText().toString();
            String desc = etDesc.getText().toString();

            if(title.isEmpty()) { etTitle.setError("Requerido"); return; }
            if(selectedLocation == null) { Toast.makeText(getContext(), "Elige ubicación en mapa", Toast.LENGTH_SHORT).show(); return; }

            firebaseHelper.crearIncidencia(title, desc, selectedLocation.latitude, selectedLocation.longitude, selectedUri, new FirebaseHelper.DataStatus() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Incidencia Publicada", Toast.LENGTH_SHORT).show();
                    loadIncidentsFromFirebase();
                    creationDialog.dismiss();
                    creationDialog = null;
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        creationDialog.findViewById(R.id.btnClose).setOnClickListener(v -> creationDialog.dismiss());
        creationDialog.findViewById(R.id.btnCancelInc).setOnClickListener(v -> creationDialog.dismiss());
        updateCoordinatesText(creationDialog);
        creationDialog.show();
    }

    // --- ÚTILES (Foto y Detalles) ---
    private Uri saveBitmapLocally(Context context, Bitmap bitmap) {
        File file = new File(context.getExternalFilesDir(null), "INC_" + System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush(); fos.close();
            return Uri.fromFile(file);
        } catch (IOException e) { return null; }
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
        String[] options = {"Cámara", "Galería"};
        new AlertDialog.Builder(getContext()).setTitle("Adjuntar foto")
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
        ImageView img = sheet.findViewById(R.id.imgDetailEvidence);

        t.setText(data.title);
        desc.setText(data.desc);

        if (data.imageUriLocal != null && !data.imageUriLocal.isEmpty()) {
            try {
                img.setImageURI(Uri.parse(data.imageUriLocal));
                img.setVisibility(View.VISIBLE);
            } catch (Exception e) { img.setVisibility(View.GONE); }
        } else { img.setVisibility(View.GONE); }

        sheet.findViewById(R.id.btnCloseDetail).setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }
}