package com.example.physiokalendar.controller;

import com.example.physiokalendar.dto.SlotSearchDTO;
import com.example.physiokalendar.service.SlotSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for slot search / Terminfinder.
 */
@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173"})
public class SlotController {

    private final SlotSearchService slotSearchService;

    public SlotController(SlotSearchService slotSearchService) {
        this.slotSearchService = slotSearchService;
    }

    /**
     * Search for available appointment slots.
     * POST /api/slots/search
     *
     * Body: SlotSearchDTO.Request
     * Returns: SlotSearchDTO.Response with available slots grouped by day
     */
    @PostMapping("/search")
    public ResponseEntity<SlotSearchDTO.Response> searchSlots(@RequestBody SlotSearchDTO.Request request) {
        if (request.getRangeFrom() == null || request.getRangeTo() == null ||
            request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // Limit search range to 30 days for performance
        if (request.getRangeFrom().plusDays(30).isBefore(request.getRangeTo())) {
            return ResponseEntity.badRequest().build();
        }

        SlotSearchDTO.Response result = slotSearchService.searchSlots(request);
        return ResponseEntity.ok(result);
    }
}
