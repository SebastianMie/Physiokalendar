package com.example.physiokalendar.controller;

import com.example.physiokalendar.entity.AbsenceException;
import com.example.physiokalendar.service.ExceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/exceptions")
public class ExceptionController {

    @Autowired
    private ExceptionService exceptionService;

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
