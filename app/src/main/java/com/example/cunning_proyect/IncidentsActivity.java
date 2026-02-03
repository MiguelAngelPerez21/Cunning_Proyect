package com.example.cunning_proyect;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class IncidentsActivity extends AppCompatActivity {

    private CardView btnNavComm, btnNavSupport;
    private ImageView iconSupport;
    private TextView textSupport;

    // Variables para crear comunidad
    private DatabaseHelper db;
    private FirebaseSyncHelper syncHelper;
    private Uri selectedImageUri;
    private double pickedLat = 40.4168;
    private double pickedLon = -3.7038;
    private ImageView imgPreviewPlaceholder;
    private TextView tvCoordsPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities); // Layout con el menú inferior

        db = new DatabaseHelper(this);
        syncHelper = new FirebaseSyncHelper(this);

        // Referencias del Menú Inferior
        btnNavComm = findViewById(R.id.navBtnCommunities);
        btnNavSupport = findViewById(R.id.navBtnSupport);
        iconSupport = findViewById(R.id.iconSupport);
        textSupport = findViewById(R.id.textSupport);

        // Cargar Fragmento Inicial
        loadFragment(new CommunitiesFragment());

        // LISTENERS DEL MENÚ
        btnNavComm.setOnClickListener(v -> {
            updateMenuUI(true);
            loadFragment(new CommunitiesFragment());
        });

        btnNavSupport.setOnClickListener(v -> {
            updateMenuUI(false);
            loadFragment(new SupportFragment());
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateMenuUI(boolean isCommunitiesActive) {
        if (isCommunitiesActive) {
            btnNavComm.setCardBackgroundColor(Color.parseColor("#2563EB"));
            btnNavSupport.setCardBackgroundColor(Color.parseColor("#1F2937"));
            if(iconSupport != null) iconSupport.setColorFilter(Color.parseColor("#888888"));
            if(textSupport != null) textSupport.setTextColor(Color.parseColor("#888888"));
        } else {
            btnNavComm.setCardBackgroundColor(Color.parseColor("#1F2937"));
            btnNavSupport.setCardBackgroundColor(Color.parseColor("#2563EB"));
            if(iconSupport != null) iconSupport.setColorFilter(Color.parseColor("#FFFFFF"));
            if(textSupport != null) textSupport.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    // --- MÉTODOS PÚBLICOS PARA QUE LOS LLAME EL FRAGMENTO ---

    // ¡IMPORTANTE! Debe ser PUBLIC para que CommunitiesFragment lo vea
    public void showNewCommunityDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_new_community);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etName = dialog.findViewById(R.id.etNewCommName);
        EditText etDesc = dialog.findViewById(R.id.etNewCommDesc);
        Button btnMap = dialog.findViewById(R.id.btnPickLocation);
        tvCoordsPreview = dialog.findViewById(R.id.tvSelectedCoords);
        Button btnGallery = dialog.findViewById(R.id.btnSelectImage);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);
        imgPreviewPlaceholder = dialog.findViewById(R.id.imgPreview);

        selectedImageUri = null;

        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(IncidentsActivity.this, PickLocationActivity.class);
            mapPickerLauncher.launch(intent);
        });

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (!name.isEmpty()) {
                if (desc.isEmpty()) desc = "Sin descripción";
                String imgPath = (selectedImageUri != null) ? selectedImageUri.toString() : "";

                Community newComm = new Community(name, desc, imgPath, pickedLat, pickedLon);

                if (db.addCommunity(newComm)) {
                    Toast.makeText(this, "Comunidad creada", Toast.LENGTH_SHORT).show();
                    syncHelper.syncIncidents();

                    // Recargar la lista del fragmento actual si es el de comunidades
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (current instanceof CommunitiesFragment) {
                        ((CommunitiesFragment) current).loadCommunities();
                    }

                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            } else {
                etName.setError("Nombre requerido");
            }
        });

        dialog.show();
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (imgPreviewPlaceholder != null) {
                        imgPreviewPlaceholder.setImageURI(selectedImageUri);
                        imgPreviewPlaceholder.setVisibility(android.view.View.VISIBLE);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pickedLat = result.getData().getDoubleExtra("LAT", 40.4168);
                    pickedLon = result.getData().getDoubleExtra("LON", -3.7038);
                    if (tvCoordsPreview != null) {
                        tvCoordsPreview.setText("Ubicación seleccionada");
                        tvCoordsPreview.setTextColor(Color.GREEN);
                    }
                }
            }
    );
}