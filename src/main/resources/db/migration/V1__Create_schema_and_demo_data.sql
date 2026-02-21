-- V1__Create_schema_and_demo_data.sql
-- Consolidated schema + comprehensive demo data for Physiokalendar

-- ============================================================
-- 1. SCHEMA
-- ============================================================

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
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
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
    date_of_birth DATE NULL,
    telefon VARCHAR(20),
    street VARCHAR(255) NULL,
    house_number VARCHAR(20) NULL,
    postal_code VARCHAR(10) NULL,
    city VARCHAR(100) NULL,
    active_since DATETIME,
    active_until DATETIME,
    is_bwo BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
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
    role VARCHAR(20) DEFAULT 'THERAPIST' NOT NULL,
    therapist_id BIGINT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    INDEX idx_therapist (therapist_id),
    INDEX idx_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Serientermine (Appointment Series)
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
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_therapist (therapist_id),
    INDEX idx_patient (patient_id),
    INDEX idx_series_status (status)
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
    status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL,
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
    INDEX idx_series (appointment_series_id),
    INDEX idx_status (status)
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

-- Abwesenheiten (Absences)
CREATE TABLE IF NOT EXISTS absence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    therapist_id BIGINT NOT NULL,
    date DATE,
    end_date DATE,
    weekday VARCHAR(20),
    start_time TIME,
    end_time TIME,
    reason VARCHAR(255),
    absence_type VARCHAR(20) DEFAULT 'SPECIAL' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (therapist_id) REFERENCES therapist(id) ON DELETE CASCADE,
    INDEX idx_therapist_date (therapist_id, date),
    INDEX idx_date (date),
    INDEX idx_absence_type (absence_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ensure 'end_date' column exists for older DBs (merged from V2)
ALTER TABLE absence ADD COLUMN IF NOT EXISTS end_date DATE NULL AFTER date;

-- Ensure 'date_of_birth' exists for older DBs (merged from V3)
ALTER TABLE patient ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL AFTER city;

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

-- Audit Event Table
CREATE TABLE IF NOT EXISTS audit_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    timestamp DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    actor_user_id BIGINT,
    actor_username VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(50) NOT NULL,
    before_json JSON,
    after_json JSON,
    metadata_json JSON,
    correlation_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_actor (actor_user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_action (action),
    INDEX idx_correlation (correlation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


/*
-- ============================================================
-- 2. DEMO DATA (COMMENTED OUT)
-- ============================================================
-- Demo/test data are commented out for production migrations. To re-enable demo data remove the surrounding /* ... */ block.


-- Therapeuten (4 aktive, 1 inaktiver)
-- INSERT INTO therapist (first_name, last_name, full_name, email, telefon, active_since, is_active, created_at) VALUES
-- ('Max',     'Mustermann', 'Max Mustermann',   'max.mustermann@physio.de', '030-123456', '2020-01-01', true,  NOW()),
-- ('Anna',    'Schmidt',    'Anna Schmidt',      'anna.schmidt@physio.de',   '030-234567', '2021-06-15', true,  NOW()),
-- ('Peter',   'Mueller',    'Peter Mueller',     'peter.mueller@physio.de',  '030-345678', '2019-03-01', true,  NOW()),
-- ('Laura',   'Weber',      'Laura Weber',       'laura.weber@physio.de',    '030-456789', '2023-01-10', true,  NOW()),
-- ('Thomas',  'Fischer',    'Thomas Fischer',    'thomas.fischer@physio.de',  '030-567890', '2018-06-01', false, NOW());

-- -- Patienten (12 Patienten, 3 BWO)
-- INSERT INTO patient (first_name, last_name, full_name, email, telefon, street, house_number, postal_code, city, active_since, active_until, is_bwo, created_at) VALUES
-- ('Maria',    'Hoffmann',   'Maria Hoffmann',    'maria.hoffmann@test.de',   '0171-1111111', 'Hauptstraße',       '12',  '10115', 'Berlin',   '2025-01-15', '2026-12-31', false, NOW()),
-- ('Klaus',    'Wagner',     'Klaus Wagner',      'klaus.wagner@test.de',     '0172-2222222', 'Berliner Str.',     '45',  '10178', 'Berlin',   '2025-02-01', '2026-12-31', true,  NOW()),
-- ('Sandra',   'Becker',     'Sandra Becker',     'sandra.becker@test.de',    '0173-3333333', 'Friedrichstraße',   '89',  '10117', 'Berlin',   '2025-03-10', '2026-06-30', false, NOW()),
-- ('Frank',    'Richter',    'Frank Richter',     'frank.richter@test.de',    '0174-4444444', 'Kurfürstendamm',    '120', '10711', 'Berlin',   '2025-01-01', '2026-12-31', true,  NOW()),
-- ('Petra',    'Koch',       'Petra Koch',        'petra.koch@test.de',       '0175-5555555', 'Schönhauser Allee', '33',  '10435', 'Berlin',   '2025-04-01', '2026-12-31', false, NOW()),
-- ('Dieter',   'Klein',      'Dieter Klein',      'dieter.klein@test.de',     '0176-6666666', 'Prenzlauer Allee',  '7',   '10405', 'Berlin',   '2025-05-15', '2026-12-31', false, NOW()),
-- ('Monika',   'Wolf',       'Monika Wolf',       'monika.wolf@test.de',      '0177-7777777', 'Torstraße',         '55',  '10119', 'Berlin',   '2025-02-20', '2026-12-31', true,  NOW()),
-- ('Stefan',   'Schwarz',    'Stefan Schwarz',    'stefan.schwarz@test.de',   '0178-8888888', 'Alexanderplatz',    '1',   '10178', 'Berlin',   '2025-06-01', '2026-12-31', false, NOW()),
-- ('Gabriele', 'Schneider',  'Gabriele Schneider','gabriele.schneider@test.de','0179-9999999','Leipziger Str.',    '22',  '10117', 'Berlin',   '2025-01-10', '2026-12-31', false, NOW()),
-- ('Jürgen',   'Braun',      'Jürgen Braun',      'juergen.braun@test.de',    '0160-1234567', 'Oranienburger Str.','15',  '10178', 'Berlin',   '2025-03-01', '2026-12-31', false, NOW()),
-- ('Helga',    'Zimmermann', 'Helga Zimmermann',  'helga.zimmermann@test.de', '0161-2345678', 'Unter den Linden',  '50',  '10117', 'Berlin',   '2025-07-01', '2026-12-31', false, NOW()),
-- ('Markus',   'Hartmann',   'Markus Hartmann',   'markus.hartmann@test.de',  '0162-3456789', 'Kastanienallee',    '8',   '10435', 'Berlin',   '2025-04-15', '2026-12-31', false, NOW());

-- -- Benutzer (Passwort für alle: "password")
-- INSERT INTO user (username, email, password, role, therapist_id, is_active, created_at) VALUES
-- ('max.mustermann', 'max.mustermann@physio.de',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'THERAPIST',  1, true, NOW()),
-- ('anna.schmidt',   'anna.schmidt@physio.de',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'THERAPIST',  2, true, NOW()),
-- ('peter.mueller',  'peter.mueller@physio.de',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'THERAPIST',  3, true, NOW()),
-- ('laura.weber',    'laura.weber@physio.de',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'THERAPIST',  4, true, NOW()),
-- ('admin',          'admin@physio.de',           '$2a$10$iXz3Gp2RN8tK25sYl9ZZ.e25Ey.ZDj5nYXGVG1oI3NyRmwUGDDT3i', 'ADMIN',      NULL, true, NOW()),
-- ('rezeption',      'rezeption@physio.de',       '$2a$10$iXz3Gp2RN8tK25sYl9ZZ.e25Ey.ZDj5nYXGVG1oI3NyRmwUGDDT3i', 'RECEPTION',  NULL, true, NOW());

-- -- Serientermine (12 Serien, verschiedene Therapeuten, Frequenzen, Status)
-- INSERT INTO appointment_series (therapist_id, patient_id, start_time, end_time, start_date, end_date, weekly_frequency, week_day, comment, status, created_at) VALUES
-- (1, 1,  '08:00:00', '08:20:00', '2026-01-05', '2026-06-30', 1, 'MONDAY',    'Rückentherapie KG',            'ACTIVE',    NOW()),
-- (1, 2,  '09:00:00', '09:40:00', '2026-01-07', '2026-12-31', 1, 'WEDNESDAY', 'BWO Behandlung mit HL',        'ACTIVE',    NOW()),
-- (1, 10, '14:00:00', '14:40:00', '2026-01-08', '2026-12-31', 1, 'THURSDAY',  'Nackenst. + Schulter KG',      'ACTIVE',    NOW()),
-- (2, 3,  '10:00:00', '10:20:00', '2026-02-04', '2026-07-31', 1, 'TUESDAY',   'Schulter Mobilisation',        'ACTIVE',    NOW()),
-- (2, 5,  '14:00:00', '14:40:00', '2026-01-09', '2026-06-30', 2, 'THURSDAY',  'Knie-OP Nachsorge US + ET',    'ACTIVE',    NOW()),
-- (2, 8,  '11:00:00', '11:30:00', '2026-01-13', '2026-12-31', 1, 'MONDAY',    'Prävention KG',                'ACTIVE',    NOW()),
-- (3, 4,  '08:30:00', '09:10:00', '2026-01-06', '2026-12-31', 1, 'MONDAY',    'BWO Wirbelsäule + US',         'ACTIVE',    NOW()),
-- (3, 7,  '15:00:00', '15:20:00', '2026-03-05', '2026-09-30', 1, 'WEDNESDAY', 'Handgelenk Therapie',          'PAUSED',    NOW()),
-- (3, 6,  '16:00:00', '16:40:00', '2026-01-15', '2026-12-31', 2, 'MONDAY',    'Hüfte KG + Heißluft',          'ACTIVE',    NOW()),
-- (4, 6,  '11:00:00', '11:40:00', '2026-01-10', '2026-06-30', 2, 'FRIDAY',    'Hüft-TEP Nachsorge ET',        'ACTIVE',    NOW()),
-- (4, 9,  '13:00:00', '13:30:00', '2026-02-01', '2026-12-31', 1, 'TUESDAY',   'Mobilisation Ganzkörper',      'ACTIVE',    NOW()),
-- (1, 11, '10:00:00', '10:30:00', '2026-02-15', '2026-06-30', 1, 'FRIDAY',    'Erstbehandlung + Schulung',    'ACTIVE',    NOW());

-- -- Ausfälle für Serien (10 Ausfälle)
-- INSERT INTO cancellation (appointment_series_id, date, created_at) VALUES
-- (1, '2026-02-16', NOW()),
-- (1, '2026-03-30', NOW()),
-- (2, '2026-03-04', NOW()),
-- (3, '2026-04-07', NOW()),
-- (5, '2026-02-23', NOW()),
-- (5, '2026-04-06', NOW()),
-- (7, '2026-03-13', NOW()),
-- (9, '2026-02-28', NOW()),
-- (10, '2026-03-20', NOW()),
-- (11, '2026-02-11', NOW());

-- -- Einzeltermine – Aktuelle Woche (Mo-Fr) für Therapeut 1 (Max)
-- INSERT INTO appointment (therapist_id, patient_id, date, start_time, end_time, comment, status, is_hotair, is_ultrasonic, is_electric, created_by_series_appointment, created_at) VALUES
-- -- Montag ---
-- (1, 1,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '08:20:00'),
--         'Rücken KG', 'SCHEDULED', false, false, false, true, NOW()),
-- (1, 2,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '09:40:00'),
--         'BWO Behandlung', 'SCHEDULED', true, false, false, false, NOW()),
-- (1, 10, CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '10:20:00'),
--         'Nackenverspannung', 'SCHEDULED', false, false, false, false, NOW()),
-- (1, 6,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '11:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '11:40:00'),
--         'Schulter Reha', 'CONFIRMED', false, true, false, false, NOW()),
-- (1, 3,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '14:30:00'),
--         'Sportphysiotherapie', 'SCHEDULED', false, false, true, false, NOW()),
-- (1, 12, CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '15:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '15:40:00'),
--         'Wirbelsäule + Kraft', 'SCHEDULED', false, false, false, false, NOW()),
-- -- Dienstag ---
-- (1, 8,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '08:20:00'),
--         'KG Fortsetzung', 'SCHEDULED', false, false, false, false, NOW()),
-- (1, 5,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '09:40:00'),
--         'OP Nachsorge Knie', 'CONFIRMED', false, true, true, false, NOW()),
-- (1, 4,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '10:40:00'),
--         'Übungen Zuhause Besprechung', 'SCHEDULED', false, false, false, false, NOW()),
-- (1, 7,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '11:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '11:30:00'),
--         'Gangschule', 'SCHEDULED', false, false, false, false, NOW()),
-- (1, 11, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:20:00'),
--         'Erste Folgebehandlung', 'SCHEDULED', false, false, false, false, NOW()),
-- -- Mittwoch ---
-- (1, 2,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '09:40:00'),
--         'BWO mit Heißluft', 'SCHEDULED', true, false, false, true, NOW()),
-- (1, 9,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '10:30:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '11:10:00'),
--         'Schiefe Körperhaltung KG', 'SCHEDULED', false, false, false, false, NOW()),
-- (1, 6,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '14:40:00'),
--         'Krafttraining mit Geräten', 'CONFIRMED', false, false, false, false, NOW()),
-- -- Donnerstag ---
-- (1, 10, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '14:40:00'),
--         'Nackstf. + Schult. Serie', 'SCHEDULED', false, true, false, true, NOW()),
-- (1, 12, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '15:30:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '16:10:00'),
--         'Rückenübungen + Stretching', 'SCHEDULED', false, false, false, false, NOW()),
-- -- Freitag ---
-- (1, 1,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '08:20:00'),
--         'Rücken KG Kontrolle', 'SCHEDULED', false, false, false, true, NOW()),
-- (1, 3,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '10:40:00'),
--         'Sport nach Verletzung', 'COMPLETED', true, true, false, false, NOW()),
-- (1, 8,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '11:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '11:30:00'),
--         'Abschlussberatung', 'SCHEDULED', false, false, false, false, NOW());

