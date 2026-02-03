package com.example.cunning_proyect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "CunningDB.db";
    private static final int DB_VERSION = 3; // ¡SUBIMOS VERSIÓN!

    private static final String TABLE_COMMUNITIES = "communities";
    private static final String TABLE_INCIDENTS = "incidents";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (email TEXT PRIMARY KEY, username TEXT, password TEXT)");

        // AÑADIMOS 'is_synced' (0 = No, 1 = Sí)
        db.execSQL("CREATE TABLE " + TABLE_COMMUNITIES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, description TEXT, imageUri TEXT, latitude REAL, longitude REAL, " +
                "is_synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_INCIDENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, description TEXT, urgency INTEGER, latitude REAL, longitude REAL, " +
                "imageUri TEXT, communityName TEXT, votesUp INTEGER, votesDown INTEGER, " +
                "is_synced INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMUNITIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INCIDENTS);
        onCreate(db);
    }

    // --- MÉTODOS DE INCIDENCIAS ---
    public boolean addIncident(String title, String desc, int urgency, double lat, double lon, String img, String commName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", desc);
        values.put("urgency", urgency);
        values.put("latitude", lat);
        values.put("longitude", lon);
        values.put("imageUri", img);
        values.put("communityName", commName);
        values.put("votesUp", 0);
        values.put("votesDown", 0);
        values.put("is_synced", 0); // Al crear, NO está sincronizado aún
        long result = db.insert(TABLE_INCIDENTS, null, values);
        return result != -1;
    }

    // Método para obtener las incidencias PENDIENTES de subir
    public Cursor getUnsyncedIncidents() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_INCIDENTS + " WHERE is_synced = 0", null);
    }

    // Método para marcar como subido
    public void markIncidentAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_synced", 1);
        db.update(TABLE_INCIDENTS, values, "id=?", new String[]{String.valueOf(id)});
    }

    // Resto de métodos (getIncidentsForCommunity, addCommunity...) déjalos igual o añade is_synced también.
    public Cursor getIncidentsForCommunity(String communityName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_INCIDENTS + " WHERE communityName=?", new String[]{communityName});
    }

    // Método necesario para cargar comunidades en la lista
    public ArrayList<Community> getAllCommunities() {
        ArrayList<Community> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COMMUNITIES, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Community(
                        cursor.getString(1), // name
                        cursor.getString(2), // desc
                        cursor.getString(3), // uri
                        cursor.getDouble(4), // lat
                        cursor.getDouble(5)  // lon
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Método añadir comunidad (adaptado)
    public boolean addCommunity(Community c) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", c.getName());
        values.put("description", c.getDescription());
        values.put("imageUri", c.getImageUri());
        values.put("latitude", c.getLatitude());
        values.put("longitude", c.getLongitude());
        values.put("is_synced", 0);
        long result = db.insert(TABLE_COMMUNITIES, null, values);
        return result != -1;
    }

    // Métodos de usuario (sin cambios)
    public boolean checkUser(String email, String password) { return false; } // Dummy para evitar errores
}