package com.example.physiokalendar.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.entity.AbsenceType;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findByTherapistId(Long therapistId);

    @Query(value = "SELECT * FROM absence a WHERE a.therapist_id = :therapistId AND DATE(a.date) = DATE(:date)", nativeQuery = true)
    List<Absence> findByTherapistIdAndDate(@Param("therapistId") Long therapistId, @Param("date") String date);

    List<Absence> findByTherapistIdAndWeekday(Long therapistId, String weekday);

    /**
     * Find all special (one-time) absences within a date range.
     */
    @Query("SELECT a FROM Absence a WHERE " +
           "a.absenceType = :absenceType AND " +
           "a.date BETWEEN :from AND :to")
    List<Absence> findSpecialByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("absenceType") AbsenceType absenceType);

    /**
     * Find all special absences for a therapist within a date range.
     */
    @Query("SELECT a FROM Absence a WHERE " +
           "a.therapist.id = :therapistId AND " +
           "a.absenceType = :absenceType AND " +
           "a.date BETWEEN :from AND :to")
    List<Absence> findSpecialByTherapistIdAndDateRange(
            @Param("therapistId") Long therapistId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("absenceType") AbsenceType absenceType);

    /**
     * Find all recurring absences (weekly blocks).
     */
    @Query("SELECT a FROM Absence a WHERE a.absenceType = :absenceType")
    List<Absence> findAllRecurring(@Param("absenceType") AbsenceType absenceType);

    /**
     * Find recurring absences for a specific therapist.
     */
    @Query("SELECT a FROM Absence a WHERE " +
           "a.therapist.id = :therapistId AND " +
           "a.absenceType = :absenceType")
    List<Absence> findRecurringByTherapistId(
            @Param("therapistId") Long therapistId,
            @Param("absenceType") AbsenceType absenceType);
}
