package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.CalendarRangeDTO;
import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for exporting appointment data in various formats.
 */
@Service
@Slf4j
public class ExportService {

    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final TherapistRepository therapistRepository;
    private final CalendarService calendarService;
    private final ObjectMapper objectMapper;

    public ExportService(
            AppointmentRepository appointmentRepository,
            AppointmentSeriesRepository seriesRepository,
            TherapistRepository therapistRepository,
            CalendarService calendarService) {
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = seriesRepository;
        this.therapistRepository = therapistRepository;
        this.calendarService = calendarService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Export appointments to CSV format.
     */
    @Transactional(readOnly = true)
    public String exportAppointmentsCsv(LocalDate from, LocalDate to, Long therapistId) {
        List<Appointment> appointments;
        if (therapistId != null) {
            appointments = appointmentRepository.findByTherapistIdAndDateRange(therapistId, from, to);
        } else {
            appointments = appointmentRepository.findByDateRange(from, to);
        }

        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("ID,Datum,Startzeit,Endzeit,Therapeut_ID,Therapeut_Name,")
           .append("Patient_ID,Patient_Name,Status,Kommentar,Heissluft,Ultraschall,Elektrotherapie,")
           .append("Aus_Serie,Serien_ID\n");

        // Data rows
        for (Appointment apt : appointments) {
            csv.append(apt.getId()).append(",");
            csv.append(apt.getDate()).append(",");
            csv.append(apt.getStartTime().toLocalTime()).append(",");
            csv.append(apt.getEndTime().toLocalTime()).append(",");
            csv.append(apt.getTherapist().getId()).append(",");
            csv.append(escapeCsv(apt.getTherapist().getFullName())).append(",");
            csv.append(apt.getPatient().getId()).append(",");
            csv.append(escapeCsv(apt.getPatient().getFullName())).append(",");
            csv.append(apt.getStatus() != null ? apt.getStatus().name() : "SCHEDULED").append(",");
            csv.append(escapeCsv(apt.getComment())).append(",");
            csv.append(apt.getIsHotair() != null && apt.getIsHotair() ? "Ja" : "Nein").append(",");
            csv.append(apt.getIsUltrasonic() != null && apt.getIsUltrasonic() ? "Ja" : "Nein").append(",");
            csv.append(apt.getIsElectric() != null && apt.getIsElectric() ? "Ja" : "Nein").append(",");
            csv.append(apt.getCreatedBySeriesAppointment() != null && apt.getCreatedBySeriesAppointment() ? "Ja" : "Nein").append(",");
            csv.append(apt.getAppointmentSeries() != null ? apt.getAppointmentSeries().getId() : "").append("\n");
        }

        return csv.toString();
    }

    /**
     * Export appointments to JSON format.
     */
    @Transactional(readOnly = true)
    public String exportAppointmentsJson(LocalDate from, LocalDate to, Long therapistId) throws IOException {
        CalendarRangeDTO calendarData = calendarService.getCalendarRange(from, to, therapistId);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(calendarData);
    }

    /**
     * Export therapist's calendar to iCalendar format.
     */
    @Transactional(readOnly = true)
    public String exportTherapistCalendarIcs(Long therapistId, LocalDate from, LocalDate to) {
        Optional<Therapist> therapistOpt = therapistRepository.findById(therapistId);
        if (therapistOpt.isEmpty()) {
            throw new IllegalArgumentException("Therapist not found: " + therapistId);
        }

        Therapist therapist = therapistOpt.get();
        CalendarRangeDTO calendarData = calendarService.getCalendarRange(from, to, therapistId);

        StringBuilder ics = new StringBuilder();

        // iCalendar header
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Physiokalendar//DE\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("X-WR-CALNAME:").append(escapeIcs(therapist.getFullName())).append(" - Termine\r\n");
        ics.append("X-WR-TIMEZONE:Europe/Berlin\r\n");

        // Add VTIMEZONE component for Europe/Berlin
        ics.append(getTimezoneComponent());

        // Export single appointments
        for (CalendarRangeDTO.CalendarAppointmentDTO apt : calendarData.getAppointments()) {
            if ("CANCELLED".equals(apt.getStatus())) continue;

            ics.append("BEGIN:VEVENT\r\n");
            ics.append("UID:apt-").append(apt.getId()).append("@physiokalendar\r\n");
            ics.append("DTSTAMP:").append(formatIcsDateTime(LocalDateTime.now())).append("\r\n");
            ics.append("DTSTART;TZID=Europe/Berlin:").append(formatIcsDateTime(apt.getStartTime())).append("\r\n");
            ics.append("DTEND;TZID=Europe/Berlin:").append(formatIcsDateTime(apt.getEndTime())).append("\r\n");
            ics.append("SUMMARY:").append(escapeIcs(apt.getPatientName())).append("\r\n");
            if (apt.getComment() != null && !apt.getComment().isEmpty()) {
                ics.append("DESCRIPTION:").append(escapeIcs(apt.getComment())).append("\r\n");
            }
            ics.append("STATUS:CONFIRMED\r\n");
            ics.append("END:VEVENT\r\n");
        }

        // Export series instances (as individual events, not RRULE)
        for (CalendarRangeDTO.CalendarSeriesInstanceDTO instance : calendarData.getSeriesInstances()) {
            if (instance.getIsCancelled() != null && instance.getIsCancelled()) continue;

            LocalDateTime startDt = LocalDateTime.of(instance.getDate(), instance.getStartTime());
            LocalDateTime endDt = LocalDateTime.of(instance.getDate(), instance.getEndTime());

            ics.append("BEGIN:VEVENT\r\n");
            ics.append("UID:series-").append(instance.getSeriesId())
               .append("-").append(instance.getInstanceIndex()).append("@physiokalendar\r\n");
            ics.append("DTSTAMP:").append(formatIcsDateTime(LocalDateTime.now())).append("\r\n");
            ics.append("DTSTART;TZID=Europe/Berlin:").append(formatIcsDateTime(startDt)).append("\r\n");
            ics.append("DTEND;TZID=Europe/Berlin:").append(formatIcsDateTime(endDt)).append("\r\n");
            ics.append("SUMMARY:").append(escapeIcs(instance.getPatientName())).append(" (Serie)\r\n");
            if (instance.getComment() != null && !instance.getComment().isEmpty()) {
                ics.append("DESCRIPTION:").append(escapeIcs(instance.getComment())).append("\r\n");
            }
            ics.append("STATUS:CONFIRMED\r\n");
            ics.append("END:VEVENT\r\n");
        }

        ics.append("END:VCALENDAR\r\n");
        return ics.toString();
    }

    /**
     * Generate PDF export (basic layout).
     * Returns HTML that can be converted to PDF client-side or by a PDF service.
     */
    @Transactional(readOnly = true)
    public String exportAppointmentsPdfHtml(LocalDate from, LocalDate to, Long therapistId) {
        CalendarRangeDTO calendarData = calendarService.getCalendarRange(from, to, therapistId);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Terminübersicht ").append(from).append(" - ").append(to).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; font-size: 12px; margin: 20px; }\n");
        html.append("h1 { font-size: 18px; margin-bottom: 10px; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
        html.append("th, td { border: 1px solid #ccc; padding: 6px; text-align: left; }\n");
        html.append("th { background-color: #f0f0f0; }\n");
        html.append(".cancelled { text-decoration: line-through; color: #999; }\n");
        html.append("@media print { body { -webkit-print-color-adjust: exact; } }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<h1>Terminübersicht: ").append(from).append(" bis ").append(to).append("</h1>\n");

        // Appointments table
        html.append("<h2>Einzeltermine</h2>\n");
        html.append("<table>\n<tr><th>Datum</th><th>Zeit</th><th>Therapeut</th><th>Patient</th><th>Status</th><th>Bemerkung</th></tr>\n");

        for (CalendarRangeDTO.CalendarAppointmentDTO apt : calendarData.getAppointments()) {
            String rowClass = "CANCELLED".equals(apt.getStatus()) ? " class=\"cancelled\"" : "";
            html.append("<tr").append(rowClass).append(">");
            html.append("<td>").append(apt.getDate()).append("</td>");
            html.append("<td>").append(apt.getStartTime().toLocalTime()).append(" - ")
                .append(apt.getEndTime().toLocalTime()).append("</td>");
            html.append("<td>").append(escapeHtml(apt.getTherapistName())).append("</td>");
            html.append("<td>").append(escapeHtml(apt.getPatientName())).append("</td>");
            html.append("<td>").append(apt.getStatus()).append("</td>");
            html.append("<td>").append(apt.getComment() != null ? escapeHtml(apt.getComment()) : "").append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>\n");

        // Series instances table
        if (!calendarData.getSeriesInstances().isEmpty()) {
            html.append("<h2>Serientermine</h2>\n");
            html.append("<table>\n<tr><th>Datum</th><th>Zeit</th><th>Therapeut</th><th>Patient</th><th>Status</th></tr>\n");

            for (CalendarRangeDTO.CalendarSeriesInstanceDTO instance : calendarData.getSeriesInstances()) {
                String rowClass = (instance.getIsCancelled() != null && instance.getIsCancelled())
                        ? " class=\"cancelled\"" : "";
                html.append("<tr").append(rowClass).append(">");
                html.append("<td>").append(instance.getDate()).append("</td>");
                html.append("<td>").append(instance.getStartTime()).append(" - ")
                    .append(instance.getEndTime()).append("</td>");
                html.append("<td>").append(escapeHtml(instance.getTherapistName())).append("</td>");
                html.append("<td>").append(escapeHtml(instance.getPatientName())).append("</td>");
                html.append("<td>").append(instance.getIsCancelled() != null && instance.getIsCancelled()
                        ? "Abgesagt" : "Aktiv").append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }

        html.append("<p style=\"margin-top: 20px; font-size: 10px; color: #666;\">")
            .append("Generiert am ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
            .append("</p>\n");
        html.append("</body>\n</html>");

        return html.toString();
    }

    // Helper methods
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeIcs(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace(",", "\\,")
                    .replace(";", "\\;")
                    .replace("\n", "\\n");
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    private String formatIcsDateTime(LocalDateTime dt) {
        return dt.format(ISO_DATETIME);
    }

    private String getTimezoneComponent() {
        return """
            BEGIN:VTIMEZONE
            TZID:Europe/Berlin
            X-LIC-LOCATION:Europe/Berlin
            BEGIN:DAYLIGHT
            TZOFFSETFROM:+0100
            TZOFFSETTO:+0200
            TZNAME:CEST
            DTSTART:19700329T020000
            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU
            END:DAYLIGHT
            BEGIN:STANDARD
            TZOFFSETFROM:+0200
            TZOFFSETTO:+0100
            TZNAME:CET
            DTSTART:19701025T030000
            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU
            END:STANDARD
            END:VTIMEZONE
            """.replace("\n", "\r\n");
    }
}
