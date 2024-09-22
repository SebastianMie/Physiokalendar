package com.example.physiokalendar.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAbsenceDTO;
import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.repository.AbsenceRepository;

@Service
public class AbsenceService {

    @Autowired
    private AbsenceRepository absenceRepository;

    public List<Absence> getAllAbsences() {
        return absenceRepository.findAll();
    }

    public Optional<Absence> getAbsenceById(Long id) {
        return absenceRepository.findById(id);
    }

    public List<Absence> getAbsencesByTherapistId(Long therapistId) {
        return absenceRepository.findByTherapistId(therapistId);
    }

    public Absence saveAbsence(JSONAbsenceDTO absence) {
        //absence.setId(null);
        return absenceRepository.save(convertDTOToEntity(absence));
    }

    public void deleteAbsence(Long id) {
        absenceRepository.deleteById(id);
    }

    public Absence convertDTOToEntity(JSONAbsenceDTO dto) {
        Absence absence = new Absence();
        absence.setId(dto.getId());
        
        absence.setTherapistId(dto.getTherapistId());
        
        absence.setDate(dto.getDate());
        absence.setWeekday(dto.getWeekday());
        absence.setStartTime(dto.getStartTime());
        absence.setEndTime(dto.getEndTime());
        return absence;
    }

    public static JSONAbsenceDTO convertEntityToDTO(Absence absence) {
        JSONAbsenceDTO dto = new JSONAbsenceDTO();
        dto.setId(absence.getId());
        dto.setTherapistId(absence.getTherapistId());
        dto.setDate(absence.getDate());
        dto.setWeekday(dto.getWeekday());
        dto.setStartTime(dto.getDate());
        dto.setEndTime(dto.getDate());
        // Weitere Felder falls nötig
        return dto;
    }
}
