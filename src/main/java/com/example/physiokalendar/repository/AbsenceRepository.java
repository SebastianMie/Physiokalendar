package com.example.physiokalendar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Absence;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findByTherapistId(Long therapistId);

    @Query(value = "SELECT * FROM absence a WHERE a.therapist_id = :therapistId AND DATE(a.date) = DATE(:date)", nativeQuery = true)
    List<Absence> findByTherapistIdAndDate(@Param("therapistId") Long therapistId, @Param("date") String date);

    List<Absence> findByTherapistIdAndWeekday(Long therapistId, String weekday);

}
