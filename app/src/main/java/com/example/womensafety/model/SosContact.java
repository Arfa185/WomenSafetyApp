package com.example.womensafety.model;

/**
 * A pre-defined emergency contact stored under /users/{uid}/contacts/{pushId}.
 */
public class SosContact {

    private String name;
    private String phone;

    public SosContact() {
        // required by Firebase
    }

    public SosContact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return name + " - " + phone;
    }
}
