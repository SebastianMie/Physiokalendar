-- V3__Add_roles.sql
-- Rollen-System hinzufügen

-- Rolle zur User-Tabelle hinzufügen
ALTER TABLE user ADD COLUMN role VARCHAR(20) DEFAULT 'THERAPIST' NOT NULL;

-- Index für Rollen
CREATE INDEX idx_user_role ON user(role);

-- Bestehende User als Therapeuten markieren (bereits default)
UPDATE user SET role = 'THERAPIST' WHERE role IS NULL OR role = '';

-- Admin-Benutzer erstellen
INSERT INTO user (username, email, password, role, is_active, created_at) VALUES
('admin', 'admin@physio.de', '$2a$10$iXz3Gp2RN8tK25sYl9ZZ.e25Ey.ZDj5nYXGVG1oI3NyRmwUGDDT3i', 'ADMIN', true, NOW()),
('rezeption', 'rezeption@physio.de', '$2a$10$iXz3Gp2RN8tK25sYl9ZZ.e25Ey.ZDj5nYXGVG1oI3NyRmwUGDDT3i', 'RECEPTION', true, NOW());
