package com.example.cunning_proyect;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.widget.Toast;

public class SyncService {

    private DatabaseHelper dbHelper;
    private FirebaseHelper firebaseHelper;
    private Context context;

    public SyncService(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.firebaseHelper = new FirebaseHelper();
    }

    // Comprueba si hay internet
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // Llama a esto para intentar subir todo lo pendiente
    public void syncNow() {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "Sin conexión. Se guardó localmente 💾", Toast.LENGTH_SHORT).show();
            return;
        }

        syncCommunities();
        syncIncidents();
    }

    private void syncCommunities() {
        Cursor cursor = dbHelper.getUnsyncedCommunities();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0); // ID local
                String nombre = cursor.getString(1);
                String desc = cursor.getString(2);
                double lat = cursor.getDouble(3);
                double lon = cursor.getDouble(4);
                String fotoStr = cursor.getString(5);
                Uri fotoUri = (fotoStr != null && !fotoStr.isEmpty()) ? Uri.parse(fotoStr) : null;

                // Subir a Firebase
                firebaseHelper.crearComunidad(nombre, desc, lat, lon, fotoUri, new FirebaseHelper.DataStatus() {
                    @Override
                    public void onSuccess() {
                        // ¡Éxito! Marcamos como sincronizado en SQLite
                        dbHelper.markCommunityAsSynced(id);
                    }

                    @Override
                    public void onError(String error) {
                        // Falló, lo intentaremos la próxima vez. No hacemos nada.
                    }
                });
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void syncIncidents() {
        Cursor cursor = dbHelper.getUnsyncedIncidents();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String titulo = cursor.getString(1);
                String desc = cursor.getString(2);
                double lat = cursor.getDouble(3);
                double lon = cursor.getDouble(4);
                String fotoStr = cursor.getString(5);
                Uri fotoUri = (fotoStr != null && !fotoStr.isEmpty()) ? Uri.parse(fotoStr) : null;

                firebaseHelper.crearIncidencia(titulo, desc, lat, lon, fotoUri, new FirebaseHelper.DataStatus() {
                    @Override
                    public void onSuccess() {
                        dbHelper.markIncidentAsSynced(id);
                    }

                    @Override
                    public void onError(String error) {
                    }
                });
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}