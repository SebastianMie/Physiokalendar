// AppointmentController.java
package com.example.physiokalendar.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.physiokalendar.dto.AppointmentDraftDTO;
import com.example.physiokalendar.dto.AppointmentSaveResult;
import com.example.physiokalendar.dto.ConflictCheckDTO;
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
    public ResponseEntity<?> createOrUpdateAppointment(
            @RequestBody JSONAppointmentDTO appointmentDTO,
            @RequestParam(defaultValue = "false") boolean forceOnConflict) {
        try {
            AppointmentSaveResult result = appointmentService.saveAppointmentWithConflictCheck(appointmentDTO, forceOnConflict);

            if (!result.isSaved() && result.hasConflicts()) {
                // Return conflict info without saving
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result.getConflictCheck());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Erstellen des Termins: " + e.getMessage()));
        }
    }

    /**
     * Move an appointment (Drag & Drop).
     * POST /api/appointments/{id}/move
     */
    @PostMapping("/{id}/move")
    public ResponseEntity<?> moveAppointment(
            @PathVariable Long id,
            @RequestBody MoveAppointmentRequest request,
            @RequestParam(defaultValue = "false") boolean forceOnConflict) {
        try {
            AppointmentSaveResult result = appointmentService.moveAppointment(
                    id,
                    request.getNewDate(),
                    request.getNewStartTime(),
                    request.getNewEndTime(),
                    request.getNewTherapistId(),
                    forceOnConflict);

            if (!result.isSaved() && result.hasConflicts()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result.getConflictCheck());
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Verschieben: " + e.getMessage()));
        }
    }

    /**
     * Cancel an appointment (soft delete).
     * POST /api/appointments/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            Appointment cancelled = appointmentService.cancelAppointment(id, reason);
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Stornieren: " + e.getMessage()));
        }
    }

    /**
     * Check conflicts for a draft appointment (before saving).
     * POST /api/appointments/check-conflicts
     */
    @PostMapping("/check-conflicts")
    public ResponseEntity<ConflictCheckDTO> checkConflicts(@RequestBody AppointmentDraftDTO draft) {
        ConflictCheckDTO result = appointmentService.checkConflictsForDraft(draft);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable Long id,
            @RequestBody JSONAppointmentDTO appointmentDTO,
            @RequestParam(defaultValue = "false") boolean forceOnConflict) {
        try {
            if (!appointmentService.getAppointmentById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Termin nicht gefunden."));
            }

            appointmentDTO.setId(id);
            AppointmentSaveResult result = appointmentService.saveAppointmentWithConflictCheck(appointmentDTO, forceOnConflict);

            if (!result.isSaved() && result.hasConflicts()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result.getConflictCheck());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Aktualisieren: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        try {
            appointmentService.deleteAppointment(id);
            return ResponseEntity.ok(Map.of("message", "Termin gelöscht"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Löschen: " + e.getMessage()));
        }
    }

    /**
     * Get all appointments for a specific therapist.
     * GET /api/appointments/therapist/{therapistId}
     */
    @GetMapping("/therapist/{therapistId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByTherapist(
            @PathVariable Long therapistId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
            LocalDate toDate = to != null ? LocalDate.parse(to) : null;

            List<Appointment> appointments = appointmentService.getAppointmentsByTherapist(therapistId, fromDate, toDate);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all appointments for a specific patient.
     * GET /api/appointments/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
            LocalDate toDate = to != null ? LocalDate.parse(to) : null;

            List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId, fromDate, toDate);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get appointments for a date range.
     * GET /api/appointments/range
     */
    @GetMapping("/range")
    public ResponseEntity<List<Appointment>> getAppointmentsByDateRange(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);

            List<Appointment> appointments = appointmentService.getAppointmentsByDateRange(fromDate, toDate);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO for move appointment request.
     */
    @lombok.Data
    public static class MoveAppointmentRequest {
        private LocalDate newDate;
        private LocalDateTime newStartTime;
        private LocalDateTime newEndTime;
        private Long newTherapistId; // Optional - null means same therapist
    }
}
