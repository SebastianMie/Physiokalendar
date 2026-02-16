-- Add end_date column for date range absences
-- Make idempotent to avoid errors when column already exists (safe on MySQL 8+)
ALTER TABLE absence ADD COLUMN IF NOT EXISTS end_date DATE NULL AFTER date;
