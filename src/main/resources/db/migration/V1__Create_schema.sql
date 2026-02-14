-- Therapeuten-Tabelle
CREATE TABLE IF NOT EXISTS therapist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    telefon VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    active_since DATETIME,
    active_until DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Patienten-Tabelle
CREATE TABLE IF NOT EXISTS patient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    telefon VARCHAR(20),
    active_since DATETIME,
    active_until DATETIME,
    is_bwo BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    INDEX idx_bwo (is_bwo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Benutzer-Tabelle
CREATE TABLE IF NOT EXISTS user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    therapist_id BIGINT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    INDEX idx_therapist (therapist_id),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Serientermine (Appointment Series) - muss vor Appointments erstellt werden
CREATE TABLE IF NOT EXISTS appointment_series (
    id BIGINT NOT NULL AUTO_INCREMENT,
    therapist_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    start_time TIME,
    end_time TIME,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    weekly_frequency INTEGER DEFAULT 1,
    week_day VARCHAR(20),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_therapist (therapist_id),
    INDEX idx_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Einzeltermine (Appointments)
CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    therapist_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    comment TEXT,
    is_hotair BOOLEAN DEFAULT false,
    is_ultrasonic BOOLEAN DEFAULT false,
    is_electric BOOLEAN DEFAULT false,
    created_by_series_appointment BOOLEAN DEFAULT false,
    appointment_series_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_series_id) REFERENCES appointment_series(id) ON DELETE SET NULL,
    INDEX idx_date (date),
    INDEX idx_therapist (therapist_id),
    INDEX idx_patient (patient_id),
    INDEX idx_series (appointment_series_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ausfallstermine (Cancellations) - für Serientermine
CREATE TABLE IF NOT EXISTS cancellation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_series_id BIGINT NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (appointment_series_id) REFERENCES appointment_series(id) ON DELETE CASCADE,
    UNIQUE KEY uk_series_date (appointment_series_id, date),
    INDEX idx_series (appointment_series_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Abwesenheiten (Absences) - wiederkehrende Abwesenheiten
CREATE TABLE IF NOT EXISTS absence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    therapist_id BIGINT NOT NULL,
    date DATE,
    weekday VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    INDEX idx_therapist_date (therapist_id, date),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ausnahmen zu Abwesenheiten (Absence Exceptions)
CREATE TABLE IF NOT EXISTS absence_exception (
    id BIGINT NOT NULL AUTO_INCREMENT,
    therapist_id BIGINT NOT NULL,
    date DATE NOT NULL,
    weekday VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    UNIQUE KEY uk_therapist_date (therapist_id, date),
    INDEX idx_therapist (therapist_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;