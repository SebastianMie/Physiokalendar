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

import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.service.TherapistService;

@RestController
@RequestMapping("/api/therapists")
public class TherapistController {

    @Autowired
    private TherapistService therapistService;

    @GetMapping
    public List<Therapist> getAllTherapists() {
        return therapistService.getAllTherapists();
    }

    @GetMapping("/{id}")
    public Optional<Therapist> getTherapistById(@PathVariable Long id) {
        return therapistService.getTherapistById(id);
    }

    @PostMapping
    public Therapist createOrUpdateTherapist(@RequestBody Therapist therapist) {
        return therapistService.saveTherapist(therapist);
    }

    @DeleteMapping("/{id}")
    public void deleteTherapist(@PathVariable Long id) {
        therapistService.deleteTherapist(id);
    }
}
