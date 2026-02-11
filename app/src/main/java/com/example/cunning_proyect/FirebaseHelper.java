package com.example.cunning_proyect;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // YA NO NECESITAMOS FIREBASE STORAGE
    // private FirebaseStorage storage;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public interface DataStatus {
        void onSuccess();
        void onError(String error);
    }

    // =================================================================
    // 1. CREAR COMUNIDAD (FOTO LOCAL)
    // =================================================================
    public void crearComunidad(String nombre, String descripcion, double lat, double lon, Uri fotoUri, DataStatus status) {
        // En lugar de subir, convertimos la URI local a String directamente
        String rutaFotoLocal = (fotoUri != null) ? fotoUri.toString() : "";

        guardarDatosComunidad(nombre, descripcion, lat, lon, rutaFotoLocal, status);
    }

    private void guardarDatosComunidad(String nombre, String desc, double lat, double lon, String urlFoto, DataStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("nombre", nombre);
        map.put("descripcion", desc);
        map.put("latitud", lat);
        map.put("longitud", lon);
        map.put("fotoUrl", urlFoto); // Guardamos la ruta local (file://...)
        map.put("creadorId", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonimo");
        map.put("fechaCreacion", System.currentTimeMillis());

        db.collection("comunidades").add(map)
                .addOnSuccessListener(doc -> status.onSuccess())
                .addOnFailureListener(e -> status.onError(e.getMessage()));
    }

    // =================================================================
    // 2. CREAR INCIDENCIA (FOTO LOCAL)
    // =================================================================
    public void crearIncidencia(String titulo, String descripcion, double lat, double lon, Uri fotoUri, String commId, int selectedUrgency, DataStatus status) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String id = db.collection("incidencias").document().getId();

        Map<String, Object> datos = new HashMap<>();
        datos.put("id", id);
        datos.put("titulo", titulo);
        datos.put("descripcion", descripcion);
        datos.put("latitud", lat);
        datos.put("longitud", lon);
        datos.put("comunidadId", commId); // 🔥 Guardamos el ID de la comunidad
        datos.put("urgencia", selectedUrgency); // 🔥 Guardamos el nivel (1, 2 o 3)
        datos.put("timestamp", System.currentTimeMillis());

        if (fotoUri != null) {
            // Lógica de subir foto a Storage (si la tienes) y luego guardar
            subirFotoYGuardar(fotoUri, datos, status);
        } else {
            db.collection("incidencias").document(id).set(datos)
                    .addOnSuccessListener(aVoid -> status.onSuccess())
                    .addOnFailureListener(e -> status.onError(e.getMessage()));
        }
    }

    private void guardarDatosIncidencia(String titulo, String desc, double lat, double lon, String urlFoto, DataStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("titulo", titulo);
        map.put("descripcion", desc);
        map.put("latitud", lat);
        map.put("longitud", lon);
        map.put("fotoUrl", urlFoto); // Ruta local
        map.put("usuarioId", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonimo");
        map.put("estado", "pendiente");
        map.put("fecha", System.currentTimeMillis());

        db.collection("incidencias").add(map)
                .addOnSuccessListener(doc -> status.onSuccess())
                .addOnFailureListener(e -> status.onError(e.getMessage()));
    }
    // --- MÉTODO PARA EDITAR COMUNIDAD ---
    public void editarComunidad(String docId, String nuevoNombre, String nuevaDesc, DataStatus status) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("nombre", nuevoNombre);
        updateMap.put("descripcion", nuevaDesc);

        db.collection("comunidades").document(docId)
                .update(updateMap)
                .addOnSuccessListener(aVoid -> status.onSuccess())
                .addOnFailureListener(e -> status.onError(e.getMessage()));
    }
    // 🔥 MÉTODO NUEVO PARA SUBIR LA FOTO A STORAGE Y LUEGO GUARDAR LOS DATOS 🔥
    private void subirFotoYGuardar(android.net.Uri fotoUri, java.util.Map<String, Object> datos, DataStatus status) {
        // 1. Creamos una referencia en Firebase Storage con un nombre único para la foto
        com.google.firebase.storage.StorageReference storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                .getReference().child("fotos_incidencias/" + System.currentTimeMillis() + ".jpg");

        // 2. Subimos el archivo
        storageRef.putFile(fotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 3. Si se sube bien, pedimos la URL pública de la foto
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                        // 4. Añadimos la URL de la foto a los datos de la incidencia
                        datos.put("fotoUrl", uri.toString());

                        // 5. Guardamos todo por fin en Firestore
                        String id = (String) datos.get("id");
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("incidencias").document(id).set(datos)
                                .addOnSuccessListener(aVoid -> status.onSuccess())
                                .addOnFailureListener(e -> status.onError(e.getMessage()));

                    }).addOnFailureListener(e -> status.onError("Error al obtener enlace: " + e.getMessage()));
                })
                .addOnFailureListener(e -> status.onError("Error al subir foto: " + e.getMessage()));
    }
}