-- Link patient_id to users.id to enforce referential integrity.
-- Added as a separate migration so existing databases are upgraded safely.

CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_patient_id ON medical_records(patient_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_appointments_patient_id_users'
    ) THEN
        ALTER TABLE appointments
            ADD CONSTRAINT fk_appointments_patient_id_users
            FOREIGN KEY (patient_id) REFERENCES users(id)
            ON UPDATE CASCADE ON DELETE CASCADE
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_medical_records_patient_id_users'
    ) THEN
        ALTER TABLE medical_records
            ADD CONSTRAINT fk_medical_records_patient_id_users
            FOREIGN KEY (patient_id) REFERENCES users(id)
            ON UPDATE CASCADE ON DELETE CASCADE
            NOT VALID;
    END IF;
END $$;

-- Attempt full validation; keep migration non-breaking if legacy orphan rows exist.
DO $$
BEGIN
    BEGIN
        ALTER TABLE appointments VALIDATE CONSTRAINT fk_appointments_patient_id_users;
    EXCEPTION
        WHEN foreign_key_violation THEN
            RAISE NOTICE 'appointments has orphan patient_id rows; FK remains NOT VALID for now';
    END;

    BEGIN
        ALTER TABLE medical_records VALIDATE CONSTRAINT fk_medical_records_patient_id_users;
    EXCEPTION
        WHEN foreign_key_violation THEN
            RAISE NOTICE 'medical_records has orphan patient_id rows; FK remains NOT VALID for now';
    END;
END $$;
