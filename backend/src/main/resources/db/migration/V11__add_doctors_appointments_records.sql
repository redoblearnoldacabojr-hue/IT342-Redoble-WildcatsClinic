CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS doctor_id BIGINT;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS completion_remarks TEXT;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS completion_results TEXT;

ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS appointment_id BIGINT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS doctor_id BIGINT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS doctor_name VARCHAR(255);
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS remarks TEXT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS results TEXT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_appointment_id ON medical_records(appointment_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_doctor_id ON medical_records(doctor_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_appointments_doctor_id_doctors'
    ) THEN
        ALTER TABLE appointments
            ADD CONSTRAINT fk_appointments_doctor_id_doctors
            FOREIGN KEY (doctor_id) REFERENCES doctors(id)
            ON UPDATE CASCADE ON DELETE SET NULL
            NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_medical_records_appointment_id_appointments'
    ) THEN
        ALTER TABLE medical_records
            ADD CONSTRAINT fk_medical_records_appointment_id_appointments
            FOREIGN KEY (appointment_id) REFERENCES appointments(id)
            ON UPDATE CASCADE ON DELETE SET NULL
            NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_medical_records_doctor_id_doctors'
    ) THEN
        ALTER TABLE medical_records
            ADD CONSTRAINT fk_medical_records_doctor_id_doctors
            FOREIGN KEY (doctor_id) REFERENCES doctors(id)
            ON UPDATE CASCADE ON DELETE SET NULL
            NOT VALID;
    END IF;
END $$;
