package com.example.physiokalendar.entity;

import org.apache.poi.ss.formula.functions.Columns;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Exception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "therapist_id")
    private Therapist therapist;

    private String day;

    @Column(name = "start_time")

    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    // Getter und Setter werden durch Lombok generiert
}
