package com.example.cunning_proyect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// 1. IMPORTAR FIREBASE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // Variable para gestionar Auth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. INICIALIZAR FIREBASE AUTH
        mAuth = FirebaseAuth.getInstance();

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPass = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        // 3. COMPROBAR SI YA ESTABA LOGUEADO (AUTO-LOGIN)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            irAPantallaPrincipal(currentUser.getEmail());
        }

        // 4. BOTÓN LOGIN (AHORA PROTEGIDO)
        btnLogin.setOnClickListener(v -> {

            // --- NUEVO: COMPROBACIÓN DE INTERNET ---
            // Si no hay red, mostramos aviso y cortamos (return)
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "⚠ No tienes conexión a internet", Toast.LENGTH_SHORT).show();
                return;
            }
            // ---------------------------------------

            String email = etEmail.getText().toString();
            String pass = etPass.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Rellena los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // MÉTODOS NATIVOS DE FIREBASE PARA LOGIN
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // ¡Éxito!
                            FirebaseUser user = mAuth.getCurrentUser();
                            irAPantallaPrincipal(user.getEmail());
                        } else {
                            // Fallo
                            String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                            Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    private void irAPantallaPrincipal(String email) {
        Intent intent = new Intent(MainActivity.this, IncidentsActivity.class);
        intent.putExtra("USER_EMAIL", email);
        startActivity(intent);
        finish(); // Cierra el login para que no se pueda volver atrás
    }
}