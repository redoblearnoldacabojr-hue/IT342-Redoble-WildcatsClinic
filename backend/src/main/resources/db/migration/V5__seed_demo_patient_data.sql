INSERT INTO medical_records (patient_id, title, summary, date, created_at)
SELECT u.id, 'Annual Physical', 'Vitals are stable. Continue routine exercise and hydration.', '2026-02-14', CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'student@uniclinic.edu'
ON CONFLICT DO NOTHING;

INSERT INTO medical_records (patient_id, title, summary, date, created_at)
SELECT u.id, 'Vaccination Review', 'Flu booster completed. Next review due in one year.', '2026-01-15', CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'student@uniclinic.edu'
ON CONFLICT DO NOTHING;

INSERT INTO appointments (patient_id, patient_name, date, time, reason, created_at)
SELECT u.id, CONCAT(u.first_name, ' ', u.last_name), DATE '2026-03-03', TIME '10:00', 'General Checkup', CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'student@uniclinic.edu'
ON CONFLICT DO NOTHING;

INSERT INTO appointments (patient_id, patient_name, date, time, reason, created_at)
SELECT u.id, CONCAT(u.first_name, ' ', u.last_name), DATE '2026-03-10', TIME '14:30', 'Eye Exam', CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'student@uniclinic.edu'
ON CONFLICT DO NOTHING;
