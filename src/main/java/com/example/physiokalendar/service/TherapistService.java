package com.example.physiokalendar.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAbsenceDTO;
import com.example.physiokalendar.dto.JSONAbsenceExceptionDTO;
import com.example.physiokalendar.dto.JSONTherapistDTO;
import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.entity.AbsenceException;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AbsenceExceptionRepository;
import com.example.physiokalendar.repository.AbsenceRepository;
import com.example.physiokalendar.repository.TherapistRepository;

@Service
public class TherapistService {

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private AbsenceExceptionRepository exceptionRepository;

    public List<JSONTherapistDTO> getAllTherapists() {
        return therapistRepository.findAll().stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    public JSONTherapistDTO getTherapistById(Long id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));
        return convertEntityToDTO(therapist);
    }

    public Therapist createTherapist(JSONTherapistDTO dto) {
        Therapist therapist = convertDTOToEntity(dto);
        return therapistRepository.save(therapist);
    }

    public Therapist updateTherapist(Long id, JSONTherapistDTO dto) {
        Therapist existingTherapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));
        existingTherapist.setFirstName(dto.getFirstName());
        existingTherapist.setLastName(dto.getLastName());
        existingTherapist.setFullName(dto.getFirstName() + " " + dto.getLastName());
        existingTherapist.setActiveSince(dto.getActiveSince());
        existingTherapist.setActiveUntil(dto.getActiveUntil());
        existingTherapist.setIsActive(dto.getIsActive());
        return therapistRepository.save(existingTherapist);
    }

    public void deleteTherapist(Long id) {
        therapistRepository.deleteById(id);
    }

    public List<JSONAbsenceDTO> getAllAbsences() {
        return absenceRepository.findAll().stream()
                .map(AbsenceService::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    public Therapist addAbsence(Long therapistId, JSONAbsenceDTO absenceDTO) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Absence absence = AbsenceService.convertDTOToEntity(absenceDTO);
        absence.setTherapist(therapist);
        absenceRepository.save(absence);
        therapist.getAbsences().add(absence);
        return therapistRepository.save(therapist);
    }

    public Therapist addAbsenceException(Long therapistId, JSONAbsenceExceptionDTO exceptionDTO) {
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        AbsenceException exception = AbsenceExceptionService.convertDTOToEntity(exceptionDTO);
        exception.setTherapist(therapist);
        exceptionRepository.save(exception);
        therapist.getExceptions().add(exception);
        return therapistRepository.save(therapist);
    }

    public JSONTherapistDTO convertEntityToDTO(Therapist therapist) {
        JSONTherapistDTO dto = new JSONTherapistDTO();
        dto.setId(therapist.getId());
        dto.setFirstName(therapist.getFirstName());
        dto.setLastName(therapist.getLastName());
        dto.setFullName(therapist.getFullName());
        dto.setActiveSince(therapist.getActiveSince());
        dto.setActiveUntil(therapist.getActiveUntil());
        dto.setIsActive(therapist.getIsActive());
        return dto;
    }

    public Therapist convertDTOToEntity(JSONTherapistDTO dto) {
        Therapist therapist = new Therapist();
        therapist.setId(dto.getId());
        therapist.setFirstName(dto.getFirstName());
        therapist.setLastName(dto.getLastName());
        therapist.setFullName(dto.getFullName());
        therapist.setActiveSince(dto.getActiveSince());
        therapist.setActiveUntil(dto.getActiveUntil());
        therapist.setIsActive(dto.getIsActive());
        return therapist;
    }

}
