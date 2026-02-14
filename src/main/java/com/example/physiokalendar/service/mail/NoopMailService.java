package com.example.physiokalendar.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-operation mail service for development.
 * Logs mail operations but doesn't actually send emails.
 */
@Service
@ConditionalOnProperty(name = "mail.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoopMailService implements MailService {

    @Override
    public boolean sendMail(String to, String subject, String body, boolean isHtml) {
        log.info("[NOOP MAIL] Would send to: {}, Subject: {}", to, subject);
        log.debug("[NOOP MAIL] Body: {}", body);
        return true;
    }

    @Override
    public boolean sendMail(String to, String cc, String subject, String body, boolean isHtml) {
        log.info("[NOOP MAIL] Would send to: {}, CC: {}, Subject: {}", to, cc, subject);
        log.debug("[NOOP MAIL] Body: {}", body);
        return true;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getServiceType() {
        return "NOOP";
    }
}
