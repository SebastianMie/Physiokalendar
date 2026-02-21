package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.AppSetting;
import com.example.physiokalendar.repository.AppSettingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing application settings/configuration.
 * Provides centralized access to all app configuration stored in the database.
 */
@Service
@Slf4j
@AllArgsConstructor
public class AppSettingService {

    private final AppSettingRepository repository;

    /**
     * Get a setting value by key. Returns empty if not found.
     */
    public Optional<String> getSetting(String key) {
        return repository.findByKey(key)
                .map(AppSetting::getValue);
    }

    /**
     * Get a setting value by key, with a default fallback.
     */
    public String getSettingOrDefault(String key, String defaultValue) {
        return getSetting(key).orElse(defaultValue);
    }

    /**
     * Get a setting as boolean.
     */
    public boolean getSettingAsBoolean(String key, boolean defaultValue) {
        return getSetting(key)
                .map(v -> v.equalsIgnoreCase("true") || v.equals("1"))
                .orElse(defaultValue);
    }

    /**
     * Save or update a setting.
     */
    @Transactional
    public AppSetting saveSetting(String key, String value, String category, String description) {
        Optional<AppSetting> existing = repository.findByKey(key);

        AppSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setValue(value);
            setting.setDescription(description);
        } else {
            setting = AppSetting.builder()
                    .key(key)
                    .value(value)
                    .category(category)
                    .description(description)
                    .build();
        }

        return repository.save(setting);
    }

    /**
     * Get all settings in a category.
     */
    public List<AppSetting> getSettingsByCategory(String category) {
        return repository.findByCategory(category);
    }

    /**
     * Check if a day is active based on opening hours setting.
     * Example keys: "MONDAY_ACTIVE", "TUESDAY_ACTIVE", "SATURDAY_ACTIVE"
     */
    public boolean isDayActive(String dayName) {
        String key = dayName.toUpperCase() + "_ACTIVE";
        return getSettingAsBoolean(key, false);
    }

    /**
     * Get opening time for a day.
     * Example keys: "MONDAY_OPEN_TIME", "SATURDAY_OPEN_TIME"
     */
    public Optional<String> getDayOpenTime(String dayName) {
        String key = dayName.toUpperCase() + "_OPEN_TIME";
        return getSetting(key);
    }

    /**
     * Get closing time for a day.
     * Example keys: "MONDAY_CLOSE_TIME", "SATURDAY_CLOSE_TIME"
     */
    public Optional<String> getDayCloseTime(String dayName) {
        String key = dayName.toUpperCase() + "_CLOSE_TIME";
        return getSetting(key);
    }

    /**
     * Initialize default opening hours if not already set.
     * Mon-Fri: 08:00-18:00, Sat-Sun: closed
     */
    @Transactional
    public void initializeDefaultOpeningHours() {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        String[] dayNames = {"Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"};

        for (int i = 0; i < days.length; i++) {
            String dayKey = days[i];
            String dayName = dayNames[i];

            // Only init if not already set
            if (!repository.existsByKey(dayKey + "_ACTIVE")) {
                boolean isWeekday = i < 5; // Mon-Fri

                saveSetting(
                        dayKey + "_ACTIVE",
                        String.valueOf(isWeekday),
                        "OPENING_HOURS",
                        "Is " + dayName + " an open day"
                );

                if (isWeekday) {
                    saveSetting(
                            dayKey + "_OPEN_TIME",
                            "08:00",
                            "OPENING_HOURS",
                            dayName + " opening time"
                    );
                    saveSetting(
                            dayKey + "_CLOSE_TIME",
                            "18:00",
                            "OPENING_HOURS",
                            dayName + " closing time"
                    );
                }
            }
        }

        log.info("Default opening hours initialized");
    }
}
