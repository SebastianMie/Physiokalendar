package com.example.physiokalendar.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "therapist_id")
    private Therapist therapist;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "start_time")
    private Date startTime;

    @Column(name = "end_time")
    private Date endTime;

    @Column(name = "date")
    private Date date;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_by_series_appointment")
    private Boolean createdBySeriesAppointment;

    @Column(name = "is_hotair")
    private Boolean isHotair;

    @Column(name = "is_ultrasonic")
    private Boolean isUltrasonic;

    @Column(name = "is_electric")
    private Boolean isElectric;

    // Getter und Setter werden durch Lombok generiert
}
