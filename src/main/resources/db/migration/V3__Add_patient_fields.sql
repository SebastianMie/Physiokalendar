-- V3__Add_patient_fields.sql
-- Add missing patient fields: dateOfBirth, insuranceType, notes, isActive

-- Add date_of_birth column
ALTER TABLE patient ADD COLUMN date_of_birth DATE NULL AFTER city;

-- Add insurance_type column
ALTER TABLE patient ADD COLUMN insurance_type VARCHAR(100) NULL AFTER date_of_birth;

-- Add notes column (TEXT for longer notes)
ALTER TABLE patient ADD COLUMN notes TEXT NULL AFTER insurance_type;

-- Add is_active column with default true
ALTER TABLE patient ADD COLUMN is_active BOOLEAN DEFAULT true AFTER notes;

-- Add index for active patients
ALTER TABLE patient ADD INDEX idx_active (is_active);
