package com.example.physiokalendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class HolidayService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private List<Holiday> holidays;

    public HolidayService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.holidays = loadHolidays();
    }

    /**
     * Lädt alle Feiertage aus holidays.json
     */
    private List<Holiday> loadHolidays() {
        try {
            Resource resource = resourceLoader.getResource("classpath:holidays.json");
            HolidayData data = objectMapper.readValue(resource.getFile(), HolidayData.class);
            return data.getHolidays();
        } catch (IOException e) {
            log.error("Fehler beim Laden der Feiertage aus holidays.json", e);
            return List.of();
        }
    }

    /**
     * Gibt alle Feiertage zurück
     */
    public List<Holiday> getAllHolidays() {
        return holidays;
    }

    /**
     * Gibt nur die Feiertags-Daten als Set zurück (für schnelle Lookups)
     */
    public Set<LocalDate> getHolidayDates() {
        Set<LocalDate> dates = new HashSet<>();
        for (Holiday h : holidays) {
            dates.add(LocalDate.parse(h.getDate()));
        }
        return dates;
    }

    /**
     * Überprüft, ob ein Datum ein Feiertag ist
     */
    public boolean isHoliday(LocalDate date) {
        return getHolidayDates().contains(date);
    }

    /**
     * Gibt den Namen eines Feiertags zurück, wenn das Datum ein Feiertag ist
     */
    public String getHolidayName(LocalDate date) {
        return holidays.stream()
                .filter(h -> h.getDate().equals(date.toString()))
                .map(Holiday::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * Inner class für JSON-Mapping
     */
    public static class HolidayData {
        private List<Holiday> holidays;

        public List<Holiday> getHolidays() {
            return holidays;
        }

        public void setHolidays(List<Holiday> holidays) {
            this.holidays = holidays;
        }
    }

    /**
     * Holiday-Datenklasse
     */
    public static class Holiday {
        private String date; // YYYY-MM-DD
        private String name;
        private boolean recurring;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isRecurring() {
            return recurring;
        }

        public void setRecurring(boolean recurring) {
            this.recurring = recurring;
        }
    }
}
