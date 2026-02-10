package com.example.cunning_proyect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CunningDB.db";
    private static final int DATABASE_VERSION = 4; // Subimos versión por los cambios

    // TABLA COMUNIDADES
    public static final String TABLE_COMMUNITIES = "communities";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_DESC = "description";
    public static final String COL_LAT = "latitude";
    public static final String COL_LON = "longitude";
    public static final String COL_PHOTO = "photoUri";
    public static final String COL_SYNC = "sync_status"; // 0: Pendiente, 1: Sincronizado

    // TABLA INCIDENCIAS
    public static final String TABLE_INCIDENTS = "incidents";
    public static final String COL_TITLE = "title";
    // Usamos las mismas columnas LAT, LON, PHOTO, SYNC...

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear Tabla Comunidades
        String createComm = "CREATE TABLE " + TABLE_COMMUNITIES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_DESC + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_PHOTO + " TEXT, " +
                COL_SYNC + " INTEGER DEFAULT 0)"; // Por defecto 0 (No sincronizado)
        db.execSQL(createComm);

        // Crear Tabla Incidencias
        String createInc = "CREATE TABLE " + TABLE_INCIDENTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_DESC + " TEXT, " +
                COL_LAT + " REAL, " +
                COL_LON + " REAL, " +
                COL_PHOTO + " TEXT, " +
                COL_SYNC + " INTEGER DEFAULT 0)";
        db.execSQL(createInc);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMUNITIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INCIDENTS);
        onCreate(db);
    }

    // --- MÉTODOS PARA GUARDAR (SIEMPRE LOCAL PRIMERO) ---

    public long addCommunity(String name, String desc, double lat, double lon, String photoUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_DESC, desc);
        values.put(COL_LAT, lat);
        values.put(COL_LON, lon);
        values.put(COL_PHOTO, photoUri);
        values.put(COL_SYNC, 0); // 0 = Pendiente de subir a Firebase
        return db.insert(TABLE_COMMUNITIES, null, values);
    }

    public long addIncident(String title, String desc, double lat, double lon, String photoUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_DESC, desc);
        values.put(COL_LAT, lat);
        values.put(COL_LON, lon);
        values.put(COL_PHOTO, photoUri);
        values.put(COL_SYNC, 0); // 0 = Pendiente
        return db.insert(TABLE_INCIDENTS, null, values);
    }

    // --- MÉTODOS PARA LEER (LO QUE SE MUESTRA EN LA APP) ---

    public Cursor getAllCommunities() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Leemos TODO (Sincronizado y No Sincronizado) para que el usuario vea lo suyo al momento
        return db.rawQuery("SELECT * FROM " + TABLE_COMMUNITIES, null);
    }

    public Cursor getAllIncidents() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_INCIDENTS, null);
    }

    // --- MÉTODOS PARA LA SINCRONIZACIÓN (SYNC) ---

    // Obtener solo lo que falta por subir (Sync = 0)
    public Cursor getUnsyncedCommunities() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_COMMUNITIES + " WHERE " + COL_SYNC + " = 0", null);
    }

    public Cursor getUnsyncedIncidents() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_INCIDENTS + " WHERE " + COL_SYNC + " = 0", null);
    }

    // Marcar como subido (Sync = 1)
    public void markCommunityAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNC, 1);
        db.update(TABLE_COMMUNITIES, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void markIncidentAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNC, 1);
        db.update(TABLE_INCIDENTS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }
}