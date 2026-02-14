package com.example.physiokalendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAbsenceExceptionDTO;
import com.example.physiokalendar.entity.AbsenceException;
import com.example.physiokalendar.repository.AbsenceExceptionRepository;

@Service
public class AbsenceExceptionService {

    @Autowired
    private AbsenceExceptionRepository exceptionRepository;

    public List<AbsenceException> getAllExceptions() {
        return exceptionRepository.findAll();
    }

    public Optional<AbsenceException> getExceptionById(Long id) {
        return exceptionRepository.findById(id);
    }

    public AbsenceException saveException(AbsenceException exception) {
        return exceptionRepository.save(exception);
    }

    public void deleteException(Long id) {
        exceptionRepository.deleteById(id);
    }

    public static AbsenceException convertDTOToEntity(JSONAbsenceExceptionDTO dto) {
        AbsenceException exception = new AbsenceException();
        exception.setId(dto.getId());
        exception.setWeekday(dto.getWeekday());
        exception.setDate(dto.getDate() != null ? dateToLocalDate(dto.getDate()) : null);
        exception.setStartTime(dto.getStartTime() != null ? dateToLocalDateTime(dto.getStartTime()) : null);
        exception.setEndTime(dto.getEndTime() != null ? dateToLocalDateTime(dto.getEndTime()) : null);
        // Weitere Felder falls nötig
        return exception;
    }

    public static JSONAbsenceExceptionDTO convertEntityToDTO(AbsenceException exception) {
        JSONAbsenceExceptionDTO dto = new JSONAbsenceExceptionDTO();
        dto.setId(exception.getId());
        dto.setDate(exception.getDate() != null ? localDateToDate(exception.getDate()) : null);
        dto.setWeekday(exception.getWeekday());
        dto.setStartTime(exception.getStartTime() != null ? localDateTimeToDate(exception.getStartTime()) : null);
        dto.setEndTime(exception.getEndTime() != null ? localDateTimeToDate(exception.getEndTime()) : null);
        // Weitere Felder falls nötig
        return dto;
    }

    private static LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDate();
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(java.time.ZoneId.of("UTC")).toLocalDateTime();
    }

    private static Date localDateToDate(LocalDate localDate) {
        return java.sql.Date.valueOf(localDate);
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return java.sql.Timestamp.valueOf(localDateTime);
    }
}
