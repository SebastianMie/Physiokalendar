package com.example.physiokalendar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAbsenceDTO;
import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AbsenceRepository;
import com.example.physiokalendar.repository.TherapistRepository;

@Service
public class AbsenceService {

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    public List<Absence> getAllAbsences() {
        return absenceRepository.findAll();
    }

    public Optional<Absence> getAbsenceById(Long id) {
        return absenceRepository.findById(id);
    }

    public List<Absence> getAbsencesByTherapistId(Long therapistId) {
        return absenceRepository.findByTherapistId(therapistId);
    }

    public List<Absence> getAbsencesByTherapistAndDate(Long therapistId, String date) {
        return absenceRepository.findByTherapistIdAndDate(therapistId, date);
    }

    public List<Absence> getAbsencesByTherapistAndWeekday(Long therapistId, String weekday) {
        return absenceRepository.findByTherapistIdAndWeekday(therapistId, weekday);
    }

    public Absence saveAbsence(JSONAbsenceDTO absence) {
        return absenceRepository.save(convertDTOToEntity(absence));
    }

    public void deleteAbsence(Long id) {
        absenceRepository.deleteById(id);
    }

    public Absence convertDTOToEntity(JSONAbsenceDTO dto) {
    Absence absence = new Absence();
    absence.setId(dto.getId());
    
    // Hole das Therapist-Objekt anhand der ID
    Therapist therapist = therapistRepository.findById(dto.getTherapistId())
        .orElseThrow(() -> new RuntimeException("Therapist not found with id " + dto.getTherapistId()));

    // Setze das Therapist-Objekt
    absence.setTherapist(therapist);
    
    absence.setDate(dto.getDate());
    absence.setWeekday(dto.getWeekday());
    absence.setStartTime(dto.getStartTime());
    absence.setEndTime(dto.getEndTime());
    return absence;
}


    public static JSONAbsenceDTO convertEntityToDTO(Absence absence) {
        JSONAbsenceDTO dto = new JSONAbsenceDTO();
        dto.setId(absence.getId());
        dto.setTherapistId(absence.getTherapist().getId());
        dto.setDate(absence.getDate());
        dto.setWeekday(absence.getWeekday());
        dto.setStartTime(absence.getStartTime());
        dto.setEndTime(absence.getEndTime());
        // Weitere Felder falls nötig
        return dto;
    }

     public List<JSONAbsenceDTO> convertEntitiesToDTOs(List<Absence> absences) {
        List<JSONAbsenceDTO> dtoList = new ArrayList<>();
        for (Absence absence : absences) {
            dtoList.add(convertEntityToDTO(absence));
        }
        return dtoList;
    }
}
