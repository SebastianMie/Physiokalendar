package com.example.physiokalendar.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Test mail service that redirects all emails to a test inbox.
 * Used in TEST environment to prevent accidental emails to real addresses.
 */
@Service
@ConditionalOnProperty(name = "mail.redirect.all", havingValue = "true")
@Primary
@Slf4j
public class TestRedirectMailService implements MailService {

    @Value("${mail.redirect.to:test-inbox@localhost}")
    private String redirectTo;

    @Value("${mail.redirect.subject-prefix:[TEST] }")
    private String subjectPrefix;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    private final MailService delegateMailService;

    public TestRedirectMailService() {
        // Use a simple logging delegate since we can't have circular dependency
        this.delegateMailService = null;
    }

    @Override
    public boolean sendMail(String originalTo, String subject, String body, boolean isHtml) {
        if (!mailEnabled) {
            log.info("[TEST REDIRECT] Mail disabled. Would redirect {} -> {} with subject: {}",
                    originalTo, redirectTo, subjectPrefix + subject);
            return true;
        }

        // Validate redirect address is configured
        if (redirectTo == null || redirectTo.isEmpty() || redirectTo.contains("localhost")) {
            log.warn("[TEST REDIRECT] Blocking mail - no valid redirect address configured. " +
                    "Original to: {}, Subject: {}", originalTo, subject);
            return false;
        }

        String newSubject = subjectPrefix + subject;
        String newBody = buildRedirectedBody(originalTo, null, body, isHtml);

        log.info("[TEST REDIRECT] Redirecting mail from {} to {} with subject: {}",
                originalTo, redirectTo, newSubject);

        // In test mode, just log - actual sending would be done by SMTP service
        log.debug("[TEST REDIRECT] Body: {}", newBody);
        return true;
    }

    @Override
    public boolean sendMail(String originalTo, String originalCc, String subject, String body, boolean isHtml) {
        if (!mailEnabled) {
            log.info("[TEST REDIRECT] Mail disabled. Would redirect {}/{} -> {} with subject: {}",
                    originalTo, originalCc, redirectTo, subjectPrefix + subject);
            return true;
        }

        if (redirectTo == null || redirectTo.isEmpty() || redirectTo.contains("localhost")) {
            log.warn("[TEST REDIRECT] Blocking mail - no valid redirect address configured. " +
                    "Original to: {}, CC: {}, Subject: {}", originalTo, originalCc, subject);
            return false;
        }

        String newSubject = subjectPrefix + subject;
        String newBody = buildRedirectedBody(originalTo, originalCc, body, isHtml);

        log.info("[TEST REDIRECT] Redirecting mail from {}/{} to {} with subject: {}",
                originalTo, originalCc, redirectTo, newSubject);
        log.debug("[TEST REDIRECT] Body: {}", newBody);
        return true;
    }

    @Override
    public boolean isEnabled() {
        return mailEnabled;
    }

    @Override
    public String getServiceType() {
        return "TEST_REDIRECT";
    }

    private String buildRedirectedBody(String originalTo, String originalCc, String body, boolean isHtml) {
        StringBuilder prefix = new StringBuilder();

        if (isHtml) {
            prefix.append("<div style=\"background-color: #fff3cd; padding: 10px; margin-bottom: 20px; border: 1px solid #ffc107;\">");
            prefix.append("<strong>⚠️ TEST-UMLEITUNG</strong><br>");
            prefix.append("Original-Empfänger: ").append(originalTo).append("<br>");
            if (originalCc != null && !originalCc.isEmpty()) {
                prefix.append("Original-CC: ").append(originalCc).append("<br>");
            }
            prefix.append("</div>");
            prefix.append(body);
        } else {
            prefix.append("=== TEST-UMLEITUNG ===\n");
            prefix.append("Original-Empfänger: ").append(originalTo).append("\n");
            if (originalCc != null && !originalCc.isEmpty()) {
                prefix.append("Original-CC: ").append(originalCc).append("\n");
            }
            prefix.append("========================\n\n");
            prefix.append(body);
        }

        return prefix.toString();
    }
}
