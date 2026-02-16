package com.example.physiokalendar.controller;

import com.example.physiokalendar.dto.CalendarRangeDTO;
import com.example.physiokalendar.service.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for calendar range queries.
 * Provides unified view of appointments, series instances, and absences.
 */
@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:5173"})
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Get calendar data for a date range.
     * GET /api/calendar?from=2026-02-01&to=2026-02-28&therapistId=1
     *
     * Returns all appointments, computed series instances, and absence blocks.
     */
    @GetMapping
    public ResponseEntity<CalendarRangeDTO> getCalendarRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long therapistId) {

        // Validate date range (max 3 months to prevent performance issues)
        if (from.plusMonths(3).isBefore(to)) {
            return ResponseEntity.badRequest().build();
        }

        CalendarRangeDTO result = calendarService.getCalendarRange(from, to, therapistId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get calendar data for today.
     * GET /api/calendar/today?therapistId=1
     */
    @GetMapping("/today")
    public ResponseEntity<CalendarRangeDTO> getToday(
            @RequestParam(required = false) Long therapistId) {

        LocalDate today = LocalDate.now();
        CalendarRangeDTO result = calendarService.getCalendarRange(today, today, therapistId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get calendar data for current week.
     * GET /api/calendar/week?therapistId=1
     */
    @GetMapping("/week")
    public ResponseEntity<CalendarRangeDTO> getCurrentWeek(
            @RequestParam(required = false) Long therapistId) {

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        CalendarRangeDTO result = calendarService.getCalendarRange(startOfWeek, endOfWeek, therapistId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get calendar data for current month.
     * GET /api/calendar/month?therapistId=1
     */
    @GetMapping("/month")
    public ResponseEntity<CalendarRangeDTO> getCurrentMonth(
            @RequestParam(required = false) Long therapistId) {

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        CalendarRangeDTO result = calendarService.getCalendarRange(startOfMonth, endOfMonth, therapistId);
        return ResponseEntity.ok(result);
    }
}
