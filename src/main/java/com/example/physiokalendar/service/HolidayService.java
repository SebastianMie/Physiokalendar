package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.AppSetting;
import com.example.physiokalendar.repository.AppSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class HolidayService {

    public static final String CATEGORY_HOLIDAYS = "HOLIDAYS";
    private static final String KEY_PREFIX = "HOLIDAY_";

    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;

    public HolidayService(AppSettingRepository appSettingRepository, ObjectMapper objectMapper) {
        this.appSettingRepository = appSettingRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lädt alle Feiertage aus der Datenbank (app_settings mit category = HOLIDAYS)
     */
    private List<Holiday> loadHolidays() {
        List<AppSetting> settings = appSettingRepository.findByCategory(CATEGORY_HOLIDAYS);
        List<Holiday> holidays = new ArrayList<>();

        for (AppSetting setting : settings) {
            try {
                // Key format: HOLIDAY_2026-01-01
                String dateStr = setting.getKey().replace(KEY_PREFIX, "");
                HolidayValue value = objectMapper.readValue(setting.getValue(), HolidayValue.class);

                Holiday holiday = new Holiday();
                holiday.setDate(dateStr);
                holiday.setName(value.getName());
                holiday.setRecurring(value.isRecurring());
                holidays.add(holiday);
            } catch (JsonProcessingException e) {
                log.warn("Fehler beim Parsen des Feiertags {}: {}", setting.getKey(), e.getMessage());
            }
        }

        return holidays;
    }

    /**
     * Gibt alle Feiertage zurück (frisch aus DB)
     */
    public List<Holiday> getAllHolidays() {
        return loadHolidays();
    }

    /**
     * Speichert einen Feiertag in der Datenbank
     */
    @Transactional
    public Holiday saveHoliday(Holiday holiday) {
        String key = KEY_PREFIX + holiday.getDate();

        try {
            HolidayValue value = new HolidayValue(holiday.getName(), holiday.isRecurring());
            String valueJson = objectMapper.writeValueAsString(value);

            AppSetting setting = appSettingRepository.findByKey(key)
                    .orElse(AppSetting.builder()
                            .key(key)
                            .category(CATEGORY_HOLIDAYS)
                            .build());

            setting.setValue(valueJson);
            setting.setDescription("Feiertag: " + holiday.getName());
            appSettingRepository.save(setting);

            log.info("Feiertag gespeichert: {} - {}", holiday.getDate(), holiday.getName());
            return holiday;
        } catch (JsonProcessingException e) {
            log.error("Fehler beim Speichern des Feiertags {}", holiday.getDate(), e);
            throw new RuntimeException("Fehler beim Speichern des Feiertags", e);
        }
    }

    /**
     * Löscht einen Feiertag
     */
    @Transactional
    public void deleteHoliday(String date) {
        String key = KEY_PREFIX + date;
        appSettingRepository.findByKey(key).ifPresent(appSettingRepository::delete);
        log.info("Feiertag gelöscht: {}", date);
    }

    /**
     * Gibt nur die Feiertags-Daten als Set zurück (für schnelle Lookups)
     */
    public Set<LocalDate> getHolidayDates() {
        Set<LocalDate> dates = new HashSet<>();
        for (Holiday h : loadHolidays()) {
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
        return loadHolidays().stream()
                .filter(h -> h.getDate().equals(date.toString()))
                .map(Holiday::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * Inner class für JSON-Value Mapping (in app_settings.value gespeichert)
     */
    public static class HolidayValue {
        private String name;
        private boolean recurring;

        public HolidayValue() {}

        public HolidayValue(String name, boolean recurring) {
            this.name = name;
            this.recurring = recurring;
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

    /**
     * Holiday-Datenklasse (für API-Responses)
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
