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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CommunityMapFragment extends Fragment {

    private GoogleMap mMap;
    private Dialog creationDialog;
    private FirebaseHelper firebaseHelper;
    private FirebaseFirestore db;

    private String commId;
    private String commCreatorId;
    private String commName;

    private int selectedUrgency = 2;
    private LatLng selectedLocation = null;
    private Uri selectedUri = null;
    private boolean isPickingLocation = false;
    private ImageView imgEvidencePreview;

    private TextView tvActiveMapCount;

    private static class IncidentData {
        String title, desc;
        int urgency;
        String imageUriLocal;
        IncidentData(String t, String d, int u, String uri) {
            title = t; desc = d; urgency = u; imageUriLocal = uri;
        }
    }

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

                        // Creamos un hilo paralelo para que comprimir y guardar la foto no congele la UI
                        new Thread(() -> {
                            Uri savedUri = saveBitmapLocally(requireContext(), bmp);
                            // Volvemos al hilo principal para actualizar la variable
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    selectedUri = savedUri;
                                });
                            }
                        }).start();
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
        return inflater.inflate(R.layout.fragment_community_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(super.getContext() != null ? view : null, savedInstanceState);

        firebaseHelper = new FirebaseHelper();
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            commId = getArguments().getString("COMM_ID");
            commCreatorId = getArguments().getString("COMM_CREATOR");
            commName = getArguments().getString("COMM_NAME");
        }

        TextView tvTitle = view.findViewById(R.id.tvDetailCommName);
        if (tvTitle != null && commName != null) {
            tvTitle.setText(commName);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        ImageView btnDelete = view.findViewById(R.id.btnDeleteCommunity);
        ImageView btnEdit = view.findViewById(R.id.btnEditCommunity);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonimo";

        if (commCreatorId != null && commCreatorId.equals(currentUserId)) {
            btnDelete.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDeleteCommunity());
            btnEdit.setOnClickListener(v -> showEditCommunityDialog(tvTitle));
        } else {
            btnDelete.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
        }

        Button btnAction = view.findViewById(R.id.btnReportIncident);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("comunidades").document(commId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                ArrayList<String> miembros = (ArrayList<String>) doc.get("miembros");
                if (miembros != null && miembros.contains(userId)) {
                    btnAction.setText("+ Reportar Incidencia");
                    btnAction.setOnClickListener(v -> {
                        selectedUrgency = 2;
                        if (mMap != null) selectedLocation = mMap.getCameraPosition().target;
                        selectedUri = null;
                        showIncidentDialog();
                    });
                } else {
                    btnAction.setText("Unirse a la Comunidad");
                    btnAction.setOnClickListener(v -> {
                        db.collection("comunidades").document(commId)
                                .update("miembros", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                                    getActivity().recreate();
                                });
                    });
                }
            }
        });

        View btnTabChat = view.findViewById(R.id.btnTabChat);
        if (btnTabChat != null) {
            btnTabChat.setOnClickListener(v -> {
                Fragment chatFragment = new CommunityChatFragment();
                Bundle args = new Bundle();
                args.putString("COMM_ID", commId);
                args.putString("COMM_NAME", commName);
                chatFragment.setArguments(args);
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, chatFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        tvActiveMapCount = view.findViewById(R.id.tvActiveMapCount);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(callback);
    }

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            mMap = googleMap;
            if (getArguments() != null) {
                double lat = getArguments().getDouble("COMM_LAT", 0);
                double lon = getArguments().getDouble("COMM_LON", 0);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));
            }
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

    private void loadIncidentsFromFirebase() {
        db.collection("incidencias").get().addOnSuccessListener(queryDocumentSnapshots -> {
            mMap.clear();
            int activas = 0;
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    String title = doc.getString("titulo");
                    String desc = doc.getString("descripcion");
                    Double lat = doc.getDouble("latitud");
                    Double lon = doc.getDouble("longitud");
                    String photoUrl = doc.getString("fotoUrl");
                    Long urgLong = doc.getLong("urgencia");
                    int urgency = urgLong != null ? urgLong.intValue() : 2;
                    if(urgency == 3) activas++;
                    if (lat != null && lon != null) {
                        float color = (urgency == 3) ? BitmapDescriptorFactory.HUE_RED : (urgency == 1 ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_ORANGE);
                        Marker m = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title(title).icon(BitmapDescriptorFactory.defaultMarker(color)));
                        if (m != null) m.setTag(new IncidentData(title, desc, urgency, photoUrl));
                    }
                } catch (Exception e) { Log.e("MAP", "Error: " + e.getMessage()); }
            }
            if(tvActiveMapCount != null) tvActiveMapCount.setText(String.valueOf(activas));
        });
    }

    private void confirmDeleteCommunity() {
        new AlertDialog.Builder(getContext()).setTitle("¿Eliminar?").setMessage("Se borrará para siempre.")
                .setPositiveButton("Eliminar", (d, w) -> db.collection("comunidades").document(commId).delete().addOnSuccessListener(a -> getActivity().onBackPressed()))
                .setNegativeButton("Cancelar", null).show();
    }

    private void showIncidentDialog() {
        if (creationDialog == null) {
            creationDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            creationDialog.setContentView(R.layout.dialog_new_incident);
        }

        Button btnLow = creationDialog.findViewById(R.id.btnLow);
        Button btnMid = creationDialog.findViewById(R.id.btnMid);
        Button btnHigh = creationDialog.findViewById(R.id.btnHigh);

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

        imgEvidencePreview = creationDialog.findViewById(R.id.imgEvidencePreview);

        // Botón para seleccionar ubicación o abrir cámara (si tienes el botón de foto aquí)
        creationDialog.findViewById(R.id.btnPickOnMap).setOnClickListener(v -> {
            isPickingLocation = true;
            creationDialog.hide();
            Toast.makeText(getContext(), "Toca el mapa", Toast.LENGTH_SHORT).show();
        });

        // (Opcional) Si en tu diseño tienes un botón explícito para la foto, llámalo así:
        // creationDialog.findViewById(R.id.btnTakePhoto).setOnClickListener(v -> openCamera());

        creationDialog.findViewById(R.id.btnPublishInc).setOnClickListener(v -> {
            EditText etTitle = creationDialog.findViewById(R.id.etIncTitle);
            EditText etDesc = creationDialog.findViewById(R.id.etIncDesc);
            if(etTitle.getText().toString().isEmpty()) return;

            firebaseHelper.crearIncidencia(etTitle.getText().toString(), etDesc.getText().toString(),
                    selectedLocation.latitude, selectedLocation.longitude, selectedUri, commId, selectedUrgency, new FirebaseHelper.DataStatus() {
                        @Override public void onSuccess() { loadIncidentsFromFirebase(); creationDialog.dismiss(); }
                        @Override public void onError(String error) {}
                    });
        });

        creationDialog.findViewById(R.id.btnClose).setOnClickListener(v -> creationDialog.dismiss());
        updateCoordinatesText(creationDialog);
        creationDialog.show();
    }

    private Uri saveBitmapLocally(Context context, Bitmap bitmap) {
        File file = new File(context.getExternalFilesDir(null), "INC_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
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

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void showDetailSheet(IncidentData data) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        sheet.setContentView(R.layout.dialog_incident_detail);
        ((TextView)sheet.findViewById(R.id.tvDetailTitle)).setText(data.title);
        ((TextView)sheet.findViewById(R.id.tvDetailDesc)).setText(data.desc);
        sheet.findViewById(R.id.btnCloseDetail).setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }

    private void showEditCommunityDialog(TextView tvTitle) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_community);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        EditText etName = dialog.findViewById(R.id.etEditCommName);
        EditText etDesc = dialog.findViewById(R.id.etEditCommDesc);
        etName.setText(commName);
        db.collection("comunidades").document(commId).get().addOnSuccessListener(doc -> { if(doc.exists()) etDesc.setText(doc.getString("descripcion")); });

        dialog.findViewById(R.id.btnSaveEdit).setOnClickListener(v -> {
            String n = etName.getText().toString();
            db.collection("comunidades").document(commId).update("nombre", n, "descripcion", etDesc.getText().toString())
                    .addOnSuccessListener(a -> { commName = n; tvTitle.setText(n); dialog.dismiss(); });
        });
        dialog.show();
    }
}