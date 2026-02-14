-- V4__Add_address_fields.sql
-- Add address fields to patient and updated_at timestamps

-- Add address fields to patient
ALTER TABLE patient
ADD COLUMN street VARCHAR(255) NULL AFTER telefon,
ADD COLUMN house_number VARCHAR(20) NULL AFTER street,
ADD COLUMN postal_code VARCHAR(10) NULL AFTER house_number,
ADD COLUMN city VARCHAR(100) NULL AFTER postal_code,
ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- Add updated_at to therapist
ALTER TABLE therapist
ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- Add updated_at to user
ALTER TABLE user
ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
