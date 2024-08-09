// AppointmentSeriesService.java
package com.example.physiokalendar.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAppointmentSeriesDTO;
import com.example.physiokalendar.dto.JSONCancellationDTO;
import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.entity.Cancellation;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AppointmentSeriesRepository;
import com.example.physiokalendar.repository.CancellationRepository;
import com.example.physiokalendar.repository.PatientRepository;
import com.example.physiokalendar.repository.TherapistRepository;

import jakarta.transaction.Transactional;

@Service
public class AppointmentSeriesService {

    @Autowired
    private AppointmentSeriesRepository appointmentSeriesRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private CancellationRepository cancellationRepository;

    public List<AppointmentSeries> getAllAppointmentSeries() {
        return appointmentSeriesRepository.findAll();
    }

    public Optional<AppointmentSeries> getAppointmentSeriesById(Long id) {
        return appointmentSeriesRepository.findById(id);
    }

    @Transactional
    public AppointmentSeries saveAppointmentSeries(JSONAppointmentSeriesDTO appointmentSeriesDTO) {
        // Mapping DTO to Entity
        Long therapistId = appointmentSeriesDTO.getTherapist().getId();
        Long patientId = appointmentSeriesDTO.getPatientId();
        
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID"));

        List<Cancellation> cancellations = appointmentSeriesDTO.getCancellationIds().stream()
                .map(cancellationId -> cancellationRepository.findById(cancellationId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid cancellation ID: " + cancellationId)))
                .collect(Collectors.toList());

        AppointmentSeries appointmentSeries = new AppointmentSeries();
        appointmentSeries.setTherapist(therapist);
        appointmentSeries.setPatient(patient);
        appointmentSeries.setStartTime(appointmentSeriesDTO.getStartTime());
        appointmentSeries.setEndTime(appointmentSeriesDTO.getEndTime());
        appointmentSeries.setComment(appointmentSeriesDTO.getComment());
        appointmentSeries.setStartDate(appointmentSeriesDTO.getStartDate());
        appointmentSeries.setEndDate(appointmentSeriesDTO.getEndDate());
        appointmentSeries.setWeeklyfrequency(appointmentSeriesDTO.getWeeklyFrequency());
        appointmentSeries.setCancellations(cancellations);
        appointmentSeries.setIsBWO(appointmentSeriesDTO.getIsBWO());

        return appointmentSeriesRepository.save(appointmentSeries);
    }

    @Transactional
    public AppointmentSeries addCancellations(Long appointmentSeriesId, List<JSONCancellationDTO> cancellationDTOs) {
        // Holen des AppointmentSeries-Objekts anhand der ID
        AppointmentSeries appointmentSeries = appointmentSeriesRepository.findById(appointmentSeriesId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment series ID"));

        // Durchlaufen der DTOs und Erstellen der Cancellation-Objekte
        List<Cancellation> cancellations = cancellationDTOs.stream().map(dto -> {
            Cancellation cancellation = convertDTOToEntity(dto);

            // Zuweisen des AppointmentSeries zu Cancellation
            cancellation.setAppointmentSeries(appointmentSeries);

            // Speichern der Cancellation in der Datenbank
            return cancellationRepository.save(cancellation);
        }).collect(Collectors.toList());

        // Hinzufügen der neuen Cancellations zur AppointmentSeries
        appointmentSeries.getCancellations().addAll(cancellations);

        // Speichern des aktualisierten AppointmentSeries-Objekts
        return appointmentSeriesRepository.save(appointmentSeries);
    }

    public void deleteAppointmentSeries(Long id) {
        appointmentSeriesRepository.deleteById(id);
    }

    private Cancellation convertDTOToEntity(JSONCancellationDTO dto) {
        Cancellation cancellation = new Cancellation();
        cancellation.setId(dto.getId());
        cancellation.setDate(dto.getDate());
        // Weitere Felder falls nötig
        return cancellation;
    }
}