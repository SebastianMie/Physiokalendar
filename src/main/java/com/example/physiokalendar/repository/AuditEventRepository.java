package com.example.physiokalendar.repository;

import com.example.physiokalendar.entity.AuditAction;
import com.example.physiokalendar.entity.AuditEntityType;
import com.example.physiokalendar.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /**
     * Find audit events within a date range.
     */
    Page<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Find audit events by entity type.
     */
    Page<AuditEvent> findByEntityTypeOrderByTimestampDesc(
            AuditEntityType entityType, Pageable pageable);

    /**
     * Find audit events by action.
     */
    Page<AuditEvent> findByActionOrderByTimestampDesc(
            AuditAction action, Pageable pageable);

    /**
     * Find audit events by actor (user).
     */
    Page<AuditEvent> findByActorUserIdOrderByTimestampDesc(
            Long actorUserId, Pageable pageable);

    /**
     * Find audit events for a specific entity.
     */
    List<AuditEvent> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            AuditEntityType entityType, Long entityId);

    /**
     * Complex query with multiple optional filters.
     */
    @Query("SELECT e FROM AuditEvent e WHERE " +
           "(:from IS NULL OR e.timestamp >= :from) AND " +
           "(:to IS NULL OR e.timestamp <= :to) AND " +
           "(:entityType IS NULL OR e.entityType = :entityType) AND " +
           "(:action IS NULL OR e.action = :action) AND " +
           "(:actorUserId IS NULL OR e.actorUserId = :actorUserId) " +
           "ORDER BY e.timestamp DESC")
    Page<AuditEvent> findWithFilters(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("entityType") AuditEntityType entityType,
            @Param("action") AuditAction action,
            @Param("actorUserId") Long actorUserId,
            Pageable pageable);
}
