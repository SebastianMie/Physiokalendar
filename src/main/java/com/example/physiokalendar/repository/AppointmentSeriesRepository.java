package com.example.physiokalendar.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.entity.SeriesStatus;

@Repository
public interface AppointmentSeriesRepository extends JpaRepository<AppointmentSeries, Long> {

    /**
     * Find all active series that overlap with the given date range.
     * A series overlaps if its start_date <= rangeTo AND end_date >= rangeFrom
     */
    @Query("SELECT s FROM AppointmentSeries s WHERE " +
           "s.status = :status AND " +
           "s.startDate <= :rangeTo AND " +
           "s.endDate >= :rangeFrom")
    List<AppointmentSeries> findActiveByDateRange(
            @Param("rangeFrom") LocalDate rangeFrom,
            @Param("rangeTo") LocalDate rangeTo,
            @Param("status") SeriesStatus status);

    /**
     * Find all active series for a specific therapist that overlap with the given date range.
     */
    @Query("SELECT s FROM AppointmentSeries s WHERE " +
           "s.therapist.id = :therapistId AND " +
           "s.status = :status AND " +
           "s.startDate <= :rangeTo AND " +
           "s.endDate >= :rangeFrom")
    List<AppointmentSeries> findActiveByTherapistIdAndDateRange(
            @Param("therapistId") Long therapistId,
            @Param("rangeFrom") LocalDate rangeFrom,
            @Param("rangeTo") LocalDate rangeTo,
            @Param("status") SeriesStatus status);

    /**
     * Find all active series for a specific patient.
     */
    @Query("SELECT s FROM AppointmentSeries s WHERE " +
           "s.patient.id = :patientId AND " +
           "s.status = :status AND " +
           "s.endDate >= :fromDate")
    List<AppointmentSeries> findActiveByPatientId(
            @Param("patientId") Long patientId,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") SeriesStatus status);

    /**
     * Find all series for a specific patient.
     */
    @Query("SELECT s FROM AppointmentSeries s WHERE s.patient.id = :patientId")
    List<AppointmentSeries> findByPatientId(@Param("patientId") Long patientId);

    /**
     * Find all series for a specific therapist.
     */
    @Query("SELECT s FROM AppointmentSeries s WHERE s.therapist.id = :therapistId")
    List<AppointmentSeries> findByTherapistId(@Param("therapistId") Long therapistId);

    /**
     * Check if a similar series already exists (for duplicate detection during import).
     */
    @Query("SELECT COUNT(s) > 0 FROM AppointmentSeries s WHERE " +
           "s.therapist.id = :therapistId AND " +
           "s.patient.id = :patientId AND " +
           "s.weekday = :weekday AND " +
           "s.startTime = :startTime AND " +
           "s.endTime = :endTime")
    boolean existsByTherapistPatientWeekdayAndTime(
            @Param("therapistId") Long therapistId,
            @Param("patientId") Long patientId,
            @Param("weekday") String weekday,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime);

    /**
     * Find all active series (for generator job).
     */
    @Query("SELECT s FROM AppointmentSeries s WHERE s.status = :status")
    List<AppointmentSeries> findByStatus(@Param("status") SeriesStatus status);
}
