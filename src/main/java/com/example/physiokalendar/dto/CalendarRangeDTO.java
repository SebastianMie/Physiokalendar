package com.example.physiokalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO for Calendar Range API response.
 * Contains all calendar items (appointments, series instances, absences) for a given date range.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarRangeDTO {
    private LocalDate from;
    private LocalDate to;
    private List<TherapistSummaryDTO> therapists;
    private List<CalendarAppointmentDTO> appointments;
    private List<CalendarSeriesInstanceDTO> seriesInstances;
    private List<CalendarBlockDTO> blocks;

    /**
     * Summary info for therapist in calendar context.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TherapistSummaryDTO {
        private Long id;
        private String fullName;
        private String color; // Optional: for UI distinguishing
    }

    /**
     * Single appointment in calendar view.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarAppointmentDTO {
        private Long id;
        private Long therapistId;
        private String therapistName;
        private Long patientId;
        private String patientName;
        private LocalDate date;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String comment;
        private Boolean isHotair;
        private Boolean isUltrasonic;
        private Boolean isElectric;
        private Boolean isFromSeries;
        private Long seriesId;
    }

    /**
     * Computed series instance (not materialized in DB).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarSeriesInstanceDTO {
        private Long seriesId;
        private Long therapistId;
        private String therapistName;
        private Long patientId;
        private String patientName;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private String comment;
        private Boolean isCancelled; // Instance-level cancellation
        private Boolean isException; // Has exception applied
        private Integer instanceIndex; // Which occurrence in series
    }

    /**
     * Block (absence/unavailability) in calendar view.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalendarBlockDTO {
        private Long id;
        private Long therapistId;
        private String therapistName;
        private LocalDate date;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String reason;
        private String blockType; // SPECIAL, RECURRING
        private Boolean isRecurring;
    }
}