-- -- Einzeltermine – Aktuelle Woche für Therapeut 2 (Anna) – 12 Termine
-- INSERT INTO appointment (therapist_id, patient_id, date, start_time, end_time, comment, status, is_hotair, is_ultrasonic, is_electric, created_by_series_appointment, created_at) VALUES
-- (2, 8,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '09:30:00'),
--         'Prävention KG', 'SCHEDULED', false, false, false, true, NOW()),
-- (2, 1,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '10:40:00'),
--         'Wirbelsäule KG + Kraft', 'SCHEDULED', false, false, false, false, NOW()),
-- (2, 3,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '10:20:00'),
--         'Schulter Mobilisation', 'SCHEDULED', false, false, false, true, NOW()),
-- (2, 5,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:40:00'),
--         'Knie-OP Nachsorge', 'CONFIRMED', false, true, true, true, NOW()),
-- (2, 10, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '08:40:00'),
--         'Rücken Therapie mit HL', 'SCHEDULED', true, false, false, false, NOW()),
-- (2, 2,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '09:30:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '10:10:00'),
--         'Heißluft + Massage', 'SCHEDULED', true, false, false, false, NOW()),
-- (2, 7,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '11:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '11:30:00'),
--         'Lymphdrainage', 'SCHEDULED', false, false, false, false, NOW()),
-- (2, 9,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '13:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '13:30:00'),
--         'Mobilisation Ganzkörper', 'CONFIRMED', false, true, false, true, NOW()),
-- (2, 4,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '09:40:00'),
--         'Sport nach Verletzung', 'SCHEDULED', false, false, false, false, NOW()),
-- (2, 11, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '14:30:00'),
--         'Schmerztherapie erste Sitzung', 'SCHEDULED', false, false, true, false, NOW());

