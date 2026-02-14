package com.example.physiokalendar.dto;

import com.example.physiokalendar.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of saving an appointment, including conflict information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSaveResult {

    private Appointment appointment;
    private ConflictCheckDTO conflictCheck;
    private boolean saved;

    public boolean hasConflicts() {
        return conflictCheck != null && conflictCheck.isHasConflict();
    }
}
