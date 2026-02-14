package com.example.physiokalendar.entity;

public enum Role {
    ADMIN,      // Vollzugriff auf alles
    RECEPTION,  // Kann Termine und Patienten verwalten, aber keine Therapeuten
    THERAPIST   // Kann nur eigene Termine sehen
}
