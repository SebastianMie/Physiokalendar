package com.example.physiokalendar.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.dto.JSONTherapistDTO;
import com.example.physiokalendar.service.TherapistService;

@RestController
@RequestMapping("/api/therapists")
public class TherapistController {

    @Autowired
    private TherapistService therapistService;

    @GetMapping
    public List<JSONTherapistDTO> getAllTherapists() {
        return therapistService.getAllTherapists();
    }

    @GetMapping("/{id}")
    public JSONTherapistDTO getTherapistById(@PathVariable String id) {
        return therapistService.getTherapistById(Long.valueOf(id));
    }

    @PostMapping
    public JSONTherapistDTO createOrUpdateTherapist(@RequestBody JSONTherapistDTO therapistDTO) {
        return therapistService.saveTherapist(therapistDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteTherapist(@PathVariable String id) {
        therapistService.deleteTherapist(id);
    }
}
