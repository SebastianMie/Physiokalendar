package com.example.physiokalendar.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.entity.AbsenceException;
import com.example.physiokalendar.service.AbsenceExceptionService;



@RestController
@RequestMapping("/api/exceptions")
public class ExceptionController {

    @Autowired
    private AbsenceExceptionService exceptionService;

    @GetMapping
    public List<AbsenceException> getAllExceptions() {
        return exceptionService.getAllExceptions();
    }

    @GetMapping("/{id}")
    public Optional<AbsenceException> getExceptionById(@PathVariable Long id) {
        return exceptionService.getExceptionById(id);
    }

    @PostMapping
    public AbsenceException createOrUpdateException(@RequestBody AbsenceException exception) {
        return exceptionService.saveException(exception);
    }

    @DeleteMapping("/{id}")
    public void deleteException(@PathVariable Long id) {
        exceptionService.deleteException(id);
    }
}
