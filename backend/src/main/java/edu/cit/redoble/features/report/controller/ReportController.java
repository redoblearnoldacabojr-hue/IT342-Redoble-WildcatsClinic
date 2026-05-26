package edu.cit.redoble.features.report.controller;

import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import edu.cit.redoble.features.appointment.entity.AppointmentStatus;
import edu.cit.redoble.features.appointment.repository.AppointmentRepository;
import edu.cit.redoble.features.doctor.repository.DoctorRepository;
import edu.cit.redoble.features.record.repository.MedicalRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public ReportController(AppointmentRepository appointmentRepository,
                            DoctorRepository doctorRepository,
                            MedicalRecordRepository medicalRecordRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Map<String, Object>> summary() {
        List<AppointmentEntity> appointments = appointmentRepository.findAll();
        long totalAppointments = appointments.size();
        long processing = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PROCESSING).count();
        long approved = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.APPROVED).count();
        long canceled = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELED).count();
        long completed = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        Map<Long, Long> doctorCounts = appointments.stream()
                .filter(a -> a.getDoctorId() != null)
                .collect(java.util.stream.Collectors.groupingBy(AppointmentEntity::getDoctorId, java.util.stream.Collectors.counting()));

        List<Map<String, Object>> doctorUtilization = doctorRepository.findAll().stream()
                .map(doctor -> {
                    Map<String, Object> item = new HashMap<>();
                    long count = doctorCounts.getOrDefault(doctor.getId(), 0L);
                    item.put("id", doctor.getId());
                    item.put("name", doctor.getName());
                    item.put("specialization", doctor.getSpecialization());
                    item.put("assignedAppointments", count);
                    return item;
                })
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("name"))))
                .toList();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAppointments", totalAppointments);
        summary.put("processingAppointments", processing);
        summary.put("approvedAppointments", approved);
        summary.put("canceledAppointments", canceled);
        summary.put("completedAppointments", completed);
        summary.put("totalDoctors", doctorRepository.count());
        summary.put("doctorsIn", doctorRepository.findByDoctorInTrueOrderByNameAsc().size());
        summary.put("availableDoctors", doctorRepository.findByDoctorInTrueOrderByNameAsc().size());
        summary.put("totalRecords", medicalRecordRepository.count());
        summary.put("doctorUtilization", doctorUtilization);

        return ResponseEntity.ok(summary);
    }
}