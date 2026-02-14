package com.example.physiokalendar.controller;

import com.example.physiokalendar.dto.AppointmentDraftDTO;
import com.example.physiokalendar.dto.ConflictCheckDTO;
import com.example.physiokalendar.service.ConflictService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for conflict checking.
 */
@RestController
@RequestMapping("/api/conflicts")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173"})
public class ConflictController {

    private final ConflictService conflictService;

    public ConflictController(ConflictService conflictService) {
        this.conflictService = conflictService;
    }

    /**
     * Check for conflicts with a proposed appointment.
     * POST /api/conflicts/check
     *
     * Body: AppointmentDraftDTO
     * Returns: ConflictCheckDTO with hasConflict flag and conflict details
     */
    @PostMapping("/check")
    public ResponseEntity<ConflictCheckDTO> checkConflicts(@RequestBody AppointmentDraftDTO draft) {
        if (draft.getTherapistId() == null || draft.getDate() == null ||
            draft.getStartTime() == null || draft.getEndTime() == null) {
            return ResponseEntity.badRequest().build();
        }

        ConflictCheckDTO result = conflictService.checkConflicts(draft);
        return ResponseEntity.ok(result);
    }
}
