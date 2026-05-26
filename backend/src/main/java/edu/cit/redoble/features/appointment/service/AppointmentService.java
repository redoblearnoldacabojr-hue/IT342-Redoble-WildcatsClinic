package edu.cit.redoble.features.appointment.service;

import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import edu.cit.redoble.features.appointment.entity.AppointmentStatus;
import edu.cit.redoble.features.appointment.repository.AppointmentRepository;
import edu.cit.redoble.features.doctor.entity.DoctorEntity;
import edu.cit.redoble.features.doctor.repository.DoctorRepository;
import edu.cit.redoble.features.record.entity.MedicalRecordEntity;
import edu.cit.redoble.features.record.repository.MedicalRecordRepository;
import edu.cit.redoble.features.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Comparator;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              MedicalRecordRepository medicalRecordRepository,
                              NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notificationService = notificationService;
    }

    public List<AppointmentEntity> getForUser(Long userId) {
        return appointmentRepository.findByPatientId(userId).stream()
                .sorted(Comparator.comparing(AppointmentEntity::getDate).thenComparing(AppointmentEntity::getTime))
                .toList();
    }

    public List<AppointmentEntity> getAll() {
        return appointmentRepository.findAll().stream()
                .sorted(Comparator.comparing(AppointmentEntity::getDate).thenComparing(AppointmentEntity::getTime))
                .toList();
    }

    public AppointmentEntity create(AppointmentEntity appt) {
        // check conflict
        LocalDate date = appt.getDate();
        LocalTime time = appt.getTime();
        if (appointmentRepository.findByPatientIdAndDateAndTime(appt.getPatientId(), date, time).isPresent()) {
            throw new IllegalArgumentException("Appointment conflict");
        }

        if (appt.getStatus() == null) {
            appt.setStatus(AppointmentStatus.PROCESSING);
        }

        return appointmentRepository.save(appt);
    }

    @Transactional
    public AppointmentEntity updateStatus(Long appointmentId, AppointmentStatus status, Long doctorId, String remarks, String results) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        Long resolvedDoctorId = doctorId != null ? doctorId : appointment.getDoctorId();
        DoctorEntity doctor = null;
        if (resolvedDoctorId != null) {
            doctor = doctorRepository.findById(resolvedDoctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        }

        if (status == AppointmentStatus.COMPLETED && resolvedDoctorId == null) {
            throw new IllegalArgumentException("Doctor is required when completing an appointment");
        }

        appointment.setDoctorId(resolvedDoctorId);
        appointment.setStatus(status);
        appointment.setCompletionRemarks(remarks);
        appointment.setCompletionResults(results);

        if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(LocalDateTime.now());
            createOrUpdateRecord(appointment, doctor, remarks, results);
        }

        AppointmentEntity saved = appointmentRepository.save(appointment);

        String message = String.format("Your appointment on %s at %s changed to %s.",
                saved.getDate(), saved.getTime(), saved.getStatus());
        notificationService.create(saved.getPatientId(), message);

        return saved;
    }

    private void createOrUpdateRecord(AppointmentEntity appointment, DoctorEntity doctor, String remarks, String results) {
        MedicalRecordEntity record = medicalRecordRepository.findByAppointmentId(appointment.getId())
                .orElseGet(MedicalRecordEntity::new);

        record.setAppointmentId(appointment.getId());
        record.setPatientId(appointment.getPatientId());
        record.setDoctorId(doctor == null ? appointment.getDoctorId() : doctor.getId());
        record.setDoctorName(doctor == null ? null : doctor.getName());
        record.setTitle(appointment.getReason());
        record.setSummary(buildSummary(remarks, results));
        record.setDate(appointment.getDate().toString());
        record.setRemarks(remarks);
        record.setResults(results);
        record.setCompletedAt(LocalDateTime.now());
        medicalRecordRepository.save(record);
    }

    private String buildSummary(String remarks, String results) {
        String safeRemarks = remarks == null ? "" : remarks.trim();
        String safeResults = results == null ? "" : results.trim();

        if (safeRemarks.isBlank() && safeResults.isBlank()) {
            return "Appointment completed.";
        }

        if (safeRemarks.isBlank()) {
            return safeResults;
        }

        if (safeResults.isBlank()) {
            return safeRemarks;
        }

        return safeRemarks + "\n\nResults: " + safeResults;
    }
}
