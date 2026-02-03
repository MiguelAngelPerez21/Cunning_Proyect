package com.example.cunning_proyect;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseSyncHelper {

    private DatabaseHelper dbHelper;
    private FirebaseFirestore firestore;
    private Context context;

    public FirebaseSyncHelper(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void syncIncidents() {
        // 1. CHEQUEO DE SEGURIDAD: SI NO HAY INTERNET, NO HACEMOS NADA
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d("Sync", "No hay internet. Sincronización pospuesta.");
            return; // Salimos silenciosamente. Los datos siguen seguros en SQLite.
        }

        // 2. Si hay internet, procedemos
        Cursor cursor = dbHelper.getUnsyncedIncidents();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0); // ID local
                // ... (recuperar resto de datos, asegúrate que los índices coinciden con tu tabla) ...
                // Un truco seguro es usar getColumnIndex si dudas del orden
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                int urgency = cursor.getInt(cursor.getColumnIndexOrThrow("urgency"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                String imgUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"));
                String commName = cursor.getString(cursor.getColumnIndexOrThrow("communityName"));

                Map<String, Object> incidentMap = new HashMap<>();
                incidentMap.put("title", title);
                incidentMap.put("description", desc);
                incidentMap.put("urgency", urgency);
                incidentMap.put("latitude", lat);
                incidentMap.put("longitude", lon);
                incidentMap.put("community", commName);
                incidentMap.put("local_image_path", imgUri);
                incidentMap.put("timestamp", System.currentTimeMillis());

                firestore.collection("incidents")
                        .add(incidentMap)
                        .addOnSuccessListener(documentReference -> {
                            // ÉXITO: Marcamos como subido en SQLite
                            dbHelper.markIncidentAsSynced(id);
                            Log.d("Sync", "Incidencia subida: " + title);
                        })
                        .addOnFailureListener(e -> {
                            // FALLO SILENCIOSO: No molestamos al usuario con Toasts.
                            // Simplemente se queda en SQLite y se intentará luego.
                            Log.e("Sync", "Error subiendo: " + e.getMessage());
                        });

            } while (cursor.moveToNext());
            cursor.close();
        }
    }
}