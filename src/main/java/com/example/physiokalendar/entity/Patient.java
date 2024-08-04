package com.example.physiokalendar.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(name = "active_since")
    private Date activeSince;

    @Column(name = "active_until")
    private Date activeUntil;

    @Column(name = "is_bwo")
    private Boolean isBWO;

    // Getter und Setter
}
