// AppointmentController.java
package com.example.physiokalendar.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.dto.JSONAppointmentDTO;
import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.service.AppointmentService;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping
    public List<Appointment> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    public Optional<Appointment> getAppointmentById(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }

    @PostMapping
    public ResponseEntity<String> createOrUpdateAppointment(@RequestBody JSONAppointmentDTO appointmentDTO) {
        try {
            if (appointmentService.checkForConflicts(appointmentService.convertDTOToEntity(appointmentDTO))) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Konflikt mit einem bestehenden Termin für den Therapeuten.");
            }

            appointmentService.saveAppointment(appointmentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Termin erfolgreich erstellt.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Erstellen des Termins.");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateAppointment(@PathVariable Long id, @RequestBody JSONAppointmentDTO appointmentDTO) {
        try {
            // Überprüfe, ob der Termin existiert
            if (!appointmentService.getAppointmentById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Termin nicht gefunden.");
            }

            // Check for conflicts
            if (appointmentService.checkForConflicts(appointmentService.convertDTOToEntity(appointmentDTO))) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Konflikt mit einem bestehenden Termin für den Therapeuten.");
            }

            // Aktualisiere den Termin
            appointmentService.saveAppointment(appointmentDTO);
            return ResponseEntity.status(HttpStatus.OK).body("Termin erfolgreich aktualisiert.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fehler beim Aktualisieren des Termins.");
        }
    }


    @DeleteMapping("/{id}")
    public void deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
    }
}
