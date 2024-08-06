package com.example.physiokalendar.entity;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AppointmentSeries {

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

    private String comment;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    private Integer weeklyfrequency;

    @Column(name = "is_bwo")
    private Boolean isBWO;

    @ManyToMany
    @JoinTable(name = "appointment_series_has_cancellation", joinColumns = @JoinColumn(name = "appointment_series_id"), inverseJoinColumns = @JoinColumn(name = "cancellation_id"))
    private List<Cancellation> cancellations;

    // Getter und Setter werden durch Lombok generiert
}
