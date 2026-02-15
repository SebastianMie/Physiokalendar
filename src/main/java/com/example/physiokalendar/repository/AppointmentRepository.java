package com.example.physiokalendar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.AppointmentStatus;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Paginated query for appointments with optional filters.
     * Supports filtering by date range, therapist, status, and search term.
     * Excludes series-created appointments when seriesOnly is false.
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.createdBySeriesAppointment = false AND " +
           "(:dateFrom IS NULL OR a.date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.date <= :dateTo) AND " +
           "(:therapistId IS NULL OR a.therapist.id = :therapistId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "  LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.therapist.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.therapist.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.comment) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Appointment> findSingleAppointmentsFiltered(
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("therapistId") Long therapistId,
            @Param("status") AppointmentStatus status,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Paginated query for all appointments (including series) with appointment type filter.
     * Used for therapist detail view with faceted search.
     * @param appointmentType: null=all, true=series only, false=single only
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "(:appointmentType IS NULL OR a.createdBySeriesAppointment = :appointmentType) AND " +
           "(:dateFrom IS NULL OR a.date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.date <= :dateTo) AND " +
           "(:therapistId IS NULL OR a.therapist.id = :therapistId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "  LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.therapist.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.therapist.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(a.comment) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Appointment> findAppointmentsFiltered(
            @Param("appointmentType") Boolean appointmentType,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("therapistId") Long therapistId,
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Count non-series appointments for cache invalidation checks.
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.createdBySeriesAppointment = false")
    long countSingleAppointments();
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

    // Series appointment queries
    @Query("SELECT MAX(a.date) FROM Appointment a WHERE a.appointmentSeries.id = :seriesId")
    LocalDate findLatestDateBySeriesId(@Param("seriesId") Long seriesId);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.appointmentSeries.id = :seriesId AND a.date = :date")
    boolean existsBySeriesIdAndDate(@Param("seriesId") Long seriesId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentSeries.id = :seriesId ORDER BY a.date")
    List<Appointment> findBySeriesId(@Param("seriesId") Long seriesId);
}
