package com.example.physiokalendar.service.mail;

/**
 * Interface for mail service implementations.
 * Provides abstraction for different mail sending strategies.
 */
public interface MailService {

    /**
     * Send an email.
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body (can be HTML)
     * @param isHtml Whether the body is HTML
     * @return true if mail was sent successfully
     */
    boolean sendMail(String to, String subject, String body, boolean isHtml);

    /**
     * Send an email with CC.
     */
    boolean sendMail(String to, String cc, String subject, String body, boolean isHtml);

    /**
     * Check if mail service is enabled.
     */
    boolean isEnabled();

    /**
     * Get the service type name for logging.
     */
    String getServiceType();
}
