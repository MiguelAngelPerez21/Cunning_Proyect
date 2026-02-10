package com.example.cunning_proyect;

import com.google.firebase.firestore.Exclude; // Importante para no guardar el ID dentro del documento dos veces

public class Community {
    private String id; // ID del documento (No se guarda en la BD, se asigna al bajarlo)
    private String nombre;
    private String descripcion;
    private String fotoUrl;
    private double latitud;
    private double longitud;
    private String creadorId; // 🔥 NUEVO: El ID del usuario que la creó

    public Community() { } // Constructor vacío obligatorio para Firebase

    public Community(String nombre, String descripcion, String fotoUrl, double lat, double lon, String creadorId) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fotoUrl = fotoUrl;
        this.latitud = lat;
        this.longitud = lon;
        this.creadorId = creadorId;
    }

    // --- GETTERS Y SETTERS ---

    @Exclude // Significa: "Firebase, ignora esto al subir, úsalo solo en la App"
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getCreadorId() { return creadorId; }
    public void setCreadorId(String creadorId) { this.creadorId = creadorId; }
}