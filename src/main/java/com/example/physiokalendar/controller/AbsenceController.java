package com.example.physiokalendar.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.dto.JSONAbsenceDTO;
import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.service.AbsenceService;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    @Autowired
    private AbsenceService absenceService;

    @GetMapping
    public List<Absence> getAllAbsences() {
        return absenceService.getAllAbsences();
    }

    @GetMapping("/{id}")
    public Optional<Absence> getAbsenceById(@PathVariable Long id) {
        return absenceService.getAbsenceById(id);
    }

    @GetMapping("/therapist/{id}")
    public List<Absence> getAbsenceByTherapistId(@PathVariable Long id) {
        return absenceService.getAbsencesByTherapistId(id);
    }

    @PostMapping
    public Absence createOrUpdateAbsence(@RequestBody JSONAbsenceDTO absence) {
        return absenceService.saveAbsence(absence);
    }

    @PutMapping("/{id}")
    public Absence updateAbsence(@PathVariable Long id, @RequestBody JSONAbsenceDTO absenceDTO) {
        return absenceService.saveAbsence(absenceDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteAbsence(@PathVariable Long id) {
        absenceService.deleteAbsence(id);
    }
}
