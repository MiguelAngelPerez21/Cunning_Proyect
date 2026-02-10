package com.example.cunning_proyect;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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

    // UI del Menú Inferior
    private CardView btnNavComm, btnNavSupport;
    private ImageView iconSupport;
    private TextView textSupport;

    // --- FIREBASE HELPER (Tu conexión a la nube) ---
    private FirebaseHelper firebaseHelper;

    // Variables para la creación de comunidad (Dialog)
    private Uri selectedImageUri; // Guarda la foto seleccionada
    private double pickedLat = 40.4168; // Latitud por defecto (Madrid)
    private double pickedLon = -3.7038; // Longitud por defecto

    // Referencias a la UI del Dialog (para actualizar tras seleccionar foto/mapa)
    private ImageView imgPreviewPlaceholder;
    private TextView tvCoordsPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities); // Asegúrate de que este sea tu layout principal

        // 1. INICIALIZAMOS FIREBASE
        firebaseHelper = new FirebaseHelper();

        // 2. Referencias del Menú Inferior
        btnNavComm = findViewById(R.id.navBtnCommunities);
        btnNavSupport = findViewById(R.id.navBtnSupport);
        iconSupport = findViewById(R.id.iconSupport);
        textSupport = findViewById(R.id.textSupport);

        // 3. Cargar Fragmento Inicial (Comunidades)
        if (savedInstanceState == null) {
            loadFragment(new CommunitiesFragment());
            updateMenuUI(true);
        }

        // 4. LISTENERS DEL MENÚ (Cambio de pantallas)
        btnNavComm.setOnClickListener(v -> {
            updateMenuUI(true);
            loadFragment(new CommunitiesFragment());
        });

        btnNavSupport.setOnClickListener(v -> {
            updateMenuUI(false);
            loadFragment(new SupportFragment());
        });
    }

    // Método para cambiar el fragmento central
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // Método visual para pintar los botones del menú
    private void updateMenuUI(boolean isCommunitiesActive) {
        if (isCommunitiesActive) {
            btnNavComm.setCardBackgroundColor(Color.parseColor("#2563EB")); // Azul activo
            btnNavSupport.setCardBackgroundColor(Color.parseColor("#1F2937")); // Gris inactivo
            if(iconSupport != null) iconSupport.setColorFilter(Color.parseColor("#888888"));
            if(textSupport != null) textSupport.setTextColor(Color.parseColor("#888888"));
        } else {
            btnNavComm.setCardBackgroundColor(Color.parseColor("#1F2937")); // Gris inactivo
            btnNavSupport.setCardBackgroundColor(Color.parseColor("#2563EB")); // Azul activo
            if(iconSupport != null) iconSupport.setColorFilter(Color.parseColor("#FFFFFF"));
            if(textSupport != null) textSupport.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    // ================================================================
    //  MÉTODO PÚBLICO: MOSTRAR DIÁLOGO DE CREAR COMUNIDAD
    //  (Llamado desde CommunitiesFragment al pulsar el botón +)
    // ================================================================
    public void showNewCommunityDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_new_community);

        // Fondo transparente para que se vea bonito
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Referencias dentro del diálogo
        EditText etName = dialog.findViewById(R.id.etNewCommName);
        EditText etDesc = dialog.findViewById(R.id.etNewCommDesc);
        Button btnMap = dialog.findViewById(R.id.btnPickLocation);
        tvCoordsPreview = dialog.findViewById(R.id.tvSelectedCoords);
        Button btnGallery = dialog.findViewById(R.id.btnSelectImage);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);
        imgPreviewPlaceholder = dialog.findViewById(R.id.imgPreview);

        // Reiniciamos variables temporales
        selectedImageUri = null;
        pickedLat = 40.4168;
        pickedLon = -3.7038;

        // --- BOTÓN MAPA ---
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(IncidentsActivity.this, PickLocationActivity.class);
            mapPickerLauncher.launch(intent);
        });

        // --- BOTÓN GALERÍA ---
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // --- BOTÓN CREAR (GUARDAR EN FIREBASE) ---
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (!name.isEmpty()) {
                if (desc.isEmpty()) desc = "Sin descripción";

                // Feedback visual: Desactivar botón y mostrar mensaje
                btnCreate.setEnabled(false);
                btnCreate.setText("Subiendo...");
                Toast.makeText(this, "Subiendo a la nube... ☁️", Toast.LENGTH_SHORT).show();

                // 🚀 LLAMADA AL HELPER: Guarda Texto + GPS + Foto
                firebaseHelper.crearComunidad(name, desc, pickedLat, pickedLon, selectedImageUri, new FirebaseHelper.DataStatus() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(IncidentsActivity.this, "¡Comunidad Creada! ✅", Toast.LENGTH_SHORT).show();

                        // Refrescar la lista en el fragmento si está visible
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                        if (current instanceof CommunitiesFragment) {
                            ((CommunitiesFragment) current).loadCommunities();
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        btnCreate.setEnabled(true);
                        btnCreate.setText("Crear Comunidad");
                        Toast.makeText(IncidentsActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                etName.setError("El nombre es obligatorio");
            }
        });

        dialog.show();
    }

    // --- LAUNCHER PARA SELECCIONAR FOTO ---
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (imgPreviewPlaceholder != null) {
                        imgPreviewPlaceholder.setImageURI(selectedImageUri);
                        imgPreviewPlaceholder.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    // --- LAUNCHER PARA SELECCIONAR UBICACIÓN ---
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pickedLat = result.getData().getDoubleExtra("LAT", 40.4168);
                    pickedLon = result.getData().getDoubleExtra("LON", -3.7038);
                    if (tvCoordsPreview != null) {
                        tvCoordsPreview.setText("📍 Ubicación guardada");
                        tvCoordsPreview.setTextColor(Color.parseColor("#4CAF50")); // Verde
                    }
                }
            }
    );
}