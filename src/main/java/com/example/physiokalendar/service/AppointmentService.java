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

import java.util.List;
import java.util.Optional;

import com.example.physiokalendar.repository.PatientRepository;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment saveAppointment(JSONAppointmentDTO appointmentDTO) {
        // Mapping DTO to Entity
        Long therapistId = appointmentDTO.getTherapist().getId();
        Long patientId = appointmentDTO.getPatientId();
        
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID"));

        Appointment appointment = new Appointment();
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
}
