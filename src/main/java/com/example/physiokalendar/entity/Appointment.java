package com.example.physiokalendar.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "appointment", indexes = {
    @Index(name = "idx_date", columnList = "date"),
    @Index(name = "idx_therapist", columnList = "therapist_id"),
    @Index(name = "idx_patient", columnList = "patient_id")
})
public class Appointment {

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

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "is_hotair")
    private Boolean isHotair = false;

    @Column(name = "is_ultrasonic")
    private Boolean isUltrasonic = false;

    @Column(name = "is_electric")
    private Boolean isElectric = false;

    @Column(name = "created_by_series_appointment")
    private Boolean createdBySeriesAppointment = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_series_id")
    @JsonBackReference
    private AppointmentSeries appointmentSeries;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // JSON serialization helpers - expose IDs and names for API responses
    @JsonProperty("therapistId")
    public Long getTherapistId() {
        return therapist != null ? therapist.getId() : null;
    }

    @JsonProperty("patientId")
    public Long getPatientId() {
        return patient != null ? patient.getId() : null;
    }

    @JsonProperty("therapistName")
    public String getTherapistName() {
        return therapist != null ? therapist.getFullName() : null;
    }

    @JsonProperty("patientName")
    public String getPatientName() {
        return patient != null ? patient.getFullName() : null;
    }

    @JsonProperty("isBWO")
    public Boolean getIsBWO() {
        return patient != null ? patient.getIsBWO() : false;
    }

    @JsonProperty("appointmentSeriesId")
    public Long getAppointmentSeriesId() {
        return appointmentSeries != null ? appointmentSeries.getId() : null;
    }
}
