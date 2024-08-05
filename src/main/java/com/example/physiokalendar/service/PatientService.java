package com.example.physiokalendar.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONPatientDTO;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.repository.PatientRepository;

@Service
public class PatientService {
    
    @Autowired
    private PatientRepository patientRepository;

    public List<JSONPatientDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream().map(this::convertEntityToDTO).collect(Collectors.toList());
    }

    public JSONPatientDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        return convertEntityToDTO(patient);
    }

    public Patient createPatient(JSONPatientDTO dto) {
        Patient patient = convertDTOToEntity(dto);
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, JSONPatientDTO dto) {
        Patient existingPatient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        existingPatient.setFirstName(dto.getFirstName());
        existingPatient.setLastName(dto.getLastName());
        existingPatient.setActiveSince(new Date(dto.getActiveSince()));
        existingPatient.setActiveUntil(new Date(dto.getActiveUntil()));
        existingPatient.setIsBWO(dto.getIsBWO());
        return patientRepository.save(existingPatient);
    }

    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }

    private JSONPatientDTO convertEntityToDTO(Patient patient) {
        JSONPatientDTO dto = new JSONPatientDTO();
        dto.setId(patient.getId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setActiveSince(patient.getActiveSince().getTime()); // Convert Date to long
        dto.setActiveUntil(patient.getActiveUntil().getTime()); // Convert Date to long
        dto.setIsBWO(patient.getIsBWO());
        return dto;
    }

    private Patient convertDTOToEntity(JSONPatientDTO dto) {
        Patient patient = new Patient();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setActiveSince(new Date(dto.getActiveSince())); // Convert long to Date
        patient.setActiveUntil(new Date(dto.getActiveUntil())); // Convert long to Date
        patient.setIsBWO(dto.getIsBWO());
        return patient;
    }
}