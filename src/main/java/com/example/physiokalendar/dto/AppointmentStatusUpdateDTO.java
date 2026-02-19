package com.example.physiokalendar.dto;

import com.example.physiokalendar.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating appointment status with optional reason.
 * Used by frontend to update status via PATCH endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusUpdateDTO {
    private AppointmentStatus status;
    private String reason; // Optional reason for status change (e.g., for NO_SHOW, CANCELLED)
}