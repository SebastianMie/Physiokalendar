package com.example.physiokalendar.controller;

import com.example.physiokalendar.dto.AuditEventDTO;
import com.example.physiokalendar.entity.AuditAction;
import com.example.physiokalendar.entity.AuditEntityType;
import com.example.physiokalendar.entity.AuditEvent;
import com.example.physiokalendar.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller for audit event queries.
 * Only accessible by ADMIN role.
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:5173"})
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Query audit events with optional filters.
     * GET /api/audit?from=...&to=...&entityType=...&action=...&actorUserId=...&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AuditEventDTO>> getAuditEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AuditEvent> events = auditService.findEvents(from, to, entityType, action, actorUserId, pageable);

        Page<AuditEventDTO> dtoPage = events.map(this::toDTO);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Get audit history for a specific entity.
     * GET /api/audit/entity/{entityType}/{entityId}
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<?> getEntityHistory(
            @PathVariable AuditEntityType entityType,
            @PathVariable Long entityId) {

        var history = auditService.getEntityHistory(entityType, entityId)
                .stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(history);
    }

    /**
     * Get available entity types for filtering.
     */
    @GetMapping("/entity-types")
    public ResponseEntity<AuditEntityType[]> getEntityTypes() {
        return ResponseEntity.ok(AuditEntityType.values());
    }

    /**
     * Get available actions for filtering.
     */
    @GetMapping("/actions")
    public ResponseEntity<AuditAction[]> getActions() {
        return ResponseEntity.ok(AuditAction.values());
    }

    private AuditEventDTO toDTO(AuditEvent event) {
        return AuditEventDTO.builder()
                .id(event.getId())
                .timestamp(event.getTimestamp())
                .actorUserId(event.getActorUserId())
                .actorUsername(event.getActorUsername())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .beforeJson(event.getBeforeJson())
                .afterJson(event.getAfterJson())
                .metadataJson(event.getMetadataJson())
                .correlationId(event.getCorrelationId())
                .build();
    }
}
