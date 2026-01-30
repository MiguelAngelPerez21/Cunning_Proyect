package com.example.cunning_proyect;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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

public class CommunityMapFragment extends Fragment {

    private GoogleMap mMap;

    // Variables temporales para el formulario
    private Dialog creationDialog;
    private int selectedUrgency = 2; // 1=Baja, 2=Media, 3=Alta
    private LatLng selectedLocation = null;
    private Bitmap selectedBitmap = null;
    private boolean isPickingLocation = false; // Controla si estamos eligiendo punto en el mapa

    // Vista previa de imagen en el formulario
    private ImageView imgEvidencePreview;

    // Clase simple para guardar datos de la incidencia dentro del marcador
    private static class IncidentData {
        String title, desc;
        int urgency;
        Bitmap image;

        IncidentData(String t, String d, int u, Bitmap i) {
            title = t; desc = d; urgency = u; image = i;
        }
    }

    // 1. Launcher Galería
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1 && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if(imgEvidencePreview != null) {
                        imgEvidencePreview.setImageURI(imageUri);
                        // Nota: Para guardarlo bien deberías convertir Uri a Bitmap,
                        // pero visualmente esto ya funciona.
                    }
                }
            }
    );

    // 2. Launcher Cámara
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

    // 3. Launcher Permiso Cámara
    private final ActivityResultLauncher<String> requestCameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) openCamera();
                else Toast.makeText(getContext(), "Permiso denegado", Toast.LENGTH_SHORT).show();
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

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(callback);

        // Botón azul grande "Reportar Incidencia"
        view.findViewById(R.id.btnReportIncident).setOnClickListener(v -> {
            selectedUrgency = 2; // Reiniciar a Media
            if (mMap != null) selectedLocation = mMap.getCameraPosition().target;
            selectedBitmap = null;
            showIncidentDialog();
        });
    }

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            mMap = googleMap;
            LatLng madrid = new LatLng(40.416775, -3.703790);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 12));

            // CLICK EN EL MAPA (Para seleccionar ubicación)
            mMap.setOnMapClickListener(latLng -> {
                if (isPickingLocation) {
                    selectedLocation = latLng;
                    isPickingLocation = false;
                    creationDialog.show(); // Volvemos a mostrar el diálogo
                    updateCoordinatesText(creationDialog);
                }
            });

            // CLICK EN UN MARCADOR (Ver detalles)
            mMap.setOnMarkerClickListener(marker -> {
                Object tag = marker.getTag();
                if (tag instanceof IncidentData) {
                    showDetailDialog((IncidentData) tag);
                }
                return true;
            });
        }
    };

    // --- LÓGICA DEL FORMULARIO DE CREACIÓN ---
    private void showIncidentDialog() {
        if (creationDialog == null) {
            creationDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            creationDialog.setContentView(R.layout.dialog_new_incident);
        }

        // Elementos UI
        Button btnLow = creationDialog.findViewById(R.id.btnLow);
        Button btnMid = creationDialog.findViewById(R.id.btnMid);
        Button btnHigh = creationDialog.findViewById(R.id.btnHigh);

        // CORREGIDO: Usamos el ID correcto que pusimos en el XML anterior
        Button btnPickOnMap = creationDialog.findViewById(R.id.btnPickOnMap);

        // 1. Gestión de Urgencia (Visual)
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

        // Seleccionar Media por defecto
        btnMid.performClick();

        // 2. Gestión de Ubicación "En Mapa"
        if (btnPickOnMap != null) {
            btnPickOnMap.setOnClickListener(v -> {
                isPickingLocation = true;
                creationDialog.hide(); // Ocultamos temporalmente para ver el mapa
                Toast.makeText(getContext(), "Toca el lugar exacto en el mapa", Toast.LENGTH_LONG).show();
            });
        }

        // 3. Gestión de Foto
        imgEvidencePreview = creationDialog.findViewById(R.id.imgEvidencePreview);
        LinearLayout layoutPhoto = creationDialog.findViewById(R.id.layoutPhoto);

        if (layoutPhoto != null) {
            layoutPhoto.setOnClickListener(v -> showPhotoOptions());
        }

        // 4. Publicar
        Button btnPublish = creationDialog.findViewById(R.id.btnPublishInc);
        btnPublish.setOnClickListener(v -> {
            EditText etTitle = creationDialog.findViewById(R.id.etIncTitle);
            EditText etDesc = creationDialog.findViewById(R.id.etIncDesc);

            String title = etTitle.getText().toString();
            if(title.isEmpty()) { etTitle.setError("Requerido"); return; }

            // Añadir al mapa
            addMarkerToMap(title, etDesc.getText().toString(), selectedUrgency, selectedLocation);

            creationDialog.dismiss();
            creationDialog = null; // Limpiar
        });

        // Botones de Cerrar
        creationDialog.findViewById(R.id.btnClose).setOnClickListener(v -> creationDialog.dismiss());
        creationDialog.findViewById(R.id.btnCancelInc).setOnClickListener(v -> creationDialog.dismiss());

        updateCoordinatesText(creationDialog);
        creationDialog.show();
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
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void addMarkerToMap(String title, String desc, int urgency, LatLng loc) {
        if (loc == null) return;

        float hue;
        if (urgency == 1) hue = BitmapDescriptorFactory.HUE_GREEN;
        else if (urgency == 2) hue = BitmapDescriptorFactory.HUE_ORANGE;
        else hue = BitmapDescriptorFactory.HUE_RED;

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(loc)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));

        if (marker != null) {
            marker.setTag(new IncidentData(title, desc, urgency, selectedBitmap));
        }
    }

    private void showDetailDialog(IncidentData data) {
        Dialog d = new Dialog(getContext());
        d.setContentView(R.layout.dialog_incident_detail);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView t = d.findViewById(R.id.tvDetailTitle);
        TextView desc = d.findViewById(R.id.tvDetailDesc);
        TextView urg = d.findViewById(R.id.tvDetailUrgency);
        ImageView img = d.findViewById(R.id.imgDetailEvidence);

        t.setText(data.title);
        desc.setText(data.desc);

        if (data.urgency == 1) {
            urg.setText("Urgencia BAJA");
            urg.setBackgroundColor(Color.parseColor("#2ECC71"));
        } else if (data.urgency == 2) {
            urg.setText("Urgencia MEDIA");
            urg.setBackgroundColor(Color.parseColor("#F1C40F"));
        } else {
            urg.setText("Urgencia ALTA");
            urg.setBackgroundColor(Color.parseColor("#E74C3C"));
        }

        if (data.image != null) {
            img.setImageBitmap(data.image);
            img.setVisibility(View.VISIBLE);
        } else {
            img.setVisibility(View.GONE);
        }

        d.findViewById(R.id.btnCloseDetail).setOnClickListener(v -> d.dismiss());
        d.show();
    }
}