package com.example.cunning_proyect;

public class Community {
    private String name;
    private String description;
    private String imageUri; // Guardaremos la ruta de la imagen

    public Community(String name, String description, String imageUri) {
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUri() { return imageUri; }
}