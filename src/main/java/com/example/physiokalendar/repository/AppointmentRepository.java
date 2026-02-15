package com.example.physiokalendar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.AppointmentStatus;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.date = :date")
    List<Appointment> findAllByTherapistIdAndDate(@Param("therapistId") Long therapistId, @Param("date") Date date);

    @Query("SELECT a FROM Appointment a WHERE DATE(a.date) = DATE(:date)")
    List<Appointment> findByDate(@Param("date") Date date);

    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.date = :date")
    List<Appointment> findByTherapistIdAndDate(@Param("therapistId") Long therapistId, @Param("date") Date date);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.date = :date")
    List<Appointment> findByPatientIdAndDate(@Param("patientId") Long patientId, @Param("date") Date date);

    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.patient.id = :patientId AND a.date = :date")
    List<Appointment> findByTherapistIdAndPatientIdAndDate(@Param("therapistId") Long therapistId, @Param("patientId") Long patientId, @Param("date") Date date);

    // For duplicate checking during import with LocalDate
    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.patient.id = :patientId AND a.date = :date")
    List<Appointment> findByTherapistIdAndPatientIdAndLocalDate(@Param("therapistId") Long therapistId, @Param("patientId") Long patientId, @Param("date") LocalDate date);

    // Check if exact appointment exists (for duplicate detection during import)
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.therapist.id = :therapistId AND a.patient.id = :patientId AND a.date = :date AND a.startTime = :startTime AND a.endTime = :endTime")
    boolean existsByTherapistPatientDateAndTime(
            @Param("therapistId") Long therapistId,
            @Param("patientId") Long patientId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.patient.id = :patientId")
    List<Appointment> findByTherapistIdAndPatientId(@Param("therapistId") Long therapistId, @Param("patientId") Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId")
    List<Appointment> findByTherapistId(@Param("therapistId") Long therapistId);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId")
    List<Appointment> findByPatientId(@Param("patientId") Long patientId);

    // Calendar Range queries
    @Query("SELECT a FROM Appointment a WHERE a.date BETWEEN :from AND :to ORDER BY a.date, a.startTime")
    List<Appointment> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.date BETWEEN :from AND :to ORDER BY a.date, a.startTime")
    List<Appointment> findByTherapistIdAndDateRange(
            @Param("therapistId") Long therapistId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.date BETWEEN :from AND :to ORDER BY a.date, a.startTime")
    List<Appointment> findByPatientIdAndDateRange(
            @Param("patientId") Long patientId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // Conflict detection queries
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.therapist.id = :therapistId AND " +
           "a.date = :date AND " +
           "a.status != :excludeStatus AND " +
           "((a.startTime < :endTime AND a.endTime > :startTime))")
    List<Appointment> findOverlapping(
            @Param("therapistId") Long therapistId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeStatus") AppointmentStatus excludeStatus);

    @Query("SELECT a FROM Appointment a WHERE " +
           "a.therapist.id = :therapistId AND " +
           "a.date = :date AND " +
           "a.id != :excludeId AND " +
           "a.status != :excludeStatus AND " +
           "((a.startTime < :endTime AND a.endTime > :startTime))")
    List<Appointment> findOverlappingExcludingId(
            @Param("therapistId") Long therapistId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId,
            @Param("excludeStatus") AppointmentStatus excludeStatus);
}
