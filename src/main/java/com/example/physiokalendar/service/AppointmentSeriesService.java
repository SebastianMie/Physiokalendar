package com.example.physiokalendar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.repository.AppointmentSeriesRepository;

@Service
public class AppointmentSeriesService {

    @Autowired
    private AppointmentSeriesRepository appointmentSeriesRepository;

    public List<AppointmentSeries> getAllAppointmentSeries() {
        return appointmentSeriesRepository.findAll();
    }

    public Optional<AppointmentSeries> getAppointmentSeriesById(Long id) {
        return appointmentSeriesRepository.findById(id);
    }

    public AppointmentSeries saveAppointmentSeries(AppointmentSeries appointmentSeries) {
        return appointmentSeriesRepository.save(appointmentSeries);
    }

    public void deleteAppointmentSeries(Long id) {
        appointmentSeriesRepository.deleteById(id);
    }
}
