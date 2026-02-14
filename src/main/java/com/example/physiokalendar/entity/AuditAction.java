package com.example.physiokalendar.entity;

/**
 * Enum for audit event actions.
 */
public enum AuditAction {
    // CRUD Operations
    CREATE,
    UPDATE,
    DELETE,

    // Scheduling specific
    CANCEL,
    RESCHEDULE,

    // Authentication
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_RESET_REQUEST,
    PASSWORD_CHANGE,

    // Export/Import
    EXPORT,
    IMPORT,

    // Mail
    MAIL_SEND_REQUESTED,
    MAIL_SENT,
    MAIL_BLOCKED,
    MAIL_FAILED
}
