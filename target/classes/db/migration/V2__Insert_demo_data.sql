-- V2__Insert_demo_data.sql
-- Demo data for development

-- Insert Demo Therapeuten
INSERT INTO therapist (first_name, last_name, full_name, email, telefon, active_since, is_active, created_at) VALUES
('Max', 'Mustermann', 'Max Mustermann', 'max.mustermann@physio.de', '030-123456', '2020-01-01', true, NOW()),
('Anna', 'Schmidt', 'Anna Schmidt', 'anna.schmidt@physio.de', '030-234567', '2021-06-15', true, NOW()),
('Peter', 'Mueller', 'Peter Mueller', 'peter.mueller@physio.de', '030-345678', '2019-03-01', true, NOW());

-- Insert Demo Patienten
INSERT INTO patient (first_name, last_name, full_name, email, telefon, active_since, active_until, is_bwo, created_at) VALUES
('John', 'Doe', 'John Doe', 'john.doe@test.de', '0123-456789', '2025-01-01', '2026-12-31', false, NOW()),
('Jane', 'Smith', 'Jane Smith', 'jane.smith@test.de', '0234-567890', '2025-06-15', '2026-12-31', true, NOW()),
('Bob', 'Johnson', 'Bob Johnson', 'bob.johnson@test.de', '0345-678901', '2025-03-01', '2026-12-31', false, NOW()),
('Alice', 'Brown', 'Alice Brown', 'alice.brown@test.de', '0456-789012', '2025-11-01', '2026-12-31', true, NOW());

-- Insert Demo Benutzer
-- Password for all: "password" (bcrypt hashed)
INSERT INTO user (username, email, password, therapist_id, is_active, created_at) VALUES
('max.mustermann', 'max.mustermann@physio.de', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1, true, NOW()),
('anna.schmidt', 'anna.schmidt@physio.de', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2, true, NOW()),
('peter.mueller', 'peter.mueller@physio.de', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 3, true, NOW());

-- Insert Demo AppointmentSeries
INSERT INTO appointment_series (therapist_id, patient_id, start_time, end_time, start_date, end_date, weekly_frequency, week_day, comment, created_at) VALUES
(1, 1, '09:00:00', '10:00:00', '2026-02-01', '2026-12-31', 1, 'MONDAY', 'Wöchentliche Therapie', NOW()),
(1, 2, '10:30:00', '11:30:00', '2026-02-01', '2026-12-31', 2, 'WEDNESDAY', 'Bi-wöchentliche Therapie', NOW()),
(2, 3, '14:00:00', '15:00:00', '2026-02-01', '2026-12-31', 1, 'TUESDAY', 'Wöchentliche Behandlung', NOW());

-- Insert Demo Appointments
INSERT INTO appointment (therapist_id, patient_id, date, start_time, end_time, comment, is_hotair, is_ultrasonic, is_electric, created_by_series_appointment, created_at) VALUES
(1, 1, '2026-02-13', '2026-02-13 09:00:00', '2026-02-13 10:00:00', 'Regelmäßige Behandlung', false, true, false, true, NOW()),
(1, 2, '2026-02-17', '2026-02-17 10:30:00', '2026-02-17 11:30:00', 'Folgebehandlung', true, false, true, true, NOW()),
(2, 3, '2026-02-16', '2026-02-16 14:00:00', '2026-02-16 15:00:00', 'Behandlung mit Ultraschall', false, true, false, true, NOW());

-- Insert Demo Cancellations
INSERT INTO cancellation (appointment_series_id, date, created_at) VALUES
(1, '2026-03-02', NOW()),
(2, '2026-03-17', NOW());

-- Insert Demo Absences
INSERT INTO absence (therapist_id, date, weekday, start_time, end_time, reason, created_at) VALUES
(1, '2026-03-15', 'SUNDAY', '2026-03-15 08:00:00', '2026-03-15 17:00:00', 'Urlaub', NOW()),
(2, '2026-04-10', 'FRIDAY', '2026-04-10 08:00:00', '2026-04-10 17:00:00', 'Fortbildung', NOW()),
(3, '2026-02-20', 'FRIDAY', '2026-02-20 08:00:00', '2026-02-20 12:00:00', 'Krankenheit', NOW());

-- Insert Demo AbsenceExceptions
INSERT INTO absence_exception (therapist_id, date, weekday, start_time, end_time, created_at) VALUES
(1, '2026-03-16', 'MONDAY', '2026-03-16 09:00:00', '2026-03-16 10:00:00', NOW()),
(2, '2026-04-11', 'SATURDAY', '2026-04-11 10:00:00', '2026-04-11 11:00:00', NOW());
