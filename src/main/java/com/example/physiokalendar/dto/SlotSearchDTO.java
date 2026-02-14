package com.example.physiokalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO for slot search request and results.
 */
public class SlotSearchDTO {

    /**
     * Request for slot search.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private LocalDate rangeFrom;
        private LocalDate rangeTo;
        private Integer durationMinutes;
        private Long therapistId; // null = any therapist
        private List<DayPart> dayParts; // null/empty = any time
        private Long excludePatientId; // Optional: exclude patient's existing appointments
    }

    /**
     * Day part enum for filtering.
     */
    public enum DayPart {
        MORNING,    // 06:00 - 12:00
        AFTERNOON,  // 12:00 - 17:00
        EVENING     // 17:00 - 21:00
    }

    /**
     * Response containing available slots.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private List<SlotGroupDTO> slotsByDay;
        private int totalSlotsFound;
    }

    /**
     * Group of slots for a specific day.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotGroupDTO {
        private LocalDate date;
        private List<SlotDTO> slots;
    }

    /**
     * Single available slot.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotDTO {
        private Long therapistId;
        private String therapistName;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private DayPart dayPart;
    }
}
