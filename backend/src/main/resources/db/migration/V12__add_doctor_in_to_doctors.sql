ALTER TABLE doctors ADD COLUMN IF NOT EXISTS doctor_in BOOLEAN;

UPDATE doctors
SET doctor_in = COALESCE(doctor_in, available, TRUE)
WHERE doctor_in IS NULL;

ALTER TABLE doctors
    ALTER COLUMN doctor_in SET DEFAULT TRUE,
    ALTER COLUMN doctor_in SET NOT NULL;

COMMENT ON COLUMN doctors.doctor_in IS 'Whether the doctor is currently in/on duty and available for assignment.';
