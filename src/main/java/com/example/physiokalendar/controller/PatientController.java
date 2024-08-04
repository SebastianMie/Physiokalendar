package com.example.physiokalendar.controller;

import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @GetMapping
    public List<Patient> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GetMapping("/{id}")
    public Patient getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    @PostMapping
    public Patient createPatient(@RequestBody Patient patient) {
        return PatientService.createPatient(patient);
    }

    @PutMapping("/{id}")
    public Patient updatePatient(@PathVariable Long id, @RequestBody Patient updatedPatient) {
        return patientService.updatePatient(id, updatedPatient);
    }

    @DeleteMapping("/{id}")
    public void deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
    }

    @GetMapping("/filter")
    public List<Patient> filterPatients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Date activeSince,
            @RequestParam(required = false) Date activeUntil,
            @RequestParam(required = false) Boolean isBWO) {
        return patientService.filterPatients(name, activeSince, activeUntil, isBWO);
    }
}
