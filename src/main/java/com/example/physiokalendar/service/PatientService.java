package com.example.physiokalendar.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.physiokalendar.entity.AuditAction;
import com.example.physiokalendar.entity.AuditEntityType;
import com.example.physiokalendar.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.physiokalendar.dto.JSONPatientDTO;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.repository.PatientRepository;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AuditService auditService;

    public List<JSONPatientDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream().map(this::convertEntityToDTO).collect(Collectors.toList());
    }

    public JSONPatientDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        return convertEntityToDTO(patient);
    }

    @Transactional
    public Patient createPatient(JSONPatientDTO dto) {
        Patient patient = convertDTOToEntity(dto);
        patient.setId(null);
        Patient saved = patientRepository.save(patient);

        // Audit-Log
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.PATIENT, saved.getId())
                .action(AuditAction.CREATE)
                .after(auditService.toAuditJson(saved)));

        return saved;
    }

    @Transactional
    public Patient updatePatient(Long id, JSONPatientDTO dto) {
        Patient existingPatient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));

        String beforeJson = auditService.toAuditJson(existingPatient);

        existingPatient.setFirstName(dto.getFirstName());
        existingPatient.setLastName(dto.getLastName());
        existingPatient.setFullName(dto.getFirstName() + " " + dto.getLastName());
        existingPatient.setEmail(dto.getEmail());
        existingPatient.setTelefon(dto.getTelefon());
        existingPatient.setStreet(dto.getStreet());
        existingPatient.setHouseNumber(dto.getHouseNumber());
        existingPatient.setPostalCode(dto.getPostalCode());
        existingPatient.setCity(dto.getCity());
        existingPatient.setActiveSince(dto.getActiveSince());
        existingPatient.setActiveUntil(dto.getActiveUntil());
        existingPatient.setIsBWO(dto.getIsBWO());

        Patient updated = patientRepository.save(existingPatient);

        // Audit-Log
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.PATIENT, id)
                .action(AuditAction.UPDATE)
                .before(beforeJson)
                .after(auditService.toAuditJson(updated)));

        return updated;
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient existing = patientRepository.findById(id).orElse(null);
        if (existing != null) {
            String beforeJson = auditService.toAuditJson(existing);
            patientRepository.deleteById(id);

            // Audit-Log
            auditService.record(AuditService.builder()
                    .actor(getCurrentUserId(), getCurrentUsername())
                    .entity(AuditEntityType.PATIENT, id)
                    .action(AuditAction.DELETE)
                    .before(beforeJson));
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "system";
    }

    public JSONPatientDTO convertEntityToDTO(Patient patient) {
        JSONPatientDTO dto = new JSONPatientDTO();
        dto.setId(patient.getId());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setFullName(patient.getFirstName() + " " + patient.getLastName());
        dto.setEmail(patient.getEmail());
        dto.setTelefon(patient.getTelefon());
        dto.setStreet(patient.getStreet());
        dto.setHouseNumber(patient.getHouseNumber());
        dto.setPostalCode(patient.getPostalCode());
        dto.setCity(patient.getCity());
        dto.setActiveSince(patient.getActiveSince());
        dto.setActiveUntil(patient.getActiveUntil());
        dto.setIsBWO(patient.getIsBWO());
        dto.setCreatedAt(patient.getCreatedAt());
        dto.setUpdatedAt(patient.getUpdatedAt());
        return dto;
    }

    public Patient convertDTOToEntity(JSONPatientDTO dto) {
        Patient patient = new Patient();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setFullName(dto.getFirstName() + " " + dto.getLastName());
        patient.setEmail(dto.getEmail());
        patient.setTelefon(dto.getTelefon());
        patient.setStreet(dto.getStreet());
        patient.setHouseNumber(dto.getHouseNumber());
        patient.setPostalCode(dto.getPostalCode());
        patient.setCity(dto.getCity());
        patient.setActiveSince(dto.getActiveSince());
        patient.setActiveUntil(dto.getActiveUntil());
        patient.setIsBWO(dto.getIsBWO());
        return patient;
    }
}