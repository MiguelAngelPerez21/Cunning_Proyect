package com.example.cunning_proyect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CommunityDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvDesc;
    private ImageView imgHeader;
    private FloatingActionButton btnEdit, btnDelete;

    // Variables de datos
    private String commId, commCreatorId, commName, commDesc;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_detail);

        firebaseHelper = new FirebaseHelper();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonimo";

        // Referencias
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDesc = findViewById(R.id.tvDetailDesc);
        imgHeader = findViewById(R.id.imgDetailHeader);
        btnEdit = findViewById(R.id.fabEditCommunity);
        btnDelete = findViewById(R.id.fabDeleteCommunity);

        // Recibimos los datos
        if (getIntent() != null) {
            commId = getIntent().getStringExtra("COMM_ID");
            commCreatorId = getIntent().getStringExtra("COMM_CREATOR");
            commName = getIntent().getStringExtra("COMM_NAME");
            // Parche por si el nombre de la variable en el adapter era distinto
            if (commName == null) commName = getIntent().getStringExtra("COMMUNITY_NAME");

            commDesc = getIntent().getStringExtra("COMM_DESC");
            String photoUrl = getIntent().getStringExtra("COMM_PHOTO");

            tvTitle.setText(commName);
            tvDesc.setText(commDesc);

            // --- CARGA DE IMAGEN SEGURA (Anti-Crash) ---
            if (photoUrl != null && !photoUrl.isEmpty()) {
                try {
                    imgHeader.setImageURI(Uri.parse(photoUrl));
                } catch (Exception e) {
                    // Si falla (permisos o ruta rota), ponemos imagen por defecto
                    imgHeader.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                // Si no había foto, ponemos la por defecto
                imgHeader.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // --- VISIBILIDAD DE BOTONES ---
        // ¿Soy yo el creador?
        if (commCreatorId != null && commCreatorId.equals(currentUserId)) {
            // SI -> Muestro los botones
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // NO -> Los escondo (GONE hace que desaparezcan del mapa, no ocupan sitio)
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        // Listeners
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    // ... (El resto de métodos showEditDialog y confirmDelete igual que antes) ...

    private void showEditDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_new_community);

        Button btnSave = dialog.findViewById(R.id.btnCreate);
        btnSave.setText("Guardar");
        EditText etName = dialog.findViewById(R.id.etNewCommName);
        EditText etDesc = dialog.findViewById(R.id.etNewCommDesc);

        etName.setText(commName);
        etDesc.setText(commDesc);

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString();
            String newDesc = etDesc.getText().toString();
            firebaseHelper.editarComunidad(commId, newName, newDesc, new FirebaseHelper.DataStatus() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CommunityDetailActivity.this, "Actualizado", Toast.LENGTH_SHORT).show();
                    tvTitle.setText(newName);
                    tvDesc.setText(newDesc);
                    dialog.dismiss();
                }
                @Override
                public void onError(String error) { }
            });
        });
        dialog.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("¿Eliminar Comunidad?")
                .setMessage("Esta acción es permanente.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection("comunidades").document(commId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Eliminada", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}