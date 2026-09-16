package com.example.womensafety.model;

/**
 * Represents one group member's last known location.
 * Mirrors the structure stored under /groups/{groupCode}/members/{uid} in Firebase.
 * Needs a public no-arg constructor for Firebase's automatic deserialization.
 */
public class MemberLocation {

    private String email;
    private double latitude;
    private double longitude;
    private long updatedAt;

    public MemberLocation() {
        // required by Firebase
    }

    public MemberLocation(String email, double latitude, double longitude, long updatedAt) {
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return email + "  (" + String.format("%.5f", latitude) + ", "
                + String.format("%.5f", longitude) + ")";
    }
}
