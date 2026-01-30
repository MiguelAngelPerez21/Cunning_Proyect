package com.example.cunning_proyect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString();

            if (!isValidEmail(email)) {
                etEmail.setError("Email inválido");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(pass) || pass.length() < 4) {
                etPassword.setError("Mínimo 4 caracteres");
                etPassword.requestFocus();
                return;
            }


            Intent intent = new Intent(this, IncidentsActivity.class);
            intent.putExtra("USER_EMAIL", email);
            startActivity(intent);

            Toast.makeText(this, "Login correcto", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
