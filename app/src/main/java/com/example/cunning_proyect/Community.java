package com.example.cunning_proyect;

public class Community {
    private String name;
    private String description;
    private String imageUri;
    private double latitude;
    private double longitude;

    // Constructor actualizado con lat/lon
    public Community(String name, String description, String imageUri, double latitude, double longitude) {
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUri() { return imageUri; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}