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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.physiokalendar.dto.AppointmentDraftDTO;
import com.example.physiokalendar.dto.AppointmentSaveResult;
import com.example.physiokalendar.dto.AppointmentStatusUpdateDTO;
import com.example.physiokalendar.dto.ConflictCheckDTO;
import com.example.physiokalendar.dto.JSONAppointmentDTO;
import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.AppointmentStatus;
import com.example.physiokalendar.service.AppointmentService;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // Try to parse date strings flexibly: accept yyyy-MM-dd or full ISO timestamps
    private LocalDate parseToLocalDate(String input) {
        if (input == null || input.isEmpty()) return null;
        try {
            return LocalDate.parse(input);
        } catch (java.time.format.DateTimeParseException ignored) {
        }
        try {
            java.time.Instant inst = java.time.Instant.parse(input);
            return java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(input);
            return ldt.toLocalDate();
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Paginated endpoint for appointments with server-side filtering and sorting.
     * GET /api/appointments/paginated
     *
     * @param page Page number (0-indexed)
     * @param size Page size (default 50)
     * @param sortBy Sort field: date, time, patient, therapist
     * @param sortDir Sort direction: asc, desc
     * @param dateFrom Filter: start date (yyyy-MM-dd)
     * @param dateTo Filter: end date (yyyy-MM-dd)
     * @param therapistId Filter: therapist ID
     * @param status Filter: appointment status
     * @param search Search term (patient name, therapist name, comment)
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<Appointment>> getPaginatedAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            // Parse dates (accept either yyyy-MM-dd or full ISO timestamps)
            LocalDate fromDate = parseToLocalDate(dateFrom);
            LocalDate toDate = parseToLocalDate(dateTo);

            // Parse status
            AppointmentStatus appointmentStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // Invalid status, ignore filter
                }
            }

            // Build sort
            String sortProperty = switch (sortBy) {
                case "time" -> "startTime";
                case "patient" -> "patient.lastName";
                case "therapist" -> "therapist.lastName";
                default -> "date";
            };
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortProperty);

            // Add secondary sort by startTime for date/patient/therapist
            if (!sortBy.equals("time")) {
                sort = sort.and(Sort.by(Sort.Direction.ASC, "startTime"));
            }

            PageRequest pageRequest = PageRequest.of(page, size, sort);

            Page<Appointment> appointments = appointmentService.getSingleAppointmentsPaginated(
                    fromDate, toDate, therapistId, appointmentStatus, search, pageRequest);

            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extended paginated endpoint for appointments with appointment type filter.
     * Used for therapist detail view and patient detail view with faceted search.
     * GET /api/appointments/paginated-extended
     *
     * @param appointmentType Filter: 'series' (only series), 'single' (only single), null (all)
     * @param timeFilter Filter: 'upcoming' (today+future, non-cancelled), 'past' (before today), null (all)
     */
    @GetMapping("/paginated-extended")
    public ResponseEntity<Page<Appointment>> getPaginatedAppointmentsExtended(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long therapistId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String appointmentType,
            @RequestParam(required = false) String timeFilter) {
        try {
            // Parse dates based on timeFilter
            LocalDate fromDate = null;
            LocalDate toDate = null;
            LocalDate today = LocalDate.now();

            if ("upcoming".equalsIgnoreCase(timeFilter)) {
                fromDate = today;
            } else if ("past".equalsIgnoreCase(timeFilter)) {
                toDate = today.minusDays(1);
            } else {
                fromDate = parseToLocalDate(dateFrom);
                toDate = parseToLocalDate(dateTo);
            }

            // Parse appointment type
            Boolean appointmentTypeBool = null;
            if ("series".equalsIgnoreCase(appointmentType)) {
                appointmentTypeBool = true;
            } else if ("single".equalsIgnoreCase(appointmentType)) {
                appointmentTypeBool = false;
            }

            // Parse status (exclude cancelled for upcoming)
            AppointmentStatus appointmentStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            // Build sort
            String sortProperty = switch (sortBy) {
                case "time" -> "startTime";
                case "patient" -> "patient.lastName";
                case "therapist" -> "therapist.lastName";
                default -> "date";
            };
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortProperty);
            if (!sortBy.equals("time")) {
                sort = sort.and(Sort.by(Sort.Direction.ASC, "startTime"));
            }

            PageRequest pageRequest = PageRequest.of(page, size, sort);

            Page<Appointment> appointments = appointmentService.getAppointmentsPaginated(
                    appointmentTypeBool, fromDate, toDate, therapistId, patientId, appointmentStatus, search, pageRequest);

            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


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
     * Update appointment status.
     * PATCH /api/appointments/{id}/status
     *
     * Status transitions follow business rules:
     * - SCHEDULED: Initial status
     * - CONFIRMED: Manually confirmed appointment
     * - COMPLETED: Appointment completed (past appointment or manual marking)
     * - NO_SHOW: Patient didn't show up (should be set for past appointments)
     * - CANCELLED: Appointment cancelled (can transition from any state)
     *
     * @param id the appointment to update
     * @param statusUpdateDTO contains new status and optional reason
     * @return updated appointment
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestBody AppointmentStatusUpdateDTO statusUpdateDTO) {
        try {
            Appointment updated = appointmentService.updateAppointmentStatus(id, statusUpdateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Aktualisieren des Status: " + e.getMessage()));
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
