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

import com.example.physiokalendar.entity.Cancellation;
import com.example.physiokalendar.service.CancellationService;

@RestController
@RequestMapping("/api/cancellations")
public class CancellationController {

    @Autowired
    private CancellationService cancellationService;

    @GetMapping
    public List<Cancellation> getAllCancellations() {
        return cancellationService.getAllCancellations();
    }

    @GetMapping("/{id}")
    public Optional<Cancellation> getCancellationById(@PathVariable Long id) {
        return cancellationService.getCancellationById(id);
    }

    @PostMapping
    public Cancellation createOrUpdateCancellation(@RequestBody Cancellation cancellation) {
        return cancellationService.saveCancellation(cancellation);
    }

    @DeleteMapping("/{id}")
    public void deleteCancellation(@PathVariable Long id) {
        cancellationService.deleteCancellation(id);
    }
}