-- -- Einzeltermine – Aktuelle Woche für Therapeut 3 (Peter) – 11 Termine
-- INSERT INTO appointment (therapist_id, patient_id, date, start_time, end_time, comment, status, is_hotair, is_ultrasonic, is_electric, created_by_series_appointment, created_at) VALUES
-- (3, 4,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '08:30:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '09:10:00'),
--         'BWO Wirbelsäule + US', 'SCHEDULED', false, true, false, true, NOW()),
-- (3, 6,  CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '10:40:00'),
--         'Hüfte KG + Heißluft', 'CONFIRMED', true, false, false, true, NOW()),
-- (3, 12, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '11:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '11:40:00'),
--         'Wirbelsäulenschulung', 'SCHEDULED', false, false, false, false, NOW()),
-- (3, 3,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:20:00'),
--         'Schulter Mobilisation', 'SCHEDULED', false, false, false, false, NOW()),
-- (3, 7,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '15:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '15:20:00'),
--         'Handgelenk Therapie', 'SCHEDULED', false, false, false, true, NOW()),
-- (3, 8,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '09:40:00'),
--         'Präventionsprogramm', 'SCHEDULED', false, false, false, false, NOW()),
-- (3, 10, CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '10:30:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '11:10:00'),
--         'Schiefe Haltung - Kontrolle', 'SCHEDULED', false, false, false, false, NOW()),
-- (3, 9,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '08:40:00'),
--         'Ganzkörper Mobilisation', 'SCHEDULED', true, true, true, false, NOW());

