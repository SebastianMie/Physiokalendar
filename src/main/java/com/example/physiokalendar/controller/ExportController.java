package com.example.physiokalendar.controller;

import com.example.physiokalendar.service.ExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller for data export functions.
 * Supports CSV, JSON, and iCalendar formats.
 */
@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:5173"})
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Export appointments to CSV format.
     * GET /api/export/appointments.csv?from=2026-02-01&to=2026-02-28&therapistId=1
     */
    @GetMapping("/appointments.csv")
    public ResponseEntity<String> exportAppointmentsCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long therapistId) {

        String csv = exportService.exportAppointmentsCsv(from, to, therapistId);
        String filename = generateFilename("termine", from, to, "csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    /**
     * Export appointments to JSON format.
     * GET /api/export/appointments.json?from=2026-02-01&to=2026-02-28&therapistId=1
     */
    @GetMapping("/appointments.json")
    public ResponseEntity<String> exportAppointmentsJson(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long therapistId) throws IOException {

        String json = exportService.exportAppointmentsJson(from, to, therapistId);
        String filename = generateFilename("termine", from, to, "json");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /**
     * Export therapist's calendar to iCalendar format.
     * GET /api/export/therapists/{id}/calendar.ics?from=2026-02-01&to=2026-02-28
     */
    @GetMapping("/therapists/{id}/calendar.ics")
    public ResponseEntity<String> exportTherapistCalendar(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        try {
            String ics = exportService.exportTherapistCalendarIcs(id, from, to);
            String filename = "therapeut-" + id + "-kalender.ics";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                    .body(ics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Export appointments as printable HTML (can be printed to PDF).
     * GET /api/export/appointments.html?from=2026-02-01&to=2026-02-28&therapistId=1
     */
    @GetMapping("/appointments.html")
    public ResponseEntity<String> exportAppointmentsHtml(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long therapistId) {

        String html = exportService.exportAppointmentsPdfHtml(from, to, therapistId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    /**
     * Print/PDF endpoint (returns HTML for browser printing).
     * GET /api/print/appointments.pdf?from=2026-02-01&to=2026-02-28&therapistId=1
     */
    @GetMapping("/print/appointments")
    public ResponseEntity<String> printAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long therapistId) {

        // For now, return HTML that can be printed in browser
        // Future: Integrate with a PDF generation library
        String html = exportService.exportAppointmentsPdfHtml(from, to, therapistId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String generateFilename(String prefix, LocalDate from, LocalDate to, String extension) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        return prefix + "_" + from.format(fmt) + "_" + to.format(fmt) + "." + extension;
    }
}
