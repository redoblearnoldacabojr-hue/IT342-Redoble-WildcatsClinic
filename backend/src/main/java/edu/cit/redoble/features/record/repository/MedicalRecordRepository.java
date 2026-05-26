package edu.cit.redoble.features.record.repository;

import edu.cit.redoble.features.record.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecordEntity, Long> {
    List<MedicalRecordEntity> findByPatientId(Long patientId);
    List<MedicalRecordEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    java.util.Optional<MedicalRecordEntity> findByAppointmentId(Long appointmentId);
}
