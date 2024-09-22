// AppointmentService.java
package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.JSONAppointmentDTO;
import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.TherapistRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.physiokalendar.repository.PatientRepository;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TherapistService therapistService;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment saveAppointment(JSONAppointmentDTO appointmentDTO) {
        // Mapping DTO to Entity
        Long therapistId = appointmentDTO.getTherapist().getId();
        Long patientId = appointmentDTO.getPatient().getId();
        
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID"));

        Appointment appointment = new Appointment();
        appointment.setId(appointmentDTO.getId());
        appointment.setTherapist(therapist);
        appointment.setPatient(patient);
        appointment.setDate(appointmentDTO.getDate());
        appointment.setStartTime(appointmentDTO.getStartTime());
        appointment.setEndTime(appointmentDTO.getEndTime());
        appointment.setComment(appointmentDTO.getComment());
        appointment.setCreatedBySeriesAppointment(appointmentDTO.getCreatedBySeriesAppointment());
        appointment.setIsHotair(appointmentDTO.getIsHotair());
        appointment.setIsUltrasonic(appointmentDTO.getIsUltrasonic());
        appointment.setIsElectric(appointmentDTO.getIsElectric());

        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    public List<Appointment> getAppointmentsWithConflicts() {
        List<Appointment> allAppointments = this.getAllAppointments();
        List<Appointment> conflictingAppointments = new ArrayList<>();
        Set<Long> addedAppointments = new HashSet<>();
    
        for (Appointment currentAppointment : allAppointments) {
            if (addedAppointments.contains(currentAppointment.getId())) {
                // Überspringe dieses Appointment, wenn es bereits als konfliktbehaftet markiert wurde
                continue;
            }
            for (Appointment compareAppointment : allAppointments) {
                if (!currentAppointment.getId().equals(compareAppointment.getId()) &&
                    this.checkForConflicts(currentAppointment, compareAppointment)) {
    
                    // Füge nur das erste gefundene konfliktbehaftete Appointment hinzu
                    conflictingAppointments.add(currentAppointment);
                    // Markiere beide Termine als bearbeitet
                    addedAppointments.add(currentAppointment.getId());
                    addedAppointments.add(compareAppointment.getId());
                    break;
                }
            }
        }
        return conflictingAppointments;
    }

    private boolean checkForConflicts(Appointment app1, Appointment app2) {
        return app1.getTherapist().getId().equals(app2.getTherapist().getId()) &&
               app1.getDate().equals(app2.getDate()) &&
               app1.getStartTime().before(app2.getEndTime()) &&
               app1.getEndTime().after(app2.getStartTime());
    }

    public boolean checkForConflicts(Appointment newAppointment) {
        // Holen Sie alle Termine
        List<Appointment> allAppointments = appointmentRepository.findAll();

        List<Appointment> todayAppointments = allAppointments.stream()
                .filter(appointment -> appointment.getDate().equals(newAppointment.getDate()))
                .collect(Collectors.toList());

        // Filtern Sie nach Therapeut
        List<Appointment> therapistAppointments = todayAppointments.stream()
                .filter(appointment -> appointment.getTherapist().getId().equals(newAppointment.getTherapist().getId()))
                .collect(Collectors.toList());

        // Überprüfen Sie auf Überlappungen
        for (Appointment existingAppointment : therapistAppointments) {
            if (isOverlapping(existingAppointment, newAppointment)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOverlapping(Appointment existingAppointment, Appointment newAppointment) {
        return existingAppointment.getStartTime().before(newAppointment.getEndTime()) &&
               existingAppointment.getEndTime().after(newAppointment.getStartTime());
    }

     public Appointment convertDTOToEntity(JSONAppointmentDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setId(dto.getId());
        appointment.setDate(dto.getDate());
        appointment.setComment(dto.getComment());
        appointment.setCreatedBySeriesAppointment(dto.getCreatedBySeriesAppointment());
        appointment.setAppointmentSeriesId(dto.getAppointmentSeriesId());
        appointment.setStartTime(dto.getStartTime());
        appointment.setEndTime(dto.getEndTime());
        appointment.setIsElectric(dto.getIsElectric());
        appointment.setIsHotair(dto.getIsHotair());
        appointment.setIsUltrasonic(dto.getIsUltrasonic());
        appointment.setPatient(patientService.convertDTOToEntity(dto.getPatient()));
        appointment.setTherapist(therapistService.convertDTOToEntity(dto.getTherapist()));
        // Weitere Felder falls nötig
        return appointment;
    }
}
