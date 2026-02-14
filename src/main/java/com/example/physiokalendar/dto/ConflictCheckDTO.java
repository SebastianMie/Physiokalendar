package com.example.physiokalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for conflict check results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictCheckDTO {
    private boolean hasConflict;
    private List<ConflictDTO> conflicts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConflictDTO {
        private String type; // APPOINTMENT, SERIES_INSTANCE, ABSENCE
        private Long id;
        private Long therapistId;
        private String therapistName;
        private Long patientId;
        private String patientName;
        private LocalDate date;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String description;
    }
}
