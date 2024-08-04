package com.example.physiokalendar.entity;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Therapist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "active_since")
    private Date activeSince;

    @Column(name = "active_until")
    private Date activeUntil;

    @OneToMany(mappedBy = "therapist")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "therapist")
    private List<AppointmentSeries> appointmentSeries;

    @OneToMany(mappedBy = "therapist")
    private List<Absence> absences;

    @OneToMany(mappedBy = "therapist")
    private List<Exception> exceptions;

    // Getter und Setter werden durch Lombok generiert
}
