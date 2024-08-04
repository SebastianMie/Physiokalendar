package com.example.physiokalendar.controller;

import com.example.physiokalendar.entity.Exception;
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
    public List<Exception> getAllExceptions() {
        return exceptionService.getAllExceptions();
    }

    @GetMapping("/{id}")
    public Optional<Exception> getExceptionById(@PathVariable Long id) {
        return exceptionService.getExceptionById(id);
    }

    @PostMapping
    public Exception createOrUpdateException(@RequestBody Exception exception) {
        return exceptionService.saveException(exception);
    }

    @DeleteMapping("/{id}")
    public void deleteException(@PathVariable Long id) {
        exceptionService.deleteException(id);
    }
}
