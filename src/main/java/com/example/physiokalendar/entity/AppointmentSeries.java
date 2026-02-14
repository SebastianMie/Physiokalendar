package com.example.physiokalendar.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "appointment_series", indexes = {
    @Index(name = "idx_start_date", columnList = "start_date"),
    @Index(name = "idx_therapist", columnList = "therapist_id")
})
public class AppointmentSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    @JsonBackReference
    private Therapist therapist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonBackReference
    private Patient patient;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "weekly_frequency")
    @JsonProperty("weeklyFrequency")
    private Integer weeklyfrequency = 1;

    @Column(name = "week_day")
    private String weekday;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeriesStatus status = SeriesStatus.ACTIVE;

    @OneToMany(mappedBy = "appointmentSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Cancellation> cancellations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Transient getters for JSON serialization (therapist/patient have @JsonBackReference)
    @JsonProperty("therapistId")
    public Long getTherapistId() {
        return therapist != null ? therapist.getId() : null;
    }

    @JsonProperty("therapistName")
    public String getTherapistName() {
        return therapist != null ? therapist.getFullName() : null;
    }

    @JsonProperty("patientId")
    public Long getPatientId() {
        return patient != null ? patient.getId() : null;
    }

    @JsonProperty("patientName")
    public String getPatientName() {
        return patient != null ? patient.getFullName() : null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
