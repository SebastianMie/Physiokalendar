package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PatientService {

    @Autowired
    private static PatientRepository patientRepository;

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public static Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, Patient updatedPatient) {
        Patient patient = getPatientById(id);
        patient.setFirstName(updatedPatient.getFirstName());
        patient.setLastName(updatedPatient.getLastName());
        patient.setActiveSince(updatedPatient.getActiveSince());
        patient.setActiveUntil(updatedPatient.getActiveUntil());
        patient.setIsBWO(updatedPatient.getIsBWO());
        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }

    public List<Patient> filterPatients(String name, Date activeSince, Date activeUntil, Boolean isBWO) {
        return patientRepository.findAll().stream()
                .filter(p -> (name == null || (p.getFirstName() + " " + p.getLastName()).toLowerCase().contains(name.toLowerCase())))
                .filter(p -> (activeSince == null || !p.getActiveSince().before(activeSince)))
                .filter(p -> (activeUntil == null || !p.getActiveUntil().after(activeUntil)))
                .filter(p -> (isBWO == null || p.getIsBWO().equals(isBWO)))
                .toList();
    }
}
