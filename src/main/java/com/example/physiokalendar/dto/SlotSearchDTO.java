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
        MORNING,       // 07:00 - 12:00 (Morgens)
        LATE_MORNING,  // 12:00 - 15:00 (Vormittags)
        AFTERNOON,     // 15:00 - 18:00 (Nachmittags)
        EVENING        // 18:00 - 20:00 (Abends)
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
