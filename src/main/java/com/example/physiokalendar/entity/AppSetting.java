package com.example.physiokalendar.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic application settings/configuration.
 * Stores key-value pairs for application configuration.
 * Examples: opening hours, working days, default durations, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "app_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"setting_key"})
})
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration key (e.g., "MONDAY_ACTIVE", "MONDAY_OPEN_TIME", "SATURDAY_ACTIVE")
     */
    @Column(name = "setting_key", nullable = false, length = 100)
    private String key;

    /**
     * Configuration value (e.g., "true", "08:00", "18:00")
     */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    /**
     * Category for grouping settings (e.g., "OPENING_HOURS", "GENERAL", "CALENDAR")
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * Optional description/documentation
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
