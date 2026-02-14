package com.example.physiokalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for appointment draft (used for conflict checking before creation/update).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDraftDTO {
    private Long id; // null for new appointments
    private Long therapistId;
    private Long patientId;
    private LocalDate date;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String comment;
    private Boolean isHotair;
    private Boolean isUltrasonic;
    private Boolean isElectric;
}
