package com.example.physiokalendar.dto;

import com.example.physiokalendar.entity.AuditAction;
import com.example.physiokalendar.entity.AuditEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for audit event responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventDTO {
    private Long id;
    private LocalDateTime timestamp;
    private Long actorUserId;
    private String actorUsername;
    private AuditEntityType entityType;
    private Long entityId;
    private AuditAction action;
    private String beforeJson;
    private String afterJson;
    private String metadataJson;
    private String correlationId;
}
