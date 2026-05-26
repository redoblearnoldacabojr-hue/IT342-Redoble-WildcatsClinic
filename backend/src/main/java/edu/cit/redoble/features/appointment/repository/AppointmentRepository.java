package edu.cit.redoble.features.appointment.repository;

import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {
    List<AppointmentEntity> findByPatientId(Long patientId);
    Optional<AppointmentEntity> findByPatientIdAndDateAndTime(Long patientId, LocalDate date, LocalTime time);
    Optional<AppointmentEntity> findByDoctorIdAndDateAndTime(Long doctorId, LocalDate date, LocalTime time);
}
