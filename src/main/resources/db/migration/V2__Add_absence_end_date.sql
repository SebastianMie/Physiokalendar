-- Add end_date column for date range absences
ALTER TABLE absence ADD COLUMN end_date DATE NULL AFTER date;
