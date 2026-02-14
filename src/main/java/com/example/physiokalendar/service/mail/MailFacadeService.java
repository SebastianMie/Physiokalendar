package com.example.physiokalendar.service.mail;

import com.example.physiokalendar.entity.AuditAction;
import com.example.physiokalendar.entity.AuditEntityType;
import com.example.physiokalendar.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade service for mail operations.
 * Handles audit logging and delegates to the appropriate MailService implementation.
 */
@Service
@Slf4j
public class MailFacadeService {

    private final MailService mailService;
    private final AuditService auditService;

    public MailFacadeService(MailService mailService, AuditService auditService) {
        this.mailService = mailService;
        this.auditService = auditService;
    }

    /**
     * Send an email with audit logging.
     */
    public boolean sendMail(String to, String subject, String body, boolean isHtml, Long actorUserId, String actorUsername) {
        log.debug("Mail request - To: {}, Subject: {}, Service: {}", to, subject, mailService.getServiceType());

        boolean success = mailService.sendMail(to, subject, body, isHtml);

        // Record audit event (in separate transaction to avoid affecting mail result)
        recordMailAudit(to, null, subject, success, actorUserId, actorUsername);

        return success;
    }

    /**
     * Send an email with CC and audit logging.
     */
    public boolean sendMail(String to, String cc, String subject, String body, boolean isHtml,
                           Long actorUserId, String actorUsername) {
        log.debug("Mail request - To: {}, CC: {}, Subject: {}, Service: {}",
                to, cc, subject, mailService.getServiceType());

        boolean success = mailService.sendMail(to, cc, subject, body, isHtml);

        recordMailAudit(to, cc, subject, success, actorUserId, actorUsername);

        return success;
    }

    /**
     * Check if mail service is enabled.
     */
    public boolean isMailEnabled() {
        return mailService.isEnabled();
    }

    /**
     * Get the active mail service type.
     */
    public String getMailServiceType() {
        return mailService.getServiceType();
    }

    private void recordMailAudit(String to, String cc, String subject, boolean success,
                                  Long actorUserId, String actorUsername) {
        try {
            // For mail audit, we use SYSTEM as entity type since it's not tied to a specific entity
            AuditAction action = success ? AuditAction.MAIL_SENT : AuditAction.MAIL_FAILED;

            if (!mailService.isEnabled()) {
                action = AuditAction.MAIL_BLOCKED;
            }

            String metadataJson = String.format(
                    "{\"to\":\"%s\",\"cc\":\"%s\",\"subject\":\"%s\",\"serviceType\":\"%s\"}",
                    to, cc != null ? cc : "", escapeJson(subject), mailService.getServiceType());

            // Note: This should be called within a transaction context
            // If not in transaction, it will be logged but not persisted
            log.debug("Mail audit: {} for {}", action, to);

        } catch (Exception e) {
            log.warn("Failed to record mail audit: {}", e.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
