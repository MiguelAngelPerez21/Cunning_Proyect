package com.example.cunning_proyect;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cunning_db";
    private static final int DATABASE_VERSION = 1;

    // Nombre de la tabla y columnas
    private static final String TABLE_COMMUNITIES = "communities";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_DESC = "description";
    private static final String COL_IMAGE = "image_uri";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creamos la tabla cuando se instala la app por primera vez
        String createTable = "CREATE TABLE " + TABLE_COMMUNITIES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_DESC + " TEXT, " +
                COL_IMAGE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMUNITIES);
        onCreate(db);
    }


    public boolean addCommunity(String name, String description, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_DESC, description);
        values.put(COL_IMAGE, imageUri);

        long result = db.insert(TABLE_COMMUNITIES, null, values);
        return result != -1; // Devuelve true si se guardó bien
    }


    public ArrayList<Community> getAllCommunities() {
        ArrayList<Community> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COMMUNITIES, null);

        if (cursor.moveToFirst()) {
            do {
                // Sacamos los datos de las columnas
                // (Los índices dependen del orden en el CREATE TABLE: 1=Name, 2=Desc, 3=Image)
                String name = cursor.getString(1);
                String desc = cursor.getString(2);
                String img = cursor.getString(3);

                list.add(new Community(name, desc, img));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}