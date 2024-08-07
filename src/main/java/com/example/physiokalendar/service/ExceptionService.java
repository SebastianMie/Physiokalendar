package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.AbsenceException;
import com.example.physiokalendar.repository.ExceptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExceptionService {

    @Autowired
    private ExceptionRepository exceptionRepository;

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
}
