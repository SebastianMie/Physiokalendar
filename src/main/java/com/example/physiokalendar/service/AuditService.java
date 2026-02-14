package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for recording and querying audit events.
 * All relevant CAD (Create/Alter/Delete) operations should be logged through this service.
 */
@Service
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${audit.log-before-after:true}")
    private boolean logBeforeAfter;

    @Value("${audit.log-metadata:true}")
    private boolean logMetadata;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Record an audit event. This should be called within the same transaction
     * as the domain operation to ensure consistency.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEventBuilder builder) {
        AuditEvent event = builder.build();

        // Add metadata if enabled
        if (logMetadata) {
            event.setMetadataJson(buildMetadataJson());
        }

        // Clear before/after if disabled
        if (!logBeforeAfter) {
            event.setBeforeJson(null);
            event.setAfterJson(null);
        }

        try {
            auditEventRepository.save(event);
            log.debug("Recorded audit event: {} {} on {} #{}",
                    event.getAction(), event.getEntityType(),
                    event.getEntityId(), event.getActorUsername());
        } catch (Exception e) {
            log.error("Failed to record audit event: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    /**
     * Record an audit event with a new transaction (for cases where the main transaction may fail).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(AuditEventBuilder builder) {
        AuditEvent event = builder.build();

        if (logMetadata) {
            event.setMetadataJson(buildMetadataJson());
        }
        if (!logBeforeAfter) {
            event.setBeforeJson(null);
            event.setAfterJson(null);
        }

        auditEventRepository.save(event);
    }

    /**
     * Query audit events with filters.
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findEvents(
            LocalDateTime from,
            LocalDateTime to,
            AuditEntityType entityType,
            AuditAction action,
            Long actorUserId,
            Pageable pageable) {
        return auditEventRepository.findWithFilters(from, to, entityType, action, actorUserId, pageable);
    }

    /**
     * Get audit history for a specific entity.
     */
    @Transactional(readOnly = true)
    public List<AuditEvent> getEntityHistory(AuditEntityType entityType, Long entityId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    /**
     * Build JSON metadata from current request context.
     */
    private String buildMetadataJson() {
        try {
            Map<String, Object> metadata = new HashMap<>();

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                metadata.put("ip", getClientIp(request));
                metadata.put("userAgent", request.getHeader("User-Agent"));
                metadata.put("requestUri", request.getRequestURI());
                metadata.put("method", request.getMethod());
            }

            metadata.put("correlationId", UUID.randomUUID().toString());

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to build metadata JSON: {}", e.getMessage());
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs (X-Forwarded-For can contain comma-separated list), take first
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Convert object to JSON for before/after logging.
     * Sensitive fields are filtered out.
     */
    public String toAuditJson(Object obj) {
        if (obj == null) return null;
        try {
            // Filter sensitive fields
            Map<String, Object> filtered = filterSensitiveFields(obj);
            return objectMapper.writeValueAsString(filtered);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to audit JSON: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterSensitiveFields(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            // Remove sensitive fields
            Set<String> sensitiveFields = Set.of("password", "passwordHash", "token", "secret", "apiKey");
            sensitiveFields.forEach(map::remove);

            return map;
        } catch (JsonProcessingException e) {
            return Map.of("_error", "Failed to serialize");
        }
    }

    /**
     * Builder for creating audit events with fluent API.
     */
    public static class AuditEventBuilder {
        private Long actorUserId;
        private String actorUsername;
        private AuditEntityType entityType;
        private Long entityId;
        private AuditAction action;
        private String beforeJson;
        private String afterJson;
        private String correlationId;

        public AuditEventBuilder actor(Long userId, String username) {
            this.actorUserId = userId;
            this.actorUsername = username;
            return this;
        }

        public AuditEventBuilder entity(AuditEntityType type, Long id) {
            this.entityType = type;
            this.entityId = id;
            return this;
        }

        public AuditEventBuilder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public AuditEventBuilder before(String json) {
            this.beforeJson = json;
            return this;
        }

        public AuditEventBuilder after(String json) {
            this.afterJson = json;
            return this;
        }

        public AuditEventBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public AuditEvent build() {
            return AuditEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .actorUserId(actorUserId)
                    .actorUsername(actorUsername)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .beforeJson(beforeJson)
                    .afterJson(afterJson)
                    .correlationId(correlationId)
                    .build();
        }
    }

    /**
     * Create a new builder for audit events.
     */
    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }
}
