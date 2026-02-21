package com.example.physiokalendar.controller;

import com.example.physiokalendar.service.HolidayService;
import com.example.physiokalendar.service.HolidayService.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * GET /api/holidays - Gibt alle Feiertage zurück
     */
    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        List<Holiday> holidays = holidayService.getAllHolidays();
        return ResponseEntity.ok(holidays);
    }

    /**
     * POST /api/holidays - Erstellt oder aktualisiert einen Feiertag
     */
    @PostMapping
    public ResponseEntity<Holiday> saveHoliday(@RequestBody Holiday holiday) {
        Holiday saved = holidayService.saveHoliday(holiday);
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /api/holidays/{date} - Löscht einen Feiertag
     */
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable String date) {
        holidayService.deleteHoliday(date);
        return ResponseEntity.noContent().build();
    }
}
