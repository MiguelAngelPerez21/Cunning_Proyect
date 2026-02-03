package com.example.cunning_proyect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// IMPORTS FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // INICIALIZAR
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText etUser = findViewById(R.id.etRegUsername);
        EditText etEmail = findViewById(R.id.etRegEmail);
        EditText etPass = findViewById(R.id.etRegPassword);
        Button btnRegister = findViewById(R.id.btnDoRegister);
        Button btnBack = findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(v -> {
            String username = etUser.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Rellena todo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. CREAR USUARIO EN AUTHENTICATION (EMAIL/PASS)
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Usuario creado en Auth, ahora guardamos el nombre en Firestore
                            FirebaseUser user = mAuth.getCurrentUser();
                            guardarDatosEnFirestore(user.getUid(), username, email);
                        } else {
                            Toast.makeText(this, "Fallo al registrar: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void guardarDatosEnFirestore(String uid, String username, String email) {
        // Creamos un "mapa" de datos
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("role", "user"); // Por si en el futuro quieres admins

        // Guardamos en la colección "users", documento con ID del usuario
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
                    finish(); // Volver al login
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Usuario creado, pero error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}