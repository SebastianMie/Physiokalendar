// AppointmentSeriesService.java
package com.example.physiokalendar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAppointmentSeriesDTO;
import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AppointmentSeriesRepository;
import com.example.physiokalendar.repository.PatientRepository;
import com.example.physiokalendar.repository.TherapistRepository;

@Service
public class AppointmentSeriesService {

    @Autowired
    private AppointmentSeriesRepository appointmentSeriesRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    public List<AppointmentSeries> getAllAppointmentSeries() {
        return appointmentSeriesRepository.findAll();
    }

    public Optional<AppointmentSeries> getAppointmentSeriesById(Long id) {
        return appointmentSeriesRepository.findById(id);
    }

    public AppointmentSeries saveAppointmentSeries(JSONAppointmentSeriesDTO appointmentSeriesDTO) {
        // Mapping DTO to Entity
        Long therapistId = appointmentSeriesDTO.getTherapist().getId();
        Long patientId = appointmentSeriesDTO.getPatientId();
        
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID"));

        AppointmentSeries appointmentSeries = new AppointmentSeries();
        appointmentSeries.setTherapist(therapist);
        appointmentSeries.setPatient(patient);
        appointmentSeries.setStartTime(appointmentSeriesDTO.getStartTime());
        appointmentSeries.setEndTime(appointmentSeriesDTO.getEndTime());
        appointmentSeries.setComment(appointmentSeriesDTO.getComment());
        appointmentSeries.setStartDate(appointmentSeriesDTO.getStartDate());
        appointmentSeries.setEndDate(appointmentSeriesDTO.getEndDate());
        appointmentSeries.setWeeklyfrequency(appointmentSeriesDTO.getWeeklyFrequency());
        appointmentSeries.setIsBWO(appointmentSeriesDTO.getIsBWO());

        return appointmentSeriesRepository.save(appointmentSeries);
    }

    public void deleteAppointmentSeries(Long id) {
        appointmentSeriesRepository.deleteById(id);
    }
}
