package com.example.cunning_proyect;

import android.app.Dialog;
import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class IncidentsActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private ImageView imgPreviewPlaceholder;
    private Uri selectedImageUri;

    // Listas y Adapters (Sin base de datos)
    private ArrayList<Community> communityList = new ArrayList<>();
    private RecyclerView rvCommunities;
    private CommunityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities);

        // 1. Vincular vistas
        tvWelcome = findViewById(R.id.tvWelcomeUser);
        rvCommunities = findViewById(R.id.rvCommunities);

        // 2. Configurar nombre de usuario
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail != null && userEmail.contains("@")) {
            tvWelcome.setText("Hola, " + userEmail.split("@")[0]);
        } else {
            tvWelcome.setText("Hola, Usuario");
        }

        // 3. Configurar RecyclerView
        rvCommunities.setLayoutManager(new LinearLayoutManager(this));

        // Inicializamos la lista vacía (se borrará al cerrar la app)
        adapter = new CommunityAdapter(communityList);
        rvCommunities.setAdapter(adapter);

        // 4. Botón flotante para abrir el diálogo
        findViewById(R.id.btnAddCommunity).setOnClickListener(v -> showNewCommunityDialog());

        // 5. Lógica del Botón SALIR (Logout)
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent intent = new Intent(IncidentsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // Launcher para la galería
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

    private void showNewCommunityDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_new_community);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Vincular elementos del diálogo
        EditText etName = dialog.findViewById(R.id.etNewCommName);
        EditText etDesc = dialog.findViewById(R.id.etNewCommDesc); // <--- Aquí corregimos el error de antes

        Button btnGallery = dialog.findViewById(R.id.btnSelectImage);
        Button btnCreate = dialog.findViewById(R.id.btnCreate);
        imgPreviewPlaceholder = dialog.findViewById(R.id.imgPreview);

        selectedImageUri = null;

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            // Obtenemos la descripción correctamente
            String desc = etDesc.getText().toString().trim();

            if (!name.isEmpty()) {
                String imagePath = (selectedImageUri != null) ? selectedImageUri.toString() : "";
                if (desc.isEmpty()) desc = "Zona sin descripción";

                // Añadimos directamente a la lista visual (Solo memoria RAM)
                communityList.add(new Community(name, desc, imagePath));

                adapter.notifyItemInserted(communityList.size() - 1);
                rvCommunities.scrollToPosition(communityList.size() - 1);

                dialog.dismiss();
            } else {
                etName.setError("Escribe un nombre");
            }
        });

        dialog.show();
    }
}