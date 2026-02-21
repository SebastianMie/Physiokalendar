package com.example.physiokalendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAbsenceDTO;
import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.entity.AbsenceType;
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

        // Parse date strings directly without timezone conversion
        absence.setDate(dto.getDate() != null ? parseLocalDate(dto.getDate()) : null);
        absence.setEndDate(dto.getEndDate() != null ? parseLocalDate(dto.getEndDate()) : null);
        absence.setWeekday(dto.getWeekday());
        absence.setStartTime(dto.getStartTime() != null ? parseLocalTime(dto.getStartTime()) : null);
        absence.setEndTime(dto.getEndTime() != null ? parseLocalTime(dto.getEndTime()) : null);
        absence.setReason(dto.getReason());
        if (dto.getAbsenceType() != null) {
            absence.setAbsenceType(AbsenceType.valueOf(dto.getAbsenceType()));
        }

        // Validation
        if (absence.getAbsenceType() == AbsenceType.SPECIAL && absence.getDate() == null) {
            throw new IllegalArgumentException("Date is required for SPECIAL absences");
        }
        if (absence.getAbsenceType() == AbsenceType.RECURRING && (absence.getWeekday() == null || absence.getWeekday().trim().isEmpty())) {
            throw new IllegalArgumentException("Weekday is required for RECURRING absences");
        }

        return absence;
    }

    public JSONAbsenceDTO convertEntityToDTO(Absence absence) {
        JSONAbsenceDTO dto = new JSONAbsenceDTO();
        dto.setId(absence.getId());
        if (absence.getTherapist() != null) {
            dto.setTherapistId(absence.getTherapist().getId());
        }
        // Format dates as strings
        dto.setDate(absence.getDate() != null ? absence.getDate().toString() : null);
        dto.setEndDate(absence.getEndDate() != null ? absence.getEndDate().toString() : null);
        dto.setWeekday(absence.getWeekday());
        dto.setStartTime(absence.getStartTime() != null ? absence.getStartTime().toString() : null);
        dto.setEndTime(absence.getEndTime() != null ? absence.getEndTime().toString() : null);
        dto.setReason(absence.getReason());
        dto.setAbsenceType(absence.getAbsenceType() != null ? absence.getAbsenceType().name() : null);
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
        // Verwende die System-Zeitzone für konsistente Datumsbehandlung
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        // Verwende die System-Zeitzone statt UTC für korrekte Zeitbehandlung
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date localDateToDate(LocalDate localDate) {
        // Verwende die System-Zeitzone für konsistente Konvertierung
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date localDateTimeToDate(LocalDateTime localDateTime) {
        // Verwende die System-Zeitzone für konsistente Konvertierung
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Parse a string date (YYYY-MM-DD) directly to LocalDate without timezone conversion
     */
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            // Handle ISO format with time part (YYYY-MM-DDTHH:mm:ss)
            if (dateStr.contains("T")) {
                dateStr = dateStr.split("T")[0];
            }
            return java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    /**
     * Parse a string time directly to LocalTime without timezone conversion
     */
    private LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;
        try {
            // Handle ISO format (HH:mm or HH:mm:ss)
            if (timeStr.contains("T")) {
                // If it's a full datetime string, extract just the time part
                String[] parts = timeStr.split("T");
                timeStr = parts.length > 1 ? parts[1] : timeStr;
            }
            // Remove milliseconds if present
            if (timeStr.contains(".")) {
                timeStr = timeStr.split("\\.")[0];
            }
            return java.time.LocalTime.parse(timeStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format: " + timeStr);
        }
    }

    /**
     * Format LocalDateTime to ISO string
     */
    private String formatLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.toString();
    }}