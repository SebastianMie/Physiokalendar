package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.Exception;
import com.example.physiokalendar.repository.ExceptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExceptionService {

    @Autowired
    private ExceptionRepository exceptionRepository;

    public List<Exception> getAllExceptions() {
        return exceptionRepository.findAll();
    }

    public Optional<Exception> getExceptionById(Long id) {
        return exceptionRepository.findById(id);
    }

    public Exception saveException(Exception exception) {
        return exceptionRepository.save(exception);
    }

    public void deleteException(Long id) {
        exceptionRepository.deleteById(id);
    }
}
