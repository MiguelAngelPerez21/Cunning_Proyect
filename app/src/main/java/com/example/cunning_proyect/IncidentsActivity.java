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

    private DatabaseHelper dbHelper;


    // Listas y Adapters
    private ArrayList<Community> communityList = new ArrayList<>();
    private RecyclerView rvCommunities;
    private CommunityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_communities);

        dbHelper = new DatabaseHelper(this);


        tvWelcome = findViewById(R.id.tvWelcomeUser);
        rvCommunities = findViewById(R.id.rvCommunities);


        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail != null && userEmail.contains("@")) {
            tvWelcome.setText("Hola, " + userEmail.split("@")[0]);
        } else {
            tvWelcome.setText("Hola, Usuario");
        }

        rvCommunities.setLayoutManager(new LinearLayoutManager(this));

        //se carga la base de datos con las comunidades

        communityList = dbHelper.getAllCommunities();

        adapter = new CommunityAdapter(communityList);
        rvCommunities.setAdapter(adapter);


        rvCommunities.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityAdapter(communityList);
        rvCommunities.setAdapter(adapter);


        findViewById(R.id.btnAddCommunity).setOnClickListener(v -> showNewCommunityDialog());
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


        EditText etName = dialog.findViewById(R.id.etNewCommName);
        EditText etDesc = dialog.findViewById(R.id.etNewCommDesc); // <--- ¡ESTA LÍNEA FALTABA!
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
            String desc = etDesc.getText().toString().trim();

            if (!name.isEmpty()) {
                String imagePath = (selectedImageUri != null) ? selectedImageUri.toString() : "";
                if (desc.isEmpty()) desc = "Zona sin descripción";

                // GUARDAR EN BASE DE DATOS
                boolean success = dbHelper.addCommunity(name, desc, imagePath);

                if (success) {
                    // ACTUALIZAR LA LISTA VISUAL
                    communityList.add(new Community(name, desc, imagePath));
                    adapter.notifyItemInserted(communityList.size() - 1);
                    rvCommunities.scrollToPosition(communityList.size() - 1);

                    Toast.makeText(this, "Comunidad guardada", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            } else {
                etName.setError("Escribe un nombre");
            }
        });

        dialog.show();
    }
}