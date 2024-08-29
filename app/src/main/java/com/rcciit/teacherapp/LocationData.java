package com.rcciit.teacherapp;


public class LocationData {
    private double latitude;
    private double longitude;
    private String name; // Add this line
    private String email; // Add this line
    private boolean withinRadius;
    private long lastUpdateTime;


    // Required empty constructor for Firebase
    public LocationData() {
    }

    // Constructor with parameters for your convenience
    public LocationData(double latitude, double longitude, String name, String email) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.email = email;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

}
