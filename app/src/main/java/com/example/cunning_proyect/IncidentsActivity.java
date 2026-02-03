package com.example.cunning_proyect;

import android.app.Dialog;
import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class IncidentsActivity extends AppCompatActivity {

    // UI
    private TextView tvWelcome;
    private RecyclerView rvCommunities;
    private CommunityAdapter adapter;

    // UI del Diálogo (variables temporales)
    private ImageView imgPreviewPlaceholder;
    private TextView tvCoordsPreview;

    // Lógica y Datos
    private DatabaseHelper db;            // Base de datos Local (SQLite)
    private FirebaseSyncHelper syncHelper; // Sincronizador Nube (Firestore)
    private ArrayList<Community> communityList = new ArrayList<>();

    // Variables temporales para crear comunidad
    private Uri selectedImageUri;
    private double pickedLat = 40.4168; // Madrid por defecto
    private double pickedLon = -3.7038;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities);

        // 1. INICIALIZAR AYUDANTES
        db = new DatabaseHelper(this);
        syncHelper = new FirebaseSyncHelper(this);

        // 2. VINCULAR VISTAS
        tvWelcome = findViewById(R.id.tvWelcomeUser);
        rvCommunities = findViewById(R.id.rvCommunities);
        rvCommunities.setLayoutManager(new LinearLayoutManager(this));

        // 3. RECIBIR EMAIL DEL LOGIN (Intent)
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail != null) {
            String userName = userEmail.split("@")[0];
            tvWelcome.setText("Hola, " + userName.substring(0, 1).toUpperCase() + userName.substring(1));
        }

        // 4. CARGAR DATOS LOCALES
        loadCommunitiesFromDB();

        // 5. LISTENERS BOTONES
        findViewById(R.id.btnAddCommunity).setOnClickListener(v -> showNewCommunityDialog());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // Cerrar sesión y volver
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // --- ESTRATEGIA OFFLINE FIRST ---
        // 1. Intentar subir datos pendientes a la nube (si hay internet)
        syncHelper.syncIncidents();

        // 2. Refrescar la lista local por si algo cambió
        loadCommunitiesFromDB();
    }

    /**
     * Carga las comunidades desde SQLite para que sea instantáneo
     */
    private void loadCommunitiesFromDB() {
        communityList = db.getAllCommunities();

        // Si la BD está vacía (primera vez), creamos una de ejemplo
        if (communityList.isEmpty()) {
            Community defaultComm = new Community("Madrid Centro", "Zona centro de ejemplo", "", 40.4168, -3.7038);
            // Guardamos en SQLite
            db.addCommunity(defaultComm);
            // Añadimos a la lista visual
            communityList.add(defaultComm);
        }

        if (adapter == null) {
            adapter = new CommunityAdapter(communityList);
            rvCommunities.setAdapter(adapter);
        } else {
            // Si ya existe el adaptador, solo actualizamos los datos para no parpadear
            // (Idealmente crearías un método updateList en el adapter, pero esto funciona recreándolo)
            adapter = new CommunityAdapter(communityList);
            rvCommunities.setAdapter(adapter);
        }
    }

    // --- LAUNCHERS DE RESULTADOS ---

    // 1. Selector de Galería
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

    // 2. Selector de Mapa (PickLocationActivity)
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    pickedLat = result.getData().getDoubleExtra("LAT", 40.4168);
                    pickedLon = result.getData().getDoubleExtra("LON", -3.7038);

                    if (tvCoordsPreview != null) {
                        tvCoordsPreview.setText(String.format("Ubicación: %.4f, %.4f", pickedLat, pickedLon));
                        tvCoordsPreview.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                    }
                }
            }
    );

    /**
     * Muestra el diálogo para crear una nueva comunidad
     */
    private void showNewCommunityDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_new_community);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Referencias del layout del diálogo
        EditText etName = dialog.findViewById(R.id.etNewCommName);
        EditText etDesc = dialog.findViewById(R.id.etNewCommDesc);

        Button btnMap = dialog.findViewById(R.id.btnPickLocation);
        tvCoordsPreview = dialog.findViewById(R.id.tvSelectedCoords);

        Button btnGallery = dialog.findViewById(R.id.btnSelectImage);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);
        imgPreviewPlaceholder = dialog.findViewById(R.id.imgPreview);

        // Resetear variables temporales
        selectedImageUri = null;
        pickedLat = 40.4168;
        pickedLon = -3.7038;

        // Acción: Abrir Mapa
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(IncidentsActivity.this, PickLocationActivity.class);
            mapPickerLauncher.launch(intent);
        });

        // Acción: Abrir Galería
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Acción: CREAR
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (!name.isEmpty()) {
                if (desc.isEmpty()) desc = "Sin descripción";
                String imgPath = (selectedImageUri != null) ? selectedImageUri.toString() : "";

                // Crear objeto
                Community newComm = new Community(name, desc, imgPath, pickedLat, pickedLon);

                // 1. GUARDAR EN LOCAL (SQLite)
                if (db.addCommunity(newComm)) {
                    // Actualizar UI
                    loadCommunitiesFromDB();
                    dialog.dismiss();
                    Toast.makeText(this, "Comunidad creada (Local)", Toast.LENGTH_SHORT).show();

                    // 2. INTENTAR SUBIR A LA NUBE (Segundo plano)
                    syncHelper.syncIncidents();
                    // Nota: Si implementaste syncCommunities en el Helper, llámalo aquí también.
                } else {
                    Toast.makeText(this, "Error al guardar en base de datos", Toast.LENGTH_SHORT).show();
                }

            } else {
                etName.setError("Nombre requerido");
            }
        });

        dialog.show();
    }
}