-- -- Einzeltermine – Aktuelle Woche für Therapeut 4 (Laura) – 10 Termine
-- INSERT INTO appointment (therapist_id, patient_id, date, start_time, end_time, comment, status, is_hotair, is_ultrasonic, is_electric, created_by_series_appointment, created_at) VALUES
-- (4, 6,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '11:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '11:40:00'),
--         'Hüft-TEP Nachsorge ET', 'CONFIRMED', false, false, false, true, NOW()),
-- (4, 9,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '13:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 4) DAY, '13:30:00'),
--         'Mobilisation', 'SCHEDULED', false, false, false, true, NOW()),
-- (4, 11, CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE())) DAY, '08:20:00'),
--         'Ersttermin Schulter', 'SCHEDULED', false, false, false, false, NOW()),
-- (4, 2,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 2) DAY, '14:20:00'),
--         'Heißluft Massage', 'SCHEDULED', true, false, false, false, NOW()),
-- (4, 3,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '09:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 3) DAY, '09:40:00'),
--         'Sprunggelenk Reha', 'SCHEDULED', false, true, false, false, NOW()),
-- (4, 5,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '08:40:00'),
--         'Knie Therapie Fortgeschrittene', 'SCHEDULED', false, false, false, false, NOW()),
-- (4, 7,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '10:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '10:30:00'),
--         'Nachbehandlung Hand', 'CONFIRMED', false, false, true, false, NOW()),
-- (4, 8,  CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY,
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) - 1) DAY, '14:40:00'),
--         'Funktionstraining', 'SCHEDULED', false, false, false, false, NOW());

