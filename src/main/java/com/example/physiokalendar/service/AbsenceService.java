package com.example.physiokalendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
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

        absence.setDate(dto.getDate() != null ? dateToLocalDate(dto.getDate()) : null);
        absence.setWeekday(dto.getWeekday());
        absence.setStartTime(dto.getStartTime() != null ? dateToLocalDateTime(dto.getStartTime()) : null);
        absence.setEndTime(dto.getEndTime() != null ? dateToLocalDateTime(dto.getEndTime()) : null);
        // Weitere Felder falls nötig
        return absence;
    }

    public JSONAbsenceDTO convertEntityToDTO(Absence absence) {
        JSONAbsenceDTO dto = new JSONAbsenceDTO();
        dto.setId(absence.getId());
        if (absence.getTherapist() != null) {
            dto.setTherapistId(absence.getTherapist().getId());
        }
        dto.setDate(absence.getDate() != null ? localDateToDate(absence.getDate()) : null);
        dto.setWeekday(absence.getWeekday());
        dto.setStartTime(absence.getStartTime() != null ? localDateTimeToDate(absence.getStartTime()) : null);
        dto.setEndTime(absence.getEndTime() != null ? localDateTimeToDate(absence.getEndTime()) : null);
        return dto;
    }

    public List<JSONAbsenceDTO> convertEntitiesToDTOs(List<Absence> absences) {
        List<JSONAbsenceDTO> dtoList = new ArrayList<>();
        for (Absence absence : absences) {
            dtoList.add(convertEntityToDTO(absence));
        }
        return dtoList;
    }

    private LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    private Date localDateToDate(LocalDate localDate) {
        return java.sql.Date.valueOf(localDate);
    }

    private Date localDateTimeToDate(LocalDateTime localDateTime) {
        return java.sql.Timestamp.valueOf(localDateTime);
    }
}
