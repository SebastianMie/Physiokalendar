package com.example.physiokalendar.service;

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
        exception.setDate(dto.getDate());
        exception.setStartTime(dto.getDate());
        exception.setEndTime(dto.getDate());
        // Weitere Felder falls nötig
        return exception;
    }

    public static JSONAbsenceExceptionDTO convertEntityToDTO(AbsenceException exception) {
        JSONAbsenceExceptionDTO dto = new JSONAbsenceExceptionDTO();
        dto.setId(exception.getId());
        dto.setDate(exception.getDate());
        dto.setWeekday(dto.getWeekday());
        dto.setStartTime(exception.getDate());
        dto.setEndTime(exception.getDate());
        // Weitere Felder falls nötig
        return dto;
    }
}
