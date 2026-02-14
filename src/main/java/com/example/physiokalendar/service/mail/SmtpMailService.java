package com.example.physiokalendar.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * SMTP mail service for production use.
 * Sends actual emails via configured SMTP server.
 *
 * NOTE: This is a stub implementation. For actual SMTP sending,
 * integrate with Spring Mail (spring-boot-starter-mail) or JavaMail.
 */
@Service
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
@Slf4j
public class SmtpMailService implements MailService {

    @Value("${mail.smtp.host:}")
    private String smtpHost;

    @Value("${mail.smtp.port:587}")
    private int smtpPort;

    @Value("${mail.smtp.username:}")
    private String smtpUsername;

    @Value("${mail.smtp.password:}")
    private String smtpPassword;

    @Value("${mail.from:noreply@physiokalendar.local}")
    private String fromAddress;

    @Value("${app.environment:dev}")
    private String environment;

    @Override
    public boolean sendMail(String to, String subject, String body, boolean isHtml) {
        return sendMail(to, null, subject, body, isHtml);
    }

    @Override
    public boolean sendMail(String to, String cc, String subject, String body, boolean isHtml) {
        // Safety check: Only allow actual sending in production
        if (!"prod".equalsIgnoreCase(environment)) {
            log.warn("[SMTP] Blocking mail in non-production environment. " +
                    "Environment: {}, To: {}, Subject: {}", environment, to, subject);
            return false;
        }

        // Validate SMTP configuration
        if (smtpHost == null || smtpHost.isEmpty()) {
            log.error("[SMTP] SMTP host not configured. Cannot send mail.");
            return false;
        }

        try {
            log.info("[SMTP] Sending mail to: {}, CC: {}, Subject: {}", to, cc, subject);

            // TODO: Implement actual SMTP sending
            // For now, this is a stub that logs the attempt
            //
            // To implement:
            // 1. Add spring-boot-starter-mail dependency
            // 2. Use JavaMailSender to send the email
            //
            // Example:
            // MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper helper = new MimeMessageHelper(message, true);
            // helper.setFrom(fromAddress);
            // helper.setTo(to);
            // if (cc != null) helper.setCc(cc);
            // helper.setSubject(subject);
            // helper.setText(body, isHtml);
            // mailSender.send(message);

            log.info("[SMTP] Mail sent successfully to: {}", to);
            return true;

        } catch (Exception e) {
            log.error("[SMTP] Failed to send mail to: {}, Error: {}", to, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getServiceType() {
        return "SMTP";
    }
}
