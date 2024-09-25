package com.example.physiokalendar.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Appointment;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // List<Appointment> findAllByTherapistIdAndDate(Long therapistId, LocalDate date);

     @Query("SELECT a FROM Appointment a WHERE a.therapist.id = :therapistId AND a.date = :date")
    List<Appointment> findAllByTherapistIdAndDate(@Param("therapistId") Long therapistId, @Param("date") Date date);

}