-- -- Historische/stornierte Termine
-- INSERT INTO appointment (therapist_id, patient_id, date, start_time, end_time, comment, status, is_hotair, is_ultrasonic, is_electric, created_by_series_appointment, created_at) VALUES
-- (1, 1,  CURDATE() - INTERVAL 7 DAY,
--         TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '08:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '08:20:00'),
--         'Letzte Woche Rücken', 'COMPLETED', false, false, false, true, NOW()),
-- (2, 5,  CURDATE() - INTERVAL 14 DAY,
--         TIMESTAMP(CURDATE() - INTERVAL 14 DAY, '14:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL 14 DAY, '14:40:00'),
--         'Knie Nachsorge – Patient nicht erschienen', 'NO_SHOW', false, true, true, true, NOW()),
-- (3, 7,  CURDATE() - INTERVAL 7 DAY,
--         TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '15:00:00'),
--         TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '15:20:00'),
--         'Abgesagt wegen Krankheit', 'CANCELLED', false, false, false, true, NOW());

-- -- Abwesenheiten (9 Absences - Recurring + Special)
-- INSERT INTO absence (therapist_id, date, weekday, start_time, end_time, reason, absence_type, created_at) VALUES
-- -- Recurring: Peter frei am Freitag Nachmittag
-- (3, NULL, 'FRIDAY', NULL, NULL, 'Freitag Nachmittag frei', 'RECURRING', NOW()),
-- -- Recurring: Anna jeden Mittwoch Nachmittag
-- (2, NULL, 'WEDNESDAY', '1970-01-01 14:00:00', '1970-01-01 17:00:00', 'Organisatorische Aufgaben', 'RECURRING', NOW()),
-- -- Special: Anna Urlaub
-- (2, CURDATE() + INTERVAL 14 DAY, NULL,
--     TIMESTAMP(CURDATE() + INTERVAL 14 DAY, '08:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 14 DAY, '17:00:00'),
--     'Urlaub', 'SPECIAL', NOW()),
-- -- Special: Anna zweiter Urlaubstag
-- (2, CURDATE() + INTERVAL 15 DAY, NULL,
--     TIMESTAMP(CURDATE() + INTERVAL 15 DAY, '08:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 15 DAY, '17:00:00'),
--     'Urlaub Tag 2', 'SPECIAL', NOW()),
-- -- Special: Max Fortbildung
-- (1, CURDATE() + INTERVAL 21 DAY, NULL,
--     TIMESTAMP(CURDATE() + INTERVAL 21 DAY, '08:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 21 DAY, '17:00:00'),
--     'Fortbildung Manuelle Therapie', 'SPECIAL', NOW()),
-- -- Special: Laura krank
-- (4, CURDATE() + INTERVAL 7 DAY, NULL,
--     TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '08:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '12:00:00'),
--     'Arzttermin (halber Tag)', 'SPECIAL', NOW()),
-- -- Special: Peter Inspektionstermin
-- (3, CURDATE() + INTERVAL 10 DAY, NULL,
--     TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '14:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '15:30:00'),
--     'Zahnärztlicher Termin', 'SPECIAL', NOW()),
-- -- Special: Max Konferenz
-- (1, CURDATE() + INTERVAL 35 DAY, NULL,
--     TIMESTAMP(CURDATE() + INTERVAL 35 DAY, '08:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 35 DAY, '17:00:00'),
--     'Fachkonferenz Physiotherapie', 'SPECIAL', NOW());

-- -- Abwesenheits-Ausnahmen (3 Exceptions)
-- INSERT INTO absence_exception (therapist_id, date, weekday, start_time, end_time, created_at) VALUES
-- -- Peter arbeitet doch am 28. (Exception zu Freitag frei)
-- (3, CURDATE() + INTERVAL 28 DAY, 'FRIDAY',
--     TIMESTAMP(CURDATE() + INTERVAL 28 DAY, '14:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 28 DAY, '16:00:00'),
--     NOW()),
-- -- Anna arbeitet doch Mittwoch 5. März (Exception zur Routine)
-- (2, CURDATE() + INTERVAL 19 DAY, 'WEDNESDAY',
--     TIMESTAMP(CURDATE() + INTERVAL 19 DAY, '14:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 19 DAY, '17:00:00'),
--     NOW()),
-- -- Peter frei am Donnerstag 6.3. (einmalige Abwesenheit)
-- (3, CURDATE() + INTERVAL 20 DAY, 'THURSDAY',
--     TIMESTAMP(CURDATE() + INTERVAL 20 DAY, '08:00:00'),
--     TIMESTAMP(CURDATE() + INTERVAL 20 DAY, '17:00:00'),
--     NOW());
-- */
