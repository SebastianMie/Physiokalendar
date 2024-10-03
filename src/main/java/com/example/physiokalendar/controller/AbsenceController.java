package com.example.physiokalendar.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<JSONAbsenceDTO> getAllAbsences() {
        List<Absence> absences = absenceService.getAllAbsences();
        return absenceService.convertEntitiesToDTOs(absences);
    }

    @GetMapping("/{id}")
    public Optional<Absence> getAbsenceById(@PathVariable Long id) {
        return absenceService.getAbsenceById(id);
    }

    @GetMapping("/therapist/{therapistId}/date")
    public ResponseEntity<List<JSONAbsenceDTO>> getAbsencesByDateOrWeekday(
            @PathVariable Long therapistId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String weekday) {

        List<Absence> absences = new ArrayList<>();

        if (date != null) {
            absences.addAll(absenceService.getAbsencesByTherapistAndDate(therapistId, date));
        } 
        if (weekday != null) {
            absences.addAll(absenceService.getAbsencesByTherapistAndWeekday(therapistId, weekday));
        } 
        if (date == null && weekday == null) {
            absences = absenceService.getAbsencesByTherapistId(therapistId);
        }

        List<JSONAbsenceDTO> absenceDTOs = absenceService.convertEntitiesToDTOs(absences);
        return ResponseEntity.ok(absenceDTOs);
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
