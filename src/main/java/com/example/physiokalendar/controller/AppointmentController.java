// AppointmentController.java
package com.example.physiokalendar.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<List<Appointment>> getAppointments(
        @RequestParam(required = false) Long therapistId,
        @RequestParam(required = false) Long patientId,
        @RequestParam(required = false) String date
    ) {
        try {
            Date parsedDate = null;
            // Parse the date only if it's provided
            if (date != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                parsedDate = dateFormat.parse(date);
            }

            // Call the service method with the optional parameters
            List<Appointment> appointments = appointmentService.getAppointmentsByCriteria(therapistId, patientId, parsedDate);

            if (appointments.isEmpty()) {
                return ResponseEntity.noContent().build(); // No appointments found, return 204 No Content
            } else {
                return ResponseEntity.ok(appointments); // Return the found appointments
            }
        } catch (ParseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Return 500 on date parsing error
        }
    }


    @GetMapping("/{id}")
    public Optional<Appointment> getAppointmentById(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }

    @GetMapping("/date")
    public ResponseEntity<List<Appointment>> getAppointmentsForDate(@RequestParam String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            // Hier rufst du die Termine für das gegebene Datum aus dem Service ab
            List<Appointment> appointments = appointmentService.getAppointmentsForDate(dateFormat.parse(date));            if (appointments.isEmpty()) {
                return ResponseEntity.noContent().build(); // Leere Antwort, wenn keine Termine gefunden wurden
            }
            return ResponseEntity.ok(appointments); // Rückgabe der Liste der Termine
        } catch (ParseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Bei Fehlern wird ein interner Server-Fehler gesendet
        }
    }

    @GetMapping("/conflicts")
    public ResponseEntity<List<Appointment>> getAppointmentConflicts() {
        try {
            List<Appointment> conflicts = appointmentService.getAppointmentsWithConflicts();
            if (conflicts.isEmpty()) {
                return ResponseEntity.noContent().build(); // Keine Konflikte gefunden
            }
            return ResponseEntity.status(HttpStatus.OK).body(conflicts); 
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

     @GetMapping("/available")
     public ResponseEntity<List<Appointment>> findAvailableAppointments(
        @RequestParam Long therapistId,
        @RequestParam Long patientId,
        @RequestParam Integer timeOfDayId,
        @RequestParam Integer duration) {

        try {
            List<Appointment> appointments = appointmentService.findAvailableAppointments(therapistId, patientId, timeOfDayId, duration);
            if (appointments.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.status(HttpStatus.OK).body(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<String> createOrUpdateAppointment(@RequestBody JSONAppointmentDTO appointmentDTO) {
        try {
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
