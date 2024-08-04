package com.example.physiokalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.AppointmentSeries;

@Repository
public interface AppointmentSeriesRepository extends JpaRepository<AppointmentSeries, Long> {
}
