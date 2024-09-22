package com.example.physiokalendar.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.dto.JSONTherapistDTO;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.service.TherapistService;

@RestController
@RequestMapping("/api/therapists")
public class TherapistController {
    private final TherapistService therapistService;

    public TherapistController(TherapistService therapistService) {
        this.therapistService = therapistService;
    }

    @GetMapping
    public List<JSONTherapistDTO> getAllTherapists() {
        return therapistService.getAllTherapists();
    }

    @GetMapping("/{id}")
    public JSONTherapistDTO getTherapistById(@PathVariable Long id) {
        return therapistService.getTherapistById(id);
    }

    @PostMapping
    public Therapist createTherapist(@RequestBody JSONTherapistDTO therapistDTO) {
        return therapistService.createTherapist(therapistDTO);
    }

    @PutMapping("/{id}")
    public Therapist updateTherapist(@PathVariable Long id, @RequestBody JSONTherapistDTO therapistDTO) {
        return therapistService.updateTherapist(id, therapistDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteTherapist(@PathVariable Long id) {
        therapistService.deleteTherapist(id);
    }
}
